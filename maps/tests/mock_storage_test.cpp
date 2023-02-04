#include "../test_types/mock_storage.h"
#include "../test_tools/test_cmp.h"
#include "../test_tools/geom_comparison.h"

#include <yandex/maps/wiki/topo/storage.h>
#include <maps/libs/geolib/include/bounding_box.h>

#include <boost/optional.hpp>
#include <boost/test/unit_test.hpp>

#include <initializer_list>

using namespace maps::wiki::topo;
using namespace maps::geolib3;

namespace gt = maps::wiki::topo::test;

static const IncidenceType START = IncidenceType::Start;
static const IncidenceType END = IncidenceType::End;

struct IDsByBBoxTest {
    BoundingBox bbox;
    EdgeIDSet edgeIds;
    NodeIDSet nodeIds;
};

bool isValid(const IncidentNodes& nodes)
{
    return nodes.start && nodes.end;
}

template <class ObjectsVector, class ObjectIDSet, class IncidencesMap>
void checkIncidences(
    Storage& storage,
    IncidencesMap (Storage::*incidencesBy)(const ObjectIDSet&),
    const ObjectsVector& objects,
    const IncidencesMap& allIncidences)
{
    for (size_t i = 0; i < std::pow(2, objects.size()); ++i) {
        ObjectIDSet objectIds;
        IncidencesMap incidences;
        for (size_t j = 0; j < objects.size(); ++j) {
            if (i & (1ul << j)) {
                objectIds.insert(objects[j].id);
                incidences[objects[j].id] =
                    allIncidences.find(objects[j].id)->second;
            }
        }
        BOOST_CHECK((storage.*incidencesBy)(objectIds) == incidences);
    }
}

IncidencesByEdgeMap
nodesIncToEdgesInc(const IncidencesByNodeMap& nodesIncidences)
{
    IncidencesByEdgeMap edgesIncidences;
    for (const auto& incidencePair : nodesIncidences) {
        const IncidentEdges& incidentEdges = incidencePair.second;
        for (const auto& incidentEdge : incidentEdges) {
            (incidentEdge.second == START
                ? edgesIncidences[incidentEdge.first].start
                : edgesIncidences[incidentEdge.first].end
            ) = incidencePair.first;
        }
    }
    return edgesIncidences;
}

IncidencesByNodeMap
edgesIncToNodesInc(const IncidencesByEdgeMap& edgesIncidences)
{
    IncidencesByNodeMap nodesIncidences;
    for (const auto& edgeIncidencePair : edgesIncidences) {
        if (isValid(edgeIncidencePair.second)) {
            EdgeID edgeId = edgeIncidencePair.first;
            const IncidentNodes& nodes = edgeIncidencePair.second;
            nodesIncidences[nodes.start].emplace_back(edgeId, START);
            nodesIncidences[nodes.end].emplace_back(edgeId, END);
        }
    }
    return nodesIncidences;
}

test::EdgeDataVector
edgesFromEdgesIncidencesAndNodes(
    const IncidencesByEdgeMap& edgesIncidences,
    const test::NodeDataVector& nodes)
{
    test::EdgeDataVector edges;
    for (const auto& incidencePair : edgesIncidences) {
        if (isValid(incidencePair.second)) {
            NodeID startId = incidencePair.second.start;
            NodeID endId = incidencePair.second.end;
            const test::Node& start = *std::find_if(
                nodes.begin(), nodes.end(),
                [startId] (const test::Node& node) -> bool
                {
                    return node.id == startId;
                }
            );
            const test::Node& end = *std::find_if(
                nodes.begin(), nodes.end(),
                [endId] (const test::Node& node) -> bool
                {
                    return node.id == endId;
                }
            );
            edges.emplace_back(
                incidencePair.first,
                startId,
                endId,
                Polyline2 { PointsVector { start.pos, end.pos } }
            );
        }
    }
    return edges;
}

void checkNode(const test::Node& testNode, const Node& node,
    Storage& /*storage*/)
{
    BOOST_CHECK_EQUAL(node.id(), testNode.id);
    BOOST_CHECK(node.pos() == testNode.pos);
}

void checkGeom(const Polyline2& testGeom, const Polyline2& geom)
{
    BOOST_REQUIRE_EQUAL(geom.pointsNumber(), testGeom.pointsNumber());
    for (size_t i = 0; i < geom.pointsNumber(); ++i) {
        BOOST_CHECK(geom.pointAt(i) == testGeom.pointAt(i));
    }
}

