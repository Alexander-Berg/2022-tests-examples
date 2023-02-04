#include <maps/indoor/long-tasks/src/pg-dumper/lib/dump_rotation.h>

#include <mapreduce/yt/tests/yt_unittest_lib/yt_unittest_lib.h>
#include <maps/indoor/long-tasks/src/pg-dumper/lib/yt_gateway.h>

#include <library/cpp/testing/unittest/gtest.h>

using namespace NYT::NTesting;

namespace maps::mirc::pg_dumper::tests {

namespace {

NYT::IClientPtr getTestYTClientPtr()
{
    static const auto ytClient = CreateTestClient();
    return ytClient;
}

} // namespace

TEST(KeepNLatest, NoThrow)
{
    const auto client = getTestYTClientPtr();
    const auto dir = CreateTestDirectory(client);
    const YTGateway gw{client};
    EXPECT_NO_THROW(
        keepNLatestDumps(dir, 17, gw)
    );
}

TEST(KeepNLatest, RemovesOldestFolders)
{
    const auto client = getTestYTClientPtr();
    const auto dir = CreateTestDirectory(client);
    const YTGateway gw{client};
    {
        gw.createFolder(dir + "/2022-06-01");
        gw.createFolder(dir + "/2022-06-02");
        gw.createFolder(dir + "/2022-06-04");
        gw.createFolder(dir + "/2022-06-03");
    }
    ASSERT_NO_THROW(
        keepNLatestDumps(dir, 2, gw)
    );

    auto kept = gw.listContent(dir);
    sort(kept.begin(), kept.end());
    ASSERT_EQ(kept.size(), 2u);
    EXPECT_EQ(kept[0], std::string("2022-06-03"));
    EXPECT_EQ(kept[1], std::string("2022-06-04"));
}

TEST(KeepNLatest, RemovesOnlyFolderesWithValidNames)
{
    const auto client = getTestYTClientPtr();
    const auto dir = CreateTestDirectory(client);
    const YTGateway gw{client};
    {
        gw.createFolder(dir + "/2022-06-01_INVALID_NAME");
        gw.createFolder(dir + "/2022-06-01");
        gw.createFolder(dir + "/2022-06-02");
        gw.createFolder(dir + "/latest");
        gw.createFolder(dir + "/2022-06-04");
        gw.createFolder(dir + "/2022-06-03");
    }
    ASSERT_NO_THROW(
        keepNLatestDumps(dir, 2, gw)
    );

    auto kept = gw.listContent(dir);
    sort(kept.begin(), kept.end());

    ASSERT_EQ(kept.size(), 4u);
    EXPECT_EQ(kept[0], std::string("2022-06-01_INVALID_NAME"));
    EXPECT_EQ(kept[1], std::string("2022-06-03"));
    EXPECT_EQ(kept[2], std::string("2022-06-04"));
    EXPECT_EQ(kept[3], std::string("latest"));
}

} // namespace maps::mirc::pg_dumper::tests
