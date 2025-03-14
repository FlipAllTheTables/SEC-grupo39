import subprocess
import time

# Define the base command for WSL
base_command = "cd .. && mvn compile && mvn exec:java -Dexec.args="

# Open the first terminal to run mvn clean install before executing the command
subprocess.Popen([
    "wt.exe", "new-tab", "wsl.exe", "--", "bash", "-c",
    "cd .. && mvn clean install && mvn compile && mvn exec:java -Dexec.args='0 6 0 0'"
])
time.sleep(5)  # Wait for build process to start

# Open five additional terminals as new tabs (not split panes)
for i in range(3):
    subprocess.Popen([
        "wt.exe", "new-tab", "wsl.exe", "--", "bash", "-c",
        f"{base_command}'{i+1} 6 0 0'"
    ])

# Open one last terminal with the `6 0 1` argument
subprocess.Popen([
    "wt.exe", "new-tab", "wsl.exe", "--", "bash", "-c",
    f"{base_command}'4 6 1 0'"
])

# Open one last terminal with the `6 0 1` argument
subprocess.Popen([
    "wt.exe", "new-tab", "wsl.exe", "--", "bash", "-c",
    f"{base_command}'5 6 1 0'"
])

# Open one last terminal with the `6 0 1` argument
subprocess.Popen([
    "wt.exe", "new-tab", "wsl.exe", "--", "bash", "-c",
    f"{base_command}'6 6 0 1'"
])