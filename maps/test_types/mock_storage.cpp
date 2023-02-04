#include "mock_storage.h"

#include "../test_tools/geom_comparison.h"
#include "../test_tools/storage_diff_helpers.h"
#include "../geom_tools/geom_io.h"

#include <yandex/maps/wiki/topo/exception.h>

#include <maps/libs/geolib/include/intersection.h>
#include <maps/libs/geolib/include/contains.h>

#include <set>


namespace maps {
namespace wiki {
namespace topo {
namespace test {

void
MockStorage::checkNodeExists(NodeID id, IncidenceType incidenceType,
    OptionalEdgeID edgeId)
{
    REQUIRE(id, "Invalid node id");
    auto nodeIt = nodes_.find(id);
    REQUIRE(nodeIt != nodes_.end(),
        "Incorrect test data: " << incidenceType << " node not found, "
        "edge id " << (edgeId ? *edgeId : 0) <<
        ", " << incidenceType << " node id " << id);
}

void
MockStorage::checkEdgeExists(EdgeID id)
{
    REQUIRE(id, "Invalid edge id");
    auto edgeIt = edges_.find(id);
    REQUIRE(edgeIt != edges_.end(), "Edge " << id << " not found");
}

void
MockStorage::initNodes(const NodeDataVector& nodes)
{
    for (const auto& node : nodes) {
        nodes_.insert({node.id, node});
        newId_ = std::max(newId_, node.id + 1);
    }
}

void
MockStorage::initEdges(const EdgeDataVector& edges)
{
    for (const auto& edge : edges) {
        checkNodeExists(edge.start, IncidenceType::Start, edge.id);
        checkNodeExists(edge.end, IncidenceType::End, edge.id);
        edges_.insert({edge.id, edge});
        newId_ = std::max(newId_, edge.id + 1);
    }
}

void
MockStorage::initFaces(const FaceDataVector& faces)
{
    for (const auto& face : faces) {
        for (auto edgeId : face.edgeIds) {
            checkEdgeExists(edgeId);
        }
        faces_.insert({face.id, face});
        newId_ = std::max(newId_, face.id + 1);
    }
}

MockStorage::MockStorage()
    : newId_(1)
    , original_(nullptr)
    , faceRelationsAvailability_(defaultFaceRelationsAvailability())
{}

MockStorage::MockStorage(
        const NodeDataVector& nodes,
        const EdgeDataVector& edges)
    : newId_(1)
    , original_(nullptr)
    , faceRelationsAvailability_(defaultFaceRelationsAvailability())
{
    initNodes(nodes);
    initEdges(edges);
}

MockStorage::MockStorage(
        const NodeDataVector& nodes,
        const EdgeDataVector& edges,
        const FaceDataVector& faces)
    : newId_(1)
    , original_(nullptr)
    , faceRelationsAvailability_(defaultFaceRelationsAvailability())
{
    initNodes(nodes);
    initEdges(edges);
    initFaces(faces);
}

MockStorage::MockStorage(const MockStorage& other)
    : Storage()
    , nodes_(other.nodes_)
    , edges_(other.edges_)
    , faces_(other.faces_)
    , newId_(other.newId_)
    , original_(other.original_)
    , faceRelationsAvailability_(other.faceRelationsAvailability_)
{}

MockStorage& MockStorage::operator=(const MockStorage& other)
{
    nodes_ = other.nodes_;
    edges_ = other.edges_;
    faces_ = other.faces_;
    newId_ = other.newId_;
    original_ = other.original_;
    faceRelationsAvailability_ = other.faceRelationsAvailability_;
    return *this;
}

MockStorage::MockStorage(MockStorage&& other)
    : Storage()
    , nodes_(std::move(other.nodes_))
    , edges_(std::move(other.edges_))
    , faces_(std::move(other.faces_))
    , newId_(other.newId_)
    , original_(other.original_)
    , faceRelationsAvailability_(other.faceRelationsAvailability_)
{}

MockStorage& MockStorage::operator=(MockStorage&& other)
{
    nodes_ = std::move(other.nodes_);
    edges_ = std::move(other.edges_);
    faces_ = std::move(other.faces_);
    newId_ = other.newId_;
    original_ = other.original_;
    faceRelationsAvailability_ = other.faceRelationsAvailability_;
    return *this;
}


NodeDataMap::iterator MockStorage::addNode(const geolib3::Point2& pos)
{
    NodeID newId = newId_++;
    return nodes_.insert(
        std::make_pair(newId, test::Node(newId, pos))).first;
}

NodeDataMap::iterator MockStorage::addNode(NodeID id, const geolib3::Point2& pos)
{
    REQUIRE(nodes_.find(id) == nodes_.end(),
        "Node with id " << id << " already exists");
    newId_ = std::max(newId_, id + 1);
    while (nodes_.count(newId_)) {
        ++newId_;
    }
    return nodes_.insert({id, test::Node(id, pos)}).first;
}

EdgeDataMap::iterator
MockStorage::addEdge(const geolib3::Polyline2& geom, const IncidentNodes& nodes)
{
    checkNodeExists(nodes.start, IncidenceType::Start, boost::none);
    checkNodeExists(nodes.end, IncidenceType::End, boost::none);
    EdgeID newId = newId_++;
    return edges_.insert({newId, test::Edge(newId, nodes.start, nodes.end, geom)}).first;
}

EdgeDataMap::iterator
MockStorage::addEdge(
    EdgeID id, const geolib3::Polyline2& geom, const IncidentNodes& nodes)
{
    REQUIRE(edges_.find(id) == edges_.end(),
        "Edge with id " << id << " already exists");
    checkNodeExists(nodes.start, IncidenceType::Start, id);
    checkNodeExists(nodes.end, IncidenceType::End, id);
    newId_ = std::max(newId_, id + 1);
    while (edges_.count(newId_)) {
        ++newId_;
    }
    return edges_.insert({id, test::Edge(id, nodes.start, nodes.end, geom)}).first;
}

NodeIDSet MockStorage::allNodeIds() const
{
    NodeIDSet res;
    for (const auto& nodePair : nodes_) {
        res.insert(nodePair.first);
    }
    return res;
}

EdgeIDSet MockStorage::allEdgeIds() const
{
    EdgeIDSet res;
    for (const auto& edgePair : edges_) {
        res.insert(edgePair.first);
    }
    return res;
}

FaceIDSet MockStorage::allFaceIds() const
{
    FaceIDSet res;
    for (const auto& facePair : faces_) {
        res.insert(facePair.first);
    }
    return res;
}

test::Node& MockStorage::testNode(NodeID id)
{
    auto it = nodes_.find(id);
    REQUIRE(it != nodes_.end(), "No node with id " << id);
    return it->second;
}

const test::Node& MockStorage::testNode(NodeID id) const
{
    auto it = nodes_.find(id);
    REQUIRE(it != nodes_.end(), "No node with id " << id);
    return it->second;
}

bool MockStorage::nodeExists(NodeID id) const
{
    return nodes_.find(id) != nodes_.end();
}

test::Edge& MockStorage::testEdge(EdgeID id)
{
    auto it = edges_.find(id);
    REQUIRE(it != edges_.end(), "No edge with id " << id);
    return it->second;
}

const test::Edge& MockStorage::testEdge(EdgeID id) const
{
    auto it = edges_.find(id);
    REQUIRE(it != edges_.end(), "No edge with id " << id);
    return it->second;
}

bool MockStorage::edgeExists(EdgeID id) const
{
    return edges_.find(id) != edges_.end();
}


test::Face& MockStorage::testFace(FaceID id)
{
    auto it = faces_.find(id);
    REQUIRE(it != faces_.end(), "No face with id " << id);
    return it->second;
}

const test::Face& MockStorage::testFace(FaceID id) const
{
    auto it = faces_.find(id);
    REQUIRE(it != faces_.end(), "No face with id " << id);
    return it->second;
}

bool MockStorage::faceExists(FaceID id) const
{
    return faces_.find(id) != faces_.end();
}

/// Storage implementation

NodeIDSet MockStorage::nodeIds(const geolib3::BoundingBox& bbox)
{
    NodeIDSet res;
    for (const auto& nodePair : nodes_) {
        if (geolib3::contains(bbox, nodePair.second.pos)) {
            res.insert(nodePair.first);
        }
    }
    return res;
}

EdgeIDSet MockStorage::edgeIds(const geolib3::BoundingBox& bbox)
{
    EdgeIDSet res;
    for (const auto& edgePair : edges_) {
        if (geolib3::intersects(bbox, edgePair.second.geom)) {
            res.insert(edgePair.first);
        }
    }
    return res;
}

IncidencesByNodeMap MockStorage::incidencesByNodes(const NodeIDSet& nodeIds)
{
    IncidencesByNodeMap res;
    for (const auto& id : nodeIds) {
        if (nodes_.find(id) != nodes_.end()) {
            res.insert(std::make_pair(id, IncidentEdges()));
        }
    }
    for (const auto& edgePair : edges_) {
        const test::Edge& edge = edgePair.second;
        auto it = nodeIds.find(edge.start);
        if (it != nodeIds.end()) {
            res[edge.start].emplace_back(edge.id, IncidenceType::Start);
        }
        if ((it = nodeIds.find(edge.end)) != nodeIds.end()) {
            res[edge.end].emplace_back(edge.id, IncidenceType::End);
        }
    }
    typedef IncidentEdges::value_type IncEdgePair;
    for (auto& nodeInc : res) {
        std::sort(
            nodeInc.second.begin(), nodeInc.second.end(),
            [] (const IncEdgePair& lhs, const IncEdgePair& rhs) -> bool
            {
                return lhs.first < rhs.first;
            }
        );
    }
    return res;
}

IncidencesByEdgeMap MockStorage::incidencesByEdges(const EdgeIDSet& edgeIds)
{
    IncidencesByEdgeMap res;
    for (const auto& edgeId : edgeIds) {
        auto it = edges_.find(edgeId);
        if (it != edges_.end()) {
            res[edgeId] = IncidentNodes(it->second.start, it->second.end);
        }
    }
    return res;
}

IncidencesByEdgeMap MockStorage::originalIncidencesByEdges(const EdgeIDSet& edgeIds)
{
    REQUIRE(original_,
        "No original storage set for originalIncidencesByEdges() method");

    MockStorage copy = *original_;
    return copy.incidencesByEdges(edgeIds);
}

EdgeIDSet MockStorage::incidentEdges(NodeID nodeId)
{
    EdgeIDSet res;
    for (const auto& edgePair : edges_) {
        const test::Edge& edge = edgePair.second;
        if ((edge.start == nodeId || edge.end == nodeId)) {
            res.insert(edge.id);
        }
    }
    return res;
}

IncidentNodes MockStorage::incidentNodes(EdgeID edgeId)
{
    auto it = edges_.find(edgeId);
    REQUIRE(it != edges_.end(),
        "Edge " << edgeId << " was not loaded or invalid");
    return IncidentNodes(it->second.start, it->second.end);
}

topo::Node MockStorage::node(NodeID id)
{
    auto it = nodes_.find(id);
    REQUIRE(it != nodes_.end(), "Node " << id << " was not loaded");
    return topo::Node(it->first, it->second.pos);
}

topo::Edge MockStorage::edge(EdgeID id)
{
    auto it = edges_.find(id);
    REQUIRE(it != edges_.end(),
        "Edge " << id << " was not loaded or invalid");
    const auto& edge = it->second;
    return topo::Edge(it->first, edge.geom, edge.start, edge.end);
}

NodeVector MockStorage::nodes(const NodeIDSet& nodeIds)
{
    NodeVector res;
    for (const auto& nodeId : nodeIds) {
        auto it = nodes_.find(nodeId);
        REQUIRE(it != nodes_.end(), "Node " << nodeId << " was not loaded");
        res.emplace_back(it->first, it->second.pos);
    }
    return res;
}

EdgeVector MockStorage::edges(const EdgeIDSet& edgeIds)
{
    EdgeVector res;
    for (const auto& edgeId : edgeIds) {
        auto it = edges_.find(edgeId);
        REQUIRE(it != edges_.end(),
            "Edge " << edgeId << " was not loaded or not valid");
        const auto& edge = it->second;
        res.emplace_back(it->first, edge.geom, edge.start, edge.end);
    }
    return res;
}

NodeID MockStorage::newNodeId()
{
    return newId_++;
}

EdgeID MockStorage::newEdgeId()
{
    return newId_++;
}

topo::Node MockStorage::createNode(const geolib3::Point2& pos)
{
    auto predefIt = std::find_if(predefinedNodeIds_.begin(), predefinedNodeIds_.end(),
        [&pos] (const std::pair<NodeID, geolib3::Point2>& predefNode)
        {
            return test::compare(pos, predefNode.second, geolib3::EPS);
        });
    NodeDataMap::iterator it;
    if (predefIt != predefinedNodeIds_.end()) {
        it = addNode(predefIt->first, pos);
    } else {
        it = addNode(pos);
    }
    const test::Node& node = it->second;
    return topo::Node(node.id, node.pos);
}

void MockStorage::updateNodePos(NodeID id, const geolib3::Point2& pos)
{
    auto it = nodes_.find(id);
    REQUIRE(it != nodes_.end(), "Node " << id << " was not loaded");
    it->second.pos = pos;
}

void MockStorage::deleteNode(NodeID id)
{
    nodes_.erase(id);
}

topo::Edge
MockStorage::createEdge(const geolib3::Polyline2& geom,
    const IncidentNodes& nodes)
{
    auto predefIt = std::find_if(predefinedEdgeIds_.begin(), predefinedEdgeIds_.end(),
        [&geom] (const std::pair<EdgeID, geolib3::Polyline2>& predefEdge)
        {
            return test::compare(geom, predefEdge.second, geolib3::EPS);
        }
    );
    if (predefIt == predefinedEdgeIds_.end()) {
        predefIt = std::find_if(
            predefinedEdgeIds_.begin(), predefinedEdgeIds_.end(),
            [&geom] (const std::pair<EdgeID, geolib3::Polyline2>& predefEdge)
            {
                const auto result = geolib3::intersection(geom, predefEdge.second);

                if (result.size() != 1) {
                    return false;
                }

                return test::compare(result.front(), predefEdge.second, geolib3::EPS);
            }
        );
    }
    EdgeDataMap::iterator it;
    if (predefIt != predefinedEdgeIds_.end()) {
        it = addEdge(predefIt->first, geom, nodes);
    } else {
        it = addEdge(geom, nodes);
    }
    const test::Edge& edge = it->second;
    return topo::Edge(edge.id, edge.geom, nodes.start, nodes.end);
}

void MockStorage::updateEdgeGeom(EdgeID id,
    const geolib3::Polyline2& geom,
    const IncidentNodes& nodes)
{
    auto it = edges_.find(id);
    REQUIRE(it != edges_.end(), "Edge " << id << " was not loaded");
    auto& edge = it->second;
    edge.geom = geom;
    edge.start = nodes.start;
    edge.end = nodes.end;
}

void MockStorage::deleteEdge(EdgeID id)
{
    edges_.erase(id);
}

FacesByEdgeMap MockStorage::facesByEdges(const EdgeIDSet& edgeIds)
{
    FacesByEdgeMap result;
    for (const auto& facePair : faces_) {
        for (auto edgeId : facePair.second.edgeIds) {
            if (edgeIds.count(edgeId)) {
                result[edgeId].insert(facePair.first);
            }
        }
    }
    return result;
}

namespace {

EdgeIDSet
edgeIdsDiff(const EdgeIDSet& ids, const EdgeIDSet& otherIds)
{
    EdgeIDSet res;
    for (auto id : ids) {
        if (!otherIds.count(id)) {
            res.insert(id);
        }
    }
    return res;
}

EdgeIDSet
edgeIdsIntersection(const EdgeIDSet& ids, const EdgeIDSet& otherIds)
{
    EdgeIDSet res;
    for (auto id : ids) {
        if (otherIds.count(id)) {
            res.insert(id);
        }
    }
    return res;
}

} // namespace

FaceDiff MockStorage::faceDiff(FaceID faceId)
{
    REQUIRE(original_, "No original storage set for faceDiff() method");
    if (!original_->faceExists(faceId)) {
        const test::Face& face = testFace(faceId);
        return {{face.edgeIds}, {}, {}};
    }
    const test::Face& origFace = original_->testFace(faceId);
    const test::Face& face = testFace(faceId);

    auto movedEdgeIds = movedEdges(*original_, *this);

    return {
        edgeIdsDiff(face.edgeIds, origFace.edgeIds),
        edgeIdsIntersection(movedEdgeIds, face.edgeIds),
        edgeIdsDiff(origFace.edgeIds, face.edgeIds)
    };
}

boost::optional<EdgeIDSet> MockStorage::tryGetFaceEdges(FaceID faceId)
{
    if (faceRelationsAvailability_ == FaceRelationsAvailability::All) {
        return faces_.at(faceId).edgeIds;
    }
    return boost::none;
}

EdgeIDSet MockStorage::getFaceEdges(FaceID faceId)
{
    return faces_.at(faceId).edgeIds;
}

} // namespace test
} // namespace topo
} // namespace wiki
} // namespace maps
