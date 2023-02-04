locals {
    hostname = "${var.ig_name}-{instance.internal_dc}-{instance.index_in_zone}.${var.dns_zone[var.env]}"
    shortname = "${var.ig_name}-{instance.internal_dc}-{instance.index_in_zone}"
    nsdomain = var.dns_zone[var.env]
}

resource "ycp_microcosm_instance_group_instance_group" "monitoring-ig" {
    count = 1
    name = "${var.ig_name}-ig"
    service_account_id = ycp_iam_service_account.ig-sa.id

    deploy_policy {
        max_unavailable = 1
        max_expansion   = 0
    }

    scale_policy {
        fixed_scale {
            size = var.ig_size
        }
    }

    allocation_policy {
        dynamic "zone" {
            for_each = toset(var.zones)
            content {
                zone_id = zone.value
            }
        }
    }

    depends_on = [
        time_sleep.wait-sa
    ]

    instance_template {
        name = local.shortname
        hostname = local.hostname
        fqdn = local.hostname

        platform_id = "standard-v2"
        service_account_id = ycp_iam_service_account.vm-sa.id

        resources {
            memory = var.memory
            cores  = var.cores
        }

        boot_disk {
            mode = "READ_WRITE"
            disk_spec {
                image_id = var.disk_image_id
                type_id = "network-hdd"
                size = var.disk_size
            }
        }

        secondary_disk {
            mode = "READ_WRITE"
            device_name = var.logs_disk_id
            disk_spec {
                description = "logs disk of ${local.hostname}"
                preserve_after_instance_delete = false
                type_id = "network-ssd"
                size = var.logs_disk_size
            }
        }

        network_interface {
            network_id = ycp_vpc_network.monitoring-nets.id
            subnet_ids = values({ for name, subnet in ycp_vpc_subnet.monitoring-nets : name => subnet.id })
            primary_v4_address {}
            primary_v6_address {}
        }

        underlay_network {
            network_name = "underlay-v6"
            ipv6_dns_record_spec {
                dns_zone_id = var.dns_zone_id
                fqdn = "${local.hostname}."
                ptr = var.set_ptr
                ttl = 600
            }
        }

        metadata = {
            serial-port-enable = 1
            skip_update_ssh_keys = true
            enable-oslogin = true
            shortname = local.shortname
            nsdomain = local.nsdomain
            k8s-runtime-bootstrap-yaml = ""
            user-data = templatefile("${path.module}/user-data.yaml", {
                service_env: var.service_env[var.env],
                dns_zone: var.dns_zone[var.env],
                hostname: local.hostname,
                disk_id: var.logs_disk_id,
            })
            skm = file("${path.module}/skm.yaml")
        }
    }
}
