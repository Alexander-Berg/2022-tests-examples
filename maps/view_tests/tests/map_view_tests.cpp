#include "tools.h"

#include <yandex/maps/runtime/time.h>
#include <yandex/maps/runtime/async/dispatcher.h>

#include <boost/test/auto_unit_test.hpp>
#include <boost/test/test_tools.hpp>

#include <thread>

using namespace yandex::maps::runtime;
using namespace yandex::maps::mapkit;

namespace {

struct Fixture {
    Fixture()
    {
        async::ui()->spawn([]()
        {
            viewtests::tests::showMapView();
        }).wait();
    }

    ~Fixture()
    {
        async::ui()->spawn([]()
        {
            viewtests::tests::hideMapView();
        }).wait();
    }
};

} //StrictFixture

BOOST_FIXTURE_TEST_SUITE(MapViewTestSuite, Fixture)

BOOST_AUTO_TEST_CASE(emptyTest)
{
    async::ui()->spawn([]()
    {
        viewtests::tests::map()->setNightModeEnabled(true);
    }).wait();
    std::this_thread::sleep_for(TimeInterval(10000));

    BOOST_TEST_CHECKPOINT("empty test");
}

BOOST_AUTO_TEST_SUITE_END()
