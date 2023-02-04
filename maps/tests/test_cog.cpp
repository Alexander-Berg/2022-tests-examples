#include <maps/factory/libs/delivery/cog.h>

#include <maps/factory/libs/unittest/tests_common.h>
#include <maps/factory/libs/dataset/dataset.h>
#include <maps/factory/libs/dataset/tiles.h>
#include <maps/factory/libs/storage/local_storage.h>

namespace maps::factory::delivery::tests {
using namespace maps::factory::tests;

Y_UNIT_TEST_SUITE(cog_should) {

constexpr auto wkt =
    R"(PROJCS["WGS 84 / World Mercator",GEOGCS["WGS 84",DATUM["WGS_1984",SPHEROID["WGS 84",6378137,298.257223563,AUTHORITY["EPSG","7030"]],AUTHORITY["EPSG","6326"]],PRIMEM["Greenwich",0,AUTHORITY["EPSG","8901"]],UNIT["degree",0.0174532925199433,AUTHORITY["EPSG","9122"]],AUTHORITY["EPSG","4326"]],PROJECTION["Mercator_1SP"],PARAMETER["central_meridian",0],PARAMETER["scale_factor",1],PARAMETER["false_easting",0],PARAMETER["false_northing",0],UNIT["metre",1,AUTHORITY["EPSG","9001"]],AXIS["Easting",EAST],AXIS["Northing",NORTH],AUTHORITY["EPSG","3395"]])";

constexpr auto geojson =
    R"({"coordinates":[[[43.81347656249999,61.64816245852389],[47.50488281249999,61.64816245852389],[47.50488281249999,62.92523566254294],[43.81347656249999,62.92523566254294],[43.81347656249999,61.64816245852389]]],"type":"Polygon"})";

constexpr auto jsonRgb =
    R"({"version":1,"color":"Rgb8","zoom":16,"images":{"mul":{"path":"rgb.tif","size":[100,200],"transform":[1,11,0,2,0,12]},"preview":{"path":"preview.jpg","size":[10,20],"transform":[0,1,0,0,0,1]}},"statistics":{"version":2,"covariance":{"weight":0,"mean":[0,0,0,0,0],"covariance":[0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0]},"histogram":{"max":2047,"bins":[[],[],[],[],[]]}},"sr":"PROJCS[\"WGS 84 / World Mercator\",GEOGCS[\"WGS 84\",DATUM[\"WGS_1984\",SPHEROID[\"WGS 84\",6378137,298.257223563,AUTHORITY[\"EPSG\",\"7030\"]],AUTHORITY[\"EPSG\",\"6326\"]],PRIMEM[\"Greenwich\",0,AUTHORITY[\"EPSG\",\"8901\"]],UNIT[\"degree\",0.0174532925199433,AUTHORITY[\"EPSG\",\"9122\"]],AUTHORITY[\"EPSG\",\"4326\"]],PROJECTION[\"Mercator_1SP\"],PARAMETER[\"central_meridian\",0],PARAMETER[\"scale_factor\",1],PARAMETER[\"false_easting\",0],PARAMETER[\"false_northing\",0],UNIT[\"metre\",1,AUTHORITY[\"EPSG\",\"9001\"]],AXIS[\"Easting\",EAST],AXIS[\"Northing\",NORTH],AUTHORITY[\"EPSG\",\"3395\"]]","contour":{"coordinates":[[[43.81347656249999,61.64816245852389],[47.50488281249999,61.64816245852389],[47.50488281249999,62.92523566254294],[43.81347656249999,62.92523566254294],[43.81347656249999,61.64816245852389]]],"type":"Polygon"}})";

constexpr auto jsonPan =
    R"({"version":1,"color":"PanMul16","zoom":20,"images":{"mul":{"path":"mul.tif","size":[25,50],"transform":[1,44,0,2,0,48]},"pan":{"path":"pan.tif","size":[100,200],"transform":[1,11,0,2,0,12]},"preview":{"path":"preview.jpg","size":[10,20],"transform":[0,1,0,0,0,1]}},"statistics":{"version":2,"covariance":{"weight":0,"mean":[0,0,0,0,0],"covariance":[0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0]},"histogram":{"max":2047,"bins":[[],[],[],[],[]]}},"sr":"PROJCS[\"WGS 84 / World Mercator\",GEOGCS[\"WGS 84\",DATUM[\"WGS_1984\",SPHEROID[\"WGS 84\",6378137,298.257223563,AUTHORITY[\"EPSG\",\"7030\"]],AUTHORITY[\"EPSG\",\"6326\"]],PRIMEM[\"Greenwich\",0,AUTHORITY[\"EPSG\",\"8901\"]],UNIT[\"degree\",0.0174532925199433,AUTHORITY[\"EPSG\",\"9122\"]],AUTHORITY[\"EPSG\",\"4326\"]],PROJECTION[\"Mercator_1SP\"],PARAMETER[\"central_meridian\",0],PARAMETER[\"scale_factor\",1],PARAMETER[\"false_easting\",0],PARAMETER[\"false_northing\",0],UNIT[\"metre\",1,AUTHORITY[\"EPSG\",\"9001\"]],AXIS[\"Easting\",EAST],AXIS[\"Northing\",NORTH],AUTHORITY[\"EPSG\",\"3395\"]]","rpc":[6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6],"contour":{"coordinates":[[[43.81347656249999,61.64816245852389],[47.50488281249999,61.64816245852389],[47.50488281249999,62.92523566254294],[43.81347656249999,62.92523566254294],[43.81347656249999,61.64816245852389]]],"type":"Polygon"}})";

