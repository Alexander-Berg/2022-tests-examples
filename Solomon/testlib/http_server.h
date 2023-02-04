#pragma once

#include <library/cpp/http/client/client.h>
#include <library/cpp/http/fetch/exthttpcodes.h>
#include <library/cpp/http/server/http.h>
#include <library/cpp/http/server/response.h>
#include <library/cpp/http/misc/parsed_request.h>
#include <library/cpp/testing/common/network.h>

#include <util/generic/hash.h>

#include <utility>

namespace NSolomon::NTesting {
    class TTestServer: public ::NTesting::TFreePortOwner, public THttpServer, public THttpServer::ICallBack {
    public:
        using THandler = std::function<THttpResponse()>;
        using TReqHandler = std::function<THttpResponse(TParsedHttpFull)>;
        using THandlers = THashMap<TString, TReqHandler>;

        TTestServer();
        ~TTestServer() {
            Stop();
        }

        // NOLINTNEXTLINE(performance-unnecessary-value-param): false positive
        void AddHandler(TString path, THandler resp) {
            AddHandler(std::move(path), [r{std::move(resp)}] (auto) {
                return r();
            });
        }

        void AddHandler(const TString& path, TReqHandler resp) {
            Handlers_[path] = std::move(resp);
        }

        const TString& Address() const noexcept {
            return Address_;
        }

        ui16 Port() const noexcept {
            return GetPort();
        }

    private:
        TClientRequest* CreateClient() override;

    private:
        THandlers Handlers_;
        TString Address_;
    };
}
