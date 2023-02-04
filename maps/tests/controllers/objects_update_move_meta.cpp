#include "controller_tests_common_includes.h"
#include <maps/wikimap/mapspro/services/editor/src/tests/helpers/controller_helpers.h>
#include <maps/wikimap/mapspro/services/editor/src/tests/helpers/db_helpers.h>
#include <maps/wikimap/mapspro/services/editor/src/tests/helpers/helpers.h>

namespace maps::wiki::tests {

Y_UNIT_TEST_SUITE(objects_update_move_meta)
{
WIKI_FIXTURE_TEST_CASE(test_objects_update_move_meta, EditorTestFixture)
{
    ObjectsUpdateMoveMeta::Request request {
        TESTS_USER
    };
    ObjectsUpdateMoveMeta controller(request);
    auto suggestedResult = controller();
    auto jsonFormatter = Formatter::create(common::FormatType::JSON,
        make_unique<TestFormatterContext>());
    validateJsonResponse((*jsonFormatter)(*suggestedResult), "ObjectsUpdateMoveMeta");
}
}//Y_UNIT_TEST_SUITE

} // namespace maps::wiki::tests
