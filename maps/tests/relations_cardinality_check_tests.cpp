#include "helpers.h"

#include <maps/wikimap/mapspro/libs/validator/common/magic_strings.h>

#include <yandex/maps/wiki/validator/validator.h>

#include <maps/libs/log8/include/log8.h>

#include <library/cpp/testing/unittest/registar.h>

namespace gl = maps::geolib3;
namespace rev = maps::wiki::revision;

namespace maps {
namespace wiki {
namespace validator {
namespace tests {

using Fixture = CompositeFixture<EditorConfigValidatorMixin, DbMixin>;

Y_UNIT_TEST_SUITE_F(relations_cardinality_checks, Fixture) {

Y_UNIT_TEST(test_road_element_base_check_missing_start)
{
    const DBID headCommitId = loadJson(
        *revisionPgPool(),
        dataPath("road_element_base_check_missing_start.data.json"));

    const Messages messages = validator.run(
        {"rd_base_check_aux"},
        *revisionPgPool(),
        rev::TRUNK_BRANCH_ID,
        headCommitId
    )->drainAllMessages();

    UNIT_ASSERT_VALUES_EQUAL(messages.size(), 1);
    checkMessage(messages[0], "not-enough-slave-relations-of-role-start", {{10, 1}});
}

Y_UNIT_TEST(test_road_element_base_check_doubled_end)
{
    const DBID commitId = loadJson(
        *revisionPgPool(),
        dataPath("road_element_base_check_doubled_end.data.json"));

    const Messages messages = validator.run(
        {"rd_base_check_aux"},
        *revisionPgPool(),
        rev::TRUNK_BRANCH_ID,
        commitId
    )->drainAllMessages();

    UNIT_ASSERT_VALUES_EQUAL(messages.size(), 1);
    checkMessage(messages[0], "too-many-slave-relations-of-role-end", {{10, 1}, {11, 1}, {12, 1}});
}

Y_UNIT_TEST(test_addr_associated_with_two_roads)
{
    const DBID commitId = loadJson(
        *revisionPgPool(),
        dataPath("addr_associated_with_two_roads.data.json"));

    const Messages messages = validator.run(
        {"rd_base_check_aux2"},
        *revisionPgPool(),
        rev::TRUNK_BRANCH_ID,
        commitId
    )->drainAllMessages();

    UNIT_ASSERT_VALUES_EQUAL(messages.size(), 1);
    checkMessage(messages[0], "too-many-master-relations-of-role-associated_with", {{40, 1}, {10, 1}, {11, 1}});
}

Y_UNIT_TEST(test_transport_metro_thread_without_transport_metro_line)
{
    const DBID commitId = loadJson(
        *revisionPgPool(),
        dataPath("transport_metro_thread_without_transport_metro_line.data.json"));

    const Messages messages = validator.run(
        {"master_min_occurs_base_check"},
        *revisionPgPool(),
        rev::TRUNK_BRANCH_ID,
        commitId
    )->drainAllMessages();

    UNIT_ASSERT_VALUES_EQUAL(messages.size(), 1);
    checkMessage(messages[0], "not-enough-master-relations-of-role-assigned_thread", {{10, 1}});
}

Y_UNIT_TEST(test_road_element_base_check_wrong_relation_role_id)
{
    const DBID headCommitId = loadJson(
        *revisionPgPool(),
        dataPath("road_element_base_check_wrong_relation_role_id.data.json"));

    const Messages messages = validator.run(
        {"rd_base_check_aux3"},
        *revisionPgPool(),
        rev::TRUNK_BRANCH_ID,
        headCommitId
    )->drainAllMessages();

    checkMessages(
        messages, {
            {"wrong-master-relations-of-role-some_bad_role", {{10, 1}, {13, 1}}},
            {"wrong-slave-relations-of-role-some_bad_role", {{10, 1}, {13, 1}}}
        }
    );
}

Y_UNIT_TEST(test_road_element_base_check_wrong_relative_category)
{
    const DBID headCommitId = loadJson(
        *revisionPgPool(),
        dataPath("road_element_base_check_wrong_relative_category.data.json"));

    const Messages messages = validator.run(
        {"rd_base_check_aux3"},
        *revisionPgPool(),
        rev::TRUNK_BRANCH_ID,
        headCommitId
    )->drainAllMessages();

    checkMessages(
        messages, {
            {"wrong-master-relations-of-role-end", {{10, 1}, {12, 1}}},
            {"wrong-slave-relative-category-ad_jc", {{10, 1}, {12, 1}}}
        }
    );
}

} // Y_UNIT_TEST_SUITE_F

} // namespace tests
} // namespace validator
} // namespace wiki
} // namespace maps
