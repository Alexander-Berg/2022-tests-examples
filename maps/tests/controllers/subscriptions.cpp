#include <maps/wikimap/mapspro/services/editor/src/actions/social/get_subscriptions.h>
#include <maps/wikimap/mapspro/services/editor/src/observers/category_specific.h>
#include <maps/wikimap/mapspro/services/editor/src/serialize/formatter.h>

#include "controller_tests_common_includes.h"
#include <maps/wikimap/mapspro/services/editor/src/tests/helpers/controller_helpers.h>
#include <maps/wikimap/mapspro/services/editor/src/tests/helpers/db_helpers.h>
#include <maps/wikimap/mapspro/services/editor/src/tests/helpers/helpers.h>

namespace maps::wiki::tests {

namespace {

const std::string CREATE_SUBSCRIPTION_BODY = R"(
{
    "categoryId":"feed_region",
    "uuid":"8f133a39-8abe-4bf1-be24-207b98905d97",
    "geometry":{
        "type":"Polygon",
        "coordinates":[[[37.556709623428326,55.82460028045846],[37.55610880860899,55.831593397227905],[37.58110699662775,56.8328252144506],[37.58297381410216,55.82428622487431],[37.556709623428326,55.82460028045846]]]
    },
    "attrs":{
        "feed_region:name": "Test Region"
    },
    "editContext":{
        "center":{
            "type":"Point",
            "coordinates":[37.56883320817565,55.82856803275882]
        },
        "zoom":16
    }
}
)";

} // namespace


Y_UNIT_TEST_SUITE(test_subscriptions)
{
WIKI_FIXTURE_TEST_CASE(test_subscriptions, EditorTestFixture)
{
    auto createResult = json::Value::fromString(
        performSaveObjectRequestJsonStr(
            CREATE_SUBSCRIPTION_BODY,
            makeObservers<CategorySpecificObserver>()
        )
    );

    UNIT_ASSERT_EQUAL(createResult["geoObjects"][0]["title"].toString(), "Test Region");
    auto regionId = createResult["geoObjects"][0]["id"].toString();

    {
        GetSocialSubscriptions::Request getListRequest{TESTS_USER, ""};
        GetSocialSubscriptions controller(getListRequest);
        auto formatter = Formatter::create(
                common::FormatType::JSON, make_unique<TestFormatterContext>());
        auto result = (*formatter)(*controller());
        validateJsonResponse(result, "GetSocialSubscriptions");
        auto regionsJson = json::Value::fromString(result)["regions"];
        WIKI_TEST_REQUIRE_EQUAL(regionsJson.size(), 1);
        WIKI_TEST_REQUIRE_EQUAL(regionsJson[0]["id"].toString(), regionId);
        WIKI_TEST_REQUIRE_EQUAL(regionsJson[0]["title"].toString(), "Test Region");
    }
}
}//Y_UNIT_TEST_SUITE

} // namespace maps::wiki::tests
