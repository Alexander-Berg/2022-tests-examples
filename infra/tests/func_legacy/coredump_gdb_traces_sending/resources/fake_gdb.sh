#!/bin/sh

cat << EOF
Program terminated with signal SIGSEGV

Thread 1 (Thread 0x0)
#0  0x0 in pthread_join () from /build/buildd/eglibc-2.15/nptl/pthread_join.c:89

EOF
