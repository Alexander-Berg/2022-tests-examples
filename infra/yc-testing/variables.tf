# Values that come from environment variables 'TF_VAR_*'.
variable "env_iam_token" {
  type = string
}

variable "env_ycp_sa_profile_name" {
  type = string
}

variable "YAV_OAUTH_TOKEN" {
  type = string
}

variable "main_mongodb_password_secret" {
  type = object({
    secret_id   = string
    secret_name = string
  })
}

variable "cloud" {
  type = object({
    cloud_id  = string
    folder_id = string
    region    = string
  })
}

variable "service_accounts" {
  type = object({
    main_sa_id      = string
    mk8s_sa_id      = string
    mk8s_node_sa_id = string
  })
}

variable "provider_yandex" {
  type = object({
    endpoint         = string
    storage_endpoint = string
  })
}

variable "web_ssl_certs_domains" {
  type = object({
    api        = string
    # ui_preprod = string
    ui = string
  })
}

#variable "walle_preprod_api" {
#  type = object({
#    ip_address_name = string
#    dns_record_name = string
#  })
#}

variable "walle_api" {
  type = object({
    ip_address_name = string
    dns_record_name = string
  })
}

#variable "walle_preprod_ui" {
#  type = object({
#    ip_address_name = string
#  })
#}

variable "walle_ui" {
  type = object({
    ip_address_name = string
  })
}

variable "kube_socat" {
  type = object({
    ip_address_name = string
    dns_record_name = string
  })
}

variable "walle_dns_zone_id" {
  type = string
}

variable "main_network" {
  type = object({
    name = string
  })
}

variable "main_subnets" {
  type = object({
    subnets = map(object({
      name              = string
      zone_id           = string
      ipv4_subnets      = list(string)
      ipv6_subnets      = list(string)
      egress_nat_enable = bool
    }))
  })
}

variable "walle_registry" {
  type = object({
    name   = string
    subnet = string
  })
}

variable "mk8s_cluster_kms_key_name" {
  type = string
}

variable "mk8s_cluster_settings" {
  type = object({
    name            = string
    version         = string
    release_channel = string
  })
}

#variable "preprod_mk8s_nodegroup_settings" {
#  type = object({
#    name = string
#    size = number
#  })
#}

variable "mk8s_nodegroup_settings" {
  type = object({
    name = string
    size = number
  })
}

variable "mk8s_nodegroup_instance_template_settings" {
  type = object({
    platform_id = string
    metadata = object({
      ssh_keys_file_path = string
    })
    boot_disk = object({
      size = number
      type = string
    })
    resources = object({
      core_fraction = number
      cores         = number
      memory        = number
    })
  })
}
