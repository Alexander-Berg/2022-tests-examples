#include <yandex_io/libs/configuration/configuration.h>
#include <yandex_io/libs/configuration/eval.h>

#include <yandex_io/libs/json_utils/json_utils.h>

#include <yandex_io/tests/testlib/test_utils.h>

#include <library/cpp/testing/unittest/registar.h>
#include <library/cpp/testing/unittest/tests_data.h>

#include <unordered_map>

using namespace quasar;

Y_UNIT_TEST_SUITE(ConfigurationTests) {
    Y_UNIT_TEST(testBulkReplace)
    {
        std::unordered_map<std::string, std::string> mp;
        mp["A"] = "1";
        mp["B"] = "2";
        mp["C"] = "3";
        mp["D"] = "${D}";
        UNIT_ASSERT_VALUES_EQUAL("123", bulkReplace("${A}${B}${C}", mp));
        UNIT_ASSERT_VALUES_EQUAL("A1$A$", bulkReplace("A${A}$A$", mp));
        UNIT_ASSERT_VALUES_EQUAL("${D}", bulkReplace("${D}", mp));
    }

    Y_UNIT_TEST(testClosure)
    {
        std::unordered_map<std::string, std::string> mp;
        mp["A"] = "1";
        mp["B"] = "${A}2${A}";
        mp["C"] = "${B}3${A}";
        closure(mp);
        UNIT_ASSERT_VALUES_EQUAL("1", mp["A"]);
        UNIT_ASSERT_VALUES_EQUAL("121", mp["B"]);
        UNIT_ASSERT_VALUES_EQUAL("12131", mp["C"]);
    }

    Y_UNIT_TEST(testPreprocess)
    {
        const char* strPre = R"(
{
    "patterns": { "A": "1", "B": "2${A}"},
    "key": {
        "value": ["${B}"]
    }
}
    )";
        const char* strPost = R"({"key":{"value":["21"]}}
)";
        auto jsonPre = parseJson(strPre);
        auto jsonPost = YandexIO::preprocessConfig(jsonPre, {});
        auto str = jsonToString(jsonPost);
        UNIT_ASSERT_VALUES_EQUAL(strPost, str);
    }
}
