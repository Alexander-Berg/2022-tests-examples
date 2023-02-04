#pragma once

#include <solomon/libs/cpp/http/client/http.h>

#include <util/generic/vector.h>
#include <util/string/cast.h>

#include <utility>

namespace NSolomon::NTesting {
    struct TMockHeaders: public IHeaders {
        TMockHeaders(TVector<std::pair<TString, TString>> headers)
            : Headers{std::move(headers)}
        {
        }

        TMaybe<TStringBuf> Find(TStringBuf key) const {
            const auto it = FindIf(Headers, [&] (auto&& header) {
                return AsciiEqualsIgnoreCase(header.first, key);
            });
            return it == Headers.end() ? Nothing() : TMaybe<TStringBuf>(it->second);
        }

        void Add(TStringBuf k, TStringBuf v) {
            Headers.emplace_back(ToString(k), ToString(v));
        }

        void ForEach(std::function<void(TStringBuf, TStringBuf)> f) const {
            for (auto [k, v]: Headers) {
                f(k, v);
            }
        }

        TVector<std::pair<TString, TString>> Headers;
    };

    class TMockResponse: public IResponse {
    public:
        TMockResponse(HttpCodes code, TString data, TMockHeaders headers)
            : Code_{code}
            , Data_{std::move(data)}
            , Headers_{std::move(headers)}
        {
        }

        HttpCodes Code() const override {
            return Code_;
        }

        TStringBuf Data() const override {
            return Data_;
        }

        TString ExtractData() override {
            return Data_;
        }

        const IHeaders& Headers() const override {
            return Headers_;
        }

    private:
        HttpCodes Code_;
        TString Data_;
        TMockHeaders Headers_;
    };

    class TMockHttpClient: public IHttpClient {
    public:
        void Request(IRequestPtr, TOnComplete cb, const TRequestOpts&) noexcept override {
            cb(std::move(NextResponse));
        }

        TResult NextResponse;
    };

    inline IResponsePtr MakeHttpResponse(HttpCodes code, TString data, TVector<std::pair<TString, TString>> headers) {
        return MakeHolder<TMockResponse>(code, std::move(data), TMockHeaders(std::move(headers)));
    }

    inline IHttpClient::TResult MakeFetchResult(HttpCodes code, TString data, TVector<std::pair<TString, TString>> headers) {
        return IHttpClient::TResult::FromValue(new TMockResponse(
            code, std::move(data), TMockHeaders(std::move(headers))
        ));
    }

} // namespace NSolomon::NTesting
