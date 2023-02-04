#define BOOST_TEST_MAIN

#include <maps/analyzer/libs/common/include/exception.h>
#include <maps/analyzer/services/jams_analyzer/modules/usershandler/tests/test_tools/include/test_tools.h>
#include <maps/analyzer/services/jams_analyzer/modules/usershandler/lib/config.h>
#include <maps/analyzer/services/jams_analyzer/modules/usershandler/lib/usershandler.h>

#include <boost/test/auto_unit_test.hpp>
#include <boost/test/floating_point_comparison.hpp>
#include <boost/test/test_tools.hpp>

#include <chrono>
#include <iostream>

using std::string;
using boost::posix_time::ptime;
namespace pt = boost::posix_time;

BOOST_AUTO_TEST_CASE(config_test) {
    Config config(makeUsershandlerConfig("usershandler.conf"));
    BOOST_CHECK_EQUAL(config.threadsNumber(), 0);
    BOOST_CHECK_EQUAL(config.processWaiting(), pt::seconds(10));
}

BOOST_AUTO_TEST_CASE(test_hosts_config)
{
    Config config(makeUsershandlerConfig("usershandler.conf"));
    BOOST_CHECK_EQUAL(config.targets().segmentTargets.size(), 2);
    BOOST_CHECK_EQUAL(config.newTargets()->segmentTargets.size(), 1);
    for (const auto& target : config.targets().segmentTargets) {
        BOOST_CHECK_EQUAL(target.nodes.size(), 2);
        for (const auto& node: target.nodes) {
            BOOST_CHECK_EQUAL(node.options.vhost.value(), "segmentshandler.maps.yandex.net");
        }
    }
}

BOOST_AUTO_TEST_CASE(no_graph_file) {
    const auto configPath = makeUsershandlerConfig("usershandler.conf");

    updateConfig(configPath, {"road_graph", "path_to_graph"}, NONEXISTENT_PATH);

    BOOST_CHECK_THROW(UsersHandler u(configPath), maps::analyzer::NoExternalResourceError);
}

BOOST_AUTO_TEST_CASE(no_edges_tree) {
    const auto configPath = makeUsershandlerConfig("usershandler.conf");

    updateConfig(configPath, {"road_graph", "path_to_rtree"}, NONEXISTENT_PATH);

    BOOST_CHECK_THROW(UsersHandler u(configPath), maps::analyzer::NoExternalResourceError);
}

BOOST_AUTO_TEST_CASE(no_edges_persistent_index) {
    const auto configPath = makeUsershandlerConfig("usershandler.conf");

    updateConfig(configPath, {"road_graph", "path_to_edges_persistent_index"}, NONEXISTENT_PATH);

    BOOST_CHECK_THROW(UsersHandler u(configPath), maps::analyzer::NoExternalResourceError);
}
