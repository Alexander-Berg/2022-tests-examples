#include <saas/rtyserver/components/fullarchive/component.h>
#include <saas/rtyserver/model/component.h>
#include <saas/rtyserver/indexer_core/index_component_storage.h>
#include <saas/rtyserver/components/fullarchive/light_entity.h>
#include <saas/rtyserver/components/fullarchive/disk_manager.h>
#include <saas/rtyserver/config/layers.h>
#include <saas/rtyserver/model/component.h>
#include <saas/rtyserver/config/const.h>
#include <saas/rtyserver/components/l2/globals.h>

#include <library/cpp/yconf/patcher/unstrict_config.h>
#include <library/cpp/testing/unittest/registar.h>

// Stub for linkage
namespace NFormulaBoost {
    void CalcDocumentDataImpl(const TBoostCommonData& /*commonData*/, const TDocumentIndices& /*id*/, TDocumentData& /*documentData*/) {
        // do nothing here
    }
}

namespace NRTYServer {
    class TL2ComponentTest: public NUnitTest::TTestBase {
        UNIT_TEST_SUITE(TL2ComponentTest)
        UNIT_TEST(TestStoringBlob)
        UNIT_TEST_SUITE_END();

    private:
        void SetUp() override {
            InitGlobalLog2Console(TLOG_WARNING);
        }

        static void FillConfig(TUnstrictConfig& cfg) {
            const TString& emptyConfig = TDaemonConfig::DefaultEmptyConfig;
            Y_VERIFY(cfg.ParseMemory(TStringBuf(emptyConfig), false, nullptr));
            cfg.SetValue("DaemonConfig.LogLevel", ToString((int)TLOG_WARNING));
            cfg.AddSection("Server.Repair");
            cfg.AddSection("Server.Merger");
            cfg.AddSection("Server.Monitoring");
            auto mockHttpOptions = [](TUnstrictConfig& cfg, const TString& path) {
                cfg.SetValue(path + ".Threads", "1");
                cfg.SetValue(path + ".Host", "none");
                cfg.SetValue(path + ".Port", "1");
            };
            mockHttpOptions(cfg, "Server.BaseSearchersServer");
            mockHttpOptions(cfg, "Server.Searcher.HttpOptions");
            mockHttpOptions(cfg, "Server.Indexer.Common.HttpOptions");
            cfg.SetValue("Server.Indexer.Common.RecognizeLibraryFile", "NOTSET");
            cfg.AddSection("Server.Indexer.Disk");
            cfg.AddSection("Server.Indexer.Memory");
        }

        void TearDown() override {
        }

    public:
        // Simple test for storing/obtaining blobs in FullArc light layer using TRTYFullArchiveLightEntity
        void TestStoringBlob() {
            TUnstrictConfig cfg;
            FillConfig(cfg);
            cfg.SetValue("Server.Components", "FULLARC,L2Raw");
            cfg.SetValue("Server.ComponentsConfig." FULL_ARCHIVE_COMPONENT_NAME ".ActiveLayers", "L2Raw");

            TAtomicSharedPtr<TDaemonConfig> dae = MakeAtomicShared<TDaemonConfig>(cfg.ToString().c_str(), false);
            THolder<TRTYServerConfig> rtyConfig = MakeHolder<TRTYServerConfig>(dae);
            rtyConfig->InitFromString(cfg.ToString());

            TIndexComponentsStorage::Instance().ResetConfig(*rtyConfig.Get());

            const ui32 TestDocumentId = 42;
            TParsedDocument testDocument{*rtyConfig.Get()};
            TRTYFullArchiveLightEntity* lightEntity = testDocument.GetComponentEntity<TRTYFullArchiveLightEntity>(TString{NRTYServer::L2ArcComponentName});
            UNIT_ASSERT(lightEntity != nullptr);
            const TString testData{"some_data"};
            const TBlob testDataBlob = TBlob::Copy(testData.data(), testData.size());
            lightEntity->SetData(TBlob(testDataBlob));

            const TSet<TString> layers{
                TString{NRTYServer::L2ArcComponentName},
            };

            TDiskFAManager diskManager(
                GetSystemTempDir(),
                100,
                *rtyConfig.Get(),
                0,
                layers,
                /*enableFastUpdates=*/true,
                /*readOnly=*/false,
                /*final=*/false);
            diskManager.DoOpen();

            diskManager.Index(testDocument, TestDocumentId);

            const TBlob actualDataBlob = diskManager.ReadRawDoc(TString{NRTYServer::L2ArcComponentName}, TestDocumentId);
            UNIT_ASSERT(testDataBlob.Size() == actualDataBlob.Size() && std::equal(testDataBlob.Begin(), testDataBlob.End(), actualDataBlob.Begin()));
        }
    };
} //namespace NRTYServer

UNIT_TEST_SUITE_REGISTRATION(NRTYServer::TL2ComponentTest);
