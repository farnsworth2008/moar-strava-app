set -e

# Switch to JAVA8
export PATH=$JAVA8_HOME/bin:$PATH
BUILD_VERSION="`git describe --always --tags --long`"
./gradlew bootWar -Dbuild_version=$BUILD_VERSION
rm -rf artifacts
mkdir -p artifacts
mv `find . -name ROOT.war` artifacts
