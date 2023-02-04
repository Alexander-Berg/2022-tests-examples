#!/bin/sh

set -e

echo deleting local master
git branch -D master || true
echo git remote set-branches --add origin master
git remote set-branches --add origin master
echo git fetch
git fetch
echo git checkout master
git checkout master

echo merging tested
if [ -z "$RELEASE_NOTES" ] ; then
  echo no release notes
  git merge tested
else
  echo release notes: "$RELEASE_NOTES"
  git merge --no-ff -m "$RELEASE_NOTES"  tested
fi