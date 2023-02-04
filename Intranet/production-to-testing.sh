#!/bin/bash

set -xe

# Backup script that copies DB from production to testing.
# Author: Mikhail Veltishchev <mvel@yandex-team.ru>

date="`date +%Y.%m.%d`"
prodHost="lib01e.tools.yandex.net"
testHost="lib.test.tools.yandex-team.ru"
prodDump="/tmp/libra-production-$date.sql"
ssh="ssh -T"

$ssh $prodHost << EOF
    mysqldump -ulibra -pumerbdcb libra > $prodDump
EOF
scp $prodHost:$prodDump $prodDump
scp $prodDump $testHost:$prodDump
$ssh $testHost << EOF
    mysql -ulibra -pumerbdcb libra < $prodDump
EOF
$ssh $prodHost << EOF
    rm -f $prodDump
EOF
#rm -f $prodDump
$ssh $testHost << EOF
    rm -f $prodDump
EOF
