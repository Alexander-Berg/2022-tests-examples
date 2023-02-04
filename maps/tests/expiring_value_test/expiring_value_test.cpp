#define BOOST_TEST_MAIN

#include <maps/analyzer/libs/common/include/expiring_value.h>

#include <boost/test/unit_test.hpp>


namespace ma = maps::analyzer;
namespace pt = boost::posix_time;


BOOST_AUTO_TEST_CASE( check_expiration )
{
    ma::ExpiringValue<int> x(pt::milliseconds(10), 42);
    BOOST_CHECK_EQUAL(x.get(), 42);

    x.set(0);
    BOOST_CHECK_EQUAL(x.get(), 0);

    sleep(1);
    BOOST_CHECK_EQUAL(x.get(), 42);
}
