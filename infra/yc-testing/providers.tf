terraform {
  required_providers {
    yandex = {
      source = "terraform-registry.storage.yandexcloud.net/yandex-cloud/yandex"
    }
    ycp = {
      source = "terraform.storage.cloud-preprod.yandex.net/yandex-cloud/ycp"
    }
  }
  required_version = ">= 0.13"

  backend "s3" {
    region                      = "ru-central1"
    key                         = "terraform.tfstate"
    skip_region_validation      = true
    skip_credentials_validation = true
  }
}

provider "yandex" {
  cloud_id         = var.cloud.cloud_id
  folder_id        = var.cloud.folder_id
  endpoint         = var.provider_yandex.endpoint
  token            = var.env_iam_token
  storage_endpoint = var.provider_yandex.storage_endpoint
}

provider "ycp" {
  alias       = "ycp"
  cloud_id    = var.cloud.cloud_id
  folder_id   = var.cloud.folder_id
  ycp_profile = var.env_ycp_sa_profile_name
  token       = var.env_iam_token
}
