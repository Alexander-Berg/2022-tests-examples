#!/bin/bash

FR=FR
PROG_ROOT=/FR
DATA_ROOT=$PROG_ROOT/data
LOG_ROOT=$PROG_ROOT/log
DOC_DB=$DATA_ROOT/FRdoc.db
TABLE_DB=$DATA_ROOT/FRtable.db

DEBUG=0

PROG_LOG=$LOG_ROOT/FR.log

export PROG_ROOT
export DATA_ROOT
export LOG_ROOT
export DOC_DB
export TABLE_DB

cd $PROG_ROOT

if [ -f debug.flag ]
then
    DEBUG=1
fi

FR_PARMS=""
if [ $DEBUG -eq 1 ]
then
    FR_PARMS="$FR_PARMS -debug"
fi
if [ $DOC_DB ]
then
    FR_PARMS="$FR_PARMS -doc-db $DOC_DB"
fi
if [ $TABLE_DB ]
then
    FR_PARMS="$FR_PARMS -table-db $TABLE_DB"
fi

FR_PARMS="$FR_PARMS -console -no-syslog"

echo "Program root: $PROG_ROOT"
echo "Data root   : $DATA_ROOT"
echo "Log root    : $LOG_ROOT"
echo "Doc DB      : $DOC_DB"
echo "Table DB    : $TABLE_DB"
echo "Program log : $PROG_LOG"
echo "Parameters  : $FR_PARMS"

while true
do
    if [ ! -d $DATA_ROOT ]
    then
        mkdir $DATA_ROOT
    fi

    if [ ! -d $LOG_ROOT ]
    then
        mkdir $LOG_ROOT
    fi

    if [ -f FR.new.zip ]
    then
        echo "INFO: New FR found, do unzip..."
        unzip -o FR.new.zip
        echo "...done"
        rm FR.new.zip
    fi

    if [ -f FR.good ]
    then
        echo "INFO: Do update..."
        echo "Do update..."

## #        ./update.sh
        cp $PROG_ROOT/FR.good $PROG_ROOT/FR.bin
        gpg --decrypt $PROG_ROOT/FR.bin > $PROG_ROOT/update.zip
        rm $PROG_ROOT/FR.bin
        unzip $PROG_ROOT/update.zip
        rm $PROG_ROOT/update.zip
        cp $PROG_ROOT/update/FR $PROG_ROOT/FR
        if [ $? -ne 0 ]
        then
            echo "ERROR: cp $PROG_ROOT/update/FR $PROG_ROOT/FR"
        fi

        cp $PROG_ROOT/update/version.json $PROG_ROOT/
        if [ $? -ne 0 ]
        then
            echo "ERROR: cp $PROG_ROOT/update/version.json $PROG_ROOT/"
        fi

        if [ $? -eq 0 ]
        then
            rm -rf $PROG_ROOT/update/
            rm $PROG_ROOT/FR.good
        fi
        chmod +x $PROG_ROOT/FR
        echo "...done"
    fi

    if [ -f dropdb.flag ]
    then
        echo "INFO: Pack old data"
        DATE=$(date +"%Y%m%d%H%M")
        zip -r "data$DATE".zip $DATA_ROOT
        ZIP_RC=$?
        if [ $ZIP_RC -eq 0 ]
        then
            echo "INFO: Drop database"
            rm -r $DATA_ROOT
            if [ $? -eq 0 ]
            then
              rm dropdb.flag
            fi
        fi
    fi

    if [ -f reboot.flag ]
    then
        rm reboot.flag
        echo REBOOT!!!
        sleep 10
    fi

    if [ -f poweroff.flag ]
    then
        rm poweroff.flag
        echo POWER OFF!!!
        exit
    fi

    qemu-arm  $PROG_ROOT/$FR $FR_PARMS
    FR_RC=$?
    echo "INFO: FR rc = $FR_RC"
    sleep 10
done
