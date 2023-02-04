resource "ycp_vpc_network" "monitoring-nets" {
    lifecycle {
        prevent_destroy = true
    }
    folder_id = var.folder_id
    name = "monitoring-nets"
}

resource "ycp_vpc_subnet" "monitoring-nets" {
    for_each = toset(var.zones)
    lifecycle {
        prevent_destroy = true
        ignore_changes  = [extra_params]
    }
    v6_cidr_blocks = [
        cidrsubnet(
            cidrsubnet(var.ipv6-prefixes[each.key], var.netmask, var.project_id[var.env]),
            var.net_new_bits,
            0
        )
    ]
    extra_params {
        hbf_enabled = var.hbf_enabled
        rpf_enabled = false
        export_rts = ["65533:666"]
        import_rts = ["65533:776"]
    }
    folder_id = var.folder_id
    name = format("monitoring-nets-%s", each.key)
    network_id = ycp_vpc_network.monitoring-nets.id
    v4_cidr_blocks = [var.ipv4-prefixes[each.key]]
    zone_id = each.key
}
