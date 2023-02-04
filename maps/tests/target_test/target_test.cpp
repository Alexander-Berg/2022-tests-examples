#define BOOST_AUTO_TEST_MAIN

#include <boost/test/auto_unit_test.hpp>
#include <boost/test/test_tools.hpp>

#include <maps/analyzer/libs/http/include/target.h>
#include <maps/analyzer/libs/http/include/node.h>

#include <functional>

namespace http = maps::analyzer::http;
using std::string;
using namespace std::placeholders;

void increment(const http::Response&, size_t* counter)
{
    (*counter)++;
}

BOOST_AUTO_TEST_CASE(push_and_read)
{
    http::Logger logger;
    http::IoService io(logger);
    http::Node<string> nodeA("nodeA", std::nullopt, io, logger);
    http::Node<string> nodeB("nodeB", std::nullopt, io, logger);

    size_t a = 0, b = 0;
    http::Target<string> targetA(logger, std::bind(increment, _1, &a));
    http::Target<string> targetB(logger, std::bind(increment, _1, &b));

    targetA.addNode(&nodeA);
    targetA.addNode(&nodeA); // duplicate
    targetA.addNode(&nodeB);
    targetB.addNode(&nodeB);

    BOOST_CHECK(targetA.push(string()));
    BOOST_CHECK(targetB.push(string()));

    sleep(5); // TODO fix to syncRequest

    BOOST_CHECK_EQUAL(a, 3);//processed two request to B and one request to A
    BOOST_CHECK_EQUAL(b, 2);//processed two request to B
}

BOOST_AUTO_TEST_CASE(failed_push)
{
    http::Logger logger;
    http::IoService io(logger);
    http::Node<string> nodeA("nodeA", std::nullopt, io, logger,
            http::Timeouts(), http::NodeLimits(100, 1));
    http::Node<string> nodeB("nodeB", std::nullopt, io, logger,
            http::Timeouts(), http::NodeLimits(100, 2));

    size_t a = 0;
    http::Target<string> targetA(logger, std::bind(increment, _1, &a));

    targetA.addNode(&nodeA);
    targetA.addNode(&nodeB);

    BOOST_CHECK(targetA.push(string()));
    BOOST_CHECK(targetA.push(string()));
    int i;
    for (i = 0; i < 5; ++i) {
        if(!targetA.push(string())) break;
    }
    // sooner or later push must fail
    BOOST_CHECK(i < 5);
    sleep(5); // TODO fix to syncRequest

}
