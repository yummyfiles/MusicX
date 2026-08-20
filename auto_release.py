import os
import re
import subprocess
import shutil

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
    version = get_version()
    if not version:
        print("Error: Could not find versionName in build.gradle.kts")
        return

    tag = f"v{version}"
    apk_path = "app/build/outputs/apk/release/app-release.apk"

    if not os.path.exists(apk_path):
        print(f"Error: APK not found at {apk_path}. Did you build the release?")
        return

    print(f"Detected version: {version}")

    # Check if tag exists locally
    tags = subprocess.check_output(["git", "tag"]).decode().split()
    if tag in tags:
        print(f"Tag {tag} already exists. Skipping.")
    else:
        print(f"Creating tag {tag}...")
        try:
            subprocess.run(["git", "tag", tag], check=True)
            print(f"Pushing tag {tag} to origin...")
            subprocess.run(["git", "push", "origin", tag], check=True)
            print("Successfully pushed tag. GitHub Actions will handle the release.")
        except subprocess.CalledProcessError as e:
            print(f"Error running git command: {e}")

if __name__ == "__main__":
    main()
