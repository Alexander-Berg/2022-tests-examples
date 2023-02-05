#include "base_test.h"
#include "util.h"

using namespace maps::road_graph;

class LanesTest : public BaseTest {
public:
    void operator()() const override {
        size_t checkedCount = 0;
        size_t unequalLanesCount = 0;

        for (EdgeId edgeId{0}; edgeId < experimentRoadGraph().edgesNumber(); ++edgeId) {
            if (!experimentRoadGraph().isBase(edgeId)) {
                continue;
            }

            LongEdgeId longEdgeId = experimentPersistentIndex().findLongId(edgeId).value();

            auto etlEdgeId = etalonPersistentIndex().findShortId(longEdgeId);
            if (!etlEdgeId) {
                continue;
            }

            ++checkedCount;

            const EdgeData& expEdgeData = experimentRoadGraph().edgeData(edgeId);
            const EdgeData& etlEdgeData = etalonRoadGraph().edgeData(*etlEdgeId);

            const auto& expLanes = expEdgeData.lanes();
            const auto& etlLanes = etlEdgeData.lanes();

            if (!equal(expLanes, etlLanes)) {
                ++unequalLanesCount;
            }
        }

        INFO() << "\tUnequal lanes: " <<
            outOfStr(unequalLanesCount, checkedCount);
    }

private:
    static bool equal(
            const maps::xrange_view::XRangeView<Lane>& a,
            const maps::xrange_view::XRangeView<Lane>& b) {
        return a.size() == b.size() &&
            std::equal(a.begin(), a.end(), b.begin(),
                [](const Lane& lhs, const Lane& rhs) {
                    return lhs.kind == rhs.kind && lhs.directionMask == rhs.directionMask;
                });
    }
};

DECLARE_TEST(LanesTest)
