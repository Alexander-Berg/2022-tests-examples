#include "../geom_tools/geom_io.h"
#include "../util/io.h"
#include "../test_tools/test_io.h"
#include "../test_tools/test_cmp.h"
#include "../test_types/mock_storage.h"

#include <yandex/maps/wiki/topo/cache.h>

#include <boost/test/unit_test.hpp>

#include <algorithm>

using namespace boost::unit_test;
using namespace maps;
using namespace maps::wiki;
using namespace maps::wiki::topo;
using namespace maps::wiki::topo::geom;
using namespace maps::wiki::topo::test;
using namespace maps::geolib3;


template <class Set, class Vector>
Set
selectIds(size_t subsetNum, const Vector& allIds)
{
    Set ids;
    for (size_t j = 0; j < allIds.size(); ++j) {
        if (subsetNum & (1ul << j)) {
            ids.insert(allIds[j]);
        }
    }
    return ids;
}

template <class Map, class Set>
void checkIncidences(
    const Set& ids,
    Cache& cache, Map (Cache::*cacheIncidencesBy)(const Set&),
    Storage& storage, Map (Storage::*storageIncidencesBy)(const Set&))
{
    Map storageIncidences = (storage.*storageIncidencesBy)(ids);
    Map cacheIncidences = (cache.*cacheIncidencesBy)(ids);
    BOOST_CHECK_MESSAGE(storageIncidences == cacheIncidences,
        "Incidences on cache and storage differ" <<
        "\nstorage incidences: " << util::print(storageIncidences) <<
        "\ncache incidences: " << util::print(cacheIncidences)
    );
}


BOOST_AUTO_TEST_SUITE(cache_tests)

static const test::MockStorage storage {
    {   test::Node {1, {1, 1}},
        test::Node {2, {2, 2}},
        test::Node {3, {5, 2}},
        test::Node {4, {5, 4}},
        test::Node {5, {7, 2}},
        test::Node {6, {2, 5}},
        test::Node {7, {8, 1}}
    },
    {   test::Edge {1, 1, 2, Polyline2 { PointsVector {{1, 1}, {2, 2}} }},
        test::Edge {2, 2, 3, Polyline2 { PointsVector {{2, 2}, {5, 2}} }},
        test::Edge {3, 3, 4, Polyline2 { PointsVector {{5, 2}, {6, 3}, {5, 4}} }},
        test::Edge {4, 3, 5, Polyline2 { PointsVector {{5, 2}, {7, 2}} }},
        test::Edge {5, 2, 4, Polyline2 { PointsVector {{2, 2}, {5, 4}} }},
        test::Edge {6, 2, 6, Polyline2 { PointsVector {{2, 2}, {2, 5}} }},
        test::Edge {7, 4, 6, Polyline2 { PointsVector {{5, 4}, {2, 5}} }},
        test::Edge {8, 5, 5, Polyline2 { PointsVector {{7, 2}, {7, 3}, {8, 3}, {8, 2}, {7, 2}} }},
        test::Edge {9, 7, 7, Polyline2 { PointsVector {{8, 1}, {7, 0}, {8, 0}, {8, 1}} }}
    }
};

BOOST_AUTO_TEST_CASE(test_incidences_by_nodes)
{
    NodeIDSet allNodeIds = storage.allNodeIds();
    NodeIDVector allIdsVector(allNodeIds.begin(), allNodeIds.end());
    for (size_t i = 0; i < std::pow(2, allNodeIds.size()); ++i) {
        NodeIDSet nodeIds = selectIds<NodeIDSet>(i, allIdsVector);
        test::MockStorage localStorage = storage;
        Cache cache(localStorage, geolib3::EPS);
        checkIncidences<IncidencesByNodeMap>(
            nodeIds,
            cache, &Cache::incidencesByNodes,
            localStorage, &Storage::incidencesByNodes);
    }
}

BOOST_AUTO_TEST_CASE(test_incidences_by_edges)
{
    EdgeIDSet allEdgeIds = storage.allEdgeIds();
    EdgeIDVector allIdsVector(allEdgeIds.begin(), allEdgeIds.end());
    for (size_t i = 0; i < std::pow(2, allEdgeIds.size()); ++i) {
        EdgeIDSet edgeIds = selectIds<EdgeIDSet>(i, allIdsVector);

        test::MockStorage localStorage = storage;
        Cache cache(localStorage, geolib3::EPS);
        checkIncidences<IncidencesByEdgeMap>(
            edgeIds,
            cache, &Cache::incidencesByEdges,
            localStorage, &Storage::incidencesByEdges);
    }
}

