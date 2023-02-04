#define BOOST_TEST_ALTERNATIVE_INIT_API

#include "common.h"
#include "suite.h"

#include "../test_types/save_objects_test_data.h"

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

class SaveObjectsTestRunner
{
public:

    static void run(const SaveObjectsTestData& test)
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

    static std::unique_ptr<TestContext> prepareTestContext(const SaveObjectsTestData& test)
    {
        std::unique_ptr<TestContext> result = std::make_unique<TestContext>();
        result->storage = test.original();
        result->cache = std::make_unique<Cache>(result->storage, geolib3::EPS);
        result->editor = result->cache->editor();

        result->editor->registerTopoCallback(
            std::make_unique<topo::TopoCallback<topo::AddEdgeEvent>>());

        result->editor->registerTopoCallback(
            std::make_unique<topo::TopoCallback<topo::MoveEdgeEvent>>());

        result->editor->registerCallback(
            std::make_unique<topo::Callback<topo::SplitEdgeRequest, topo::SplitEdgeEvent>>());

        return result;
    }

    static void runCorrect(const SaveObjectsTestData& test)
    {
        auto context = prepareTestContext(test);

        context->editor->saveObjects(test.data(), test.restrictions());

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

    static void runIncorrect(const SaveObjectsTestData& test)
    {
        auto context = prepareTestContext(test);

        auto errorCode = test.expectedError();
        ASSERT(errorCode);

        CHECK_TOPO_ERROR(
            context->editor->saveObjects(test.data(), test.restrictions()),
            *errorCode);

        // check no changes in storage
        test::checkStorageContents(context->storage, test.original());
    }
};

// tests init and run

boost::unit_test::test_suite* init_unit_test_suite(int, char**)
{
    BoostTestSuiteBuilder<SaveObjectsTestData, SaveObjectsTestRunner> builder(
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

