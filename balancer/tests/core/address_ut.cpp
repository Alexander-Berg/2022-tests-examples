#include <library/cpp/testing/unittest/registar.h>
#include <library/cpp/testing/unittest/env.h>
#include <balancer/serval/core/address.h>

Y_UNIT_TEST_SUITE(Address) {
    static void CheckUrl(TString str, TStringBuf scheme, TStringBuf host, ui32 port, TStringBuf path,
                         TStringBuf query, TStringBuf fragment, TMaybe<ui32> timeout = {}, bool withTimeout = true) {
        auto res = NSv::URL::Parse(str, withTimeout);
        UNIT_ASSERT(res);
        UNIT_ASSERT_VALUES_EQUAL(res->Scheme, scheme);
        UNIT_ASSERT_VALUES_EQUAL(res->Host, host);
        UNIT_ASSERT_VALUES_EQUAL(res->Port, port);
        UNIT_ASSERT_VALUES_EQUAL(res->Path, path);
        UNIT_ASSERT_VALUES_EQUAL(res->Query, query);
        UNIT_ASSERT_VALUES_EQUAL(res->Fragment, fragment);
        UNIT_ASSERT_VALUES_EQUAL(res->Timeout, timeout);
        if (withTimeout && !timeout)
            CheckUrl(str, scheme, host, port, path, query, fragment, timeout, false);
    }

    Y_UNIT_TEST(Parsing) {
        CheckUrl("asd.qwe",                      "",     "asd.qwe", 0,  "",     "",    ""   );
        CheckUrl("http://asd.qwe",               "http", "asd.qwe", 80, "",     "",    ""   );
        CheckUrl("asd.qwe:1",                    "",     "asd.qwe", 1,  "",     "",    ""   );
        CheckUrl("http://asd.qwe:1",             "http", "asd.qwe", 1,  "",     "",    ""   );
        CheckUrl("asd.qwe/zxc",                  "",     "asd.qwe", 0,  "/zxc", "",    ""   );
        CheckUrl("http://asd.qwe/zxc",           "http", "asd.qwe", 80, "/zxc", "",    ""   );
        CheckUrl("asd.qwe:1/zxc",                "",     "asd.qwe", 1,  "/zxc", "",    ""   );
        CheckUrl("http://asd.qwe:1/zxc",         "http", "asd.qwe", 1,  "/zxc", "",    ""   );
        CheckUrl("asd.qwe/zxc?vbn",              "",     "asd.qwe", 0,  "/zxc", "vbn", ""   );
        CheckUrl("http://asd.qwe/zxc?vbn",       "http", "asd.qwe", 80, "/zxc", "vbn", ""   );
        CheckUrl("asd.qwe:1/zxc?vbn",            "",     "asd.qwe", 1,  "/zxc", "vbn", ""   );
        CheckUrl("http://asd.qwe:1/zxc?vbn",     "http", "asd.qwe", 1,  "/zxc", "vbn", ""   );
        CheckUrl("asd.qwe/zxc#fgh",              "",     "asd.qwe", 0,  "/zxc", "",    "fgh");
        CheckUrl("http://asd.qwe/zxc#fgh",       "http", "asd.qwe", 80, "/zxc", "",    "fgh");
        CheckUrl("asd.qwe:1/zxc#fgh",            "",     "asd.qwe", 1,  "/zxc", "",    "fgh");
        CheckUrl("http://asd.qwe:1/zxc#fgh",     "http", "asd.qwe", 1,  "/zxc", "",    "fgh");
        CheckUrl("asd.qwe/zxc?vbn#fgh",          "",     "asd.qwe", 0,  "/zxc", "vbn", "fgh");
        CheckUrl("http://asd.qwe/zxc?vbn#fgh",   "http", "asd.qwe", 80, "/zxc", "vbn", "fgh");
        CheckUrl("asd.qwe:1/zxc?vbn#fgh",        "",     "asd.qwe", 1,  "/zxc", "vbn", "fgh");
        CheckUrl("http://asd.qwe:1/zxc?vbn#fgh", "http", "asd.qwe", 1,  "/zxc", "vbn", "fgh");
        CheckUrl("asd.qwe/zxc:123",              "",     "asd.qwe", 0,  "/zxc", "",    "",    123);
        CheckUrl("asd.qwe/zxc?vbn:123",          "",     "asd.qwe", 0,  "/zxc", "vbn", "",    123);
        CheckUrl("asd.qwe/zxc#fgh:123",          "",     "asd.qwe", 0,  "/zxc", "",    "fgh", 123);
        CheckUrl("asd.qwe/zxc?vbn#fgh:123",      "",     "asd.qwe", 0,  "/zxc", "vbn", "fgh", 123);
        CheckUrl("asd.qwe:1:123",                "",     "asd.qwe", 1,  "",     "",    "",    123);
    }

    static void CheckNormPath(TString in, TStringBuf expect) {
        UNIT_ASSERT_VALUES_EQUAL(NSv::NormalizePathInPlace(in), expect);
    }

    Y_UNIT_TEST(NormalizePath) {
        CheckNormPath("", "");
        CheckNormPath("/", "/");
        CheckNormPath("//", "/");
        CheckNormPath("asd", "asd");
        CheckNormPath("/asd", "/asd");
        CheckNormPath("asd///zxc/vbn", "asd/zxc/vbn");
        CheckNormPath("/asd///zxc/////vbn", "/asd/zxc/vbn");
        CheckNormPath(".", "");
        CheckNormPath("/.", "/");
        CheckNormPath("/./", "/");
        CheckNormPath("asd/.", "asd/");
        CheckNormPath("/asd/.", "/asd/");
        CheckNormPath("/asd/./zxc///.///////./vbn/././.", "/asd/zxc/vbn/");
        CheckNormPath("..", "");
        CheckNormPath("/..", "/");
        CheckNormPath("/../", "/");
        CheckNormPath("asd/..", "");
        CheckNormPath("/asd/..", "/");
        CheckNormPath("/asd/qwe/../../../../", "/");
        CheckNormPath("/asd/./zxc///../vbn/.", "/asd/vbn/");
        CheckNormPath("/asd/..#qwe/../zxc", "/#qwe/../zxc");
    }

    Y_UNIT_TEST(IP) {
        UNIT_ASSERT_VALUES_EQUAL(NSv::IP::Parse("1::2", 12345).FormatFull(), "[1::2]:12345");
        UNIT_ASSERT_VALUES_EQUAL(NSv::IP::Parse("10.11.12.13", 12345).FormatFull(), "10.11.12.13:12345");
    }

    Y_UNIT_TEST(Resolve) {
        UNIT_ASSERT(NSv::IP::Resolve("yandex.ru").size() > 0);
        UNIT_ASSERT(NSv::IP::Resolve(".....").size() == 0);
        UNIT_ASSERT(NSv::IP::Resolve("asb.agjskhdj.agvjhk.com").size() == 0);
    }
}
