#######################################################################
# Network and subnets.
module "network" {
  source = "../modules/network"
  providers = {
    ycp = ycp.ycp
  }

  cloud        = var.cloud
  main_network = var.main_network
  main_subnets = var.main_subnets
}

#######################################################################
# API IPv6 address and DNS record.
module "api_ip_address" {
  source = "../modules/yandex-only-ip-with-dns-record"
  providers = {
    ycp = ycp.ycp
  }

  ip_address_name = var.walle_api.ip_address_name
  dns_record_name = var.walle_api.dns_record_name
  dns_zone_id     = var.walle_dns_zone_id
}

#######################################################################
# Wall-e testing API Web SSL certificate.
module "api_certificate" {
  source = "../modules/certificate"
  providers = {
    ycp = ycp.ycp
  }

  cloud = var.cloud
  fqdn  = var.web_ssl_certs_domains.api
}

#######################################################################
# UI IPv6 address.
# UI DNS record is in .yandex.ru and is managed externally.
module "ui_ip_address" {
  source = "../modules/yandex-only-ip"
  providers = {
    ycp = ycp.ycp
  }

  ip_address_name = var.walle_ui.ip_address_name
}

#######################################################################
# Wall-e testing UI Web SSL certificate.
module "ui_certificate" {
  source = "../modules/certificate"
  providers = {
    ycp = ycp.ycp
  }

  cloud = var.cloud
  fqdn  = var.web_ssl_certs_domains.ui
}

#######################################################################
# Endpoint to Kubernetes API IPv6 address.
# https://wiki.yandex-team.ru/users/nrkk/kubernetes-v-ipv6-setjax/#kakpodkljuchatsjavipv6master
module "kube_socat_ip_address" {
  source = "../modules/yandex-only-ip-with-dns-record"
  providers = {
    ycp = ycp.ycp
  }

  ip_address_name = var.kube_socat.ip_address_name
  dns_record_name = var.kube_socat.dns_record_name
  dns_zone_id     = var.walle_dns_zone_id
}

#######################################################################
# Docker container registry.
resource "yandex_container_registry" "walle_registry" {
  name      = var.walle_registry.name
  folder_id = var.cloud.folder_id
}

#######################################################################
# Managed Kubernetes cluster.
module "mk8s_cluster" {
  source = "../modules/mk8s_cluster"

  kms_key_name = var.mk8s_cluster_kms_key_name
  mk8s_cluster_settings = {
    name                       = var.mk8s_cluster_settings.name
    version                    = var.mk8s_cluster_settings.version
    release_channel            = var.mk8s_cluster_settings.release_channel
    master_network_id          = module.network.main_network_id
    master_region              = var.cloud.region
    master_zone_ids_subnet_ids = module.network.zone_ids_subnet_ids
    service_account_id         = var.service_accounts.mk8s_sa_id
    node_service_account_id    = var.service_accounts.mk8s_node_sa_id
  }
}

#######################################################################
# Managed Kubernetes cluster nodegroup.
module "mk8s_nodegroup" {
  source = "../modules/mk8s_nodegroup"

  mk8s_nodegroup_settings = {
    name                 = var.mk8s_nodegroup_settings.name
    mk8s_cluster_id      = module.mk8s_cluster.mk8s_cluster_id
    mk8s_cluster_version = module.mk8s_cluster.mk8s_cluster_version
    zone_ids             = keys(module.network.zone_ids_subnet_ids)
    size                 = var.mk8s_nodegroup_settings.size
  }

  instance_template_settings = {
    platform_id = var.mk8s_nodegroup_instance_template_settings.platform_id
    metadata = {
      ssh_keys_file_path = var.mk8s_nodegroup_instance_template_settings.metadata.ssh_keys_file_path
    }
    boot_disk = {
      size = var.mk8s_nodegroup_instance_template_settings.boot_disk.size
      type = var.mk8s_nodegroup_instance_template_settings.boot_disk.type
    }
    network_interface = {
      security_group_ids = []
      subnet_ids         = values(module.network.zone_ids_subnet_ids)
    }
    resources = {
      core_fraction = var.mk8s_nodegroup_instance_template_settings.resources.core_fraction
      cores         = var.mk8s_nodegroup_instance_template_settings.resources.cores
      memory        = var.mk8s_nodegroup_instance_template_settings.resources.memory
    }
  }
}

#######################################################################
# Logging group.
resource "yandex_logging_group" "walle_mk8s_logs" {
  name             = "walle-mk8s-logs"
  folder_id        = var.cloud.folder_id
  retention_period = "72h0m0s"
}

#######################################################################
# Main MongoDb password.
module "main_mongodb_password_secret" {
  source = "../modules/yav-secret-getter"

  yav_oauth = var.YAV_OAUTH_TOKEN
  sec_id    = var.main_mongodb_password_secret.secret_id
  sec_name  = var.main_mongodb_password_secret.secret_name
}
