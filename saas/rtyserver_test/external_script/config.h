#pragma once

#include <saas/library/daemon_base/config/daemon_config.h>
#include <saas/library/daemon_base/module/module.h>

#include <library/cpp/logger/global/global.h>

#include <util/string/vector.h>

namespace NRTYExternalScript {
    class TConfig : public IServerConfig {
    public:
        enum TType {RUN_SHELL, CONTROL};

        struct TRunShellConfig {
            void Init(const TYandexConfig::Directives& directives);
            void ToString(TStringStream& ss) const;
            TString Command;
            TString StopCommand;
            TString StdOutFile;
            TString StdErrFile;
            bool WaitCommandOnStart;
            bool WaitCommandOnStop;
        };

        struct TControlConfig {
            void Init(const TYandexConfig::Directives& directives);
            void ToString(TStringStream& ss) const;
            TString StartCommand;
            TString StopCommand;
            TString PingCommand;
            TString PingOkValue;
        };

    public:
        TConfig(const TServerConfigConstructorParams& params);
        void Init(const TYandexConfig::Section& root);
        void Init(const TYandexConfig::Directives& directives);
        TString ToString() const;
        const TDaemonConfig& GetDaemonConfig() const {
            return DaemonConfig;
        }

        const TRunShellConfig GetRunShellConfig() const {
            VERIFY_WITH_LOG(Type == RUN_SHELL, "invalid script type");
            return RunShellConfig;
        }

        const TControlConfig GetControlConfig() const {
            VERIFY_WITH_LOG(Type == CONTROL, "invalid script type");
            return ControlConfig;
        }

        TSet<TString> GetModulesSet() const {
            return TSet<TString>();
        }

        TVector<TString> FilesForPreprocess;
        TString Name;
        TType Type;
        TConfigPatcher* Preprocessor;
    private:
        const TDaemonConfig& DaemonConfig;
        TRunShellConfig RunShellConfig;
        TControlConfig ControlConfig;
    };
}
