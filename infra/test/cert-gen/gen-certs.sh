#!/usr/bin/env bash
# ===
# Copied from https://a.yandex-team.ru/arc/trunk/arcadia/balancer/production/x/gen_certs as is
# ===
set -euox pipefail

NAME=${1?"Usage: $0 FQDN [enable_ocsp?1:0]"}
with_ocsp=${2:-0}

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
    openssl req -new -nodes -out "$CSR" -keyout "$KEY" -subj "$SUBJ" -config "$CONFIG"
    echo -e "y\ny\n" | openssl ca -in "$CSR" -out "$CRT" -config "$CONFIG"
    rm "$CSR"

    if [[ "$with_ocsp" -eq 1 ]]; then
        openssl ocsp -CAfile "$ROOT_CRT" -issuer "$ROOT_CRT" -cert "$CRT" -url http://"$HOST":"$PORT" -respout "$CERTS_DIR/${NAME}_ocsp.0.der"
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

echo "01" > "$SERIAL"
> "$DATABASE"
openssl req -x509 -new -nodes -config openssl.cnf -out "$ROOT_CRT" -keyout "$ROOT_KEY" -subj "$ROOT_SUBJ" -config "$CONFIG" -days 365242

if [[ "$with_ocsp" -eq 1 ]]; then
    openssl req -new -nodes -out "$OCSP_CSR" -keyout "$OCSP_KEY" -extensions v3_OCSP -subj "$OCSP_SUBJ" -config "$CONFIG"
    echo -e "y\ny\n" | openssl ca -in "$OCSP_CSR" -out "$OCSP_CRT" -extensions v3_OCSP -config "$CONFIG"
    rm "$OCSP_CSR"
    openssl ocsp -index "$DATABASE" -port "$PORT" -rsigner "$OCSP_CRT" -rkey "$OCSP_KEY" -CA "$ROOT_CRT" &
    PID=$!
    sleep 2
fi


gen_certs

cp "$ROOT_CRT" "$CERTS_DIR"

if [[ "$with_ocsp" -eq 1 ]]; then
    kill "$PID"
fi
