#include "helpers.h"
#include "../checks/utils.h"
#include <yandex/maps/wiki/diffalert/message.h>
#include <yandex/maps/wiki/diffalert/revision/diff_context.h>
#include <yandex/maps/wiki/diffalert/revision/editor_config.h>

#define  UNIT_ASSERT_DOUBLES_CLOSE(A, B, percent) \
    UNIT_ASSERT_DOUBLES_EQUAL(A, B, A * percent * 0.01)

namespace maps::wiki::diffalert::tests {

Y_UNIT_TEST_SUITE_F(utils, SetLogLevelFixture) {

Y_UNIT_TEST(test_sym_diff_area)
{
    const auto snapshotsPair = loadData(
        dataPath("tests_data/sym_diff_area.before.json"),
        dataPath("tests_data/sym_diff_area.after.json"));
    EditorConfig editorConfig(EDITOR_CONFIG_PATH);
    auto result = LongtaskDiffContext::compareSnapshots(
            snapshotsPair.oldBranch, snapshotsPair.oldSnapshotId,
            snapshotsPair.newBranch, snapshotsPair.newSnapshotId,
            RevisionDB::pool(),
            ViewDB::pool(),
            editorConfig);

    UNIT_ASSERT(result.badObjects().empty());
    UNIT_ASSERT(result.badRelations().empty());

    for (const auto& diff: result.diffContexts()) {
        if (diff.objectId() == 21) {
            // deleted ad object

            auto sdArea = symDiffArea(diff);
            UNIT_ASSERT_DOUBLES_CLOSE(sdArea, 0.906e6, 1 /* percent */);

            auto oldArea = contourObjectArea(diff, SnapshotTime::Old);
            UNIT_ASSERT_DOUBLES_CLOSE(sdArea, oldArea.exterior - oldArea.interior, 0.01 /* percent */);

        } else if (diff.objectId() == 81) {
            // moved ad object (no intersection between new and old geometries)
            auto sdArea = symDiffArea(diff);
            UNIT_ASSERT_DOUBLES_CLOSE(sdArea, 1.694e6, 1 /* percent */);

            auto oldArea = contourObjectArea(diff, SnapshotTime::Old);
            auto newArea = contourObjectArea(diff, SnapshotTime::New);
            UNIT_ASSERT_DOUBLES_CLOSE(
                    sdArea,
                    oldArea.exterior - oldArea.interior + newArea.exterior - newArea.interior,
                    0.01 /* percent */);
        } else if (diff.objectId() == 101) {
            // removed a part of external contour for ad object
            auto sdArea = symDiffArea(diff);
            UNIT_ASSERT_DOUBLES_CLOSE(sdArea, 0.467e6, 1 /* percent */);

            auto oldArea = contourObjectArea(diff, SnapshotTime::Old);
            auto newArea = contourObjectArea(diff, SnapshotTime::New);
            UNIT_ASSERT_DOUBLES_CLOSE(
                    sdArea,
                    oldArea.exterior - newArea.exterior,
                    0.01 /* percent */);
        } else if (diff.objectId() == 121) {
            // moved ad object with internal contour (complex
            // intersection between new and old geometries)
            auto sdArea = symDiffArea(diff);
            UNIT_ASSERT_DOUBLES_CLOSE(sdArea, 0.532e6, 1 /* percent */);
        }
    }
}

Y_UNIT_TEST(test_responsibility_ft_type_groups)
{
    std::string commonValues;
    for (const auto ftType : NMAPS_RESPONSIBILITY_FT_TYPES) {
        if (SPRAV_RESPONSIBILITY_FT_TYPES.count(ftType)) {
            commonValues += " " + std::to_string(ftType);
        }
    }
    UNIT_ASSERT_VALUES_EQUAL(commonValues, "");
}

Y_UNIT_TEST(test_prioByRespGroup)
{
    UNIT_ASSERT_EQUAL(prioByRespGroup(Priority(0, 0), TeamResponsibilityGroup::Nmaps), Priority(0, 0));
    UNIT_ASSERT_EQUAL(prioByRespGroup(Priority(1, 0), TeamResponsibilityGroup::Nmaps), Priority(0, 0));
    UNIT_ASSERT_EQUAL(prioByRespGroup(Priority(1, 0), TeamResponsibilityGroup::Sprav), Priority(3, 0));
    UNIT_ASSERT_EQUAL(prioByRespGroup(Priority(10, 0), TeamResponsibilityGroup::Nmaps), Priority(2, 0));
    UNIT_ASSERT_EQUAL(prioByRespGroup(Priority(10, 0), TeamResponsibilityGroup::Sprav), Priority(10, 0));
}

} // Y_UNIT_TEST_SUITE

} // maps::wiki::diffalert::tests
