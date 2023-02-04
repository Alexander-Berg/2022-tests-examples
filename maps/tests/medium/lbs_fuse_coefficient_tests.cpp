#include <maps/indoor/libs/db/include/lbs_fuse_coefficient.h>

#include <library/cpp/testing/unittest/gtest.h>
#include <contrib/libs/fmt/include/fmt/format.h>

#include <maps/indoor/libs/unittest/fixture.h>


namespace maps::mirc::db::ugc::tests {

namespace {

void addLbsFuseCoefficient(
    const std::string& planId,
    const std::string& lvlId,
    const double value,
    pgpool3::Pool& pool)
{
    auto txn = pool.masterWriteableTransaction();
    const std::string query = R"end(
        INSERT INTO ugc.lbs_fuse_coefficient (
            indoor_plan_id, indoor_level_universal_id, value
        ) VALUES (
            {planId}, {lvlId}, {value}
    ))end";
    txn->exec(fmt::format(
        query,
        fmt::arg("planId", txn->quote(planId)),
        fmt::arg("lvlId", txn->quote(lvlId)),
        fmt::arg("value", value)
    ));
    txn->commit();
}

struct TestLbsFuseCoefficientsDB : mirc::unittest::Fixture, ::testing::Test {
    using ::testing::Test::SetUp;
    using mirc::unittest::Fixture::SetUp;
    void SetUp() override {
        addLbsFuseCoefficient("p1", "l1", 0.1, pgPool());
        addLbsFuseCoefficient("p1", "l2", 0.2, pgPool());
        addLbsFuseCoefficient("p1", "l3", 0.3, pgPool());
        addLbsFuseCoefficient("p2", "l1", 0.4, pgPool());
        addLbsFuseCoefficient("p2", "l2", 0.5, pgPool());
    }
};

} // namespace

TEST_F(TestLbsFuseCoefficientsDB, LoadededCount)
{
    auto txn = pgPool().masterReadOnlyTransaction();
    EXPECT_EQ(loadLbsFuseCoefficients(0, 0, *txn).size(), 0ul);
    EXPECT_EQ(loadLbsFuseCoefficients(150, 10, *txn).size(), 0ul);
    EXPECT_EQ(loadLbsFuseCoefficients(0, 1, *txn).size(), 1ul);
    EXPECT_EQ(loadLbsFuseCoefficients(0, 3, *txn).size(), 3ul);
    EXPECT_EQ(loadLbsFuseCoefficients(0, 33, *txn).size(), 5ul);
    EXPECT_EQ(loadLbsFuseCoefficients(5, 33, *txn).size(), 1ul);
}

TEST_F(TestLbsFuseCoefficientsDB, LoadOrder)
{
    auto txn = pgPool().masterReadOnlyTransaction();
    const auto loaded = loadLbsFuseCoefficients(2, 3, *txn);

    ASSERT_EQ(loaded.size(), 3u);
    EXPECT_EQ(loaded[0].value, 0.2);
    EXPECT_EQ(loaded[1].value, 0.3);
    EXPECT_EQ(loaded[2].value, 0.4);
}

} // namespace maps::mirc::db::ugc::tests
