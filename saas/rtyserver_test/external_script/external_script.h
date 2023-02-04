#pragma once

#include "config.h"

#include <saas/library/daemon_base/daemon/messages.h>
#include <saas/library/daemon_base/daemon/controller.h>

#include <library/cpp/cgiparam/cgiparam.h>

namespace NRTYExternalScript {
    class TServer : public IServer {
    public:
        typedef NRTYExternalScript::TConfig TConfig;
        typedef TCollectServerInfo TInfoCollector;

        TServer(const TConfig& config);
        ~TServer();
        const TConfig& GetConfig() {
            return Config;
        }
        void Stop(ui32 rigidStopLevel = 0, const TCgiParameters* cgiParams = nullptr);
        void Run();
    private:
        class IImpl;
        class TShellRunImpl;
        class TControlImpl;
        const TConfig& Config;
        THolder<IImpl> Impl;
    };
}
