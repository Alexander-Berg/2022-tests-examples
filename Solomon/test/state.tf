terraform {
    // OAUTH="XXX" # from https://oauth.yandex-team.ru/authorize?response_type=token&client_id=6797456f343042aabba07f49b478c49b
    // curl -X POST -H "Authorization: OAuth $OAUTH" "https://s3-idm.mds.yandex.net/credentials/create-access-key" --data "service_id=3047"
    backend "s3" {
        endpoint = "s3.mds.yandex.net"
        bucket = "yc-solomon"
        key = "terraform/preprod/alerting.tfstate"
        region = "us-east-1"
        access_key = "KEYX"
        secret_key = "SECRET"
        skip_credentials_validation = true
        skip_metadata_api_check = true
    }
}
