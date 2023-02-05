#!/bin/bash

DATABASE=wiki_test_migration
export PGPASSWORD=mapadmin
HOST=wikimaps1.dev.yandex.net

#dropdb --host $HOST --username mapadmin $DATABASE
#createdb --host $HOST --username mapadmin -E utf-8 $DATABASE

psql --host canopus.maps --username mapadmin -d $DATABASE -f /usr/share/postgresql-8.3-postgis/lwpostgis.sql 
psql --host canopus.maps --username mapadmin -d $DATABASE -f /usr/share/postgresql-8.3-postgis/spatial_ref_sys.sql

upgrade-db init wikimaps -d $DATABASE -p .
upgrade-db check wikimaps -d $DATABASE -p .
upgrade-db upgrade wikimaps -d $DATABASE -p .

