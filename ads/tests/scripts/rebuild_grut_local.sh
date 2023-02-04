#!/usr/bin/env bash

set -e

if [[ "$OSTYPE" != "linux-gnu"* && "$OSTYPE" != "darwin"* ]]; then
    echo "Script should be run on linux or mac machine"
    exit 1
fi

BUILD_ARGS=
if [[ "$OSTYPE" == "darwin"* ]]; then
    # may be it would suitable for another os, but it was not tested
    BUILD_ARGS=" --target-platform linux --dist -E"
fi

ARCADIA_ROOT="${PWD}"
while [[ ! -e "${ARCADIA_ROOT}/.arcadia.root" ]]; do
    if [[ "${ARCADIA_ROOT}" == "/" ]]; then
        echo "$0: must be run from inside Arcadia checkout" >&2
        exit 1
    fi
    ARCADIA_ROOT="$(dirname "${ARCADIA_ROOT}")"
done

if [ -e $ARCADIA_ROOT/.svn ]; then
    vcs="svn"
    vcs_tool="$ARCADIA_ROOT/ya tool svn"
elif [ -e $ARCADIA_ROOT/.arc ]; then
    vcs="arc"
    vcs_tool="arc"
else
    echo "ARCADIA ROOT: $ARCADIA_ROOT is not under svn or arc vcs"
    exit 1
fi

echo "Build grut-admin..."
( cd "${ARCADIA_ROOT}/grut/tools/admin";
  ya make -DSTRIP $BUILD_ARGS
  true
  )

echo "Build orm..."
( cd "${ARCADIA_ROOT}/grut/bin/orm";
  ya make -DSTRIP $BUILD_ARGS
  true
  )

echo "Build object_api..."
( cd "${ARCADIA_ROOT}/grut/bin/object_api";
  ya make -DSTRIP $BUILD_ARGS
  true
  )

echo "Build docker image..."

cp "${ARCADIA_ROOT}/grut/tools/admin/grut-admin" "${ARCADIA_ROOT}/ads/bsyeti/caesar/tests/grut-docker/"
cp "${ARCADIA_ROOT}/grut/bin/orm/grut-orm" "${ARCADIA_ROOT}/ads/bsyeti/caesar/tests/grut-docker/"
cp "${ARCADIA_ROOT}/grut/bin/object_api/object_api" "${ARCADIA_ROOT}/ads/bsyeti/caesar/tests/grut-docker/"

cp -r "${ARCADIA_ROOT}/direct/qa/docker-files/grut-docker/grut_object_api" "${ARCADIA_ROOT}/ads/bsyeti/caesar/tests/grut-docker/"
cp -r "${ARCADIA_ROOT}/direct/qa/docker-files/grut-docker/grut_orm" "${ARCADIA_ROOT}/ads/bsyeti/caesar/tests/grut-docker/"
cp -r "${ARCADIA_ROOT}/direct/qa/docker-files/grut-docker/supervisord" "${ARCADIA_ROOT}/ads/bsyeti/caesar/tests/grut-docker/"
cp "${ARCADIA_ROOT}/direct/qa/docker-files/grut-docker/init_db.sh" "${ARCADIA_ROOT}/ads/bsyeti/caesar/tests/grut-docker/"
cp "${ARCADIA_ROOT}/direct/qa/docker-files/grut-docker/start_yt_local.sh" "${ARCADIA_ROOT}/ads/bsyeti/caesar/tests/grut-docker/"

random_tag=$(cat /dev/urandom | LC_CTYPE=C tr -dc 'a-z0-9' | fold -w 10 | head -n 1)
image="registry.yandex.net/sestepanov/grut_local:${random_tag}"
image_txt="ads/bsyeti/caesar/tests/grut-docker/docker_files/docker-compose.yml"

( cd ${ARCADIA_ROOT}/ads/bsyeti/caesar/tests/grut-docker/;
  # using host network to avoid troubles with ipv6 connectivity from container when apt-get working
  docker build --network host -t "${image}" .
  true
  )

echo "Upload new image..."
docker push "${image}"

sha256=$(docker inspect --format='{{index .RepoDigests 0}}' registry.yandex.net/sestepanov/grut_local:${random_tag})

echo "Save new image tag and sha256 to docker-compose.yml ${image_txt}..."
sed -i "11s#.*#    image: \"${sha256}\"#" ${ARCADIA_ROOT}/${image_txt}

echo "Update file status in $vcs..."

if [ "$vcs" = "svn" ]; then
    ( cd "$ARCADIA_ROOT";
        $vcs_tool st $image_txt >$TMP/status
        cat $TMP/status | awk '/^\?/ {print $2}' > $TMP/added
        cat $TMP/status | awk '/^!/ {print $2}' > $TMP/deleted
        test -s $TMP/added && cat $TMP/added | xargs $vcs_tool add
        test -s $TMP/deleted && cat $TMP/deleted | xargs $vcs_tool remove
        true
        )
elif [ "$vcs" = "arc" ]; then
    ( cd "$ARCADIA_ROOT";
        $vcs_tool add --all $image_txt
        true
        )
else
    echo "wrong vcs"
    clear
    exit 1
fi

rm "${ARCADIA_ROOT}/ads/bsyeti/caesar/tests/grut-docker/grut-admin"
rm "${ARCADIA_ROOT}/ads/bsyeti/caesar/tests/grut-docker/grut-orm"
rm "${ARCADIA_ROOT}/ads/bsyeti/caesar/tests/grut-docker/object_api"

rm -r "${ARCADIA_ROOT}/ads/bsyeti/caesar/tests/grut-docker/grut_object_api"
rm -r "${ARCADIA_ROOT}/ads/bsyeti/caesar/tests/grut-docker/grut_orm"
rm -r "${ARCADIA_ROOT}/ads/bsyeti/caesar/tests/grut-docker/supervisord"
rm -r "${ARCADIA_ROOT}/ads/bsyeti/caesar/tests/grut-docker/init_db.sh"
rm -r "${ARCADIA_ROOT}/ads/bsyeti/caesar/tests/grut-docker/start_yt_local.sh"
