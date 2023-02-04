#include <balancer/serval/tests/helpers.h>
#include <library/cpp/json/json_reader.h>
#include <library/cpp/json/json_value.h>
#include <library/cpp/testing/unittest/registar.h>
#include <library/cpp/testing/unittest/env.h>
#include <library/cpp/testing/unittest/tests_data.h>
#include <util/string/subst.h>

static NSv::TThreadLocalRoot TLSRoot;

Y_UNIT_TEST_SUITE(Proxy) {

    static auto GetSignals(const NSv::TAuxData& aux, const TVector<TString>& names) {
        TVector<size_t> ret(names.size());
        NJson::TJsonValue value;
        NJson::ReadJsonTree(NSv::SerializeSignals(aux.Signals()), &value);
        for (const auto& item : value.GetArray()) {
            for (size_t i = 0; i < names.size(); i++) {
                if (item.GetArray()[0].GetString() == names[i]) {
                    ret[i] = item.GetArray()[1].GetUInteger();
                }
            }
        }
        return ret;
    }

    ui16 GetPort(ui16 port) {
        return Singleton<TPortManager>()->GetTcpPort(port);
    }

    TString FillPort(const TString config, ui16 port, const TString name = "PORT") {
        return SubstGlobalCopy(config, name, ToString(port));
    }

    Y_UNIT_TEST(OK) {
        const auto backendPort = GetPort(29821);
        auto mod = FromConfig(YAML::Load(FillPort(
            "proxy: http://[::1]:PORT", backendPort)));
        cone::guard backend = RunBackend(backendPort, [&](NSv::IStreamPtr s) {
            return s->WriteHead(200) && s->Write("OK\n") && s->Close();
        });
        UNIT_ASSERT(backend);
        ServerClient(ExpectError(EREQDONE, mod), [&](NSv::IConnection& c) {
            EXPECT_RESPONSE(c.Request({"GET", "/"}), 200, "OK\n");
        });
    }

    Y_UNIT_TEST(ConnRefused) {
        const auto backendPort = GetPort(28062);
        auto mod = FromConfig(YAML::Load(FillPort(
            "proxy: http://[::1]:PORT", backendPort)));
        ServerClient(ExpectError(ECONNREFUSED, mod), [&](NSv::IConnection& c) {
            auto rsp = c.Request({"GET", "/"});
            UNIT_ASSERT(rsp && !rsp->Head() && mun_errno == ECONNRESET);
        });
    }

#if 0
    Y_UNIT_TEST(ConnTimeout) {
        // TODO(velavokr): use the blackhole ips and external network to simulate conn timeouts
        // 0100::2          # rfc6666, 0100::/64
        // 87.250.233.254   # yandex specific, 87.250.233.254/32 (blackhole.yandex.net)
        // 10.66.66.2       # yandex specific, 10.66.66.0/24
        // 2a02:6b8:6666::2 # yandex specific, 2a02:6b8:6666::/64
        auto mod = FromConfig(YAML::Load("{proxy: \"http://[fc00::1]:2331\", conn-timeout: 0.2s}"));
        ServerClient(ExpectError(ETIMEDOUT, mod), [&](NSv::IConnection& c) {
            auto rsp = c.Request({"GET", "/"});
            UNIT_ASSERT(rsp && !rsp->Head() && mun_errno == ECONNRESET);
        });
    }
#endif

    // Recv timeout cancels the request with ERTIMEDOUT.
    Y_UNIT_TEST_TEMPLATE(RecvTimeout, struct { bool sync; }, {false}, {true}) {
        const auto backendPort = GetPort(5072);
        auto mod = FromConfig(YAML::Load(FillPort(sync
            ? "{proxy: \"http://[::1]:PORT\", recv-timeout: 0.05s, sync: true}"
            : "{proxy: \"http://[::1]:PORT\", recv-timeout: 0.05s, sync: false}", backendPort)));
        cone::guard backend = RunBackend(backendPort, [&](NSv::IStreamPtr) {
            UNIT_ASSERT(!cone::sleep_for(std::chrono::seconds(1)));
            return false;
        });
        UNIT_ASSERT(backend);
        ServerClient(ExpectError(ERTIMEDOUT, mod), [&](NSv::IConnection& c) {
            auto rsp = c.Request({"GET", "/", {{":authority", "localhost"}, {":scheme", "http"}}});
            UNIT_ASSERT(rsp);
            UNIT_ASSERT(!rsp->Head() && mun_errno == ECONNRESET);
        });
    }

    // Recv timeout does not begin until payload is uploaded.
    Y_UNIT_TEST_TEMPLATE(RecvTimeoutSlowLoris, struct { bool sync; }, {false}, {true}) {
        const auto backendPort = GetPort(14350);
        auto mod = FromConfig(YAML::Load(FillPort(sync
            ? "{proxy: \"http://[::1]:PORT\", recv-timeout: 0.05s, sync: true}"
            : "{proxy: \"http://[::1]:PORT\", recv-timeout: 0.05s, sync: false}", backendPort)));
        cone::guard backend = RunBackend(backendPort, [&](NSv::IStreamPtr s) {
            UNIT_ASSERT(NSv::ReadFrom(*s));
            return s->WriteHead(200) && s->Write("ok") && s->Close();
        });
        UNIT_ASSERT(backend);
        ServerClient(ExpectError(EREQDONE, mod), [&](NSv::IConnection& c) {
            auto rsp = c.Request({"POST", "/", {{":authority", "localhost"}, {":scheme", "http"}}}, true);
            UNIT_ASSERT(rsp);
            UNIT_ASSERT(cone::sleep_for(std::chrono::milliseconds(150)));
            UNIT_ASSERT(rsp->Close());
            EXPECT_RESPONSE(rsp, 200, "ok");
        });
    }

    // Send timeout cancels the request with EWTIMEDOUT.
    Y_UNIT_TEST_TEMPLATE(SendTimeout, struct { bool sync; }, {false}, {true}) {
        const auto backendPort0 = GetPort(15588);
        const auto backendPort1 = GetPort(15589);
        // Use h2 flow control to block the writer. Default window size is 64k.
        auto mod = FromConfig(YAML::Load(sync
            ? FillPort("{proxy: \"h2c://[::1]:PORT_1\", send-timeout: 0.05s, sync: true}", backendPort1, "PORT_1")
            : FillPort("{proxy: \"h2c://[::1]:PORT_0\", send-timeout: 0.05s, sync: false}", backendPort0, "PORT_0")));
        cone::guard backend = RunBackend(sync ? backendPort1 : backendPort0, [&](NSv::IStreamPtr) {
            UNIT_ASSERT(!cone::sleep_for(std::chrono::milliseconds(500)));
            return false;
        });
        UNIT_ASSERT(backend);
        ServerClient(ExpectError(EWTIMEDOUT, mod), [&](NSv::IConnection& c) {
            auto rsp = c.Request({"POST", "/", {{":authority", "localhost"}, {":scheme", "http"}}}, true);
            UNIT_ASSERT(rsp);
            UNIT_ASSERT(rsp->Write(TString(256 * 1024, 'a')));
            UNIT_ASSERT(!rsp->Head() && mun_errno == ECONNRESET);
        });
    }

    // Backend can respond without reading the payload.
    Y_UNIT_TEST_TEMPLATE(SendDiscard, struct { bool sync; }, {false}, {true}) {
        const auto backendPort = GetPort(21810);
        auto mod = FromConfig(YAML::Load(FillPort(sync
            ? "{proxy: \"http://[::1]:PORT\", sync: true}"
            : "{proxy: \"http://[::1]:PORT\", sync: false}", backendPort)));
        // So our server will actually read, but ignore the payload. Here's a basic h1
        // server that will close the connection instead.
        auto sk = BindBackend(backendPort);
        UNIT_ASSERT(sk);
        cone::guard backend = [sk = std::move(sk)]() mutable {
            while (auto child = NSv::TFile::Accept(sk))
                child.Write("HTTP/1.1 200 OK\r\nContent-Length: 2\r\n\r\nok");
            return false;
        };
        ServerClient(ExpectError(EREQDONE, mod), [&](NSv::IConnection& c) {
            auto rsp = c.Request({"POST", "/", {{":authority", "localhost"}, {":scheme", "http"}}}, true);
            if (sync) {
                // In sync mode, it is necessary to write to get the server to notice closure.
                UNIT_ASSERT(rsp);
                for (size_t i = 0; i < 100; i++) // Should be enough to respond...
                    UNIT_ASSERT(cone::yield());
                UNIT_ASSERT(rsp->Write("nah"));
            }
            EXPECT_RESPONSE(rsp, 200, "ok");
        });
    }

    // Should correctly forward informational responses.
    Y_UNIT_TEST(Forward100) {
        const auto backendPort = GetPort(30469);
        auto mod = FromConfig(YAML::Load(FillPort(
            "!f100 proxy: http://[::1]:PORT", backendPort)));
        cone::guard backend = RunBackend(backendPort, [&](NSv::IStreamPtr s) {
            if (!s->WriteHead(100))
                return false;
            auto payload = NSv::ReadFrom(*s);
            if (!payload)
                return false;
            return s->WriteHead(200) && s->Write(*payload) && s->Close();
        });
        UNIT_ASSERT(backend);
        ServerClient(ExpectError(EREQDONE, mod), [&](NSv::IConnection& c) {
            auto rsp = c.Request({"POST", "/", {{"expect", "100-continue"}}}, true);
            UNIT_ASSERT(rsp);
            auto cont = rsp->Head();
            UNIT_ASSERT(cont);
            UNIT_ASSERT_VALUES_EQUAL(cont->Code, 100);
            UNIT_ASSERT(rsp->Write("Okay\n") && rsp->Close());
            EXPECT_RESPONSE(rsp, 200, "Okay\n");
        });
        UNIT_ASSERT_VALUES_EQUAL(
            GetSignals(mod.second, {"-f100-status-1xx_dmmm", "-f100-status-2xx_dmmm"}), TVector<size_t>({1, 1}));
    }

    // Should reuse a single connection for two back-to-back requests.
    Y_UNIT_TEST(BackendKeepalive) {
        const auto backendPort = GetPort(12345);
        auto mod = FromConfig(YAML::Load(FillPort(
            "proxy: http://[::1]:PORT", backendPort)));
        auto sk = BindBackend(backendPort);
        UNIT_ASSERT(sk);
        cone::guard backend = [sk = std::move(sk)]() mutable {
            auto child = NSv::TFile::Accept(sk);
            UNIT_ASSERT(child);
            UNIT_ASSERT(NSv::Serve(child, [&](NSv::IStreamPtr s) {
                return s->WriteHead(200) && s->Write("OK\n") && s->Close();
            }, nullptr));
            return true;
        };
        ServerClient(ExpectError(EREQDONE, mod), [&](NSv::IConnection& c) {
            EXPECT_RESPONSE(c.Request({"GET", "/"}), 200, "OK\n");
            EXPECT_RESPONSE(c.Request({"GET", "/"}), 200, "OK\n");
        });
    }

    void CollectCode(NSv::THead head, NSv::TAction action, unsigned* out) {
        NSv::IStreamPtr fake = NSv::ConstRequestStream(std::move(head), {},
            [&](NSv::THead& h) { UNIT_ASSERT(h.Code >= 200); out[h.Code - 200]++; return true; },
            [&](TStringBuf) { return true; },
            [&](NSv::THeaderVector&) { return true; }
        );
        UNIT_ASSERT(!action(fake));
        UNIT_ASSERT_VALUES_EQUAL(mun_errno, (int)EREQDONE);
    }

    Y_UNIT_TEST(Weights) {
        auto mod = FromConfig(YAML::Load(
            "proxy:\n"
            "- 4: {const: a, :code: 200}\n"
            "- 2: {const: b, :code: 201}\n"
            "- 1: {const: c, :code: 202}\n"
            "- 4: {const: a, :code: 200}\n"
            "- 2: {const: b, :code: 201}\n"
            "- 1: {const: c, :code: 202}\n"
            "- 0: {const: d, :code: 203}\n"
            "attempts: 3"));
        unsigned r[4] = {};
        for (size_t i = 0; i < 200000; i++)
            CollectCode({"GET", "/"}, mod.first, r);
        UNIT_ASSERT(r[3] == 0);
        UNIT_ASSERT_C(r[2] * 3.8 <= r[0] && r[0] <= 4.2 * r[2]
                   && r[2] * 1.8 <= r[1] && r[1] <= 2.2 * r[2], r[0] << "/" << r[1] << "/" << r[2]);
    }

    Y_UNIT_TEST(InfWeight) {
        auto mod = FromConfig(YAML::Load(
            "proxy:\n"
            "- inf: {const: a, :code: 200}\n"
            "- 1.0: {const: b, :code: 201}"));
        unsigned r[2] = {};
        for (size_t i = 0; i < 10000; i++)
            CollectCode({"GET", "/"}, mod.first, r);
        UNIT_ASSERT(r[0] == 10000 && r[1] == 0);
    }

    UT_ACTION("econnreset") {
        return !mun_error(ECONNRESET, "connection reset");
    }

    Y_UNIT_TEST(RateLimit) {
        auto mod = FromConfig(YAML::Load(
            "proxy:\n"
            "- econnreset\n"
            "- {const: a, :code: 200}\n"
            "else: {const: b, :code: 201}\n"
            "attempts: 2\n"
            "attempt-rate: 1.25"));
        unsigned r[2] = {};
        for (size_t i = 0; i < 200000; i++)
            CollectCode({"GET", "/"}, mod.first, r);
        // ~50% go to 200 directly, 25% are retried, 25% fail. So ratio of 201 to 200 is about 1/3.
        UNIT_ASSERT_C(r[0] * 0.28 <= r[1] && r[1] <= r[0] * 0.38, r[0] << "/" << r[1]);
    }

    UT_ACTION("econnrefused") {
        return !mun_error(ECONNREFUSED, "connection refused");
    }

    Y_UNIT_TEST(FastRetry) {
        auto mod = FromConfig(YAML::Load(
            "proxy:\n"
            "- econnrefused\n"
            "- const: a\n"
            "attempts: 1\n"
            "attempts-fast: 1\n"
            "attempt-rate: 1.25" /* has no effect -- fast attempts do not produce backend load */));
        ServerClient(ExpectError(EREQDONE, mod), [&](NSv::IConnection& c) {
            for (size_t i = 0; i < 20; i++)
                EXPECT_RESPONSE(c.Request({"GET", "/"}), 200, "a");
        });
    }

    Y_UNIT_TEST_TEMPLATE(Hashing, struct { int mode; }, {0}, {1}, {2}) {
        auto mod = FromConfig(YAML::Load(
            "proxy:\n"
            "- {const: a, :code: 200}\n"
            "- {const: b, :code: 201}\n"
            "hash-by: x-header"));
        unsigned r[2] = {};
        for (size_t i = 0; i < 1000; i++)
            CollectCode({"GET", "/",
                mode == 0 ? NSv::THeaderVector{{"x-header", "same"}} :
                mode == 1 ? NSv::THeaderVector{{"x-header", ToString(i)}} : NSv::THeaderVector{}}, mod.first, r);
        if (mode == 0) {
            UNIT_ASSERT_C((r[0] == 1000 && r[1] == 0) || (r[0] == 0 && r[1] == 1000), r[0] << "/" << r[1]);
        } else {
            UNIT_ASSERT_C(r[0] * 0.8 <= r[1] && r[1] <= r[0] * 1.2, r[0] << "/" << r[1]);
        }
    }

    Y_UNIT_TEST(HashingBySubnet) {
        auto mod = FromConfig(YAML::Load(
            "proxy:\n"
            "- {const: a, :code: 200}\n"
            "- {const: b, :code: 201}\n"
            "hash-by: !net/16/64 x-forwarded-for"));
        unsigned r[2] = {};
        for (size_t i = 0; i < 200; i++)
            CollectCode({"GET", "/", {{"x-forwarded-for", TString::Join("200a::", ToString(i))}}}, mod.first, r);
        UNIT_ASSERT((r[0] == 200 && r[1] == 0) || (r[0] == 0 && r[1] == 200));
    }

    Y_UNIT_TEST(Hedged) {
        auto mod = FromConfig(YAML::Load(
            "!hd proxy:\n"
            "- [{delay: 1d}, {const: a, :code: 200}]\n"
            "- {const: b, :code: 201}\n"
            "attempt-delay: 1ms"));
        unsigned r[2] = {};
        for (size_t i = 0; i < 50; i++)
            CollectCode({"GET", "/"}, mod.first, r);
        UNIT_ASSERT(r[0] == 0 && r[1] == 50);
    }

    Y_UNIT_TEST(HedgedFast) {
        auto mod = FromConfig(YAML::Load(
            "proxy:\n"
            "- econnrefused\n"
            "- {const: b, :code: 200}\n"
            "attempt-delay: 1d"));
        unsigned r[1] = {};
        for (size_t i = 0; i < 50; i++)
            CollectCode({"GET", "/"}, mod.first, r);
        UNIT_ASSERT(r[0] == 50);
    }

    Y_UNIT_TEST(HedgedTimeout) {
        auto mod = FromConfig(YAML::Load(
            "proxy:\n"
            "- [{delay: 1s}, {const: no, :code: 500}]\n"
            "attempts: 5\n"
            "attempt-delay: 2ms\n"
            "attempt-for: 20ms\n"
            "else: {const: a, :code: 200}"));
        ServerClient(ExpectError(EREQDONE, mod), [&](NSv::IConnection& c) {
            EXPECT_RESPONSE(c.Request({"GET", "/"}), 200, "a");
        });
    }

    Y_UNIT_TEST(HedgedTimeoutIndivisible) {
        auto mod = FromConfig(YAML::Load(
            "proxy:\n"
            "- [{delay: 20ms}, {const: no, :code: 500}]\n"
            "attempts: 2\n"
            "attempt-delay: 25ms\n"
            "attempt-for: 1ms\n"
            "else: {const: a, :code: 200}"));
        ServerClient(ExpectError(EREQDONE, mod), [&](NSv::IConnection& c) {
            EXPECT_RESPONSE(c.Request({"GET", "/"}), 200, "a");
        });
    }

    Y_UNIT_TEST(HedgedAllErrors) {
        auto mod = FromConfig(YAML::Load(
            "proxy: econnrefused\n"
            "attempts: 2\n"
            "attempt-delay: 2ms"));
        ServerClient(ExpectError(ECONNREFUSED, mod), [&](NSv::IConnection& c) {
            auto rsp = c.Request({"GET", "/"});
            UNIT_ASSERT(rsp);
            UNIT_ASSERT(!rsp->Head());
        });
    }

    // No connection-specific headers from a backend should reach the client
    Y_UNIT_TEST(BackendHopByHopHeaders) {
        const auto backendPort = GetRandomPort();
        const NSv::THeaderVector headers = {
            {"transfer-encoding", "chunked"},
            {"connection", "xxx"},
            {"proxy-connection", "yyy"},
            {"keep-alive", "timeout=60"},
        };
        auto mod = FromConfig(YAML::Load(FillPort("proxy: http://[::1]:PORT", backendPort)));
        cone::guard backend = RunBackend(backendPort, [&](NSv::IStreamPtr s) {
            return s->WriteHead({200, headers}) && s->Write("ok") && s->Close();
        });
        ServerClient(ExpectError(EREQDONE, mod), [&](NSv::IConnection& c) {
            auto rsp = c.Request({"GET", "/", {{":authority", "localhost"}, {":scheme", "http"}}}, false);
            EXPECT_RESPONSE(rsp, 200, "ok");
            for (auto&& h : headers) {
                EXPECT_NO_HEADER(*rsp->Head(), h.first);
            }
        }, true);
    }
//
//    // No connection-specific headers from the client should reach a backend
//    Y_UNIT_TEST(ClientHopByHopHeaders) {
//    }
}
