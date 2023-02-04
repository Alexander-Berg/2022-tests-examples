#include <ads/tensor_transport/lib_2/difacto_model.h>
#include <ads/tensor_transport/lib_2/feature_computer.h>
#include <ads/tensor_transport/yt_lib/page_scorer.h>
#include <mapreduce/yt/interface/operation.h>
#include <library/cpp/testing/unittest/registar.h>
#include <mapreduce/yt/tests/yt_unittest_lib/yt_unittest_lib.h>
#include <mapreduce/yt/interface/client.h>
#include <util/system/fs.h>
#include <util/system/env.h>
#include <util/stream/file.h>
#include "yabs/proto/tsar.pb.h"


using namespace NTsarTransport;

using NYT::TTableReader;
using NYT::TTableWriter;
using NYT::TNodeWriter;
using NYT::TNode;
using NYT::IClientPtr;
using NYT::NT_TABLE;
using NYT::IMapper;
using NYT::TMessageReader;
using NYT::TMessageWriter;
using NYT::IReducer;
using NYT::IClientBasePtr;
using NYT::TRichYPath;
using NYT::TMapOperationSpec;

REGISTER_MAPPER(TPageScorerMapper<TModel>);


class TPageTransportTest : public TTestBase {
public:
    void SetUp() override {
        TString ytProxy = GetEnv("YT_PROXY");

        Client = NYT::CreateClient(ytProxy);
        TString source = "//tmp/table";
        ResultTable = "//tmp/resultTable";
        auto writer = Client->CreateTableWriter<NYT::TNode>(source);
        TVector<NYT::TNode> records = {
                NYT::TNode()("PageID", 186465L)("ImpID", 3L),
        };

        for (const auto& record: records) {
            writer->AddRow(record);
        }
        writer->Finish();
        THashMap<ui64, TVector<float>> table;
        table[2268279877198854598] = FirstPageVector;

        TComputer<TModel> computer(MakeHolder<TModel>(std::move(table)));
        auto spec = TMapOperationSpec()
                .AddInput<TensorTransport::TPageRecord>(source)
                .AddOutput<TNode>(ResultTable);

        Client->Map(
                spec,
                new TPageScorerMapper<TModel>(std::move(computer), VectorVersion)
        );

    }

    void TearDown() override {
        Client->Remove(ResultTable, NYT::TRemoveOptions().Recursive(true));
    }

    void ApplyVectorTest() {
        auto tableReader = Client->CreateTableReader<NYT::TNode>(ResultTable);
        for (; tableReader->IsValid(); tableReader->Next())
        {
            auto record = tableReader->GetRow();
            yabs::proto::NTsar::TTsarVector vector;
            const TString& strVector = record.ChildAsString("Vector");
            Y_PROTOBUF_SUPPRESS_NODISCARD vector.ParseFromString(strVector);
            auto factors = vector.GetFactors();

            for (int i = 1; i< factors.size(); ++i) {
                float expectedValue = FirstPageVector[i - 1];
                float actualValue = factors[i];
                UNIT_ASSERT_VALUES_EQUAL(expectedValue, actualValue);
            }
        }
    }

    void ApplyVersionTest() {
        auto tableReader = Client->CreateTableReader<NYT::TNode>(ResultTable);
        for (; tableReader->IsValid(); tableReader->Next()) {
            auto record = tableReader->GetRow();
            ui64 actualValue = record.ChildAsUint64("Version");
            UNIT_ASSERT_EQUAL(actualValue, VectorVersion);
        }
    }


private:
    UNIT_TEST_SUITE(TPageTransportTest);
    UNIT_TEST(ApplyVectorTest);
    UNIT_TEST(ApplyVersionTest);
    UNIT_TEST_SUITE_END();
    TString ResultTable;
    NYT::IClientPtr Client;
    TVector<float> FirstPageVector = {3.0, 4.0, 5.0};
    ui64 VectorVersion = 5;


};
UNIT_TEST_SUITE_REGISTRATION(TPageTransportTest);