void checkEdge(const test::Edge& testEdge, const Edge& edge,
    Storage& storage)
{
    BOOST_CHECK_EQUAL(edge.id(), testEdge.id);

    const Node& start = storage.node(edge.startNode());
    const Node& end = storage.node(edge.endNode());
    BOOST_CHECK_EQUAL(start.id(), testEdge.start);
    BOOST_CHECK_EQUAL(end.id(), testEdge.end);
    const Polyline2& geom = edge.geom();
    BOOST_CHECK(geom.pointAt(0) == start.pos());
    BOOST_CHECK(geom.pointAt(geom.pointsNumber() - 1) == end.pos());

    checkGeom(testEdge.geom, geom);
}

template <class Type, class TestType, class IDSet>
void checkObjects(
    const std::vector<TestType>& objects,
    Storage& storage,
    std::vector<Type> (Storage::*getObjects)(const IDSet&),
    void (*checkObject)(const TestType&, const Type&, Storage&))
{
    for (size_t i = 0; i < std::pow(2, objects.size()); ++i) {
        IDSet ids;
        std::vector<TestType> testObjects;
        for (size_t j = 0; j < objects.size(); ++j) {
            if (i & (1ul << j)) {
                ids.insert(objects[j].id);
                testObjects.push_back(objects[j]);
            }
        }
        std::vector<Type> res = (storage.*getObjects)(ids);
        std::sort(
            res.begin(), res.end(),
            [] (const Type& l, const Type& r) -> bool
                { return l.id() < r.id(); }
        );
        std::sort(
            testObjects.begin(), testObjects.end(),
            [] (const TestType& l, const TestType& r) -> bool
                { return l.id < r.id; }
        );
        BOOST_REQUIRE_EQUAL(res.size(), testObjects.size());
        auto ti = testObjects.cbegin();
        auto ri = res.cbegin();
        for (; ri != res.cend(); ++ti, ++ri) {
            checkObject(*ti, *ri, storage);
        }
    }
}

template <class Vector, class AssociativeCont>
void checkEqualIds(const Vector& recvIds, const AssociativeCont& expIds)
{
    BOOST_CHECK_EQUAL(recvIds.size(), expIds.size());
    for (const auto& recvId : recvIds) {
        BOOST_CHECK(expIds.find(recvId) != expIds.end());
    }
}

BOOST_AUTO_TEST_SUITE(storage_helper_tests)

BOOST_AUTO_TEST_CASE(test_construction_and_access)
{
    test::NodeDataVector nodes {
        gt::Node { 1, {1, 1} },
        gt::Node { 2, {3, 2} },
        gt::Node { 3, {5, 2} },
        gt::Node { 4, {6, 3} }
    };
    test::EdgeDataVector edges {
        gt::Edge { 1, 1, 2, Polyline2 { {{1, 1}, {1, 3}, {2, 3}, {3, 2}} } },
        gt::Edge { 2, 2, 3, Polyline2 { PointsVector {{3, 2}, {5, 2}} } },
        gt::Edge { 3, 3, 4, Polyline2 { {{5, 2}, {4, 4}, {6, 5}, {6, 3}} } },
        gt::Edge { 4, 3, 4, Polyline2 { PointsVector {{5, 2}, {6, 3}} } },
        gt::Edge { 5, 1, 1, Polyline2 { PointsVector {{1, 1}, {0, 2}, {0, 0}, {1, 1}} } }
    };

    test::MockStorage storage { nodes, edges };

    for (const auto& testNode : nodes ) {
        boost::optional<Node> node;
        BOOST_CHECK_NO_THROW(node.reset(storage.node(testNode.id)));
        checkNode(testNode, *node, storage);
    }

    checkObjects<Node, test::Node, NodeIDSet>(
        nodes, storage, &Storage::nodes, &checkNode);

    for (const auto& testEdge : edges) {
        boost::optional<Edge> edge;
        BOOST_CHECK_NO_THROW(edge.reset(storage.edge(testEdge.id)));
        checkEdge(testEdge, *edge, storage);
    }

    test::EdgeDataVector edgesToCheck(edges.begin(), edges.end() - 1);
    checkObjects<Edge, test::Edge, EdgeIDSet>(
        edgesToCheck, storage, &Storage::edges, &checkEdge);
}

