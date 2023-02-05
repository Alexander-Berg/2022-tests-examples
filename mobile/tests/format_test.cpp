#include <boost/test/unit_test.hpp>

#include <yandex/maps/navikit/format.h>

#include <boost/math/constants/constants.hpp>

namespace yandex::maps::navikit {

BOOST_AUTO_TEST_CASE(FormatTests)
{
    BOOST_CHECK_EQUAL("hello world", format("hello %s", "world"));
    BOOST_CHECK_EQUAL("I've got 3 apples", format("%s've got %d apples", "I", 3));
    BOOST_CHECK_EQUAL("Approximate value of Pi is 3.14", format(
        "Approximate value of %s is %.2lf", "Pi", boost::math::constants::pi<double>()));
    BOOST_CHECK_EQUAL("Char works too", format(
        "%c%c%c%c%c%c%c%c%c%c%c%c%c%c",
        'C', 'h', 'a', 'r', ' ', 'w', 'o', 'r', 'k', 's', ' ', 't', 'o', 'o'));
    BOOST_CHECK_EQUAL("Time is 14:03", format("Time is %02d:%02d", 14, 3));

    BOOST_CHECK_EQUAL("В юникоде тоже все хорошо",
        format("В юникоде тоже все %s", "хорошо"));
}

} // namespace yandex
