#include <maps/factory/libs/dataset/create_vector_dataset.h>
#include <maps/factory/libs/dataset/memory_file.h>

#include <maps/factory/libs/unittest/tests_common.h>
#include <maps/libs/common/include/file_utils.h>

#include <boost/filesystem/operations.hpp>

#include <contrib/libs/gdal/ogr/ogr_geometry.h>
#include <contrib/libs/gdal/ogr/ogrsf_frmts/ogrsf_frmts.h>

namespace maps::factory::dataset::tests {
using namespace maps::factory::tests;
using common::readFileToString;

namespace {

constexpr auto geoJsonStr =
    "{\n\"type\": \"FeatureCollection\",\n"
    "\"features\": [\n"
    "{ \"type\": \"Feature\", \"properties\": { }, \"geometry\": "
    "{ \"type\": \"Polygon\", \"coordinates\":"
    " [ [ [ 0.0, 0.0 ], [ 1.0, 0.0 ], [ 0.0, 1.0 ], [ 0.0, 0.0 ] ] ] } "
    "}\n]\n}\n";

constexpr auto kmlStr =
    "<?xml version=\"1.0\" encoding=\"utf-8\" ?>\n"
    "<kml xmlns=\"http://www.opengis.net/kml/2.2\">\n"
    "<Document id=\"root_doc\">\n<Folder><name>data</name>\n"
    "  <Placemark>\n"
    "\t<Style><LineStyle><color>ff0000ff</color></LineStyle><PolyStyle><fill>0</fill></PolyStyle></Style>\n"
    "      <LinearRing><coordinates>0,0 1,0 0,1 0,0</coordinates></LinearRing>\n"
    "  </Placemark>\n"
    "</Folder>\n"
    "</Document></kml>\n";

Geometry testRing()
{
    TypedGeometry<OGRLinearRing> ring;
    ring->addPoint(0, 0);
    ring->addPoint(1, 0);
    ring->addPoint(0, 1);
    ring->addPoint(0, 0); // Geolib adds closing point.
    return std::move(ring);
}

Geometry testPolygon()
{
    TypedGeometry<OGRPolygon> geom;
    geom->addRing(testRing()->toCurve());
    return std::move(geom);
}

} // namespace

Y_UNIT_TEST_SUITE(vector_dataset_should) {

Y_UNIT_TEST(create_empty_geojson)
{
    auto path = "create_empty_geojson.geojson";
    CreateGeoJsonDataset(path).get();
    ASSERT_TRUE(boost::filesystem::exists(path));
    const auto expected =
        "{\n\"type\": \"FeatureCollection\",\n"
        "\"features\": [\n\n]\n}\n";
    EXPECT_EQ(readFileToString(path), expected);
}

Y_UNIT_TEST(create_geojson_with_polygon)
{
    auto path = "create_geojson_with_polygon.geojson";
    auto geom = testPolygon();
    {
        VectorDataset ds = CreateGeoJsonDataset(path);
        ds.addGeometry(std::move(geom), 0);
    }
    EXPECT_EQ(readFileToString(path), geoJsonStr);
}

Y_UNIT_TEST(read_geojson_from_memory)
{
    auto mem = MemoryFile::unique("read_geojson_from_memory.geojson");
    mem.write(geoJsonStr);
    VectorDataset memDs = OpenDataset(mem.path());
    auto geoms = memDs.layerGeometries(0);
    ASSERT_EQ(geoms.size(), 1u);
    EXPECT_EQ(geoms[0], testPolygon());
}

Y_UNIT_TEST(write_geojson_to_memory)
{
    auto mem = MemoryFile::unique("write_geojson_to_memory.geojson");
    CreateGeoJsonDataset(mem.path()).get().addGeometry(testPolygon(), 0);
    EXPECT_EQ(mem.readString(), geoJsonStr);
}

Y_UNIT_TEST(read_single_geometry_from_geojson)
{
    auto mem = MemoryFile::unique("read_single_geom.geojson");
    mem.write(geoJsonStr);
    VectorDataset memDs = OpenDataset(mem.path());
    auto geom = memDs.singleGeometry();
    EXPECT_EQ(geom, testPolygon());
}

Y_UNIT_TEST(write_gpx_to_memory)
{
    auto mem = MemoryFile::unique("write_gpx_to_memory.kml");
    CreateVectorDataset(mem.path())
        .setDriverName("KML")
        .setLayerSpatialReference(geodeticSr())
        .setLayerName("data")
        .setLayerGeomType(GeometryType::LineString)
        .get()
        .addGeometry(testRing(), 0);
    EXPECT_EQ(mem.readString(), kmlStr);
}

} // suite
} //namespace maps::factory::dataset::tests
