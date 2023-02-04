#include <yandex_io/services/aliced/capabilities/alice_capability/vins/vins_utils.h>

#include <yandex_io/libs/base/utils.h>
#include <yandex_io/libs/json_utils/json_utils.h>
#include <yandex_io/tests/testlib/unittest_helper/unit_test_fixture.h>

#include <json/json.h>

#include <library/cpp/testing/unittest/registar.h>

using namespace quasar;

Y_UNIT_TEST_SUITE_F(VinsUtilsTest, QuasarUnitTestFixture) {
    Y_UNIT_TEST(testEnrichEmptyCard) {
        Json::Value payload;
        std::string result = VinsUtils::getDivCardFromVins(payload);
        UNIT_ASSERT(result.empty());

        payload["card"]["type"] = "div_card";
        payload["card"]["body"] = Json::objectValue;
        result = VinsUtils::getDivCardFromVins(payload);
        UNIT_ASSERT(result.empty());

        payload["card"]["type"] = "div2_card";
        result = VinsUtils::getDivCardFromVins(payload);
        UNIT_ASSERT(result.empty());
    }

    Y_UNIT_TEST(testEnrichDivCard) {
        Json::Value payload;
        Json::Value body;
        body["may"] = "the force be with you";
        payload["card"]["type"] = "div_card";
        payload["card"]["body"] = body;

        std::string result = VinsUtils::getDivCardFromVins(payload);
        UNIT_ASSERT(!result.empty());
        UNIT_ASSERT_VALUES_EQUAL(jsonToString(body), result);
    }

    Y_UNIT_TEST(testEnrichDiv2Card) {
        Json::Value payload;
        Json::Value body;
        Json::Value templates;
        body["may"] = "the force be with you";
        templates["master"] = "yoda";
        payload["card"]["type"] = "div2_card";
        payload["card"]["body"] = body;
        payload["templates"] = templates;

        Json::Value expected;
        expected["card"] = body;
        expected["templates"] = templates;
        std::string result = VinsUtils::getDivCardFromVins(payload);
        UNIT_ASSERT(!result.empty());
        UNIT_ASSERT_VALUES_EQUAL(jsonToString(expected), result);
    }

    Y_UNIT_TEST(testWrongDiv2Card) {
        Json::Value payload;
        Json::Value templates;
        templates["master"] = "yoda";
        payload["templates"] = templates;

        // check with templates only
        std::string result = VinsUtils::getDivCardFromVins(payload);
        UNIT_ASSERT(result.empty());

        // check with null card
        payload["card"] = Json::nullValue;
        result = VinsUtils::getDivCardFromVins(payload);
        UNIT_ASSERT(result.empty());

        // check with empty card
        payload["card"] = Json::objectValue;
        result = VinsUtils::getDivCardFromVins(payload);
        UNIT_ASSERT(result.empty());

        // check with wrong card type
        payload["card"]["type"] = "SOME WRONG TYPE";
        result = VinsUtils::getDivCardFromVins(payload);
        UNIT_ASSERT(result.empty());

        // check with wrong card type and null body
        payload["card"]["body"] = Json::nullValue;
        result = VinsUtils::getDivCardFromVins(payload);
        UNIT_ASSERT(result.empty());

        // check with wrong card type and empty body
        payload["card"]["body"] = Json::objectValue;
        result = VinsUtils::getDivCardFromVins(payload);
        UNIT_ASSERT(result.empty());

        // check without templates but with proper card type and body
        payload.removeMember("templates");
        payload["card"]["type"] = "div2_card";
        payload["card"]["body"]["hello"] = "world";
        result = VinsUtils::getDivCardFromVins(payload);
        UNIT_ASSERT(result.empty());
    }

} /* test suite */
