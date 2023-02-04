#!/bin/sh -e

SECRETS="../../tools/secrets/secrets"

crt_encrypt() {
    local crt_id="$1" crt_name="$2" sec_id sec_data crt_data key_data

    sec_id=$(yav list secrets -q $crt_id --compact | awk 'FNR == 3 {print $1}')
    if [ -z "$sec_id" ] ; then
        echo "No secrets found for $crt_id!"
        exit 1
    fi
    echo "Using sec id '$sec_id'"

    sec_data="$(yav get version $sec_id -j)"
    crt_data="$(python -c 'D = '"$sec_data"'; print(D["value"]["'${crt_id}'_certificate"])')"
    key_data="$(python -c 'D = '"$sec_data"'; print(D["value"]["'${crt_id}'_private_key"])')"

    echo "$crt_data" | ${SECRETS} encrypt \
            --yav-key sec-01erybhveah0qkgnx1yq9027n2/testing \
            --in - \
            --out ssl.${crt_name}_cert

    echo "$key_data" | ${SECRETS} encrypt \
            --yav-key sec-01erybhveah0qkgnx1yq9027n2/testing \
            --in - \
            --out ssl.${crt_name}_key
}


../../../ya make $(dirname ${SECRETS})

crt_encrypt 7F001ADB87E540D2BEFA5159710002001ADB87 pm
