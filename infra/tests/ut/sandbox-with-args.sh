#!/bin/bash
 
set -e

echo "{$@}"
echo 'Hello world from vm  $(hostname)'
echo "hello world from vmexec reverse args: '$3 $2 $1'" > sandbox-result2.txt
