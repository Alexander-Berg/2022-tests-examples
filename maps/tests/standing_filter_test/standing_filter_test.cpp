#define BOOST_TEST_MODULE best_segments_test
#define BOOST_AUTO_TEST_MAIN

#include "mock_signal_consumer.h"

#include <maps/analyzer/libs/signal_filters/include/standing_filter.h>

#include <library/cpp/testing/common/env.h>
#include <boost/test/auto_unit_test.hpp>
#include <boost/test/floating_point_comparison.hpp>
#include <boost/assign/std/list.hpp>

#include <iostream>

using namespace maps::analyzer::signal_filters;
using namespace boost::assign;

const std::string TEST_DATA_ROOT = ArcadiaSourceRoot() + "/maps/analyzer/libs/signal_filters/tests/data/";

StandingFilter::Config config(double mergeRadius) {
    StandingFilter::Config config;
    config.mergeRadius = mergeRadius;
    return config;
}

BOOST_AUTO_TEST_CASE(two_clusters) {
    // Two signals far from each other and several signals near them
    // We should have two moving signals other should be standing

    TaskList tasks;
    tasks += Task(Task::ADD_SIGNAL, MOVING_SIGNAL, "1");
    tasks += Task(Task::ADD_SIGNAL, STANDING_SIGNAL, "2");
    tasks += Task(Task::ADD_SIGNAL, STANDING_SIGNAL, "3");
    tasks += Task(Task::ADD_SIGNAL, MOVING_SIGNAL, "4");
    tasks += Task(Task::ADD_SIGNAL, STANDING_SIGNAL, "5");
    tasks += Task(Task::ADD_SIGNAL, STANDING_SIGNAL, "6");
    tasks += Task(Task::PROCESS_BUFFERED);

    MockSignalConsumer tester(tasks);

    StandingFilter filter(config(30.0), tester);

    filter.add(signal(0.000000, 0.000000,  0, "1"));
    filter.add(signal(0.000001, 0.000000,  0, "2"));
    filter.add(signal(0.000001, 0.000001,  0, "3"));
    // Now second bunch of signals
    filter.add(signal(0.010000, 0.000000,  0, "4"));
    filter.add(signal(0.010001, 0.000000,  0, "5"));
    filter.add(signal(0.010001, 0.000001,  0, "6"));

    filter.processBuffered();
}

BOOST_AUTO_TEST_CASE(linear) {
    // Signals are placed in line close from each other
    // Check that we don't filter out all of them

    TaskList tasks;
    tasks += Task(Task::ADD_SIGNAL, MOVING_SIGNAL, "1");
    tasks += Task(Task::ADD_SIGNAL, STANDING_SIGNAL, "2");
    tasks += Task(Task::ADD_SIGNAL, STANDING_SIGNAL, "3");
    tasks += Task(Task::ADD_SIGNAL, MOVING_SIGNAL, "4");
    tasks += Task(Task::ADD_SIGNAL, STANDING_SIGNAL, "5");
    tasks += Task(Task::ADD_SIGNAL, STANDING_SIGNAL, "6");
    tasks += Task(Task::PROCESS_BUFFERED);

    MockSignalConsumer tester(tasks);

    StandingFilter filter(config(30.0), tester);

    filter.add(signal(0.000000, 0.000000,  0, "1"));
    filter.add(signal(0.000100, 0.000000,  0, "2"));
    filter.add(signal(0.000200, 0.000000,  0, "3"));
    // Next point is far enough from first one to be considered moving
    filter.add(signal(0.000300, 0.000000,  0, "4"));
    filter.add(signal(0.000400, 0.000000,  0, "5"));
    filter.add(signal(0.000500, 0.000000,  0, "6"));

    filter.processBuffered();
}

BOOST_AUTO_TEST_CASE(first_real_test) {
    std::ifstream tasksInput(TEST_DATA_ROOT + "standing_filter_real_test_data_1.tasks");
    TaskList tasks = readTasksList(tasksInput);
    std::cerr << "Tasks list size is " << tasks.size() << "\n";
    MockSignalConsumer tester(tasks);
    StandingFilter filter(config(10.0), tester);
    std::ifstream signalsInput(TEST_DATA_ROOT + "standing_filter_real_test_data_1.signals");
    while (!(signalsInput >> std::ws).eof()) {
        maps::analyzer::data::GpsSignal signal;
        maps::analyzer::readProtobuf(signalsInput, signal.data());
        filter.add(signal);
    }
}
