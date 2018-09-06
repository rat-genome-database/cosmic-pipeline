#!/usr/bin/env bash
#
# cosmic loading pipeline
#
. /etc/profile
APPNAME=cosmicPipeline
SERVER=`hostname -s | tr '[a-z]' '[A-Z]'`

APPDIR=/home/rgddata/pipelines/$APPNAME
cd $APPDIR
pwd
DB_OPTS="-Dspring.config=$APPDIR/../properties/default_db.xml"
LOG4J_OPTS="-Dlog4j.configuration=file://$APPDIR/properties/log4j.properties"
declare -x "COSMIC_PIPELINE_OPTS=$DB_OPTS $LOG4J_OPTS"
bin/$APPNAME "$@" 2>&1 | tee run.log

mailx -s "[$SERVER] Cosmic Pipeline run" mtutaj@mcw.edu < logs/status.log