#pragma once

#include "model.h"
#include <library/cpp/http/server/http.h>

class TViewerServer: public THttpServer, public THttpServer::ICallBack {
public:
    TViewerServer(TModel& model, const THttpServerOptions& options);
    virtual TClientRequest* CreateClient();
private:
    TModel& Model;
};
