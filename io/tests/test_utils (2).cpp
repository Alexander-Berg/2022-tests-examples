#include <yandex_io/libs/base/utils.h>

#include <yandex_io/libs/json_utils/json_utils.h>

#include <yandex_io/tests/testlib/unittest_helper/unit_test_fixture.h>

#include <library/cpp/testing/unittest/registar.h>

#include <iostream>
#include <unordered_map>

using namespace quasar;

Y_UNIT_TEST_SUITE_F(TestUtils, QuasarUnitTestFixture) {
    Y_UNIT_TEST(testUtilsTrim)
    {
        UNIT_ASSERT_VALUES_EQUAL(trim("  abs  b add     "), "abs  b add");
        UNIT_ASSERT_VALUES_EQUAL(trim("abs  b add"), "abs  b add");
        UNIT_ASSERT_VALUES_EQUAL(trim("ab add "), "ab add");
        UNIT_ASSERT_VALUES_EQUAL(trim(" ab add"), "ab add");
        UNIT_ASSERT_VALUES_EQUAL(trim(" \t\nab add\t \n "), "ab add");
    }

    Y_UNIT_TEST(testUtilsUrlParams)
    {
        auto params = getUrlParams(
            "https://www.jdoodle.com/online-java-compiler?"
            "a=3&b=lol&{%22bu%22:3}={%22der%22:%20[{%22action%22:%20%22lol%22},%20{%22action%22:%20%22troll}]}");
        UNIT_ASSERT_VALUES_EQUAL(params.size(), 3u);
        UNIT_ASSERT_VALUES_EQUAL(params["a"], "3");
        UNIT_ASSERT_VALUES_EQUAL(params["b"], "lol");
        UNIT_ASSERT_VALUES_EQUAL(params["{\"bu\":3}"], "{\"der\": [{\"action\": \"lol\"}, {\"action\": \"troll}]}");
    }

    Y_UNIT_TEST(testUtilsGetStringSafe)
    {
        UNIT_ASSERT(getStringSafe(R"=({"foo": {"bar": "baz"}})=", ".foo.bar", "123") == "baz");
        UNIT_ASSERT(getStringSafe(R"=({"foo": {"bar": "baz"}})=", ".foo.pew", "123") == "123");
        UNIT_ASSERT(getStringSafe("", ".foo.bar", "123") == "123");
    }

    Y_UNIT_TEST(testUtilsExecuteWithOutput)
    {
        UNIT_ASSERT_VALUES_EQUAL(executeWithOutput("echo 1234 test"), "1234 test\n");
        UNIT_ASSERT_VALUES_EQUAL(executeWithOutput("notExistingCommand"), "");
        UNIT_ASSERT_VALUES_EQUAL(executeWithOutput("echo 123 > tmpfile"), "");
        UNIT_ASSERT_VALUES_EQUAL(executeWithOutput("echo йцукен && echo qwerty"), "йцукен\nqwerty\n");
        UNIT_ASSERT_VALUES_EQUAL(executeWithOutput("echo йцукен || echo qwerty"), "йцукен\n");
        UNIT_ASSERT_VALUES_EQUAL(executeWithOutput("notExistingCommand || echo qwerty"), "qwerty\n");

        std::remove("tmpfile");
    }

    Y_UNIT_TEST(testUtilsmaskToken)
    {
        UNIT_ASSERT_VALUES_EQUAL(maskToken("abcdefghij"), "abcde*****");
        UNIT_ASSERT_VALUES_EQUAL(maskToken("abcdefghi"), "abcd*****");
        UNIT_ASSERT_VALUES_EQUAL(maskToken(""), "");
    }

    Y_UNIT_TEST(testUtilsAddGETParam)
    {
        // std::string addGETParam(const std::string& url, const std::string& key, const std::string& value);
        UNIT_ASSERT_VALUES_EQUAL(addGETParam("http://example.com", "vsid", "blablabla", true), "http://example.com?vsid=blablabla");
        UNIT_ASSERT_VALUES_EQUAL(addGETParam("http://example.com#super", "vsid", "blablabla", true), "http://example.com?vsid=blablabla#super");
        UNIT_ASSERT_VALUES_EQUAL(addGETParam("http://example.com?key=value", "vsid", "blablabla", true), "http://example.com?key=value&vsid=blablabla");
        UNIT_ASSERT_VALUES_EQUAL(addGETParam("http://example.com?key=value#super", "vsid", "blablabla", true), "http://example.com?key=value&vsid=blablabla#super");
        UNIT_ASSERT_VALUES_EQUAL(addGETParam("http://example.com?key=value&vsid=1", "vsid", "blablabla", true), "http://example.com?key=value&vsid=blablabla");
        UNIT_ASSERT_VALUES_EQUAL(addGETParam("http://example.com?key=value&vsid=1#super", "vsid", "blablabla", true), "http://example.com?key=value&vsid=blablabla#super");
        UNIT_ASSERT_VALUES_EQUAL(addGETParam("http://example.com?vsid=1&key=value", "vsid", "blablabla", true), "http://example.com?vsid=blablabla&key=value");
        UNIT_ASSERT_VALUES_EQUAL(addGETParam("http://example.com?vsid=1&key=value#super", "vsid", "blablabla", true), "http://example.com?vsid=blablabla&key=value#super");

        UNIT_ASSERT_VALUES_EQUAL(addGETParam("http://example.com", "vsid", "blablabla", false), "http://example.com?vsid=blablabla");
        UNIT_ASSERT_VALUES_EQUAL(addGETParam("http://example.com#super", "vsid", "blablabla", false), "http://example.com?vsid=blablabla#super");
        UNIT_ASSERT_VALUES_EQUAL(addGETParam("http://example.com?key=value", "vsid", "blablabla", false), "http://example.com?key=value&vsid=blablabla");
        UNIT_ASSERT_VALUES_EQUAL(addGETParam("http://example.com?key=value#super", "vsid", "blablabla", false), "http://example.com?key=value&vsid=blablabla#super");
        UNIT_ASSERT_VALUES_EQUAL(addGETParam("http://example.com?key=value&vsid=1", "vsid", "blablabla", false), "http://example.com?key=value&vsid=1");
        UNIT_ASSERT_VALUES_EQUAL(addGETParam("http://example.com?key=value&vsid=1#super", "vsid", "blablabla", false), "http://example.com?key=value&vsid=1#super");
        UNIT_ASSERT_VALUES_EQUAL(addGETParam("http://example.com?vsid=1&key=value", "vsid", "blablabla", false), "http://example.com?vsid=1&key=value");
        UNIT_ASSERT_VALUES_EQUAL(addGETParam("http://example.com?vsid=1&key=value#super", "vsid", "blablabla", false), "http://example.com?vsid=1&key=value#super");
    }

    Y_UNIT_TEST(testGzip) {
        const std::string text = "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.";

        const auto compressed = gzipCompress(text);

        UNIT_ASSERT(compressed.size() < text.size());
        UNIT_ASSERT_VALUES_EQUAL(text, gzipDecompress(compressed));
    }

    Y_UNIT_TEST(testUtilsSplit)
    {
        {
            auto parts = split("1.2.3.4", ".");
            UNIT_ASSERT_VALUES_EQUAL(parts.size(), 4);
            UNIT_ASSERT_VALUES_EQUAL(parts[0], "1");
            UNIT_ASSERT_VALUES_EQUAL(parts[1], "2");
            UNIT_ASSERT_VALUES_EQUAL(parts[2], "3");
            UNIT_ASSERT_VALUES_EQUAL(parts[3], "4");
        }
        {
            auto parts = split("1..4", ".");
            UNIT_ASSERT_VALUES_EQUAL(parts.size(), 3);
            UNIT_ASSERT_VALUES_EQUAL(parts[0], "1");
            UNIT_ASSERT_VALUES_EQUAL(parts[1], "");
            UNIT_ASSERT_VALUES_EQUAL(parts[2], "4");
        }
        {
            auto parts = split("", ".");
            UNIT_ASSERT_VALUES_EQUAL(parts.size(), 1);
            UNIT_ASSERT_VALUES_EQUAL(parts[0], "");
        }
        {
            auto parts = split("1.2.3", ".", 0);
            UNIT_ASSERT_VALUES_EQUAL(parts.size(), 3);
            UNIT_ASSERT_VALUES_EQUAL(parts[0], "1");
            UNIT_ASSERT_VALUES_EQUAL(parts[1], "2");
            UNIT_ASSERT_VALUES_EQUAL(parts[2], "3");
        }
        {
            auto parts = split("1.2.3", ".", 1);
            UNIT_ASSERT_VALUES_EQUAL(parts.size(), 1);
            UNIT_ASSERT_VALUES_EQUAL(parts[0], "1.2.3");
        }
        {
            auto parts = split("1.2.3", ".", 2);
            UNIT_ASSERT_VALUES_EQUAL(parts.size(), 2);
            UNIT_ASSERT_VALUES_EQUAL(parts[0], "1");
            UNIT_ASSERT_VALUES_EQUAL(parts[1], "2.3");
        }
        {
            auto parts = split("1.2.", ".", 2);
            UNIT_ASSERT_VALUES_EQUAL(parts.size(), 2);
            UNIT_ASSERT_VALUES_EQUAL(parts[0], "1");
            UNIT_ASSERT_VALUES_EQUAL(parts[1], "2.");
        }
        {
            auto parts = split("1..2.", ".", 2);
            UNIT_ASSERT_VALUES_EQUAL(parts.size(), 2);
            UNIT_ASSERT_VALUES_EQUAL(parts[0], "1");
            UNIT_ASSERT_VALUES_EQUAL(parts[1], ".2.");
        }
        {
            auto parts = split("1.2.", ".");
            UNIT_ASSERT_VALUES_EQUAL(parts.size(), 3);
            UNIT_ASSERT_VALUES_EQUAL(parts[0], "1");
            UNIT_ASSERT_VALUES_EQUAL(parts[1], "2");
            UNIT_ASSERT_VALUES_EQUAL(parts[2], "");
        }
    }
}
