#include "helpers.h"
#include <maps/wikimap/mapspro/libs/validator/common/magic_strings.h>

#include <yandex/maps/wiki/validator/validator.h>

#include <library/cpp/testing/unittest/registar.h>

namespace rev = maps::wiki::revision;

namespace maps {
namespace wiki {
namespace validator {
namespace tests {

Y_UNIT_TEST_SUITE_F(base_checks, DbFixture) {

Y_UNIT_TEST(test_polygon_feature_base_check)
{
    const DBID commitId = loadJson(*revisionPgPool(), dataPath("polygon_feature_base_check.data.json"));

    const Messages messages = validator.run(
        {"polygon_feature_base_check_aux"},
        *revisionPgPool(),
        rev::TRUNK_BRANCH_ID,
        commitId
    )->drainAllMessages();

    checkMessages(
        messages, {
            { "bad-parent-relation", {{11, 1}, {12, 1}, {13, 1}} },
            { "bad-parent-relation", {{21, 1}, {22, 1}, {23, 1}} }
        }
    );
}

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
    checkMessage(messages[0], "bad-start-junction-relation", {{10, 1}});
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
    checkMessage(messages[0], "bad-end-junction-relation", {{10, 1}, {11, 1}, {12, 1}});
}

Y_UNIT_TEST(test_name_base_check)
{
    const DBID headCommitId = loadJson(
        *revisionPgPool(),
        dataPath("name_base_check.data.json"));

    const Messages messages = validator.run(
        {"rd_base_check_aux"},
        *revisionPgPool(),
        rev::TRUNK_BRANCH_ID,
        headCommitId
    )->drainAllMessages();

    UNIT_ASSERT_VALUES_EQUAL(messages.size(), 1);
    checkMessage(messages[0], "bad-name", {{10, 1}});
}

Y_UNIT_TEST(test_name_loading)
{
    const DBID headCommitId = loadJson(
        *revisionPgPool(),
        dataPath("name_loading.data.json"));

    const Messages messages = validator.run(
        {"name_loading_aux"},
        *revisionPgPool(),
        rev::TRUNK_BRANCH_ID,
        headCommitId
    )->drainAllMessages();

    for (const auto& message: messages) {
        UNIT_FAIL(
            "unexpected message is reported: "
            << message.attributes().description
            << ", object ids: "<< revisionIdsToString(message.revisionIds())
        );
    }
}

Y_UNIT_TEST(test_schedule_loading)
{
    const DBID headCommitId = loadJson(
        *revisionPgPool(),
        dataPath("schedule_loading.data.json"));

    const Messages messages = validator.run(
        {"rd_base_check_aux"},
        *revisionPgPool(),
        rev::TRUNK_BRANCH_ID,
        headCommitId
    )->drainAllMessages();

    UNIT_ASSERT_VALUES_EQUAL(messages.size(), 3);
    for (const auto& message: messages) {
        UNIT_ASSERT_VALUES_EQUAL(message.revisionIds().size(), 1);
        checkMessage(message, "bad-weekdays-mask");
    }
}

} // Y_UNIT_TEST_SUITE

} // namespace tests
} // namespace validator
} // namespace wiki
} // namespace maps
