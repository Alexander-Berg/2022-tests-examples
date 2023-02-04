#include "../test_types/mock_storage.h"
#include "../test_types/mock_callbacks.h"

#include "../test_tools/test_io.h"
#include "../test_tools/test_cmp.h"

#include <yandex/maps/wiki/topo/editor.h>
#include <yandex/maps/wiki/topo/cache.h>
#include <yandex/maps/wiki/topo/events.h>

#include <boost/test/unit_test.hpp>

#include <string>

using namespace boost::unit_test;
using namespace maps;
using namespace maps::wiki;
using namespace maps::wiki::topo;
using namespace maps::wiki::topo::geom;
using namespace maps::wiki::topo::test;
using namespace maps::geolib3;

EdgeIDSet
incidentEdges(MockStorage& storage, NodeID nodeId)
{
    const auto& inc = storage.incidencesByNodes({nodeId});
    ASSERT(inc.size() == 1);
    EdgeIDSet res;
    for (const auto& incPair : inc.begin()->second) {
        res.insert(incPair.first);
    }
    return res;
}

struct DeleteEdgeTest
{
    DeleteEdgeTest(
            const test::MockStorage& storage,
            EdgeID edgeId)
        : storage(storage)
        , edgeId(edgeId)
    {}

    test::MockStorage storage;
    EdgeID edgeId;
};

class DeleteEdgeTestRunner
{
public:
    static void run(const DeleteEdgeTest& test)
    {
        test::MockStorage storage(test.storage);
        Cache cache(storage, geolib3::EPS);
        std::unique_ptr<Editor> editor = cache.editor();

        const auto& edge = storage.testEdge(test.edgeId);
        IncidentNodes incidences{edge.start, edge.end};

        std::list<DeleteEdgeEventData> deleteData {
            { test.edgeId, incidences.start, incidences.end }
        };

        auto topoDeleteCallback = std::make_unique<MockTopoDeleteEdgeCallback>(deleteData);
        MockTopoDeleteEdgeCallback* topoDeleteCallbackPtr = topoDeleteCallback.get();
        editor->registerTopoCallback(std::move(topoDeleteCallback));

        auto deleteCallback = std::make_unique<MockDeleteEdgeCallback>(deleteData);
        MockDeleteEdgeCallback* deleteCallbackPtr = deleteCallback.get();
        editor->registerCallback(std::move(deleteCallback));

        editor->deleteEdge(test.edgeId);

        BOOST_CHECK(topoDeleteCallbackPtr->unprocessedEventIds().empty());
        BOOST_CHECK(deleteCallbackPtr->unprocessedRequestIds().empty());
        BOOST_CHECK(deleteCallbackPtr->unprocessedEventIds().empty());

        test::MockStorage storageCopy = test.storage;
        test::MockStorageDiff diff{ {}, {{ test.edgeId, boost::none }} };
        if (incidentEdges(storageCopy, incidences.start).size() == 1) {
            diff.nodes.insert({incidences.start, boost::none});
        }
        if (incidences.start != incidences.end &&
            incidentEdges(storageCopy, incidences.end).size() == 1)
        {
            diff.nodes.insert({incidences.end, boost::none});
        }
        test::MockStorage resultStorage(test.storage, diff);

        Cache resultCache(resultStorage, geolib3::EPS);

        cache.loadByNodes(storage.allNodeIds());
        resultCache.loadByNodes(resultStorage.allNodeIds());

        test::checkStorageContents(storage, resultStorage);
        test::checkCacheContents(cache, resultCache);
    }

};

BOOST_AUTO_TEST_SUITE(delete_edge_tests)

static test::MockStorage storage1 {
    {   test::Node { 1, {1, 1} },
        test::Node { 2, {2, 2} },
        test::Node { 3, {3, 3} }
    },
    {   test::Edge { 1, 1, 2, Polyline2 { PointsVector {{1, 1}, {2, 2}} } },
        test::Edge { 2, 2, 3, Polyline2 { PointsVector {{2, 2}, {3, 3}} } }
    }
};

