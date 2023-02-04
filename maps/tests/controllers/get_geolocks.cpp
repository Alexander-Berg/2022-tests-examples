#include "controller_tests_common_includes.h"
#include <maps/wikimap/mapspro/services/editor/src/tests/helpers/controller_helpers.h>
#include <maps/wikimap/mapspro/services/editor/src/tests/helpers/db_helpers.h>
#include <maps/wikimap/mapspro/services/editor/src/tests/helpers/helpers.h>

#include <maps/libs/json/include/value.h>

namespace maps::wiki::tests {

Y_UNIT_TEST_SUITE(get_geolocks)
{
WIKI_FIXTURE_TEST_CASE(test_get_geolocks, EditorTestFixture)
{
    executeSqlFile("tests/sql/create_geolock.sql");

    GetGeoLocks::Request getRequest {
        TESTS_USER,
        revision::TRUNK_BRANCH_ID,
        "-37.51974887249752,-55.72735012314865,37.552900975921595,55.73877964006198",
    };

    GetGeoLocks controller(getRequest);
    auto result = controller();
    UNIT_ASSERT_EQUAL(result->locks.size(), 1);

    auto formatter = Formatter::create(
        common::FormatType::JSON,
        make_unique<TestFormatterContext>());
    validateJsonResponse((*formatter)(*result), "GetGeoLocks");
}
}//Y_UNIT_TEST_SUITE

} // namespace maps::wiki::tests
