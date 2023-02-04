#include <ads/tensor_transport/yt_lib/genocide.h>
#include <mapreduce/yt/interface/operation.h>
#include <library/cpp/testing/unittest/registar.h>
#include <mapreduce/yt/interface/client.h>
#include <util/system/fs.h>
#include <util/system/env.h>
#include "yabs/proto/tsar.pb.h"




REGISTER_MAPPER(TGenocideMapper);


class TGenocideTest : public TTestBase {
public:
    void SetUp() override {
        TString ytProxy = GetEnv("YT_PROXY");

        Client = NYT::CreateClient(ytProxy);
        TString source = "//tmp/table";
        ResultTable = "//tmp/resultTable";
        auto writer = Client->CreateTableWriter<NYT::TNode>(source);
        TVector<NYT::TNode> records = {
                NYT::TNode()("BannerID", 0ul)("NeedLoad", true)(GROUP_EXPORT_ID_LABEL, GoodExportID),
                NYT::TNode()("BannerID", 4ul)("NeedLoad", false)(GROUP_EXPORT_ID_LABEL, BadExportID),
                NYT::TNode()("BannerID", 4ul)("NeedLoad", false)(GROUP_EXPORT_ID_LABEL, BadExportID)
        };

        for (const auto &record: records) {
            writer->AddRow(record);
        }
        writer->Finish();
        auto spec = NYT::TMapOperationSpec()
                .AddInput<NYT::TNode>(source)
                .AddOutput<NYT::TNode>(ResultTable);

        Client->Map(
                spec,
                new TGenocideMapper()
        );

    }

    void TearDown() override {
        Client->Remove(ResultTable, NYT::TRemoveOptions().Recursive(true));
    }

    void TestGoodExpGroupID() {
        auto tableReader = Client->CreateTableReader<NYT::TNode>(ResultTable);
        ui8 rowsCount = 0;
        for (; tableReader->IsValid(); tableReader->Next()) {
            auto record = tableReader->GetRow();
            if (record.ChildAsUint64(GROUP_EXPORT_ID_LABEL) == GoodExportID) {
                UNIT_ASSERT_EQUAL(record.ChildAsBool("WasLoaded"), true);
            }

            if (record.ChildAsUint64(GROUP_EXPORT_ID_LABEL) == BadExportID)
            {
                ythrow yexception() << "That GroupExportID doesn't supposed to be load";
            }

            ++rowsCount;
        }

        UNIT_ASSERT_EQUAL(rowsCount, 1);
    }

private:
    UNIT_TEST_SUITE(TGenocideTest);
    UNIT_TEST(TestGoodExpGroupID);
    UNIT_TEST_SUITE_END();
    TString ResultTable;
    NYT::IClientPtr Client;
    ui64 GoodExportID = 124124;
    ui64 BadExportID = 124125;
    TString GROUP_EXPORT_ID_LABEL = "GroupExportID";
};
UNIT_TEST_SUITE_REGISTRATION(TGenocideTest);
