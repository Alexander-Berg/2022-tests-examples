#include "controller_tests_common_includes.h"

#include <maps/wikimap/mapspro/services/editor/src/configs/config.h>
#include <maps/wikimap/mapspro/services/editor/src/serialize/formatter.h>
#include <maps/wikimap/mapspro/services/editor/src/serialize/save_object_parser.h>
#include <maps/wikimap/mapspro/services/editor/src/utils.h>

#include <maps/wikimap/mapspro/services/editor/src/tests/helpers/controller_helpers.h>
#include <maps/wikimap/mapspro/services/editor/src/tests/helpers/db_helpers.h>
#include <maps/wikimap/mapspro/services/editor/src/tests/helpers/helpers.h>

#include <maps/libs/json/include/value.h>
#include <yandex/maps/wiki/topo/exception.h>

namespace maps::wiki::tests {

Y_UNIT_TEST_SUITE(contour_validity)
{
WIKI_FIXTURE_TEST_CASE(test_contour_validity, EditorTestFixture)
{
    performSaveObjectRequest("tests/data/create_vegetation_1.json");
    performSaveObjectRequest("tests/data/create_vegetation_2.json");

    UNIT_CHECK_GENERATED_EXCEPTION(
        performSaveObjectRequest("tests/data/edit_vegetation_2.json"),
        topo::InvalidFaceError
    );
}
} // Y_UNIT_TEST_SUITE

} // namespace maps::wiki::tests
