#define BOOST_AUTO_TEST_MAIN

#include <boost/test/auto_unit_test.hpp>
#include <boost/test/test_tools.hpp>
#include <maps/analyzer/libs/http/include/node.h>

namespace http = maps::analyzer::http;
using std::string;
using std::unique_ptr;

class IoServiceMock : public http::IoService
{
public:
    IoServiceMock(http::Logger& logger) : http::IoService(logger) {}
    void notify(http::NodeBase*) override {}
};

BOOST_AUTO_TEST_CASE(push_and_read)
{
    http::Logger logger;
    IoServiceMock io(logger);
    http::Node<string> node("node", std::nullopt, io, logger);

    node.push("");
    unique_ptr<http::Request> request = node.requestToSend();
    BOOST_REQUIRE(request.get() != 0);
}
