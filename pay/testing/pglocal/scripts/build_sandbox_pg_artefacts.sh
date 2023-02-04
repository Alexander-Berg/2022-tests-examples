#!/bin/bash

# Borrowed from https://github.com/zonkyio/embedded-postgres-binaries
# Since zonkyio/embedded-postgres-binaries doesn't have some essentials
# (bin/createdb and bin/pg_basebackup for master/slave replication),
# we build it with this script; it builds and uploads sandbox resources
# for Linux, Mac OS X and Windows.

set -ex

TEMP_DIR=$(mktemp -d)
DIST_DIR=$TEMP_DIR/mail-postgresql
PG_PARTMAN_DIR=$TEMP_DIR/pg-partman
PG_VERSION='12.3'

mkdir -p $DIST_DIR $PG_PARTMAN_DIR

# --------------- Linux -----------------

IMG_NAME='amd64/ubuntu:14.04'

# build postgres
docker run -i --rm -v ${DIST_DIR}:/usr/local/pg-dist \
-e PG_VERSION=$PG_VERSION \
-e POSTGIS_VERSION=$POSTGIS_VERSION \
-e ICU_ENABLED=$ICU_ENABLED \
-e PROJ_VERSION=6.0.0 \
-e PROJ_DATUMGRID_VERSION=1.8 \
-e GEOS_VERSION=3.7.2 \
-e GDAL_VERSION=2.4.1 \
$DOCKER_OPTS $IMG_NAME /bin/bash -ex -c 'echo "Starting building postgres binaries" \
    && apt-get update && apt-get install -y --no-install-recommends \
        ca-certificates \
        wget \
        bzip2 \
        xz-utils \
        gcc \
        g++ \
        make \
        pkg-config \
        libc-dev \
        libicu-dev \
        libossp-uuid-dev \
        libxml2-dev \
        libxslt1-dev \
        libssl-dev \
        libz-dev \
        libperl-dev \
        python3-dev \
        tcl-dev \
        git \
        \
    && wget -O patchelf.tar.gz "https://nixos.org/releases/patchelf/patchelf-0.9/patchelf-0.9.tar.gz" \
    && mkdir -p /usr/src/patchelf \
    && tar -xf patchelf.tar.gz -C /usr/src/patchelf --strip-components 1 \
    && cd /usr/src/patchelf \
    && wget -O config.guess "https://git.savannah.gnu.org/gitweb/?p=config.git;a=blob_plain;f=config.guess;hb=b8ee5f79949d1d40e8820a774d813660e1be52d3" \
    && wget -O config.sub "https://git.savannah.gnu.org/gitweb/?p=config.git;a=blob_plain;f=config.sub;hb=b8ee5f79949d1d40e8820a774d813660e1be52d3" \
    && ./configure --prefix=/usr/local \
    && make -j$(nproc) \
    && make install \
    \
    && wget -O postgresql.tar.bz2 "https://ftp.postgresql.org/pub/source/v$PG_VERSION/postgresql-$PG_VERSION.tar.bz2" \
    && mkdir -p /usr/src/postgresql \
    && tar -xf postgresql.tar.bz2 -C /usr/src/postgresql --strip-components 1 \
    && cd /usr/src/postgresql \
    && wget -O config/config.guess "https://git.savannah.gnu.org/gitweb/?p=config.git;a=blob_plain;f=config.guess;hb=b8ee5f79949d1d40e8820a774d813660e1be52d3" \
    && wget -O config/config.sub "https://git.savannah.gnu.org/gitweb/?p=config.git;a=blob_plain;f=config.sub;hb=b8ee5f79949d1d40e8820a774d813660e1be52d3" \
    && ./configure \
        CFLAGS="-O2 -DMAP_HUGETLB=0x40000" \
        PYTHON=/usr/bin/python3 \
        --prefix=/usr/local/pg-build \
        --enable-integer-datetimes \
        --enable-thread-safety \
        --with-ossp-uuid \
        $([ "$ICU_ENABLED" = true ] && echo "--with-icu") \
        --with-libxml \
        --with-libxslt \
        --with-openssl \
        --with-perl \
        --with-python \
        --with-tcl \
        --without-readline \
    && make -j$(nproc) world \
    && make install-world \
    && make -C contrib install \
    \
    && if [ -n "$POSTGIS_VERSION" ]; then \
      apt-get install -y --no-install-recommends curl libjson-c-dev libsqlite3-0 libsqlite3-dev sqlite3 unzip \
      && mkdir -p /usr/src/proj \
        && curl -sL "https://download.osgeo.org/proj/proj-$PROJ_VERSION.tar.gz" | tar -xzf - -C /usr/src/proj --strip-components 1 \
        && cd /usr/src/proj \
        && curl -sL "https://download.osgeo.org/proj/proj-datumgrid-$PROJ_DATUMGRID_VERSION.zip" > proj-datumgrid.zip \
        && unzip -o proj-datumgrid.zip -d data\
        && curl -sL "https://git.savannah.gnu.org/gitweb/?p=config.git;a=blob_plain;f=config.guess;hb=b8ee5f79949d1d40e8820a774d813660e1be52d3" > config.guess \
        && curl -sL "https://git.savannah.gnu.org/gitweb/?p=config.git;a=blob_plain;f=config.sub;hb=b8ee5f79949d1d40e8820a774d813660e1be52d3" > config.sub \
        && ./configure --disable-static --prefix=/usr/local/pg-build \
        && make -j$(nproc) \
        && make install \
      && mkdir -p /usr/src/geos \
        && curl -sL "https://download.osgeo.org/geos/geos-$GEOS_VERSION.tar.bz2" | tar -xjf - -C /usr/src/geos --strip-components 1 \
        && cd /usr/src/geos \
        && curl -sL "https://git.savannah.gnu.org/gitweb/?p=config.git;a=blob_plain;f=config.guess;hb=b8ee5f79949d1d40e8820a774d813660e1be52d3" > config.guess \
        && curl -sL "https://git.savannah.gnu.org/gitweb/?p=config.git;a=blob_plain;f=config.sub;hb=b8ee5f79949d1d40e8820a774d813660e1be52d3" > config.sub \
        && ./configure --disable-static --prefix=/usr/local/pg-build \
        && make -j$(nproc) \
        && make install \
      && mkdir -p /usr/src/gdal \
        && curl -sL "https://download.osgeo.org/gdal/$GDAL_VERSION/gdal-$GDAL_VERSION.tar.xz" | tar -xJf - -C /usr/src/gdal --strip-components 1 \
        && cd /usr/src/gdal \
        && curl -sL "https://git.savannah.gnu.org/gitweb/?p=config.git;a=blob_plain;f=config.guess;hb=b8ee5f79949d1d40e8820a774d813660e1be52d3" > config.guess \
        && curl -sL "https://git.savannah.gnu.org/gitweb/?p=config.git;a=blob_plain;f=config.sub;hb=b8ee5f79949d1d40e8820a774d813660e1be52d3" > config.sub \
        && ./configure --disable-static --prefix=/usr/local/pg-build \
        && make -j$(nproc) \
        && make install \
      && mkdir -p /usr/src/postgis \
        && curl -sL "https://postgis.net/stuff/postgis-$POSTGIS_VERSION.tar.gz" | tar -xzf - -C /usr/src/postgis --strip-components 1 \
        && cd /usr/src/postgis \
        && curl -sL "https://git.savannah.gnu.org/gitweb/?p=config.git;a=blob_plain;f=config.guess;hb=b8ee5f79949d1d40e8820a774d813660e1be52d3" > config.guess \
        && curl -sL "https://git.savannah.gnu.org/gitweb/?p=config.git;a=blob_plain;f=config.sub;hb=b8ee5f79949d1d40e8820a774d813660e1be52d3" > config.sub \
        && ./configure \
            --prefix=/usr/local/pg-build \
            --with-pgconfig=/usr/local/pg-build/bin/pg_config \
            --with-geosconfig=/usr/local/pg-build/bin/geos-config \
            --with-projdir=/usr/local/pg-build \
            --with-gdalconfig=/usr/local/pg-build/bin/gdal-config \
        && make -j$(nproc) \
        && make install \
    ; fi \
    \
    && cd /usr/local/pg-build \
    && cp /lib/*/libz.so.1 /lib/*/liblzma.so.5 /usr/lib/libossp-uuid.so.16 /usr/lib/*/libxml2.so.2 /usr/lib/*/libxslt.so.1 ./lib \
    && cp /lib/*/libssl.so.1.0.0 /lib/*/libcrypto.so.1.0.0 ./lib || cp /usr/lib/*/libssl.so.1.0.0 /usr/lib/*/libcrypto.so.1.0.0 ./lib \
    && if [ "$ICU_ENABLED" = true ]; then cp --no-dereference /usr/lib/*/libicudata.so* /usr/lib/*/libicuuc.so* /usr/lib/*/libicui18n.so* ./lib; fi \
    && if [ -n "$POSTGIS_VERSION" ]; then cp --no-dereference /lib/*/libjson-c.so* /usr/lib/*/libsqlite3.so* ./lib ; fi \
    && find ./bin -type f -print0 | xargs -0 -n1 patchelf --set-rpath "\$ORIGIN/../lib" \
    && find ./lib -maxdepth 1 -type f -name "*.so*" -print0 | xargs -0 -n1 patchelf --set-rpath "\$ORIGIN" \
    && find ./lib/postgresql -maxdepth 1 -type f -name "*.so*" -print0 | xargs -0 -n1 patchelf --set-rpath "\$ORIGIN/.." \
    \
    && PGPART_DIR=$(mktemp -d) \
    && pushd $PGPART_DIR \
    && git clone https://github.com/pgpartman/pg_partman.git \
    && pushd pg_partman \
    && make NO_BGW=1 \
    && popd && popd \
    && cp $PGPART_DIR/pg_partman/pg_partman.control ./share/postgresql/extension \
    && cp $PGPART_DIR/pg_partman/sql/pg_partman--*.sql ./share/postgresql/extension \
    && rm -rf $PGPART_DIR \
    \
    && tar -cJvf /usr/local/pg-dist/postgres-linux-debian.txz --hard-dereference \
        share/postgresql \
        lib \
        bin'

