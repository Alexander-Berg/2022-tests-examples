#include <library/cpp/testing/unittest/registar.h>
#include <library/cpp/testing/unittest/env.h>
#include <balancer/serval/tests/helpers.h>

static NSv::TThreadLocalRoot TLSRoot;

Y_UNIT_TEST_SUITE(Headers) {
    UT_ACTION("backend-1") {
        auto rqh = req->Head();
        UNIT_ASSERT(rqh);
        return req->WriteHead({200, *rqh}) && req->Write("OK\n") && req->Close();
    }

    Y_UNIT_TEST(Request) {
        auto mod = FromConfig(YAML::Load(
            "- request-headers:\n"
            "  - !erase x-a-.*\n"
            "  - !erase x-a\n"
            "  - x-b: xxx\n"
            "  - !weak x-c: yes\n"
            "  - !weak x-d: no\n"
            "  - !if-has x-e: [{x-f: no}]\n"
            "  - !if-has x-b: [{x-e: yes}]\n"
            "  - !if-has-none x-d: [{x-f: no}]\n"
            "  - !if-has-none x-f: [{x-g: yes}]\n"
            "  - x-h: !header x-g\n" // copying an existing header
            "  - x-i: !header x-f\n" // copying a nonexisting header
            "  - !weak x-j: !header x-c\n" // weak copying to nonexisting
            "  - !weak x-g: !header x-b\n" // weak copying to existing
            "  - x-k: !cookie a\n"
            "  - x-l: !cookie b\n"
            "- backend-1\n"));
        ServerClient(ExpectError(EREQDONE, mod), [&](NSv::IConnection& c) {
            EXPECT_RESPONSE(c.Request({"GET", "/", {
                {"x-a", "no"},
                {"x-a-2", "no"},
                {"x-d", "yes"},
                {"cookie", "a; b=c;"}
            }}), 200, "OK\n", {
                {"x-b", "xxx"},
                {"x-c", "yes"},
                {"x-d", "yes"},
                {"x-g", "yes"},
                {"x-h", "yes"},
                {"x-j", "yes"},
                {"x-k", ""},
                {"x-l", "c"},
            });
        });
    }

    Y_UNIT_TEST(Response) {
        auto mod = FromConfig(YAML::Load(
            "- response-headers:\n"
            "  - !erase x-a-.*\n"
            "  - !erase x-a\n"
            "  - x-b: yes\n"
            "  - !weak x-c: yes\n"
            "  - !weak x-d: no\n"
            "  - x-ip: !ip nil\n"
            "  - x-port: !port nil\n"
            "- const: \"OK\\n\"\n"
            "  x-a: no\n"
            "  x-a-2: no\n"
            "  x-d: yes"));
        ServerClient(ExpectError(EREQDONE, mod), [&](NSv::IConnection& c) {
            auto rsp = c.Request({"GET", "/", {{"x-forwarded-for", "[::1]:12345"}}});
            EXPECT_RESPONSE(rsp, 200, "OK\n");
            auto rsh = rsp->Head();
            UNIT_ASSERT(rsh->find("x-a") == rsh->end());
            UNIT_ASSERT(rsh->find("x-a-2") == rsh->end());
            EXPECT_HEADER(*rsh, "x-b", "yes");
            EXPECT_HEADER(*rsh, "x-c", "yes");
            EXPECT_HEADER(*rsh, "x-d", "yes");
            EXPECT_HEADER(*rsh, "x-ip", "::1");
            EXPECT_HEADER(*rsh, "x-port", "12345");
        });
    }
}
