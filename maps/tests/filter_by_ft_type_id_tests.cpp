#include <library/cpp/testing/unittest/env.h>
#include <library/cpp/testing/gmock_in_unittest/gmock.h>
#include <library/cpp/testing/unittest/registar.h>
#include <library/cpp/testing/unittest/tests_data.h>

#include <mapreduce/yt/tests/yt_unittest_lib/yt_unittest_lib.h>

#include <maps/wikimap/mapspro/services/autocart/pipeline/libs/detection/include/state.h>
#include <maps/wikimap/mapspro/services/autocart/pipeline/libs/detection/include/filter_by_ft_type_id.h>

#include <mapreduce/yt/util/temp_table.h>

using namespace testing;

namespace maps::wiki::autocart::pipeline {

namespace tests {

namespace {

static const TString FT_ID = "ft_id";
static const TString SHAPE = "shape";
static const TString FT_TYPE_ID = "ft_type_id";

} // namespace

Y_UNIT_TEST_SUITE(filter_by_ft_type_id_tests)
{

Y_UNIT_TEST(base_test)
{
    NYT::IClientPtr client = NYT::NTesting::CreateTestClient();

    const TString STATE_PATH = "//home/state";
    State::initialize(client, STATE_PATH);

    NYT::TTempTable ftYTTable(client);
    NYT::TTempTable ftGeomYTTable(client);

    const int ftId1 = 1;
    const FTTypeId ftTypeId1 = FTTypeId::URBAN_RESIDENTIAL;
    const TString shape1 = "adsf";

    const int ftId2 = 2;
    const FTTypeId ftTypeId2 = FTTypeId::URBAN_INDUSTRIAL;
    const TString shape2 = "asgklsd";

    NYT::TTableWriterPtr<NYT::TNode> ftWriter
        = client->CreateTableWriter<NYT::TNode>(ftYTTable.Name());
    ftWriter->AddRow(
        NYT::TNode()
        (FT_ID, ftId1)
        (FT_TYPE_ID, encodeFTTypeId(ftTypeId1))
    );
    ftWriter->AddRow(
        NYT::TNode()
        (FT_ID, ftId2)
        (FT_TYPE_ID, encodeFTTypeId(ftTypeId2))
    );
    ftWriter->Finish();
    client->Sort(
        NYT::TSortOperationSpec()
            .AddInput(ftYTTable.Name())
            .Output(ftYTTable.Name())
            .SortBy({FT_ID})
    );

    NYT::TTableWriterPtr<NYT::TNode> ftGeomWriter
        = client->CreateTableWriter<NYT::TNode>(ftGeomYTTable.Name());
    ftGeomWriter->AddRow(
        NYT::TNode()
        (FT_ID, ftId1)
        (SHAPE, shape1)
    );
    ftGeomWriter->AddRow(
        NYT::TNode()
        (FT_ID, ftId2)
        (SHAPE, shape2)
    );
    ftGeomWriter->Finish();
    client->Sort(
        NYT::TSortOperationSpec()
            .AddInput(ftGeomYTTable.Name())
            .Output(ftGeomYTTable.Name())
            .SortBy({FT_ID})
    );


    NYT::TTempTable outputYTTable(client);


    NYT::ITransactionPtr txn = client->StartTransaction();

    filterAreasByFTTypeId(
        txn,
        {ftYTTable.Name()},
        {ftGeomYTTable.Name()},
        {FTTypeId::URBAN_RESIDENTIAL},
        outputYTTable.Name()
    );

    txn->Commit();

    std::vector<NYT::TNode> testNodes;
    NYT::TTableReaderPtr<NYT::TNode> reader
        = client->CreateTableReader<NYT::TNode>(outputYTTable.Name());
    for (; reader->IsValid(); reader->Next()) {
        testNodes.push_back(reader->GetRow());
    }

    EXPECT_EQ(testNodes.size(), 1u);
    EXPECT_EQ(testNodes.front()[FT_TYPE_ID].AsInt64(),
              encodeFTTypeId(ftTypeId1));
    EXPECT_EQ(testNodes.front()[SHAPE].AsString(), shape1);

    State::remove(client);
}

} // Y_UNIT_TEST_SUITE(filter_by_ft_type_id_tests)

} // namespace test

} // namespace maps::wiki::autocart::pipeline
