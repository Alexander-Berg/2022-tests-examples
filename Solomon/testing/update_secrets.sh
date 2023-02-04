#!/bin/sh -e

SECRETS_DIR="../../tools/secrets"
KEY="sec-01erybhveah0qkgnx1yq9027n2/testing"
SERVICES="
    alerting
    coremon
    dumper
    fetcher
    gateway
    stockpile
    project-manager
    name-resolver
    metabase
"

ya make ${SECRETS_DIR}

for service in ${SERVICES}; do
    ${SECRETS_DIR}/secrets get --config ${service}.conf \
        | ${SECRETS_DIR}/secrets encrypt --yav-key ${KEY} --in - --out ${service}.secrets
done
