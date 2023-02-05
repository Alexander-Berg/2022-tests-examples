#!/bin/sh
RESULT=$(xsltproc --xinclude $1 $2)
echo $RESULT
exit $(echo $RESULT | grep -c error)
