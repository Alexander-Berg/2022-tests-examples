#include <boost/test/unit_test.hpp>

#include "../actions.h"
#include "../serialization.h"

#include "res/test_stopwatch.h"
#include "res/test_response.pb.h"

#include <yandex/maps/mapkit/request_point.h>
#include <yandex/maps/mapkit/directions/driving/internal/make_routes.h>
#include <yandex/maps/mapkit/geometry/geometry.h>
#include <yandex/maps/mapkit/directions/guidance/guide.h>
#include <yandex/maps/mapkit/directions/directions_factory.h>


#include <yandex/maps/runtime/async/dispatcher.h>
#include <yandex/maps/runtime/locale/test/locale_global_fixture.h>

using namespace yandex::maps;
using namespace yandex::maps::naviprovider;
using namespace yandex::maps::mapkit::directions::driving;

const std::shared_ptr<Stopwatch> TEST_STOPWATCH(new TestStopwatch);

namespace {

void checkActionSerialization(const std::unique_ptr<naviprovider::Action>& action)
{
    std::vector<uint8_t> bytes = serialize(action);

    auto deserializedAction =
        deserialize<std::unique_ptr<naviprovider::Action>>(bytes);
    std::vector<uint8_t> reserializedBytes = serialize(deserializedAction);

    BOOST_CHECK_EQUAL(bytes.size(), reserializedBytes.size());
    BOOST_TEST(bytes == reserializedBytes, boost::test_tools::per_element());
}

std::vector<std::uint8_t> createSerializedRoute()
{
    std::shared_ptr<Route> route = internal::makeRoutes(
        internal::parseRouterResponse(std::string(
            reinterpret_cast<const char*>(RESPONSE),
            sizeof(RESPONSE)/sizeof(RESPONSE[0]))),
        std::make_shared<runtime::bindings::SharedVector<mapkit::RequestPoint>>(),
        TEST_STOPWATCH,
        DrivingOptions{},
        VehicleOptions{})->sharedAt(0);

    return mapkit::directions::getDirections()->createDrivingRouter()->routeSerializer()->
        save(route);
}

}

BOOST_AUTO_TEST_SUITE(Serialization)

BOOST_AUTO_TEST_CASE(ActionRouteChangedSerialization)
{
    runtime::async::ui()->spawn([]
    {
        std::unique_ptr<naviprovider::Action> action =
            std::make_unique<ActionRoute>(createSerializedRoute());

        checkActionSerialization(action);
    }).wait();
}

BOOST_AUTO_TEST_CASE(ActionRoutePositionUpdatedSerialization)
{
    std::unique_ptr<naviprovider::Action> action =
        std::make_unique<ActionRoutePosition>(
            mapkit::geometry::PolylinePosition{73, 0.451});

    checkActionSerialization(action);
}

BOOST_AUTO_TEST_CASE(ActionAnnotationsUpdatedSerialization)
{
    runtime::bindings::SharedVector<mapkit::directions::guidance::AnnotationWithDistance> annotations;

    annotations.push_back(mapkit::directions::guidance::AnnotationWithDistance(
        mapkit::directions::driving::Annotation(
            mapkit::directions::driving::Action::Right,
            std::string("Toponym"),
            "Description",
            mapkit::directions::driving::ActionMetadata(),
            runtime::bindings::Vector<mapkit::directions::driving::Landmark>(),
            boost::none,
            boost::none),
        {0.0, "km"},
        {0, 0.0f}));

    mapkit::directions::guidance::DisplayedAnnotations displayedAnnotations(
        annotations,
        std::string("Lva Tolstogo"),
        {},
        boost::none,
        boost::none);

    std::unique_ptr<naviprovider::Action> action = std::make_unique<
        ActionAnnotations>(std::make_shared<
            mapkit::directions::guidance::DisplayedAnnotations>(displayedAnnotations));

    checkActionSerialization(action);
}

BOOST_AUTO_TEST_CASE(ActionSpeedLimitUpdatedSerialization)
{
    std::unique_ptr<naviprovider::Action> action = std::make_unique<
        ActionSpeedLimit>(mapkit::LocalizedValue(60.0, "km"));

    checkActionSerialization(action);
}

BOOST_AUTO_TEST_CASE(ActionSpeedLimitExceededSerialization)
{
    std::unique_ptr<naviprovider::Action> action = std::make_unique<
        ActionSpeedLimitExceeded>(true);

    checkActionSerialization(action);
}

