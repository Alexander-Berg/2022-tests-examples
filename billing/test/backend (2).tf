terraform {
    backend "pg" {
        conn_str = "postgres://terraform_backend@vla-y7m3i1gp78rhtjj1.db.yandex.net:6432/terraform_backend"
        schema_name = "terraform_backend_test"
        skip_schema_creation = false
    }
}
