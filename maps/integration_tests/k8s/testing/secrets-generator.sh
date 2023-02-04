#!/usr/bin/env sh

echo "
apiVersion: v1
kind: Secret
metadata:
  name: solver-integration-tests
  labels:
    app.kubernetes.io/name: solver-tests
type: Opaque
data:
  SOLVER_AUTH_TOKEN: $(aws secretsmanager get-secret-value --secret-id arn:aws:secretsmanager:us-east-2:582601430203:secret:testing/asyncsolver/auth-token-kWB6j3 | jq -r '.SecretString | fromjson | ."task_info_token"' | tr -d '\n' | base64)
" > secrets.yaml
