#include <maps/sprav/callcenter/libs/rediffed/attr/phone.h>

#include <maps/sprav/callcenter/libs/rediffed/ut/common.h>
#include <maps/sprav/callcenter/libs/test_helpers/util.h>

#include <library/cpp/json/json_reader.h>
#include <library/cpp/json/writer/json.h>
#include <library/cpp/protobuf/json/json2proto.h>
#include <library/cpp/testing/gtest/gtest.h>

#include <sprav/protos/signal.pb.h>

#include <algorithm>
#include <cctype>

namespace maps::sprav::callcenter::rediffed::tests {

TEST(PhoneTest, Json) {
    NSprav::Company preparedChanges;
    auto* phone = preparedChanges.add_phones();

    *phone = NProtobufJson::Json2Proto<NSprav::Phone>(
        R"({
            "action":"actualize",
            "access":"public",
            "country_code":"7",
            "region_code":"800",
            "number":"7700022",
            "ext":"",
            "rank":1,
            "type":
            [
                "phone"
            ],
            "mode":"world",
            "formatted":"8 (800) 770-00-22",
            "hide_reason":"none"
        })",
        NProtobufJson::TJson2ProtoConfig().SetEnumValueMode(NProtobufJson::TJson2ProtoConfig::EnumCaseInsensetive)
    );

    EXPECT_EQ(
        test_helpers::reformatJson(attrToJson(attr::Phone::preparedChangesToRediffed(preparedChanges))),
        test_helpers::reformatJson(R"({
            "raw":
            [
                {
                    "access":"public",
                    "action":"actualize",
                    "description":[],
                    "ext":"",
                    "rank":1,
                    "type":
                    [
                            "phone"
                    ],
                    "value":
                    {
                        "lang":"undefined",
                        "value":"8 (800) 770-00-22"
                    }
                }
            ],
            "unified":
            [
                {
                    "core":"88007700022",
                    "raw_id":[0],
                    "short":"8 (800) 770-00-22",
                    "slot":
                    {
                        "single_value":false,
                        "value":""
                    },
                    "value":
                    {
                        "access":"public",
                        "action":"actualize",
                        "countryCode":"7",
                        "description":[],
                        "ext":"",
                        "formatted":"8 (800) 770-00-22",
                        "hideReason":"none",
                        "mode":"world",
                        "number":"7700022",
                        "rank":1,
                        "regionCode":"800",
                        "type":
                        [
                                "phone"
                        ]
                    }
                }
            ]
        })")
    );
}

} // namespace maps::sprav::callcenter::rediffed::tests
