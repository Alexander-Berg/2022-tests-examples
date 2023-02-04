#include <library/cpp/testing/unittest/env.h>
#include <library/cpp/testing/gmock_in_unittest/gmock.h>
#include <library/cpp/testing/unittest/registar.h>
#include <library/cpp/testing/unittest/tests_data.h>

#include <mapreduce/yt/interface/client.h>

#include <mapreduce/yt/tests/yt_unittest_lib/yt_unittest_lib.h>

#include <maps/wikimap/mapspro/services/autocart/pipeline/libs/yt_utils/include/batch.h>
#include <maps/wikimap/mapspro/services/autocart/pipeline/libs/yt_utils/include/rows_count.h>

using namespace testing;

namespace maps::wiki::autocart::pipeline {

namespace tests {

Y_UNIT_TEST_SUITE(batch_tests)
{

Y_UNIT_TEST(basic_test)
{
    NYT::IClientPtr client = NYT::NTesting::CreateTestClient();

    const TString inputYTPath = "//home/input";
    const std::vector<size_t> numbers = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10};
    const size_t batchSize = 2u;
    const TString noeraseYTPath = "//home/noerase";
    const TString eraseYTPath = "//home/erase";


    auto squareBatchFunc = [&](
        NYT::IClientBasePtr batchClient,
        const NYT::TRichYPath& batchRYTPath,
        const NYT::TRichYPath& outputRYTPath)
    {
        NYT::TTableReaderPtr<NYT::TNode> batchReader
            = batchClient->CreateTableReader<NYT::TNode>(batchRYTPath);
        NYT::TTableWriterPtr<NYT::TNode> batchWriter
            = batchClient->CreateTableWriter<NYT::TNode>(outputRYTPath);
        for (; batchReader->IsValid(); batchReader->Next()) {
            const NYT::TNode& row = batchReader->GetRow();
            size_t number = row["number"].AsUint64();
            batchWriter->AddRow(NYT::TNode()("number", number * number));
        }
        batchWriter->Finish();
    };


    NYT::TTableWriterPtr<NYT::TNode> writer
        = client->CreateTableWriter<NYT::TNode>(inputYTPath);
    for (const size_t& num : numbers) {
        writer->AddRow(NYT::TNode()("number", num));
    }
    writer->Finish();

    // default: EraseBatch::No
    processBatches(
        client,
        inputYTPath,
        squareBatchFunc,
        noeraseYTPath,
        batchSize
    );
    EXPECT_EQ(getRowsCount(client, inputYTPath), numbers.size());
    EXPECT_EQ(getRowsCount(client, noeraseYTPath), numbers.size());
    NYT::TTableReaderPtr<NYT::TNode> noeraseReader
        = client->CreateTableReader<NYT::TNode>(noeraseYTPath);
    for (size_t i = 0; noeraseReader->IsValid(); noeraseReader->Next(), i++) {
        const NYT::TNode& row = noeraseReader->GetRow();
        size_t square = row["number"].AsUint64();
        EXPECT_EQ(square, numbers[i] * numbers[i]);
    }

    // EraseBatch::Yes
    processBatches(
        client,
        inputYTPath,
        squareBatchFunc,
        eraseYTPath,
        batchSize,
        EraseBatch::Yes
    );
    EXPECT_EQ(getRowsCount(client, inputYTPath), 0u);
    EXPECT_EQ(getRowsCount(client, eraseYTPath), numbers.size());
    NYT::TTableReaderPtr<NYT::TNode> eraseReader
        = client->CreateTableReader<NYT::TNode>(eraseYTPath);
    for (size_t i = 0; eraseReader->IsValid(); eraseReader->Next(), i++) {
        const NYT::TNode& row = eraseReader->GetRow();
        size_t square = row["number"].AsUint64();
        EXPECT_EQ(square, numbers[i] * numbers[i]);
    }
}

} // Y_UNIT_TEST_SUITE(batch_tests)

} // namespace test

} // namespace maps::wiki::autocart::pipeline
