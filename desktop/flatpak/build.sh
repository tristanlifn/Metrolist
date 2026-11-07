#!/bin/sh
# Builds the desktop app and installs it as a user Flatpak (com.metrolist.music).
set -eu
cd "$(dirname "$0")/../.."

./gradlew :desktop:packageUberJarForCurrentOS
mkdir -p desktop/build/flatpak
cp "$(ls -t desktop/build/compose/jars/*.jar | head -1)" desktop/build/flatpak/metrolist.jar

flatpak-builder --user --install --force-clean \
    --install-deps-from=flathub \
    --state-dir=desktop/build/flatpak/.flatpak-builder \
    desktop/build/flatpak/build-dir \
    desktop/flatpak/com.metrolist.music.yml
