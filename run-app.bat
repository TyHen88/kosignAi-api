@echo off
echo Starting ChatBot API Application...

echo Step 1: Setting up Python dependencies...
call setup-python.bat
if %errorlevel% neq 0 (
    echo Failed to setup Python dependencies
    pause
    exit /b 1
)

echo Step 2: Building the Spring Boot application...
call .\gradlew.bat clean build
if %errorlevel% neq 0 (
    echo Failed to build the application
    pause
    exit /b 1
)

echo Step 3: Running the Spring Boot application...
call .\gradlew.bat bootRun

pause 