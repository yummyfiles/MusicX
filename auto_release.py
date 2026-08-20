import os
import re
import subprocess
import sys

def get_version():
    gradle_path = "app/build.gradle.kts"
    if not os.path.exists(gradle_path):
        return None
    with open(gradle_path, "r") as f:
        content = f.read()
        match = re.search(r'versionName\s*=\s*"([^"]+)"', content)
        if match:
            return match.group(1)
    return None

def main():
    print("--- MusicX Auto-Release System ---")
    version = get_version()
    if not version:
        print("Error: Could not find versionName in build.gradle.kts")
        sys.exit(1)

    tag = f"v{version}"
    print(f"Detected app version: {version}")

    try:
        # Update remote tags list
        subprocess.run(["git", "fetch", "--tags"], check=True, capture_output=True)

        # Check if tag exists anywhere (local or remote)
        existing_tags = subprocess.check_output(["git", "tag"]).decode().split()
        if tag in existing_tags:
            print(f"Release {tag} already exists on GitHub. Skipping automation.")
            return

        print(f"Release {tag} not found. Preparing automatic publication...")

        # Ensure changes are committed (optional, but safer)
        status = subprocess.check_output(["git", "status", "--porcelain"]).decode()
        if status:
            print("Warning: You have uncommitted changes. The release tag will point to the last commit.")

        # Create and push tag
        print(f"Creating Git tag: {tag}")
        subprocess.run(["git", "tag", tag], check=True)

        print(f"Pushing to GitHub: {tag}")
        subprocess.run(["git", "push", "origin", tag], check=True)

        print(f"SUCCESS: GitHub Release for {tag} has been triggered!")
        print("The APK will appear on your GitHub Releases page in a few minutes.")

    except subprocess.CalledProcessError as e:
        print(f"Error during Git automation: {e}")
        # Don't exit with error to avoid failing the whole Gradle build
    except Exception as e:
        print(f"Unexpected error: {e}")

if __name__ == "__main__":
    main()
