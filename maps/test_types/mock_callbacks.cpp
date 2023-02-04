#include "mock_callbacks.h"

#include "../test_tools/geom_comparison.h"

#include <boost/test/unit_test.hpp>
#include <boost/test/unit_test_log.hpp>
#include <boost/test/test_tools.hpp>
#include <boost/optional/optional_io.hpp>

#include <string>


namespace maps {
namespace wiki {
namespace topo {
namespace test {


namespace {

std::string printPoint(const geolib3::Polyline2& geom, size_t index)
{
    std::ostringstream os;
    if (index < geom.pointsNumber()) {
        const geolib3::Point2& point = geom.pointAt(index);
        os << "[" << point.x() << ", " << point.y() << "]";
    } else {
        os << "doesn't exist";
    }
    return os.str();
}

std::string printSourceId(const SourceEdgeID& id)
{
    return std::to_string(id.id()) + (id.exists() ? ":EXISTS" : ":NOT_EXISTS");
}

void checkPolyline(
    const std::string& context,
    const geolib3::Polyline2& recvPolyline,
    const geolib3::Polyline2& expPolyline)
{
    boost::optional<size_t> diffPoint = test::diffPoint(recvPolyline, expPolyline, geolib3::EPS);
    if (diffPoint) {
        BOOST_ERROR(
            context << " geometry mismatch in " << *diffPoint << " point: "
                << "expected: " << printPoint(expPolyline, *diffPoint)
                << ", received: " << printPoint(recvPolyline, *diffPoint)
        );
    }
}

void checkSplitPolylines(
    const std::string& context,
    const SplitPolylinePtrVector& recvSplitPolylines,
    const SplitPolylinePtrVector& expSplitPolylines)
{
    BOOST_CHECK_MESSAGE(expSplitPolylines.size() == recvSplitPolylines.size(),
        context << " split polylines counts differ"
            << ", expected " << expSplitPolylines.size()
            << ", received " << recvSplitPolylines.size()
    );
    for (size_t i = 0; i < recvSplitPolylines.size() && i < expSplitPolylines.size(); ++i) {
        checkPolyline(
            " " + std::to_string(i) + "-th split polyline",
            recvSplitPolylines[i]->geom, expSplitPolylines[i]->geom
        );
    }
}

void checkSplitPoints(
    const std::string& context,
    const std::vector<SplitPointPtr>& recvSplitPoints,
    const std::vector<SplitPointPtr>& expSplitPoints)
{
    BOOST_CHECK_MESSAGE(expSplitPoints.size() == recvSplitPoints.size(),
        context << " split points vectors sizes differ"
            << ", expected " << expSplitPoints.size()
            << ", received " << recvSplitPoints.size()
    );

    for (size_t i = 0; i < recvSplitPoints.size() && i < expSplitPoints.size(); ++i) {
        const SplitPoint& recvSplitPoint = *recvSplitPoints[i];
        const SplitPoint& expSplitPoint = *expSplitPoints[i];
        BOOST_CHECK_MESSAGE(
                !recvSplitPoint.nodeId || expSplitPoint.nodeId == recvSplitPoint.nodeId,
                context << " " << i << "-th split point node id mismatch"
                    << ", expected " << util::print(expSplitPoint.nodeId)
                    << ", received " << util::print(recvSplitPoint.nodeId)
        );
        BOOST_CHECK_MESSAGE(
            test::compare(recvSplitPoint.geom, expSplitPoint.geom, geolib3::EPS),
            context << " " << i << "-th split point points mismatch"
                << ", expected " << util::print(expSplitPoint.geom)
                << ", received " << util::print(recvSplitPoint.geom)
        );
    }
}

void checkNodeIds(
    const std::string& context,
    const std::vector<NodeID>& recvNodeIds,
    const std::vector<SplitPointPtr>& expSplitPoints)
{
    BOOST_CHECK_MESSAGE(
        expSplitPoints.size() == recvNodeIds.size(),
        context << " node vectors sizes differ"
            << ", expected " << expSplitPoints.size()
            << ", received " << recvNodeIds.size()
    );

    for (size_t i = 0; i < recvNodeIds.size() && i < expSplitPoints.size(); ++i) {
        BOOST_CHECK_MESSAGE(
            expSplitPoints[i]->nodeId && *expSplitPoints[i]->nodeId == recvNodeIds[i],
            context << " " << i << "-th node id mismatch"
                << ", expected " << util::print(expSplitPoints[i]->nodeId)
                << ", received " << recvNodeIds[i]
        );
    }
}

void checkEdgeIds(
    const std::string& context,
    const EdgeIDVector& recvEdgeIds,
    const SplitPolylinePtrVector& expSplitPolylines)
{
    BOOST_CHECK_MESSAGE(
        expSplitPolylines.size() == recvEdgeIds.size(),
        context << " edge id vectors sizes differ"
            << ", expected " << expSplitPolylines.size()
            << ", received " << recvEdgeIds.size()
    );

    for (size_t i = 0; i < expSplitPolylines.size() && i < expSplitPolylines.size(); ++i) {
        BOOST_CHECK_MESSAGE(
            expSplitPolylines[i] && recvEdgeIds[i] == *expSplitPolylines[i]->edgeId,
            context << " " << i << "-th edge id mismatch"
                << ", expected " << util::print(expSplitPolylines[i]->edgeId)
                << ", received " << recvEdgeIds[i]
        );
    }
}

void checkSourceIds(
    const std::string& context,
    const SourceEdgeID& receivedId,
    const SourceEdgeID& expectedId)
{
    BOOST_REQUIRE_MESSAGE(
        receivedId.exists() == expectedId.exists() && receivedId.id() && expectedId.id(),
        context << " sourceId mismatch"
            << ", expected " << printSourceId(expectedId)
            << ", received " << printSourceId(receivedId)
    );
}

template <class Id>
void checkIds(
    const std::string& context,
    const Id& recvId,
    const Id& expId)
{
    BOOST_CHECK_MESSAGE(
        recvId == expId,
        context << " id mismatch" << ", expected " << expId << ", received " << recvId);
}

template <class Id>
void checkOptIds(
    const std::string& context,
    const boost::optional<Id>& recvId,
    const boost::optional<Id>& expId)
{
    BOOST_CHECK_MESSAGE(
        (!recvId && !expId) || (recvId && expId && *recvId == *expId),
        context << " id mismatch" << ", expected " << expId << ", received " << recvId);
}

} // namespace


#define INSTANTIATE_EVENT_ID_BY_EDGE_ID(event_type) \
    template <> \
    EdgeID makeId<EdgeID, event_type>(const event_type& event) \
    { \
        return event.id; \
    }

INSTANTIATE_EVENT_ID_BY_EDGE_ID(topo::AddEdgeEventData)
INSTANTIATE_EVENT_ID_BY_EDGE_ID(topo::DeleteEdgeEventData)
INSTANTIATE_EVENT_ID_BY_EDGE_ID(topo::MoveEdgeEventData)

#undef INSTANTIATE_EVENT_ID_BY_EDGE_ID

#define INSTANTIATE_EVENT_ID_BY_EDGE_ID(event_type) \
    template <> \
    EdgeID makeId<EdgeID, event_type>(const event_type& event) \
    { \
        return event.id(); \
    }

INSTANTIATE_EVENT_ID_BY_EDGE_ID(topo::AddEdgeEvent)
INSTANTIATE_EVENT_ID_BY_EDGE_ID(topo::DeleteEdgeRequest)
INSTANTIATE_EVENT_ID_BY_EDGE_ID(topo::DeleteEdgeEvent)
INSTANTIATE_EVENT_ID_BY_EDGE_ID(topo::MoveEdgeEvent)

#undef INSTANTIATE_EVENT_ID_BY_EDGE_ID


template <>
EdgeID makeId<EdgeID, topo::SplitEdgeEventData>(const topo::SplitEdgeEventData& event)
{
    return event.sourceId.id();
}

template <>
EdgeID makeId<EdgeID, topo::SplitEdgeRequest>(const topo::SplitEdgeRequest& request)
{
    return request.sourceId();
}

template <>
EdgeID makeId<EdgeID, topo::SplitEdgeEvent>(const topo::SplitEdgeEvent& event)
{
    return event.sourceId();
}

template <>
NodeID makeId<NodeID, topo::MergeEdgesEventData>(const topo::MergeEdgesEventData& event)
{
    return event.mergeNodeId;
}

template <>
NodeID makeId<NodeID, topo::MergeEdgesRequest>(const topo::MergeEdgesRequest& request)
{
    return request.nodeId();
}

template <>
NodeID makeId<NodeID, topo::MergeEdgesEvent>(const topo::MergeEdgesEvent& event)
{
    return event.nodeId();
}

typedef std::pair<NodeID, NodeID> MergeNodesID;

MergeNodesID makeMergeNodesId(NodeID id1, NodeID id2)
{
    MergeNodesID result(id1, id2);
    if (result.first > result.second) {
        std::swap(result.first, result.second);
    }
    return result;
}

template <>
MergeNodesID makeId<MergeNodesID, topo::MergeNodesEventData>(const topo::MergeNodesEventData& event)
{
    return makeMergeNodesId(event.idFrom, event.idTo);
}

template <>
MergeNodesID makeId<MergeNodesID, topo::MergeNodesRequest>(const topo::MergeNodesRequest& request)
{
    return makeMergeNodesId(request.mergedId(), request.deletedId());
}

template <>
MergeNodesID makeId<MergeNodesID, topo::MergeNodesEvent>(const topo::MergeNodesEvent& event)
{
    return makeMergeNodesId(event.mergedId(), event.deletedId());
}

// topo callbacks

template <>
void MockTopoAddEdgeCallback::processImpl(const Event& event) const
{
    const geolib3::Polyline2& geom = event.geom();
    auto geomCheck = [&geom] (const EventData& eventData) -> bool
    {
        return test::compare(geom, eventData.geom, geolib3::EPS);
    };
    auto it = std::find_if(expectedEventsData_.begin(), expectedEventsData_.end(), geomCheck);

    BOOST_REQUIRE_MESSAGE(
        it != expectedEventsData_.end(),
        "Geometry not found for AddEdge topo event\n" <<
        "geom: " << util::print(geom) <<
        "\nexpected edge id: " << event.id()
    );

    checkSourceIds(
        "AddEdge event", {event.sourceId(), event.sourceExists()}, it->sourceId);

    checkIds<NodeID>("AddEdge event start node", event.startNode(), it->start);
    checkIds<NodeID>("AddEdge event end node", event.endNode(), it->end);
}

template <>
void MockTopoMoveEdgeCallback::processImpl(const Event& event) const
{
    std::string context = "MoveEdge event, id " + std::to_string(event.id());

    BOOST_CHECK_MESSAGE(
        event.newStartNode() || event.newEndNode(),
        context << ": neither new start nor new end node set");

    auto it = cfind(event);

    checkOptIds<NodeID>(context + " start node", event.newStartNode(), it->newStartNode);
    checkOptIds<NodeID>(context + " end node", event.newEndNode(), it->newEndNode);

    checkPolyline(context, event.newGeom(), it->newGeom);

    checkIds<NodeID>(
        context + " old start node", event.oldStartNode(), it->oldIncidentNodes.start);
    checkIds<NodeID>(
        context + " old end node", event.oldEndNode(), it->oldIncidentNodes.end);
}

template <>
void MockTopoDeleteEdgeCallback::processImpl(const Event& event) const
{
    auto it = cfind(event);

    std::string context = "DeleteEdge event, id " + std::to_string(event.id());

    checkIds<NodeID>(context + " start node", event.startNodeId(), it->startNodeId);
    checkIds<NodeID>(context + " end node", event.endNodeId(), it->endNodeId);
}


// split edge

template <>
void MockSplitEdgeCallback::processRequestImpl(Request& request) const
{
    auto it = find(request);

    checkSourceIds(
        "SplitEdge request",
        {request.sourceId(), request.sourceExists()},
        it->sourceId
    );

    std::string context = "SplitEdge request, source id " + printSourceId(it->sourceId);

    if (it->partToKeepIDIndex) {
        request.selectPartToKeepID(*it->partToKeepIDIndex);
    }

    checkSplitPolylines(context, request.splitPolylines(), it->splitPolylines);
    checkSplitPoints(context, request.splitPoints(), it->splitPoints);
}

template <>
void MockSplitEdgeCallback::processEventImpl(const Event& event) const
{
    auto it = cfind(event);

    checkSourceIds(
        "SplitEdge request", {event.sourceId(), event.sourceExists()}, it->sourceId);

    std::string context = "SplitEdge event, source id " + printSourceId(it->sourceId);

    BOOST_CHECK_MESSAGE(
        event.partToKeepID() == it->partToKeepIDIndex,
        context << " part to keep id index mismatch"
            << ", expected " << util::print(it->partToKeepIDIndex)
            << ", received " << util::print(event.partToKeepID())
    );

    checkNodeIds(context, event.nodeIds(), it->splitPoints);
    checkEdgeIds(context, event.edgeIds(), it->splitPolylines);
}


// merge edges

template <>
void MockMergeEdgesCallback::processRequestImpl(Request& request) const
{
    std::string context = "MergeEdges request"
        ", merged id " + std::to_string(request.mergedId()) +
        ", deleted id " + std::to_string(request.deletedId());

    auto it = find(request);

    if (request.mergedId() != it->mergedId) {
        request.swapMergedAndDeleted();
    }

    checkIds<EdgeID>(context, request.mergedId(), it->mergedId);
    checkIds<EdgeID>(context, request.deletedId(), it->deletedId);

    checkPolyline(context, request.newGeom(), geolib3::Polyline2(it->newGeomPoints));
}

template <>
void MockMergeEdgesCallback::processEventImpl(const Event& event) const
{
    std::string context = "MergeEdges event"
        ", merged id " + std::to_string(event.mergedId()) +
        ", deleted id " + std::to_string(event.deletedId());

    auto it = cfind(event);

    checkIds<EdgeID>(context + " merged edge", event.mergedId(), it->mergedId);
    checkIds<EdgeID>(context + " deleted edge", event.deletedId(), it->deletedId);
}


// delete edge

template <>
void MockDeleteEdgeCallback::processRequestImpl(Request& request) const
{
    auto it = find(request);

    std::string context = "DeleteEdge request, id " + std::to_string(request.id());

    checkIds<NodeID>(context + " start node", request.startNodeId(), it->startNodeId);
    checkIds<NodeID>(context + " end node", request.endNodeId(), it->endNodeId);
}

template <>
void MockDeleteEdgeCallback::processEventImpl(const Event& event) const
{
    auto it = cfind(event);

    std::string context = "DeleteEdge event, id " + std::to_string(event.id());

    checkIds<NodeID>(context + " start node", event.startNodeId(), it->startNodeId);
    checkIds<NodeID>(context + " end node", event.endNodeId(), it->endNodeId);
}


// merge nodes

template <>
void MockMergeNodesCallback::processRequestImpl(Request& request) const
{
    std::string context = "MergeNodes request"
        ", merged id " + std::to_string(request.mergedId()) +
        ", deleted id " + std::to_string(request.deletedId());

    auto it = find(request);

    if (request.mergedId() != it->idTo) {
        request.swapMergedAndDeleted();
    }

    checkIds<NodeID>(context + " merged node", request.mergedId(), it->idTo);
    checkIds<NodeID>(context + " deleted node", request.deletedId(), it->idFrom);

    BOOST_CHECK_MESSAGE(
        test::compare(request.pos(), it->pos, geolib3::EPS),
            context << " node position mismatch"
                << ", expected " << util::print(it->pos)
                << ", received " << util::print(request.pos())
    );
}

template <>
void MockMergeNodesCallback::processEventImpl(const Event& event) const
{
    std::string context = "MergeNodes event"
        ", merged id " + std::to_string(event.mergedId()) +
        ", deleted id " + std::to_string(event.deletedId());

    auto it = cfind(event);

    checkIds<NodeID>(context + " merged node", event.mergedId(), it->idTo);
    checkIds<NodeID>(context + " deleted node", event.deletedId(), it->idFrom);
}

} // namespace test
} // namespace topo
} // namespace wiki
} // namespace maps
