#include <maps/factory/libs/db/release.h>
#include <maps/factory/libs/db/release_gateway.h>

#include <maps/factory/libs/unittest/fixture.h>

#include <library/cpp/testing/unittest/registar.h>
#include <library/cpp/testing/gmock_in_unittest/gmock.h>

#include <pqxx/pqxx>

namespace maps::factory::db::tests {
using namespace table::alias;

Y_UNIT_TEST_SUITE(test_release_gateway) {

Y_UNIT_TEST(test_creating_release)
{
    const std::string TEST_RELEASE_NAME = "test_release_name";
    Release testRelease(TEST_RELEASE_NAME);
    testRelease.setModifiedBy("John").setModifiedNow();

    unittest::Fixture fixture;
    pqxx::connection conn(fixture.postgres().connectionString());
    {
        pqxx::work txn(conn);
        ReleaseGateway gtw(txn);
        gtw.insert(testRelease);
        txn.commit();
        EXPECT_EQ(testRelease.id(), 1);
    }

    {
        pqxx::work txn(conn);
        ReleaseGateway gtw(txn);
        auto releases = gtw.load(_Release::status == ReleaseStatus::New);
        ASSERT_EQ(releases.size(), 1u);
        EXPECT_EQ(releases.front().name(), TEST_RELEASE_NAME);
        EXPECT_EQ(releases.front().modifiedAt(), testRelease.modifiedAt());
        EXPECT_EQ(releases.front().modifiedBy(), testRelease.modifiedBy());

        releases.front().setStatus(ReleaseStatus::Testing);
        gtw.update(releases.front());
        txn.commit();
    }

    {
        pqxx::work txn(conn);
        ReleaseGateway gtw(txn);
        auto releases = gtw.load(_Release::status == ReleaseStatus::Testing);
        ASSERT_EQ(releases.size(), 1u);
        EXPECT_EQ(releases.front().name(), TEST_RELEASE_NAME);
    }
}

} // suite

} // namespace maps::factory::db::tests
