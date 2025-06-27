@echo off
echo Setting up environment variables for ChatBot API...

echo.
echo Please provide your OpenAI API Key:
echo (You can get one from: https://platform.openai.com/api-keys)
set /p OPENAI_API_KEY="Enter your OpenAI API Key: "

if "%OPENAI_API_KEY%"=="" (
    echo Error: OpenAI API Key cannot be empty
    pause
    exit /b 1
)

echo.
echo Setting environment variable...
setx OPENAI_API_KEY "%OPENAI_API_KEY%"

if %errorlevel% neq 0 (
    echo Error: Failed to set environment variable
    pause
    exit /b 1
)

echo.
echo ✅ Environment variables set successfully!
echo.
echo Note: You may need to restart your command prompt or IDE
echo       for the environment variable to take effect.
echo.
echo Next steps:
echo 1. Restart your command prompt
echo 2. Run: run-app.bat
echo 3. Open: http://localhost:8080/chat.html
echo.
pause 