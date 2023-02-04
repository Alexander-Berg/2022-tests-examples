#pragma once

#include "updater.h"
#include "viewer_server.h"
#include <saas/library/daemon_base/config/daemon_config.h>
#include <saas/library/daemon_base/daemon/controller.h>
#include <saas/library/daemon_base/daemon/messages.h>
#include <saas/library/daemon_base/module/module.h>

class TTestModelApplication : public IServer {
public:
    class TConfig : public IServerConfig {
    public:
        TConfig(const TServerConfigConstructorParams& params);
        const TDaemonConfig& GetDaemonConfig() const;
        TSet<TString> GetModulesSet() const {
            return TSet<TString>();
        }
        TUpdater::TConfig Updater;
        THttpServerOptions Viewer;
        const TDaemonConfig& Daemon;
    };
public:
    typedef TCollectServerInfo TInfoCollector;
    TTestModelApplication(const TConfig& config);
    const TConfig& GetConfig() const;
    void Run();
    void Stop(ui32 /*rigidStopLevel*/, const TCgiParameters* /*cgiParams*/);
private:
    const TConfig& Config;
    TModel Model;
    TUpdater Updater;
    TViewerServer Viewer;
};
