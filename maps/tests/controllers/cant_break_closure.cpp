#include "controller_tests_common_includes.h"
#include <maps/wikimap/mapspro/services/editor/src/tests/helpers/helpers.h>
#include <maps/wikimap/mapspro/services/editor/src/tests/helpers/db_helpers.h>
#include <maps/wikimap/mapspro/services/editor/src/tests/helpers/controller_helpers.h>

namespace maps::wiki::tests {

Y_UNIT_TEST_SUITE(cant_break_closure)
{
WIKI_FIXTURE_TEST_CASE(test_try_to_break_closure_bu_indirect_edit, EditorTestFixture)
{
    performObjectsImport("tests/data/valid_closure.json", db.connectionString());
    UNIT_CHECK_GENERATED_EXCEPTION(
        performSaveObjectRequest("tests/data/valid_closure_move_element.json"),
        maps::wiki::LogicException
    );
}
} // Y_UNIT_TEST_SUITE

} // namespace maps::wiki::tests