BOOST_AUTO_TEST_CASE(test_1)
{
    for (const auto& edgeId : storage1.allEdgeIds()) {
        DeleteEdgeTest test {
            storage1,
            edgeId, /// deleted edge id
        };

        DeleteEdgeTestRunner::run(test);
    }
}

static test::MockStorage storage2 {
    {   test::Node { 1, {1, 2} },
        test::Node { 2, {4, 2} },
        test::Node { 3, {2, 4} },
        test::Node { 4, {5, 2} }
    },
    {   test::Edge { 1, 1, 2, Polyline2 { PointsVector {{1, 2}, {4, 2}} } },
        test::Edge { 2, 2, 3, Polyline2 { PointsVector {{4, 2}, {2, 4}} } },
        test::Edge { 3, 1, 3, Polyline2 { PointsVector {{1, 2}, {2, 4}} } },
        test::Edge { 4, 2, 4, Polyline2 { PointsVector {{4, 2}, {5, 2}} } }
    }
};

BOOST_AUTO_TEST_CASE(test_2)
{
    for (const auto& edgeId : storage2.allEdgeIds()) {
        DeleteEdgeTest test {
            storage2,
            edgeId, /// deleted edge id
        };

        DeleteEdgeTestRunner::run(test);
    }
}

static test::MockStorage storage3 {
    {   test::Node { 1, {1, 2} } },
    {   test::Edge { 1, 1, 1, Polyline2 { {{1, 2}, {4, 2}, {4, 4}, {1, 2}} } } }
};

BOOST_AUTO_TEST_CASE(test_delete_loop)
{
    for (const auto& edgeId : storage3.allEdgeIds()) {
        DeleteEdgeTest test {
            storage3,
            edgeId, /// deleted edge id
        };

        DeleteEdgeTestRunner::run(test);
    }
}

BOOST_AUTO_TEST_CASE(test_several_deletes)
{
    test::MockStorage storage {
        {   test::Node {1, {2, 2}},
            test::Node {2, {4, 4}},
            test::Node {3, {5, 2}},
            test::Node {4, {3, 1}},
            test::Node {5, {8, 2}},
            test::Node {6, {1, 3}},
            test::Node {7, {6, 3}}
        },
        {   test::Edge { 1, 1, 2, Polyline2 { PointsVector {{2, 2}, {4, 4}} } },
            test::Edge { 2, 2, 3, Polyline2 { PointsVector {{4, 4}, {5, 2}} } },
            test::Edge { 3, 1, 4, Polyline2 { PointsVector {{2, 2}, {3, 1}} } },
            test::Edge { 4, 3, 4, Polyline2 { PointsVector {{5, 2}, {3, 1}} } },
            test::Edge { 5, 5, 7, Polyline2 { PointsVector {{8, 2}, {6, 3}} } },
            test::Edge { 6, 1, 6, Polyline2 { PointsVector {{2, 2}, {1, 3}} } },
            test::Edge { 7, 3, 7, Polyline2 { PointsVector {{5, 2}, {6, 3}} } }
        }
    };

    Cache cache(storage, geolib3::EPS);

    std::unique_ptr<Editor> editor = cache.editor();

    std::list<DeleteEdgeEventData> deleteData;
    test::MockStorageDiff diff;
    for (auto id : storage.allEdgeIds()) {
        const test::Edge& deleted = storage.testEdge(id);
        deleteData.emplace_back(id, deleted.start, deleted.end);
        diff.edges.insert({id, boost::none});
    }
    for (auto nodeId : storage.allNodeIds()) {
        diff.nodes.insert({nodeId, boost::none});
    }

    MockStorage expStorage(storage, diff);
    Cache expCache(expStorage, geolib3::EPS);

    editor->registerTopoCallback(
        std::make_unique<test::MockTopoDeleteEdgeCallback>(deleteData));
    editor->registerCallback(
        std::make_unique<test::MockDeleteEdgeCallback>(deleteData));


    for (auto edgeId : storage.allEdgeIds()) {
        editor->deleteEdge(edgeId);
    }

    test::checkStorageContents(storage, expStorage);
    test::checkCacheContents(cache, expCache);
}

BOOST_AUTO_TEST_SUITE_END()
