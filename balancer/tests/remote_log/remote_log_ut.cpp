#include <library/cpp/json/json_reader.h>
#include <library/cpp/testing/unittest/registar.h>
#include <library/cpp/testing/unittest/env.h>
#include <balancer/serval/tests/helpers.h>

static NSv::TThreadLocalRoot TLSRoot;

Y_UNIT_TEST_SUITE(RemoteLog) {
    static std::pair<bool, cone::event> Ev;

    UT_ACTION("logstorage-1") {
        Y_DEFER { Ev.first = true; Ev.second.wake(); };
        auto payload = NSv::ReadFrom(*req);
        UNIT_ASSERT(payload);
        auto size = TStringBuf(*payload).Before(' ');
        auto part = TStringBuf(*payload).After(' ');
        UNIT_ASSERT_VALUES_EQUAL(FromString<size_t>(size), part.size());
        NJson::TJsonValue json;
        NJson::TJsonValue expect;
        NJson::ReadJsonTree(part, &json);
        json.EraseValue("timestamp");
        NJson::ReadJsonTree(R"({
            "addr": "[::2]:12345",
            "headers": [[":authority","unknown"],[":scheme","unknown"],["x-forwarded-for","[::2]:12345"]],
            "method": "GET",
            "protocol": "HTTP/1.1",
            "request": "/",
            "status": 200
        })", &expect);
        UNIT_ASSERT_VALUES_EQUAL(json, expect);
        return req->WriteHead(200) && req->Write("OK\n") && req->Close();
    }

    Y_UNIT_TEST(Request) {
        Ev.first = false;
        auto mod = FromConfig(YAML::Load("- remote-log: logstorage-1\n" "- const: ok\n"));
        ServerClient(ExpectError(EREQDONE, mod), [&](NSv::IConnection& c) {
            EXPECT_RESPONSE(c.Request({"GET", "/", {{"x-forwarded-for", "[::2]:12345"}}}), 200, "ok");
        });
        UNIT_ASSERT(Ev.first || Ev.second.wait());
        // Wait for detached coroutine's termination.
        UNIT_ASSERT(cone::yield() && cone::yield() && cone::yield() && cone::yield() && cone::yield());
    }
}
