#!/usr/bin/env bash
#
# cosmic loading pipeline
#
. /etc/profile
APPNAME="cosmic-pipeline"
SERVER=`hostname -s | tr '[a-z]' '[A-Z]'`

APPDIR=/home/rgddata/pipelines/$APPNAME
cd $APPDIR || exit 1

java -Dspring.config=$APPDIR/../properties/default_db2.xml \
    -Dlog4j.configurationFile=file://$APPDIR/properties/log4j2.xml \
    -jar lib/${APPNAME}.jar "$@" > run.log 2>&1
STATUS=$?

if [ $STATUS -eq 0 ]; then
    mailx -s "[$SERVER] Cosmic Pipeline OK" mtutaj@mcw.edu < $APPDIR/logs/summary.log
else
    mailx -s "[$SERVER] Cosmic Pipeline FAILED (exit code $STATUS)" mtutaj@mcw.edu < $APPDIR/logs/summary.log
fi
exit $STATUS
