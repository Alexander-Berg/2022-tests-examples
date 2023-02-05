#include <maps/goods/lib/goods_db/pgpool_wrapper.h>
#include <maps/goods/lib/goods_db/proxy/impl/schema_version_proxy.h>

#include <maps/automotive/libs/interfaces/factory_singleton.h>

#include <maps/libs/local_postgres/include/instance.h>

#include <library/cpp/testing/common/env.h>
#include <library/cpp/testing/unittest/registar.h>

#include <google/protobuf/text_format.h>

using namespace maps::goods;

inline std::string appSourcePath()
{
    return ArcadiaSourceRoot() + "/maps/goods/lib/goods_db/schema/";
}

inline std::string appSourcePath(const std::string& relPath)
{
    return appSourcePath() + relPath;
}

using DatabaseConfig = yandex::maps::proto::automotive::mdb::DatabaseConfig;

DatabaseConfig getDatabaseConfig(const maps::local_postgres::Database& db)
{
    ::setenv("DATABASE_PASSWORD", db.password().c_str(), 1);

    std::stringstream ss;
    ss << "yc: {\n"
       << "    service_account_id: \"\"\n"
       << "    endpoint: \"\"\n"
       << "    cluster_id: \"\"\n"
       << "}\n"
       << "host: \"" << db.host() << "\"\n"
       << "user: \"" << db.user() << "\"\n"
       << "name: \"" << db.dbname() << "\"\n"
       << "port: " << db.port() << "\n"
       << "master_connection_count: 1" << "\n"
       << "max_master_connection_count: 5" << "\n"
       << "slave_connection_count: 1" << "\n"
       << "max_slave_connection_count: 5" << "\n"
       << "password_variable: \"DATABASE_PASSWORD\"" << "\n";

    DatabaseConfig databaseConfig;
    ASSERT(NProtoBuf::TextFormat::ParseFromString(TString(ss.str()), &databaseConfig));
    return databaseConfig;
}

void createSchemaVersionTable(maps::local_postgres::Database& db)
{
    db.executeSqlInTransaction(R"(
    CREATE TYPE schema_version_type AS ENUM (
        'auto',
        'manual'
    );

    CREATE TABLE IF NOT EXISTS schema_version(
        version BIGINT NOT NULL PRIMARY KEY,
        description TEXT NOT NULL,
        type schema_version_type NOT NULL DEFAULT 'auto'::schema_version_type,
        installed_by TEXT NOT NULL,
        installed_on TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
    );
    )");
}

void insertDatabaseMigrationVersion(maps::local_postgres::Database& db, int version)
{
    db.executeSqlInTransaction(
        "INSERT INTO schema_version (version, description, installed_by) "
        "VALUES (" + std::to_string(version) + ", '', '');");
}

struct SchemaVersionCheckParameters {
    std::optional<int> expectedVersion;
    int actualVersion;
    bool isSuccessExpected;
};

Y_UNIT_TEST_SUITE(test_schema_version_check) {

Y_UNIT_TEST(test_skip_schema_check)
{
    maps::local_postgres::Database db;

    auto factory = std::make_shared<maps::interfaces::Factory>();
    factory->addSingleton<PgPoolWrapper>(getDatabaseConfig(db));
    const auto schemaVersionProxy =
        factory->addSingleton<SchemaVersionProxy>(SchemaVersionProxy::Config{
            .minimumExpectedSchemaVersion = std::nullopt,
        });

    UNIT_ASSERT_NO_EXCEPTION(schemaVersionProxy->checkSchemaVersion());
}

Y_UNIT_TEST(test_missing_schema_check_fails)
{
    maps::local_postgres::Database db;

    auto factory = std::make_shared<maps::interfaces::Factory>();
    factory->addSingleton<PgPoolWrapper>(getDatabaseConfig(db));
    const auto schemaVersionProxy =
        factory->addSingleton<SchemaVersionProxy>(SchemaVersionProxy::Config{
            .minimumExpectedSchemaVersion = 1,
        });

    UNIT_ASSERT_EXCEPTION(schemaVersionProxy->checkSchemaVersion(), maps::RuntimeError);
}

Y_UNIT_TEST(test_empty_schema_table_check_fails)
{
    maps::local_postgres::Database db;
    createSchemaVersionTable(db);

    auto factory = std::make_shared<maps::interfaces::Factory>();
    factory->addSingleton<PgPoolWrapper>(getDatabaseConfig(db));
    const auto schemaVersionProxy =
        factory->addSingleton<SchemaVersionProxy>(SchemaVersionProxy::Config{
            .minimumExpectedSchemaVersion = 1,
        });

    UNIT_ASSERT_EXCEPTION(schemaVersionProxy->checkSchemaVersion(), maps::RuntimeError);
}

Y_UNIT_TEST(test_schema_version_check)
{
    std::vector<SchemaVersionCheckParameters> testParameters = {
        SchemaVersionCheckParameters{
            .expectedVersion = 2,
            .actualVersion = 2,
            .isSuccessExpected = true,
        },
        SchemaVersionCheckParameters{
            .expectedVersion = 2,
            .actualVersion = 1,
            .isSuccessExpected = false,
        },
        SchemaVersionCheckParameters{
            .expectedVersion = 2,
            .actualVersion = 3,
            .isSuccessExpected = true,
        },
        SchemaVersionCheckParameters{
            .expectedVersion = std::nullopt,
            .actualVersion = 1,
            .isSuccessExpected = true,
        },
    };

    for (const auto& checkParameters : testParameters) {
        INFO() << "Testing with parameters:"
            << " expectedVersion="
            << (
                checkParameters.expectedVersion.has_value()
                    ? std::to_string(checkParameters.expectedVersion.value())
                    : "any"
            )
            << " actualVersion=" << checkParameters.actualVersion
            << " isSuccessExpected=" << checkParameters.isSuccessExpected;

        maps::local_postgres::Database db;
        createSchemaVersionTable(db);
        insertDatabaseMigrationVersion(db, checkParameters.actualVersion);

        auto factory = std::make_shared<maps::interfaces::Factory>();
        factory->addSingleton<PgPoolWrapper>(getDatabaseConfig(db));
        const auto schemaVersionProxy =
            factory->addSingleton<SchemaVersionProxy>(SchemaVersionProxy::Config{
                .minimumExpectedSchemaVersion = checkParameters.expectedVersion,
            });

        if (checkParameters.isSuccessExpected) {
            UNIT_ASSERT_NO_EXCEPTION(schemaVersionProxy->checkSchemaVersion());
        } else {
            UNIT_ASSERT_EXCEPTION(schemaVersionProxy->checkSchemaVersion(), maps::RuntimeError);
        }
    }
}

}
