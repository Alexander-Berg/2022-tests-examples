#include <maps/wikimap/mapspro/services/editor/src/configs/config.h>
#include <maps/wikimap/mapspro/services/editor/src/serialize/formatter.h>
#include <maps/wikimap/mapspro/services/editor/src/serialize/save_object_parser.h>
#include <maps/wikimap/mapspro/services/editor/src/utils.h>

#include "controller_tests_common_includes.h"
#include <maps/wikimap/mapspro/services/editor/src/tests/helpers/controller_helpers.h>
#include <maps/wikimap/mapspro/services/editor/src/tests/helpers/db_helpers.h>
#include <maps/wikimap/mapspro/services/editor/src/tests/helpers/helpers.h>

#include <yandex/maps/wiki/revision/branch.h>

namespace maps::wiki::tests {

Y_UNIT_TEST_SUITE(prohibited_objects)
{
WIKI_FIXTURE_TEST_CASE(prohibited_objects_create_inside, EditorTestFixture)
{
    performObjectsImport("tests/data/create_small_turkey_road.json", db.connectionString());
    executeSqlFile("tests/sql/fill_small_turkey_contour_objects_geom.sql");
    populateACLPermissionsTree();
    TUid userUID = createRandomUser();
    setUserPermissions(
        userUID,
        {
            {"mpro", "editor", "rd_el"},
            {"mpro", "editor", "rd_jc"},
            {"mpro", "editor", "cond_cam"}
        });
    UNIT_CHECK_GENERATED_EXCEPTION(
        performSaveObjectRequest("tests/data/create_cam_inside_small_turkey.json", makeObservers<>(), userUID),
        LogicException);
    setUserPermissions(
        userUID,
        {{"mpro", "tools", "ignore-restrictions", "cond_cam"}});
    UNIT_ASSERT_NO_EXCEPTION(
        performSaveObjectRequest("tests/data/create_cam_inside_small_turkey.json", makeObservers<>(), userUID));
}

WIKI_FIXTURE_TEST_CASE(prohibited_objects_create_outside_and_move, EditorTestFixture)
{
    performObjectsImport("tests/data/create_small_turkey_road.json", db.connectionString());
    executeSqlFile("tests/sql/fill_small_turkey_contour_objects_geom.sql");
    populateACLPermissionsTree();
    TUid userUID = createRandomUser();
    setUserPermissions(
        userUID,
        {
            {"mpro", "editor", "rd_el"},
            {"mpro", "editor", "rd_jc"},
            {"mpro", "editor", "cond_cam"}
        });
    UNIT_ASSERT_NO_EXCEPTION(
        performSaveObjectRequest("tests/data/create_cam_outside_small_turkey.json", makeObservers<>(), userUID));
    UNIT_CHECK_GENERATED_EXCEPTION(
        performSaveObjectRequest("tests/data/move_cam_inside_small_turkey.json", makeObservers<>(), userUID),
        LogicException);
    setUserPermissions(
        userUID,
        {{"mpro", "tools", "ignore-restrictions", "cond_cam"}});
    UNIT_ASSERT_NO_EXCEPTION(
        performSaveObjectRequest("tests/data/move_cam_inside_small_turkey.json", makeObservers<>(), userUID));
}
} // Y_UNIT_TEST_SUITE

} // namespace maps::wiki::tests
