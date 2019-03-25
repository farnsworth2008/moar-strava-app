set -e
./build-cli.sh
time java -Dmoar.ansi.enabled=true -jar `find . -name \*cli.fat.jar` moar-strava "$@"
