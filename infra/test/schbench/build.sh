#!/bin/sh
test -d src || git clone git://git.kernel.org/pub/scm/linux/kernel/git/mason/schbench.git src
make -C src clean all CFLAGS="-Wall -O2 -g -W -Wextra -static"
rm -f schbench
cp src/schbench .
tar -czf schbench.tar.gz schbench
