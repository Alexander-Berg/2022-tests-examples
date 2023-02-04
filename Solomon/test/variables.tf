// COMMON ================================================

variable "yc_endpoint" {
    type = string
    default = null
}

variable "yc_storage_endpoint" {
    type = string
    default = "storage.yandexcloud.net"
}

variable "env" {
    type = string
    default = "preprod"
}

variable "service_env" {
    type = map(string)
    default = {
        preprod = "cloud-preprod"
        prod = "cloud-prod"
        gpn = "cloud-gpn-1"
    }
}

variable "ig_name" {
    type = string
    description = "Yandex Cloud instance group name"
    default = "alerting"
}

variable "ig_size" {
    type = number
    default = 3
    description = "Number of instances"
}

variable "cloud_id" {
    type = string
    description = "Yandex Cloud cloud id"
    default = "yc.monitoring.cloud"
}

variable "folder_id" {
    type = string
    description = "Yandex Cloud folder id for Instances"
    default = "aoevf7r0oava431p9n7d"
}

variable "infra_folder_id" {
    type = map(string)
    default = {
        preprod = "aoekkv85v2jm9vg0k36q" // infra@yc-monitoring
        prod = ""
    }
}

variable "disk_image_id" {
    type = string
    default = "fdv1gv3b2k6ateapamj9"
}

variable "enable_certificates_downloader_binding" {
    type = bool
    default = false
}

// RESOURCES ================================================

variable "memory" {
    type = number
    default = 16
}

variable "cores" {
    type = number
    default = 8
}

variable "disk_size" {
    type = number
    default = 15
}

variable "logs_disk_size" {
    type = number
    default = 5
}

variable "logs_disk_id" {
    type = string
    default = "logs-disk"
}

// NET ================================================

variable "zones" {
    type = list(string)
    description = "Yandex Cloud availability zones"
    default = [
        "ru-central1-a",
        "ru-central1-b",
        "ru-central1-c"
    ]
}

variable "ipv4-prefixes" {
    type = map(string)
    default = {
        ru-central1-a = "172.16.0.0/16"
        ru-central1-b = "172.17.0.0/16"
        ru-central1-c = "172.18.0.0/16"
    }
}

variable "ipv6-prefixes" {
    type = map(string)
    default = {
        // _CLOUDTESTNETS_:
        ru-central1-a = "2a02:6b8:c0e:501::/64"
        ru-central1-b = "2a02:6b8:c02:901::/64"
        ru-central1-c = "2a02:6b8:c03:501::/64"
    }
}

variable "hbf_enabled" {
    type = bool
    default = true
}

variable "netmask" {
    type = number
    default = 32
}

variable "project_id" {
    type = map(number)
    default = {
        // testing = 16762 // == 0x417a == https://racktables.yandex-team.ru/index.php?page=services&tab=projects&project_name=_CLOUDTESTNETS_
        preprod = 64669 // == 0xfc9d == https://racktables.yandex-team.ru/index.php?page=services&tab=projects&project_name=_CLOUD_MONITORING_PREPROD_NETS_
        prod = 63652 // == 0xf8a4 == https://racktables.yandex-team.ru/index.php?page=services&tab=projects&project_name=_CLOUD_MONITORING_PROD_NETS_
    }
}

variable "net_new_bits" {
    type = number
    default = 16
}

// DNS ================================================

variable "create_dns_zone" {
    type = bool
    default = false
}

variable "dns_zone_id" {
    type = string
    default = "yc.solomon.svc"
}

variable "dns_zone" {
    type = map(string)
    default = {
        preprod = "mon.cloud-preprod.yandex.net"
        prod = "mon.cloud.yandex.net"
    }
}

variable "set_ptr" {
    type = bool
    default = true
}
