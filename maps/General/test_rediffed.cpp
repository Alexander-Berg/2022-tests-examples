#include <maps/sprav/callcenter/libs/rediffed/rediffed_request.h>

#include <maps/sprav/callcenter/libs/rediffed/ut/common.h>
#include <maps/sprav/callcenter/libs/test_helpers/util.h>

#include <library/cpp/json/json_reader.h>
#include <library/cpp/json/writer/json.h>
#include <library/cpp/testing/gtest/gtest.h>

#include <sprav/protos/signal.pb.h>

#include <algorithm>
#include <cctype>

namespace maps::sprav::callcenter::rediffed::tests {

bool featureIsMultiple(uint64_t /* featureId */) {
    return false;
}

TEST(RediffedTest, Empty) {
    EXPECT_EQ(
        test_helpers::reformatJson(rediffedRequestJson(NSprav::Company{}, featureIsMultiple)),
        test_helpers::reformatJson(R"({
            "address":              { "raw":[], "unified":[] },
            "phones":               { "raw":[], "unified":[] },
            "urls":                 { "raw":[], "unified":[] },
            "emails":               { "raw":[], "unified":[] },
            "names":                { "raw":[], "unified":[] },
            "work_intervals":       { "raw":[], "unified":[] },
            "rubrics":              { "raw":[], "unified":[] },
            "features":             { "raw":[], "unified":[] },
            "chains":               { "raw":[], "unified":[] },
            "publishing_status":    { "raw":[], "unified":[] },
            "duplicate_company_id": { "raw":[], "unified":[] },
            "inn":                  { "raw":[], "unified":[] },
            "ogrn":                 { "raw":[], "unified":[] }
        })"));
}

// TODO(ALTAY-15306): all fields should be filled
TEST(RediffedTest, Full) {
    NSprav::Company preparedChanges;

    auto* nameMain = preparedChanges.add_names();
    nameMain->set_action(NSprav::Action::ACTUALIZE);
    nameMain->set_value("Company name value");
    nameMain->set_type(NSprav::Name::MAIN);
    nameMain->set_lang(NSprav::NLanguage::RU);

    auto* inn = preparedChanges.mutable_inn();
    inn->set_action(NSprav::Action::ACTUALIZE);
    inn->set_id("1234567890");

    EXPECT_EQ(
        test_helpers::reformatJson(rediffedRequestJson(preparedChanges, featureIsMultiple)),
        test_helpers::reformatJson(R"({
            "address":              { "raw":[], "unified":[] },
            "phones":               { "raw":[], "unified":[] },
            "urls":                 { "raw":[], "unified":[] },
            "emails":               { "raw":[], "unified":[] },
            "names": {
                "raw": [
                    {
                        "action": "actualize",
                        "lang": "ru",
                        "type": "main",
                        "value": "Company name value"
                    }
                ],
                "unified": [
                    {
                        "slot": {
                            "value": {
                                "lang": "ru",
                                "type": "main"
                            },
                            "single_value": true
                        },
                        "core": "",
                        "value": {
                            "action": "actualize",
                            "lang": "ru",
                            "type": "main",
                            "value": "Company name value"
                        },
                        "short": "Company name value",
                        "raw_id": [0]
                    }
                ]
            },
            "work_intervals":       { "raw":[], "unified":[] },
            "rubrics":              { "raw":[], "unified":[] },
            "features":             { "raw":[], "unified":[] },
            "chains":               { "raw":[], "unified":[] },
            "publishing_status":    { "raw":[], "unified":[] },
            "duplicate_company_id": { "raw":[], "unified":[] },
            "inn": {
                "raw": [
                    {
                        "action": "actualize",
                        "id": "1234567890"
                    }
                ],
                "unified": [
                    {
                        "slot": {
                            "value": "",
                            "single_value": true
                        },
                        "core": "",
                        "value": {
                            "action": "actualize",
                            "id": "1234567890"
                        },
                        "short": "1234567890",
                        "raw_id": [0]
                    }
                ]
            },
            "ogrn":                 { "raw":[], "unified":[] }
        })"));
}

} // namespace maps::sprav::callcenter::rediffed::tests
