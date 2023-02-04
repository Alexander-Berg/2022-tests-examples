#!/bin/bash -x

# USAGE: ./bench.sh iva1-5840.search.yandex.net
# apt-get install gnuplot for graphs
# output will be in $HOST/ directory

HOST=$1

: ${TEST:="simple"}

TESTFILE="./"$TEST".json"
rm -rv $HOST
mkdir $HOST

REMOTE=`ssh $HOST mktemp -d`
scp ./rt-app $HOST:$REMOTE
scp $TESTFILE $HOST:$REMOTE

ssh $HOST 'sudo apt-get install libjson-c2'
ssh $HOST 'sudo portoctl destroy bbb'

# We need one 'clean' core for workload latency calibration
ssh $HOST 'sudo portoctl run bbb cpu_limit=16c cpu_policy=idle command="stress -c 16" cpu_set="1-15,17-31"'

sleep 5

ssh $HOST "cd $REMOTE; sudo portoctl exec aaa cpu_limit=16c cpu_guarantee=16c cwd=$REMOTE command='./rt-app $TESTFILE'"
ssh $HOST 'sudo portoctl destroy bbb'

scp $HOST:$REMOTE/*.log $HOST/
scp $HOST:$REMOTE/*.plot $HOST/

cd $HOST
gnuplot -p $TEST-load-0.plot 

ssh $HOST rm -rv $REMOTE
