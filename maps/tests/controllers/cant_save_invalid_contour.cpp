#include <maps/wikimap/mapspro/services/editor/src/configs/config.h>
#include <maps/wikimap/mapspro/services/editor/src/serialize/formatter.h>
#include <maps/wikimap/mapspro/services/editor/src/serialize/save_object_parser.h>
#include <maps/wikimap/mapspro/services/editor/src/utils.h>

#include "controller_tests_common_includes.h"
#include <maps/wikimap/mapspro/services/editor/src/tests/helpers/controller_helpers.h>
#include <maps/wikimap/mapspro/services/editor/src/tests/helpers/db_helpers.h>
#include <maps/wikimap/mapspro/services/editor/src/tests/helpers/helpers.h>

#include <yandex/maps/wiki/topo/exception.h>
#include <maps/libs/json/include/value.h>

namespace maps::wiki::tests {

Y_UNIT_TEST_SUITE(cant_save_invalid_contour)
{
WIKI_FIXTURE_TEST_CASE(test_dont_allow_invalid_contours, EditorTestFixture)
{
    UNIT_CHECK_GENERATED_EXCEPTION(
        performSaveObjectRequest("tests/data/create_invalid_vegetation_contour.json"),
        topo::InvalidFaceError
    );
}
} // Y_UNIT_TEST_SUITE

} // namespace maps::wiki::tests
