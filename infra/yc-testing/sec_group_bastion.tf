resource "ycp_vpc_security_group" "sec_group_bastion_ssh22" {
  provider = ycp.ycp

  name        = "sec-group-bastion-ssh22"
  folder_id   = var.cloud.folder_id
  description = "Rules to cloud-walle-testing-nets"
  network_id  = module.network.main_network_id

  rule_spec {
    direction   = "INGRESS"
    description = "income-bastion-ssh2"
    ports {
      from_port = 22
      to_port   = 22
    }
    protocol_name   = "ANY"
    protocol_number = -1
    cidr_blocks {
      // prod bastion2.0 ips
      v6_cidr_blocks = [
        "2a02:6b8:c0e:500:0:f847:696:0/112",
        "2a02:6b8:c02:900:0:f847:696:0/112",
        "2a02:6b8:c03:500:0:f847:696:0/112"
      ]
    }
  }

  rule_spec {
    direction   = "EGRESS"
    description = "to yandex 443"
    ports {
      from_port = 443
      to_port   = 443
    }
    protocol_name   = "ANY"
    protocol_number = -1
    cidr_blocks {
      v6_cidr_blocks = [
        "2a02:6b8::/32" // yandex subnet
      ]
    }
  }

  rule_spec {
    direction   = "EGRESS"
    description = "to cr.yandex"
    ports {
      from_port = 443
      to_port   = 443
    }
    protocol_name   = "ANY"
    protocol_number = -1
    cidr_blocks {
      v6_cidr_blocks = [
        var.walle_registry.subnet
      ]
    }
  }

  rule_spec {
    direction   = "EGRESS"
    description = "to Managed Kubernetes API in cloud-walle-testing-nets IPv4 subnets"
    ports {
      from_port = 443
      to_port   = 443
    }
    protocol_name   = "ANY"
    protocol_number = -1
    cidr_blocks {
      v4_cidr_blocks = module.network.v4_cidr_blocks
    }
  }

  rule_spec {
    direction   = "EGRESS"
    description = "to kubelets in cloud-walle-testing-nets IPv4 subnets"
    ports {
      from_port = 10250
      to_port   = 10250
    }
    protocol_name   = "ANY"
    protocol_number = -1
    cidr_blocks {
      v4_cidr_blocks = module.network.v4_cidr_blocks
    }
  }
}
