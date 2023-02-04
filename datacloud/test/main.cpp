#include <library/cpp/testing/unittest/registar.h>
#include <mapreduce/yt/tests/yt_unittest_lib/yt_unittest_lib.h>
#include <datacloud/launcher/lib/grep/fast_grep.h>


using namespace NYT;
using namespace NYT::NTesting;


Y_UNIT_TEST_SUITE(DatacloudFastGrepTest) {
    static void CreateTable(
        IClientPtr client, const TString& path, const TVector<TNode>& records) {
        client->Create(path, NT_TABLE, TCreateOptions().Recursive(true));
        auto writer = client->CreateTableWriter<TNode>(path);
        for (const auto& record : records) {
            writer->AddRow(record);
        }
        writer->Finish();
    }                       

    Y_UNIT_TEST(FastGrepReducer) {
        auto client = CreateTestClient();
        const auto cryptaTablePath = "//home/x-products/production/crypta_v2/crypta_db_last/all_interesting_yuid";
        CreateTable(
            client,
            cryptaTablePath,
            {
                TNode()("key", "y11111")("cid", "11111")("yuid", "11111"),
            }
        );
        const auto spyLogTablePath = "//user_sessions/pub/spy_log/daily/2019-07-13/clean";
        CreateTable(
            client,
            spyLogTablePath,
            {
                TNode()("key", "y11111")("subkey", "1563101444")("value", "type=TRAFFIC\turl=https://vk.com\ttitle=Sample Page"),
            }
        );
        const auto watchLogTablePath = "//user_sessions/pub/watch_log_tskv/daily/2019-07-13/clean";
        CreateTable(
            client,
            watchLogTablePath,
            {
                TNode()("key", "y11111")("subkey", "1563101444")("value", "type=TRAFFIC\turl=https://yandex.net\ttitle=Sample Title"),
                TNode()("key", "y11111")("subkey", "1563101666")("value", "type=TRAFFIC\turl=https://random.net\ttitle=Another title"),
                TNode()("key", "y11111")("subkey", "1563101888")("value", "type=NOT-TRAFFIC"),
            }
        );

        client->Sort(
            TSortOperationSpec()
                .AddInput(cryptaTablePath)
                .Output(cryptaTablePath)
                .SortBy({"key"}));
        client->Sort(
            TSortOperationSpec()
                .AddInput(spyLogTablePath)
                .Output(spyLogTablePath)
                .SortBy({"key", "subkey"}));
        client->Sort(
            TSortOperationSpec()
                .AddInput(watchLogTablePath)
                .Output(watchLogTablePath)
                .SortBy({"key", "subkey"}));

        Datacloud::Grep::grep(
            client,
            "2019-07-13"
        );
        const auto resultSpyLogTable = TString("//home/x-products/production/datacloud/grep/spy_log/2019-07-13");
        auto nRows = client->Get(resultSpyLogTable + "/@row_count").AsInt64();
        UNIT_ASSERT_EQUAL(nRows, 1);

        const auto resultWatchLogTable = TString("//home/x-products/production/datacloud/grep/watch_log_tskv/2019-07-13");
        nRows = client->Get(resultWatchLogTable + "/@row_count").AsInt64();
        UNIT_ASSERT_EQUAL(nRows, 2);
    }
}
