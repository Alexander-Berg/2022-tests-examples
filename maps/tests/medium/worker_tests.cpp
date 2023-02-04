#include <maps/indoor/long-tasks/src/pg-dumper/lib/worker.h>

#include <maps/indoor/long-tasks/src/pg-dumper/lib/dump_rotation.h>
#include <maps/indoor/long-tasks/src/pg-dumper/lib/yt_gateway.h>

#include <maps/indoor/libs/db/include/lbs_fuse_coefficient.h>
#include <maps/indoor/libs/db/include/outdoor_radiomap.h>

#include <library/cpp/testing/unittest/gtest.h>
#include <contrib/libs/fmt/include/fmt/format.h>

#include <mapreduce/yt/tests/yt_unittest_lib/yt_unittest_lib.h>
#include <maps/indoor/libs/unittest/fixture.h>

#include <util/datetime/constants.h>

#include <string>
#include <vector>


namespace maps::mirc::pg_dumper::tests {

namespace {

using NYT::NTesting::CreateTestDirectory;

NYT::IClientPtr getTestYTClientPtr()
{
    static const auto ytClient = NYT::NTesting::CreateTestClient();
    return ytClient;
}

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

const std::string TEST_GEOM_WKT_STR =
    "POLYGON((37 55, 38 56, 39 57, 37 55))";

void addOutdoorRadiomap(
    const std::string& wkt,
    pgpool3::Pool& pool)
{
    auto txn = pool.masterWriteableTransaction();
    const std::string query = R"end(
        INSERT INTO ugc.outdoor_radiomap (
            geometry
        ) VALUES (
            ST_GeomFromText({wkt}, 4326)
    ))end";
    txn->exec(fmt::format(
        query,
        fmt::arg("wkt", txn->quote(wkt))
    ));
    txn->commit();
}

struct TestWorker : mirc::unittest::Fixture, ::testing::Test {
    using ::testing::Test::SetUp;
    using mirc::unittest::Fixture::SetUp;

    void SetUp() override {
        for (const auto& kFuse : kFuses) {
            addLbsFuseCoefficient(kFuse, pgPool());
        }
        for (size_t i = 0; i < 17; ++i) {
            addOutdoorRadiomap(TEST_GEOM_WKT_STR, pgPool());
        }
        {
            const auto ytClient = getTestYTClientPtr();
            DUMP_DIR = std::string(CreateTestDirectory(ytClient));
            createOldDumpsFolders();
        }
    }

    void createOldDumpsFolders() const {
        const auto ytClient = getTestYTClientPtr();
        const auto microsecsSinceEpoch = Microseconds(1654569027000000); // GMT: Tuesday, 7 June 2022 г., 2:30:27

        const YTGateway gw{ytClient};
        // Create KEPT_DUMPS_COUNT number of dumps for previous days.
        for (size_t i = 1; i <= KEPT_DUMPS_COUNT; ++i) {
            constexpr size_t microsecsInDay = 86400000000;
            const auto prevDumpMicroseconds = microsecsSinceEpoch - Microseconds(i * microsecsInDay);
            const auto prevDumpDir = DUMP_DIR + "/" + getDateString(prevDumpMicroseconds);
            gw.createFolder(prevDumpDir);

            const auto table = prevDumpDir + "/table_" + std::to_string(i);
            gw.createTable(table);

            if (i == 1) {
                gw.createLink(prevDumpDir, DUMP_DIR + "/latest");
            }
        }
    }

    std::string DUMP_DIR;
    const size_t KEPT_DUMPS_COUNT = 3;

    const std::vector<db::ugc::LbsFuseCoefficient> kFuses{
        db::ugc::LbsFuseCoefficient{0, "p1", "l1", 0.1},
        db::ugc::LbsFuseCoefficient{0, "p1", "l2", 0.2},
        db::ugc::LbsFuseCoefficient{0, "p1", "l3", 0.3},
        db::ugc::LbsFuseCoefficient{0, "p2", "l1", 0.4},
        db::ugc::LbsFuseCoefficient{0, "p2", "l2", 0.5},
    };
};

} // namespace

