set -e

# Build the boot war and refresh the eclipse project settings
BUILD_VERSION="`git describe --always --tags --long`"
./gradlew fatJar -Dbuild_version=$BUILD_VERSION
