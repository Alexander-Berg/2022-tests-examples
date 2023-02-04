#include "constants.h"

#include <balancer/kernel/http/parser/http.h>

#include <contrib/libs/benchmark/include/benchmark/benchmark.h>

#include <library/cpp/resource/resource.h>

#include <util/generic/scope.h>
#include <util/generic/string.h>
#include <util/generic/xrange.h>
#include <util/random/shuffle.h>

using namespace NSrvKernel;

namespace {

void CreateHeaders(THeaders& headers) {
    headers.Add(":authority", "yandex.ru");
    headers.Add(":method", "GET");
    headers.Add(":path", NHeadersBenchmarkInternal::Path);
    headers.Add(":scheme", "unknown");
    headers.Add("x-antirobot-robotness-y", "0.0");
    headers.Add("x-forwarded-for", "188.243.246.86");
    headers.Add("x-forwarded-for-y", "188.243.246.86");
    headers.Add("x-forwarded-proto", "https");
    headers.Add("x-https-request", "yes");
    headers.Add("x-req-id", "1585218285566454-13903879570471513787");
    headers.Add("x-source-port-y", "52876");
    headers.Add("x-start-time", "1585218285566454");
    headers.Add("x-yandex-http-version", "http2");
    headers.Add("x-yandex-https", "yes");
    headers.Add("x-yandex-https-info", "handshake-time=0.064124s, no-tls-tickets, handshake-ts=1585218263, cipher-id=4866, protocol-id=772");
    headers.Add("x-yandex-icookie", "9814156611006264886");
    headers.Add("x-yandex-icookie-info", "source=uuid");
    headers.Add("x-yandex-ip", "77.88.55.77");
    headers.Add("x-yandex-internal-request", "0");
    headers.Add("x-yandex-ja3", "771,4865-4866-4867-49195-49199-49196-49200-52393-52392-49171-49172-156-157-47-53-10,0-23-65281-10-11-35-16-5-13-18-51-45-43-21,29-23-24,0");
    headers.Add("x-yandex-loginhash", "3482");
    headers.Add("x-yandex-randomuid", "3715137871585218285");
    headers.Add("x-yandex-suspected-robot", "0");
    headers.Add("x-yandex-tcp-info", "v=2; rtt=0.107053s; rttvar=0.008721s; snd_cwnd=64; total_retrans=2");
    headers.Add("y-balancer-experiments", "187228,0,86");
    headers.Add("accept", "application/json, text/javascript, */*; q=0.01");
    headers.Add("accept-encoding", "gzip, deflate");
    headers.Add("accept-language", "ru-RU,ru;q=0.9,en-US;q=0.8,en;q=0.7");
    headers.Add("cookie", NHeadersBenchmarkInternal::Cookie);
    headers.Add("host", "yandex.ru");
    headers.Add("referer", NHeadersBenchmarkInternal::Referer);
    headers.Add("sec-fetch-dest", "empty");
    headers.Add("sec-fetch-mode", "cors");
    headers.Add("sec-fetch-site", "same-origin");
    headers.Add("user-agent", NHeadersBenchmarkInternal::UserAgent);
    headers.Add("x-requested-with", "XMLHttpRequest");
    headers.Add("connection", "Close");
    headers.Add("content-length", "0");
}

template <typename TKey, typename TValue>
void RunSpecifiedAdd(benchmark::State& state) {
    THeaders headers;
    CreateHeaders(headers);

    for (auto _ : state) {
        state.PauseTiming();

        TVector<std::pair<TKey, TValue>> headerPairs;
        headerPairs.reserve(headers.Size());
        for (const auto& [key, value] : headers) {
            Y_VERIFY(value.size() == 1u);
            headerPairs.emplace_back(key.AsStringBuf(), value[0].AsStringBuf());
        }

        THeaders temp;

        state.ResumeTiming();

        for (auto&& [key, value] : headerPairs) {
            temp.Add(std::move(key), std::move(value));
        }
    }
}

}  // namespace

static void BMAddStringAndString(benchmark::State& state) {
    RunSpecifiedAdd<TString, TString>(state);
}

