#pragma once

#include <yandex/maps/wiki/topo/storage.h>
#include <yandex/maps/wiki/topo/node.h>
#include <yandex/maps/wiki/topo/edge.h>
#include <yandex/maps/wiki/topo/editor.h>

#include <boost/iterator/transform_iterator.hpp>

#include <unordered_map>
#include <vector>
#include <functional>

namespace maps {
namespace wiki {
namespace topo {
namespace test {

struct Node {
    Node(NodeID id, const geolib3::Point2& pos)
        : id(id)
        , pos(pos)
    {}

    NodeID id;
    geolib3::Point2 pos;
};

struct Edge {
    Edge(EdgeID id, NodeID start, NodeID end, const geolib3::Polyline2& geom)
        : id(id)
        , start(start)
        , end(end)
        , geom(geom)
    {
        REQUIRE(start && end, "Invalid node ids");
    }

    EdgeID id;
    NodeID start;
    NodeID end;
    geolib3::Polyline2 geom;
};

struct Face {
    Face(FaceID id, const EdgeIDSet& edgeIds)
        : id(id)
        , edgeIds(edgeIds)
    {}

    FaceID id;
    EdgeIDSet edgeIds;
};

enum class FaceRelationsAvailability { All, Diff };

typedef std::vector<test::Node> NodeDataVector;
typedef std::vector<test::Edge> EdgeDataVector;
typedef std::vector<test::Face> FaceDataVector;

typedef std::map<NodeID, test::Node> NodeDataMap;
typedef std::map<EdgeID, test::Edge> EdgeDataMap;
typedef std::map<FaceID, test::Face> FaceDataMap;

struct MockStorageDiff
{
    typedef std::map<NodeID, boost::optional<test::Node>> NodesDiffMap;
    typedef std::map<EdgeID, boost::optional<test::Edge>> EdgesDiffMap;
    typedef std::map<FaceID, boost::optional<test::Face>> FacesDiffMap;

    MockStorageDiff() {}

    MockStorageDiff(NodesDiffMap nodes, EdgesDiffMap edges)
        : nodes(std::move(nodes))
        , edges(std::move(edges))
    {}

    MockStorageDiff(NodesDiffMap nodes, EdgesDiffMap edges, FacesDiffMap faces)
        : nodes(std::move(nodes))
        , edges(std::move(edges))
        , faces(std::move(faces))
    {}

    NodesDiffMap nodes;
    EdgesDiffMap edges;
    FacesDiffMap faces;
};

class MockStorage : public topo::Storage
{
public:
    MockStorage();
    MockStorage(const NodeDataVector& nodes, const EdgeDataVector& edges);
    MockStorage(
        const NodeDataVector& nodes,
        const EdgeDataVector& edges,
        const FaceDataVector& faces);

    MockStorage(MockStorage other, const MockStorageDiff& diff)
    {
        nodes_ = std::move(other.nodes_);
        edges_ = std::move(other.edges_);
        faces_ = std::move(other.faces_);
        newId_ = other.newId_;
        applyDiff(diff);
        original_ = nullptr;
        faceRelationsAvailability_ = defaultFaceRelationsAvailability();
    }

    MockStorage(const MockStorage&);
    MockStorage& operator=(const MockStorage&);
    MockStorage(MockStorage&&);
    MockStorage& operator=(MockStorage&&);

    NodeDataMap::iterator addNode(const geolib3::Point2& pos);
    NodeDataMap::iterator addNode(NodeID id, const geolib3::Point2& pos);

    EdgeDataMap::iterator
    addEdge(const geolib3::Polyline2& geom, const IncidentNodes& nodes);
    EdgeDataMap::iterator
    addEdge(EdgeID id, const geolib3::Polyline2& geom, const IncidentNodes& nodes);

    NodeIDSet allNodeIds() const;
    EdgeIDSet allEdgeIds() const;
    FaceIDSet allFaceIds() const;

    test::Node& testNode(NodeID id);
    const test::Node& testNode(NodeID id) const;
    bool nodeExists(NodeID id) const;

    test::Edge& testEdge(EdgeID id);
    const test::Edge& testEdge(EdgeID id) const;
    bool edgeExists(EdgeID id) const;

    test::Face& testFace(FaceID id);
    const test::Face& testFace(FaceID id) const;
    bool faceExists(FaceID id) const;

    /// Diff

    void applyDiff(const MockStorageDiff& diff)
    {
        for (auto nodeIt = diff.nodes.cbegin(); nodeIt != diff.nodes.cend(); ++nodeIt) {
            nodes_.erase(nodeIt->first);
            if (nodeIt->second) {
                nodes_.insert({nodeIt->first, *nodeIt->second});
            }
        }
        for (auto edgeIt = diff.edges.cbegin(); edgeIt != diff.edges.cend(); ++edgeIt) {
            edges_.erase(edgeIt->first);
            if (edgeIt->second) {
                edges_.insert({edgeIt->first, *edgeIt->second});
            }
        }
        for (auto faceIt = diff.faces.cbegin(); faceIt != diff.faces.cend(); ++faceIt) {
            faces_.erase(faceIt->first);
            if (faceIt->second) {
                faces_.insert({faceIt->first, *faceIt->second});
            }
        }
    }

    void addPredefinedNodePos(NodeID id, const geolib3::Point2& pos)
    {
        REQUIRE(predefinedNodeIds_.insert({id, pos}).second,
            "Node " << id << " is already added to predefined node positions");
    }

