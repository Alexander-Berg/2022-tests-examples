#include <library/cpp/testing/unittest/registar.h>
#include <library/cpp/testing/unittest/env.h>
#include <balancer/serval/tests/helpers.h>

static NSv::TThreadLocalRoot TLSRoot;

Y_UNIT_TEST_SUITE(Geobase) {
    UT_ACTION("laas-1") {
        auto rqh = req->Head();
        UNIT_ASSERT(rqh);
        EXPECT_HEADER(*rqh, "x-url-prefix", "http://yandex.ru/search/touch/");
        return req->WriteHead({200, {{"x-region-city", "123"}}}) && req->Close();
    }

    UT_ACTION("backend-1") {
        auto rqh = req->Head();
        UNIT_ASSERT(rqh);
        EXPECT_HEADER(*rqh, "x-laas-answered", "1");
        EXPECT_HEADER(*rqh, "x-region-city", "123");
        EXPECT_HEADER(*rqh, "x-url-prefix", "nope");
        return req->WriteHead(200) && req->Write("OK\n") && req->Close();
    }

    Y_UNIT_TEST(OK) {
        auto mod = FromConfig(YAML::Load("- geobase: laas-1\n" "- backend-1\n"));
        ServerClient(ExpectError(EREQDONE, mod), [&](NSv::IConnection& c) {
            EXPECT_RESPONSE(c.Request({"GET", "/search/touch/?text=test",
                {{":authority", "yandex.ru"}, {"x-url-prefix", "nope"}, {"x-forwarded-for-y", "127.0.0.1"}}}), 200, "OK\n");
        });
    }

    UT_ACTION("laas-2") {
        return !mun_error(ERTIMEDOUT, "fake timeout");
    }

    UT_ACTION("backend-2") {
        auto rqh = req->Head();
        UNIT_ASSERT(rqh);
        UNIT_ASSERT(rqh->find("x-region-city") == rqh->end());
        EXPECT_HEADER(*rqh, "x-laas-answered", "0");
        return req->WriteHead(200) && req->Write("OK\n") && req->Close();
    }

    Y_UNIT_TEST(Failed) {
        auto mod = FromConfig(YAML::Load("- geobase: laas-2\n" "- backend-2\n"));
        ServerClient(ExpectError(EREQDONE, mod), [&](NSv::IConnection& c) {
            EXPECT_RESPONSE(c.Request({"GET", "/search/touch/?text=test",
                {{":authority", "yandex.ru"}, {"x-region-city", "123"}, {"x-forwarded-for-y", "127.0.0.1"}}}), 200, "OK\n");
        });
    }
}