BOOST_AUTO_TEST_CASE(test_incidences_by_nodes_sequential_loads)
{
    NodeIDSet allNodeIds = storage.allNodeIds();
    NodeIDVector allIdsVector(allNodeIds.begin(), allNodeIds.end());
    for (size_t i1 = 0; i1 < std::pow(2, allNodeIds.size()); ++i1) {
        for (size_t i2 = 0; i2 < std::pow(2, allNodeIds.size()); ++i2) {
            NodeIDSet nodeIds1 = selectIds<NodeIDSet>(i1, allIdsVector);
            NodeIDSet nodeIds2 = selectIds<NodeIDSet>(i2, allIdsVector);

            test::MockStorage localStorage = storage;
            Cache cache(localStorage, geolib3::EPS);
            cache.loadByNodes(nodeIds1);
            cache.loadByNodes(nodeIds2);

            NodeIDSet nodeIds = nodeIds1;
            nodeIds.insert(nodeIds2.begin(), nodeIds2.end());

            checkIncidences<IncidencesByNodeMap>(
                nodeIds,
                cache, &Cache::incidencesByNodes,
                localStorage, &Storage::incidencesByNodes);
        }
    }
}

BOOST_AUTO_TEST_CASE(test_incidences_by_edges_sequential_loads)
{
    EdgeIDSet allEdgeIds = storage.allEdgeIds();
    EdgeIDVector allIdsVector(allEdgeIds.begin(), allEdgeIds.end());
    for (size_t i1 = 0; i1 < std::pow(2, allEdgeIds.size()); ++i1) {
        for (size_t i2 = 0; i2 < std::pow(2, allEdgeIds.size()); ++i2) {
            EdgeIDSet edgeIds1 = selectIds<EdgeIDSet>(i1, allIdsVector);
            EdgeIDSet edgeIds2 = selectIds<EdgeIDSet>(i2, allIdsVector);

            test::MockStorage localStorage = storage;
            Cache cache(localStorage, geolib3::EPS);
            cache.loadByEdges(edgeIds1);
            cache.loadByEdges(edgeIds2);

            EdgeIDSet edgeIds = edgeIds1;
            edgeIds.insert(edgeIds2.begin(), edgeIds2.end());

            checkIncidences<IncidencesByEdgeMap>(
                edgeIds,
                cache, &Cache::incidencesByEdges,
                localStorage, &Storage::incidencesByEdges);
        }
    }
}

void
shuffleEdges(const EdgeIDVector& shuffledEdgeIds, size_t revSubsetIdx,
    test::EdgeDataVector& edges)
{
    BOOST_REQUIRE(edges.size() >= shuffledEdgeIds.size());
    for (size_t j = 0; j < shuffledEdgeIds.size(); ++j) {
        edges[j].id = shuffledEdgeIds[j];
        if (revSubsetIdx & (1ul << j)) {
            std::swap(edges[j].start, edges[j].end);
            auto points = edges[j].geom.points();
            std::reverse(points.begin(), points.end());
            edges[j].geom = Polyline2(points);
        }
    }
}

