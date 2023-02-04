#!/usr/bin/env bash

set -Eeuo pipefail

TERRAFORM_STATE_STORAGE_YAV_SECRET_ID="sec-01g8dysxmmpfycvff3dwn750th"
YC_SA_PROFILE_NAME="preprod-yc.wall-e.main-sa-testing"
YCP_SA_PROFILE_NAME="preprod-yc.wall-e.main-sa-testing"
# Overrides. Just uncomment and run when needed.
# YCP_SA_PROFILE_NAME="preprod-fed-user"  # 'preprod-yc.wall-e.main-sa-testing' may not have all permissions needed.

# Manage CLI arguments.
USAGE="Usage: $(basename "$0") plan | apply"

TERRAFORM_COMMAND=("$@")

if [ "${#TERRAFORM_COMMAND[@]}" -eq 0 ];
then
    echo "${USAGE}"
    exit 1
fi

if [ "${TERRAFORM_COMMAND[0]%% *}" = "init" ];
then
    declare bucket
    declare endpoint
    declare access_key
    declare secret_key
    eval "$(yav get version ${TERRAFORM_STATE_STORAGE_YAV_SECRET_ID} -o | \
            jq -r 'to_entries | .[] | .key + "=" + (.value | @sh)')"
    terraform init -reconfigure \
        -backend-config="bucket=${bucket}" \
        -backend-config="endpoint=${endpoint}" \
        -backend-config="access_key=${access_key}" \
        -backend-config="secret_key=${secret_key}"
else
    yc config profile activate "${YC_SA_PROFILE_NAME}"
    TF_VAR_env_iam_token=$(yc iam create-token) \
    TF_VAR_env_ycp_sa_profile_name="${YCP_SA_PROFILE_NAME}" \
    terraform "${TERRAFORM_COMMAND[@]}"
fi
