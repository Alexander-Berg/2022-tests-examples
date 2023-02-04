#include <maps/wikimap/mapspro/services/editor/src/acl_role_info.h>
#include <maps/wikimap/mapspro/services/editor/src/configs/config.h>

#include "maps/wikimap/mapspro/services/editor/src/tests/helpers/tests_common.h"

#include <maps/libs/common/include/exception.h>
#include <yandex/maps/wiki/configs/editor/category_groups.h>

using namespace maps::wiki::moderation;

namespace maps::wiki::tests {

namespace {

class Fixture : public EditorTestFixture {
public:
    Fixture()
    {
        for (const auto& group: cfg()->editor()->categoryGroups().allGroups()) {
            allCategoryGroups.insert(group.first);
        }
    }
    StringSet allCategoryGroups;
};

} // namespace

/*****************************************************************************/
/*                                 T E S T S                                 */
/*****************************************************************************/
Y_UNIT_TEST_SUITE(acl_role_info)
{

WIKI_FIXTURE_TEST_CASE(should_extract_role_name, Fixture)
{
    UNIT_ASSERT_EQUAL(AclRoleInfo{"expert"}.roleName(), "expert");
    UNIT_ASSERT_EQUAL(AclRoleInfo{"expert.addr_group"}.roleName(), "expert");
    UNIT_ASSERT_EQUAL(AclRoleInfo{"expert.addr_group.bld_group"}.roleName(), "expert");
    UNIT_ASSERT_EQUAL(AclRoleInfo{"expert.not.addr_group"}.roleName(), "expert");
    UNIT_ASSERT_EQUAL(AclRoleInfo{"expert.not.addr_group.bld_group"}.roleName(), "expert");
}


WIKI_FIXTURE_TEST_CASE(should_not_allow_an_empty_name, Fixture)
{
    UNIT_CHECK_GENERATED_EXCEPTION(AclRoleInfo{""}, RuntimeError);
}


WIKI_FIXTURE_TEST_CASE(should_set_trust_level, Fixture)
{
    UNIT_ASSERT_EQUAL(AclRoleInfo{"role"}.trustLevel()            , TrustLevel::NotTrusted);
    UNIT_ASSERT_EQUAL(AclRoleInfo{"expert"}.trustLevel()          , TrustLevel::AcceptAndModerate);
    UNIT_ASSERT_EQUAL(AclRoleInfo{"autoapprove-edit"}.trustLevel(), TrustLevel::AcceptAndModerate);
    UNIT_ASSERT_EQUAL(AclRoleInfo{"moderator"}.trustLevel()       , TrustLevel::AcceptAndModerate);
    UNIT_ASSERT_EQUAL(AclRoleInfo{"autoapprove-mod"}.trustLevel() , TrustLevel::AcceptAndModerate);
    UNIT_ASSERT_EQUAL(AclRoleInfo{"yandex-moderator"}.trustLevel(), TrustLevel::AcceptAndModerate);
    UNIT_ASSERT_EQUAL(AclRoleInfo{"cartographer"}.trustLevel()    , TrustLevel::SkipModeration);
}


WIKI_FIXTURE_TEST_CASE(should_not_allow_period_at_the_end, Fixture)
{
    UNIT_CHECK_GENERATED_EXCEPTION(AclRoleInfo{"role."}, RuntimeError);
    UNIT_CHECK_GENERATED_EXCEPTION(AclRoleInfo{"expert."}, RuntimeError);
}


WIKI_FIXTURE_TEST_CASE(should_not_allow_bare_not_at_the_end, Fixture)
{
    UNIT_CHECK_GENERATED_EXCEPTION(AclRoleInfo{"expert.not"}, RuntimeError);
    UNIT_CHECK_GENERATED_EXCEPTION(AclRoleInfo{"expert.not."}, RuntimeError);
}


WIKI_FIXTURE_TEST_CASE(should_not_allow_category_group_ids, Fixture)
{
    UNIT_CHECK_GENERATED_EXCEPTION(AclRoleInfo{"role.addr_group"}, RuntimeError);
}


WIKI_FIXTURE_TEST_CASE(should_allow_auto_for_expert_role_only, Fixture)
{
    UNIT_ASSERT_NO_EXCEPTION(AclRoleInfo{"expert.auto"});

    UNIT_CHECK_GENERATED_EXCEPTION(AclRoleInfo{"role.auto"}, RuntimeError);
    UNIT_CHECK_GENERATED_EXCEPTION(AclRoleInfo{"autoapprove-edit.auto"}, RuntimeError);
    UNIT_CHECK_GENERATED_EXCEPTION(AclRoleInfo{"moderator.auto"}, RuntimeError);
    UNIT_CHECK_GENERATED_EXCEPTION(AclRoleInfo{"autoapprove-mod.auto"}, RuntimeError);
    UNIT_CHECK_GENERATED_EXCEPTION(AclRoleInfo{"yandex-moderator.auto"}, RuntimeError);
    UNIT_CHECK_GENERATED_EXCEPTION(AclRoleInfo{"cartographer.auto"}, RuntimeError);
}


WIKI_FIXTURE_TEST_CASE(should_allow_category_group_ids, Fixture)
{
    UNIT_ASSERT_NO_EXCEPTION(AclRoleInfo{"expert.addr_group"});
    UNIT_ASSERT_NO_EXCEPTION(AclRoleInfo{"autoapprove-edit.addr_group"});
    UNIT_ASSERT_NO_EXCEPTION(AclRoleInfo{"moderator.addr_group"});
    UNIT_ASSERT_NO_EXCEPTION(AclRoleInfo{"autoapprove-mod.addr_group"});
    UNIT_ASSERT_NO_EXCEPTION(AclRoleInfo{"yandex-moderator.addr_group"});
    UNIT_ASSERT_NO_EXCEPTION(AclRoleInfo{"cartographer.addr_group"});
}


WIKI_FIXTURE_TEST_CASE(should_set_all_category_group_ids, Fixture)
{
    UNIT_ASSERT_EQUAL(AclRoleInfo{"role"}.categoryGroupIds(), allCategoryGroups);
    UNIT_ASSERT_EQUAL(AclRoleInfo{"expert"}.categoryGroupIds(), allCategoryGroups);
    UNIT_ASSERT_EQUAL(AclRoleInfo{"expert.auto"}.categoryGroupIds(), allCategoryGroups);
    UNIT_ASSERT_EQUAL(AclRoleInfo{"expert.not.common"}.categoryGroupIds(), allCategoryGroups);
}


WIKI_FIXTURE_TEST_CASE(should_allow_moderate_all_groups, Fixture)
{
    // Trusted roles
    UNIT_ASSERT_EQUAL(AclRoleInfo{"expert"}.canProcessAllGroups(), true);
    UNIT_ASSERT_EQUAL(AclRoleInfo{"autoapprove-edit"}.canProcessAllGroups(), true);
    UNIT_ASSERT_EQUAL(AclRoleInfo{"yandex-moderator"}.canProcessAllGroups(), true);
    UNIT_ASSERT_EQUAL(AclRoleInfo{"autoapprove-mod"}.canProcessAllGroups(), true);
    UNIT_ASSERT_EQUAL(AclRoleInfo{"cartographer"}.canProcessAllGroups(), true);

    // Modifiers which does not affect groups
    UNIT_ASSERT_EQUAL(AclRoleInfo{"expert.auto.not.common"}.canProcessAllGroups(), true);
}


WIKI_FIXTURE_TEST_CASE(should_not_allow_moderate_all_groups, Fixture)
{
    // Untrusted users
    UNIT_ASSERT_EQUAL(AclRoleInfo{"role"}.canProcessAllGroups(), false);

    // Modifiers which affects groups
    UNIT_ASSERT_EQUAL(AclRoleInfo{"expert.common"}.canProcessAllGroups(), false);
    UNIT_ASSERT_EQUAL(AclRoleInfo{"expert.addr_group"}.canProcessAllGroups(), false);
    UNIT_ASSERT_EQUAL(AclRoleInfo{"expert.addr_group.common"}.canProcessAllGroups(), false);
    UNIT_ASSERT_EQUAL(AclRoleInfo{"expert.not.addr_group"}.canProcessAllGroups(), false);
    UNIT_ASSERT_EQUAL(AclRoleInfo{"expert.not.addr_group.common"}.canProcessAllGroups(), false);
}


WIKI_FIXTURE_TEST_CASE(should_set_category_group_ids, Fixture)
{
    UNIT_ASSERT_EQUAL(AclRoleInfo{"expert.addr_group"}.categoryGroupIds(), StringSet({"addr_group"}));
    UNIT_ASSERT_EQUAL(
        AclRoleInfo{"expert.addr_group.bld_group"}.categoryGroupIds(),
        StringSet({"addr_group", "bld_group"}));
    {
        StringSet allButAddr{allCategoryGroups};
        allButAddr.erase("addr_group");
        UNIT_ASSERT_EQUAL(AclRoleInfo{"expert.not.addr_group"}.categoryGroupIds(), allButAddr);
    }
    {
        StringSet allButAddrAndBld{allCategoryGroups};
        allButAddrAndBld.erase("addr_group");
        allButAddrAndBld.erase("bld_group");
        UNIT_ASSERT_EQUAL(AclRoleInfo{"expert.not.addr_group.bld_group"}.categoryGroupIds(), allButAddrAndBld);
    }
}


WIKI_FIXTURE_TEST_CASE(should_not_set_wrong_category_group_ids, Fixture)
{
    UNIT_CHECK_GENERATED_EXCEPTION(AclRoleInfo{"expert.wrong_group"}, RuntimeError);
    UNIT_CHECK_GENERATED_EXCEPTION(AclRoleInfo{"expert.addr_group.wrong_group"}, RuntimeError);
    UNIT_CHECK_GENERATED_EXCEPTION(AclRoleInfo{"expert.wrong_group.addr_group"}, RuntimeError);
    UNIT_CHECK_GENERATED_EXCEPTION(AclRoleInfo{"expert.addr_group.wrong_group.bld_group"}, RuntimeError);
    UNIT_CHECK_GENERATED_EXCEPTION(AclRoleInfo{"expert.not.wrong_group"}, RuntimeError);
}


WIKI_FIXTURE_TEST_CASE(should_not_set_access_to_common_tasks, Fixture)
{
    UNIT_ASSERT_EQUAL(AclRoleInfo{"expert.addr_group"}.hasAccessToCommonTasks(), false);
    UNIT_ASSERT_EQUAL(AclRoleInfo{"expert.addr_group.bld_group"}.hasAccessToCommonTasks(), false);
    UNIT_ASSERT_EQUAL(AclRoleInfo{"expert.not.common"}.hasAccessToCommonTasks(), false);
    UNIT_ASSERT_EQUAL(AclRoleInfo{"expert.auto.not.common"}.hasAccessToCommonTasks(), false);
}


WIKI_FIXTURE_TEST_CASE(should_set_access_to_common_tasks, Fixture)
{
    UNIT_ASSERT_EQUAL(AclRoleInfo{"expert"}.hasAccessToCommonTasks(), true);
    UNIT_ASSERT_EQUAL(AclRoleInfo{"expert.auto"}.hasAccessToCommonTasks(), true);
    UNIT_ASSERT_EQUAL(AclRoleInfo{"expert.not.addr_group"}.hasAccessToCommonTasks(), true);
    {
        AclRoleInfo roleInfo("expert.common");
        UNIT_ASSERT_EQUAL(roleInfo.hasAccessToCommonTasks(), true);
        UNIT_ASSERT_EQUAL(roleInfo.categoryGroupIds().size(), 0);
    }
    {
        AclRoleInfo roleInfo("expert.addr_group.common");
        UNIT_ASSERT_EQUAL(roleInfo.hasAccessToCommonTasks(), true);
        UNIT_ASSERT_EQUAL(roleInfo.categoryGroupIds(), StringSet({"addr_group"}));
    }
    {
        AclRoleInfo roleInfo("expert.addr_group.common.bld_group");
        UNIT_ASSERT_EQUAL(roleInfo.hasAccessToCommonTasks(), true);
        UNIT_ASSERT_EQUAL(roleInfo.categoryGroupIds(), StringSet({"addr_group", "bld_group"}));
    }
}


WIKI_FIXTURE_TEST_CASE(should_moderate_category, Fixture)
{
    {
        UNIT_ASSERT_EQUAL(AclRoleInfo{"moderator"}.canProcessCategory("addr"), true);
        UNIT_ASSERT_EQUAL(AclRoleInfo{"moderator.addr_group"}.canProcessCategory("addr"), true);
        UNIT_ASSERT_EQUAL(AclRoleInfo{"moderator.addr_group.rd_group"}.canProcessCategory("addr"), true);
        UNIT_ASSERT_EQUAL(AclRoleInfo{"moderator.common.addr_group"}.canProcessCategory("addr"), true);
        UNIT_ASSERT_EQUAL(AclRoleInfo{"moderator.not.rd_group"}.canProcessCategory("addr"), true);
        UNIT_ASSERT_EQUAL(AclRoleInfo{"moderator.not.common"}.canProcessCategory("addr"), true);
    }
    {
        UNIT_ASSERT_EQUAL(AclRoleInfo{"autoapprove-mod"}.canProcessCategory("addr"), true);
        UNIT_ASSERT_EQUAL(AclRoleInfo{"autoapprove-mod.addr_group"}.canProcessCategory("addr"), true);
        UNIT_ASSERT_EQUAL(AclRoleInfo{"autoapprove-mod.addr_group.rd_group"}.canProcessCategory("addr"), true);
        UNIT_ASSERT_EQUAL(AclRoleInfo{"autoapprove-mod.common.addr_group"}.canProcessCategory("addr"), true);
        UNIT_ASSERT_EQUAL(AclRoleInfo{"autoapprove-mod.not.rd_group"}.canProcessCategory("addr"), true);
        UNIT_ASSERT_EQUAL(AclRoleInfo{"autoapprove-mod.not.common"}.canProcessCategory("addr"), true);
    }
}


WIKI_FIXTURE_TEST_CASE(should_not_moderate_category, Fixture)
{
    {
        UNIT_ASSERT_EQUAL(AclRoleInfo{"moderator.rd_group"}.canProcessCategory("addr"), false);
        UNIT_ASSERT_EQUAL(AclRoleInfo{"moderator.rd_group.bld_group"}.canProcessCategory("addr"), false);
        UNIT_ASSERT_EQUAL(AclRoleInfo{"moderator.common.rd_group"}.canProcessCategory("addr"), false);
        UNIT_ASSERT_EQUAL(AclRoleInfo{"moderator.not.addr_group"}.canProcessCategory("addr"), false);
    }
    {
        UNIT_ASSERT_EQUAL(AclRoleInfo{"autoapprove-mod.rd_group"}.canProcessCategory("addr"), false);
        UNIT_ASSERT_EQUAL(AclRoleInfo{"autoapprove-mod.rd_group.bld_group"}.canProcessCategory("addr"), false);
        UNIT_ASSERT_EQUAL(AclRoleInfo{"autoapprove-mod.common.rd_group"}.canProcessCategory("addr"), false);
        UNIT_ASSERT_EQUAL(AclRoleInfo{"autoapprove-mod.not.addr_group"}.canProcessCategory("addr"), false);
    }
}


WIKI_FIXTURE_TEST_CASE(should_not_set_auto, Fixture)
{
    UNIT_ASSERT_EQUAL(AclRoleInfo{"role"}.isAuto(), false);
    UNIT_ASSERT_EQUAL(AclRoleInfo{"expert"}.isAuto(), false);
    UNIT_ASSERT_EQUAL(AclRoleInfo{"autoapprove-edit"}.isAuto(), false);
    UNIT_ASSERT_EQUAL(AclRoleInfo{"moderator"}.isAuto(), false);
    UNIT_ASSERT_EQUAL(AclRoleInfo{"autoapprove-mod"}.isAuto(), false);
    UNIT_ASSERT_EQUAL(AclRoleInfo{"yandex-moderator"}.isAuto(), false);
    UNIT_ASSERT_EQUAL(AclRoleInfo{"cartographer"}.isAuto(), false);
}


WIKI_FIXTURE_TEST_CASE(should_set_auto, Fixture)
{
    UNIT_ASSERT_EQUAL(AclRoleInfo{"expert.auto"}.isAuto(), true);
    UNIT_ASSERT_EQUAL(AclRoleInfo{"expert.auto.addr_group"}.isAuto(), true);
    UNIT_ASSERT_EQUAL(AclRoleInfo{"expert.auto.not.addr_group"}.isAuto(), true);
    UNIT_ASSERT_EQUAL(AclRoleInfo{"expert.auto.common"}.isAuto(), true);
}

WIKI_FIXTURE_TEST_CASE(should_not_allow_service_flags_in_wrong_places, Fixture)
{
    UNIT_CHECK_GENERATED_EXCEPTION(AclRoleInfo{"expert.addr_group.not"}, RuntimeError);
    UNIT_CHECK_GENERATED_EXCEPTION(AclRoleInfo{"expert.addr_group.auto"}, RuntimeError);

    UNIT_CHECK_GENERATED_EXCEPTION(AclRoleInfo{"expert.addr_group.not."}, RuntimeError);
    UNIT_CHECK_GENERATED_EXCEPTION(AclRoleInfo{"expert.addr_group.auto."}, RuntimeError);

    UNIT_CHECK_GENERATED_EXCEPTION(AclRoleInfo{"expert.addr_group.not.bld_group"}, RuntimeError);
    UNIT_CHECK_GENERATED_EXCEPTION(AclRoleInfo{"expert.addr_group.auto.bld_group"}, RuntimeError);

    UNIT_CHECK_GENERATED_EXCEPTION(AclRoleInfo{"expert.not.auto.addr_group"}, RuntimeError);
}


WIKI_FIXTURE_TEST_CASE(should_not_add_service_names_into_groups, Fixture)
{
    UNIT_ASSERT_EQUAL(AclRoleInfo{"expert.common.addr_group"}.categoryGroupIds().count(CATEGORY_GROUP_COMMON), 0);
    UNIT_ASSERT_EQUAL(AclRoleInfo{"expert.not.addr_group"}.categoryGroupIds().count(NOT_FLAG), 0);
    UNIT_ASSERT_EQUAL(AclRoleInfo{"expert.auto.addr_group"}.categoryGroupIds().count(AUTO_FLAG), 0);
}


WIKI_FIXTURE_TEST_CASE(service_names_should_not_be_part_of_cfg_groups, Fixture)
{
    UNIT_ASSERT_EQUAL(allCategoryGroups.count(CATEGORY_GROUP_COMMON), 0);
    UNIT_ASSERT_EQUAL(allCategoryGroups.count(NOT_FLAG), 0);
    UNIT_ASSERT_EQUAL(allCategoryGroups.count(AUTO_FLAG), 0);
}

} // Y_UNIT_TEST_SUITE

} // namespace maps::wiki::tests
