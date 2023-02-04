terraform {
    required_providers {
        yandex = {
            source = "yandex-cloud/yandex"
        }
    }
    required_version = ">= 0.13"
}

provider "yandex" {
    endpoint = "gw.db.yandex-team.ru:443"
    token = var.yc_token
    cloud_id = "akua8r7qsqp91h88ag8o"
    folder_id = "akuavd4h1op08t039hgi"
}

module "yav-secret-getter" {
    source = "../../../modules/yav-secret-getter"

    yav_oauth = var.yav_token
    sec_id = "sec-01fhb0ntqvfq8czaggtdnm75c0"
    sec_name = "secret"
}


module "pg" {
    source = "../../../modules/mdb/pg"

    name = var.name
    env = var.env
    pgversion = var.pgversion
    description = var.description
    resource_preset = var.resource_preset
    disk_size = var.disk_size
    dbname = var.dbname
    dbowner = var.dbowner
    username = var.username
    pgconfig = var.pgconfig
    hosts = var.hosts
    password = module.yav-secret-getter.secret
}
