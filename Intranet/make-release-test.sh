#!/bin/bash

# this is a version of release.sh for "plain" projects - all modules are released at once with the same version

set -e
set -o pipefail

# checks whether current artifact with given version is already deployed to artifactory
# param: base url, version
# requires: pom.xml in current directory
# returns: 0 - found, 1 - not found; exits with code 1 if error occurs
check_artifact () {
  BASE_URL=$1
  ARTIFACT_VERSION=$2
  ARTIFACT_ID=`xpath -q -e '/project/artifactId/text()' pom.xml`
  ARTIFACT_POM_URL="$BASE_URL/$GROUP_PATH/$ARTIFACT_ID/$ARTIFACT_VERSION/$ARTIFACT_ID-$ARTIFACT_VERSION.pom"
  echo -n "resolving artifact: $ARTIFACT_POM_URL ... "
  STATUS_CODE=`curl -Is $ARTIFACT_POM_URL | head -1 | cut -d ' ' -f 2`
  case $STATUS_CODE in
    "200" )
      echo success
      return 0
    ;;
    "404" )
      echo not found
      return 1
    ;;
    * )
      echo "Unexpected status code connecting to $ARTIFACT_POM_URL : $STATUS_CODE"
      exit 1
esac
}

# determine layout
LAYOUT_FROM_PLUGIN=`xpath -q -e '/project/build/pluginManagement/plugins/plugin/configuration/layout/text()' pom.xml | head -1`
LAYOUT_FROM_PROPERTY=`xpath -q -e '/project/properties/layout/text()' pom.xml | head -1`
EXPLICIT_LAYOUT=${LAYOUT_FROM_PROPERTY:-$LAYOUT_FROM_PLUGIN}
LAYOUT=${EXPLICIT_LAYOUT:-standard}

#determine versionMajor
MAJOR_FROM_PLUGIN=`xpath -q -e '/project/build/pluginManagement/plugins/plugin/configuration/versionMajor/text()' pom.xml | head -1`
MAJOR_FROM_PROPERTY=`xpath -q -e '/project/properties/versionMajor/text()' pom.xml | head -1`
EXPLICIT_MAJOR=${MAJOR_FROM_PROPERTY:-$MAJOR_FROM_PLUGIN}
MAJOR_FROM_SNAPSHOT_VERSION=`xpath -q -e '/project/version/text()' pom.xml | head -1 | sed 's/-SNAPSHOT$//'`
VERSION_MAJOR=${EXPLICIT_MAJOR:-$MAJOR_FROM_SNAPSHOT_VERSION}

CURRENT_BRANCH=`git branch | grep -F '*' | sed 's/^\* *//'`
BRANCH_SUFFIX="`basename $CURRENT_BRANCH`"

echo starting build for layout: $LAYOUT, versionMajor:$VERSION_MAJOR, branch:$CURRENT_BRANCH

SETTINGS_XML=`pwd`/settings.xml

if [ -z $MAVEN_COMMAND ] ; then
  ACTION="deploy -s $SETTINGS_XML"
else
  ACTION="$MAVEN_COMMAND -s $SETTINGS_XML"
fi

