import subprocess
import sys
import os
import re
import hashlib
import argparse
from pathlib import Path

# MusicX Release Automation Script
# Version: 1.0.0

def print_step(step_num, message, status=None):
    status_str = f" [{status}]" if status else ""
    print(f"{step_num}. {message}{status_str}")

def run_command(cmd, shell=False, check=True, capture_output=True):
    try:
        result = subprocess.run(
            cmd,
            shell=shell,
            check=check,
            capture_output=capture_output,
            text=True,
            encoding='utf-8'
        )
        return result
    except subprocess.CalledProcessError as e:
        if check:
            print(f"Error executing command: {' '.join(cmd) if isinstance(cmd, list) else cmd}")
            print(f"Stdout: {e.stdout}")
            print(f"Stderr: {e.stderr}")
            sys.exit(1)
        return e

class ReleaseManager:
    def __init__(self, args):
        self.args = args
        self.project_root = Path(__file__).parent.absolute()
        self.version_name = None
        self.short_sha = None
        self.full_sha = None
        self.branch = None
        self.apk_path = None
        self.apk_sha256 = None

    def verify_git(self):
        print_step(1, "Verifying Git installation and repository...")
        run_command(["git", "--version"])

        if not (self.project_root / ".git").exists():
            print("[ERROR] Not a git repository.")
            sys.exit(1)

        remotes = run_command(["git", "remote", "-v"]).stdout
        if "origin" not in remotes:
            print("[ERROR] 'origin' remote not found.")
            sys.exit(1)
        print_step(1, "Git verified", "OK")

    def verify_working_tree(self):
        print_step(2, "Checking working tree status...")
        status = run_command(["git", "status", "--porcelain"]).stdout.strip()
        if status:
            print("[ERROR] Working tree is not clean. Please commit or stash changes.")
            print("Changed files:")
            print(status)
            sys.exit(1)
        print_step(2, "Working tree clean", "OK")

    def identify_commit(self):
        print_step(3, "Identifying commit metadata...")
        self.full_sha = run_command(["git", "rev-parse", "HEAD"]).stdout.strip()
        self.short_sha = run_command(["git", "rev-parse", "--short", "HEAD"]).stdout.strip()
        self.branch = run_command(["git", "rev-parse", "--abbrev-ref", "HEAD"]).stdout.strip()

        if self.branch != "main":
            print(f"[WARNING] You are on branch '{self.branch}', not 'main'.")
            if not self.args.dry_run and not self.args.build:
                 input("Press Enter to continue anyway, or Ctrl+C to abort...")

        print_step(3, f"Commit: {self.short_sha} on {self.branch}", "OK")

    def detect_version(self):
        print_step(4, "Reading version from app/build.gradle.kts...")
        gradle_path = self.project_root / "app" / "build.gradle.kts"
        if not gradle_path.exists():
            print("[ERROR] app/build.gradle.kts not found.")
            sys.exit(1)

        content = gradle_path.read_text(encoding='utf-8')
        match = re.search(r'versionName\s*=\s*"([^"]+)"', content)
        if not match:
            print("[ERROR] Could not find versionName in build.gradle.kts")
            sys.exit(1)

        self.version_name = match.group(1)
        print_step(4, f"Version detected: {self.version_name}", "OK")

    def version_continuity_check(self):
        print_step(5, "Checking version continuity...")
        # Check if gh is installed
        run_command(["gh", "--version"])

        try:
            tags_out = run_command(["git", "tag", "--sort=-v:refname"]).stdout.strip().split('\n')
            latest_tag = tags_out[0] if tags_out and tags_out[0] else None
        except:
            latest_tag = None

        if latest_tag:
            current_v = self.version_name
            prev_v = latest_tag.lstrip('v')

            # Simple semver comparison
            try:
                curr_parts = [int(p) for p in current_v.split('.')]
                prev_parts = [int(p) for p in prev_v.split('.')]

                # Check for jumps
                is_jump = False
                if len(curr_parts) == 3 and len(prev_parts) == 3:
                    if curr_parts[0] == prev_parts[0] and curr_parts[1] == prev_parts[1]:
                        if curr_parts[2] > prev_parts[2] + 1:
                            is_jump = True
                    elif curr_parts[0] == prev_parts[0]:
                         if curr_parts[1] > prev_parts[1] + 1:
                            is_jump = True

                if is_jump and not self.args.allow_version_jump:
                    print(f"[WARNING] Version jump detected: {prev_v} -> {current_v}")
                    if not self.args.dry_run:
                        confirm = input("Allow version jump? (y/N): ")
                        if confirm.lower() != 'y':
                            sys.exit(1)
            except ValueError:
                print(f"[WARNING] Non-standard version format. skipping continuity check.")

        print_step(5, "Version continuity checked", "OK")

    def fetch_and_sync(self):
        print_step(6, "Checking remote synchronization...")
        if self.args.dry_run:
            print("  [DRY RUN] Skipping git fetch")
            return

        run_command(["git", "fetch", "origin"])

        tracking = run_command(["git", "rev-parse", "--abbrev-ref", "--symbolic-full-name", "@{u}"], check=False).stdout.strip()
        if not tracking:
            print("[WARNING] No tracking branch found. Will attempt to push to origin.")
            return

        local_sha = self.full_sha
        remote_sha = run_command(["git", "rev-parse", "@{u}"]).stdout.strip()
        base_sha = run_command(["git", "merge-base", "@", "@{u}"]).stdout.strip()

        if local_sha == remote_sha:
            print("  Local is up to date with remote.")
        elif local_sha == base_sha:
            print("[ERROR] Local is behind remote. Please pull changes.")
            sys.exit(1)
        elif remote_sha == base_sha:
            print("  Local is ahead of remote. Will push during release.")
        else:
            print("[ERROR] Local and remote have diverged.")
            sys.exit(1)
        print_step(6, "Sync status verified", "OK")

    def build_apk(self):
        if self.args.no_build:
            print_step(7, "Skipping build as requested", "OK")
            # Try to find existing APK if we need to release
            self.locate_apk()
            return

        print_step(7, "Building Release APK...")
        gradlew = "gradlew.bat" if os.name == 'nt' else "./gradlew"

        if self.args.dry_run:
            print(f"  [DRY RUN] Would run: {gradlew} assembleRelease")
            self.apk_path = "app/build/outputs/apk/release/app-release.apk"
            self.apk_sha256 = "DRY-RUN-SHA-256"
            return

        # Clean build to ensure integrity
        run_command([gradlew, "clean", "assembleRelease"], shell=True)
        self.locate_apk()
        self.verify_apk()
        print_step(7, "Build successful", "OK")

    def locate_apk(self):
        potential_paths = [
            self.project_root / "app/build/outputs/apk/release/app-release.apk",
            self.project_root / "app/build/outputs/apk/release/MusicX-release.apk" # Just in case
        ]

        for path in potential_paths:
            if path.exists():
                self.apk_path = path
                return

        if not self.args.dry_run:
            print("[ERROR] Could not find release APK after build.")
            sys.exit(1)

    def verify_apk(self):
        if not self.apk_path or not self.apk_path.exists():
            return

        # Check size > 1MB as a basic integrity check
        size_mb = os.path.getsize(self.apk_path) / (1024 * 1024)
        if size_mb < 1.0:
             print(f"[WARNING] APK size is unusually small: {size_mb:.2f} MB")

        # Calculate SHA-256
        sha256_hash = hashlib.sha256()
        with open(self.apk_path, "rb") as f:
            for byte_block in iter(lambda: f.read(4096), b""):
                sha256_hash.update(byte_block)
        self.apk_sha256 = sha256_hash.hexdigest()
        print(f"  APK SHA-256: {self.apk_sha256}")

    def tag_and_push(self):
        tag_name = f"v{self.version_name}"
        print_step(8, f"Managing Git tag {tag_name}...")

        # Check if tag exists
        tag_exists = run_command(["git", "tag", "-l", tag_name]).stdout.strip()
        if tag_exists:
            existing_sha = run_command(["git", "rev-list", "-n", "1", tag_name]).stdout.strip()
            if existing_sha == self.full_sha:
                print(f"  Tag {tag_name} already exists on this commit.")
            else:
                print(f"[ERROR] Tag {tag_name} already exists on a different commit ({existing_sha[:7]}).")
                print("Manual intervention required. No force tag allowed.")
                sys.exit(1)
        else:
            if self.args.dry_run:
                print(f"  [DRY RUN] Would create tag {tag_name}")
            else:
                run_command(["git", "tag", "-a", tag_name, "-m", f"Release {tag_name}"])
                print(f"  Created tag {tag_name}")

        print_step(9, "Pushing to origin...")
        if self.args.dry_run:
            print(f"  [DRY RUN] Would push branch {self.branch} and tag {tag_name}")
        else:
            run_command(["git", "push", "origin", self.branch])
            run_command(["git", "push", "origin", tag_name])
            print("  Pushed successfully.")

    def create_github_release(self):
        if self.args.no_release:
             print_step(10, "Skipping GitHub Release as requested", "OK")
             return

        tag_name = f"v{self.version_name}"
        release_name = f"MusicX {tag_name}"
        print_step(10, f"Creating GitHub Release: {release_name}...")

        # Verify gh auth
        if not self.args.dry_run:
            auth_check = run_command(["gh", "auth", "status"], check=False)
            if auth_check.returncode != 0:
                print("[ERROR] GitHub CLI (gh) is not authenticated. Run 'gh auth login'.")
                sys.exit(1)

        # Check if release already exists
        release_exists = run_command(["gh", "release", "view", tag_name], check=False).returncode == 0

        if self.args.dry_run:
            print(f"  [DRY RUN] Would create release {release_name} and upload {self.apk_path.name}")
            return

        if not release_exists:
            notes = f"Release version {self.version_name}\n\nCommit: {self.short_sha}\nSHA-256: {self.apk_sha256}"
            run_command([
                "gh", "release", "create", tag_name,
                "--title", release_name,
                "--notes", notes
            ])
            print(f"  Release created.")
        else:
            print(f"  Release {tag_name} already exists.")

        # Upload APK
        print_step(11, f"Uploading APK to release...")
        # Check if file is already there
        assets = run_command(["gh", "release", "view", tag_name, "--json", "assets", "-q", ".assets[].name"]).stdout.strip().split('\n')
        if self.apk_path.name in assets:
            print(f"  Asset {self.apk_path.name} already exists. Skipping upload.")
        else:
            run_command(["gh", "release", "upload", tag_name, str(self.apk_path)])
            print(f"  APK uploaded.")

    def final_summary(self):
        print("\n" + "="*50)
        print("RELEASE SUMMARY")
        print("="*50)
        print(f"Version:   {self.version_name}")
        print(f"Tag:       v{self.version_name}")
        print(f"Branch:    {self.branch}")
        print(f"Commit:    {self.short_sha}")
        if self.apk_sha256:
            print(f"SHA-256:   {self.apk_sha256}")

        if not self.args.dry_run and not self.args.no_release:
            repo_url = run_command(["gh", "repo", "view", "--json", "url", "-q", ".url"]).stdout.strip()
            print(f"Release:   {repo_url}/releases/tag/v{self.version_name}")
            print(f"Artifact:  {self.apk_path.name}")

        print("\n[SUCCESS] Release process completed.")

def main():
    parser = argparse.ArgumentParser(description="MusicX Post-Commit Release Automation")
    parser.add_argument("--dry-run", action="store_true", help="Show actions without executing them")
    parser.add_argument("--no-build", action="store_true", help="Skip building the APK")
    parser.add_argument("--no-release", action="store_true", help="Skip creating GitHub release and uploading")
    parser.add_argument("--build", action="store_true", help="Only build the APK, do not release")
    parser.add_argument("--allow-version-jump", action="store_true", help="Do not prompt for version jumps")

    args = parser.parse_args()

    if args.build:
        args.no_release = True

    manager = ReleaseManager(args)

    try:
        manager.verify_git()
        manager.verify_working_tree()
        manager.identify_commit()
        manager.detect_version()
        manager.version_continuity_check()
        manager.fetch_and_sync()

        manager.build_apk()

        if not args.build:
            manager.tag_and_push()
            manager.create_github_release()

        manager.final_summary()

    except KeyboardInterrupt:
        print("\n[ABORTED] User interrupted the process.")
        sys.exit(1)
    except Exception as e:
        print(f"\n[ERROR] Unexpected error: {e}")
        import traceback
        traceback.print_exc()
        sys.exit(1)

if __name__ == "__main__":
    main()
