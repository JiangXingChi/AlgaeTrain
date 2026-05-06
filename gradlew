#!/bin/sh
JAVA_HOME="/var/lib/flatpak/app/com.google.AndroidStudio/x86_64/stable/dc3668614b503ccfc4e05b437f9c91b9329472a7e1156e628d6b4d84d84f48f5/files/extra/jbr"
exec "$JAVA_HOME/bin/java" -classpath "gradle/wrapper/gradle-wrapper.jar" org.gradle.wrapper.GradleWrapperMain "$@"