LAYOUT_BASE=${LAYOUT%-debian}
NO_DEBIAN=${LAYOUT/*-debian/}
echo LAYOUT_BASE: $LAYOUT_BASE
echo NO_DEBIAN: $NO_DEBIAN


rm -f released.txt tags.txt
find . -name release.marker -delete

URL_BASE=`xpath -q -e '/project/repositories/repository/url/text()' pom.xml | head -1 | sed 's/\/$//'`
GROUP_PATH=`xpath -q -e '/project/groupId/text()' pom.xml | sed 's/\./\//g'`
GROUP_ID=`xpath -q -e '/project/groupId/text()' pom.xml`

rm -f released.txt tags.txt

PARENT_VERSION="$BRANCH_SUFFIX"

# parent
rm -f release.marker
echo checking parent $URL_BASE $PARENT_VERSION
check_artifact $URL_BASE $PARENT_VERSION || touch release.marker

MODULES=`xpath -q -e '/project/modules/module/text()' pom.xml`

for module in $MODULES ; do
  pushd $module
  rm -f release.marker module.version
  MODULE_VERSION_DEPENDENCY=`xpath -q -e '/project/properties/versioningParent/text()' pom.xml`
  if [ -z $MODULE_VERSION_DEPENDENCY ] ; then
    MODULE_MAJOR=$VERSION_MAJOR
  else
    if [ -f $MODULE_VERSION_DEPENDENCY/module.version ] ; then
      MODULE_MAJOR=`cat $MODULE_VERSION_DEPENDENCY/module.version`
    else
      echo "$MODULE_VERSION_DEPENDENCY should precede $module: it is its MODULE_VERSION_DEPENDENCY"
      exit 1;
    fi
  fi
  MODULE_VERSION=$MODULE_MAJOR
  echo $MODULE_VERSION > module.version
  check_artifact $URL_BASE $MODULE_VERSION || touch release.marker
  popd
done


mvn versions:set -DnewVersion="$PARENT_VERSION" -DupdateMatchingVersions=false

echo parent version set to \"$PARENT_VERSION\"

for module in $MODULES ; do
  pushd $module
  mvn versions:set -DnewVersion=`cat module.version`
  popd
done

# build the whole project to ensure that depencencies are built first
mvn install -DskipTests -DskipGitLog=true

echo "<html><body>" >release.tmp
echo "<h3>Release `xpath -q -e '/project/groupId/text()' pom.xml` at `date '+%F %R'`</h3>" >>release.tmp

  ARTIFACT_ID=`xpath -q -e '/project/artifactId/text()' pom.xml`
  if [ -f release.marker ] ; then
    echo "building parent"
    mvn  $ACTION --non-recursive -DskipGitLog=$SKIP_GITLOG
    echo "<DIV style=\"color:green\">" >>release.tmp
    echo "$ARTIFACT_ID-$PARENT_VERSION released <a href=\"$URL_BASE/$GROUP_PATH/$ARTIFACT_ID/$PARENT_VERSION/$ARTIFACT_ID-$PARENT_VERSION-gitlog.txt\">git log</a>" >>release.tmp
    echo "</DIV>" >>release.tmp
    echo "$ARTIFACT_ID-$PARENT_VERSION released" >> released.txt
    echo "$ARTIFACT_ID-$PARENT_VERSION" >> tags.txt
  else
    echo "skipping parent - no changes"
    echo "$ARTIFACT_ID-$PARENT_VERSION not released - no changes" >> released.txt
    echo "<DIV style=\"color:black\">" >>release.tmp
    echo "$ARTIFACT_ID-$PARENT_VERSION not released - no changes" >>release.tmp
    echo "</DIV>" >>release.tmp
  fi

for component in $MODULES ; do
  pushd $component
  if [ -f release.marker ] ; then
    echo "building $component"
    mvn $ACTION -DskipGitLog=$SKIP_GITLOG -DdeployAtEnd=true
    MODULE_VERSION=`cat module.version`
    MODULE_ARTIFACT=`xpath -q -e '/project/artifactId/text()' pom.xml`

    echo "<DIV style=\"color:green\">" >>../release.tmp
    echo "$component $MODULE_VERSION released <a href=\"$URL_BASE/$GROUP_PATH/$MODULE_ARTIFACT/$MODULE_VERSION/$MODULE_ARTIFACT-$MODULE_VERSION-gitlog.txt\">git log</a>" >>../release.tmp
    echo "$component $MODULE_VERSION: see $URL_BASE/$GROUP_PATH/$MODULE_ARTIFACT/$MODULE_VERSION/$MODULE_ARTIFACT-$MODULE_VERSION-gitlog.txt" >> ../released.txt
    echo "<ul>" >> ../release.tmp
    for artifact_path in `find . -name "*$MODULE_VERSION.war" | grep -vF WEB-INF` ; do
      artifact=`basename $artifact_path`
      echo "<li>$artifact</li>" >> ../release.tmp
      echo "    $artifact" >> ../released.txt
    done
    for artifact_path in `find . -name "*$MODULE_VERSION.jar" | grep -vF WEB-INF` ; do
      artifact=`basename $artifact_path`
      echo "<li>$artifact</li>" >> ../release.tmp
      echo "    $artifact" >> ../released.txt
    done
    for dockerprops in `find . -name "docker.properties"` ; do
      echo "<p>Docker deployment: </p>" >> ../release.tmp
      echo "<pre>" >> ../release.tmp
      cat $dockerprops >> ../release.tmp
      echo "</pre>" >> ../release.tmp
    done
    echo "</ul>" >>../release.tmp
    echo "</DIV>" >>../release.tmp
    echo "$MODULE_ARTIFACT-$MODULE_VERSION" >>../tags.txt
  else
    echo "skipping $component - no changes"
    MODULE_VERSION=`cat module.version`
    echo "<DIV style=\"color:black\">" >>../release.tmp
    echo "$component $MODULE_VERSION not released - no changes" >>../release.tmp
    echo "</DIV>" >>../release.tmp
    echo "$component $MODULE_VERSION not released - previous version kept " >> ../released.txt
  fi
 popd
done

# build

PROJECT_ROOT=$(pwd)
DOCKER_FILE=$PROJECT_ROOT/docker/Dockerfile
TARGET_DIR=$PROJECT_ROOT/server/standalone/target
DOCKER_IMAGE_NAME="registry.yandex.net/dispenser/dispenser-standalone-test:$PARENT_VERSION"

cp "$DOCKER_FILE" "$TARGET_DIR/Dockerfile"
sed -i "s/##RUNNABLE_JAR##/dispenser-standalone-$VERSION_MAJOR-runnable.jar/g" "$TARGET_DIR/Dockerfile"

docker build -t "$DOCKER_IMAGE_NAME" "$TARGET_DIR/"

# deploy

docker push "$DOCKER_IMAGE_NAME"


echo "</body></html>" >>release.tmp
mv release.tmp release.html

if [ -z "$NO_DEBIAN" ] ; then
  pushd deploy
  ant deb:release
  popd
fi

git reset --hard

find . -name '*.versionsBackup' -delete -o -name 'released.module' -delete

echo BUILD SUCCESSFUL
