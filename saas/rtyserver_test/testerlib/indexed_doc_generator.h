#pragma once

#include "backend_proxy.h"
#include <saas/protos/rtyserver.pb.h>

class TIndexedDocGenerator: public TNonCopyable {
public:
    TIndexedDocGenerator(const TBackendProxy &backend);
    void ProcessDoc(NRTYServer::TMessage& message);
    ~TIndexedDocGenerator();

private:
    class TImpl;
    TImpl* Impl;
};
