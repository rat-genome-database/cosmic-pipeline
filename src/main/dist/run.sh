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
    -Dlog4j.configuration=file://$APPDIR/properties/log4j.properties \
    -jar lib/${APPNAME}.jar "$@" > run.log 2>&1

mailx -s "[$SERVER] Cosmic Pipeline run" mtutaj@mcw.edu < $APPDIR/logs/status.log
