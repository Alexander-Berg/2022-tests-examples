#!/bin/dash
export SERVANT_DIR="`dirname $0`"
MODE="start"
if [ "$1" = "stop" ] ; then
    MODE="stop"
fi

echo "Stopping..."
export PYTHONPATH=../cluster_tools:$PYTHONPATH
"balance-servant-ctl" stop balance-test-xmlrpc -user &&
echo OK || echo FAIL
if [ "$MODE" != "stop" ] ; then
    echo "Starting..."
    setsid "balance-servant-ctl" start balance-test-xmlrpc -user &
    "balance-servant-ctl" checkstart balance-test-xmlrpc -user &&
    echo OK || echo FAIL
fi
