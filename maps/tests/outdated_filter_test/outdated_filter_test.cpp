#define BOOST_TEST_MODULE best_segments_test
#define BOOST_AUTO_TEST_MAIN

#include "outdated_filter_test.h"
#include "mock_signal_consumer.h"

#include <maps/analyzer/libs/signal_filters/include/outdated_filter.h>

#include <library/cpp/testing/common/env.h>
#include <boost/test/auto_unit_test.hpp>
#include <boost/test/floating_point_comparison.hpp>
#include <boost/assign/std/list.hpp>

#include <iostream>

using namespace maps::analyzer::signal_filters;
using namespace boost::assign;
using namespace boost::posix_time;

const std::string TEST_DATA_ROOT = ArcadiaSourceRoot() + "/maps/analyzer/libs/signal_filters/tests/data/";

OutdatedFilter::Config config(int maxSignalInterval) {
    OutdatedFilter::Config config;
    config.maxSignalInterval = seconds(maxSignalInterval);

    return config;
}

BOOST_AUTO_TEST_CASE(outdated_filter_test) {
    TaskList tasks;
    tasks += Task(Task::ADD_SIGNAL, MOVING_SIGNAL, "1");
    tasks += Task(Task::ADD_SIGNAL, MOVING_SIGNAL, "2");
    tasks += Task(Task::ADD_SIGNAL, STANDING_SIGNAL, "3");
    tasks += Task(Task::ADD_SIGNAL, STANDING_SIGNAL, "4");
    tasks += Task(Task::PROCESS_BUFFERED);
    tasks += Task(Task::ADD_SIGNAL, MOVING_SIGNAL, "5");
    tasks += Task(Task::ADD_SIGNAL, MOVING_SIGNAL, "6");
    tasks += Task(Task::PROCESS_BUFFERED);

    MockSignalConsumer tester(tasks);

    OutdatedFilter filter(config(30), tester);

    filter.add(signal(0.000000, 0.0,  0, "1"), MOVING_SIGNAL);
    filter.add(signal(0.000000, 0.0, 10, "2"), MOVING_SIGNAL);
    filter.add(signal(0.000001, 0.0, 20, "3"), STANDING_SIGNAL);
    filter.add(signal(0.000000, 0.0, 30, "4"), STANDING_SIGNAL);
    // After 4th signal outdated filter should generate ProcessBuffered event
    filter.add(signal(0.000000, 0.0, 80, "5"), MOVING_SIGNAL);
    filter.add(signal(0.000000, 0.0, 90, "6"), MOVING_SIGNAL);
    filter.processBuffered();
}

BOOST_AUTO_TEST_CASE(first_real_test) {
    std::ifstream tasksInput(TEST_DATA_ROOT + "outdated_filter_real_test_data_1.tasks");
    TaskList tasks = readTasksList(tasksInput);
    std::cerr << "Tasks list size is " << tasks.size() << "\n";
    MockSignalConsumer tester(tasks);
    OutdatedFilter filter(config(5), tester);
    std::ifstream signalsInput(TEST_DATA_ROOT + "outdated_filter_real_test_data_1.signals");
    size_t i = 0;
    while (!(signalsInput >> std::ws).eof()) {
        maps::analyzer::data::GpsSignal signal;
        maps::analyzer::readProtobuf(signalsInput, signal.data());
        filter.add(signal, generateSignalTypeByIndex(i++));
    }
}

