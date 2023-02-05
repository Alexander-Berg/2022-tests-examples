#include "helpers.h"
#include <yandex/maps/wiki/diffalert/revision/diff_context.h>
#include <yandex/maps/wiki/diffalert/revision/diff_envelopes.h>
#include <yandex/maps/wiki/diffalert/revision/editor_config.h>

#include <yandex/maps/wiki/revision/branch_manager.h>
#include <yandex/maps/wiki/revision/snapshot_id.h>
#include <yandex/maps/wiki/revision/revisionsgateway.h>

#include <maps/libs/geolib/include/conversion.h>

#include <iostream>
#include <cmath>
#include <map>

namespace maps {
namespace wiki {
namespace diffalert {
namespace tests {

namespace {

bool equal(const Envelope& left, const Envelope& right)
{
    const double EPS = 1e-2;
    return std::abs(left.getMinX() - right.getMinX()) < EPS
        && std::abs(left.getMinY() - right.getMinY()) < EPS
        && std::abs(left.getMaxX() - right.getMaxX()) < EPS
        && std::abs(left.getMaxY() - right.getMaxY()) < EPS;
}

std::string
notEqualMessage(const Envelope& left, const Envelope& right)
{
    std::stringstream ss;
    ss << left << " != " << right;
    return ss.str();
}

#define CHECK_ENVELOPES_EQUAL(left, right) \
    UNIT_ASSERT_C(equal(left, right), notEqualMessage(left, right));

struct DataContext
{
    DataContext()
        : editorConfig(EDITOR_CONFIG_PATH)
    {
        snapshotsPairPtr.reset(new SnapshotsPair(loadData(
            dataPath("tests_data/diff_envelopes_tests.before.json"),
            dataPath("tests_data/diff_envelopes_tests.after.json"))));

        auto result = LongtaskDiffContext::compareSnapshots(
            snapshotsPairPtr->oldBranch, snapshotsPairPtr->oldSnapshotId,
            snapshotsPairPtr->newBranch, snapshotsPairPtr->newSnapshotId,
            RevisionDB::pool(),
            ViewDB::pool(),
            editorConfig);
        diffs = std::move(result).diffContexts();
    }

