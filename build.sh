set -e

BUILD_VERSION="`git describe --always --tags --long`"
./gradlew bootWar -Dbuild_version=$BUILD_VERSION
rm -rf artifacts stage
mkdir -p artifacts
mkdir -p stage
cp docker/* stage
mv `find . -name ROOT.war` stage
cd stage
zip -r ../artifacts/image.zip *

cd ~/moar-workspace
rm -rf `find . -name build -type d`
