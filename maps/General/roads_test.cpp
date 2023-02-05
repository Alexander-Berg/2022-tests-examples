#include "base_test.h"
#include "util.h"

using namespace maps::road_graph;

class RoadsTest : public BaseTest {
public:
    void operator()() const override {
        size_t checkedCount = 0;
        size_t unequalRoadsCount = 0;
        size_t moreRoadsCount = 0;
        size_t lessRoadsCount = 0;

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

            const auto& expRoads = experimentRoadGraph().edgeData(edgeId).roads();
            const auto& etlRoads = etalonRoadGraph().edgeData(*etlEdgeId).roads();

            if (expRoads.size() > etlRoads.size()) {
                ++moreRoadsCount;
            }
            if (expRoads.size() < etlRoads.size()) {
                ++lessRoadsCount;
            }
            if (!equal(expRoads, etlRoads)) {
                ++unequalRoadsCount;
            }
        }

        INFO() << "\tUnequal roads: " <<
            outOfStr(unequalRoadsCount, checkedCount);
        INFO() << "\tMore roads: " <<
            outOfStr(moreRoadsCount, checkedCount);
        INFO() << "\tLess roads: " <<
            outOfStr(lessRoadsCount, checkedCount);
    }

private:
    static bool equal(
            const maps::xrange_view::XRangeView<Road>& a,
            const maps::xrange_view::XRangeView<Road>& b) {
        return a.size() == b.size() &&
            std::equal(a.begin(), a.end(), b.begin(),
                [](const Road& lhs, const Road& rhs) {
                    return equal(lhs, rhs);
                });
    }

    static bool equal(
            const Road& a,
            const Road& b) {
        const auto& aToponymTranlationsRange = a.toponym.translations();
        const auto& bToponymTranlationsRange = b.toponym.translations();

        std::vector<ToponymTranslation> aToponymTranlations(
            aToponymTranlationsRange.begin(), aToponymTranlationsRange.end());
        std::vector<ToponymTranslation> bToponymTranlations(
            bToponymTranlationsRange.begin(), bToponymTranlationsRange.end());

        const auto comp = [](const auto& lhs, const auto& rhs) {
            return lhs.locale < rhs.locale;
        };
        std::sort(aToponymTranlations.begin(), aToponymTranlations.end(), comp);
        std::sort(bToponymTranlations.begin(), bToponymTranlations.end(), comp);

        return aToponymTranlations.size() == bToponymTranlations.size() &&
            std::equal(aToponymTranlations.begin(), aToponymTranlations.end(),
                bToponymTranlations.begin(),
                [](const auto& f, const auto& s) {
                    return equal(f, s);
                });
    }

    static bool equal(
            const ToponymTranslation& a,
            const ToponymTranslation& b) {
        return a.text == b.text && a.locale == b.locale;
    }
};

DECLARE_TEST(RoadsTest)
