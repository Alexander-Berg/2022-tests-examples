#define BOOST_TEST_MAIN
#include "../test_tools.h"

#include <boost/test/auto_unit_test.hpp>

#include <maps/analyzer/services/jams_analyzer/modules/outputbuilder/lib/config.h>
#include <maps/analyzer/services/jams_analyzer/modules/outputbuilder/lib/segment_info_requester.h>
#include <maps/libs/deprecated/boost_time/utils.h>
#include <maps/libs/road_graph/include/graph.h>
#include <maps/libs/edge_persistent_index/include/persistent_index.h>
#include <maps/libs/common/include/exception.h>
#include <yandex/maps/pb_stream2/writer.h>

#include <library/cpp/testing/unittest/env.h>

using std::string;
using std::vector;
namespace pt = boost::posix_time;

class MockedSegmentInfoRequester : public SegmentInfoRequester
{
public:
    MockedSegmentInfoRequester(
        const Config::Targets& targets,
        maps::analyzer::http::Logger& logger,
        const maps::road_graph::PersistentIndex& edgePersistentIndex,
        const maps::road_graph::Graph& graph
    ):SegmentInfoRequester(targets, logger, edgePersistentIndex, graph), persistentIndex_(edgePersistentIndex) { }

protected:
    std::future<std::string> requestNodeContent(size_t targetId,
        size_t nodeId, const std::string&) override
    {
        std::ostringstream out;
        {
            maps::pb_stream2::Writer pbStream(&out);
            maps::road_graph::EdgeId shortId{0};
            maps::analyzer::data::SegmentInfo info = {
                "test-graph3",
                // pseudo-unique segment index
                {shortId, maps::road_graph::SegmentIndex(targetId * 3 + nodeId)},
                0.0, 0, 0
            };
            info.setPersistentEdgeId(
                persistentIndex_.findLongId(shortId).value()
            );
            pbStream << info.data();
        }
        const std::string result = out.str();
        return std::async(std::launch::async, [=]() {
            if (targetId >= 2) {
                throw maps::Exception("invalid url");
            }
            return result;
        });
    }
private:
    const maps::road_graph::PersistentIndex& persistentIndex_;
};

BOOST_AUTO_TEST_CASE(onlineTargetTest)
{
    // 2 accessible targets inside config
    Config config(makeOutputBuilderConfig("outputbuilder.conf"));
    maps::analyzer::http::Logger logger;
    maps::road_graph::PersistentIndex edgePersistentIndex(EDGES_PERSISTENT_INDEX_PATH);
    maps::road_graph::Graph graph(GRAPH_DATA_PATH);
    MockedSegmentInfoRequester requester(config.targets(), logger, edgePersistentIndex, graph);

    pt::ptime timeStart = maps::nowUtc();

    BOOST_REQUIRE(!requester.perform().empty());

    size_t delay = (maps::nowUtc() - timeStart).total_seconds();
    // accept limits equal for all nodes
    size_t limit = config.targets()[0].nodes[0].options.timeouts.connect;
    BOOST_REQUIRE(delay < limit);
}

BOOST_AUTO_TEST_CASE(offlineTargetTest)
{
    // 2 accessible targets inside config
    Config config(makeOutputBuilderConfig("outputbuilder.conf", OK, "outputbuilder-hosts-offline.conf"));
    maps::analyzer::http::Logger logger;
    maps::road_graph::PersistentIndex edgePersistentIndex(EDGES_PERSISTENT_INDEX_PATH);
    maps::road_graph::Graph graph(GRAPH_DATA_PATH);

    MockedSegmentInfoRequester requester(config.targets(), logger, edgePersistentIndex, graph);

    pt::ptime timeStart = maps::nowUtc();

    BOOST_REQUIRE_THROW(
        requester.perform(),
        maps::Exception);

    // need iptables DROP rule to proper work
    // sudo iptables -A INPUT -p tcp --dport 6666 -j DROP

    size_t delay = (maps::nowUtc() - timeStart).total_seconds();
    // accept limits equal for all nodes
    size_t limit = config.targets()[0].nodes[0].options.timeouts.connect;
    // <= because on buildfarm we cannot access to dev machine
    // and timeout = 0
    BOOST_REQUIRE(delay < limit);
}
