@echo off
echo Setting up Python environment for the scraper...

REM Check if Python is installed
python --version >nul 2>&1
if %errorlevel% neq 0 (
    echo Python is not installed or not in PATH
    echo Please install Python 3.8+ and add it to your PATH
    pause
    exit /b 1
)

echo Python found. Installing dependencies...

REM Install required packages
pip install -r src/main/resources/scraper-script/requirements.txt

if %errorlevel% neq 0 (
    echo Failed to install Python dependencies
    pause
    exit /b 1
)

echo Python environment setup completed successfully!
pause 