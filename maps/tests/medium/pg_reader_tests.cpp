#include <maps/indoor/long-tasks/src/pg-dumper/lib/pg_reader.h>

#include <maps/indoor/libs/db/include/lbs_fuse_coefficient.h>
#include <maps/indoor/libs/db/include/outdoor_radiomap.h>

#include <library/cpp/testing/unittest/gtest.h>
#include <contrib/libs/fmt/include/fmt/format.h>

#include <maps/indoor/libs/unittest/fixture.h>


namespace maps::mirc::pg_dumper::tests {

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

struct TestDB : mirc::unittest::Fixture, ::testing::Test {
    using ::testing::Test::SetUp;
    using mirc::unittest::Fixture::SetUp;
    void SetUp() override {
        addLbsFuseCoefficient("p1", "l1", 0.1, pgPool());
        addLbsFuseCoefficient("p1", "l2", 0.2, pgPool());
        addLbsFuseCoefficient("p1", "l3", 0.3, pgPool());
        addLbsFuseCoefficient("p2", "l1", 0.4, pgPool());
        addLbsFuseCoefficient("p2", "l2", 0.5, pgPool());

        addOutdoorRadiomap(1, TEST_GEOM_WKT_STR, pgPool());
        addOutdoorRadiomap(22, TEST_GEOM_WKT_STR, pgPool());
        addOutdoorRadiomap(3, TEST_GEOM_WKT_STR, pgPool());
        addOutdoorRadiomap(444, TEST_GEOM_WKT_STR, pgPool());
        addOutdoorRadiomap(5, TEST_GEOM_WKT_STR, pgPool());
    }
};

} // namespace

TEST_F(TestDB, PGReaderThrowsOnZeroBatchSize)
{
    auto txn = pgPool().masterReadOnlyTransaction();

    EXPECT_THROW(
        PGReader(0, *txn),
        RuntimeError
    );
}

TEST_F(TestDB, LbsFuseCoefficientReader)
{
    auto txn = pgPool().masterReadOnlyTransaction();

    {
        PGReader r{999, *txn};
        EXPECT_EQ(r.read<db::ugc::LbsFuseCoefficient>().size(), 5ul);
    }
    {
        PGReader r{2, *txn};
        EXPECT_EQ(r.read<db::ugc::LbsFuseCoefficient>().size(), 2ul);
        EXPECT_EQ(r.read<db::ugc::LbsFuseCoefficient>().size(), 2ul);
        EXPECT_EQ(r.read<db::ugc::LbsFuseCoefficient>().size(), 1ul);
        EXPECT_EQ(r.read<db::ugc::LbsFuseCoefficient>().size(), 0ul);
    }
}

TEST_F(TestDB, NoRepeatingReread)
{
    auto txn = pgPool().masterReadOnlyTransaction();

    {
        PGReader r{999, *txn};
        EXPECT_EQ(r.read<db::ugc::LbsFuseCoefficient>().size(), 5ul); // All data is read.
        EXPECT_TRUE(r.read<db::ugc::LbsFuseCoefficient>().empty()); // No new data is read.
        EXPECT_TRUE(r.read<db::ugc::LbsFuseCoefficient>().empty()); // Again no new data is read.
    }
}

TEST_F(TestDB, OutdoorRadiomapReaderOrder)
{
    auto txn = pgPool().masterReadOnlyTransaction();

    PGReader r{2, *txn};
    auto data = r.read<db::ugc::OutdoorRadiomap>();
    EXPECT_EQ(data.size(), 2ul);
    EXPECT_EQ(data[0].id, 1);
    EXPECT_EQ(data[1].id, 3);

    data = r.read<db::ugc::OutdoorRadiomap>();
    EXPECT_EQ(data.size(), 2ul);
    EXPECT_EQ(data[0].id, 5);
    EXPECT_EQ(data[1].id, 22);

    data = r.read<db::ugc::OutdoorRadiomap>();
    EXPECT_EQ(data.size(), 1ul);
    EXPECT_EQ(data[0].id, 444);

    data = r.read<db::ugc::OutdoorRadiomap>();
    EXPECT_TRUE(data.empty());
    data = r.read<db::ugc::OutdoorRadiomap>();
    EXPECT_TRUE(data.empty());
}

} // namespace maps::mirc::pg_dumper::tests
