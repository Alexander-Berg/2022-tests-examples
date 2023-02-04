#!/bin/bash

set -e

MYSQL_NAME="$1"
DATE=$(date +%Y%m%d)
S3URL="http://s3.mds.yandex.net"
S3PATH="vertis-backups/testing/mysql"
LOGFILE="/var/log/backup-mysql-to-s3.log"
AWS="/usr/local/bin/aws --profile=vertis-backups --endpoint-url=http://s3.mds.yandex.net"


if [ -z "${MYSQL_NAME}" ];
then
  date +"[%F %X] MYSQL_NAME isn't provided. Exit."
  exit 1
fi

if [ ! $(echo "${MYSQL_NAME}" | grep "\-devtest") ];
then
  date +"[%F %X] MYSQL_NAME is not for devtest instance. Exit."
  exit 1
fi

if [ $(hostname | grep 'dev.vertis.yandex.net') ]; then
    date +"[%F %X] I am in dev environment, no replication here. Exit."
    exit 0
fi

function check_error() {
    if [[ ${1} -ne 0 ]]; then
      date +"[%F %X] Error, while taking snapshot for s3."
      exit 1
    fi
}

function log {
    date +"[%F %X] $1" >> "$LOGFILE"
}



function truncate_backup_s3 {
  # expires don't work in mds https://st.yandex-team.ru/MDS-2887
  log "[#] Start S3 cleanup"
  S3OVERLIST=$($AWS s3 ls s3://${S3PATH}/ | grep -v latest | awk '{print $4}'| grep "^${MYSQL_NAME}-[0-9]\{8\}.tgz" | sort -r | tail -n +14)
  if [ -n "$S3OVERLIST" ]; then
    for n in $S3OVERLIST; do
      log "[->] Clean old: s3://${S3PATH}/$n"
      $AWS s3 rm "s3://${S3PATH}/$n" >> $LOGFILE 2>&1
    done
    log "[#] End S3 cleanup"
  else
    log "[#] Nothing to cleanup"
  fi
}


MYSQL_CONFIG="/etc/mysql-multi/$MYSQL_NAME.cnf"
SCREEN_NAME="backup-s3.mysql-${MYSQL_NAME}"
BACKUPDIR="/tmp/backups-for-s3"
SNAPSHOT_NAME=${MYSQL_NAME}-${DATE}
BACKUPDIR_INSTANCE="${BACKUPDIR}/${SNAPSHOT_NAME}/"

#"cool" hack - if previous backup failed it will clean up space to prevent disaster
rm -rf ${BACKUPDIR}/*

mkdir -p ${BACKUPDIR_INSTANCE}


screen -S ${SCREEN_NAME} -dm bash -c "while ps $$; do sleep 1; done"

screen -S $SCREEN_NAME -X sessionname \"$SCREEN_NAME [\backup data]\"
date +"[%F %X] Starting xtrabackup instance ${MYSQL_NAME} "
ulimit -n 40960 ; xtrabackup --defaults-file=/etc/mysql-multi/${MYSQL_NAME}.cnf --target-dir=${BACKUPDIR_INSTANCE} --backup --parallel=4
check_error $?
ulimit -n 40960 ; xtrabackup --prepare --export --use-memory=2G --parallel=4 --target-dir=${BACKUPDIR_INSTANCE}
check_error $?
date +"[%F %X] Xtrabackup instance ${MYSQL_NAME} finished"


cd ${BACKUPDIR}
date +"[%F %X] Starting backup instance ${MYSQL_NAME} to s3"

tar -cf - ${SNAPSHOT_NAME} | gzip -c | ${AWS} --only-show-errors s3 cp - s3://${S3PATH}/${SNAPSHOT_NAME}.tgz >> $LOGFILE 2>&1 ; rm -rf ${SNAPSHOT_NAME}

if [ "$?" -eq "0" ]; then
  # echo "${S3URL}/${S3PATH}/${SNAPSHOT_NAME}.aes" |  ${AWS} --only-show-errors s3 cp - s3://${S3PATH}/${MYSQL_NAME}_LATEST.txt >> $LOGFILE 2>&1
  echo "${S3URL}/${S3PATH}/${SNAPSHOT_NAME}.tgz" |  ${AWS} --only-show-errors s3 cp - s3://${S3PATH}/${MYSQL_NAME}_LATEST.txt >> $LOGFILE 2>&1
fi


date +"[%F %X] Backup instance ${MYSQL_NAME} to s3 finished"

date +"[%F %X] Cleaning up instance ${MYSQL_NAME} on s3"
truncate_backup_s3 >> $LOGFILE 2>&1
date +"[%F %X] Cleaning up instance ${MYSQL_NAME} on s3 finished"
