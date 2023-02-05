#pragma once

#include <maps/routing/offline-data-packer/cost_calculator.h>
#include <maps/routing/offline-data-packer/test_graph.h>
#include <maps/routing/turn_penalties/include/turn_penalties_builder.h>

namespace mrg = maps::road_graph;

namespace {

int32_t asInt(double value) {
    ASSERT(value >= 0);
    // Throws in case of integer overflow.
    return boost::numeric_cast<int32_t>(value * 10);
}

} // namespace

class TestCostCalculatorImpl : public CostCalculator {
public:
    TestCostCalculatorImpl(
            const maps::road_graph::Graph* roadGraph,
            double distanceTimeTradeoff) :
        roadGraph_(roadGraph),
        distanceTimeTradeoff_(distanceTimeTradeoff)
    {
        std::vector<VertexEdges> vertexEdges(TEST_VERTEX_DATA.size());
        for (const auto& e: TEST_EDGES) {
            vertexEdges[e.source.value()].outEdges.push_back(e.id);
            vertexEdges[e.target.value()].inEdges.push_back(e.id);
        }

        for (size_t i = 0; i < TEST_VERTEX_DATA.size(); ++i) {
            std::vector<maps::turn_penalties2::PenaltyAtShortTurn> penalties;
            for (size_t from = 0; from < vertexEdges[i].inEdges.size(); ++from) {
                for (size_t to = 0; to < vertexEdges[i].outEdges.size(); ++to) {
                    const auto fromEdgeId = vertexEdges[i].inEdges[from];
                    const auto toEdgeId = vertexEdges[i].outEdges[to];
                    const auto penalty = turnPenaltyOracle()(fromEdgeId, toEdgeId);
                    const auto accessPass = turnPenaltyOracle().requiresAccessPass(
                        fromEdgeId, toEdgeId);
                    penalties.push_back({
                        static_cast<maps::turn_penalties2::ShortId>(from),
                        static_cast<maps::turn_penalties2::ShortId>(to),
                        {penalty, penalty, accessPass, false}});
                }
            }
            turnPenaltiesData_.setVertexPenalties(i, penalties);
        }
    }

    Cost operator()(mrg::EdgeId edgeId) const override {
        const double distance = roadGraph_->edgeData(mrg::EdgeId{edgeId}).length();
        const double speed = roadGraph_->edgeData(mrg::EdgeId{edgeId}).speed();
        const double duration = distance / std::max(speed, 1.0);
        return {
            std::max(1, asInt(duration + distance * distanceTimeTradeoff_)),
            std::max(1, asInt(duration))
        };
    }

    boost::optional<Cost> turnCost(
            mrg::EdgeId sourceEdge,
            mrg::EdgeId targetEdge) const override {
        const auto vertexId = roadGraph_->edge(sourceEdge).target;
        ASSERT(vertexId == roadGraph_->edge(targetEdge).source);
        auto findEdgeIndex = [this](mrg::EdgeId edge, mrg::VertexId vertexId, bool inEdges) {
            const auto& edgesRange = inEdges ?
                roadGraph_->inEdgeIds(vertexId) : roadGraph_->outEdgeIds(vertexId);
            size_t edgeIndex = 0;
            for (const auto edgeId: edgesRange) {
                if (edge == edgeId) {
                    break;
                }
                ++edgeIndex;
            }
            return edgeIndex;
        };
        const auto sourceEdgeIndex = findEdgeIndex(sourceEdge, vertexId, true /* inEdges */);
        const auto targetEdgeIndex = findEdgeIndex(targetEdge, vertexId, false /* inEdges */);
        const auto penalty = turnPenaltiesData_.getPenalty(
            vertexId.value(),
            sourceEdgeIndex,
            targetEdgeIndex,
            1.0,
            1.0);
        if (!std::isfinite(penalty.weight)) {
            return {};
        }
        return Cost {
            asInt(penalty.weight),
            asInt(penalty.time)};
    }

    bool requiresAccessPass(
            mrg::EdgeId sourceEdge,
            mrg::EdgeId targetEdge) const override {
        return roadGraph_->turnData(sourceEdge, targetEdge).
            isAccessPassFor(mrg::AccessId::Automobile);
    }

private:
    const maps::road_graph::Graph* roadGraph_;
    maps::turn_penalties2::TurnPenaltiesData turnPenaltiesData_;
    double distanceTimeTradeoff_;
};