Y_UNIT_TEST(save_rgb)
{
    auto cog = Cog()
        .setColor(CogColorChannels::Rgb8)
        .setMaxZoom(16)
        .setSpatialRefWkt(wkt)
        .setMulImage(CogImage()
            .setRelativePath("rgb.tif")
            .setSize({100, 200})
            .setTransform(Eigen::Translation2d(1, 2) * Eigen::AlignedScaling2d(11, 12)))
        .setPreviewImage(CogImage()
            .setRelativePath("preview.jpg")
            .setSize({10, 20})
            .setTransform(Affine2d::Identity()))
        .setContourGeoJson(geojson)
        .validate();
    EXPECT_EQ(cog.json(), jsonRgb);
}

Y_UNIT_TEST(load_rgb)
{
    Cog cog(jsonRgb);
    cog.validate();
    EXPECT_EQ(cog.color(), CogColorChannels::Rgb8);
    EXPECT_EQ(cog.maxZoom(), 16);
    EXPECT_EQ(cog.spatialRefWkt(), wkt);
    EXPECT_FALSE(cog.rpc());

    {
        auto&& img = cog.mulImage();
        EXPECT_EQ(img.relativePath().native(), "rgb.tif");
        EXPECT_THAT(img.size(), EigEq(Array2i(100, 200)));
        Eigen::Matrix3d gt;
        gt << 11, 0, 1,
            0, 12, 2,
            0, 0, 1;
        EXPECT_THAT(img.transform().matrix(), EigEq(gt));
    }

    EXPECT_FALSE(cog.panImage());

    {
        auto&& img = cog.previewImage();
        EXPECT_EQ(img.relativePath().native(), "preview.jpg");
        EXPECT_THAT(img.size(), EigEq(Array2i(10, 20)));
        Eigen::Matrix3d gt;
        gt << 1, 0, 0,
            0, 1, 0,
            0, 0, 1;
        EXPECT_THAT(img.transform().matrix(), EigEq(gt));
    }

    EXPECT_EQ(cog.contourGeoJson(), geojson);
}

Y_UNIT_TEST(save_pan)
{
    auto cog = Cog()
        .setColor(CogColorChannels::PanMul16)
        .setMaxZoom(20)
        .setSpatialRefWkt(wkt)
        .setRpc(TRpc::fromTiffTag(std::vector<double>(92, 6.0)))
        .setPanImage(CogImage()
            .setRelativePath("pan.tif")
            .setSize({100, 200})
            .setTransform(Eigen::Translation2d(1, 2) * Eigen::AlignedScaling2d(11, 12)))
        .setMulImage(CogImage()
            .setRelativePath("mul.tif")
            .setSize({25, 50})
            .setTransform(Eigen::Translation2d(1, 2) * Eigen::AlignedScaling2d(44, 48)))
        .setPreviewImage(CogImage()
            .setRelativePath("preview.jpg")
            .setSize({10, 20})
            .setTransform(Affine2d::Identity()))
        .setContourGeoJson(geojson)
        .validate();

    EXPECT_EQ(cog.json(), jsonPan);
}

Y_UNIT_TEST(load_pan)
{
    Cog cog(jsonPan);
    cog.validate();
    EXPECT_EQ(cog.color(), CogColorChannels::PanMul16);
    EXPECT_EQ(cog.maxZoom(), 20);
    EXPECT_EQ(cog.spatialRefWkt(), wkt);
    ASSERT_TRUE(cog.rpc());
    EXPECT_EQ(cog.rpc()->toTiffTag(), std::vector<double>(92, 6.0));

    {
        ASSERT_TRUE(cog.panImage());
        auto&& img = *cog.panImage();
        EXPECT_EQ(img.relativePath().native(), "pan.tif");
        EXPECT_THAT(img.size(), EigEq(Array2i(100, 200)));
        Eigen::Matrix3d gt;
        gt << 11, 0, 1,
            0, 12, 2,
            0, 0, 1;
        EXPECT_THAT(img.transform().matrix(), EigEq(gt));
    }

    {
        auto&& img = cog.mulImage();
        EXPECT_EQ(img.relativePath().native(), "mul.tif");
        EXPECT_THAT(img.size(), EigEq(Array2i(25, 50)));
        Eigen::Matrix3d gt;
        gt << 44, 0, 1,
            0, 48, 2,
            0, 0, 1;
        EXPECT_THAT(img.transform().matrix(), EigEq(gt));
    }

    {
        auto&& img = cog.previewImage();
        EXPECT_EQ(img.relativePath().native(), "preview.jpg");
        EXPECT_THAT(img.size(), EigEq(Array2i(10, 20)));
        Eigen::Matrix3d gt;
        gt << 1, 0, 0,
            0, 1, 0,
            0, 0, 1;
        EXPECT_THAT(img.transform().matrix(), EigEq(gt));
    }

    EXPECT_EQ(cog.contourGeoJson(), geojson);
}

