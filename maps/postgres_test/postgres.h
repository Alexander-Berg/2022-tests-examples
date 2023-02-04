#pragma once

#include <maps/libs/local_postgres/include/instance.h>
#include <maps/libs/pgpool/include/pgpool3.h>

class Postgres : public maps::local_postgres::Database {
public:
    Postgres(const std::string& dbSchema = "db_schema.sql");
};

maps::pgpool3::Pool createPool(const Postgres& postgres);
