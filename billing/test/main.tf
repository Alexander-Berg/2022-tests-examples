terraform {
    required_providers {
        yandex = {
            source = "yandex-cloud/yandex"
            version = "~> 0.61.0"
        }
    }
    required_version = ">= 0.13"
}

provider "yandex" {
    endpoint = "gw.db.yandex-team.ru:443"
    token = var.yc_token
    cloud_id = "akua8r7qsqp91h88ag8o"
    folder_id = "aku3e2id7e12ka1petnv"
}

module "yav-secret-getter" {
    source = "../../../modules/yav-secret-getter"

    yav_oauth = var.yav_token
    sec_id = "sec-01esgbpv1d80kqajc3bmxk8ts1"
    sec_name = "secret"
}


module "pg" {
    source = "../../../modules/mdb/pg"

    count = var.cnt
    name = join("", [
        var.name,
        format("%02d", count.index + 1)
    ])
    env = var.env
    resource_preset = var.resource_preset
    disk_size = var.disk_size
    dbname = var.dbname
    dbowner = var.dbowner
    username = var.username
    hosts = var.hosts
    password = module.yav-secret-getter.secret
}
