#include <yandex_io/libs/configuration/configuration_helper.h>

#include <yandex_io/tests/testlib/test_utils.h>

#include <library/cpp/testing/unittest/registar.h>
#include <library/cpp/testing/unittest/tests_data.h>

using namespace quasar;

Y_UNIT_TEST_SUITE(ConfigurationHandlerTests) {
    Y_UNIT_TEST(testConfigUpdate)
    {
        auto initialConfig = parseJson(R"({
        "number": 1,
        "string": "asd",
        "array": ["1", "2", "3"],
        "object": {
            "var": "value"
        },
        "object2": {
            "var2": "value2"
        }
    })");

        ConfigurationHelper helper(initialConfig);

        auto config = parseJson(R"({
        "number": 2,
        "string": "asd",
        "array": ["1", "2"],
        "object": {
            "var": "val",
            "new_var": "new_val"
        },
        "object2": {
            "var2": "value2"
        }
    })");

        auto update = helper.getConfigurationUpdate(config);
        auto expectedUpdate = parseJson(R"({
        "number": 2,
        "array": ["1", "2"],
        "object": {
            "var": "val",
            "new_var": "new_val"
        }
    })");
        UNIT_ASSERT_VALUES_EQUAL(jsonToString(update), jsonToString(expectedUpdate));

        auto emptyConfig = Json::Value();
        update = helper.getConfigurationUpdate(emptyConfig);

        UNIT_ASSERT_VALUES_EQUAL(jsonToString(helper.getCurrentConfig()), jsonToString(initialConfig));

        expectedUpdate = parseJson(R"({
        "number": 1,
        "array": ["1", "2", "3"],
        "object": {
            "var": "value"
        }
    })");
        UNIT_ASSERT_VALUES_EQUAL(jsonToString(update), jsonToString(expectedUpdate));
    }
}
