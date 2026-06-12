@echo off
setlocal

if defined JAVA_HOME (
    echo Using JAVA_HOME=%JAVA_HOME%
    goto run
)

for %%J in (
    "C:\Program Files\Eclipse Adoptium\jdk-8.0.452.9-hotspot"
    "C:\Program Files\Eclipse Adoptium\jdk-8.0.432.6-hotspot"
    "%LOCALAPPDATA%\Programs\Eclipse Adoptium\jdk-8.0.432.6-hotspot"
) do (
    if exist "%%~J\bin\java.exe" (
        set "JAVA_HOME=%%~J"
        goto run
    )
)

echo ERROR: JDK 8 is required to build this 1.8.9 Forge mod.
echo Install Eclipse Temurin 8, or set JAVA_HOME to a JDK 8 install.
exit /b 1

:run
call "%~dp0gradlew.bat" %*
