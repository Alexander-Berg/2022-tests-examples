#/bin/bash

SOCKET=genfiles/testapp.sock
LOGFILE=genfiles/testapp.log
YCR_MODE=fastcgi:$SOCKET

rm -f $LOGFILE $SOCKET

env LD_LIBRARY_PATH=`realpath .` YCR_MODE=$YCR_MODE \
./testapp/yacare-testapp </dev/null > "$LOGFILE" 2>&1 & apppid=$!
echo Testapp is running with pid $apppid
trap 'kill -9 $apppid' EXIT

while ! /bin/nc.openbsd -q 1 -U "$SOCKET" >/dev/null 2>&1 </dev/null; do sleep 1; done
echo Testapp is listening on "$SOCKET"

cpu_count=`grep -o processor /proc/cpuinfo | wc -l`
for i in `seq $cpu_count`; do
    cd genfiles;
    ../tests/fcgi_test & pids="$pids $!";
    cd ..;
done
FAILED=
echo 
for pid in "$pids"; do wait $pid || FAILED=y; done

if grep -q "protocol violation" "$LOGFILE"; then
    echo "fcgi-test: race in fastcgi frontend detected" >&2
    FAILED=y
fi

[ -z $FAILED ]
echo -e "$cpu_count test(s) executed"
