#define BOOST_TEST_MAIN

#include <maps/analyzer/libs/types/include/interval.h>

#include <boost/test/unit_test.hpp>
#include <boost/lexical_cast.hpp>

#include <iostream>

namespace ma = maps::analyzer;

template<class T>
void checkInterval(const ma::Interval<T>& interval,
    const T& correctLeft, const T& correctRight)
{
    BOOST_CHECK_EQUAL(interval.left(),  correctLeft);
    BOOST_CHECK_EQUAL(interval.right(), correctRight);
}

BOOST_AUTO_TEST_CASE( contains_test )
{
    BOOST_CHECK(ma::Interval<size_t>().isEmpty());


    ma::Interval<int> interval(5, 5);
    BOOST_CHECK(interval.isEmpty());
    checkInterval(interval, 5, 5);
    for (int t = -100; t <= 100; ++t) {
        BOOST_CHECK_MESSAGE(!ma::contains(interval, t), t);
    }

    interval = ma::Interval<int>(1, 10);
    BOOST_CHECK(!interval.isEmpty());
    for (int t = -100; t <= 100; ++t) {
        BOOST_CHECK_MESSAGE((t < 1 || t >= 10) != ma::contains(interval, t), t);
    }

    interval = ma::Interval<int>(10, 1);
    BOOST_CHECK(!interval.isEmpty());
    for (int t = -100; t <= 100; ++t) {
        BOOST_CHECK_MESSAGE((t >= 1 && t < 10) != ma::contains(interval, t), t);
    }
}

template<class T>
void checkIO(const std::string& string, const ma::Interval<T>& interval)
{
    checkInterval(boost::lexical_cast< ma::Interval<int> >(string),
        interval.left(), interval.right());
}

BOOST_AUTO_TEST_CASE( io_test )
{
    checkIO(" 5  5 ", ma::Interval<int>(5, 5));
    checkIO("5 10", ma::Interval<int>(5, 10));
}
