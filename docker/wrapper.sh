#!/usr/bin/env bash
exec java --add-opens 'java.base/java.lang=ALL-UNNAMED' -jar 'ROOT.war' > /dev/stdout 2>&1
