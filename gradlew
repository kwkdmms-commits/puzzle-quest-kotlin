#!/bin/bash

# Gradle wrapper script for CI/CD environments
# Downloads and runs Gradle if not present

GRADLE_VERSION="8.5"
GRADLE_HOME="${GRADLE_USER_HOME:-$HOME/.gradle}"
GRADLE_DIST="$GRADLE_HOME/gradle-$GRADLE_VERSION"

if [ ! -d "$GRADLE_DIST" ]; then
  mkdir -p "$GRADLE_HOME"
  cd "$GRADLE_HOME"
  curl -L "https://services.gradle.org/distributions/gradle-$GRADLE_VERSION-bin.zip" -o gradle-$GRADLE_VERSION-bin.zip
  unzip -q gradle-$GRADLE_VERSION-bin.zip
  rm gradle-$GRADLE_VERSION-bin.zip
  cd -
fi

exec "$GRADLE_DIST/bin/gradle" "$@"
