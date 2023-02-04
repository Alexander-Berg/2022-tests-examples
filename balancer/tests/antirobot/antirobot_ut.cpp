#include <library/cpp/testing/unittest/registar.h>
#include <library/cpp/testing/unittest/env.h>
#include <balancer/serval/tests/helpers.h>

static NSv::TThreadLocalRoot TLSRoot;

SV_DEFINE_ACTION("expect-payload", [](const YAML::Node& args, NSv::TAuxData&) {
    return [expect = NSv::Required<TString>(args.begin()->second)](NSv::IStreamPtr& s) {
        auto data = NSv::ReadFrom(*s);
        UNIT_ASSERT(data);
        UNIT_ASSERT_VALUES_EQUAL(*data, expect);
        return true;
    };
});

Y_UNIT_TEST_SUITE(Antirobot) {
    UT_ACTION("backend-1") {
        auto rqh = req->Head();
        UNIT_ASSERT(rqh);
        EXPECT_HEADER(*rqh, "x-yandex-suspected-robot", "0");
        return req->WriteHead(200) && req->Write("OK\n") && req->Close();
    }

    UT_ACTION("fail") {
        return !mun_error(EINVAL, "failure mode");
    }

    Y_UNIT_TEST(OK) {
        auto mod = FromConfig(YAML::Load(
            "- antirobot:\n"
            "  - expect-payload: \"GET / HTTP/1.1\\r\\nhost: yandex.ru\\r\\n\\r\\n\"\n"
            "  - {const: ok, x-yandex-suspected-robot: 0}\n"
            "- backend-1"));
        ServerClient(ExpectError(EREQDONE, mod), [&](NSv::IConnection& c) {
            EXPECT_RESPONSE(c.Request({"GET", "/", {{":authority", "yandex.ru"}}}), 200, "OK\n");
        });
    }

    Y_UNIT_TEST(OKWithPayload) {
        auto mod = FromConfig(YAML::Load(
            "- antirobot:\n"
            "  - expect-payload: \"POST / HTTP/1.1\\r\\nhost: yandex.ru\\r\\n\\r\\nsome-pay\"\n"
            "  - {const: ok, x-yandex-suspected-robot: 0}\n"
            "  cut-payload: 8\n"
            "- expect-payload: some-payload\n"
            "- const: ok"));
        ServerClient(ExpectError(EREQDONE, mod), [&](NSv::IConnection& c) {
            auto req = c.Request({"POST", "/", {{":authority", "yandex.ru"}}}, true);
            UNIT_ASSERT(req);
            UNIT_ASSERT(req->Write("some-payload"));
            UNIT_ASSERT(req->Close());
            EXPECT_RESPONSE(req, 200, "ok");
        });
    }

    Y_UNIT_TEST(ReplayPayload) {
        auto mod = FromConfig(YAML::Load(
            "- antirobot:\n"
            "  - expect-payload: \"POST / HTTP/1.1\\r\\nhost: yandex.ru\\r\\n\\r\\nsome-payload\"\n"
            "  - {const: ok, x-yandex-suspected-robot: 0}\n"
            "- proxy:\n"
            "  - inf: [{expect-payload: some-payload}, fail]\n"
            "  - [{expect-payload: some-payload}, {const: ok}]"));
        ServerClient(ExpectError(EREQDONE, mod), [&](NSv::IConnection& c) {
            auto req = c.Request({"POST", "/", {{":authority", "yandex.ru"}}}, true);
            UNIT_ASSERT(req);
            UNIT_ASSERT(req->Write("some-payload"));
            UNIT_ASSERT(req->Close());
            EXPECT_RESPONSE(req, 200, "ok");
        });
    }

    Y_UNIT_TEST(Failing) {
        auto mod = FromConfig(YAML::Load(
            "- antirobot:\n"
            "  - expect-payload: \"GET / HTTP/1.1\\r\\nhost: yandex.ru\\r\\n\\r\\n\"\n"
            "  - fail\n"
            "- const: ok"));
        ServerClient(ExpectError(EREQDONE, mod), [&](NSv::IConnection& c) {
            EXPECT_RESPONSE(c.Request({"GET", "/", {{":authority", "yandex.ru"}}}), 200, "ok");
        });
    }

    Y_UNIT_TEST(Robot) {
        auto mod = FromConfig(YAML::Load(
            "- antirobot:\n"
            "  - expect-payload: \"GET / HTTP/1.1\\r\\nhost: yandex.ru\\r\\n\\r\\n\"\n"
            "  - {const: robot, x-yandex-suspected-robot: 1, x-yandex-internal-request: 0, x-forwardtouser-y: 1}\n"
            "- fail"));
        ServerClient(ExpectError(EREQDONE, mod), [&](NSv::IConnection& c) {
            auto req = c.Request({"GET", "/", {{":authority", "yandex.ru"}}});
            EXPECT_RESPONSE(req, 200, "robot");
            auto rqh = req->Head();
            UNIT_ASSERT(rqh);
            UNIT_ASSERT(rqh->find("x-yandex-internal-request") == rqh->end());
            UNIT_ASSERT(rqh->find("x-yandex-suspected-robot") == rqh->end());
            UNIT_ASSERT(rqh->find("x-forwardtouser-y") == rqh->end());
        });
    }
}