TEST_F(TestWorker, CheckTestEnvironmentIsReady)
{
    const auto ytClient = getTestYTClientPtr();

    const YTGateway gw{ytClient};
    auto dumpDirContent = gw.listContent(DUMP_DIR);
    ASSERT_EQ(dumpDirContent.size(), KEPT_DUMPS_COUNT + 1);
    std::sort(dumpDirContent.begin(), dumpDirContent.end());
    EXPECT_EQ(dumpDirContent[0], "2022-06-04");
    EXPECT_EQ(dumpDirContent[1], "2022-06-05");
    EXPECT_EQ(dumpDirContent[2], "2022-06-06");
    EXPECT_EQ(dumpDirContent[3], "latest");
    const auto latestDir = DUMP_DIR + "/latest";
    const auto latestContent = gw.listContent(latestDir);
    ASSERT_EQ(latestContent.size(), 1u);

    EXPECT_EQ(latestContent[0], "table_1");
    EXPECT_EQ(gw.listContent(DUMP_DIR + "/2022-06-06")[0], "table_1");

    EXPECT_EQ(gw.listContent(DUMP_DIR + "/2022-06-05")[0], "table_2");
    EXPECT_EQ(gw.listContent(DUMP_DIR + "/2022-06-04")[0], "table_3");
}

TEST_F(TestWorker, FirstLaunchNoThrow)
{
    const auto ytClient = getTestYTClientPtr();
    const auto emptyDir = CreateTestDirectory(ytClient);

    const auto worker = Worker{emptyDir, KEPT_DUMPS_COUNT, 3};
    EXPECT_NO_THROW(worker.run(YTGateway{ytClient}, pgPool()));
}

TEST_F(TestWorker, FirstLaunchResult)
{
    const auto ytClient = getTestYTClientPtr();
    const auto emptyDumpDir = CreateTestDirectory(ytClient);

    const auto worker = Worker{emptyDumpDir, KEPT_DUMPS_COUNT, 3};
    worker.run(YTGateway{ytClient}, pgPool());

    {
        const YTGateway gw{ytClient};
        auto dumpedContent = gw.listContent(emptyDumpDir);
        ASSERT_EQ(dumpedContent.size(), 2u);
        std::sort(dumpedContent.begin(), dumpedContent.end());
        ASSERT_TRUE(isValidDateString(dumpedContent[0]));
        ASSERT_EQ(dumpedContent[1], "latest");

        auto latestContent = gw.listContent(emptyDumpDir + "/latest");
        ASSERT_EQ(latestContent.size(), 2u);
        std::sort(latestContent.begin(), latestContent.end());
        ASSERT_EQ(latestContent[0], "ugc_lbs_fuse_coefficient");
        ASSERT_EQ(latestContent[1], "ugc_outdoor_radiomaps");
    }
}

TEST_F(TestWorker, WorkerTest)
{
    const auto ytClient = getTestYTClientPtr();

    const auto worker = Worker{DUMP_DIR, KEPT_DUMPS_COUNT, 3};
    worker.run(YTGateway{ytClient}, pgPool());

    const YTGateway gw{ytClient};
    {
        auto dumpedContent = gw.listContent(DUMP_DIR);
        std::sort(dumpedContent.begin(), dumpedContent.end());

        ASSERT_EQ(dumpedContent.size(), KEPT_DUMPS_COUNT + 1);
        ASSERT_EQ(dumpedContent[KEPT_DUMPS_COUNT], "latest");

        EXPECT_EQ(dumpedContent[0], "2022-06-05");
        EXPECT_EQ(dumpedContent[1], "2022-06-06");
        EXPECT_TRUE(isValidDateString(dumpedContent[2]));
    }
    {
        auto latestContent = gw.listContent(DUMP_DIR + "/latest");
        ASSERT_EQ(latestContent.size(), 2u);
        std::sort(latestContent.begin(), latestContent.end());
        ASSERT_EQ(latestContent[0], "ugc_lbs_fuse_coefficient");
        ASSERT_EQ(latestContent[1], "ugc_outdoor_radiomaps");
    }
}

// TODO (efrolov89): add dump test with non-existent parent directory for table.

} // namespace maps::mirc::pg_dumper::tests
