#include <maps/libs/local_postgres/include/instance.h>

#include <library/cpp/testing/unittest/env.h>
#include <library/cpp/testing/unittest/registar.h>
#include <yandex/maps/wiki/unittest/default_extensions.h>

namespace maps::wiki::migrations::tests {

namespace {

const std::string MIGRATIONS_PATH =
    ArcadiaSourceRoot() + "/maps/wikimap/mapspro/migrations/migrations";

} // namespace

Y_UNIT_TEST_SUITE(test_migrations) {

Y_UNIT_TEST(test_apply_migrations)
{
    local_postgres::Database postgres;
    for (const auto& ext : maps::wiki::unittest::defaultPsqlExtensions()) {
        postgres.createExtension(ext);
    }

    postgres.runPgMigrate(MIGRATIONS_PATH);
}

} // Y_UNIT_TEST_SUITE

} // namespace maps::wiki::migrations::tests