Y_UNIT_TEST(load_scanex_cog_from_file)
{
    const std::string src = ArcadiaSourceRoot() + "/maps/factory/test_data/cog/scanex_13174627";
    const auto dir = storage::localStorage(src);
    Cog cog(*dir);
    cog.validate();
    EXPECT_EQ(cog.color(), CogColorChannels::Rgb8);
    EXPECT_EQ(cog.maxZoom(), 16);
    EXPECT_THAT(cog.spatialRefWkt(), StartsWith("PROJCS[\"WGS 84 / World Mercator\""));
    EXPECT_FALSE(cog.rpc());
    EXPECT_TRUE(dir->file(cog.mulImage().relativePath())->exists());
    EXPECT_FALSE(cog.panImage());
    EXPECT_TRUE(dir->file(cog.previewImage().relativePath())->exists());
    EXPECT_FALSE(cog.contourGeoJson().empty());
    EXPECT_GT(cog.statistics().covariance.mean()(0), 0.0);
}

Y_UNIT_TEST(load_dg_cog_from_file)
{
    const std::string src = ArcadiaSourceRoot() + "/maps/factory/test_data/cog/dg_058800151040_01_P001";
    const auto dir = storage::localStorage(src);
    Cog cog(*dir);
    cog.validate();
    EXPECT_EQ(cog.color(), CogColorChannels::PanMul16);
    EXPECT_EQ(cog.maxZoom(), 16);
    EXPECT_THAT(cog.spatialRefWkt(), StartsWith("PROJCS[\"WGS 84 / UTM zone 37N\""));
    EXPECT_TRUE(cog.rpc());
    EXPECT_GT(cog.rpc()->factors()(0, 0), 0.0);
    EXPECT_TRUE(dir->file(cog.mulImage().relativePath())->exists());
    EXPECT_TRUE(cog.panImage());
    EXPECT_TRUE(dir->file(cog.panImage()->relativePath())->exists());
    EXPECT_TRUE(dir->file(cog.previewImage().relativePath())->exists());
    EXPECT_FALSE(cog.contourGeoJson().empty());
    EXPECT_GT(cog.statistics().covariance.mean()(0), 0.0);
}

Y_UNIT_TEST(get_paths)
{
    EXPECT_THAT(toStrings(Cog(jsonRgb).paths()), UnorderedElementsAre(
        "COG.JSON", "preview.jpg", "rgb.tif"));
    EXPECT_THAT(toStrings(Cog(jsonPan).paths()), UnorderedElementsAre(
        "COG.JSON", "mul.tif", "pan.tif", "preview.jpg"));
}

Y_UNIT_TEST(get_bounds_in_lat_lng_format)
{
    const std::string src = ArcadiaSourceRoot() + "/maps/factory/test_data/cog";
    const auto dir = storage::localStorage(src);
    Cog cogScanex(*dir->dir("scanex_13174627"));
    Cog cogDg(*dir->dir("dg_058800151040_01_P001"));

    EXPECT_THAT(cogScanex.boundsLatLng(), EigEq(Box2d(
        Eigen::Vector2d(44.0991170259577103, 10.5004984702938788),
        Eigen::Vector2d(44.2161612985429563, 10.5090856463547890)), 1e-6));

    EXPECT_THAT(cogDg.boundsLatLng(), EigEq(Box2d(
        Eigen::Vector2d(37.4924668766302105, 36.8887938079097140),
        Eigen::Vector2d(37.5136348671374122, 37.0106812037096731)), 1e-6));
}

Y_UNIT_TEST(check_zoom)
{
    const std::string src = ArcadiaSourceRoot() + "/maps/factory/test_data/cog/dg_058800151040_01_P001";
    const auto dir = storage::localStorage(src);
    Cog cog(*dir);
    dataset::TDataset ds = dataset::OpenDataset(src / cog.panImage()->relativePath());
    EXPECT_EQ(cog.maxZoom(), ds.Site().MaxTileZoom());
}

} // Y_UNIT_TEST_SUITE

} //namespace maps::factory::delivery::tests

