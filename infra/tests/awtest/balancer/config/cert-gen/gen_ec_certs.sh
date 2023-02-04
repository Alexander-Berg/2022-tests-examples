#!/usr/bin/env bash
# ===
# Copied from https://a.yandex-team.ru/arc/trunk/arcadia/balancer/production/x/gen_certs as is
# ===
set -euox pipefail

NAME=${1?"Usage: $0 FQDN [enable_ocsp?1:0]"}
WITH_SAN=${2:-""}
SAN=${2:-"DNS:$NAME"}

CONFIG="$(dirname $0)/openssl.cnf"
HOST="localhost"
PORT=8123
KEY_LEN=48

SUBJ_FORMAT="/C=RU/ST=Russian Federation/L=/O=Yandex/OU=/CN=%s/emailAddress=$USER@yandex-team.ru"
ROOT_SUBJ=$(printf "$SUBJ_FORMAT" "")
OCSP_SUBJ=$(printf "$SUBJ_FORMAT" "localhost")

CA_DIR="./ca"
CERTS="$CA_DIR/certs"
NEW_CERTS="$CA_DIR/new_certs"
PRIVATE="$CA_DIR/private"

SERIAL="$CA_DIR/serial"
DATABASE="$CA_DIR/index.txt"

ROOT_CRT="$CA_DIR/root_ca.crt"
ROOT_KEY="$PRIVATE/root_ca.key"

OCSP_CSR="$CA_DIR/ocsp.csr"
OCSP_KEY="$CA_DIR/ocsp.key"
OCSP_CRT="$CA_DIR/ocsp.crt"

CERTS_DIR="./certs"

function gen_certs {
    local SUBJ=$(printf "$SUBJ_FORMAT" "$NAME")
    local CSR="$CERTS_DIR/$NAME.csr"
    local KEY="$CERTS_DIR/$NAME.key"
    local CRT="$CERTS_DIR/$NAME.crt"

    openssl ecparam -name secp256r1 -genkey -noout -out "$KEY"

    export SAN=$SAN
    if [ -z "$WITH_SAN" ]
    then
      openssl req -new -out "$CSR" -key "$KEY" -subj "$SUBJ" -config "$CONFIG"
      echo -e "y\ny\n" | openssl ca -in "$CSR" -out "$CRT" -config "$CONFIG"
    else
      openssl req -new -extensions san_env -out "$CSR" -key "$KEY" -subj "$SUBJ" -config "$CONFIG"
      echo -e "y\ny\n" | openssl ca -extensions san_env -in "$CSR" -out "$CRT" -config "$CONFIG"
    fi

    for (( i = 0; i < 3; i++ ))
    do
        openssl rand -out "$CERTS_DIR/${NAME}_ticket.$i.raw" "$KEY_LEN"
        openssl rand -base64 "$KEY_LEN" | awk '{print "-----BEGIN SESSION TICKET KEY-----"; print; print "-----END SESSION TICKET KEY-----"}' > "$CERTS_DIR/${NAME}_ticket.$i.pem"
    done
}

rm -rf "$CA_DIR"
rm -rf "$CERTS_DIR"

mkdir -p "$CA_DIR"
mkdir -p "$CERTS"
mkdir -p "$NEW_CERTS"
mkdir -p "$PRIVATE"
mkdir -p "$CERTS_DIR"

cp ./root/root_ca.crt $CA_DIR
cp ./root/root_ca.key $PRIVATE

echo "01" > "$SERIAL"
> "$DATABASE"

gen_certs

