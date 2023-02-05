#include "helpers.h"
#include "parsed_message.h"

#include <yandex/maps/wiki/diffalert/runner.h>
#include <yandex/maps/wiki/diffalert/revision/runner.h>
#include <yandex/maps/wiki/diffalert/revision/diff_context.h>
#include <yandex/maps/wiki/diffalert/revision/editor_config.h>

#include <maps/libs/common/include/exception.h>

#include <vector>

namespace maps {
namespace wiki {
namespace diffalert {
namespace tests {

Y_UNIT_TEST_SUITE_F(interal_check_error, SetLogLevelFixture) {

Y_UNIT_TEST(no_geom)
{
    EditorConfig editorConfig(EDITOR_CONFIG_PATH);
    const auto snapshotPair = loadData(
        dataPath("tests_data/internal_check_error_no_geom.before.json"),
        dataPath("tests_data/internal_check_error_no_geom.after.json")
    );

    auto result = LongtaskDiffContext::compareSnapshots(
        snapshotPair.oldBranch, snapshotPair.oldSnapshotId,
        snapshotPair.newBranch, snapshotPair.newSnapshotId,
        RevisionDB::pool(),
        ViewDB::pool(),
        editorConfig);

    UNIT_ASSERT(result.badObjects().empty());
    UNIT_ASSERT(result.badRelations().empty());

    UNIT_CHECK_GENERATED_EXCEPTION(
        messagesFromOutput(runEditorChecks, result.diffContexts()),
        maps::RuntimeError
    );

    compare(
        messagesFromOutput(runLongTaskChecks, result.diffContexts()),
        { {41, "0.0", "check-internal-error"} },
        "longtask"
    );
}

Y_UNIT_TEST(no_attr)
{
    EditorConfig editorConfig(EDITOR_CONFIG_PATH);
    const auto snapshotPair = loadData(
        dataPath("tests_data/internal_check_error_no_attr.before.json"),
        dataPath("tests_data/internal_check_error_no_attr.after.json")
    );

    auto result = LongtaskDiffContext::compareSnapshots(
        snapshotPair.oldBranch, snapshotPair.oldSnapshotId,
        snapshotPair.newBranch, snapshotPair.newSnapshotId,
        RevisionDB::pool(),
        ViewDB::pool(),
        editorConfig);

    UNIT_ASSERT(result.badObjects().empty());
    UNIT_ASSERT(result.badRelations().empty());

    UNIT_CHECK_GENERATED_EXCEPTION(
        messagesFromOutput(runEditorChecks, result.diffContexts()),
        maps::RuntimeError
    );

    const auto msgs = messagesFromOutput(runLongTaskChecks, result.diffContexts());

    compare(
        messagesFromOutput(runLongTaskChecks, result.diffContexts()),
        { {13, "0.0", "check-internal-error"} },
        "longtask"
    );
}

Y_UNIT_TEST(bad_objects)
{
    EditorConfig editorConfig(EDITOR_CONFIG_PATH);
    const auto snapshotPair = loadData(
        dataPath("tests_data/bad_objects.before.json"),
        dataPath("tests_data/bad_objects.after.json")
    );

    auto result = LongtaskDiffContext::compareSnapshots(
        snapshotPair.oldBranch, snapshotPair.oldSnapshotId,
        snapshotPair.newBranch, snapshotPair.newSnapshotId,
        RevisionDB::pool(),
        ViewDB::pool(),
        editorConfig);

    TIds expectedBadObjects{21, 10000};
    TIds expectedBadRelations{10004};

    UNIT_ASSERT_EQUAL(result.badObjects(), expectedBadObjects);
    UNIT_ASSERT_EQUAL(result.badRelations(), expectedBadRelations);

    UNIT_ASSERT_VALUES_EQUAL(result.diffContexts().size(), 4);
}

} // Y_UNIT_TEST_SUITE_F

} // namespace tests
} // namespace diffalert
} // namespace wiki
} // namespace maps
