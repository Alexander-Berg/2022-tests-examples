#define BOOST_TEST_ALTERNATIVE_INIT_API

#include "common.h"
#include "suite.h"

#include "../test_types/move_node_test_data.h"

#include "../test_types/mock_storage.h"
#include "../test_types/mock_callbacks.h"
#include "../test_tools/events_builder.h"
#include "../test_tools/storage_diff_helpers.h"
#include "../test_tools/test_cmp.h"
#include "../test_tools/test_suite.h"

#include <yandex/maps/wiki/topo/cache.h>
#include <yandex/maps/wiki/topo/editor.h>
#include <yandex/maps/wiki/topo/events.h>
#include <maps/libs/geolib/include/intersection.h>
#include <boost/test/unit_test.hpp>

#include <iostream>

using namespace maps;
using namespace maps::wiki;
using namespace maps::wiki::topo;
using namespace maps::wiki::topo::test;



class MoveNodeSplitEdgeCallback: public SplitEdgeCallback {

public:
    MoveNodeSplitEdgeCallback(const MockStorage& result)
        : result_(result) {}

    void processRequest(SplitEdgeRequest& request) const override {
        const test::Edge& edge = result_.testEdge(request.sourceId());
        for (size_t i = 0; i < request.splitPolylines().size(); ++i) {
            const geolib3::Polyline2& polyline = request.splitPolylines()[i]->geom;
            const geolib3::PolylinesVector intersection = geolib3::intersection(edge.geom, polyline);
            if (intersection.size() == 1 && compare(intersection.front(), polyline)) {
                request.selectPartToKeepID(i);
            }
        }
    }

private:

    const MockStorage& result_;
};


class MoveNodeTestRunner
{
public:

    static void run(const MoveNodeTestData& test)
    {
        if (test.type() == MoveNodeTestData::Incorrect) {
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

    static std::unique_ptr<TestContext> prepareTestContext(const MoveNodeTestData& test)
    {
        std::unique_ptr<TestContext> result = std::make_unique<TestContext>();
        result->storage = test.original();
        result->cache = std::make_unique<Cache>(result->storage, geolib3::EPS);
        result->editor = result->cache->editor();

        result->editor->registerTopoCallback(std::make_unique<TopoAddEdgeCallback>());
        result->editor->registerTopoCallback(std::make_unique<TopoMoveEdgeCallback>());

        if (test.type() == MoveNodeTestData::Incorrect) {
            result->editor->registerCallback(std::make_unique<SplitEdgeCallback>());
            result->editor->registerCallback(std::make_unique<MergeNodesCallback>());
        } else {
            if (test.type() == MoveNodeTestData::Correct) {
                std::list<topo::SplitEdgeEventData> splitData;
                if (test.splitEdges().size() == 1) {
                        const SourceEdgeID& splitedEdgeId = test.splitEdges().front().sourceId;
                        splitData = makeSplitEventData(
                        splitedEdgeId,
                        test.splitEdges(),
                        test.original().testEdge(splitedEdgeId.id()).geom,
                        test.original(),
                        test.result()
                    );
                } else if (test.splitEdges().size() > 1) {
                    std::cout << "check test data" << std::endl;
                }

                auto splitCallback = std::make_unique<MockSplitEdgeCallback>(splitData);
                result->editor->registerCallback(std::move(splitCallback));
            } else {
                auto splitCallback = std::make_unique<MoveNodeSplitEdgeCallback>(test.result());
                result->editor->registerCallback(std::move(splitCallback));
            }

            std::list<MergeNodesEventData> mergeData;
            boost::optional<topo::MergeNodesEventData> optMergeData = makeMergeNodesEventData(
                test.nodeId(), test.mergedNodeId(), test.result()
            );

            if (optMergeData) {
                mergeData.push_back(*optMergeData);
            }

            auto mergeNodesCallback = std::make_unique<MockMergeNodesCallback>(mergeData);
            result->editor->registerCallback(std::move(mergeNodesCallback));

            for (auto id : addedNodes(test.original(), test.result())) {
                result->storage.addPredefinedNodePos(id, test.result().testNode(id).pos);
            }
            for (auto id : addedEdges(test.original(), test.result())) {
                result->storage.addPredefinedEdgePos(id, test.result().testEdge(id).geom);
            }
        }

        return result;
    }

    static void runCorrect(const MoveNodeTestData& test)
    {
        auto context = prepareTestContext(test);

        Editor::NodeData data{test.nodeId(), test.pos()};
        context->editor->saveNode(data, test.restrictions());

        test::MockStorage resStorage = test.result();
        Cache resCache(resStorage, geolib3::EPS);

        context->cache->loadByNodes(context->storage.allNodeIds());
        resCache.loadByNodes(resStorage.allNodeIds());

        test::checkCacheContents(*context->cache, resCache);
        test::checkStorageContents(context->storage, resStorage);
    }

    static void runIncorrect(const MoveNodeTestData& test)
    {
        auto context = prepareTestContext(test);

        auto errorCode = test.expectedError();
        ASSERT(errorCode);

        Editor::NodeData data{test.nodeId(), test.pos()};
        CHECK_TOPO_ERROR(
            context->editor->saveNode(data, test.restrictions()),
            *errorCode);
    }
};

// tests init and run

boost::unit_test::test_suite* init_unit_test_suite(int, char**)
{
    BoostTestSuiteBuilder<MoveNodeTestData, MoveNodeTestRunner> builder(
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
