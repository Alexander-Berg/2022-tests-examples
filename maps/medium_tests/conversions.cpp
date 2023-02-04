#include "fixtures.h"

#include <yandex/maps/wiki/common/geojson.h>
#include <yandex/maps/wiki/unittest/arcadia.h>
#include <maps/libs/geolib/include/polygon.h>
#include <maps/libs/geolib/include/serialization.h>
#include <maps/libs/json/include/value.h>
#include <maps/libs/local_postgres/include/instance.h>
#include <library/cpp/testing/unittest/env.h>
#include <library/cpp/testing/unittest/registar.h>

#include <sstream>

namespace maps::wiki::common::tests {

namespace {
    void testGeoJsonToWkb(pgpool3::Pool& pool, const std::string& polygonFilePath)
    {
        auto json = json::Value::fromFile(
            ArcadiaSourceRoot() + "/maps/wikimap/mapspro/libs/common/" + polygonFilePath
        );
        auto polygonJson = json["polygon"];
        std::stringstream stream;
        stream << polygonJson;
        auto polygonText = stream.str();

        auto txn = pool.masterReadOnlyTransaction();

        // json -> geom(4326) -> geom(3395)
        const auto referenceGeomQueryPart =
            "ST_Transform("
                "ST_SetSRID("
                    "ST_GeomFromGeoJSON(" + txn->quote(polygonText) + "), 4326"
                "), 3395"
            ")";

        // json -> wkb(3395) -> geom(3395)
        const auto testedGeomQueryPart =
            "ST_GeomFromWKB(" + txn->quote_raw(geoJsonToMercatorWkb(polygonText)) + ", 3395)";

        // compare 3395-geoms
        auto result = txn->exec("SELECT ST_Area(ST_Difference("
            + referenceGeomQueryPart + ", " + testedGeomQueryPart
            + "))");
        const double EPSILON = 1e-6;
        UNIT_ASSERT(result[0][0].as<double>() < EPSILON);
    }

    void testWkbToGeoJson(pgpool3::Pool& pool, const std::string& polygonFilePath)
    {
        auto json = json::Value::fromFile(
            ArcadiaSourceRoot() + "/maps/wikimap/mapspro/libs/common/" + polygonFilePath
        );
        auto polygonJson = json["polygon"];
        std::stringstream stream;
        stream << polygonJson;
        auto polygonText = stream.str();

        auto txn = pool.masterReadOnlyTransaction();
        txn->exec("SET bytea_output='escape'");

        // json -> geom(4326)
        const auto referenceGeomQueryPart =
            "ST_SetSRID("
                "ST_GeomFromGeoJSON(" + txn->quote(polygonText) + "), 4326"
            ")";

        // geom(4326) -> geom(3395) -> wkb(3395)
        const auto convertToWkbQueryPart =
            "ST_AsBinary("
                "ST_Transform(" + referenceGeomQueryPart + ", 3395)"
            ")";
        auto wkbResult = txn->exec("SELECT " + convertToWkbQueryPart);
        auto wkb = txn->unesc_raw(wkbResult[0][0].as<std::string>());

        // wkb(3395) -> json -> geom(4326)
        const auto testedGeomQueryPart =
            "ST_SetSRID("
                "ST_GeomFromGeoJson(" + txn->quote(mercatorWkbToGeoJson(wkb)) + "), 4326"
            ")";

        // compare 4326-geoms
        auto result = txn->exec("SELECT ST_Area(ST_Difference("
            + referenceGeomQueryPart + ", " + testedGeomQueryPart
            + "))");
        const double EPSILON = 1e-12;
        UNIT_ASSERT(result[0][0].as<double>() < EPSILON);
    }
} // anonymous namespace

Y_UNIT_TEST_SUITE(geojson_wkb) {

Y_UNIT_TEST_F(geojson_to_wkb1, DBFixture)
{
    testGeoJsonToWkb(pool(), "tests/data/splitPolygon_1.json");
}

Y_UNIT_TEST_F(geojson_to_wkb2, DBFixture)
{
    testGeoJsonToWkb(pool(), "tests/data/splitPolygon_2.json");
}

Y_UNIT_TEST_F(geojson_to_wkb3, DBFixture)
{
    testGeoJsonToWkb(pool(), "tests/data/splitPolygon_3.json");
}

Y_UNIT_TEST_F(geojson_to_wkb4, DBFixture)
{
    testGeoJsonToWkb(pool(), "tests/data/splitPolygon_4.json");
}

Y_UNIT_TEST_F(geojson_to_wkb5, DBFixture)
{
    testGeoJsonToWkb(pool(), "tests/data/splitPolygon_5.json");
}

Y_UNIT_TEST_F(geojson_to_wkb6, DBFixture)
{
    testGeoJsonToWkb(pool(), "tests/data/splitPolygon_6.json");
}

Y_UNIT_TEST_F(geojson_to_wkb7, DBFixture)
{
    testGeoJsonToWkb(pool(), "tests/data/splitPolygon_7.json");
}

Y_UNIT_TEST_F(wkb_to_geojson1, DBFixture)
{
    testWkbToGeoJson(pool(), "tests/data/splitPolygon_1.json");
}

Y_UNIT_TEST_F(wkb_to_geojson2, DBFixture)
{
    testWkbToGeoJson(pool(), "tests/data/splitPolygon_2.json");
}

Y_UNIT_TEST_F(wkb_to_geojson3, DBFixture)
{
    testWkbToGeoJson(pool(), "tests/data/splitPolygon_3.json");
}

Y_UNIT_TEST_F(wkb_to_geojson4, DBFixture)
{
    testWkbToGeoJson(pool(), "tests/data/splitPolygon_4.json");
}

Y_UNIT_TEST_F(wkb_to_geojson5, DBFixture)
{
    testWkbToGeoJson(pool(), "tests/data/splitPolygon_5.json");
}

Y_UNIT_TEST_F(wkb_to_geojson6, DBFixture)
{
    testWkbToGeoJson(pool(), "tests/data/splitPolygon_6.json");
}

Y_UNIT_TEST_F(wkb_to_geojson7, DBFixture)
{
    testWkbToGeoJson(pool(), "tests/data/splitPolygon_7.json");
}

}

} // namespace maps::wiki::common::tests
