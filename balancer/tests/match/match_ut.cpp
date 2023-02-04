#include <library/cpp/testing/unittest/registar.h>
#include <library/cpp/testing/unittest/env.h>
#include <balancer/serval/tests/helpers.h>

static NSv::TThreadLocalRoot TLSRoot;

Y_UNIT_TEST_SUITE(Match) {
    Y_UNIT_TEST(Path) {
        auto mod = FromConfig(YAML::Load(
            "- match: :path\n"
            "  /a: {const: a}\n"
            "  /b: {const: b}\n"
            "- const: c"));
        ServerClient(ExpectError(EREQDONE, mod), [&](NSv::IConnection& c) {
            EXPECT_RESPONSE(c.Request({"GET", "/", {}}), 200, "c");
            EXPECT_RESPONSE(c.Request({"GET", "/a", {}}), 200, "a");
            EXPECT_RESPONSE(c.Request({"GET", "/b", {}}), 200, "b");
        });
    }

    Y_UNIT_TEST(Header) {
        auto mod = FromConfig(YAML::Load(
            "- match: x-something\n"
            "  a: {const: a}\n"
            "  b: {const: b}\n"
            "- const: c"));
        ServerClient(ExpectError(EREQDONE, mod), [&](NSv::IConnection& c) {
            EXPECT_RESPONSE(c.Request({"GET", "/", {}}), 200, "c");
            EXPECT_RESPONSE(c.Request({"GET", "/", {{"x-something", "a"}}}), 200, "a");
            EXPECT_RESPONSE(c.Request({"GET", "/", {{"x-something", "b"}}}), 200, "b");
        });
    }

    Y_UNIT_TEST(Method) {
        auto mod = FromConfig(YAML::Load(
            "- match: :method\n"
            "  PUT: {const: a}\n"
            "  POST: {const: b}\n"
            "- const: c"));
        ServerClient(ExpectError(EREQDONE, mod), [&](NSv::IConnection& c) {
            EXPECT_RESPONSE(c.Request({"GET", "/", {}}), 200, "c");
            EXPECT_RESPONSE(c.Request({"PUT", "/", {}}), 200, "a");
            EXPECT_RESPONSE(c.Request({"POST", "/", {}}), 200, "b");
        });
    }

    Y_UNIT_TEST(MatchMany) {
        auto mod = FromConfig(YAML::Load(
            "- match: :path\n"
            "  /a.*: {response-headers: [x-a: a]}\n"
            "  /a.*: {response-headers: [x-b: b]}\n"
            "- const: c"));
        ServerClient(ExpectError(EREQDONE, mod), [&](NSv::IConnection& c) {
            EXPECT_RESPONSE(c.Request({"GET", "/", {}}), 200, "c");
            EXPECT_RESPONSE(c.Request({"GET", "/a", {}}), 200, "c", {"x-a", "a"}, {"x-b", "b"});
        });
    }

    Y_UNIT_TEST(Flags) {
        auto mod = FromConfig(YAML::Load(
            "- match: :path\n"
            "  !i /a: {const: a}\n"
            "  !p /b: {const: b}\n"
            "  !g /c: {const: c}\n"
            "  !ip /d: {const: d}\n"
            "  !n /e: {const: f}\n"
            "- const: e"));
        ServerClient(ExpectError(EREQDONE, mod), [&](NSv::IConnection& c) {
            EXPECT_RESPONSE(c.Request({"GET", "/A", {}}), 200, "a");
            EXPECT_RESPONSE(c.Request({"GET", "/b", {}}), 200, "b");
            EXPECT_RESPONSE(c.Request({"GET", "/b/x", {}}), 200, "b");
            EXPECT_RESPONSE(c.Request({"GET", "/ba/cd", {}}), 200, "c");
            EXPECT_RESPONSE(c.Request({"GET", "/D/q", {}}), 200, "d");
            EXPECT_RESPONSE(c.Request({"GET", "/e", {}}), 200, "e");
            EXPECT_RESPONSE(c.Request({"GET", "/f", {}}), 200, "f");
            EXPECT_RESPONSE(c.Request({"GET", "/g", {}}), 200, "f");
        });
    }

    Y_UNIT_TEST(Source) {
        auto mod = FromConfig(YAML::Load(
            "- match: :source\n"
            "  \"[200a::]/[ffff::]\": {const: a}\n"
            "  [\"[200a::]/32\"]: {const: b}\n"
            "  [[200b::/64]]: {const: c}\n"
            "- const: d"));
        ServerClient(ExpectError(EREQDONE, mod), [&](NSv::IConnection& c) {
            EXPECT_RESPONSE(c.Request({"GET", "/", {{"x-forwarded-for", "[200b::1234]"}}}), 200, "c");
        });
    }
}
