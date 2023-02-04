#pragma once

#include "task.h"

#include <maps/analyzer/libs/signal_filters/include/signal_consumer.h>
#include <maps/analyzer/libs/data/include/gpssignal.h>

#include <boost/test/auto_unit_test.hpp>
#include <boost/optional.hpp>

#include <list>
#include <string>

namespace maps {
namespace analyzer {
namespace signal_filters {

class MockSignalConsumer : public SignalConsumer {
public:
    explicit MockSignalConsumer(const TaskList& tasks) :
        tasks_(tasks) {}

    ~MockSignalConsumer() {
        BOOST_CHECK_EQUAL(tasks_.empty(), true);
    }

    virtual void add(
        const data::GpsSignal& signal,
        const SignalClass& signalClass = UNCLASSIFIED_SIGNAL
    ) {
        BOOST_CHECK_EQUAL(tasks_.empty(), false);
        BOOST_CHECK_EQUAL(tasks_.front().type, Task::ADD_SIGNAL);
        BOOST_CHECK_EQUAL(tasks_.front().signalClass, signalClass);
        BOOST_CHECK_EQUAL(tasks_.front().clid, signal.clid());
        if (tasks_.front().gpsSignal) {
            BOOST_CHECK_CLOSE(tasks_.front().gpsSignal->lon(), signal.lon(), 1e-9);
            BOOST_CHECK_CLOSE(tasks_.front().gpsSignal->lat(), signal.lat(), 1e-9);
            BOOST_CHECK_EQUAL(tasks_.front().gpsSignal->time(), signal.time());
        }
        tasks_.pop_front();
    }

    virtual void processBuffered() {
        BOOST_CHECK_EQUAL(tasks_.empty(), false);
        BOOST_CHECK_EQUAL(tasks_.front().type, Task::PROCESS_BUFFERED);
        tasks_.pop_front();
    }

private:
    TaskList tasks_;
};

// Convinience function for fast signal creation
data::GpsSignal signal(double lon, double lat, size_t time, std::string clid, double accuracy = -1.0) {
    data::GpsSignal signal;
    signal.setLon(lon);
    signal.setLat(lat);
    signal.setTime(boost::posix_time::ptime(boost::gregorian::date(2011,1,1)) + boost::posix_time::seconds(time));
    signal.setClid(clid);
    if (accuracy >= 0.0)
        signal.setAccuracy(accuracy);
    return signal;
}

}}} //maps::analyzer::signal_filters
