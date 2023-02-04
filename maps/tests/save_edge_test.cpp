#define BOOST_TEST_ALTERNATIVE_INIT_API

#include "common.h"
#include "suite.h"

#include "../test_types/save_edge_test_data.h"

#include "../test_types/mock_storage.h"
#include "../test_types/mock_callbacks.h"
#include "../test_tools/events_builder.h"
#include "../test_tools/storage_diff_helpers.h"
#include "../test_tools/test_cmp.h"
#include "../test_tools/test_suite.h"

#include <yandex/maps/wiki/topo/cache.h>
#include <yandex/maps/wiki/topo/editor.h>
#include <yandex/maps/wiki/topo/events.h>

using namespace maps;
using namespace maps::wiki;
using namespace maps::wiki::topo;
using namespace maps::wiki::topo::test;

class SaveEdgeTestRunner
{
public:

    static void run(const SaveEdgeTestData& test)
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
        MockTopoAddEdgeCallback* addCallbackPtr;
        MockTopoMoveEdgeCallback* moveCallbackPtr;
        MockSplitEdgeCallback* splitCallbackPtr;
        MockTopoDeleteEdgeCallback* deleteCallbackPtr;
    };

    static std::unique_ptr<TestContext> prepareTestContext(const SaveEdgeTestData& test)
    {
        std::unique_ptr<TestContext> result = std::make_unique<TestContext>();
        result->storage = test.original();
        result->cache = std::make_unique<Cache>(result->storage, geolib3::EPS);
        result->editor = result->cache->editor();

        if (!test.expectedError()) {
            auto splitData = makeSplitEventData(
                test.edgeId(), test.splitEdges(), test.alignedPolyline(),
                test.original(), test.result()
            );
            auto addData = makeAddEventData(
                test.edgeId(), test.splitEdges(), test.original(), test.result()
            );
            auto moveData = makeMoveEventData(test.original(), test.result());

            auto deleteData = makeDeleteEventData(test.original(), test.result());

            for (auto id : addedNodes(test.original(), test.result())) {
                result->storage.addPredefinedNodePos(id, test.result().testNode(id).pos);
            }
            for (auto id : addedEdges(test.original(), test.result())) {
                result->storage.addPredefinedEdgePos(id, test.result().testEdge(id).geom);
            }

            auto addCallback = std::make_unique<MockTopoAddEdgeCallback>(addData);
            result->addCallbackPtr = addCallback.get();
            result->editor->registerTopoCallback(std::move(addCallback));

            auto moveCallback = std::make_unique<MockTopoMoveEdgeCallback>(moveData);
            result->moveCallbackPtr = moveCallback.get();
            result->editor->registerTopoCallback(std::move(moveCallback));

            auto splitCallback = std::make_unique<MockSplitEdgeCallback>(splitData);
            result->splitCallbackPtr = splitCallback.get();
            result->editor->registerCallback(std::move(splitCallback));

            auto deleteCallback = std::make_unique<MockTopoDeleteEdgeCallback>(deleteData);
            result->deleteCallbackPtr = deleteCallback.get();
            result->editor->registerTopoCallback(std::move(deleteCallback));
        } else {
            result->editor->registerTopoCallback(
                std::make_unique<topo::TopoCallback<topo::AddEdgeEvent>>());
            result->addCallbackPtr = nullptr;

            result->editor->registerTopoCallback(
                std::make_unique<topo::TopoCallback<topo::MoveEdgeEvent>>());
            result->moveCallbackPtr = nullptr;

            result->editor->registerCallback(
                std::make_unique<topo::Callback<topo::SplitEdgeRequest, topo::SplitEdgeEvent>>());
            result->splitCallbackPtr = nullptr;
        }

        return result;
    }

    static void runCorrect(const SaveEdgeTestData& test)
    {
        auto context = prepareTestContext(test);

        Editor::EdgeData data{test.edgeId(), test.splitPoints(), test.newPolyline()};
        context->editor->saveEdge(data, test.restrictions());

        ASSERT(context->addCallbackPtr);
        BOOST_CHECK(context->addCallbackPtr->unprocessedEventIds().empty());

        ASSERT(context->moveCallbackPtr);
        BOOST_CHECK(context->moveCallbackPtr->unprocessedEventIds().empty());

        ASSERT(context->splitCallbackPtr);
        BOOST_CHECK(context->splitCallbackPtr->unprocessedRequestIds().empty());
        BOOST_CHECK(context->splitCallbackPtr->unprocessedEventIds().empty());

        ASSERT(context->deleteCallbackPtr);
        BOOST_CHECK(context->deleteCallbackPtr->unprocessedEventIds().empty());


        test::MockStorage resStorage = test.result();
        Cache resCache(resStorage, geolib3::EPS);

        context->cache->loadByNodes(context->storage.allNodeIds());
        resCache.loadByNodes(resStorage.allNodeIds());

        test::checkCacheContents(*context->cache, resCache);
        test::checkStorageContents(context->storage, resStorage);
    }

    static void runIncorrect(const SaveEdgeTestData& test)
    {
        auto context = prepareTestContext(test);

        auto errorCode = test.expectedError();
        ASSERT(errorCode);

        Editor::EdgeData data{test.edgeId(), test.splitPoints(), test.newPolyline()};
        CHECK_TOPO_ERROR(
            context->editor->saveEdge(data, test.restrictions()),
            *errorCode);

        // check no changes in storage
        test::checkStorageContents(context->storage, test.original());
    }
};

// tests init and run

boost::unit_test::test_suite* init_unit_test_suite(int, char**)
{
    BoostTestSuiteBuilder<SaveEdgeTestData, SaveEdgeTestRunner> builder(
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
