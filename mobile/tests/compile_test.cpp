#include <yandex/maps/navi/places/place.h>
#include <yandex/maps/navi/places/places_manager.h>
#include <yandex/maps/navi/places/places_manager_creator.h>
#include <yandex/maps/navi/places/places_utils.h>
#include <yandex/maps/navi/test_environment.h>

#include <yandex/maps/mapkit/geometry/tools.h>

#include <boost/test/unit_test.hpp>

namespace yandex::maps::navi::places {

bool operator==(const Place& left, const Place& right)
{
    return left.position == right.position && left.address == right.address &&
           left.shortAddress == right.shortAddress;
}

BOOST_AUTO_TEST_SUITE(PlacesTest)

BOOST_AUTO_TEST_CASE(setGetTest)
{
    const Place home(mapkit::geometry::Point(1.0, 2.0), std::string("address home"), {});
    const Place work(mapkit::geometry::Point(2.0, 1.0), std::string("address work"), {});

    std::shared_ptr<PlacesDataManager> manager;
    UI(
        manager = createManager(nullptr, nullptr, nullptr, nullptr);
    );

    utils::waitOpen(manager.get());

    UI(
        manager->setHome(home);
        BOOST_CHECK(manager->home() == home);

        manager->setWork(work);
        BOOST_CHECK(manager->work() == work);
    );

    UI(
        manager.reset();
    );
}

BOOST_AUTO_TEST_SUITE_END()

}

