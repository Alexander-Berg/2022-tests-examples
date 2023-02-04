locals {
    yc_token = data.external.yc_client_token_generator.result.token
}

terraform {
    required_providers {
        ycp = {
            source = "terraform.storage.cloud-preprod.yandex.net/yandex-cloud/ycp"
        }
        yandex = {
            source = "yandex-cloud/yandex"
        }
    }
}

provider "yandex" {
    endpoint = var.yc_endpoint
    cloud_id = var.cloud_id
    folder_id = var.folder_id
    token = local.yc_token
    storage_endpoint = var.yc_storage_endpoint
}

provider "ycp" {
    cloud_id = var.cloud_id
    folder_id = var.folder_id
    token = local.yc_token
    ycp_profile = var.env
    prod = true
}

data "external" "yc_client_token_generator" {
    program = ["bash", "${path.module}/files/yc-token.sh", var.env]
    query = {}
}
