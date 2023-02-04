#include <library/cpp/testing/unittest/env.h>
#include <library/cpp/testing/gmock_in_unittest/gmock.h>
#include <library/cpp/testing/unittest/registar.h>
#include <library/cpp/testing/unittest/tests_data.h>

#include <mapreduce/yt/tests/yt_unittest_lib/yt_unittest_lib.h>

#include <maps/wikimap/mapspro/services/autocart/pipeline/libs/detection/include/state.h>
#include <maps/wikimap/mapspro/services/autocart/pipeline/libs/detection/include/extract_unique_rows.h>

#include <mapreduce/yt/util/temp_table.h>

using namespace testing;

namespace maps::wiki::autocart::pipeline {

namespace tests {

Y_UNIT_TEST_SUITE(extract_unique_rows_tests)
{

Y_UNIT_TEST(base_test)
{
    NYT::IClientPtr client = NYT::NTesting::CreateTestClient();
    const TString STATE_PATH = "//home/state";
    State::initialize(client, STATE_PATH);
    const std::vector<uint64_t> NUMS{1, 2, 3, 4, 5, 1, 2, 3, 3, 4, 4};
    const std::vector<uint64_t> UNIQUE_NUMS{1, 2, 3, 4, 5};
    const TString KEY_NAME = "num";

    NYT::TTempTable numsYTTable(client);
    TString outputYTTableName = "//home/output";

    NYT::TTableWriterPtr<NYT::TNode> writer
        = client->CreateTableWriter<NYT::TNode>(numsYTTable.Name());
    for (uint64_t num : NUMS) {
        writer->AddRow(NYT::TNode()(KEY_NAME, num));
    }
    writer->Finish();

    extractUniqueRows(
        client,
        {numsYTTable.Name()},
        outputYTTableName,
        {KEY_NAME}
    );

    std::vector<uint64_t> testNums;
    NYT::TTableReaderPtr<NYT::TNode> reader
        = client->CreateTableReader<NYT::TNode>(outputYTTableName);
    for (; reader->IsValid(); reader->Next()) {
        testNums.push_back(reader->GetRow()[KEY_NAME].AsUint64());
    }

    EXPECT_TRUE(
        std::is_permutation(
            testNums.begin(), testNums.end(),
            UNIQUE_NUMS.begin()
        )
    );
    State::remove(client);
}

} // Y_UNIT_TEST_SUITE(extract_unique_rows_tests)

} // namespace test

} // namespace maps::wiki::autocart::pipeline