    EditorConfig editorConfig;
    std::unique_ptr<SnapshotsPair> snapshotsPairPtr;
    std::vector<LongtaskDiffContext> diffs;
};

DataContext& dataContext()
{
    static DataContext dc;
    return dc;
}

DiffEnvelopes objectDiffEnvelopes(TId objectId)
{
    std::map<TId, const LongtaskDiffContext*> diffContextById;
    for (const auto& d : dataContext().diffs) {
        diffContextById.insert({d.objectId(), &d});
    }

    auto txn = RevisionDB::pool().slaveTransaction();
    revision::RevisionsGateway rg(*txn, dataContext().snapshotsPairPtr->oldBranch);
    auto oldSnapshot = rg.snapshot(dataContext().snapshotsPairPtr->oldSnapshotId);

    return calcObjectDiffEnvelopes(objectId, diffContextById, oldSnapshot);
}

} // namespace

Y_UNIT_TEST_SUITE_F(diff_envelopes, SetLogLevelFixture) {

Y_UNIT_TEST(change_road_attributes)
{
    auto rdEnvelopes = objectDiffEnvelopes(31);

    CHECK_ENVELOPES_EQUAL(rdEnvelopes.before, rdEnvelopes.after);
    CHECK_ENVELOPES_EQUAL(rdEnvelopes.added, Envelope());
    CHECK_ENVELOPES_EQUAL(rdEnvelopes.removed, Envelope());
    CHECK_ENVELOPES_EQUAL(
            rdEnvelopes.before,
            Envelope(4194369.14, 4195424.93, 7490131.29, 7491163.19));
}

Y_UNIT_TEST(edit_ad_element)
{
    auto adEnvelopes = objectDiffEnvelopes(81);
    auto elementEnvelopes = objectDiffEnvelopes(51);

    CHECK_ENVELOPES_EQUAL(adEnvelopes.removed, elementEnvelopes.removed);
    CHECK_ENVELOPES_EQUAL(adEnvelopes.added, elementEnvelopes.added);
    CHECK_ENVELOPES_EQUAL(
            adEnvelopes.removed,
            Envelope(4195618.41, 4196609.70, 7490341.49, 7490623.35));
    CHECK_ENVELOPES_EQUAL(
            adEnvelopes.added,
            Envelope(4195618.41, 4196609.70, 7490078.74, 7490623.35));

    CHECK_ENVELOPES_EQUAL(elementEnvelopes.before, elementEnvelopes.removed);
    CHECK_ENVELOPES_EQUAL(elementEnvelopes.after, elementEnvelopes.added);
    CHECK_ENVELOPES_EQUAL(
            adEnvelopes.before,
            Envelope(4195618.41, 4196609.70, 7490341.49, 7491158.42));
    CHECK_ENVELOPES_EQUAL(
            adEnvelopes.after,
            Envelope(4195618.41, 4196609.70, 7490078.74, 7491158.41));
}

Y_UNIT_TEST(add_ad_contour)
{
    auto adEnvelopes = objectDiffEnvelopes(121);
    auto faceEnvelopes = objectDiffEnvelopes(241);

    CHECK_ENVELOPES_EQUAL(adEnvelopes.removed, Envelope());
    CHECK_ENVELOPES_EQUAL(adEnvelopes.added, faceEnvelopes.after);
    CHECK_ENVELOPES_EQUAL(
            adEnvelopes.added,
            Envelope(4197305.39, 4197613.53, 7490403.19, 7490780.60));
    CHECK_ENVELOPES_EQUAL(
            adEnvelopes.after,
            Envelope(4196818.11, 4197613.53, 7490403.19, 7491227.28));

    auto expanded = adEnvelopes.before;
    expanded.expandToInclude(&faceEnvelopes.after);
    CHECK_ENVELOPES_EQUAL(expanded, adEnvelopes.after);
}

Y_UNIT_TEST(add_and_remove_ad_element)
{
    auto adEnvelopes = objectDiffEnvelopes(171);
    auto elemBeforeEnvelopes = objectDiffEnvelopes(151);
    auto elemAfterEnvelopes = objectDiffEnvelopes(161);

    CHECK_ENVELOPES_EQUAL(adEnvelopes.removed, elemBeforeEnvelopes.before);
    CHECK_ENVELOPES_EQUAL(
            adEnvelopes.removed,
            Envelope(4198220.25, 4198812.64, 7490895.25, 7491294.16));
    CHECK_ENVELOPES_EQUAL(adEnvelopes.added, elemAfterEnvelopes.after);
    CHECK_ENVELOPES_EQUAL(
            adEnvelopes.added,
            Envelope(4198220.25, 4198812.64, 7490895.25, 7491415.98));

    auto expanded = adEnvelopes.before;
    expanded.expandToInclude(&elemBeforeEnvelopes.before);
    expanded.expandToInclude(&elemAfterEnvelopes.after);
    CHECK_ENVELOPES_EQUAL(expanded, adEnvelopes.after);
}

Y_UNIT_TEST(create_ad)
{
    auto adEnvelopes = objectDiffEnvelopes(211);

    CHECK_ENVELOPES_EQUAL(adEnvelopes.before, Envelope());
    CHECK_ENVELOPES_EQUAL(adEnvelopes.added, adEnvelopes.after);
    CHECK_ENVELOPES_EQUAL(adEnvelopes.removed, Envelope());
    CHECK_ENVELOPES_EQUAL(
            adEnvelopes.after,
            Envelope(4199112.39, 4199991.41, 7490560.51, 7491382.21));
}

Y_UNIT_TEST(point_object_nonzero_area_envelopes)
{
    auto addrEnvelopes = objectDiffEnvelopes(1001);

    UNIT_ASSERT(addrEnvelopes.added.getArea() > 0.0);
    UNIT_ASSERT(addrEnvelopes.after.getArea() > 0.0);
}

} // Y_UNIT_TEST_SUITE_F

} // namespace tests
} // namespace diffalert
} // namespace wiki
} // namespace maps
