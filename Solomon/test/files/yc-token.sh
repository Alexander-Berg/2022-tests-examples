#!/usr/bin/env bash
set -eu

ENV="$1"
TOKEN=$(yc iam create-token) &>/dev/null
jq -n \
	--arg token "$TOKEN" \
	'{"token":$token}'
