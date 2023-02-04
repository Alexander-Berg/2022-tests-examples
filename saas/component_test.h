#pragma once

#include <saas/rtyserver/model/component.h>
#include <saas/rtyserver/indexer_core/parsed_document.cpp>
#include <library/cpp/yconf/patcher/unstrict_config.h>

namespace NRTYServer {
    // mocks "application container" for unit tests
    class TComponentTestHelper: public TThrRefBase {
    public:
        using TPtr = TIntrusivePtr<TComponentTestHelper>;
        using TPatch = TVector<std::pair<TString, TString>>;

    private:
        THolder<TRTYServerConfig> Config;

    public:
        const TRTYServerConfig& GetConfig() {
            return *Config;
        }

    protected:
        TComponentTestHelper() = default;

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

        void SetConfig(THolder<TRTYServerConfig>&& config) {
            Config.Swap(config);
        }

    public:
        void SetUpGlobals() {
            TIndexComponentsStorage::Instance().ResetConfig(GetConfig());
        }

        void TearDownGlobals() {
            TIndexComponentsStorage::Instance().ReleaseComponents();
        }
    };
}
