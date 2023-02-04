#! /bin/bash
nproc=`nproc`
njobs=$(( ($nproc +1) /2))
#export XFSTEST_KERNEL=bzImage
#export XFSTEST_LIST=generic/001

cat groups.txt | xjobs -j $njobs ./run-kvm-xfstests-single-group.sh
