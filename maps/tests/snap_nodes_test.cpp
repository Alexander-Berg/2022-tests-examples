#define BOOST_TEST_ALTERNATIVE_INIT_API

#include "common.h"
#include "suite.h"

#include "../test_types/snap_nodes_test_data.h"

#include "../test_types/mock_storage.h"
#include "../test_types/mock_callbacks.h"
#include "../test_tools/events_builder.h"
#include "../test_tools/storage_diff_helpers.h"
#include "../test_tools/test_cmp.h"
#include "../test_tools/test_suite.h"

#include <yandex/maps/wiki/topo/cache.h>
#include <yandex/maps/wiki/topo/editor.h>
#include <yandex/maps/wiki/topo/events.h>
#include <boost/test/unit_test.hpp>

using namespace maps;
using namespace maps::wiki;
using namespace maps::wiki::topo;
using namespace maps::wiki::topo::test;


class SnapNodesTestMergeNodesCallback: public MergeNodesCallback {

public:
    SnapNodesTestMergeNodesCallback(const MockStorage& result)
        : existedNodeIds_(result.allNodeIds()) {}

    void processRequest(MergeNodesRequest& request) const override {
        if (existedNodeIds_.count(request.deletedId())) {
            request.swapMergedAndDeleted();
        }
    }
private:

    NodeIDSet existedNodeIds_;
};


class SnapNodesTestRunner
{
public:

    static void run(const SnapNodesTestData& test)
    {
        if (test.expectedError()) {
            runIncorrect(test);
        } else {
            runCorrect(test);
        }
    }

private:
    struct TestContext {
        MockStorage storage;
        std::unique_ptr<Cache> cache;
        std::unique_ptr<Editor> editor;
    };

    static std::unique_ptr<TestContext> prepareTestContext(const SnapNodesTestData& test)
    {
        std::unique_ptr<TestContext> result = std::make_unique<TestContext>();
        result->storage = test.original();
        result->cache = std::make_unique<Cache>(result->storage, geolib3::EPS);
        result->editor = result->cache->editor();

        for (auto id : addedNodes(test.original(), test.result())) {
                result->storage.addPredefinedNodePos(id, test.result().testNode(id).pos);
        }
        for (auto id : addedEdges(test.original(), test.result())) {
                result->storage.addPredefinedEdgePos(id, test.result().testEdge(id).geom);
        }

        auto splitData = makeSplitEventData(
            {0, false},             // fake source edge id
            test.splitEdges(),
            geolib3::Polyline2{},   // fake edge aligned geometry
            test.original(), test.result()
        );

        result->editor->registerTopoCallback(std::make_unique<TopoMoveEdgeCallback>());
        result->editor->registerCallback(
            std::make_unique<SnapNodesTestMergeNodesCallback>(test.result())
        );

        auto splitCallback = std::make_unique<MockSplitEdgeCallback>(splitData);
        result->editor->registerCallback(std::move(splitCallback));

        result->editor->registerTopoCallback(std::make_unique<TopoAddEdgeCallback>());

        return result;
    }

    static void runCorrect(const SnapNodesTestData& test)
    {
        auto context = prepareTestContext(test);

        BOOST_REQUIRE_NO_THROW(
            context->editor->snapNodes(test.nodeIds(), test.restrictions())
        );

        test::MockStorage resStorage;

        for (auto id : context->storage.allNodeIds()) {
            resStorage.addPredefinedNodePos(id, context->storage.testNode(id).pos);
        }
        for (auto id : context->storage.allEdgeIds()) {
            resStorage.addPredefinedEdgePos(id, context->storage.testEdge(id).geom);
        }

        std::map<NodeID, NodeID> nodeIdsMap;
        for (auto id : test.result().allNodeIds()) {
            nodeIdsMap.insert({id, resStorage.createNode(test.result().testNode(id).pos).id()});
        }
        for (auto id : test.result().allEdgeIds()) {
            const auto& e = test.result().testEdge(id);
            resStorage.createEdge(
                e.geom,
                IncidentNodes{nodeIdsMap.at(e.start), nodeIdsMap.at(e.end)});
        }

        Cache resCache(resStorage, geolib3::EPS);

        resCache.loadByNodes(resStorage.allNodeIds());
        context->cache->loadByNodes(context->storage.allNodeIds());

        test::checkCacheContents(*context->cache, resCache);
        test::checkStorageContents(context->storage, resStorage);
    }

    static void runIncorrect(const SnapNodesTestData& test)
    {
        auto context = prepareTestContext(test);

        auto errorCode = test.expectedError();
        ASSERT(errorCode);

        CHECK_TOPO_ERROR(
            context->editor->snapNodes(test.nodeIds(), test.restrictions()),
            *errorCode);
    }
};

// tests init and run

boost::unit_test::test_suite* init_unit_test_suite(int, char**)
{
    BoostTestSuiteBuilder<SnapNodesTestData, SnapNodesTestRunner> builder(
        boost::unit_test::framework::master_test_suite());
    mainTestSuite()->visit(builder);
    return nullptr;
}


#ifdef YANDEX_MAPS_BUILD
bool init_unit_test_suite()
{
    init_unit_test_suite(0, NULL);
    return true;
}
int main(int argc, char** argv)
{
    return boost::unit_test::unit_test_main(&init_unit_test_suite, argc, argv);
}
#endif //YANDEX_MAPS_BUILD
