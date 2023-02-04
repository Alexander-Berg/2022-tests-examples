#!/bin/bash
 
set -e

echo 'Hello world from vm  $(hostname)'
echo "hello world from vmexec" > sandbox-result.txt
pwd
mount

/usr/bin/realpath  sandbox-result.txt
