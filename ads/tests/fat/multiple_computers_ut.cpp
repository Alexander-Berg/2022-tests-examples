#include <ads/tensor_transport/yt_lib/tsar_transport.h>
#include <ads/tensor_transport/lib_2/model.h>
#include <ads/tensor_transport/lib_2/difacto_model.h>
#include <ads/tensor_transport/lib_2/feature_computer.h>
#include <ads/tensor_transport/yt_lib/banner_transport_compressed.h>
#include <mapreduce/yt/interface/client.h>
#include <library/cpp/testing/unittest/registar.h>
#include <util/system/env.h>

using namespace NTsarTransport;


class TMockModel: public IModel {
    const ui32 ModelSize;
    TVector<float> StubValue = TVector(ModelSize, 5.0f);

    const TArrayRef<const float> EmptyVector;

public:
    TMockModel() : ModelSize(50) {}

    const TArrayRef<const float> Lookup(ui64 feature) const override {

        const TArrayRef<const float> resultArrayRef(StubValue.begin(), StubValue.end());
        if (feature == 7826035404431730673) {
            return resultArrayRef;
        }
        return EmptyVector;
    }
    ui64 GetVectorSize() const override {
        return ModelSize;
    }

    void Save(IOutputStream *stream) const override {
        Y_UNUSED(stream);
    }

    void Load(IInputStream *stream) override {
        Y_UNUSED(stream);
    }

};


void CreateInputTable(TRichYPath sourceTable, IClientPtr client, int groupExportID) {

    auto writer = client->CreateTableWriter<NYT::TNode>(sourceTable);

    TVector<NYT::TNode> records = {
            NYT::TNode()("BannerID", 14)("GroupExportID", groupExportID)("IsDisabledInRsya", false)("IsActive", true)("IsArchive", false),
    };

    for (const auto& record: records) {
        writer->AddRow(record);
    }
    writer->Finish();

}

using TAdapter = TFactorizationBannerAdapter<TMockModel>;
REGISTER_MAPPER(TBannerTransportCompressedMapper<TAdapter>);



Y_UNIT_TEST_SUITE(TMultipleComputesTest) {
        Y_UNIT_TEST(TwoComputersWithFactorizationTest) {
            TString sourceTable = "//tmp/testBannerTable";
            TString ytProxy = GetEnv("YT_PROXY");
            TString outputTable = "//tmp/banner_table";
            TString versionsPath = "//tmp/versions";
            auto client = NYT::CreateClient(ytProxy);
            ui64 groupExportID = 13;
            CreateInputTable(sourceTable, client, groupExportID);
            THashMap<TString, THashSet<TString>> lemmerFixlist;
            TComputer<TMockModel> firstComputer(MakeHolder<TMockModel>());
            TAdapter firstAdapter(std::move(firstComputer), lemmerFixlist);
            THashMap<ui64, TIntrusivePtr<NYT::IMapperBase>> mappers;
            ui32 vectorSize = 51;
            ui64 version = 1;
            float minValue = -1.0;
            float maxValue = 1.0;
            mappers.emplace(version,
                            new TBannerTransportCompressedMapper<TAdapter>(
                                    vectorSize,
                                    version,
                                    {groupExportID},
                                    std::move(firstAdapter),
                                    minValue,
                                    maxValue
                            )
            );

            TComputer<TMockModel> secondComputer(MakeHolder<TMockModel>());
            TFactorizationBannerAdapter<TMockModel> secondAdapter(std::move(secondComputer), lemmerFixlist);
            ++version;
            mappers.emplace(
                    version,
                    new TBannerTransportCompressedMapper<TFactorizationBannerAdapter<TMockModel>>(
                            vectorSize,
                            version,
                            {groupExportID},
                            std::move(secondAdapter),
                            minValue,
                            maxValue
                    )
            );

            TTsarTransport transport(client, mappers, 100, 20, 1, versionsPath);
            transport.Run(sourceTable, outputTable);

            auto tableReader = client->CreateTableReader<NYT::TNode>(outputTable);
            ui64 size = 0;
            TVector<ui64> versions;
            for (; tableReader->IsValid(); tableReader->Next()) {
                auto row = tableReader->GetRow();
                ui64 actualVersion = row.ChildAsUint64("Version");
                ui64 expectedVersion = 1;
                TString message = "Actual version " + ToString(actualVersion) + " ,but expected " + ToString(expectedVersion);
                versions.push_back(row.ChildAsInt64("Version"));
                ++size;
            }
            TVector<ui64> expectedVersions = {1, 2};

            UNIT_ASSERT_EQUAL(versions, expectedVersions);
            UNIT_ASSERT_EQUAL(size, 2);
        }
}
