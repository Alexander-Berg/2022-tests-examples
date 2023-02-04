#include <maps/wikimap/mapspro/services/editor/src/actions/tools/pedestrian/task_points.h>

#include "controller_tests_common_includes.h"

#include <geos/geom/Point.h>
#include <yandex/maps/wiki/unittest/arcadia.h>

#include <library/cpp/testing/unittest/registar.h>

namespace maps::wiki::tests {

namespace {

class PedestrianFixture : public EditorTestFixture {
public:
    PedestrianFixture() {
        performObjectsImport(
            "tests/data/pedestrian_tasks_objects.json",
            db.connectionString()
        );
    }
};

const TOid ADDRESS_LAYER_OBJECT_ID = 1;
const TOid UNKNOWN_LAYER_OBJECT_ID = 2;
const TOid ENTRANCE_LAYER_OBJECT_ID = 3;

const double COORD_EPS = 1.e-5;


std::vector<Geom> pointsByRequest(const ToolsPedestrianPoints::Request& request)
{
    ToolsPedestrianPoints controller(request);
    const auto result = *controller();
    return std::move(result.points);
}

geolib3::Point2 asGeoPoint(const Geom& geom)
{
    auto geosPointPtr = dynamic_cast<const geos::geom::Point*>(geom.geosGeometryPtr());
    ASSERT(geosPointPtr);

    geolib3::Point2 pointMerc(geosPointPtr->getX(), geosPointPtr->getY());
    return geolib3::convertMercatorToGeodetic(pointMerc);
}

} // unnamed namespace

Y_UNIT_TEST_SUITE(tools_pedestrian_points)
{

WIKI_FIXTURE_TEST_CASE(test_unknown_type_layer, PedestrianFixture)
{
    UNIT_ASSERT_EXCEPTION(
        pointsByRequest(
            ToolsPedestrianPoints::Request{"", UNKNOWN_LAYER_OBJECT_ID}
        ),
        wiki::InternalErrorException
    );
}

WIKI_FIXTURE_TEST_CASE(test_entrances_layer, PedestrianFixture)
{
    auto points = pointsByRequest(
        ToolsPedestrianPoints::Request{"", ENTRANCE_LAYER_OBJECT_ID}
    );

    UNIT_ASSERT_EQUAL(points.size(), 2);

    auto noEntrancesBldPoint = asGeoPoint(points[0]);
    UNIT_ASSERT_DOUBLES_EQUAL(noEntrancesBldPoint.x(), 50.0015, COORD_EPS);
    UNIT_ASSERT_DOUBLES_EQUAL(noEntrancesBldPoint.y(), 50.0015, COORD_EPS);

    auto unnamedEntranceBldPoint = asGeoPoint(points[1]);
    UNIT_ASSERT_DOUBLES_EQUAL(unnamedEntranceBldPoint.x(), 50.0035, COORD_EPS);
    UNIT_ASSERT_DOUBLES_EQUAL(unnamedEntranceBldPoint.y(), 50.0035, COORD_EPS);
}

WIKI_FIXTURE_TEST_CASE(test_addresses_layer, PedestrianFixture)
{
    auto points = pointsByRequest(
        ToolsPedestrianPoints::Request{"", ADDRESS_LAYER_OBJECT_ID}
    );

    UNIT_ASSERT_EQUAL(points.size(), 1);

    auto noAddressesBldPoint = asGeoPoint(points[0]);
    UNIT_ASSERT_DOUBLES_EQUAL(noAddressesBldPoint.x(), 60.0015, COORD_EPS);
    UNIT_ASSERT_DOUBLES_EQUAL(noAddressesBldPoint.y(), 60.0015, COORD_EPS);
}

} // Y_UNIT_TEST_SUITE

} // namespace maps::wiki::tests
