#!/usr/bin/env bash
#
# cosmic loading pipeline
#
. /etc/profile
APPNAME=cosmicPipeline
SERVER=`hostname -s | tr '[a-z]' '[A-Z]'`

APPDIR=/home/rgddata/pipelines/$APPNAME
cd $APPDIR

java -Dspring.config=$APPDIR/../properties/default_db2.xml \
    -Dlog4j.configurationFile=file://$APPDIR/properties/log4j2.xml \
    -jar lib/${APPNAME}.jar "$@" > run.log 2>&1

mailx -s "[$SERVER] Cosmic Pipeline run" mtutaj@mcw.edu < $APPDIR/logs/status.log
