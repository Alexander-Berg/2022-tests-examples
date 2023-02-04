#include <maps/libs/local_postgres/include/instance.h>

#include <library/cpp/testing/unittest/env.h>
#include <library/cpp/testing/unittest/registar.h>

namespace maps::wiki::ugc::migrations::tests {

namespace {

const std::string MIGRATIONS_PATH =
    ArcadiaSourceRoot() + "/maps/wikimap/ugc/migrations/migrations";

} // namespace

Y_UNIT_TEST_SUITE(test_migrations) {

Y_UNIT_TEST(test_apply_migrations)
{
    local_postgres::Database postgres;

    postgres.runPgMigrate(MIGRATIONS_PATH);
}

} // Y_UNIT_TEST_SUITE

} // namespace maps::wiki::ugc::migrations::tests
