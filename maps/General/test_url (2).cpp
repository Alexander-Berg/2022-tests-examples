#include <maps/sprav/callcenter/libs/rediffed/attr/url.h>

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

TEST(UrlTest, Json) {
    NSprav::Company preparedChanges;
    *preparedChanges.add_urls() = NProtobufJson::Json2Proto<NSprav::Url>(
        R"({
            "action":"actualize",
            "value":"https://ne-diksi.com/",
            "access":"public"
        })",
        NProtobufJson::TJson2ProtoConfig().SetEnumValueMode(NProtobufJson::TJson2ProtoConfig::EnumCaseInsensetive)
    );
    *preparedChanges.add_urls() = NProtobufJson::Json2Proto<NSprav::Url>(
        R"({
            "action":"delete",
            "value":"https://vk.com/dixyclub",
            "access":"public"
        })",
        NProtobufJson::TJson2ProtoConfig().SetEnumValueMode(NProtobufJson::TJson2ProtoConfig::EnumCaseInsensetive)
    );

    EXPECT_EQ(
        test_helpers::reformatJson(attrToJson(attr::Url::preparedChangesToRediffed(preparedChanges))),
        test_helpers::reformatJson(R"({
            "raw":
                [
                    {
                        "access":"public",
                        "action":"actualize",
                        "description":[],
                        "value":"https://ne-diksi.com/"
                    },
                    {
                        "access":"public",
                        "action":"delete",
                        "description":[],
                        "value":"https://vk.com/dixyclub"
                    }
                ],
            "unified":
                [
                    {
                        "core":"",
                        "raw_id":[0],
                        "short":"https://ne-diksi.com/",
                        "slot":
                        {
                            "single_value":true,
                            "value":"main"
                        },
                        "value":
                        {
                            "access":"public",
                            "action":"actualize",
                            "description":[],
                            "value":"https://ne-diksi.com/"
                        }
                    },
                    {
                        "core":"",
                        "raw_id":[1],
                        "short":"https://vk.com/dixyclub",
                        "slot":
                        {
                            "single_value":true,
                            "value":"main"
                        },
                        "value":
                        {
                            "access":"public",
                            "action":"delete",
                            "description":[],
                            "value":"https://vk.com/dixyclub"
                        }
                    }
                ]
            })")
    );
}

} // namespace maps::sprav::callcenter::rediffed::tests
