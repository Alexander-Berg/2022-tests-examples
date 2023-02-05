#include "base_test.h"
#include "util.h"

#include <map>

using namespace maps::road_graph;

namespace {

std::string toString(EdgeType value) {
    switch (value) {
        case EdgeType::NamedRoad: return "NamedRoad";
        case EdgeType::Stairs: return "Stairs";
        case EdgeType::Crosswalk: return "Crosswalk";
        case EdgeType::Junction: return "Junction";
        case EdgeType::Uturn: return "Uturn";
        case EdgeType::FrontageRoad: return "FrontageRoad";
        case EdgeType::BoatFerry: return "BoatFerry";
        case EdgeType::Roundabout: return "Roundabout";
        case EdgeType::AccessToPoi: return "AccessToPoi";
        case EdgeType::FordCrossing: return "FordCrossing";
        case EdgeType::StairsDown: return "StairsDown";
        case EdgeType::StairsUp: return "StairsUp";
    }
    return "Unknown";
}

std::string toString(EdgeStructType value) {
    switch (value) {
        case EdgeStructType::Road: return "Road";
        case EdgeStructType::Bridge: return "Bridge";
        case EdgeStructType::Tunnel: return "Tunnel";
    }
    return "Unknown";
}

template <class T>
std::string toString(T x) {
    return std::to_string(x);
}

} // namespace

class StatisticsTest : public BaseTest {
public:
    void operator()() const override {
        INFO() << "Statistics for experimental data:";
        test(experimentRoadGraph());

        INFO() << "Statistics for etalon data:";
        test(etalonRoadGraph());
    }

private:
    static void test(const maps::road_graph::Graph& graph) {

        size_t baseEdgeCount = 0;
        size_t nonBaseEdgeCount = 0;
        size_t edgeWithReverseCount = 0;
        std::map<uint32_t, size_t> categoryToCount;
        std::map<EdgeType, size_t> typeToCount;
        std::map<EdgeStructType, size_t> structTypeToCount;
        std::map<bool, size_t> tollToCount;
        std::map<bool, size_t> pavedToCount;
        std::map<bool, size_t> poorConditionToCount;
        std::map<bool, size_t> residentialToCount;
        std::map<bool, size_t> restricedForTrucksToCount;
        std::map<bool, size_t> hasMasstransitLaneToCount;
        std::map<bool, size_t> endsWithTrafficLightToCount;

        const EdgeId edgesNumber = graph.edgesNumber();
        for (EdgeId edgeId{0}; edgeId < graph.edgesNumber(); ++edgeId) {
            const EdgeData& edgeData = graph.edgeData(edgeId);

            if (graph.isBase(edgeId)) {
                ++baseEdgeCount;
            } else {
                ++nonBaseEdgeCount;
            }

            if (graph.reverse(edgeId)) {
                ++edgeWithReverseCount;
            }

            categoryToCount[edgeData.category()] += 1;
            typeToCount[edgeData.type()] += 1;
            structTypeToCount[edgeData.structType()] += 1;
            tollToCount[edgeData.isToll()] += 1;
            pavedToCount[edgeData.isPaved()] += 1;
            poorConditionToCount[edgeData.isInPoorCondition()] += 1;
            residentialToCount[edgeData.isResidential()] += 1;
            restricedForTrucksToCount[edgeData.isRestrictedForTrucks()] += 1;
            hasMasstransitLaneToCount[edgeData.hasMasstransitLane()] += 1;
            endsWithTrafficLightToCount[edgeData.endsWithTrafficLight()] += 1;
        }

        INFO() << "\tBase edges: " <<
            outOfStr(baseEdgeCount, edgesNumber.value());

        INFO() << "\tNon-base edges: " <<
            outOfStr(nonBaseEdgeCount, edgesNumber.value());

        INFO() << "\tEdges with reverse edge: " <<
            outOfStr(edgeWithReverseCount, edgesNumber.value());

        printInfo("Category", categoryToCount, edgesNumber.value());
        printInfo("Type", typeToCount, edgesNumber.value());
        printInfo("StructType", structTypeToCount, edgesNumber.value());
        printInfo("Toll", tollToCount, edgesNumber.value());
        printInfo("Paved", pavedToCount, edgesNumber.value());
        printInfo("PoorCondition", poorConditionToCount, edgesNumber.value());
        printInfo("Residential", residentialToCount, edgesNumber.value());
        printInfo("RestrictedForTrucks", restricedForTrucksToCount, edgesNumber.value());
        printInfo("HasMasstransitLane", hasMasstransitLaneToCount, edgesNumber.value());
        printInfo("EndsWithTrafficLight", endsWithTrafficLightToCount, edgesNumber.value());
    }

    template <typename Map>
    static void printInfo(const std::string& what, const Map& m, size_t maxValue) {
        if (!m.empty()) {
            INFO() << "\t" << what << ":";
            for (const auto& [key, value]: m) {
                using namespace std;
                INFO() << "\t  " << toString(key) << ": " << outOfStr(value, maxValue);
            }
        }
    }
};

DECLARE_TEST(StatisticsTest)
