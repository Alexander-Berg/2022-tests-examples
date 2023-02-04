#include <maps/indoor/libs/db/include/outdoor_radiomap.h>

#include <maps/libs/geolib/include/serialization.h>

#include <library/cpp/testing/unittest/gtest.h>
#include <contrib/libs/fmt/include/fmt/format.h>

#include <maps/indoor/libs/unittest/fixture.h>


namespace maps::mirc::db::ugc::tests {

namespace {

const std::string TEST_GEOM_WKT_STR =
    "POLYGON((37.1 55.1, 37.2 55.2, 37.3 55.3, 37.1 55.1))";

void addOutdoorRadiomap(
    const size_t& id,
    const std::string& wkt,
    pgpool3::Pool& pool)
{
    auto txn = pool.masterWriteableTransaction();
    const std::string query = R"end(
        INSERT INTO ugc.outdoor_radiomap (
            id, geometry
        ) VALUES (
            {id}, ST_GeomFromText({wkt}, 4326)
    ))end";
    txn->exec(fmt::format(
        query,
        fmt::arg("id", id),
        fmt::arg("wkt", txn->quote(wkt))
    ));
    txn->commit();
}

struct TestOutdoorRadiomapDB : mirc::unittest::Fixture, ::testing::Test {
    using ::testing::Test::SetUp;
    using mirc::unittest::Fixture::SetUp;
    void SetUp() override {
        addOutdoorRadiomap(1000000000001, TEST_GEOM_WKT_STR, pgPool());
        addOutdoorRadiomap(1000000000002, TEST_GEOM_WKT_STR, pgPool());
        addOutdoorRadiomap(1000000000003, TEST_GEOM_WKT_STR, pgPool());
        addOutdoorRadiomap(1000000000004, TEST_GEOM_WKT_STR, pgPool());
        addOutdoorRadiomap(1000000000005, TEST_GEOM_WKT_STR, pgPool());
    }
};

} // namespace

TEST_F(TestOutdoorRadiomapDB, LoadededCount)
{
    auto txn = pgPool().masterReadOnlyTransaction();
    EXPECT_EQ(loadOutdoorRadiomaps(1000000000001, 0, *txn).size(), 0ul);
    EXPECT_EQ(loadOutdoorRadiomaps(1000000000991, 10, *txn).size(), 0ul);
    EXPECT_EQ(loadOutdoorRadiomaps(0, 1, *txn).size(), 1ul);
    EXPECT_EQ(loadOutdoorRadiomaps(0, 3, *txn).size(), 3ul);
    EXPECT_EQ(loadOutdoorRadiomaps(0, 33, *txn).size(), 5ul);
    EXPECT_EQ(loadOutdoorRadiomaps(1000000000004, 33, *txn).size(), 2ul);
}

TEST_F(TestOutdoorRadiomapDB, LoadOrder)
{
    auto txn = pgPool().masterReadOnlyTransaction();
    const auto loaded = loadOutdoorRadiomaps(1000000000002, 3, *txn);

    ASSERT_EQ(loaded.size(), 3u);
    EXPECT_EQ(loaded[0].id, 1000000000002);
    EXPECT_EQ(loaded[1].id, 1000000000003);
    EXPECT_EQ(loaded[2].id, 1000000000004);
}

} // namespace maps::mirc::db::ugc::tests
