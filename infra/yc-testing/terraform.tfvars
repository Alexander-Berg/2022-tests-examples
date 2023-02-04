cloud = {
  cloud_id  = "yc.wall-e.cloud-testing"
  folder_id = "yc.wall-e.main-testing-folder"
  region    = "ru-central1"
}

service_accounts = {
  main_sa_id      = "yc.wall-e.main-sa-testing"
  mk8s_sa_id      = "yc.wall-e.mk8s-sa-testing"
  mk8s_node_sa_id = "yc.wall-e.mk8s-node-sa-testing"
}

provider_yandex = {
  endpoint         = "api.cloud-preprod.yandex.net:443"
  storage_endpoint = "storage.cloud-preprod.yandex.net:443"
}

web_ssl_certs_domains = {
  # api.wall-e-testing.cloud-preprod.yandex.net.
  api        = "wall-e-testing.cloud-preprod.yandex.net"
  ui = "wall-e-testing.cloud.yandex.ru"
}


#walle_preprod_api = {
#  ip_address_name = "walle-preprod-api"
#  dns_record_name = "api-preprod"
#}

walle_api = {
  ip_address_name = "walle-testing-api"
  dns_record_name = "api"
}

#walle_preprod_ui = {
#  ip_address_name = "walle-preprod-ui"
#}

walle_ui = {
  ip_address_name = "walle-testing-ui"
}

kube_socat = {
  ip_address_name = "walle-mk8s-cluster-kube-socat"
  dns_record_name = "walle-mk8s-cluster-kube-socat"
}

walle_dns_zone_id = "aet1p1l0tvh5fhcs6mnk" # CLOUD-106352 wall-e-testing.cloud-preprod.yandex.net.

# CLOUD-105379 Network and subnets, macro _CLOUD_WALLE_TESTING_NETS_ .
main_network = {
  name = "cloud-walle-testing-nets"
}

main_subnets = {
  # Egress NAT enabled via CLOUD-89462.
  subnets = {
    cloud-walle-testing-nets-ru-central1-a = {
      name              = "cloud-walle-testing-nets-ru-central1-a"
      zone_id           = "ru-central1-a"
      ipv4_subnets      = ["172.16.0.0/16"]
      ipv6_subnets      = ["2a02:6b8:c0e:501:0:fcb4:abcd:0/112"]
      egress_nat_enable = true
    }
    cloud-walle-testing-nets-ru-central1-b = {
      name              = "cloud-walle-testing-nets-ru-central1-b"
      zone_id           = "ru-central1-b"
      ipv4_subnets      = ["172.17.0.0/16"]
      ipv6_subnets      = ["2a02:6b8:c02:901:0:fcb4:abcd:0/112"]
      egress_nat_enable = true
    }
    cloud-walle-testing-nets-ru-central1-c = {
      name              = "cloud-walle-testing-nets-ru-central1-c"
      zone_id           = "ru-central1-c"
      ipv4_subnets      = ["172.18.0.0/16"]
      ipv6_subnets      = ["2a02:6b8:c03:501:0:fcb4:abcd:0/112"]
      egress_nat_enable = true
    }
  }
}

# Docker container registry settings.
walle_registry = {
  name   = "wall-e-registry-testing"
  subnet = "2a0d:d6c1::/32" // Preprod subnet, Used in egress rules.
}

mk8s_cluster_kms_key_name = "walle-mk8s-testing-encryption-key"
mk8s_cluster_settings = {
  name            = "walle-mk8s-testing-cluster"
  version         = "1.21"
  release_channel = "REGULAR"
}

#preprod_mk8s_nodegroup_settings = {
#  name = "walle-mk8s-preprod-nodegroup"
#  size = 3
#}

mk8s_nodegroup_settings = {
  name = "walle-mk8s-nodegroup"
  size = 3
}

mk8s_nodegroup_instance_template_settings = {
  platform_id = "standard-v3"
  metadata = {
    ssh_keys_file_path = "../oslogin-ssh-keys.txt"
  }
  boot_disk = {
    size = 32
    type = "network-hdd"
  }
  resources = {
    core_fraction = 100
    cores         = 4
    memory        = 16
  }
}

main_mongodb_password_secret = {
  secret_id   = "sec-01g8ek79t6k4c178cgchrsc0hj" # yc-walle-testing-lockbox-secrets
  secret_name = "main-mongodb-password"
}
