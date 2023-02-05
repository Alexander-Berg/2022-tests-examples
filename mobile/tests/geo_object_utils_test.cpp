#include <boost/test/unit_test.hpp>

#include <yandex/maps/navikit/geo_object_utils.h>

#include <yandex/maps/mapkit/geo_object.h>
#include <yandex/maps/mapkit/geo_object_collection.h>
#include <yandex/maps/mapkit/geometry/point.h>
#include <yandex/maps/mapkit/search/business_object_metadata.h>
#include <yandex/maps/mapkit/search/response.h>
#include <yandex/maps/mapkit/uri/uri_object_metadata.h>

#include <memory>

namespace yandex::maps::navikit {

using namespace mapkit;
using namespace search;
using namespace uri;

BOOST_AUTO_TEST_SUITE(GeoObjectUtilsTest)

BOOST_AUTO_TEST_CASE(getValidGeoObjects_givenGeoObjectCollection_skipsIt)
{
    const auto response = std::make_shared<Response>();
    response->collection->children->push_back(
        std::make_shared<GeoObjectCollection>()
    );

    const auto result = getValidGeoObjects(response);

    BOOST_TEST(result.empty());
}

BOOST_AUTO_TEST_CASE(getValidGeoObjects_givenEmptyGeoObjectPointer_skipsIt)
{
    const auto response = std::make_shared<Response>();
    response->collection->children->push_back(
        std::shared_ptr<GeoObject>()
    );

    const auto result = getValidGeoObjects(response);

    BOOST_TEST(result.empty());
}

BOOST_AUTO_TEST_CASE(getValidGeoObjects_givenGeoObjectWithoutPosition_skipsIt)
{
    const auto response = std::make_shared<Response>();
    const auto geoObject = std::make_shared<GeoObject>();
    geoObject->metadataContainer->set(BusinessObjectMetadata());
    response->collection->children->push_back(geoObject);

    const auto result = getValidGeoObjects(response);

    BOOST_TEST(result.empty());
}

BOOST_AUTO_TEST_CASE(getValidGeoObjects_givenGeoObjectWithoutKnownMetadata_skipsIt)
{
    const auto response = std::make_shared<Response>();
    const auto geoObject = std::make_shared<GeoObject>();
    geoObject->geometry->push_back(mapkit::geometry::Point());
    response->collection->children->push_back(geoObject);

    const auto result = getValidGeoObjects(response);

    BOOST_TEST(result.empty());
}

BOOST_AUTO_TEST_CASE(getValidGeoObjects_givenValidObject_returnsIt)
{
    const auto response = std::make_shared<Response>();
    const auto geoObject = std::make_shared<GeoObject>();
    geoObject->geometry->push_back(mapkit::geometry::Point());
    geoObject->metadataContainer->set(BusinessObjectMetadata());
    response->collection->children->push_back(geoObject);

    const auto result = getValidGeoObjects(response);

    BOOST_TEST(result.size() == 1);
}

BOOST_AUTO_TEST_CASE(isEqual_givenObjectsWithMatchingUris_returnsTrue)
{
    UriObjectMetadata metadata{{Uri{"some_uri"}}};

    GeoObject lhs;
    lhs.metadataContainer->set(metadata);
    GeoObject rhs;
    rhs.metadataContainer->set(metadata);

    BOOST_TEST(isEqual(lhs, rhs));
}

BOOST_AUTO_TEST_CASE(isEqual_givenObjectsWithDifferentUris_returnsFalse)
{
    GeoObject lhs;
    lhs.metadataContainer->set(UriObjectMetadata{{Uri{"uri_1"}}});
    GeoObject rhs;
    rhs.metadataContainer->set(UriObjectMetadata{{Uri{"uri_2"}}});

    BOOST_TEST(!isEqual(lhs, rhs));
}

BOOST_AUTO_TEST_CASE(isEqual_givenObjectsWithoutUriButWithSameNameAndPosition_returnsTrue)
{
    mapkit::geometry::Point position{24, 42};
    GeoObject lhs;
    lhs.name = "object";
    lhs.geometry->push_back(position);
    GeoObject rhs;
    rhs.name = "object";
    rhs.geometry->push_back(position);

    BOOST_TEST(isEqual(lhs, rhs));
}

BOOST_AUTO_TEST_CASE(isEqual_givenObjectsWithoutUriAndWithDifferentNames_returnsFalse)
{
    mapkit::geometry::Point position{24, 42};
    GeoObject lhs;
    lhs.name = "object_1";
    lhs.geometry->push_back(position);
    GeoObject rhs;
    rhs.name = "object_2";
    rhs.geometry->push_back(position);

    BOOST_TEST(!isEqual(lhs, rhs));
}

BOOST_AUTO_TEST_CASE(isEqual_givenObjectsWithoutUriAndWithDifferentPositions_returnsFalse)
{
    GeoObject lhs;
    lhs.geometry->push_back(mapkit::geometry::Point{24, 42});
    GeoObject rhs;
    rhs.geometry->push_back(mapkit::geometry::Point{42, 24});

    BOOST_TEST(!isEqual(lhs, rhs));
}

BOOST_AUTO_TEST_SUITE_END()

} // yandex::maps::navikit