BOOST_AUTO_TEST_CASE(test_circuit)
{
    test::NodeDataVector nodes {
        test::Node {1, {1, 1}},
        test::Node {2, {3, 2}},
        test::Node {3, {5, 2}},
        test::Node {4, {6, 4}},
        test::Node {5, {7, 2}},
        test::Node {6, {4, 0}},
        test::Node {7, {2, 4}},
        test::Node {8, {1, 3}}
    };
    test::EdgeDataVector edges {
        test::Edge {1, 1, 2, Polyline2 { PointsVector {{1, 1}, {3, 2}} }},
        test::Edge {2, 2, 3, Polyline2 { PointsVector {{3, 2}, {5, 2}} }},
        test::Edge {3, 3, 4, Polyline2 { PointsVector {{5, 2}, {6, 4}} }},
        test::Edge {4, 4, 5, Polyline2 { PointsVector {{6, 4}, {7, 2}} }},
        test::Edge {5, 7, 8, Polyline2 { PointsVector {{2, 4}, {1, 3}} }},
        test::Edge {6, 2, 6, Polyline2 { PointsVector {{3, 2}, {4, 0}} }},
        test::Edge {7, 3, 6, Polyline2 { PointsVector {{5, 2}, {4, 0}} }},
        test::Edge {8, 2, 7, Polyline2 { PointsVector {{3, 2}, {2, 4}} }}
    };

    std::vector<std::pair<EdgeIDVector, NodeIDVector>> results{
        { {1}, {1, 2} },
        { {1, 2, 3, 4}, {1, 2, 3, 4, 5} }
    };

    for (const auto& testPair : results) {
        EdgeIDVector shuffledEdgeIds = testPair.first;
        NodeIDVector nodeIds = testPair.second;
        do {
            for (size_t i = 0; i < std::pow(2, shuffledEdgeIds.size()); ++i) {
                test::EdgeDataVector shuffledEdges = edges;
                shuffleEdges(shuffledEdgeIds, i, shuffledEdges);
                test::MockStorage storage(nodes, shuffledEdges);
                Cache cache(storage, geolib3::EPS);
                EdgeIDSet edgeIds(shuffledEdgeIds.begin(), shuffledEdgeIds.end());
                {
                    boost::optional<Circuit> res = cache.circuit(edgeIds, false);
                    BOOST_REQUIRE(res.is_initialized() && (*res).edges.size() == shuffledEdgeIds.size());
                    EdgeIDVector edges = (*res).edges;
                    if (edges.front() != shuffledEdgeIds.front()) {
                        std::reverse(edges.begin(), edges.end());
                    }
                    BOOST_CHECK(edges == shuffledEdgeIds);
                }
                {
                    boost::optional<Circuit> res = cache.circuit(edgeIds, true);
                    BOOST_REQUIRE(res.is_initialized());
                    BOOST_REQUIRE((*res).edges.size() == shuffledEdgeIds.size());
                    BOOST_REQUIRE((*res).nodes.is_initialized());
                    NodeIDVector nodes = *(*res).nodes;
                    BOOST_REQUIRE(nodes.size() == nodeIds.size());
                    EdgeIDVector edges = (*res).edges;
                    if (edges.front() != shuffledEdgeIds.front()) {
                        std::reverse(edges.begin(), edges.end());
                    }
                    BOOST_CHECK(edges == shuffledEdgeIds);
                    if (nodes.front() != nodeIds.front()) {
                        std::reverse(nodes.begin(), nodes.end());
                    }
                    BOOST_CHECK_MESSAGE(
                        nodes == nodeIds,
                        "Received: " << util::print(nodes)
                            << ", expected: " << util::print(nodeIds)
                    );
                }
            }
        } while (std::next_permutation(shuffledEdgeIds.begin(), shuffledEdgeIds.end()));
    }

    EdgeIDVector shuffledEdgeIds = EdgeIDVector {1, 2, 3, 4, 5};
    do {
        for (size_t i = 0; i < std::pow(2, shuffledEdgeIds.size()); ++i) {
            test::EdgeDataVector shuffledEdges = edges;
            shuffleEdges(shuffledEdgeIds, i, shuffledEdges);
            test::MockStorage storage(nodes, shuffledEdges);
            Cache cache(storage, geolib3::EPS);
            EdgeIDSet edgeIds(shuffledEdgeIds.begin(), shuffledEdgeIds.end());
            boost::optional<Circuit> res = cache.circuit(edgeIds, false);
            BOOST_REQUIRE(!res.is_initialized());
            res = cache.circuit(edgeIds, true);
            BOOST_REQUIRE(!res.is_initialized());
        }
    } while (std::next_permutation(shuffledEdgeIds.begin(), shuffledEdgeIds.end()));
}

BOOST_AUTO_TEST_CASE(test_no_circuit_with_loops)
{
    test::NodeDataVector nodes {
        test::Node {1, {1, 2}},
        test::Node {2, {4, 2}},
        test::Node {3, {6, 1}},
        test::Node {4, {2, 4}}
    };
    test::EdgeDataVector edges {
        test::Edge {1, 2, 2, Polyline2 { PointsVector {{4, 2}, {4, 3}, {5, 3}, {4, 2}} }},
        test::Edge {2, 1, 2, Polyline2 { PointsVector {{1, 2}, {4, 2}} }},
        test::Edge {3, 2, 3, Polyline2 { PointsVector {{4, 2}, {6, 1}} }},
        test::Edge {4, 2, 4, Polyline2 { PointsVector {{4, 2}, {2, 4}} }}
    };

    for (const auto& edgeIds : std::vector<EdgeIDVector> { {1}, {1, 2}, {1, 2, 3} }) {
        EdgeIDVector shuffledEdgeIds = edgeIds;
        do {
            for (size_t i = 0; i < std::pow(2, shuffledEdgeIds.size()); ++i) {
                test::EdgeDataVector shuffledEdges = edges;
                shuffleEdges(shuffledEdgeIds, i, shuffledEdges);
                test::MockStorage storage(nodes, shuffledEdges);
                Cache cache(storage, geolib3::EPS);
                EdgeIDSet edgeIds(shuffledEdgeIds.begin(), shuffledEdgeIds.end());
                boost::optional<Circuit> res = cache.circuit(edgeIds, false);
                BOOST_REQUIRE(!res.is_initialized());
                res = cache.circuit(edgeIds, true);
                BOOST_REQUIRE(!res.is_initialized());
            }
        } while (std::next_permutation(shuffledEdgeIds.begin(), shuffledEdgeIds.end()));
    }
}

BOOST_AUTO_TEST_SUITE_END()
