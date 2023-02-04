#include "http_server.h"

#include <library/cpp/http/server/response.h>
#include <library/cpp/http/misc/parsed_request.h>

#include <util/string/builder.h>

namespace NSolomon::NTesting {

    TTestServer::TTestServer()
        : TFreePortOwner()
        , THttpServer{this, TOptions(this->GetPort()).AddBindAddress("localhost")}
    {
        Address_ = TStringBuilder() << "http://localhost:" << GetPort();
        Start();
    }

    TClientRequest* TTestServer::CreateClient() {
        struct TReplier: public TRequestReplier {
            explicit TReplier(const THandlers& handlers)
                : Handlers{handlers}
            {
            }

            bool DoReply(const TReplyParams& r) override {
                // disable compression implemented inside THttpServer,
                // because we want to perform compression manually
                r.Output.EnableCompression(false);
                r.Output.EnableBodyEncoding(false);
                r.Output.EnableCompressionHeader(false);

                TParsedHttpFull req{r.Input.FirstLine()};

                if (auto it = Handlers.find(req.Path); it == Handlers.end()) {
                    THttpResponse resp{HttpCodes::HTTP_NOT_FOUND};
                    resp.SetContent(TString{"no handlers for "} + req.Path);
                    r.Output << resp;
                } else {
                    r.Output << it->second(req);
                }
                return true;
            }

            const THandlers& Handlers;
        };

        return new TReplier{Handlers_};
    }

}
