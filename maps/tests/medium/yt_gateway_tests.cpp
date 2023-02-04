#include <maps/indoor/long-tasks/src/pg-dumper/lib/yt_gateway.h>

#include <mapreduce/yt/tests/yt_unittest_lib/yt_unittest_lib.h>
#include <maps/libs/common/include/exception.h>

#include <library/cpp/testing/unittest/gtest.h>

#include <algorithm>
#include <string>
#include <unordered_map>
#include <vector>

using namespace NYT::NTesting;

namespace maps::mirc::pg_dumper::tests {

namespace {

auto sort(auto vec)
{
    std::sort(vec.begin(), vec.end());
    return vec;
};

NYT::IClientPtr getClient()
{
    static const auto ytClient = CreateTestClient();
    return ytClient;
}

} // namespace

TEST(TestYTGateway, ThrowOnNullPtr)
{
    const NYT::IClientPtr nullClientPtr;
    EXPECT_THROW(
        YTGateway(nullClientPtr),
        RuntimeError
    );
}

TEST(TestYTGatewayListContent, ThrowOnListingTableContent)
{
    auto client = getClient();;
    const auto table = CreateTestDirectory(client) + "/table";
    client->Create(table, NYT::ENodeType::NT_TABLE);
    const YTGateway gw{client};
    EXPECT_THROW(
        gw.listContent(table),
        RuntimeError
    );
}

TEST(TestYTGatewayListContent, ThrowOnPathError)
{
    auto client = getClient();
    const YTGateway gw{client};
    EXPECT_THROW(
        gw.listContent("malformedYTPath"),
        RuntimeError
    );
    const auto dir = CreateTestDirectory(client);
    EXPECT_THROW(
        gw.listContent(dir + "/nonExistentPath/someDir"),
        RuntimeError
    );
}

TEST(TestYTGatewayListContent, ListCreatedTables)
{
    const auto ytClient = getClient();;
    const auto dir = CreateTestDirectory(ytClient);
    const std::vector<std::string> tables{
        "table9", "table2", "table3"
    };
    for (const auto& table : tables) {
        ytClient->Create(dir + "/" + table, NYT::ENodeType::NT_TABLE);
    }

    const auto sortedTables = sort(tables);

    const YTGateway gw{ytClient};
    const auto sortedRead = sort(gw.listContent(dir));
    ASSERT_EQ(sortedTables.size(), sortedRead.size());
    for (size_t i = 0; i < sortedTables.size(); ++i) {
        EXPECT_EQ(sortedTables[i], sortedRead[i]);
    }
}

TEST(TestYTGatewayListContent, ListIsNotRecursive)
{
    const auto ytClient = getClient();;
    const auto dir = CreateTestDirectory(ytClient);
    const std::vector<std::string> tables{
        "table9", "table2", "table3"
    };

    {
        ytClient->Create(dir + "/subfolder", NYT::ENodeType::NT_MAP);
        for (const auto& table : tables) {
            ytClient->Create(dir + "/subfolder/" + table, NYT::ENodeType::NT_TABLE);
        }
    }

    const YTGateway gw{ytClient};
    ASSERT_EQ(gw.listContent(dir).size(), 1u);
    ASSERT_EQ(gw.listContent(dir + "/subfolder").size(), 3u);
}

TEST(TestYTGatewayCreateTable, NoThrow)
{
    auto client = getClient();;
    const auto dir = CreateTestDirectory(client);
    const YTGateway gw{client};
    EXPECT_NO_THROW(
        gw.createTable(dir + "/tableName")
    );
}

TEST(TestYTGatewayCreateTable, ThrowIfNoParentDir)
{
    auto client = getClient();;
    const auto dir = CreateTestDirectory(client);
    const auto nonExistentDir = dir + "/nonExistentDir";
    const YTGateway gw{client};
    EXPECT_THROW(
        gw.createTable(nonExistentDir + "/tableName"),
        RuntimeError
    );
}

TEST(TestYTGatewayCreateTable, ThrowIfAlreadyExists)
{
    auto client = getClient();;
    const auto dir = CreateTestDirectory(client);
    const auto tableAlreadyExists = dir + "/alreadyExists";
    const YTGateway gw{client};
    EXPECT_NO_THROW(gw.createTable(tableAlreadyExists));
    EXPECT_THROW(gw.createTable(tableAlreadyExists), RuntimeError);
}

TEST(TestYTGatewayCreateFolder, ThrowIfNoParentDir)
{
    auto client = getClient();;
    const auto dir = CreateTestDirectory(client);
    const auto nonExistentDir = dir + "/nonExistentDir";
    const YTGateway gw{client};
    EXPECT_THROW(
        gw.createFolder(nonExistentDir + "/folderName"),
        RuntimeError
    );
}

TEST(TestYTGatewayCreateFolder, ThrowIfAlreadyExists)
{
    auto client = getClient();;
    const auto dir = CreateTestDirectory(client);
    const auto existentDir = dir + "/existentDir";
    const YTGateway gw{client};
    EXPECT_NO_THROW(gw.createFolder(existentDir));
    EXPECT_THROW(
        gw.createFolder(existentDir),
        RuntimeError
    );
}

TEST(TestYTGatewayCreateFolder, FolderIsCreated)
{
    auto client = getClient();;
    const auto dir = CreateTestDirectory(client);
    const auto createdDir = dir + "/newDir";
    const auto createdSubDir = createdDir + "/subDir";

    const YTGateway gw{client};

    ASSERT_NO_THROW(gw.createFolder(createdDir));
    ASSERT_NO_THROW(gw.createFolder(createdSubDir));

    ASSERT_EQ(gw.listContent(dir).size(), 1u);
    EXPECT_EQ(gw.listContent(dir).back(), "newDir");

    ASSERT_EQ(gw.listContent(createdDir).size(), 1u);
    EXPECT_EQ(gw.listContent(createdDir).back(), "subDir");

    ASSERT_EQ(gw.listContent(createdSubDir).size(), 0u);
}

TEST(TestYTGatewayCreateLink, NoThrowLinkingFolder)
{
    auto client = getClient();;
    const auto dir = CreateTestDirectory(client);
    const auto subDir = dir + "/subDir";
    const YTGateway gw{client};
    gw.createFolder(subDir);
    EXPECT_NO_THROW(gw.createLink(subDir, dir + "/link"));
}

TEST(TestYTGatewayCreateLink, ThrowForNonExistingTarget)
{
    auto client = getClient();;
    const auto dir = CreateTestDirectory(client);
    const auto subDir = dir + "/subDir";
    const YTGateway gw{client};
    EXPECT_THROW(
        gw.createLink(subDir, dir + "/link"),
        RuntimeError
    );
}

TEST(TestYTGatewayCreateLink, ThrowLinkAlreadyExists)
{
    auto client = getClient();;
    const auto dir = CreateTestDirectory(client);
    const auto subDir = dir + "/subDir";

    const YTGateway gw{client};
    gw.createFolder(subDir);
    gw.createLink(subDir, dir + "/link");

    EXPECT_THROW(
        gw.createLink(subDir, dir + "/link"),
        RuntimeError
    );
}

TEST(TestYTGatewayCreateLink, ThrowLinkNameIsUsed)
{
    auto client = getClient();;
    const auto dir = CreateTestDirectory(client);
    const auto subDir = dir + "/subDir";
    const auto link = dir + "/link";

    const YTGateway gw{client};
    gw.createFolder(subDir);
    gw.createFolder(link);

    EXPECT_THROW(
        gw.createLink(subDir, link),
        RuntimeError
    );
}

TEST(TestYTGatewayCreateLink, LinkProvidesAccessToTarget)
{
    auto client = getClient();;
    const auto dir = CreateTestDirectory(client);
    const auto subDir = dir + "/subDir";

    const YTGateway gw{client};
    gw.createFolder(subDir);
    const auto link = dir + "/link";
    gw.createLink(subDir, link);

    ASSERT_TRUE(gw.listContent(subDir).empty());
    gw.createTable(subDir + "/someTable");

    const auto linkContent = gw.listContent(subDir);
    ASSERT_EQ(linkContent.size(), 1u);
    EXPECT_EQ(linkContent[0], "someTable");
}

TEST(TestYTGatewayPathExists, NoThrow)
{
    auto client = getClient();;
    const YTGateway gw{client};
    EXPECT_NO_THROW(gw.pathExists("//someLegalYtPath"));
}

TEST(TestYTGatewayPathExists, DoesNotExist)
{
    auto client = getClient();;
    const YTGateway gw{client};
    EXPECT_FALSE(gw.pathExists({"//some/path"}));
}

TEST(TestYTGatewayPathExists, DoesExist)
{
    auto client = getClient();;
    const YTGateway gw{client};
    const auto dir = CreateTestDirectory(client);
    EXPECT_TRUE(gw.pathExists(dir));
}

TEST(TestYTGatewayRemove, ThrowOnNonExistenTable)
{
    const auto client = getClient();;
    const YTGateway gw{client};
    const auto dir = CreateTestDirectory(client);
    EXPECT_THROW(
        gw.remove(dir + "/nonExistentTable"),
        RuntimeError
    );
}

TEST(TestYTGatewayRemove, RemovesTables)
{
    const auto client = getClient();;
    const auto dir = CreateTestDirectory(client);

    const YTGateway gw{client};
    ASSERT(gw.listContent(dir).empty());

    const std::vector<std::string> tables{"t9", "t8", "t17", "t2", "t3"};
    for (const auto& table : tables) {
        client->Create(dir + '/' + table, NYT::ENodeType::NT_TABLE);
    }

    ASSERT_EQ(gw.listContent(dir).size(), tables.size());

    const std::unordered_set<std::string> toRemove{dir + "/t8", dir + "/t2"};
    for (const auto& path : toRemove) {
        EXPECT_NO_THROW(gw.remove(path));
    }

    const auto kept = sort(gw.listContent(dir));
    ASSERT_EQ(kept.size(), 3u);

    EXPECT_EQ(kept[0], "t17");
    EXPECT_EQ(kept[1], "t3");
    EXPECT_EQ(kept[2], "t9");
}

TEST(TestYTGatewayRemove, RemoveFolderRecursively)
{
    const auto client = getClient();;
    const auto dir = CreateTestDirectory(client);

    const YTGateway gw{client};
    ASSERT(gw.listContent(dir).empty());

    {
        client->Create(
            dir + "/some/empty/sub/folder",
            NYT::ENodeType::NT_MAP,
            NYT::TCreateOptions().Recursive(true)
        );
        client->Create(
            dir + "/some/sub/folder/with/tables",
            NYT::ENodeType::NT_MAP,
            NYT::TCreateOptions().Recursive(true)
        );
        const std::vector<std::string> tables{"t9", "t8", "t17", "t2", "t3"};
        for (const auto& table : tables) {
            client->Create(dir + "/some/sub/folder/with/tables/" + table, NYT::ENodeType::NT_TABLE);
        }

        ASSERT_EQ(gw.listContent(dir).size(), 1u);
        ASSERT(gw.listContent(dir + "/some/empty/sub/folder").empty());
        ASSERT_EQ(gw.listContent(dir + "/some/sub/folder/with/tables").size(), tables.size());
    }

    EXPECT_NO_THROW(gw.remove(dir + "/some"));
    EXPECT_TRUE(gw.listContent(dir).empty());
}

TEST(TestYTGatewayRemove, RemovesLinkFolderTable)
{
    const auto client = getClient();;
    const auto dir = CreateTestDirectory(client);
    const auto folder = dir + "/folder";
    const auto link = dir + "/link";
    const auto table = dir + "/table";

    const YTGateway gw{client};
    gw.createFolder(folder);
    gw.createLink(folder,link);
    gw.createTable(table);

    ASSERT_EQ(gw.listContent(dir).size(), 3u);

    gw.remove(folder); // Remove folder first so link is left hanging.
    gw.remove(table);
    gw.remove(link);

    ASSERT_EQ(gw.listContent(dir).size(), 0u);
}

} // namespace maps::mirc::pg_dumper::tests
