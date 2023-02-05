#include "base_test.h"

#include <util/generic/xrange.h>

#include "util.h"

using namespace maps::road_graph;

class ToponymsTest : public BaseTest {
public:
    void operator()() const override {

        size_t countCheckedEdges = 0;
        size_t missedCount = 0;
        size_t unequalDefaultTranslationsCount = 0;
        size_t lessTranslationsCount = 0;
        size_t moreTranslationsCount = 0;
        size_t unequalTranslationsCount = 0;

        for (EdgeId expEdgeId: xrange(experimentRoadGraph().edgesNumber())) {
            if (!experimentRoadGraph().isBase(expEdgeId)) {
                continue;
            }

            const auto longEdgeId = experimentPersistentIndex().findLongId(expEdgeId).value();
            const auto etlEdgeId = etalonPersistentIndex().findShortId(longEdgeId);
            if (!etlEdgeId) {
                continue;
            }

            ++countCheckedEdges;

            const auto expEdgeData = experimentRoadGraph().edgeData(expEdgeId);
            const auto etlEdgeData = etalonRoadGraph().edgeData(*etlEdgeId);

            if (!expEdgeData.toponym() && etlEdgeData.toponym()) {
                ++missedCount;
                continue;
            }

            if (!expEdgeData.toponym() || !etlEdgeData.toponym()) {
                continue;
            }

            if (!equal(
                    expEdgeData.toponym()->defaultTranslation(),
                    etlEdgeData.toponym()->defaultTranslation())) {
                ++unequalDefaultTranslationsCount;
            }

            const auto expTranslationsRange = expEdgeData.toponym()->translations();
            const auto etlTranslationsRange = etlEdgeData.toponym()->translations();

            if (expTranslationsRange.size() < etlTranslationsRange.size()) {
                ++lessTranslationsCount;

            } else if (expTranslationsRange.size() > etlTranslationsRange.size()) {
                ++moreTranslationsCount;

            } else {
                if (!std::equal(
                        expTranslationsRange.begin(),
                        expTranslationsRange.end(),
                        etlTranslationsRange.begin(),
                        equal)) {
                    ++unequalTranslationsCount;
                }
            }
        }

        INFO() << "\tChecked edges: " <<
            outOfStr(countCheckedEdges, experimentRoadGraph().edgesNumber().value());

        INFO() << "\tMissed toponyms: " <<
            outOfStr(missedCount, countCheckedEdges);

        INFO() << "\tUnequal default translations: " <<
            outOfStr(unequalDefaultTranslationsCount, countCheckedEdges);

        INFO() << "\tLess number translations: " <<
            outOfStr(lessTranslationsCount, countCheckedEdges);

        INFO() << "\tMore number translations: " <<
            outOfStr(moreTranslationsCount, countCheckedEdges);

        INFO() << "\tUnequal translations: " <<
            outOfStr(unequalTranslationsCount, countCheckedEdges);
    }

private:
    static bool equal(
            const ToponymTranslation& a,
            const ToponymTranslation& b) {
        return a.text == b.text && a.locale == b.locale;
    }
};

DECLARE_TEST(ToponymsTest)
