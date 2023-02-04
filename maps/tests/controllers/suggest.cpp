#include <maps/wikimap/mapspro/services/editor/src/configs/config.h>
#include <maps/wikimap/mapspro/services/editor/src/observers/view_syncronizer.h>
#include <maps/wikimap/mapspro/services/editor/src/serialize/formatter.h>
#include <maps/wikimap/mapspro/services/editor/src/serialize/get_suggest_parser.h>
#include <maps/wikimap/mapspro/services/editor/src/utils.h>

#include "controller_tests_common_includes.h"
#include <maps/wikimap/mapspro/services/editor/src/tests/helpers/controller_helpers.h>
#include <maps/wikimap/mapspro/services/editor/src/tests/helpers/db_helpers.h>
#include <maps/wikimap/mapspro/services/editor/src/tests/helpers/helpers.h>

namespace maps::wiki::tests {

Y_UNIT_TEST_SUITE(test_suggest)
{
WIKI_FIXTURE_TEST_CASE(test_suggest, EditorTestFixture)
{
    performSaveObjectRequest("tests/data/create_abcstreet.json", makeObservers<ViewSyncronizer>());

    GetSuggestParser parser;
    parser.parse(common::FormatType::JSON, "{\"coordinates\": [37.76079899666591,55.76677622328369], \"type\": \"Point\"}");
    GetSuggest::Request getSuggestRequest{
        "rd",
        "ABC",
        0,
        "",
        "",
        revision::TRUNK_BRANCH_ID,
        2000.0,
        parser.geometry(),
        GetSuggest::GeomPredicate::Distance,
        parser.attributesValues(),
        ""};
    GetSuggest getSuggestController(getSuggestRequest);
    auto suggestedResult = getSuggestController();
    UNIT_ASSERT_EQUAL(suggestedResult->suggestedObjects.size(), 1);
    auto jsonFormatter = Formatter::create(common::FormatType::JSON,
        make_unique<TestFormatterContext>());
    validateJsonResponse((*jsonFormatter)(*suggestedResult), "GetSuggest");
}

WIKI_FIXTURE_TEST_CASE(should_suggest_for_different_categories, EditorTestFixture)
{
    GetSuggestParser parser;
    const auto observers = makeObservers<ViewSyncronizer>();

    // Parking Controlled Zone
    {
        performSaveObjectRequest("tests/data/create_parking_controlled_zone.xml", observers);
        parser.parse(common::FormatType::JSON, R"({"coordinates": [0, 0], "type": "Point"})");
        auto suggestedResult = GetSuggest({
            "urban_roadnet_parking_controlled_zone",
            "zone",
            0,
            "",
            "",
            revision::TRUNK_BRANCH_ID,
            100000000.0, // Just a big distance not to bother with coordinates.
            parser.geometry(),
            GetSuggest::GeomPredicate::Distance,
            {},
            ""
        })();

        WIKI_TEST_REQUIRE(suggestedResult->suggestedObjects.size() == 1);
        UNIT_ASSERT_EQUAL(suggestedResult->suggestedObjects[0].name, "Test parking zone");
    }

    // Zipcode
    {
        performSaveObjectRequest("tests/data/create_zipcode.xml", observers);
        parser.parse(common::FormatType::JSON, R"({"coordinates": [0, 0], "type": "Point"})");
        auto suggestedResult = GetSuggest({
            "zipcode",
            "603",
            0,
            "",
            "",
            revision::TRUNK_BRANCH_ID,
            100000000.0, // Just a big distance not to bother with coordinates.
            parser.geometry(),
            GetSuggest::GeomPredicate::Distance,
            {},
            ""
        })();

        WIKI_TEST_REQUIRE(suggestedResult->suggestedObjects.size() == 1);
        UNIT_ASSERT_EQUAL(suggestedResult->suggestedObjects[0].name, "603163");
    }
}

WIKI_FIXTURE_TEST_CASE(test_suggest_with_attributes, EditorTestFixture)
{
    const auto observers = makeObservers<ViewSyncronizer>();
    performSaveObjectRequest("tests/data/suggest/create_poi_shopping_173.json", observers);
    performSaveObjectRequest("tests/data/suggest/create_poi_shopping_1306.json", observers);

    const std::string& plainGeomBody = ""
        "{"
        "    \"coordinates\": [[[37.67130814399527, 55.810679869567466], [37.671305461786254, 55.81062473923271], [37.67148785199928, 55.810613411072026], [37.67149961876411, 55.810676915472364], [37.67130814399527, 55.810679869567466]]],"
        "    \"type\": \"Polygon\""
        "}";
    const std::string& namedGeomBody = "{\"geometry\":" + plainGeomBody + "}";

    const std::string& attributesAndGeomBody = "{\"geometry\":" + plainGeomBody + ", \"attrs\": {\"poi_shopping:ft_type_id\":\"1306\"}}";
    const std::string& attributesAndGeomBodyXml =
        "<editor xmlns=\"http://maps.yandex.ru/mapspro/editor/1.x\">"
            "<request-suggest>"
                "<geometry>" + plainGeomBody + "</geometry>"
                "<attributes>"
                    "<attribute id=\"poi_shopping:ft_type_id\">"
                        "<values>"
                            "<value><![CDATA[1306]]></value>"
                        "</values>"
                    "</attribute>"
                "</attributes>"
            "</request-suggest>"
        "</editor>";
    {//Attributes and geometry
        GetSuggestParser parser;
        parser.parse(common::FormatType::JSON, attributesAndGeomBody);
        UNIT_ASSERT_EQUAL(parser.attributesValues().size(), 1);
        GetSuggest::Request getSuggestRequest {
            "poi_shopping",
            "",
            0,
            "",
            "",
            revision::TRUNK_BRANCH_ID,
            2000.0,
            parser.geometry(),
            GetSuggest::GeomPredicate::Distance,
            parser.attributesValues(),
            ""
        };
        GetSuggest getSuggestController(getSuggestRequest);
        auto suggestedResult = getSuggestController();
        UNIT_ASSERT_EQUAL(suggestedResult->suggestedObjects.size(), 1);
        auto jsonFormatter = Formatter::create(common::FormatType::JSON,
            make_unique<TestFormatterContext>());
        validateJsonResponse((*jsonFormatter)(*suggestedResult), "GetSuggest");
    }
    {//Attributes and geometry XML
        GetSuggestParser parser;
        parser.parse(common::FormatType::XML, attributesAndGeomBodyXml);
        UNIT_ASSERT_EQUAL(parser.attributesValues().size(), 1);
        GetSuggest::Request getSuggestRequest {
            "poi_shopping",
            "",
            0,
            "",
            "",
            revision::TRUNK_BRANCH_ID,
            2000.0,
            parser.geometry(),
            GetSuggest::GeomPredicate::Distance,
            parser.attributesValues(),
            ""
        };
        GetSuggest getSuggestController(getSuggestRequest);
        auto suggestedResult = getSuggestController();
        UNIT_ASSERT_EQUAL(suggestedResult->suggestedObjects.size(), 1);
        auto jsonFormatter = Formatter::create(common::FormatType::JSON,
            make_unique<TestFormatterContext>());
        validateJsonResponse((*jsonFormatter)(*suggestedResult), "GetSuggest");
    }
    {//geometry 1
        GetSuggestParser parser;
        parser.parse(common::FormatType::JSON, namedGeomBody);
        UNIT_ASSERT_EQUAL(parser.attributesValues().size(), 0);
        GetSuggest::Request getSuggestRequest {
            "poi_shopping",
            "",
            0,
            "",
            "",
            revision::TRUNK_BRANCH_ID,
            2000.0,
            parser.geometry(),
            GetSuggest::GeomPredicate::Distance,
            parser.attributesValues(),
            ""
        };
        GetSuggest getSuggestController(getSuggestRequest);
        auto suggestedResult = getSuggestController();
        UNIT_ASSERT_EQUAL(suggestedResult->suggestedObjects.size(), 2);
        auto jsonFormatter = Formatter::create(common::FormatType::JSON,
            make_unique<TestFormatterContext>());
        validateJsonResponse((*jsonFormatter)(*suggestedResult), "GetSuggest");
    }

    {//geometry 2
        GetSuggestParser parser;
        parser.parse(common::FormatType::JSON, plainGeomBody);
        UNIT_ASSERT_EQUAL(parser.attributesValues().size(), 0);
        GetSuggest::Request getSuggestRequest {
            "poi_shopping",
            "",
            0,
            "",
            "",
            revision::TRUNK_BRANCH_ID,
            2000.0,
            parser.geometry(),
            GetSuggest::GeomPredicate::Distance,
            parser.attributesValues(),
            ""
        };
        GetSuggest getSuggestController(getSuggestRequest);
        auto suggestedResult = getSuggestController();
        UNIT_ASSERT_EQUAL(suggestedResult->suggestedObjects.size(), 2);
        auto jsonFormatter = Formatter::create(common::FormatType::JSON,
            make_unique<TestFormatterContext>());
        validateJsonResponse((*jsonFormatter)(*suggestedResult), "GetSuggest");
    }
}

WIKI_FIXTURE_TEST_CASE(test_suggest_with_boolean_attributes, EditorTestFixture)
{
    performSaveObjectRequest(
        "tests/data/mass_transit/create_transport_bus_route.json",
        makeObservers<ViewSyncronizer>());

    {//Boolean attribute = false
        const std::string& booleanAttribute = R"({"attrs": {"sys:not_operating" : false}})";
        GetSuggestParser parser;
        parser.parse(common::FormatType::JSON, booleanAttribute);
        UNIT_ASSERT_EQUAL(parser.attributesValues().size(), 1);
        GetSuggest::Request getSuggestRequest {
            "transport_bus_route",
            "test_route",
            0,
            "",
            "",
            revision::TRUNK_BRANCH_ID,
            20000000.0,
            {},
            GetSuggest::GeomPredicate::Distance,
            parser.attributesValues(),
            ""
        };
        GetSuggest getSuggestController(getSuggestRequest);
        auto suggestedResult = getSuggestController();
        UNIT_ASSERT_EQUAL(suggestedResult->suggestedObjects.size(), 1);
        auto jsonFormatter = Formatter::create(common::FormatType::JSON,
            make_unique<TestFormatterContext>());
        validateJsonResponse((*jsonFormatter)(*suggestedResult), "GetSuggest");
    }
    {//Boolean attribute = true
        const std::string& booleanAttribute = R"({"attrs": {"sys:not_operating" : true}})";
        GetSuggestParser parser;
        parser.parse(common::FormatType::JSON, booleanAttribute);
        UNIT_ASSERT_EQUAL(parser.attributesValues().size(), 1);
        GetSuggest::Request getSuggestRequest {
            "transport_bus_route",
            "test_route",
            0,
            "",
            "",
            revision::TRUNK_BRANCH_ID,
            20000000.0,
            {},
            GetSuggest::GeomPredicate::Distance,
            parser.attributesValues(),
            ""
        };
        GetSuggest getSuggestController(getSuggestRequest);
        auto suggestedResult = getSuggestController();
        UNIT_ASSERT(suggestedResult->suggestedObjects.empty());
        auto jsonFormatter = Formatter::create(common::FormatType::JSON,
            make_unique<TestFormatterContext>());
        validateJsonResponse((*jsonFormatter)(*suggestedResult), "GetSuggest");
    }
}
} //Y_UNIT_TEST_SUITE

} // namespace maps::wiki::tests
