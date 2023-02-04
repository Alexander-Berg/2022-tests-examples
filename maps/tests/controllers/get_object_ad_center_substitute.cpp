#include <maps/wikimap/mapspro/services/editor/src/configs/config.h>
#include <maps/wikimap/mapspro/services/editor/src/serialize/formatter.h>
#include <maps/wikimap/mapspro/services/editor/src/serialize/save_object_parser.h>
#include <maps/wikimap/mapspro/services/editor/src/utils.h>

#include "controller_tests_common_includes.h"
#include <maps/wikimap/mapspro/services/editor/src/tests/helpers/controller_helpers.h>
#include <maps/wikimap/mapspro/services/editor/src/tests/helpers/db_helpers.h>
#include <maps/wikimap/mapspro/services/editor/src/tests/helpers/helpers.h>

#include <maps/libs/json/include/value.h>

namespace maps::wiki::tests {

Y_UNIT_TEST_SUITE(get_object_ad_center_substitute)
{
WIKI_FIXTURE_TEST_CASE(test_get_object_substitution_for_center, EditorTestFixture)
{
    //Create object for test
    std::string result = performSaveObjectRequest("tests/data/create_ad_cnt_fc_el_el.json");

    auto outputParser = SaveObjectParser();
    UNIT_ASSERT_NO_EXCEPTION(outputParser.parse(common::FormatType::JSON, result));
    WIKI_TEST_REQUIRE_EQUAL(outputParser.objects().size(), 1);//ad
    const auto& objectData = *outputParser.objects().begin();
    UNIT_ASSERT_EQUAL(objectData.categoryId(), "ad");
    auto parsedResult = json::Value::fromString(result);
    TOid adId = boost::lexical_cast<TOid>(
        parsedResult["geoObjects"][0]["id"].toString());
    TOid adCntId =  boost::lexical_cast<TOid>(
        parsedResult["geoObjects"][0]
        ["slaves"]["center"]["geoObjects"][0]["id"].toString());

    //Now do actual test for GetObject with/without override
    GetObject::Request getObjRequest {
        adCntId,
        TESTS_USER,
        "",
        0,
        GetObject::SubstitutionPolicy::Deny,//don't allow substitution
        GetObject::PartsOfComplexContourObjectsLoadPolicy::Load
    };
    auto formatter = Formatter::create(common::FormatType::JSON,
        make_unique<TestFormatterContext>());
    GetObject getObjectController(getObjRequest);
    std::string resultNoSubstition;
    UNIT_ASSERT_NO_EXCEPTION(resultNoSubstition = (*formatter)(*getObjectController()));
    validateJsonResponse(resultNoSubstition, "GetObject");

    //turn substitution on and expect ad at root
    getObjRequest.substitutionPolicy = GetObject::SubstitutionPolicy::Allow;
    GetObject getObjectControllerSubst(getObjRequest);
    std::string resultSubstition;
    UNIT_ASSERT_NO_EXCEPTION(resultSubstition = (*formatter)(*getObjectControllerSubst()));
    validateJsonResponse(resultSubstition, "GetObject");
    UNIT_ASSERT(resultSubstition != resultNoSubstition);
    UNIT_ASSERT_EQUAL(getObjectID(resultNoSubstition), adCntId);
    UNIT_ASSERT_EQUAL(getObjectID(resultSubstition), adId);
}
}//Y_UNIT_TEST_SUITE

} // namespace maps::wiki::tests
