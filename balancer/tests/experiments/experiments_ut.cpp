#include <library/cpp/testing/unittest/registar.h>
#include <library/cpp/testing/unittest/env.h>
#include <balancer/serval/tests/helpers.h>

static NSv::TThreadLocalRoot TLSRoot;

Y_UNIT_TEST_SUITE(Experiments) {
    static const NSv::THeaderVector ExpHeaders = {
        {"x-yandex-expconfigversion", "1"},
        {"x-yandex-expconfigversion-pre", "2"},
        {"x-yandex-expboxes-pre", "4"},
        {"x-yandex-expflags", "5"},
        {"x-yandex-expflags-pre", "6"},
        {"x-yandex-is-staff-login", "7"},
    };

    UT_ACTION("uaas-1") {
        auto rqh = req->Head();
        UNIT_ASSERT(rqh);
        UNIT_ASSERT(rqh->find("x-yandex-expboxes") == rqh->end());
        UNIT_ASSERT_VALUES_EQUAL(rqh->Method, "POST");
        UNIT_ASSERT_VALUES_EQUAL(rqh->PathWithQuery, "/search/touch/?text=test");
        UNIT_ASSERT(req->AtEnd());
        return req->WriteHead({200, ExpHeaders}) && req->Close();
    }

    UT_ACTION("backend-1") {
        auto rqh = req->Head();
        UNIT_ASSERT(rqh);
        UNIT_ASSERT(rqh->find("x-yandex-expboxes") == rqh->end());
        for (const auto& header : ExpHeaders)
            EXPECT_HEADER(*rqh, header.first, header.second);
        auto payload = NSv::ReadFrom(*req);
        UNIT_ASSERT(payload);
        UNIT_ASSERT_VALUES_EQUAL(*payload, "OK?\n");
        return req->WriteHead(200) && req->Write("OK\n") && req->Close();
    }

    Y_UNIT_TEST(OK) {
        auto mod = FromConfig(YAML::Load("- experiments: uaas-1\n" "- backend-1\n"));
        ServerClient(ExpectError(EREQDONE, mod), [&](NSv::IConnection& c) {
            auto req = c.Request({"POST", "/search/touch/?text=test",
                {{":authority", "yandex.ru"}, {":scheme", "https"}, {"x-yandex-expboxes", "3"}}}, true);
            UNIT_ASSERT(req);
            UNIT_ASSERT(req->Write("OK?\n"));
            UNIT_ASSERT(req->Close());
            EXPECT_RESPONSE(req, 200, "OK\n");
        });
    }

    UT_ACTION("uaas-2") {
        return !mun_error(ERTIMEDOUT, "fake timeout");
    }

    UT_ACTION("backend-2") {
        auto rqh = req->Head();
        UNIT_ASSERT(rqh);
        UNIT_ASSERT(rqh->find("x-yandex-is-staff-login") == rqh->end());
        return req->WriteHead(200) && req->Write("OK\n") && req->Close();
    }

    Y_UNIT_TEST(Failed) {
        auto mod = FromConfig(YAML::Load("- experiments: uaas-2\n" "- backend-2\n"));
        ServerClient(ExpectError(EREQDONE, mod), [&](NSv::IConnection& c) {
            EXPECT_RESPONSE(c.Request({"GET", "/search/touch/?text=test",
                {{":authority", "yandex.ru"}, {":scheme", "https"}, {"x-yandex-is-staff-login", "1"}}}), 200, "OK\n");
        });
    }
}
