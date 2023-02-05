#include "base_test.h"

#include "util.h"

using namespace maps;
using namespace maps::road_graph;

template <AccessId ACCESS_ID>
class AccessPasses {
public:
    static bool isManoeuvre(const Graph& graph, EdgeId from, EdgeId to) {
        return graph.turnData(from, to).isAccessPassFor(ACCESS_ID);
    }
    static std::vector<Manoeuvre> manoeuvres(const Graph& graph) {
        std::vector<Manoeuvre> accessPasses;
        accessPasses.reserve(graph.accessPasses().size());

        for (Manoeuvre manoeuvre : graph.accessPasses()) {
            if (graph.turnData(manoeuvre.at(0), manoeuvre.at(1)).
                    isAccessPassFor(ACCESS_ID)) {
                accessPasses.push_back(std::move(manoeuvre));
            }
        }

        return accessPasses;
    }
};

class StrictCountryBorders {
public:
    static bool isManoeuvre(const Graph& graph, EdgeId from, EdgeId to) {
        return graph.turnData(from, to).countryBorderType() ==
            CountryBorderType::Strict;
    }
    static xrange_view::XRangeView<Manoeuvre> manoeuvres(const Graph& graph) {
        return graph.strictCountryBorders();
    }
};

template <AccessId ACCESS_ID>
class ForbiddenTurns {
public:
    static bool isManoeuvre(const Graph& graph, EdgeId from, EdgeId to) {
        return graph.turnData(from, to).isForbiddenFor(ACCESS_ID);
    }
    static std::vector<Manoeuvre> manoeuvres(const Graph& graph) {
        std::vector<Manoeuvre> forbiddenTurns;
        forbiddenTurns.reserve(graph.forbiddenTurns().size());

        for (Manoeuvre manoeuvre : graph.forbiddenTurns()) {
            if (graph.turnData(manoeuvre.at(0), manoeuvre.at(1)).
                    isForbiddenFor(ACCESS_ID)) {
                forbiddenTurns.push_back(std::move(manoeuvre));
            }
        }

        return forbiddenTurns;
    }
};

template <typename TestEntity>
class ManoeuvreTest : public BaseTest {
public:
    void operator()() const override {
        INFO() << "Experimental manoeuvres: " <<
            TestEntity::manoeuvres(experimentRoadGraph()).size();
        INFO() << "Etalon manoeuvres: " <<
            TestEntity::manoeuvres(etalonRoadGraph()).size();

        INFO() << "Finding experimental data in etalon data:";
        test(
            experimentRoadGraph(),
            experimentPersistentIndex(),
            etalonRoadGraph(),
            etalonPersistentIndex());

        INFO() << "Finding etalon data in experimental data:";
        test(
            etalonRoadGraph(),
            etalonPersistentIndex(),
            experimentRoadGraph(),
            experimentPersistentIndex());
    }

private:
    static void test(
            const Graph& roadGraph1,
            const PersistentIndex& persistentIndex1,
            const Graph& roadGraph2,
            const PersistentIndex& persistentIndex2) {

        size_t missedPersistendIndex = 0;
        size_t missedObjects = 0;

        const auto manoeuvres1 = TestEntity::manoeuvres(roadGraph1);

        for (const std::vector<EdgeId>& manoeuvre1: manoeuvres1) {

            std::vector<LongEdgeId> persistentEdgeIds;
            persistentEdgeIds.reserve(manoeuvre1.size());
            for (EdgeId edgeId: manoeuvre1) {
                if (auto longEdgeId = persistentIndex1.findLongId(edgeId)) {
                    persistentEdgeIds.push_back(*longEdgeId);
                }
            }
            if (persistentEdgeIds.size() != manoeuvre1.size()) {
                continue;
            }

            std::vector<EdgeId> manoeuvre2;
            manoeuvre2.reserve(persistentEdgeIds.size());
            for (LongEdgeId longEdgeId: persistentEdgeIds) {
                if (auto edgeId = persistentIndex2.findShortId(longEdgeId)) {
                    manoeuvre2.push_back(*edgeId);
                }
            }

            if (manoeuvre1.size() != manoeuvre2.size()) {
                ++missedPersistendIndex;
                continue;
            }

            if (!TestEntity::isManoeuvre(roadGraph2,
                    manoeuvre2.front(), manoeuvre2.back())) {
                ++missedObjects;
                continue;
            }
        }

        INFO() << "\tNo in persistent index: " <<
            outOfStr(missedPersistendIndex, manoeuvres1.size());

        INFO() << "\tManoeuvre not found: " <<
            outOfStr(missedObjects, manoeuvres1.size());

        INFO() << "\tMissed manoeuvres: " <<
            outOfStr(missedPersistendIndex + missedObjects, manoeuvres1.size());
    }
};

using CountryBordersTest = ManoeuvreTest<StrictCountryBorders>;
using ForbiddenTurnsForAutomobileTest =
    ManoeuvreTest<ForbiddenTurns<AccessId::Automobile>>;
using ForbiddenTurnsForTaxiTest =
    ManoeuvreTest<ForbiddenTurns<AccessId::Taxi>>;
using AccessPassesForAutomobileTest =
    ManoeuvreTest<AccessPasses<AccessId::Automobile>>;
using AccessPassesForTaxiTest =
    ManoeuvreTest<AccessPasses<AccessId::Taxi>>;

DECLARE_TEST(CountryBordersTest)
DECLARE_TEST(ForbiddenTurnsForAutomobileTest)
DECLARE_TEST(ForbiddenTurnsForTaxiTest)
DECLARE_TEST(AccessPassesForAutomobileTest)
DECLARE_TEST(AccessPassesForTaxiTest)
