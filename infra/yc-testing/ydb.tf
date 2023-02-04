resource "yandex_ydb_database_dedicated" "ydb_cluster" {
  name               = "testing-wall-e"
  folder_id          = var.cloud.folder_id
  network_id         = module.network.main_network_id
  subnet_ids         = values(module.network.zone_ids_subnet_ids)
  resource_preset_id = "medium"
  scale_policy {
    fixed_scale {
      size = 3
    }
  }
  storage_config {
    group_count     = 1
    storage_type_id = "ssd"
  }
  # TODO: backup config is not supported
  # run: yc ydb database update testing-wall-e --backup name=daily,daily-execute-time=6:00,ttl=48h
}
