#!/bin/sh

kafka-configs.sh --zookeeper zookeeper:2181 --alter --add-config 'SCRAM-SHA-256=[iterations=8192,password=test],SCRAM-SHA-512=[password=test]' --entity-type users --entity-name test