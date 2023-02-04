#!/bin/sh
test -d src || git clone https://github.com/scheduler-tools/rt-app.git src
cd src;
./autogen.sh
./configure --enable-static
cd ..
make -C src clean all CFLAGS="-Wall -O2 -g -W -Wextra -static"
rm -f rt-app
cp src/src/rt-app .
 tar -czf rt-app.tar.gz rt-app
