#include <maps/wikimap/mapspro/services/editor/src/configs/config.h>
#include <maps/wikimap/mapspro/services/editor/src/magic_strings.h>
#include <maps/wikimap/mapspro/services/editor/src/serialize/formatter.h>
#include <maps/wikimap/mapspro/services/editor/src/serialize/save_object_parser.h>
#include <maps/wikimap/mapspro/services/editor/src/utils.h>

#include "controller_tests_common_includes.h"
#include <maps/wikimap/mapspro/services/editor/src/tests/helpers/controller_helpers.h>
#include <maps/wikimap/mapspro/services/editor/src/tests/helpers/db_helpers.h>
#include <maps/wikimap/mapspro/services/editor/src/tests/helpers/helpers.h>

namespace maps::wiki::tests {

Y_UNIT_TEST_SUITE(create_aoi)
{
WIKI_FIXTURE_TEST_CASE(test_create_aoi, EditorTestFixture)
{
    std::string result = performSaveObjectRequest("tests/data/create_aoi.xml");

    auto outputParser = SaveObjectParser();
    UNIT_ASSERT_NO_EXCEPTION(outputParser.parse(common::FormatType::XML, result));
    WIKI_TEST_REQUIRE_EQUAL(outputParser.objects().size(), 1);
    const auto& objectData = *outputParser.objects().begin();
    UNIT_ASSERT_EQUAL(objectData.categoryId(), CATEGORY_AOI);
    UNIT_ASSERT(objectData.isGeometryDefined());
    UNIT_ASSERT_EQUAL(objectData.geometry().geometryTypeName(), "polygon");
}
} // Y_UNIT_TEST_SUITE

} // namespace maps::wiki::tests
