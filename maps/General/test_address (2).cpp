#include <maps/sprav/callcenter/libs/rediffed/attr/address.h>

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

TEST(AddressTest, Json) {
    NSprav::Company preparedChanges;
    auto* address = preparedChanges.add_addresses();

    auto addressPreparedChanges = R"(
      {
        "action":"actualize",
        "source":"original",
        "lang":"ru",
        "ad_hierarchy":
          [
            {
              "name":"Россия",
              "kind":"country",
              "matched":true
            },
            {
              "name":"Центральный федеральный округ",
              "kind":"province",
              "matched":false
            },
            {
              "name":"Тульская область",
              "kind":"province",
              "matched":false
            },
            {
              "name":"муниципальное образование Тула",
              "kind":"area",
              "matched":false
            },
            {
              "name":"Тула",
              "kind":"locality",
              "matched":true
            },
            {
              "name":"Советская улица",
              "kind":"street",
              "matched":true
            },
            {
              "name":"53/2",
              "kind":"house",
              "matched":true
            }
          ],
        "one_line":"Россия, Тула, Советская улица, 53/2",
        "coordinates":
          {
            "lat":54.191563,
            "lon":37.620874
          },
        "precision":"exact",
        "geo_id":15,
        "region_code":"RU",
        "bounding_box":
          {
            "left":
              {
                "lat":54.189113,
                "lon":37.61653
              },
            "right":
              {
                "lat":54.193928,
                "lon":37.624741
              }
          },
        "add_info":
          [
            {
              "value":"ТРЦ Гостиный двор, этаж -1",
              "lang":"ru"
            }
          ],
        "address_id":2139678658,
        "add_info_items":
          [
            {
              "kind":"ТРЦ",
              "value":"Гостиный двор",
              "type":"buildingcomponent"
            },
            {
              "kind":"этаж",
              "value":"-1",
              "type":"floorcomponent"
            }
          ]
      })";

    *address = NProtobufJson::Json2Proto<NSprav::Address>(
        addressPreparedChanges,
        NProtobufJson::TJson2ProtoConfig().SetEnumValueMode(NProtobufJson::TJson2ProtoConfig::EnumCaseInsensetive));

    EXPECT_EQ(
        test_helpers::reformatJson(attrToJson(attr::Address::preparedChangesToRediffed(preparedChanges))),
        test_helpers::reformatJson(R"-({
            "raw":
                [
                    {
                        "action":"actualize",
                        "addInfo":
                        [{
                            "lang":"ru",
                            "value":"ТРЦ Гостиный двор, этаж -1"
                        }],
                        "addInfoItems":[],
                        "coordinates":
                        {
                            "lat":54.191563,
                            "lon":37.620874
                        },
                        "lang":"ru",
                        "oneLine":"Россия, Тула, Советская улица, 53/2"
                    }
                ],
            "unified":
                [
                    {
                        "core":"",
                        "raw_id":[0],
                        "short":
                        {
                            "formatted":"Россия, Тула, Советская улица, 53/2 (54.191563, 37.620874)",
                            "source":"original"
                        },
                        "slot":
                        {
                            "single_value":true,
                            "value":""
                        },
                        "value":
                        {
                            "action":"actualize",
                            "adHierarchy":
                            [
                                {
                                    "kind":"country",
                                    "matched":true,
                                    "name":"Россия"
                                },
                                {
                                    "kind":"province",
                                    "matched":false,
                                    "name":"Центральный федеральный округ"
                                },
                                {
                                    "kind":"province",
                                    "matched":false,
                                    "name":"Тульская область"
                                },
                                {
                                    "kind":"area",
                                    "matched":false,
                                    "name":"муниципальное образование Тула"
                                },
                                {
                                    "kind":"locality",
                                    "matched":true,
                                    "name":"Тула"
                                },
                                {
                                    "kind":"street",
                                    "matched":true,
                                    "name":"Советская улица"
                                },
                                {
                                    "kind":"house",
                                    "matched":true,
                                    "name":"53/2"
                                }
                            ],
                            "addInfo":
                            [
                                {
                                    "lang":"ru",
                                    "value":"ТРЦ Гостиный двор, этаж -1"
                                }
                            ],
                            "addInfoItems":
                            [
                                {
                                    "kind":"ТРЦ",
                                    "type":"buildingcomponent",
                                    "value":"Гостиный двор"
                                },
                                {
                                    "kind":"этаж",
                                    "type":"floorcomponent",
                                    "value":"-1"
                                }
                            ],
                            "addressBuildingIds":[],
                            "addressId":2139678658,
                            "boundingBox":
                            {
                                "left":
                                {
                                    "lat":54.189113,
                                    "lon":37.61653
                                },
                                "right":
                                {
                                    "lat":54.193928,
                                    "lon":37.624741
                                }
                            },
                            "buildingAddressIds":[],
                            "coordinates":
                            {
                                "lat":54.191563,
                                "lon":37.620874
                            },
                            "geoId":15,
                            "lang":"ru",
                            "oneLine":"Россия, Тула, Советская улица, 53/2",
                            "precision":"exact",
                            "regionCode":"RU",
                            "source":"original",
                            "translations":[],
                            "unificationErrors":[]
                        }
                    }
                ]
        })-")
    );
}

} // namespace maps::sprav::callcenter::rediffed::tests
