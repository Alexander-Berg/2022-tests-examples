#include "helpers.h"
#include "../revision/db_access.h"
#include "../revision/geom_index.h"

#include <yandex/maps/wiki/common/string_utils.h>

#include <maps/libs/geolib/include/conversion.h>

#include <algorithm>
#include <iterator>
#include <sstream>
#include <vector>

namespace maps {
namespace wiki {
namespace diffalert {
namespace tests {

namespace {

struct GeomIndexPair {
    GeomIndexPtr oldIndex;
    GeomIndexPtr newIndex;
};

GeomIndexPair makeGeomIndexPair()
{
    const auto snapshotsPair = loadData(
        dataPath("tests_data/geom_index_tests.json"),
        dataPath("tests_data/geom_index_tests.json")
    );

    return GeomIndexPair {
        makeBldWithModel3dGeomIndex(
            RevSnapshotFactory{
                snapshotsPair.oldBranch, snapshotsPair.oldSnapshotId, RevisionDB::pool()
            }
        ),
        makeBldWithModel3dGeomIndex(
            RevSnapshotFactory{
                snapshotsPair.newBranch, snapshotsPair.newSnapshotId, RevisionDB::pool()
            }
        )
    };
}

// In: geo coordinates
// Out: mercator
Envelope constructEnvelope(geolib3::Point2 point1, geolib3::Point2 point2)
{
    point1 = geoPoint2Mercator(point1);
    point2 = geoPoint2Mercator(point2);
    return {point1.x(), point2.x(), point1.y(), point2.y()};
}

template<typename TContainer>
std::string idsToString(const TContainer& ids)
{
    return "[" + common::join(ids, ", ") + "]";
}

void check(const GeomIndex& index, const Envelope& query, const TIds& expectedIds) {
    const TIds receivedIds = index.objectIdsByEnvelope(query);

    std::vector<TId> receivedDiff;
    std::set_difference(
        receivedIds.begin(), receivedIds.end(),
        expectedIds.begin(), expectedIds.end(),
        std::back_inserter(receivedDiff)
    );

    std::vector<TId> expectedDiff;
    std::set_difference(
        expectedIds.begin(), expectedIds.end(),
        receivedIds.begin(), receivedIds.end(),
        std::back_inserter(expectedDiff)
    );

    UNIT_ASSERT_C(
        receivedDiff.empty() && expectedDiff.empty(),
        "Not expected "
            << idsToString(receivedDiff) << ", "
            << "not received " << idsToString(expectedDiff) << ", but "
            << "expected " << idsToString(expectedIds)
    );
}

} // namespace

Y_UNIT_TEST_SUITE_F(geom_index, SetLogLevelFixture) {

Y_UNIT_TEST(bld_with_3d_model_index_check)
{
    const auto pair = makeGeomIndexPair();
    const GeomIndex& index = *pair.newIndex;

    const Envelope overallEnvelope = constructEnvelope(
        {34.0, 46.0},
        {35.0, 48.0}
    );
    check(index, overallEnvelope, {2, 4});

    const Envelope bldOneWithoutModel3d = constructEnvelope(
        {34.576778898999997, 45.595004177},
        {34.576537885999997, 45.595077173}
    );
    check(index, bldOneWithoutModel3d, {});

    const Envelope bldTwoWithModel3d = constructEnvelope(
        {34.576778898999997, 46.595004177},
        {34.576537885999997, 46.595077173}
    );
    check(index, bldTwoWithModel3d, {2});
}

} // Y_UNIT_TEST_SUITE_F

} // namespace tests
} // namespace diffalert
} // namespace wiki
} // namespace maps