static void BMAddStaticStringAndStaticString(benchmark::State& state) {
    for (auto _ : state) {
        THeaders headers;
        CreateHeaders(headers);
    }
}

static void BMAddStringAndStringStorage(benchmark::State& state) {
    RunSpecifiedAdd<TString, TStringStorage>(state);
}

static void BMAddStringStorageAndStringStorage(benchmark::State& state) {
    RunSpecifiedAdd<TStringStorage, TStringStorage>(state);
}

namespace {

class TFixtureData {
public:
    TFixtureData(TStringBuf headersFilename)
        : Buffer_(NResource::Find(headersFilename))
    {
        InitHeaders();
    }

    const TString& GetBuffer() const {
        return Buffer_;
    }

    const TVector<std::pair<TStringBuf, TStringBuf>>& GetHeaders() const {
        return Headers_;
    }

private:
    void InitHeaders() {
        for (const auto& headerToken : StringSplitter(Buffer_).SplitByString("\r\n")) {
            TStringBuf header = headerToken.Token();
            if (header == "\n") {
                break;
            }
            TStringBuf key;
            TStringBuf value;
            Y_VERIFY(header.TrySplit(": ", key, value));
            Headers_.emplace_back(key, value);
        }
        Y_VERIFY(Headers_.size() == 38u);
    }

private:
    TString Buffer_;
    TVector<std::pair<TStringBuf, TStringBuf>> Headers_;
};

const TString HeadersFilename = "headers.in";

TFixtureData FixtureData(HeadersFilename);

}  // namespace

void BMAddFromParser(benchmark::State& state) {
    for (auto _ : state) {
        state.PauseTiming();

        TVector<std::pair<TStringBuf, TStringBuf>> headerPairs = FixtureData.GetHeaders();

        THeaders headers;
        headers.Clear(TString(FixtureData.GetBuffer()));

        state.ResumeTiming();

        for (auto&& [key, value] : headerPairs) {
            headers.AddFromParser(std::move(key), std::move(value));
        }
    }
}

static void BMFindValues(benchmark::State& state) {
    const std::vector<TStringBuf> UnusedHeaderKeys = {
        "x-aab-proxy",
        "x-aab-partnertoken",
        "shadow-x-forwarded-for",
        "x-yandex-h",
        "x-yandex-expflags",
        "pragma",
        "x-ucbrowser-ua",
        "x-csrf-token",
        "x-real-ip",
        "x-yandex-logstatuid",
        "x-yandex-expsplitparams",
        "x-yandex-expconfigversion",
        "x-yandex-expboxes-crypted",
        "x-yandex-expboxes",
        "x-yauuid",
        "x-yandex-ht",
        "x-yaclid1",
        "x-l7-exp",
        "serp-rendering-experiment"
    };

    THeaders headers;
    CreateHeaders(headers);

    Y_VERIFY(headers.Size() == UnusedHeaderKeys.size() * 2);

    TVector<std::pair<TString, bool>> queryHeaderKeys;
    for (const auto& unusedHeaderKey : UnusedHeaderKeys) {
        Y_VERIFY(headers.FindValues(unusedHeaderKey) == headers.end());
        queryHeaderKeys.emplace_back(unusedHeaderKey, /*exists =*/ false);
    }
    for (const auto& [key, value] : headers) {
        queryHeaderKeys.emplace_back(key.AsStringBuf(), /*exists =*/ true);
        if (queryHeaderKeys.size() == headers.Size()) {
            break;
        }
    }

    for (auto _ : state) {
        state.PauseTiming();

        Shuffle(queryHeaderKeys.begin(), queryHeaderKeys.end());

        state.ResumeTiming();

        for (const auto& [key, shouldExist] : queryHeaderKeys) {
            bool exists = headers.FindValues(key) != headers.end();
            Y_VERIFY(exists == shouldExist);
        }
    }
}

BENCHMARK(BMAddStringAndString);
BENCHMARK(BMAddStaticStringAndStaticString);
BENCHMARK(BMAddStringAndStringStorage);
BENCHMARK(BMAddStringStorageAndStringStorage);

BENCHMARK(BMAddFromParser);

BENCHMARK(BMFindValues);

