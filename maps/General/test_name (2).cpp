#include <maps/sprav/callcenter/libs/rediffed/attr/name.h>

#include <maps/sprav/callcenter/libs/rediffed/ut/common.h>
#include <maps/sprav/callcenter/libs/test_helpers/util.h>

#include <library/cpp/json/json_reader.h>
#include <library/cpp/json/writer/json.h>
#include <library/cpp/testing/gtest/gtest.h>

#include <sprav/protos/signal.pb.h>

#include <algorithm>
#include <cctype>

namespace maps::sprav::callcenter::rediffed::tests {

TEST(NameTest, Json) {
    NSprav::Company preparedChanges;
    auto* nameMain = preparedChanges.add_names();
    nameMain->set_action(NSprav::Action::ACTUALIZE);
    nameMain->set_value("Company name value");
    nameMain->set_type(NSprav::Name::MAIN);
    nameMain->set_lang(NSprav::NLanguage::RU);
    auto* nameSynonym = preparedChanges.add_names();
    nameSynonym->set_action(NSprav::Action::ACTUALIZE);
    nameSynonym->set_value("Company synonym name value");
    nameSynonym->set_type(NSprav::Name::SYNONYM);
    nameSynonym->set_lang(NSprav::NLanguage::RU);

    EXPECT_EQ(
        test_helpers::reformatJson(attrToJson(attr::Name::preparedChangesToRediffed(preparedChanges))),
        test_helpers::reformatJson(R"({
            "raw": [
                {
                    "action": "actualize",
                    "lang": "ru",
                    "type": "main",
                    "value": "Company name value"
                },
                {
                    "action": "actualize",
                    "lang": "ru",
                    "type": "synonym",
                    "value": "Company synonym name value"
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
                },
                {
                    "slot": {
                        "value": {
                            "lang": "ru",
                            "type": "synonym"
                        },
                        "single_value": false
                    },
                    "core": "Company synonym name value",
                    "value": {
                        "action": "actualize",
                        "lang": "ru",
                        "type": "synonym",
                        "value": "Company synonym name value"
                    },
                    "short": "Company synonym name value",
                    "raw_id": [1]
                }
            ]
        })")
    );
}

} // namespace maps::sprav::callcenter::rediffed::tests
