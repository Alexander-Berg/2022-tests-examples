#include <yandex/maps/navikit/localized_string.h>
#include <boost/test/unit_test.hpp>
#include <boost/test/unit_test.hpp>

BOOST_AUTO_TEST_SUITE()

BOOST_AUTO_TEST_CASE(localizedString)
{
    auto localizedString = yandex::maps::navikit::localizedString("CommonOk");
    BOOST_CHECK(true);
}

BOOST_AUTO_TEST_SUITE_END()
