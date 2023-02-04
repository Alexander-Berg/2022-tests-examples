#include "viewer_server.h"
#include "viewer_client.h"

TViewerServer::TViewerServer(TModel& model, const THttpServerOptions& options)
: THttpServer(this, options)
, Model(model)
{}

TClientRequest* TViewerServer::CreateClient() {
    return new TViewerClient(Model);
}
