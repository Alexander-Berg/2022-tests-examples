#!/bin/bash

set -e

KS_WITH_DATA="subscriptions_billing hydra_profile realty_offers auto_moto"
CLUSTER=$(hostname | awk -F'-' '{print $2"-"$3}')

mkdir -p /tmp/cassandra-backup-s3-test/ && cd /tmp/cassandra-backup-s3-test/

KEYSPACES=`nodetool -h ::1 cfstats | grep "Keyspace :" | egrep -v "system|system_schema|system_distributed|system_auth" | awk '{print $3}'`

for KEYSPACE in $KEYSPACES
do
    echo "Backup schema for ks $KEYSPACE"
    SCHEMA="schema-$KEYSPACE.cdl"
    cqlsh `hostname` --user cassandra --password cassandra -k $KEYSPACE -e "desc keyspace $KEYSPACE;" | tail -n +2 > "$SCHEMA"
    if echo $KS_WITH_DATA | grep -qw $KEYSPACE; then
      echo "Backup schema with data for ks $KEYSPACE"
      bash /usr/sbin/getSnapshot -k $KEYSPACE --no-timestamp || true
    fi
done

nodetool clearsnapshot || true
rm -rf /tmp/cassandra-backup-s3-test/getSnapshot* || true

/usr/bin/aws --profile=vertis-backups --endpoint-url=http://s3.mds.yandex.net s3 cp --recursive /tmp/cassandra-backup-s3-test/ s3://vertis-backups/testing/cassandra/${CLUSTER}/

rm -rf /tmp/cassandra-backup-s3-test/
