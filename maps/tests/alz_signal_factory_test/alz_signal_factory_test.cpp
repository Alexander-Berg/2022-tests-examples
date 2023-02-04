#define BOOST_TEST_MAIN
#include <boost/test/auto_unit_test.hpp>

#include <signal/alz_signal_factory.h>
#include <signal/signal_data.h>

#include <maps/libs/deprecated/boost_time/utils.h>

#include <sstream>

using std::string;
using std::auto_ptr;
using namespace boost::posix_time;
using maps::analyzer::data::GpsSignal;

string timeToString(ptime time)
{
    std::stringstream str;
    str.imbue(std::locale(
                std::locale::classic(),
                new time_facet("%d%m%Y:%H%M%S")));
    str << time;
    return str.str();
}

SignalData getSignalData()
{
    SignalData signal;
    signal.setAttr("uuid", "5");
    signal.setAttr("direction", "1");
    signal.setAttr("avg_speed", "20");
    signal.setAttr("longitude", "37.48");
    signal.setAttr("latitude", "55.78");
    signal.setAttr("time", timeToString(maps::nowUtc() - seconds(5)));
    return signal;
}

BOOST_AUTO_TEST_CASE(make_signal_test)
{
    const auto data = getSignalData();
    const auto signal = AlzSignalFactory::makeSignal(data);

    BOOST_CHECK_EQUAL(signal.clid(),         "auto");
    BOOST_CHECK_EQUAL(signal.uuid(),         "5");
    BOOST_CHECK_EQUAL(signal.direction(),    1);
    BOOST_CHECK_EQUAL(signal.averageSpeed(), 20);
    BOOST_CHECK_EQUAL(signal.lon(),          37.48);
    BOOST_CHECK_EQUAL(signal.lat(),          55.78);
    BOOST_CHECK_EQUAL(signal.time(),         maps::nowUtc() - seconds(5));
    BOOST_CHECK_EQUAL(signal.receiveTime(),  maps::nowUtc());
    BOOST_CHECK_EQUAL(signal.dispatcherReceiveTime(), maps::nowUtc());
}

BOOST_AUTO_TEST_CASE(set_clid_type_test)
{
    auto data = getSignalData();
    data.setAttr("type", "auto");
    {
        const auto signal = AlzSignalFactory::makeSignal(data);
        BOOST_CHECK_EQUAL(signal.clid(), "auto");
    }
    data.setAttr("clid", "7");
    {
        const auto signal = AlzSignalFactory::makeSignal(data);
        BOOST_CHECK_EQUAL(signal.clid(), "auto");
    }
}

BOOST_AUTO_TEST_CASE(set_clid_clid_test)
{
    auto data = getSignalData();
    data.setAttr("clid", "7");
    const auto signal = AlzSignalFactory::makeSignal(data);
    BOOST_CHECK_EQUAL(signal.clid(), "7");
}

BOOST_AUTO_TEST_CASE(synchronize_time_test)
{
    auto data = getSignalData();
    const auto signalCopy = AlzSignalFactory::makeSignal(data);

    data.setAttr("time", timeToString(maps::nowUtc() - seconds(10)));
    data.setAttr("send_time", timeToString(maps::nowUtc()-seconds(5)));
    const auto signal = AlzSignalFactory::makeSignal(data);

    BOOST_CHECK_EQUAL(signal.time(), signalCopy.time() - seconds(5));
}

BOOST_AUTO_TEST_CASE(empty_direction_test)
{
    auto data = getSignalData();
    data.setAttr("direction", "");

    BOOST_REQUIRE_THROW(
        AlzSignalFactory::makeSignal(data),
        maps::Exception);
}

BOOST_AUTO_TEST_CASE(no_speed_test)
{
    SignalData data;
    data.setAttr("uuid", "5");
    data.setAttr("direction", "1");
    data.setAttr("longitude", "37.48");
    data.setAttr("latitude", "55.78");
    data.setAttr("time", timeToString(maps::nowUtc() - seconds(5)));

    const auto signal = AlzSignalFactory::makeSignal(data);
    BOOST_CHECK_EQUAL(signal.averageSpeed(), 0);
}
