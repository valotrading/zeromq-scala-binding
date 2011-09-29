#!/bin/sh
java -Xmx512M -XX:MaxPermSize=256m -jar `dirname $0`/sbt-launch.jar "$@"