BOOST_AUTO_TEST_CASE(ActionHomeSerialization)
{
    std::unique_ptr<naviprovider::Action> action = std::make_unique<
        ActionHome>(boost::none);

    checkActionSerialization(action);
}

BOOST_AUTO_TEST_CASE(ActionWorkSerialization)
{
    std::unique_ptr<naviprovider::Action> action = std::make_unique<
        ActionWork>(Place(
            {55.733842, 37.588144},
            std::string("Moscow, Lva Tolstogo, 16"),
            std::string("Lva Tolstogo, 16")));

    checkActionSerialization(action);
}

BOOST_AUTO_TEST_CASE(ActionBookmarksSerialization)
{
    std::vector<Bookmark> bookmarks;
    bookmarks.emplace_back("title", std::string("description"), "uri");

    std::unique_ptr<naviprovider::Action> action = std::make_unique<
        ActionBookmarks>(std::make_shared<runtime::bindings::Vector<Bookmark>>(
            bookmarks));

    checkActionSerialization(action);
}

BOOST_AUTO_TEST_CASE(ActionPlacesDatabaseReadySerialization)
{
    std::unique_ptr<naviprovider::Action> action = std::make_unique<
        ActionPlacesDatabaseReady>(true);

    checkActionSerialization(action);
}

BOOST_AUTO_TEST_CASE(ActionServerVersionSerialization)
{
    std::unique_ptr<naviprovider::Action> action = std::make_unique<
        ActionServerVersion>(1);

    checkActionSerialization(action);
}

BOOST_AUTO_TEST_CASE(ActionServerMinorVersionSerialization)
{
    std::unique_ptr<naviprovider::Action> action = std::make_unique<
        ActionServerMinorVersion>(2);

    checkActionSerialization(action);
}

BOOST_AUTO_TEST_CASE(ActionPrevPathLengthSerialization)
{
    std::unique_ptr<naviprovider::Action> action = std::make_unique<
        ActionPrevPathLength>(3.14159);

    checkActionSerialization(action);
}

BOOST_AUTO_TEST_CASE(ActionRoadNameSerialization)
{
    std::unique_ptr<naviprovider::Action> action = std::make_unique<
        ActionRoadName>(std::string("Timura Frunze"));

    checkActionSerialization(action);
}

BOOST_AUTO_TEST_CASE(ActionConfirmationStatusSerialization)
{
    std::unique_ptr<naviprovider::Action> action = std::make_unique<
        ActionConfirmationStatus>(true);

    checkActionSerialization(action);
}

BOOST_AUTO_TEST_CASE(ActionFasterAlternativeSerialization)
{
    runtime::async::ui()->spawn([]
    {
        FasterAlternative fasterAlternative = {
            createSerializedRoute(),
            mapkit::LocalizedValue(10.0, "minutes")
        };

        std::unique_ptr<naviprovider::Action> action =
            std::make_unique<ActionFasterAlternative>(
                std::move(fasterAlternative));

        checkActionSerialization(action);
    }).wait();
}

BOOST_AUTO_TEST_CASE(ActionSoundSchemesSerialization)
{
    runtime::bindings::Vector<SoundScheme> soundSchemes;
    soundSchemes.emplace_back("Oksana", boost::none, "female");
    std::unique_ptr<naviprovider::Action> action = std::make_unique<
        ActionSoundSchemes>(std::make_shared<SoundSchemes>(soundSchemes, 0));

    checkActionSerialization(action);
}

BOOST_AUTO_TEST_CASE(ActionRequestedRoutesSerialization)
{
    runtime::async::ui()->spawn([]
    {
        std::vector<std::vector<uint8_t>> serializedRoutes = {
            createSerializedRoute(),
            createSerializedRoute()
        };

        std::unique_ptr<naviprovider::Action> action =
            std::make_unique<ActionRequestedRoutes>(serializedRoutes);

        checkActionSerialization(action);
    }).wait();
}

BOOST_AUTO_TEST_CASE(ActionBuildRouteErrorSerialization)
{
    auto error = BuildRouteError::NoRoute;
    std::unique_ptr<naviprovider::Action> action =
        std::make_unique<ActionBuildRouteError>(error);

    checkActionSerialization(action);
}

BOOST_AUTO_TEST_SUITE_END()
