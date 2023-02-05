#include <maps/sprav/callcenter/libs/rediffed/attr/inn.h>

#include <maps/sprav/callcenter/libs/rediffed/ut/common.h>
#include <maps/sprav/callcenter/libs/test_helpers/util.h>

#include <library/cpp/json/json_reader.h>
#include <library/cpp/json/writer/json.h>
#include <library/cpp/testing/gtest/gtest.h>

#include <sprav/protos/signal.pb.h>

#include <algorithm>
#include <cctype>

namespace maps::sprav::callcenter::rediffed::tests {

TEST(InnTest, Json) {
    NSprav::Company preparedChanges;
    auto* inn = preparedChanges.mutable_inn();
    inn->set_action(NSprav::Action::ACTUALIZE);
    inn->set_id("1234567890");

    EXPECT_EQ(
        test_helpers::reformatJson(attrToJson(attr::Inn::preparedChangesToRediffed(preparedChanges))),
        test_helpers::reformatJson(R"({
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
        })")
    );
}

} // namespace maps::sprav::callcenter::rediffed::tests
