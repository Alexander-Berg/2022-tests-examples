#!/bin/bash
CHANGELOGS=`find . -type f | grep 'debian/changelog$'` 
for changelog in $CHANGELOGS; do 
  PACKAGE_AND_VERSION=`head -n 1 $changelog | cut -d ' ' -f 1,2`
  PACKAGE=`echo $PACKAGE_AND_VERSION | cut -d ' ' -f 1`
  VERSION=`echo $PACKAGE_AND_VERSION | cut -d ' ' -f 2 | sed 's/[()]//g'`
  CONDUCTOR_VERSION=`curl -s "https://c.yandex-team.ru/api/package_version/$PACKAGE?format=xml&branch=testing" | grep '<version>' | sed 's/<[^>]*>//g' | sed 's/^ *//g'`
  if [ "$CONDUCTOR_VERSION" = "" ]; then
    STATUS="not_in_testing"
  else
    if [ "$CONDUCTOR_VERSION" = "$VERSION" ]; then
      STATUS="match"
    else
      STATUS="differ"
    fi
  fi
  echo $PACKAGE_AND_VERSION $CONDUCTOR_VERSION $STATUS
done
