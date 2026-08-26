import os
import shutil
import subprocess
import time

# Paths
ROOT_DIR = os.path.dirname(os.path.abspath(__file__))
APP_DIR = os.path.join(ROOT_DIR, "app")
SOURCE_CODE_DIR = os.path.join(ROOT_DIR, "Source code")

def run_command(command, cwd=ROOT_DIR):
    """Runs a shell command and returns the output."""
    try:
        result = subprocess.run(
            command,
            cwd=cwd,
            shell=True,
            capture_output=True,
            text=True,
            check=True
        )
        return result.stdout.strip()
    except subprocess.CalledProcessError as e:
        print(f"Error running command: {command}\n{e.stderr}")
        return None

def notify(title, message):
    """Shows a Windows toast notification using PowerShell."""
    script = f"""
    $title = '{title}'
    $message = '{message}'
    [Windows.UI.Notifications.ToastNotificationManager, Windows.UI.Notifications, ContentType = WindowsRuntime] > $null
    $template = [Windows.UI.Notifications.ToastNotificationManager]::GetTemplateContent([Windows.UI.Notifications.ToastTemplateType]::ToastText02)
    $toastXml = [xml]$template.GetXml()
    $toastXml.GetElementsByTagName('text').Item(0).AppendChild($toastXml.CreateTextNode($title)) > $null
    $toastXml.GetElementsByTagName('text').Item(1).AppendChild($toastXml.CreateTextNode($message)) > $null
    $xml = New-Object Windows.Data.Xml.Dom.XmlDocument
    $xml.LoadXml($toastXml.OuterXml)
    $toast = [Windows.UI.Notifications.ToastNotification]::new($xml)
    [Windows.UI.Notifications.ToastNotificationManager]::CreateToastNotifier('MusicX').Show($toast)
    """
    subprocess.run(["powershell", "-Command", script], capture_output=True)

def sync():
    print("Checking for changes in the app folder...")

    # Check if there are changes in the app directory compared to the last commit
    status = run_command("git status app --short")

    if not status:
        print("No changes detected in the app folder. Everything is up to date.")
        return

    print("Changes detected! Updating the 'Source code' folder...")

    # Remove existing Source code folder content (except the folder itself)
    if os.path.exists(SOURCE_CODE_DIR):
        for item in os.listdir(SOURCE_CODE_DIR):
            item_path = os.path.join(SOURCE_CODE_DIR, item)
            if os.path.isdir(item_path):
                shutil.rmtree(item_path)
            else:
                os.remove(item_path)
    else:
        os.makedirs(SOURCE_CODE_DIR)

    # Copy everything from app to Source code, excluding build folders
    def ignore_build(path, names):
        return [n for n in names if n == 'build' or n.startswith('.')]

    shutil.copytree(APP_DIR, SOURCE_CODE_DIR, ignore=ignore_build, dirs_exist_ok=True)

    print("Committing and pushing changes to GitHub...")
    run_command("git add \"Source code\"")
    run_command('git commit -m "sync: update Source code folder with latest app changes"')
    run_command("git push origin main")

    print("Done! GitHub is now synced.")
    notify("MusicX Sync", "The 'Source code' folder has been updated and pushed to GitHub.")

if __name__ == "__main__":
    print("Sync service started. Watching for changes every 60 seconds...")
    while True:
        try:
            sync()
        except Exception as e:
            print(f"Sync failed: {e}")
        time.sleep(60)  # Check every minute
