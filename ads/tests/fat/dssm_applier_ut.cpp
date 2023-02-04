#include <ads/quality/embedding/tsar_tensor/eval/lib_applier/projected_embedder.h>
#include <ads/tensor_transport/yt_lib/tsar_transport.h>
#include <ads/tensor_transport/yt_lib/banner_adapter.h>
#include <ads/tensor_transport/yt_lib/banner_transport_compressed.h>
#include <mapreduce/yt/interface/client.h>
#include <library/cpp/testing/unittest/registar.h>
#include <util/system/env.h>

using namespace NTsarTransport;
using namespace NTsarDssmEmbedder;

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



void CreateInputTableForDSSM(TRichYPath sourceTable, IClientPtr client, int groupExportID) {

    auto writer = client->CreateTableWriter<NYT::TNode>(sourceTable);

    TVector<NYT::TNode> records = {
            NYT::TNode()("BannerID", 14)("GroupExportID", groupExportID)("IsDisabledInRsya", false)("IsActive", true)("IsArchive", false),
    };

    for (const auto& record: records) {
        writer->AddRow(record);
    }
    writer->Finish();

}


REGISTER_MAPPER(TBannerTransportCompressedMapper<NTsarDssmEmbedder::TProjectedDssmApplierBannerEmbedder>);


Y_UNIT_TEST_SUITE(TMultipleDSSMTest) {
    Y_UNIT_TEST(TwoEmbeddersWithDSSMTest) {
        TString sourceTable = "//tmp/testBannerTable";
        TString ytProxy = GetEnv("YT_PROXY");
        TString outputTable = "//tmp/banner_table";
        TString versionsPath = "//tmp/versions";
        auto client = NYT::CreateClient(ytProxy);
        ui64 groupExportID = 13;
        CreateInputTableForDSSM(sourceTable, client, groupExportID);
        auto dssmModel = NNeuralNetApplier::TModel::FromFile("./rsya_ctr50.appl");
        TEmbeddingDenseProjector projector;
        TFileInput projFile{"./identity50.proj"};
        Load(&projFile, projector);
        TProjectedDssmApplierBannerEmbedder embedder(dssmModel, projector);

        Y_UNUSED(embedder);

        THashMap<ui64, TIntrusivePtr<NYT::IMapperBase>> mappers;
        ui32 vectorSize = 50;
        ui64 version = 1;
        float minValue = -1.0;
        float maxValue = 1.0;
        mappers.emplace(version,
                        new TBannerTransportCompressedMapper<NTsarDssmEmbedder::TProjectedDssmApplierBannerEmbedder>(
                                vectorSize,
                                version,
                                {groupExportID},
                                std::move(embedder),
                                minValue,
                                maxValue,
                                true
                        )
        );

        TTsarTransport transport(client, mappers, 100, 20, 1, versionsPath);
        transport.Run(sourceTable, outputTable);

        auto tableReader = client->CreateTableReader<NYT::TNode>(outputTable);
        ui64 size = 0;
        TVector<ui64> versions;
        for (; tableReader->IsValid(); tableReader->Next()) {
            auto row = tableReader->GetRow();
            ui64 actualVersion = row.ChildAsInt64("Version");
            ui64 expectedVersion = 1;
            TString message = "Actual version " + ToString(actualVersion) + " ,but expected " + ToString(expectedVersion);
            versions.push_back(row.ChildAsInt64("Version"));
            ++size;
        }
        TVector<ui64> expectedVersions = {1};

        UNIT_ASSERT_EQUAL(versions, expectedVersions);
        UNIT_ASSERT_EQUAL(size, 1);
    }
}
