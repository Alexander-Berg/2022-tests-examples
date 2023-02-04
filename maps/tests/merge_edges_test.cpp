#define BOOST_TEST_ALTERNATIVE_INIT_API
#include "common.h"
#include "suite.h"

#include "../test_types/merge_edges_test_data.h"

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

class MergeEdgesTestRunner
{
public:

    static void run(const MergeEdgesTestData& test)
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
        MockTopoMoveEdgeCallback* moveCallbackPtr;
        MockTopoDeleteEdgeCallback* deleteCallbackPtr;
        MockMergeEdgesCallback* mergeEdgesCallbackPtr;
    };

    static std::unique_ptr<TestContext> prepareTestContext(const MergeEdgesTestData& test)
    {
        std::unique_ptr<TestContext> result = std::make_unique<TestContext>();
        result->storage = test.original();
        result->cache = std::make_unique<Cache>(result->storage, geolib3::EPS);
        result->editor = result->cache->editor();

        if (!test.expectedError()) {
            std::list<topo::MergeEdgesEventData> mergeData =
                {makeMergeEdgesEventData(test.commonNodeId(), test.original(), test.result())};
            auto moveData = makeMoveEventData(test.original(), test.result());
            auto deleteData = makeDeleteEventData(test.original(), test.result());

            auto moveCallback = std::make_unique<MockTopoMoveEdgeCallback>(moveData);
            result->moveCallbackPtr = moveCallback.get();
            result->editor->registerTopoCallback(std::move(moveCallback));

            auto deleteCallback = std::make_unique<MockTopoDeleteEdgeCallback>(deleteData);
            result->deleteCallbackPtr = deleteCallback.get();
            result->editor->registerTopoCallback(std::move(deleteCallback));

            auto mergeEdgesCallback = std::make_unique<MockMergeEdgesCallback>(mergeData);
            result->mergeEdgesCallbackPtr = mergeEdgesCallback.get();
            result->editor->registerCallback(std::move(mergeEdgesCallback));

        } else {
            result->editor->registerTopoCallback(
                std::make_unique<topo::TopoCallback<topo::MoveEdgeEvent>>());
            result->moveCallbackPtr = nullptr;

            result->editor->registerTopoCallback(
                std::make_unique<topo::TopoCallback<topo::DeleteEdgeEvent>>());
            result->deleteCallbackPtr = nullptr;

            result->editor->registerCallback(
                std::make_unique<topo::Callback<topo::MergeEdgesRequest, topo::MergeEdgesEvent>>());
            result->mergeEdgesCallbackPtr = nullptr;
        }

        return result;
    }

    static void runCorrect(const MergeEdgesTestData& test)
    {
        auto context = prepareTestContext(test);

        context->editor->mergeEdges(test.commonNodeId());

        ASSERT(context->moveCallbackPtr);
        BOOST_CHECK(context->moveCallbackPtr->unprocessedEventIds().empty());
        ASSERT(context->deleteCallbackPtr);
        BOOST_CHECK(context->deleteCallbackPtr->unprocessedEventIds().empty());
        ASSERT(context->mergeEdgesCallbackPtr);
        BOOST_CHECK(context->mergeEdgesCallbackPtr->unprocessedRequestIds().empty());
        BOOST_CHECK(context->mergeEdgesCallbackPtr->unprocessedEventIds().empty());

        test::MockStorage resStorage = test.result();
        Cache resCache(resStorage, geolib3::EPS);


        resCache.loadByNodes(resStorage.allNodeIds());
        context->cache->loadByNodes(context->storage.allNodeIds());

        test::checkCacheContents(*context->cache, resCache);
        test::checkStorageContents(context->storage, resStorage);
    }

    static void runIncorrect(const MergeEdgesTestData& test)
    {
        auto context = prepareTestContext(test);

        auto errorCode = test.expectedError();
        ASSERT(errorCode);

        CHECK_TOPO_ERROR(context->editor->mergeEdges(test.commonNodeId()), *errorCode)
        test::checkStorageContents(context->storage, test.original());
    }
};

boost::unit_test::test_suite* init_unit_test_suite(int, char**)
{
    BoostTestSuiteBuilder<MergeEdgesTestData, MergeEdgesTestRunner> builder(
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
