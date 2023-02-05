#include <yandex/maps/wiki/unittest/arcadia.h>
#include <library/cpp/testing/unittest/registar.h>

#include <maps/wikimap/mapspro/libs/poi_feed/include/reserved_id.h>
#include <yandex/maps/wiki/unittest/localdb.h>

#include <pqxx/pqxx>

using namespace maps::wiki::poi_feed;
using namespace maps::wiki;

namespace {
void
fillDatabase(pqxx::transaction_base& txn)
{
    txn.exec(
        "INSERT INTO sprav.export_poi_reserved_permalinks_unused (permalink_id) "
        "VALUES (1),(2),(3),(4),(5)");
}
} // namespace

Y_UNIT_TEST_SUITE(reserved_id)
{
Y_UNIT_TEST_F(test_same_assigned, unittest::ArcadiaDbFixture)
{
    auto txn = pool().masterWriteableTransaction();
    fillDatabase(*txn);
    UNIT_ASSERT(assignReservedPermalinkId(*txn, {}).empty());
    const auto mapping = assignReservedPermalinkId(*txn, {11, 12, 13});
    UNIT_ASSERT_EQUAL(mapping, assignReservedPermalinkId(*txn, {11, 12, 13}));
}

Y_UNIT_TEST_F(test_not_enought_left, unittest::ArcadiaDbFixture)
{

    auto txn = pool().masterWriteableTransaction();
    fillDatabase(*txn);
    UNIT_ASSERT_NO_EXCEPTION(assignReservedPermalinkId(*txn, {14, 15, 16}));
    UNIT_ASSERT_EXCEPTION(assignReservedPermalinkId(*txn, {17, 18, 19}), maps::Exception);
}

Y_UNIT_TEST_F(test_same_assigned_and_new, unittest::ArcadiaDbFixture)
{
    auto txn = pool().masterWriteableTransaction();
    fillDatabase(*txn);
    const auto mapping = assignReservedPermalinkId(*txn, {11, 12, 13});
    const auto mapping2 = assignReservedPermalinkId(*txn, {11, 12, 13, 14, 15});
    for (const auto [oid, pid] : mapping) {
        UNIT_ASSERT_EQUAL(mapping2.at(oid), pid);
    }
    std::unordered_set<PermalinkId> mapped2;
    for (const auto [_, pid] : mapping2) {
        mapped2.insert(pid);
        UNIT_ASSERT(pid > 0 && pid < 6);
    }
    UNIT_ASSERT_EQUAL(mapped2.size(), 5);
}

Y_UNIT_TEST_F(test_clear_and_assign, unittest::ArcadiaDbFixture)
{
    auto txn = pool().masterWriteableTransaction();
    fillDatabase(*txn);
    const auto mapping = assignReservedPermalinkId(*txn, {11, 12, 13});
    clearAssignmentsOfReservedPermalinkId(*txn, {12});
    const auto mapping2 = assignReservedPermalinkId(*txn, {12, 13});
    UNIT_ASSERT_EQUAL(mapping.at(13), mapping2.at(13));
    UNIT_ASSERT_UNEQUAL(mapping.at(12), mapping2.at(12));
}
}