pushd $DIST_DIR
tar -xJf postgres-linux-debian.txz \
  && rm -f $_

# save pg_partman artefacts
cp ./share/postgresql/extension/pg_partman* $PG_PARTMAN_DIR

pushd .. > /dev/null
LINUX_OUT=$(echo 'Linux resource:' \
  && ya upload mail-postgresql --tar --ttl=inf --arch=linux --owner=MAIL \
    --description='Payments PostgreSQL 12 + pg_partman distribution for GNU/Linux' 2>&1 \
  && rm -rf mail-postgresql/*)

popd > /dev/null
popd

# ------- Mac OS X -------

FILE_NAME="postgresql-$PG_VERSION-1-osx-binaries.zip"
DIST_FILE=$DIST_DIR/$FILE_NAME
wget -O $DIST_FILE "https://get.enterprisedb.com/postgresql/$FILE_NAME"
unzip -q -d $DIST_DIR $DIST_FILE
rm -f $DIST_FILE
pushd $DIST_DIR
pushd pgsql
tar -czvf ../postgres-darwin-x86_64.tgz \
  share/postgresql \
  $([ -f lib/libiconv.2.dylib ] && echo lib/libiconv.2.dylib || echo lib/libicudata.*[^.][^.].dylib lib/libicui18n.*[^.][^.].dylib lib/libicuuc.*[^.][^.].dylib) \
  lib/libuuid.*.dylib \
  lib/libxml2.*.dylib \
  lib/libssl.*.dylib \
  lib/libcrypto.*.dylib \
  lib/postgresql/*.so \
  bin
popd

rm -rf ./pgsql
tar -xzf postgres-darwin-x86_64.tgz \
  && rm -f $_

# emberd pg_partman
cp $PG_PARTMAN_DIR/* $DIST_DIR/share/postgresql/extension

pushd .. > /dev/null
MACOSX_OUT=$(echo 'Mac OS X resource:' \
  && ya upload mail-postgresql --tar --ttl=inf --arch=osx --owner=MAIL \
    --description='Payments PostgreSQL 12 + pg_partman distribution for Mac OS X' 2>&1 \
  && rm -rf mail-postgresql/*)

popd > /dev/null
popd

# ------- Windows -------

FILE_NAME="postgresql-$PG_VERSION-1-windows-x64-binaries.zip"
DIST_FILE=$DIST_DIR/$FILE_NAME
wget -O $DIST_FILE "https://get.enterprisedb.com/postgresql/$FILE_NAME"
unzip -q -d $DIST_DIR $DIST_FILE
rm -f $DIST_FILE
pushd $DIST_DIR
pushd pgsql
tar -czvf ../postgres-windows-x86_64.tgz \
  share \
  lib/iconv.lib \
  lib/libxml2.lib \
  $([ -f lib/ssleay32.lib ] && echo lib/ssleay32.lib lib/ssleay32MD.lib || echo lib/libssl.lib lib/libcrypto.lib) \
  lib/*.dll \
  bin/*.exe \
  bin/*.dll
popd

rm -rf ./pgsql
tar -xzf postgres-windows-x86_64.tgz \
  && rm -f $_

# emberd pg_partman
cp $PG_PARTMAN_DIR/* $DIST_DIR/share/extension

pushd .. > /dev/null
WINDOWS_OUT=$(echo 'Windows resource:' \
  && ya upload mail-postgresql --tar --ttl=inf --arch=any --owner=MAIL \
    --description='Payments PostgreSQL 12 + pg_partman distribution for Windows' 2>&1 \
  && rm -rf mail-postgresql/*)

popd > /dev/null
popd

# --------------------------------
rm -rf $TEMP_DIR

echo $LINUX_OUT
echo $MACOSX_OUT
echo $WINDOWS_OUT
