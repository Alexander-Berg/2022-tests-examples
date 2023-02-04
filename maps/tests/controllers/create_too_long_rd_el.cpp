#include "controller_tests_common_includes.h"
#include <maps/wikimap/mapspro/services/editor/src/tests/helpers/controller_helpers.h>
#include <maps/wikimap/mapspro/services/editor/src/tests/helpers/db_helpers.h>
#include <maps/wikimap/mapspro/services/editor/src/tests/helpers/helpers.h>

#include <yandex/maps/wiki/topo/exception.h>

namespace maps::wiki::tests {

Y_UNIT_TEST_SUITE(test_restrictions_override)
{
WIKI_FIXTURE_TEST_CASE(test_create_too_long_rd_el, EditorTestFixture)
{
    populateACLPermissionsTree();
    TUid userUID = createRandomUser();
    setUserPermissions(
        userUID,
        {
            {"mpro", "editor", "rd_el"},
            {"mpro", "editor", "rd_jc"}
        });
    UNIT_CHECK_GENERATED_EXCEPTION(
        performSaveObjectRequest("tests/data/too_long_rd_el.json", makeObservers<>(), userUID),
        maps::wiki::LogicExceptionWithLocation
    );
    setUserPermissions(
        userUID,
        {{"mpro", "tools", "ignore-restrictions"}});
    auto work = cfg()->poolCore().masterWriteableTransaction();
    acl::CheckContext globalContext(
        userUID,
        std::vector<std::string>{},
        *work,
        std::set<acl::User::Status>{acl::User::Status::Active});
    acl::SubjectPath aclPath("mpro/tools/ignore-restrictions");
    UNIT_ASSERT_NO_EXCEPTION(aclPath.check(globalContext));
    UNIT_ASSERT_NO_EXCEPTION(
        performSaveObjectRequest("tests/data/too_long_rd_el.json", makeObservers<>(), userUID)
    );

}
} // Y_UNIT_TEST_SUITE

} // namespace maps::wiki::tests
