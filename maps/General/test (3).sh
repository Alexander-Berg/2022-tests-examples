#!/bin/sh -e



conn_string="host=sas-yx9d8tjc4m0f8qod.db.yandex.net port=6432 sslmode=verify-full \
target_session_attrs=read-write \
dbname=garden user=gardener \
password=$(yav get version sec-01d48bewfgw7vd3dv6dc4hmx70 -o mdb_garden_password_testing)"

schema_name=$(echo " \
    select schema_name from information_schema.schemata \
    where schema_name  not like '%_temp' and schema_name like '%cis1%' \
    order by schema_name desc \
    limit 1;" | psql "$conn_string" -t -A)

./cli "$schema_name" "$conn_string"
