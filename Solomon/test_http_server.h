#pragma once

#include <solomon/libs/cpp/http/client/http.h>

#include <library/cpp/http/server/http.h>
#include <library/cpp/http/server/response.h>
#include <library/cpp/threading/future/future.h>

#include <library/cpp/testing/common/network.h>

#include <util/generic/hash.h>

namespace NSolomon {

struct TAwaitContext {
    explicit TAwaitContext(TString address)
        : Address{std::move(address)}
    {
    }

    bool IsAny() const {
        return Address.Empty() || Address == "/";
    }

    void Signal() {
        Y_VERIFY(Request);
        Promise.SetValue(std::move(Request));
    }

    TString Address;
    IRequestPtr Request;
    NThreading::TPromise<IRequestPtr> Promise = NThreading::NewPromise<IRequestPtr>();
};

struct TPortOwner {
    TPortOwner() : Port(::NTesting::GetFreePort()) {
    }

    ::NTesting::TPortHolder Port;
};

class TTestHttpServer: private TPortOwner, public THttpServer, public THttpServer::ICallBack {
public:
    using THandler = std::function<THttpResponse()>;
    using THandlers = THashMap<TString, THandler>;
    using THandlersPtr = std::shared_ptr<THandlers>;

    TTestHttpServer();
    ~TTestHttpServer() override {
        Stop();
    }

    void AddHandler(const TString& path, THandler resp) {
        (*Handlers_)[path] = std::move(resp);
    }

    const TString& Address() const noexcept {
        return Address_;
    }

    NThreading::TFuture<IRequestPtr> Wait(TString address = {}) noexcept {
        AwaitCtx_ = std::make_unique<TAwaitContext>(std::move(address));
        return AwaitCtx_->Promise;
    }

private:
    TClientRequest* CreateClient() override;

private:
    TString Address_;
    THandlersPtr Handlers_;
    std::unique_ptr<TAwaitContext> AwaitCtx_;
};

} // namespace NSolomon
