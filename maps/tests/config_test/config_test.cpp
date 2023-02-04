// runs multiple http queries inside single thread

#define BOOST_AUTO_TEST_MAIN

#include <maps/analyzer/libs/http/include/config.h>
#include <library/cpp/testing/common/env.h>
#include <boost/test/auto_unit_test.hpp>
#include <boost/test/test_tools.hpp>
#include <fstream>

const std::string TEST_DATA_ROOT = ArcadiaSourceRoot() + "/maps/analyzer/libs/http/tests/";

namespace http = maps::analyzer::http;
namespace xml = maps::xml3;
using std::string;

std::vector<http::config::Target> loadXml(const std::string& filePath)
{
    std::vector<http::config::Target> result;
    try {
        xml::Doc doc = xml::Doc::fromFile(filePath);

        const xml::Node& root = doc.root();
        http::config::Options options(root);

        xml::Nodes targets = root.nodes("targets/target");
        for (size_t i = 0; i < targets.size(); ++i) {
            result.push_back(http::config::Target(targets[i], options));
        }
    } catch (const std::exception& e) {
        BOOST_FAIL("cannot read http config: " << e.what());
    }

    return result;
}

BOOST_AUTO_TEST_CASE(usershandlerTest)
{
    std::vector<http::config::Target> targets =
        loadXml(TEST_DATA_ROOT + "xml/usershandler_sample.xml");

    BOOST_CHECK_EQUAL(targets.size(), 2);
    BOOST_CHECK_EQUAL(targets[0].nodes.size(), 2);
    BOOST_CHECK_EQUAL(targets[1].nodes.size(), 2);

    {
        const http::config::Node& node = targets[0].nodes[0];
        BOOST_CHECK_EQUAL(node.name, "alz01d");
        BOOST_CHECK_EQUAL(node.options.vhost.value(), "whatever.maps.yandex.net");
        BOOST_CHECK_EQUAL(node.options.timeouts.transmit, 100);
        BOOST_CHECK_EQUAL(node.options.timeouts.connect,   10);
        BOOST_CHECK_EQUAL(node.options.limits.queueSize, 1000);
        BOOST_CHECK_EQUAL(node.options.limits.connections, 10);
    }
    {
        const http::config::Node& node = targets[0].nodes[1];
        BOOST_CHECK_EQUAL(node.name, "alz01e");
        BOOST_CHECK_EQUAL(node.options.vhost.value(), "whatever.maps.yandex.net");
        BOOST_CHECK_EQUAL(node.options.timeouts.transmit, 200);
        BOOST_CHECK_EQUAL(node.options.timeouts.connect,   20);
        BOOST_CHECK_EQUAL(node.options.limits.queueSize, 2000);
        BOOST_CHECK_EQUAL(node.options.limits.connections, 20);
    }
    {
        const http::config::Node& node = targets[1].nodes[0];
        BOOST_CHECK_EQUAL(node.name, "alz01f");
        BOOST_CHECK_EQUAL(node.options.vhost.value(), "segmentshandler.maps.yandex.net");
        BOOST_CHECK_EQUAL(node.options.timeouts.transmit, 200);
        BOOST_CHECK_EQUAL(node.options.timeouts.connect,   20);
        BOOST_CHECK_EQUAL(node.options.limits.queueSize, 1000);
        BOOST_CHECK_EQUAL(node.options.limits.connections, 10);
    }
    {
        const http::config::Node& node = targets[1].nodes[1];
        BOOST_CHECK_EQUAL(node.name, "alz01g");
        BOOST_CHECK_EQUAL(node.options.vhost.value(), "segmentshandler.maps.yandex.net");
        BOOST_CHECK_EQUAL(node.options.timeouts.transmit, 200);
        BOOST_CHECK_EQUAL(node.options.timeouts.connect,   20);
        BOOST_CHECK_EQUAL(node.options.limits.queueSize, 1000);
        BOOST_CHECK_EQUAL(node.options.limits.connections, 10);
    }
}
