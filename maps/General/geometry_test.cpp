#include "base_test.h"

#include <maps/libs/geolib/include/polyline.h>
#include <maps/libs/geolib/include/distance.h>

using namespace maps::road_graph;

class GeometryTest : public BaseTest {
public:
    void operator()() const override {

        size_t checkedCount = 0;
        size_t unequalEdgePolylinesCount = 0;

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

            if (!polylinesEqual(
                    experimentRoadGraph().edgeData(edgeId).geometry(),
                    etalonRoadGraph().edgeData(*etlEdgeId).geometry())) {
                ++unequalEdgePolylinesCount;
            }
        }

        INFO() << "\tChecked edges: " <<
            checkedCount << " out of " << experimentRoadGraph().edgesNumber();

        INFO() << "\tUnequal edge polylines: " <<
            unequalEdgePolylinesCount << " out of " << checkedCount <<
            ", or " << (unequalEdgePolylinesCount * 100.0 / checkedCount) << "%";
    }

private:
    static bool polylinesEqual(
            const maps::geolib3::Polyline2& a,
            const maps::geolib3::Polyline2& b) {
        if (a.pointsNumber() != b.pointsNumber()) {
            return false;
        }
        for (size_t i = 0; i < a.pointsNumber(); ++i) {
            if (!pointsEqual(a.pointAt(i), b.pointAt(i))) {
                return false;
            }
        }
        return true;
    }

    static bool pointsEqual(
            const maps::geolib3::Point2& a,
            const maps::geolib3::Point2& b) {
        const double EPS = 0.5; // 50cm
        return maps::geolib3::geoDistance(a, b) < EPS;
    }
};

DECLARE_TEST(GeometryTest)
