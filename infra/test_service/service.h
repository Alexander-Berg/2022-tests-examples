#pragma once

#include "router.h"

#include <infra/libs/http_service/service.h>

namespace NInfra::NPodAgent {

class TYpTestService {
public:
    TYpTestService(const THttpServiceConfig& cfg)
        : Logger_({})
    {
        TRequestRouterPtr router = new TYpTestServiceRouter<TYpTestService>(*this);
        HttpService_.Reset(new THttpService(cfg, router));
    }

    void Start() {
        auto logFrame = Logger_.SpawnFrame();
        HttpService_->Start(logFrame);
    }

    void Wait() {
        auto logFrame = Logger_.SpawnFrame();
        HttpService_->Wait(logFrame);
    }

    void Shutdown() {
        HttpService_->ShutDown();
    }

private:
    THolder<THttpService> HttpService_;
    TLogger Logger_;
};

} // namespace NInfra::NPodAgent