BOOST_AUTO_TEST_CASE(test_incidences_queries)
{
    test::NodeDataVector nodes {
        gt::Node { 1, {1, 1} },
        gt::Node { 2, {2, 3} },
        gt::Node { 3, {3, 1} },
        gt::Node { 4, {3, 5} },
        gt::Node { 5, {6, 4} },
        gt::Node { 6, {6, 1} },
        gt::Node { 7, {7, 6} },
        gt::Node { 8, {8, 4} },
        gt::Node { 9, {8, 2} },
        gt::Node { 10, {9, 9} },
        gt::Node { 11, {1, 5} }
    };
    IncidencesByNodeMap nodesIncidences {
        { 1, { {1, START}, {2, START} } },
        { 2, { {1, END}, {3, START}, {4, START} } },
        { 3, { {2, END}, {3, END}, {5, START}, {6, START} } },
        { 4, { {7, START}, {13, START}, {13, END} } },
        { 5, { {4, END}, {5, END}, {7, END}, {8, START}, {9, START}, {10, START}, {11, START} } },
        { 6, { {6, END}, {8, END} } },
        { 7, { {9, END}, {12, START} } },
        { 8, { {10, END}, {12, END} } },
        { 9, { {11, END} } },
        { 10, {} },
        { 11, { {14, START}, {14, END} } }
    };

    IncidencesByEdgeMap edgesIncidences = nodesIncToEdgesInc(nodesIncidences);
    BOOST_CHECK_EQUAL(edgesIncidences.size(), 14);

    test::EdgeDataVector edges =
        edgesFromEdgesIncidencesAndNodes(edgesIncidences, nodes);

    auto it = std::find_if(edges.begin(), edges.end(),
        [] (const gt::Edge& edge) -> bool { return edge.id == 13; });
    REQUIRE(it != edges.end(), "Edge 13 not found");
    it->geom = Polyline2 { PointsVector {{3, 5}, {3, 6}, {4, 6}, {3, 5}} };

    it = std::find_if(edges.begin(), edges.end(),
        [] (const gt::Edge& edge) -> bool { return edge.id == 14; });
    REQUIRE(it != edges.end(), "Edge 14 not found");
    it->geom = Polyline2 { PointsVector {{1, 5}, {0, 6}, {1, 6}, {1, 5}} };

    test::MockStorage storage { nodes, edges };

    for (const auto& nodeIncidence : nodesIncidences) {
        const EdgeIDSet& recvIncidences = storage.incidentEdges(nodeIncidence.first);
        const IncidentEdges& expIncidencesVector = nodeIncidence.second;
        EdgeIDSet expIncidences;
        for (const auto& edges : expIncidencesVector) {
            expIncidences.insert(edges.first);
        }
        BOOST_CHECK_EQUAL(recvIncidences.size(), expIncidences.size());
        for (const auto& expIncidence : expIncidences) {
            BOOST_CHECK(
                recvIncidences.find(expIncidence) != recvIncidences.end());
        }
    }

    checkIncidences(storage, &Storage::incidencesByNodes, nodes, nodesIncidences);

    for (const auto& edge : edges) {
        IncidentNodes incidentNodes = storage.incidentNodes(edge.id);
        BOOST_CHECK_EQUAL(incidentNodes.start, edge.start);
        BOOST_CHECK_EQUAL(incidentNodes.end, edge.end);
    }

    checkIncidences(storage, &Storage::incidencesByEdges, edges, edgesIncidences);
}

