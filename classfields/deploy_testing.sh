#!/bin/sh

BRANCH=`git rev-parse --abbrev-ref HEAD`

git pull
git push -u origin ${BRANCH}
git checkout testing
git pull
git merge --no-ff ${BRANCH}
git push -u origin testing
git checkout ${BRANCH}
