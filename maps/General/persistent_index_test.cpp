#include "base_test.h"

#include <util/generic/xrange.h>

#include "util.h"

#include <map>
#include <unordered_map>

using namespace maps::road_graph;

class PersistentIndexTest : public BaseTest {
public:
    void operator()() const override {
        INFO() << "Experiment edges: " << experimentRoadGraph().edgesNumber();
        INFO() << "Ethalon edges: " << etalonRoadGraph().edgesNumber();

        INFO() << "Finding experimental data in etalon data:";
        test(
            experimentRoadGraph(),
            experimentPersistentIndex(),
            etalonPersistentIndex());

        INFO() << "Finding etalon data in experimental data:";
        test(
            etalonRoadGraph(),
            etalonPersistentIndex(),
            experimentPersistentIndex());
    }

private:
    static void test(
            const Graph& roadGraph1,
            const PersistentIndex& persistentIndex1,
            const PersistentIndex& persistentIndex2) {

        std::unordered_map<CountryIsocode, size_t> countryToCount;

        size_t missedCount = 0;

        for (EdgeId edgeId: xrange(roadGraph1.edgesNumber())) {
            if (!roadGraph1.isBase(edgeId)) {
                continue;
            }

            const auto longEdgeId = persistentIndex1.findLongId(edgeId).value();

            if (!persistentIndex2.findShortId(longEdgeId)) {
                ++missedCount;

                countryToCount[roadGraph1.edgeData(edgeId).isoCode()] += 1;
            }
        }

        std::multimap<size_t, CountryIsocode, std::greater<size_t>> countToCountry;
        for (const auto& [country, count]: countryToCount) {
            countToCountry.emplace(count, country);
        }

        INFO() << "\tMissed edges: " <<
            outOfStr(missedCount, roadGraph1.edgesNumber().value());
        for (const auto& [count, country]: countToCountry) {
            INFO() << "\t\t" << country << ": " << outOfStr(count, missedCount);
        }
    }
};

DECLARE_TEST(PersistentIndexTest)
