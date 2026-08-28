#!/usr/bin/env bash
#
# Pre-installs the Android SDK platform the app compiles against, so the build
# does not spend its first minutes downloading one.
#
# The package id is not simply "android-$compileSdk". From API 36.1 onwards the
# platforms carry a minor component — the package for `compileSdk = 37` is
# `platforms;android-37.0`, and there is no `platforms;android-37` at all — while
# API 36 and earlier are still published unsuffixed as `platforms;android-36`.
# Which form applies cannot be worked out from `compileSdk` on its own, so this
# tries the dotted id first and falls back to the bare one.
#
# Failing to install is a warning rather than an error: AGP downloads a missing
# platform itself (the licences are accepted by android-actions/setup-android),
# and it knows exactly which package it wants. This step is an optimisation, and
# a wrong guess here must not be what fails the build.
set -euo pipefail

build_file="app/build.gradle.kts"

# Reads an integer `name = 123` out of the build file. `compileSdkMinor` does not
# match the `compileSdk` pattern, because the character after the name has to be
# whitespace or `=`.
read_int() {
  local value
  value=$(sed -n "s/^[[:space:]]*$1[[:space:]]*=[[:space:]]*\([0-9][0-9]*\).*/\1/p" "$build_file")
  # First match only, without a pipe to `head`, which under `pipefail` can fail
  # the script by killing `sed` with SIGPIPE.
  printf '%s' "${value%%$'\n'*}"
}

major=$(read_int compileSdk)
minor=$(read_int compileSdkMinor)
: "${minor:=0}"

if [ -z "$major" ]; then
  echo "::warning::Could not read compileSdk from $build_file; leaving the platform to AGP."
  exit 0
fi

for package in "platforms;android-$major.$minor" "platforms;android-$major"; do
  echo "Trying $package"
  if sdkmanager --install "$package"; then
    echo "Installed $package"
    exit 0
  fi
done

echo "::warning::No SDK platform package matched compileSdk $major (tried both" \
     "platforms;android-$major.$minor and platforms;android-$major). Leaving it to" \
     "AGP's SDK auto-download, which will fail loudly if the platform is genuinely" \
     "unavailable."
