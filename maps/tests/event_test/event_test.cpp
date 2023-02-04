#define BOOST_AUTO_TEST_MAIN
#define _GLIBCXX_USE_NANOSLEEP

#include "maps/analyzer/libs/http/impl/event.h"

#include <maps/analyzer/libs/http/include/logger.h>

#include <boost/test/auto_unit_test.hpp>
#include <boost/test/test_tools.hpp>

#include <chrono>
#include <functional>

namespace http = maps::analyzer::http;
using namespace std::placeholders;

void eventHandler(int, short, bool* handled)
{
    *handled = true;
}

void eventHandlerDelayed(int, short, bool* handled, size_t delay)
{
    std::this_thread::sleep_for(std::chrono::milliseconds(delay));
    *handled = true;
}

BOOST_AUTO_TEST_CASE(eventTest)
{
    http::Logger logger;
    http::EventBase base(logger);

    bool handled = false;
    http::Event timer(base, std::bind(eventHandler, _1, _2, &handled));

    // immediately
    timeval time = {0, 0};
    timer.turnOn(&time);

    // start event processing loop after all events added
    base.startLoop();

    // wait event handled
    std::this_thread::sleep_for(std::chrono::milliseconds(1000));
    BOOST_REQUIRE(handled);
}

BOOST_AUTO_TEST_CASE(delayedEventTest)
{
    http::Logger logger;
    http::EventBase base(logger);

    bool handled = false;
    http::Event timer(
        base, std::bind(eventHandlerDelayed, _1, _2, &handled, 2000));

    // immediately
    timeval time = {0, 0};
    timer.turnOn(&time);

    // start event processing loop after all events added
    base.startLoop();

    // wait event handled
    std::this_thread::sleep_for(std::chrono::milliseconds(1000));
    BOOST_REQUIRE(!handled);

    std::this_thread::sleep_for(std::chrono::milliseconds(2000));
    BOOST_REQUIRE(handled);
}

BOOST_AUTO_TEST_CASE(stopEventTest)
{
    http::Logger logger;
    http::EventBase base(logger);

    bool handled = false;
    http::Event timer(
        base, std::bind(eventHandlerDelayed, _1, _2, &handled, 2000));

    // immediately
    timeval time = {0, 0};
    timer.turnOn(&time);

    // start event processing loop after all events added
    base.startLoop();

    // wait event handled
    std::this_thread::sleep_for(std::chrono::milliseconds(1000));
    base.stopLoop();
    BOOST_REQUIRE(handled);
}

BOOST_AUTO_TEST_CASE(startStopEventTest)
{
    http::Logger logger;
    http::EventBase base(logger);

    bool handled = false;
    http::Event timer(
        base, std::bind(eventHandlerDelayed, _1, _2, &handled, 2000));

    // immediately
    timeval time = {0, 0};
    timer.turnOn(&time);

    // start event processing loop after all events added
    base.startLoop();

    // wait event handled
    std::this_thread::sleep_for(std::chrono::milliseconds(1000));
    base.stopLoop();
    BOOST_REQUIRE(handled);


    handled = false;
    timer.turnOn(&time);

    base.startLoop();
    std::this_thread::sleep_for(std::chrono::milliseconds(1000));
    base.stopLoop();

    BOOST_REQUIRE(handled);
}

BOOST_AUTO_TEST_CASE(internalThreadJoinTest)
{
    http::Logger logger;
    http::EventBase base(logger);
    base.startLoop();
    // wait deadlock in destructor
}

BOOST_AUTO_TEST_CASE(internalThreadJoinNoLoopTest)
{
    http::Logger logger;
    http::EventBase base(logger);
    // wait deadlock in destructor
}

BOOST_AUTO_TEST_CASE(twoThreadsTest)
{
    http::Logger logger;
    http::EventBase base(logger);
    http::EventBase base2(logger);

    bool handled = false;
    http::Event timer(base, std::bind(eventHandler, _1, _2, &handled));
    bool handled2 = false;
    http::Event timer2(base, std::bind(eventHandler, _1, _2, &handled2));

    // immediately
    timeval time = {0, 0};
    timer.turnOn(&time);
    timer2.turnOn(&time);

    // start event processing loop after all events added
    base.startLoop();
    base2.startLoop();

    // wait event handled
    std::this_thread::sleep_for(std::chrono::milliseconds(1000));
    BOOST_REQUIRE(handled);
    BOOST_REQUIRE(handled2);
}