BOOST_AUTO_TEST_CASE(test_bbox_queries)
{
    test::NodeDataVector nodes {
        gt::Node { 1, {1, 1} },
        gt::Node { 2, {2, 4} },
        gt::Node { 3, {4, 4} },
        gt::Node { 4, {5, 7} },
        gt::Node { 5, {2, 7} },
        gt::Node { 6, {4, 5} },
        gt::Node { 7, {6, 4} },
        gt::Node { 8, {9, 4} },
        gt::Node { 9, {10, 3} },
        gt::Node { 10, {5, 1} },
        gt::Node { 11, {7, 1} },
        gt::Node { 12, {9, 1} },
        gt::Node { 13, {7, 0} },
        gt::Node { 14, {7, 3} }
    };

    IncidencesByEdgeMap edgesIncidences {
        { 1, IncidentNodes {1, 3} },
        { 2, IncidentNodes {2, 3} },
        { 3, IncidentNodes {3, 4} },
        { 4, IncidentNodes {5, 6} },
        { 5, IncidentNodes {7, 8} },
        { 6, IncidentNodes {8, 9} },
        { 7, IncidentNodes {10, 11} },
        { 8, IncidentNodes {11, 12} },
        { 9, IncidentNodes {11, 13} },
        { 10, IncidentNodes {11, 14} }
    };

    std::vector<IDsByBBoxTest> tests {
        {   BoundingBox {{1.5, 1.5}, {5.5, 8}},
            EdgeIDSet {1, 2, 3, 4},
            NodeIDSet {2, 3, 4, 5, 6}
        },
        {   BoundingBox {{0.5, 0.5}, {5.5, 8}},
            EdgeIDSet {1, 2, 3, 4, 7},
            NodeIDSet {1, 2, 3, 4, 5, 6, 10}
        },
        {   BoundingBox {{7.0 - 1e-3, -1e-3}, {9.0 + 1e-3, 4.0 + 1e-3}},
            EdgeIDSet {5, 6, 7, 8, 9, 10},
            NodeIDSet {8, 11, 12, 13, 14}
        },
        {   BoundingBox {{7.0 + 1e-3, 1e-3}, {9.0 - 1e-3, 4.0 - 1e-3}},
            EdgeIDSet {8},
            NodeIDSet {}
        }
    };

    test::EdgeDataVector edges =
        edgesFromEdgesIncidencesAndNodes(edgesIncidences, nodes);
    BOOST_CHECK_EQUAL(edges.size(), 10);

    test::MockStorage storage { nodes, edges };

    for (const auto& test : tests) {
        BOOST_CHECK(storage.edgeIds(test.bbox) == test.edgeIds);
        BOOST_CHECK(storage.nodeIds(test.bbox) == test.nodeIds);
    }
}

BOOST_AUTO_TEST_CASE(test_create_nodes)
{
    PointsVector points { {1, 1}, {3, 2}, {5, 2}, {6, 3} };

    test::MockStorage storage;

    for (const auto& point : points) {
        Node newNode = storage.createNode(point);
        const Node& getNode = storage.node(newNode.id());
        BOOST_CHECK_EQUAL(newNode.id(), getNode.id());
        BOOST_CHECK(newNode.pos() == getNode.pos());
    }
}

BOOST_AUTO_TEST_CASE(test_update_nodes)
{
    test::NodeDataVector nodes {
        gt::Node { 1, {1, 1} },
        gt::Node { 2, {3, 2} },
        gt::Node { 3, {5, 2} },
        gt::Node { 4, {6, 3} }
    };

    test::MockStorage storage { nodes, {} };

    std::map<NodeID, Point2> newPositions {
        { 1, {6, 3} },
        { 2, {5, 2} },
        { 3, {3, 2} },
        { 4, {1, 1} }
    };

    for (const auto& posPair : newPositions) {
        storage.updateNodePos(posPair.first, posPair.second);
        const Node& getNode = storage.node(posPair.first);
        BOOST_CHECK(getNode.pos() == posPair.second);
    }
}

BOOST_AUTO_TEST_CASE(test_delete_nodes)
{
    test::NodeDataVector nodes {
        gt::Node { 1, {1, 1} },
        gt::Node { 2, {3, 2} },
        gt::Node { 3, {5, 2} },
        gt::Node { 4, {6, 3} }
    };

    test::MockStorage storage { nodes, {} };

    for (const auto& node : nodes) {
        storage.deleteNode(node.id);
        BOOST_CHECK_THROW(storage.node(node.id), maps::Exception);
    }
}

BOOST_AUTO_TEST_CASE(test_create_edges)
{
    test::NodeDataVector nodes {
        gt::Node { 1, {1, 1} },
        gt::Node { 2, {3, 2} },
        gt::Node { 3, {5, 2} },
        gt::Node { 4, {6, 3} }
    };

    test::MockStorage storage { nodes, {} };

    std::vector<std::pair<Polyline2, IncidentNodes>> newEdgesData {
        { Polyline2 { {{1, 1}, {1, 3}, {2, 3}, {3, 2}} }, IncidentNodes {1, 2} },
        { Polyline2 { PointsVector {{3, 2}, {5, 2}} },    IncidentNodes {2, 3} },
        { Polyline2 { {{5, 2}, {4, 4}, {6, 5}, {6, 3}} }, IncidentNodes {3, 4} },
        { Polyline2 { PointsVector {{5, 2}, {6, 3}} },    IncidentNodes {3, 4} }
    };

    for (const auto& newEdgeData : newEdgesData) {
        Edge newEdge = storage.createEdge(newEdgeData.first, newEdgeData.second);
        const Edge& getEdge = storage.edge(newEdge.id());
        BOOST_CHECK_EQUAL(newEdge.id(), getEdge.id());
        checkGeom(newEdge.geom(), getEdge.geom());
    }
}

