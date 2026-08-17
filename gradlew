#!/bin/sh
JAVA_HOME="/var/lib/flatpak/app/com.google.AndroidStudio/x86_64/stable/c98eca696f8cd3b09f8a9f158ccd7c0c55f1e59bb36297235759ab3afe678ba9/files/extra/jbr"
exec "$JAVA_HOME/bin/java" -classpath "gradle/wrapper/gradle-wrapper.jar" org.gradle.wrapper.GradleWrapperMain "$@"
