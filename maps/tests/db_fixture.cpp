#include "db_fixture.h"

#include <library/cpp/testing/unittest/env.h>

#include <string>

namespace maps::wiki::jams_arm2::test {

namespace {

const std::string MIGRATIONS_PATH =
    ArcadiaSourceRoot() + "/maps/wikimap/jams_arm2/migrations/migrations";

local_postgres::Database& getTemplateDatabase()
{
    struct Initializer
    {
        Initializer()
        {
            database.createExtension("postgis");
            database.runPgMigrate(MIGRATIONS_PATH);
        }
        maps::local_postgres::Database database;
    };
    static Initializer db;
    return db.database;
}

std::shared_ptr<pgpool3::Pool> createPool(local_postgres::Database& database)
{
    auto constants = pgpool3::PoolConstants(10, 20, 10, 20);
    constants.getTimeoutMs = std::chrono::seconds(15);
    constants.timeoutEarlyOnMasterUnavailable = false;

    return std::make_shared<pgpool3::Pool>(
        database.connectionString(),
        std::move(constants));
}

} //anonymous namespace

DbFixture::DbFixture()
    : database_(getTemplateDatabase().clone())
    , pool_(createPool(database_))
{
}

} // namespace maps::wiki::jams_arm2::test
