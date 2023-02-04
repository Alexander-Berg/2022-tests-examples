#include "test_http_server.h"

#include <library/cpp/http/misc/parsed_request.h>
#include <util/string/ascii.h>

namespace NSolomon {
namespace {

class THeaders: public IHeaders {
public:
    explicit THeaders(THttpHeaders headers)
        : Holder_{std::move(headers)}
    {
        for (auto& h: Holder_) {
            Headers_.emplace(h.Name(), h.Value());
        }
    }

    TMaybe<TStringBuf> Find(TStringBuf name) const override {
        for (auto&& [k, v]: Headers_) {
            if (AsciiEqualsIgnoreCase(k, name)) {
                return v;
            }
        }

        return Nothing();
    }

    void Add(TStringBuf, TStringBuf) override {
        // not implemented
        Y_VERIFY(false);
    }

    void ForEach(std::function<void(TStringBuf, TStringBuf)> fn) const override {
        for (auto&& h: Headers_) {
            fn(h.first, h.second);
        }
    }

private:
    THttpHeaders Holder_;
    THashMap<TStringBuf, TStringBuf> Headers_;
};

class TServerRequest: public IRequest {
public:
    explicit TServerRequest(const TRequestReplier::TReplyParams& r) {
        TParsedHttpFull parsed{r.Input.FirstLine()};
        Method_ = FromString(parsed.Method);
        Url_ = parsed.Path;
        if (r.Input.HasContent()) {
            Data_ = r.Input.ReadAll();
        }

        Headers_.Reset(new THeaders{r.Input.Headers()});
    }

    EHttpMethod Method() const override {
        return Method_;
    }

    TStringBuf Url() const override {
        return Url_;
    }

    TStringBuf Data() const override {
        return Data_;
    }

    const IHeaders& Headers() const override {
        return *Headers_;
    }

    IHeaders& Headers() override {
        return *Headers_;
    }

private:
    EHttpMethod Method_;
    TString Url_;
    TString Data_;
    IHeadersPtr Headers_;
};

class TReplier: public TRequestReplier {
public:
    TReplier(std::unique_ptr<TAwaitContext> ctx, TTestHttpServer::THandlersPtr handlers)
        : AwaitCtx_{std::move(ctx)}
        , Handlers_{std::move(handlers)}
    {
    }

    bool DoReply(const TReplyParams& r) override {
        static auto notFoundHandler = [] {
            THttpResponse resp;
            resp.SetHttpCode(HttpCodes::HTTP_NOT_FOUND);
            return resp;
        };

        TParsedHttpRequest req{r.Input.FirstLine()};
        if (AwaitCtx_) {
            AwaitCtx_->Request.Reset(new TServerRequest{r});
        }

        auto it = Handlers_->find(req.Request);
        if (it == Handlers_->end()) {
            r.Output << notFoundHandler();
            if (AwaitCtx_ && AwaitCtx_->IsAny()) {
                AwaitCtx_->Signal();
            }

            return true;
        }

        r.Output << it->second();

        if (AwaitCtx_ && AwaitCtx_->Address == req.Request) {
            AwaitCtx_->Signal();
        }

        return true;
    }

private:
    std::unique_ptr<TAwaitContext> AwaitCtx_;
    TTestHttpServer::THandlersPtr Handlers_;
};

} // namespace

TTestHttpServer::TTestHttpServer()
    : TPortOwner()
    , THttpServer{this, TOptions(this->Port).AddBindAddress("localhost")}
    , Address_{"http://localhost:" + ToString(Options().Port)}
    , Handlers_{std::make_shared<THandlers>()}
{
}

TClientRequest* TTestHttpServer::CreateClient() {
    return new TReplier{std::move(AwaitCtx_), Handlers_};
}

} // namespace NSolomon
