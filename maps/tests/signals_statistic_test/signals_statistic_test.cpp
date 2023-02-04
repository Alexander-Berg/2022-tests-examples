#define BOOST_TEST_MAIN
#include <boost/test/auto_unit_test.hpp>

#include <utils/statistics.h>

#include <maps/libs/common/include/exception.h>

using namespace boost::posix_time;
using namespace maps::jams::analyzer::utils;

BOOST_AUTO_TEST_CASE(update_statistics_new_source)
{
    DeliveryTimeMap deliveryTimeMap;

    SignalSource source1 = std::make_pair(
            "android", "com.yandex.mobile.realty");
    updateDeliveryTimeMap(source1, seconds(1), deliveryTimeMap);

    BOOST_CHECK(deliveryTimeMap.size() == 1);
    BOOST_CHECK(deliveryTimeMap[source1][0] == seconds(1));

    SignalSource source2 = std::make_pair(
            "ios", "com.yandex.mobile.realty");
    updateDeliveryTimeMap(source2, seconds(2), deliveryTimeMap);

    BOOST_CHECK(deliveryTimeMap.size() == 2);
    BOOST_CHECK(deliveryTimeMap[source1].size() == 1);
    BOOST_CHECK(deliveryTimeMap[source1][0] == seconds(1));
    BOOST_CHECK(deliveryTimeMap[source2].size() == 1);
    BOOST_CHECK(deliveryTimeMap[source2][0] == seconds(2));
}

BOOST_AUTO_TEST_CASE(update_statistics_existing_source)
{
    DeliveryTimeMap deliveryTimeMap;

    SignalSource source1 = std::make_pair(
            "android", "com.yandex.mobile.realty");
    updateDeliveryTimeMap(source1, seconds(1), deliveryTimeMap);

    BOOST_CHECK(deliveryTimeMap.size() == 1);
    BOOST_CHECK(deliveryTimeMap[source1][0] == seconds(1));

    updateDeliveryTimeMap(source1, seconds(2), deliveryTimeMap);

    BOOST_CHECK(deliveryTimeMap.size() == 1);
    BOOST_CHECK(deliveryTimeMap[source1].size() == 2);
    BOOST_CHECK(deliveryTimeMap[source1][0] == seconds(1));
    BOOST_CHECK(deliveryTimeMap[source1][1] == seconds(2));
}

BOOST_AUTO_TEST_CASE(calc_statistics_no_signals)
{
    std::vector<time_duration> signalsTime;
    seconds minTime(1);
    seconds medianTime(1);
    seconds maxTime(1);

    BOOST_CHECK_THROW(
            maps::jams::analyzer::utils::calcDeliveryTimeStatistics(
                    signalsTime,
                    &minTime,
                    &medianTime,
                    &maxTime),
            maps::LogicError);
    BOOST_CHECK(signalsTime.size() == 0);
    BOOST_CHECK(minTime == seconds(1));
    BOOST_CHECK(medianTime == seconds(1));
    BOOST_CHECK(maxTime == seconds(1));
}

BOOST_AUTO_TEST_CASE(calc_statistics_single_signal)
{
    std::vector<time_duration> signalsTime { seconds(100) };

    seconds minTime(0);
    seconds medianTime(0);
    seconds maxTime(0);

    BOOST_CHECK_NO_THROW(
            maps::jams::analyzer::utils::calcDeliveryTimeStatistics(
                    signalsTime,
                    &minTime,
                    &medianTime,
                    &maxTime));
    BOOST_CHECK(signalsTime.size() == 1);
    BOOST_CHECK(minTime == seconds(100));
    BOOST_CHECK(medianTime == seconds(100));
    BOOST_CHECK(maxTime == seconds(100));
}

BOOST_AUTO_TEST_CASE(calc_statistics_two_signals)
{
    std::vector<time_duration> signalsTime {
            seconds(100), seconds(300)
    };

    seconds minTime(0);
    seconds medianTime(0);
    seconds maxTime(0);

    BOOST_CHECK_NO_THROW(
            maps::jams::analyzer::utils::calcDeliveryTimeStatistics(
                    signalsTime,
                    &minTime,
                    &medianTime,
                    &maxTime));
    BOOST_CHECK(signalsTime.size() == 2);
    BOOST_CHECK(minTime == seconds(100));
    BOOST_CHECK(medianTime == seconds(300));
    BOOST_CHECK(maxTime == seconds(300));
}

BOOST_AUTO_TEST_CASE(calc_statistics_multiple_signals)
{
    std::vector<time_duration> signalsTime {
            seconds(200), seconds(300), seconds(100), seconds(500), seconds(400)
    };

    seconds minTime(0);
    seconds medianTime(0);
    seconds maxTime(0);

    BOOST_CHECK_NO_THROW(
            maps::jams::analyzer::utils::calcDeliveryTimeStatistics(
                    signalsTime,
                    &minTime,
                    &medianTime,
                    &maxTime));
    BOOST_CHECK(signalsTime.size() == 5);
    BOOST_CHECK(minTime == seconds(100));
    BOOST_CHECK(medianTime == seconds(300));
    BOOST_CHECK(maxTime == seconds(500));
}
