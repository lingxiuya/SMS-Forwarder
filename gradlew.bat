@echo off
where gradle >nul 2>nul
if %ERRORLEVEL% EQU 0 (
    gradle %*
) else (
    if exist "%~dp0gradle\wrapper\gradle-wrapper.jar" (
        java -classpath "%~dp0gradle\wrapper\gradle-wrapper.jar" org.gradle.wrapper.GradleWrapperMain %*
    ) else (
        echo Gradle wrapper JAR not found and gradle not in PATH.
        exit /b 1
    )
)
