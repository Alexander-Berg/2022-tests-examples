#!/bin/bash

DATE=$(date "+%Y-%m-%dT%H-%M-%S")
BRANCHNAME="dev/$DATE"

PULLS=$(curl -u ya-goodfella:1021cd9205943d2edc87824a461c1969c253aeba -s "https://api.github.com/repos/YandexClassifieds/auto/pulls?state=open&base=master" | grep -oP "\"number\": (\d+)," | grep -oP "\d+")

if [[ -z PULLS ]]
then
  echo "GitHub API reported empty list of open pull requests. Aborting"
  exit 1
fi

git fetch origin "+refs/pull/*/merge:refs/remotes/origin/pull/*/merge"

if [[ $? -ne 0 ]]
then
  echo "Failed to fetch changesfrom origin. Aborting"
  exit 1
fi

git checkout origin/master -b $BRANCHNAME

EMAILS=()

for pr in $PULLS
do
  EMAILS+=("$(git log --reverse --no-merges "--pretty=format:%aE" origin/pull/$pr/merge $(git merge-base master origin/pull/$pr/merge)..HEAD | head -n 1)")
done
EMAILS=($(printf "%s\n" "${EMAILS[@]}" | sort -u))

CHANGELOG=()
for pr in $PULLS
do
  CHANGELOG+=("$(git log --no-merges "--pretty=format:  * (PR #$pr) %s %b (%aN <%aE>)" origin/pull/$pr/merge $(git merge-base master origin/pull/$pr/merge)..HEAD | head -n 1)")
done

BRANCHES=()
for pr in $PULLS
do
  BRANCHES+=("origin/pull/$pr/merge")
done

git merge --squash ${BRANCHES[@]}

if [[ $? -ne 0 ]]
then
  echo -ne "Conflicting changes while merging PR ${PULLS[@]} to $BRANCHNAME, details follows\n\n$(git diff)" | mail -s "Conflict in testing!" ${EMAILS[@]}
  exit 1
else
  mvn --batch-mode versions:set -DnewVersion="1.0-${DATE}" -DartifactId="auto-micro-core" -f auto-micro-core/pom.xml && \
  mvn --batch-mode deploy -f auto-micro-core/pom.xml && \
  mvn --batch-mode versions:update-property -DallowDowngrade=true -Dproperty="yandex.auto-micro-core.version" -DnewVersion="1.0-${DATE}" -f pom.xml

  if [[ $? -ne 0 ]]
  then
    echo -ne "Look into teamcity build log for more info." | mail -s "Failed to build auto-micro-core for testing!" ${EMAILS[@]}
    exit 1
  fi

  MESSAGES=()
  for change in "${CHANGELOG[@]}"
  do
    MESSAGES+=("-m ${change}")
  done
  git commit -a "${MESSAGES[@]}"
  git push origin $BRANCHNAME
fi
