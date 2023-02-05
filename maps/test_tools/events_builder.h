#pragma once

#include "../events_data.h"
#include "../test_types/common.h"
#include "../test_types/mock_storage.h"

#include <yandex/maps/wiki/topo/events.h>

#include <boost/optional.hpp>

#include <list>

namespace maps {
namespace wiki {
namespace topo {
namespace test {

std::list<topo::SplitEdgeEventData>
makeSplitEventData(
    const SourceEdgeID& sourceEdgeId,
    const SplitEdges& ids,
    const geolib3::Polyline2& alignedPolyline,
    const MockStorage& originalStorage,
    const MockStorage& resultStorage);

std::list<topo::AddEdgeEventData>
makeAddEventData(
    const SourceEdgeID& sourceId,
    const test::SplitEdges& splitIds,
    const test::MockStorage& originalStorage,
    const test::MockStorage& resultStorage);

std::list<topo::MoveEdgeEventData>
makeMoveEventData(
    const test::MockStorage& originalStorage,
    const test::MockStorage& resultStorage);

boost::optional<topo::MergeNodesEventData>
makeMergeNodesEventData(
    NodeID nodeId,
    OptionalNodeID mergedNodeId,
    const test::MockStorage& resultStorage);

topo::MergeEdgesEventData
makeMergeEdgesEventData(
    NodeID commonNodeId,
    const test::MockStorage& originalStorage,
    const test::MockStorage& resultStorage);

std::list<topo::DeleteEdgeEventData>
makeDeleteEventData(
    const test::MockStorage& originalStorage,
    const test::MockStorage& resultStorage);

} // namespace test
} // namespace topo
} // namespace wiki
} // namespace maps