BOOST_AUTO_TEST_CASE(test_update_edges)
{
    test::NodeDataVector nodes {
        gt::Node { 1, {1, 1} },
        gt::Node { 2, {3, 2} },
        gt::Node { 3, {5, 2} },
        gt::Node { 4, {6, 3} }
    };
    test::EdgeDataVector edges {
        gt::Edge { 1, 1, 2, Polyline2 { {{1, 1}, {1, 3}, {2, 3}, {3, 2}} } },
        gt::Edge { 2, 2, 3, Polyline2 { PointsVector {{3, 2}, {5, 2}} } },
        gt::Edge { 3, 3, 4, Polyline2 { {{5, 2}, {4, 4}, {6, 5}, {6, 3}} } },
        gt::Edge { 4, 3, 4, Polyline2 { PointsVector {{5, 2}, {6, 3}} } }
    };

    test::MockStorage storage { nodes, edges };

    std::map<EdgeID, std::pair<Polyline2, IncidentNodes>> newGeoms {
        { 1, { Polyline2 { {{3, 2}, {2, 3}, {1, 3}, {1, 1}} }, IncidentNodes {2, 1} } },
        { 2, { Polyline2 { PointsVector {{5, 2}, {3, 2}} }, IncidentNodes {3, 2} } },
        { 3, { Polyline2 { {{6, 3}, {6, 5}, {4, 4}, {5, 2}} }, IncidentNodes {4, 3}} },
        { 4, { Polyline2 { PointsVector {{6, 3}, {5, 2}} }, IncidentNodes {4, 3}} }
    };

    for (const auto& newGeomPair : newGeoms) {
        storage.updateEdgeGeom(
            newGeomPair.first, newGeomPair.second.first, newGeomPair.second.second);
        const Edge& getEdge = storage.edge(newGeomPair.first);
        BOOST_CHECK_EQUAL(getEdge.startNode(), newGeomPair.second.second.start);
        BOOST_CHECK_EQUAL(getEdge.endNode(), newGeomPair.second.second.end);
        checkGeom(newGeomPair.second.first, getEdge.geom());
    }
}

BOOST_AUTO_TEST_CASE(test_delete_edges)
{
    test::NodeDataVector nodes {
        gt::Node { 1, {1, 1} },
        gt::Node { 2, {3, 2} },
        gt::Node { 3, {5, 2} },
        gt::Node { 4, {6, 3} }
    };
    test::EdgeDataVector edges {
        gt::Edge { 1, 1, 2, Polyline2 { {{1, 1}, {1, 3}, {2, 3}, {3, 2}} } },
        gt::Edge { 2, 2, 3, Polyline2 { PointsVector {{3, 2}, {5, 2}} } },
        gt::Edge { 3, 3, 4, Polyline2 { {{5, 2}, {4, 4}, {6, 5}, {6, 3}} } },
        gt::Edge { 4, 3, 4, Polyline2 { PointsVector {{5, 2}, {6, 3}} } }
    };

    test::MockStorage storage { nodes, edges };

    for (const auto& edge : edges) {
        storage.deleteEdge(edge.id);
        BOOST_CHECK_THROW(storage.edge(edge.id), maps::Exception);
    }
}

/// Test diff application

