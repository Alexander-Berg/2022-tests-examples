#include <yandex/maps/wiki/unittest/unittest.h>
#include <yandex/maps/wiki/unittest/localdb.h>
#include <yandex/maps/wiki/unittest/config.h>

#include <library/cpp/testing/unittest/registar.h>

#include <set>
#include <string>

namespace maps {
namespace wiki {
namespace unittest {
namespace tests {

namespace {

const std::set<std::string> REQUIRED_SCHEMAS {
    "public",
    "revision",
    "revision_meta",
    "service"
};

} // namespace

Y_UNIT_TEST_SUITE(time_computer_tests)
{

Y_UNIT_TEST(unittest_check_localdb)
{
    MapsproDbFixture fixture;
    auto txn = fixture.pool().masterReadOnlyTransaction();

    auto rows = txn->exec(
        "SELECT DISTINCT schemaname FROM pg_catalog.pg_tables");

    std::set<std::string> schemas;
    for (const auto& row: rows) {
        schemas.emplace(row[0].as<std::string>());
    }
    for (const auto& schema: REQUIRED_SCHEMAS) {
        UNIT_ASSERT_C(
            schemas.count(schema) > 0,
            "Schema " << schema << " was not found in postgres");
    }
}

Y_UNIT_TEST(unittest_txn_now)
{
    MapsproDbFixture fixture;

    auto txn1 = fixture.pool().masterReadOnlyTransaction();
    const auto now1 = txnNow(*txn1);

    auto txn2 = fixture.pool().masterReadOnlyTransaction();
    const auto now2 = txnNow(*txn2);
    UNIT_ASSERT_LT_C(now1, now2, chrono::formatIsoDateTime(now1) << " >= " << chrono::formatIsoDateTime(now2));

    const auto now1Again = txnNow(*txn1);
    UNIT_ASSERT_EQUAL_C(now1, now1Again, chrono::formatIsoDateTime(now1) << " != " << chrono::formatIsoDateTime(now1Again));
}

} // Y_UNIT_TEST_SUITE

} //namespace tests
} //namespace unittest
} //namespace wiki
} //namespace maps
