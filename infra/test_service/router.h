#pragma once

#include <infra/libs/service_iface/router.h>

namespace NInfra::NPodAgent {

template <typename TService>
class TYpTestServiceRouter : public IRequestRouter {
public:
    TYpTestServiceRouter(TService& service)
        : Service_(service)
    {}

    bool Has(TStringBuf path) const override {
        return path == "/ping" || path == "/shutdown";
    }

    TRouterResponse Handle(const TStringBuf path, const TString&, const TVector<std::pair<TString, TString>>&) const override {
        if (path == "/ping") {
            return {"pong", {}};
        } else if (path == "/shutdown") {
            Service_.Shutdown();
            return {"", {}};
        } else {
            throw yexception() << "Unimplemented path";
        }
    }

private:
    TService& Service_;
};

} // namespace NInfra::NPodAgent
