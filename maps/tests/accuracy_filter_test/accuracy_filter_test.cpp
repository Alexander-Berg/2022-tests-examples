#define BOOST_TEST_MODULE best_segments_test
#define BOOST_AUTO_TEST_MAIN

#include "mock_signal_consumer.h"

#include <maps/analyzer/libs/signal_filters/include/accuracy_filter.h>

#include <boost/test/auto_unit_test.hpp>
#include <boost/test/floating_point_comparison.hpp>
#include <boost/assign/std/list.hpp>

#include <iostream>

using namespace maps::analyzer::signal_filters;
using namespace boost::assign;

AccuracyFilter::Config config(double threshold) {
    AccuracyFilter::Config config;
    config.threshold = threshold;
    return config;
}

BOOST_AUTO_TEST_CASE(check_accuracy) {
    TaskList tasks;
    tasks += Task(Task::ADD_SIGNAL, UNCLASSIFIED_SIGNAL, "1");
    tasks += Task(Task::ADD_SIGNAL, UNCLASSIFIED_SIGNAL, "2");
    tasks += Task(Task::ADD_SIGNAL, UNCLASSIFIED_SIGNAL, "3");
    tasks += Task(Task::ADD_SIGNAL, ROUGH_SIGNAL, "4");
    tasks += Task(Task::ADD_SIGNAL, ROUGH_SIGNAL, "5");
    tasks += Task(Task::ADD_SIGNAL, UNCLASSIFIED_SIGNAL, "6");
    tasks += Task(Task::PROCESS_BUFFERED);

    MockSignalConsumer tester(tasks);

    AccuracyFilter filter(config(30.0), tester);

    filter.add(signal(0.000000, 0.000000, 0, "1"));
    filter.add(signal(0.000001, 0.000000, 0, "2", 5));
    filter.add(signal(0.000001, 0.000001, 0, "3", 10));
    filter.add(signal(0.010000, 0.000000, 0, "4", 50));
    filter.add(signal(0.010001, 0.000000, 0, "5", 100));
    filter.add(signal(0.010001, 0.000001, 0, "6", 5));

    filter.processBuffered();
}

BOOST_AUTO_TEST_CASE(dont_check_accuracy) {
    TaskList tasks;
    tasks += Task(Task::ADD_SIGNAL, UNCLASSIFIED_SIGNAL, "1");
    tasks += Task(Task::ADD_SIGNAL, UNCLASSIFIED_SIGNAL, "2");
    tasks += Task(Task::ADD_SIGNAL, UNCLASSIFIED_SIGNAL, "3");
    tasks += Task(Task::ADD_SIGNAL, UNCLASSIFIED_SIGNAL, "4");
    tasks += Task(Task::ADD_SIGNAL, UNCLASSIFIED_SIGNAL, "5");
    tasks += Task(Task::ADD_SIGNAL, UNCLASSIFIED_SIGNAL, "6");
    tasks += Task(Task::PROCESS_BUFFERED);

    MockSignalConsumer tester(tasks);

    AccuracyFilter filter(config(0.0), tester);

    filter.add(signal(0.000000, 0.000000, 0, "1"));
    filter.add(signal(0.000001, 0.000000, 0, "2", 5));
    filter.add(signal(0.000001, 0.000001, 0, "3", 10));
    filter.add(signal(0.010000, 0.000000, 0, "4", 50));
    filter.add(signal(0.010001, 0.000000, 0, "5", 100));
    filter.add(signal(0.010001, 0.000001, 0, "6", 5));

    filter.processBuffered();
}