BOOST_AUTO_TEST_CASE(test_diff_application)
{
    test::MockStorage storageBefore {
        test::NodeDataVector {
            gt::Node { 1, {1, 1} },
            gt::Node { 2, {2, 2} },
            gt::Node { 3, {4, 2} },
            gt::Node { 4, {4, 4} },
            gt::Node { 5, {5, 4} },
            gt::Node { 6, {6, 1} }
        },
        test::EdgeDataVector {
            gt::Edge { 1, 1, 2, Polyline2 { PointsVector {{1, 1}, {2, 2}} } },
            gt::Edge { 2, 2, 3, Polyline2 { PointsVector {{2, 2}, {4, 2}} } },
            gt::Edge { 3, 3, 4, Polyline2 { PointsVector {{4, 2}, {4, 4}} } },
            gt::Edge { 4, 5, 3, Polyline2 { PointsVector {{5, 4}, {4, 2}} } },
            gt::Edge { 5, 3, 6, Polyline2 { PointsVector {{4, 2}, {6, 1}} } }
        }
    };

    test::MockStorageDiff diff {
        {
            { 1, boost::none },
            { 4, boost::none },
            { 5, boost::none },
            { 6, gt::Node { 6, {7, 1} } },
            { 7, gt::Node { 7, {3, 4} } },
            { 8, gt::Node { 8, {6, 4} } }
        },
        {
            { 1, boost::none },
            { 2, gt::Edge { 2, 2, 3, Polyline2 { {{2, 2}, {3, 1}, {4, 2}} } } },
            { 3, gt::Edge { 3, 3, 7, Polyline2 { PointsVector {{4, 2}, {3, 4}} } } },
            { 4, gt::Edge { 4, 8, 3, Polyline2 { PointsVector {{6, 4}, {4, 2}} } } },
            { 5, boost::none },
            { 6, gt::Edge { 6, 8, 6, Polyline2 { PointsVector {{6, 4}, {7, 1}} } } }
        }
    };

    test::MockStorage storageAfter {
        test::NodeDataVector {
            gt::Node { 2, {2, 2} },
            gt::Node { 3, {4, 2} },
            gt::Node { 6, {7, 1} },
            gt::Node { 7, {3, 4} },
            gt::Node { 8, {6, 4} }
        },
        test::EdgeDataVector {
            gt::Edge { 2, 2, 3, Polyline2 { {{2, 2}, {3, 1}, {4, 2}} } },
            gt::Edge { 3, 3, 7, Polyline2 { PointsVector {{4, 2}, {3, 4}} } },
            gt::Edge { 4, 8, 3, Polyline2 { PointsVector {{6, 4}, {4, 2}} } },
            gt::Edge { 6, 8, 6, Polyline2 { PointsVector {{6, 4}, {7, 1}} } }
        }
    };

    test::MockStorage result(storageBefore, diff);

    BOOST_CHECK_EQUAL(storageAfter.allNodeIds().size(), result.allNodeIds().size());
    for (auto nodeId : storageAfter.allNodeIds()) {
        bool exists = result.nodeExists(nodeId);
        BOOST_CHECK(exists);
        if (!exists) {
            continue;
        }
        BOOST_CHECK(test::compare(
            storageAfter.testNode(nodeId).pos,
            result.testNode(nodeId).pos));
    }

    BOOST_CHECK_EQUAL(storageAfter.allEdgeIds().size(), result.allEdgeIds().size());
    for (auto edgeId : storageAfter.allEdgeIds()) {
        bool exists = result.edgeExists(edgeId);
        BOOST_CHECK(exists);
        if (!exists) {
            continue;
        }
        const test::Edge& exp = storageAfter.testEdge(edgeId);
        const test::Edge& recv = result.testEdge(edgeId);
        BOOST_CHECK_EQUAL(exp.start, recv.start);
        BOOST_CHECK_EQUAL(exp.end, recv.end);
        BOOST_CHECK(test::compare(exp.geom, recv.geom));
    }
}

BOOST_AUTO_TEST_CASE(test_adapters)
{
    auto nodes = test::NodeDataVector {
        gt::Node { 1, {1, 1} },
        gt::Node { 2, {3, 2} },
        gt::Node { 3, {5, 2} }
    };
    std::unordered_set<NodeID> nodeIds = {1, 2, 3};
    auto edges = test::EdgeDataVector {
        gt::Edge { 1, 1, 2, Polyline2 { {{1, 1}, {1, 3}, {2, 3}, {3, 2}} } },
        gt::Edge { 2, 2, 3, Polyline2 { PointsVector {{3, 2}, {5, 2}} } },
        gt::Edge { 3, 1, 3, Polyline2 { PointsVector {{1, 1}, {5, 2}} } }
    };
    std::unordered_set<EdgeID> edgeIds = {1, 2, 3};

    test::MockStorage storage {nodes, edges};


    test::MockStorageEdgeRangeAdaptor edgeRange(storage);
    for (const auto& edge : edgeRange) {
        BOOST_CHECK(edgeIds.count(edge.id) == 1);
    }

    test::MockStorageNodeRangeAdaptor nodeRange(storage);
    for (const auto& node : nodeRange) {
        BOOST_CHECK(nodeIds.count(node.id) == 1);
    }
}

BOOST_AUTO_TEST_SUITE_END()
