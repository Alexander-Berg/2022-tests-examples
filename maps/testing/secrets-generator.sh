#!/bin/bash

printf "
apiVersion: v1
kind: Secret
metadata:
  name: auth-proxy-testing
  labels:
    app.kubernetes.io/name: auth-proxy
type: Opaque
data:
  APIKEY_TO_HTTP_SIGNATURE_KEY: $(aws secretsmanager get-secret-value --secret-id arn:aws:secretsmanager:us-east-2:582601430203:secret:solver/apikey_to_signature-H0uvTO | jq -r .SecretString | tr -d '\n' | base64 -w 0)
  AUTHORIZATION_APIKEYS_NOT_TO_CHECK: $(aws secretsmanager get-secret-value --secret-id arn:aws:secretsmanager:us-east-2:582601430203:secret:authorization/apikeys-not-to-check-XOQf8E | jq -r .SecretString | tr -d '\n' | base64 -w 0)
  CSRF_KEY: $(aws secretsmanager get-secret-value --secret-id arn:aws:secretsmanager:us-east-2:582601430203:secret:testing/authorization/csrf-key-c6iYnd | jq -r .SecretString | tr -d '\n' | base64 -w 0)
  COMPANY_ID_TO_APIKEY: $(aws secretsmanager get-secret-value --secret-id arn:aws:secretsmanager:us-east-2:582601430203:secret:testing/authorization/company-to-apikey-ld4ZpM | jq -r .SecretString | tr -d '\n' | base64 -w 0)
  KEYCLOAK_KEY_MAP: $(aws secretsmanager get-secret-value --secret-id arn:aws:secretsmanager:us-east-2:582601430203:secret:testing/keycloak/routeq-customers-public-key-map-RlENqp | jq -r .SecretString | base64 -w 0)
  PRIVATE_KEY: $(echo dummy_value | base64 -w 0)
  KEY_ID: $(echo dummy_value | base64 -w 0)
" > secrets.yaml
