#include "postgres_fixture.h"

#include <maps/libs/local_postgres/include/instance.h>

namespace maps::renderer::cartograph::test_util {

namespace {

local_postgres::Database& dbInstance()
{
    static local_postgres::Database instance;
    return instance;
}

local_postgres::Database createDatabase()
{
    return dbInstance().clone();
}

config::PgPool makePostgresConfig(const local_postgres::Database& db)
{
    config::PgPool cfg;
    cfg.instances.emplace_back(db.host(), db.port());
    cfg.connectionParams = db.connectionString();
    return cfg;
}

} // namespace

PostgresFixture::PostgresFixture()
    : db{createDatabase()}, pgPool{std::make_shared<PgPoolWrapper>(makePostgresConfig(db))}
{ }

} // namespace maps::renderer::cartograph::test_util