    void addPredefinedEdgePos(EdgeID id, const geolib3::Polyline2& geom)
    {
        REQUIRE(predefinedEdgeIds_.insert({id, geom}).second,
            "Edge " << id << " is already added to predefined edge positions");
    }

    void setOriginal(const MockStorage* original) { original_ = original; }

    static FaceRelationsAvailability defaultFaceRelationsAvailability()
    { return FaceRelationsAvailability::Diff; }

    void setFaceRelationsAvailability(FaceRelationsAvailability availability)
    { faceRelationsAvailability_ = availability; }

    /// topo::Storage implementation

    virtual NodeIDSet nodeIds(const geolib3::BoundingBox& bbox);
    virtual EdgeIDSet edgeIds(const geolib3::BoundingBox& bbox);

    virtual IncidencesByNodeMap incidencesByNodes(const NodeIDSet& nodeIds);
    virtual IncidencesByEdgeMap incidencesByEdges(const EdgeIDSet& edgeIds);

    virtual IncidencesByEdgeMap originalIncidencesByEdges(const EdgeIDSet& edgeIds);

    virtual EdgeIDSet incidentEdges(NodeID endNode);
    virtual IncidentNodes incidentNodes(EdgeID edgeId);

    virtual topo::Node node(NodeID id);
    virtual topo::Edge edge(EdgeID id);

    virtual NodeVector nodes(const NodeIDSet& nodeIds);
    virtual EdgeVector edges(const EdgeIDSet& edgeIds);

    virtual NodeID newNodeId();
    virtual EdgeID newEdgeId();

    virtual topo::Node createNode(const geolib3::Point2& pos);
    virtual void updateNodePos(NodeID id, const geolib3::Point2& pos);
    virtual void deleteNode(NodeID id);

    virtual topo::Edge createEdge(const geolib3::Polyline2& geom,
        const IncidentNodes& nodes);
    virtual void updateEdgeGeom(EdgeID id,
        const geolib3::Polyline2& geom,
        const IncidentNodes& nodes);
    virtual void deleteEdge(EdgeID id);

    virtual bool nodeCanBeDeleted(NodeID /*nodeId*/) { return true; }
    virtual bool edgeCanBeDeleted(EdgeID /*edgeId*/) { return true; }

    virtual FacesByEdgeMap facesByEdges(const EdgeIDSet& edgeIds);

    virtual FaceDiff faceDiff(FaceID faceId);

    virtual boost::optional<EdgeIDSet> tryGetFaceEdges(FaceID faceId);
    virtual EdgeIDSet getFaceEdges(FaceID faceId);

private:
    friend void checkStorageContents(const MockStorage&, const MockStorage&);

    void checkNodeExists(
        NodeID id, IncidenceType incidenceType, OptionalEdgeID edgeId);
    void checkEdgeExists(EdgeID id);

    void initNodes(const NodeDataVector& nodes);
    void initEdges(const EdgeDataVector& edges);
    void initFaces(const FaceDataVector& faces);

    friend class MockStorageNodeRangeAdaptor;
    friend class MockStorageEdgeRangeAdaptor;

    NodeDataMap nodes_;
    EdgeDataMap edges_;
    FaceDataMap faces_;
    uint64_t newId_;

    std::map<NodeID, geolib3::Point2> predefinedNodeIds_;
    std::map<EdgeID, geolib3::Polyline2> predefinedEdgeIds_;

    const MockStorage* original_;
    FaceRelationsAvailability faceRelationsAvailability_;
};

class MockStorageNodeRangeAdaptor
{
public:
    typedef test::Node value_type;
    typedef test::NodeDataMap::const_iterator BaseIterator;
    typedef std::function<const test::Node& (const test::NodeDataMap::value_type&)> Transform;
    typedef boost::transform_iterator<Transform, BaseIterator, const test::Node&, test::Node> Iterator;

    explicit MockStorageNodeRangeAdaptor(const MockStorage& storage)
        : storage_(storage)
        , transform_([] (const test::NodeDataMap::value_type& value) -> const test::Node& { return value.second; })
    {}

    Iterator begin() const { return boost::make_transform_iterator(storage_.nodes_.begin(), transform_); }
    Iterator end() const { return boost::make_transform_iterator(storage_.nodes_.end(), transform_); }

private:
    const MockStorage& storage_;
    Transform transform_;
};

class MockStorageEdgeRangeAdaptor
{
public:
    typedef test::Edge value_type;
    typedef test::EdgeDataMap::const_iterator BaseIterator;
    typedef std::function<const test::Edge& (const test::EdgeDataMap::value_type&)> Transform;
    typedef boost::transform_iterator<Transform, BaseIterator, const test::Edge&, test::Edge> Iterator;

    explicit MockStorageEdgeRangeAdaptor(const MockStorage& storage)
        : storage_(storage)
        , transform_([] (const test::EdgeDataMap::value_type& value) -> const test::Edge&  { return value.second; })
    {}

    Iterator begin() const { return boost::make_transform_iterator(storage_.edges_.begin(), transform_); }
    Iterator end() const { return boost::make_transform_iterator(storage_.edges_.end(), transform_); }

private:
    const MockStorage& storage_;
    Transform transform_;
};

} // namespace test
} // namespace topo
} // namespace wiki
} // namespace maps
