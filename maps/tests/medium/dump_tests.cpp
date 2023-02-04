#include <maps/indoor/long-tasks/src/pg-dumper/lib/dump.h>

#include <maps/indoor/long-tasks/src/pg-dumper/lib/pg_reader.h>
#include <maps/indoor/long-tasks/src/pg-dumper/lib/yt_gateway.h>
#include <maps/indoor/long-tasks/src/pg-dumper/lib/yt_writer.h>

#include <maps/indoor/libs/db/include/lbs_fuse_coefficient.h>
#include <maps/indoor/libs/db/include/outdoor_radiomap.h>

#include <contrib/libs/fmt/include/fmt/format.h>
#include <library/cpp/testing/unittest/gtest.h>
#include <mapreduce/yt/tests/yt_unittest_lib/yt_unittest_lib.h>
#include <maps/indoor/libs/unittest/fixture.h>


namespace maps::mirc::pg_dumper::tests {

namespace {

using NYT::NTesting::CreateTestClient;
using NYT::NTesting::CreateTestDirectory;

void addLbsFuseCoefficient(
    const db::ugc::LbsFuseCoefficient& kFuse,
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
        fmt::arg("planId", txn->quote(kFuse.indoorPlanId)),
        fmt::arg("lvlId", txn->quote(kFuse.indoorLevelUniversalId)),
        fmt::arg("value", kFuse.value)
    ));
    txn->commit();
}

struct TestDB : mirc::unittest::Fixture, ::testing::Test {
    using ::testing::Test::SetUp;
    using mirc::unittest::Fixture::SetUp;
    void SetUp() override {
        for (const auto& kFuse : kFuses) {
            addLbsFuseCoefficient(kFuse, pgPool());
        }
    }

    const Batch<db::ugc::LbsFuseCoefficient> kFuses{
        db::ugc::LbsFuseCoefficient{0, "p1", "l1", 0.1},
        db::ugc::LbsFuseCoefficient{0, "p1", "l2", 0.2},
        db::ugc::LbsFuseCoefficient{0, "p1", "l3", 0.3},
        db::ugc::LbsFuseCoefficient{0, "p2", "l1", 0.4},
        db::ugc::LbsFuseCoefficient{0, "p2", "l2", 0.5},
    };
};

} // namespace

TEST_F(TestDB, DumpLbsFuseCoefficients)
{
    const auto ytClient = CreateTestClient();
    const std::string table = CreateTestDirectory(ytClient) + "/table";

    {
        auto txn = pgpool3::makeRepeatableReadTransaction(pgPool().getSlaveConnection());
        auto ytClient = CreateTestClient();
        const YTGateway gw{ytClient};
        const size_t batchSize = 2;
        gw.createTable(table);
        setSchema<db::ugc::LbsFuseCoefficient>(table, ytClient);
        dump<db::ugc::LbsFuseCoefficient>(PGReader(batchSize, *txn), YTWriter(ytClient, table));
    }

    {
        auto reader = ytClient->CreateTableReader<NYT::TNode>(TString(table));

        ASSERT_TRUE(reader->IsValid() && !reader->IsEndOfStream());

        size_t counter = 0;
        for (auto& cursor : *reader) {
            auto& row = cursor.GetRow();
            EXPECT_EQ(
                row["lbs_fuse_coefficient_id"].AsInt64(),
                counter + 1
            );
            EXPECT_EQ(
                row["indoor_plan_id"].AsString(),
                kFuses[counter].indoorPlanId
            );
            EXPECT_EQ(
                row["indoor_level_universal_id"].AsString(),
                kFuses[counter].indoorLevelUniversalId
            );
            EXPECT_DOUBLE_EQ(
                row["value"].AsDouble(),
                kFuses[counter].value
            );

            ++counter;
        }
        EXPECT_EQ(counter, kFuses.size());
    }
}

// TODO (efrolov89): add dump test with non-existent parent directory for table.

} // namespace maps::mirc::pg_dumper::tests
