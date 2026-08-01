#!/usr/bin/env sh
if command -v gradle >/dev/null 2>&1; then
    gradle "$@"
elif [ -f "$(dirname "$0")/gradle/wrapper/gradle-wrapper.jar" ]; then
    exec java -classpath "$(dirname "$0")/gradle/wrapper/gradle-wrapper.jar" org.gradle.wrapper.GradleWrapperMain "$@"
else
    echo "Gradle not found" >&2
    exit 1
fi
