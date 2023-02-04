resource "yandex_mdb_mongodb_cluster" "mongodb_cluster" {
  name                = "testing-walle-mongodb"
  environment         = "PRODUCTION"
  network_id          = module.network.main_network_id
  deletion_protection = false

  dynamic "host" {
    for_each = module.network.zone_ids_subnet_ids
    content {
      zone_id   = host.key
      subnet_id = host.value
    }
  }

  cluster_config {
    version = "4.2"
  }

  resources {
    resource_preset_id = "s2.micro"
    disk_size          = 10
    disk_type_id       = "network-ssd"
  }

  database {
    name = "walle"
  }

  database {
    name = "walle-health"
  }

  user {
    name     = "walle"
    password = module.main_mongodb_password_secret.secret
    permission {
      database_name = "admin"
      roles         = ["mdbMonitor"]
    }
    permission {
      database_name = "walle"
      roles         = ["readWrite"]
    }
    permission {
      database_name = "walle-health"
      roles         = ["readWrite"]
    }
  }

  labels = {
    mdb-auto-purge = "off"
  }
}
