#!/usr/bin/env bash

#yarn run flbt
./generate.sh -p=testopithecus -P=packages/testopithecus -p -r
rm -rf ././../../android/mail-app/x-mail-android-testopithecus/testopithecus
cp -Rf ./android/Sources/testopithecus/. ././../../android/mail-app/x-mail-android-testopithecus/testopithecus
