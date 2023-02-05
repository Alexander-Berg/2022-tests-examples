#include "test_util.h"

bool equalToScale(const Point2& lhs, const Point2& rhs)
{
    // precision decreased by 10e-1 to pass tests with road_graph
    static const double scale =
        std::pow(10., -od::DataTraits_CoordinatePrecision + 1);
    return std::fabs(lhs.x() - rhs.x()) < scale &&
        std::fabs(lhs.y() - rhs.y()) < scale;
}

void map(const od::FixedWidthVector* fbVector, FixedWidthVector& vector) {
    auto buffer = fbVector
        // ->flatbuffers::Vector->uint8_t*
        ->data()->data();
    succinct::mapper::map(
        vector.data, reinterpret_cast<const char*>(buffer));
    vector.width = fbVector->width();
}

void map(const od::GeometriesFringe* fbFringe, Fringe& fringe)
{
    map(fbFringe->rowOffsets(), fringe.rowOffsets);
    map(fbFringe->data(), fringe.data);
}

void map(const od::EdgesBegin* fbEdgesBegin, EdgesBegin& edgesBegin)
{
    auto edgesBeginBuf = fbEdgesBegin->edgesBegin()
        // ->flatbuffers::Vector->uint8_t*
        ->data()->data();
    succinct::mapper::map(
        edgesBegin.edgesBegin, reinterpret_cast<const char*>(edgesBeginBuf));
}

void map(
        const od::AdjacencyLists* fbAdjacencyLists,
        AdjacencyLists& adjacencyLists)
{
    map(fbAdjacencyLists->edgesBegin(),
        static_cast<EdgesBegin&>(adjacencyLists));
    map(fbAdjacencyLists->sourceTargetDifferenceZigzag(),
        adjacencyLists.sourceTargetDiff);
}

void map(const od::TurnsFringe* fbFringe, TurnsFringe& fringe) {
    map(fbFringe->rowOffsets(), fringe.rowOffsets);
    for (const auto& duration: *fbFringe->durations()) {
        fringe.durations.push_back(decodeWeight(duration));
    }
    map(fbFringe->requiresAccessPass(), fringe.accessPass);
}

void map(const od::TurnTables* fbTurnTables, TurnTables& turnTables)
{
    map(fbTurnTables->tables(), turnTables.tables);
    map(fbTurnTables->indices(), turnTables.indices);
}

bool weightsAreClose(int32_t w1, int32_t w2, int errorMultiplier)
{
    const int64_t diff = std::abs(w1 - w2);
    // Test relative precision.
    return diff * 2048 <= std::min(w1, w2) * errorMultiplier;
}
