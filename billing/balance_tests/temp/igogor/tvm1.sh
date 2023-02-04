#!/usr/bin/env bash

# Test "mds-proxy" TVM app
CLIENT_ID=2000013
# get_secret(*TvmSecrets.BALANCE_TEST_SECRET) - секрет можно получить так
SECRET=''

base64url_decode() {
    sed "s,-,+,g; s,_,/,g" | openssl base64 -d
}
base64url_encode() {
    openssl base64 | sed "s,+,-,g; s,/,_,g"
}
TS=$(date +%s)
printf "curl -XPOST https://tvm-api.yandex.net/ticket -d client_id=%d -d grant_type=client_credentials -d ts=%d -d env_sign=%q\n" \
  "$CLIENT_ID" "$TS" "$(printf "%s" "${TS}${CLIENT_ID}" | openssl sha -sha256 -hmac "$(echo "$SECRET==" | base64url_decode)" -binary | base64url_encode)"

set -x

ENV_SIGN=$(printf "%s" "${TS}${CLIENT_ID}" | openssl sha -sha256 -hmac "$(echo "$SECRET==" | base64url_decode)" -binary | base64url_encode)

TVM_KEY = $(curl -XPOST https://tvm-api.yandex.net/ticket -d client_id=$CLIENT_ID -d grant_type=client_credentials -d ts=$TS -d env_sign=$ENV_SIGN)

curl -XGET https://balance.greed-tm.paysys.yandex.ru/documents/invoices/77279601

echo $TVM_KEY
curl -H 'X-Tvm: $TVM_KEY' -XGET https://balance.greed-tm.paysys.yandex.ru/documents/invoices/77279601

