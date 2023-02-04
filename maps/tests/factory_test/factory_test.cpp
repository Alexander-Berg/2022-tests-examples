#define BOOST_AUTO_TEST_MAIN

#include <boost/test/auto_unit_test.hpp>
#include <boost/test/test_tools.hpp>
#include <library/cpp/testing/common/env.h>
#include <maps/analyzer/libs/http/include/factory.h>
#include <maps/analyzer/libs/http/include/target.h>
#include <maps/analyzer/libs/http/include/node.h>

namespace http = maps::analyzer::http;
namespace xml = maps::xml3;

const std::string TEST_DATA_ROOT = ArcadiaSourceRoot() + "/maps/analyzer/libs/http/tests/";

class IoServiceMock : public http::IoService
{
public:
    IoServiceMock(http::Logger& logger) : http::IoService(logger) {}
    void notify(http::NodeBase*) override {}
};

BOOST_AUTO_TEST_CASE(test_compilation)
{
    http::Logger logger;
    IoServiceMock io(logger);
    http::Factory factory(io, logger);

    xml::Doc doc = xml::Doc::fromFile(TEST_DATA_ROOT + "xml/usershandler_sample.xml");
    const xml::Node& root = doc.root();
    http::config::Target targetConfig(root.nodes("targets/target")[0],
                                      http::config::Options(root));
    factory.target<http::Target<std::string>>(targetConfig);
}

BOOST_AUTO_TEST_CASE(test_node_creation)
{
    http::Logger logger;
    IoServiceMock io(logger);
    http::Factory factory(io, logger);

    xml::Doc doc = xml::Doc::fromFile(TEST_DATA_ROOT + "xml/usershandler_sample.xml");
    const xml::Node& root = doc.root();

    http::config::Node nodeConfig(
        root.nodes("targets/target/node")[0],
        http::config::Options(root)
    );
    http::Node<std::string>* node = factory.node<http::Node<std::string>>(nodeConfig);
    BOOST_CHECK_EQUAL(node->vhost(), "whatever.maps.yandex.net");
}
