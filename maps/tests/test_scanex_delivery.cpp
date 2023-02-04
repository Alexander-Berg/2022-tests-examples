#include <maps/factory/libs/delivery/scanex_delivery.h>

#include <maps/factory/libs/storage/local_storage.h>
#include <maps/factory/libs/geometry/geometry.h>
#include <maps/factory/libs/dataset/vector_dataset.h>

#include <maps/factory/libs/unittest/tests_common.h>

namespace maps::factory::delivery::tests {
using namespace maps::factory::tests;

namespace {
const std::string DL_PATH =
    ArcadiaSourceRoot() + "/maps/factory/test_data/scanex_deliveries/11123809_extracted_mini";
} // namespace

Y_UNIT_TEST_SUITE(scanex_delivery_should) {

Y_UNIT_TEST(scan_files)
{
    ScanexDelivery dl(storage::localStorage(DL_PATH));

    EXPECT_EQ(dl.midFile()->pathStr(), "SPOT6_PMS_201708090316140_ORT_2438229101_X1.Y2.mid");
    EXPECT_EQ(dl.mifFile()->pathStr(), "SPOT6_PMS_201708090316140_ORT_2438229101_X1.Y2.mif");
    EXPECT_EQ(dl.tifFile()->pathStr(), "SPOT6_PMS_201708090316140_ORT_2438229101_X1.Y2.TIF");
}

Y_UNIT_TEST(check_md5_sums)
{
    {
        ScanexDelivery dl(storage::localStorage(DL_PATH));
        EXPECT_TRUE(dl.hasMD5());
        dl.checkMD5();
    }
    {
        const std::string path =
            ArcadiaSourceRoot() + "/maps/factory/test_data/scanex_deliveries/13174627_geom";
        ScanexDelivery dl(storage::localStorage(path));
        EXPECT_TRUE(dl.hasMD5());
        dl.checkMD5();
    }
    {
        const std::string path =
            ArcadiaSourceRoot() + "/maps/factory/test_data/scanex_deliveries/15675831_all_black";
        ScanexDelivery dl(storage::localStorage(path));
        EXPECT_TRUE(dl.hasMD5());
        dl.checkMD5();
    }
}

Y_UNIT_TEST(copy)
{
    const auto local = storage::localStorage("./tmp")->dir(this->Name_);
    const auto src = storage::localStorage(DL_PATH);

    ScanexDelivery dl = ScanexDelivery(src).copyTo(local);

    EXPECT_TRUE(dl.hasGeometry());
    EXPECT_TRUE(dl.hasMD5());
    dl.checkMD5();
}

Y_UNIT_TEST(try_open_ready)
{
    const auto local = storage::localStorage("./tmp")->dir(this->Name_);
    const auto src = storage::localStorage(DL_PATH);
    ScanexDelivery srcDl(src);
    src->copyAll(*local);

    EXPECT_FALSE(ScanexDelivery::createIfReady(local));
    local->file(".ready")->touch();
    const auto dl = ScanexDelivery::createIfReady(local);
    EXPECT_TRUE(dl);
    EXPECT_TRUE(dl->hasGeometry());
    EXPECT_TRUE(dl->hasMD5());
    dl->checkMD5();
}

Y_UNIT_TEST(mark_kill)
{
    const auto local = storage::localStorage("./tmp")->dir(this->Name_);
    const auto src = storage::localStorage(DL_PATH);
    ScanexDelivery srcDl(src);
    src->copyAll(*local);

    ScanexDelivery dl(local);
    dl.createKillFile();
    EXPECT_TRUE(local->file("mosaic.kill")->exists());
}

Y_UNIT_TEST(mark_broken)
{
    const auto local = storage::localStorage("./tmp")->dir(this->Name_);
    const auto src = storage::localStorage(DL_PATH);
    ScanexDelivery srcDl(src);
    src->copyAll(*local);

    ScanexDelivery dl(local);
    dl.createBrokenFile();
    EXPECT_TRUE(local->file("mosaic.broken")->exists());
}

Y_UNIT_TEST(check_all)
{
    ScanexDelivery dl(storage::localStorage(DL_PATH));
    dl.check();
}

Y_UNIT_TEST(check_ready)
{
    const auto local = storage::localStorage("./tmp")->dir(this->Name_);
    const auto src = storage::localStorage(DL_PATH);
    ScanexDelivery srcDl(src);
    src->copyAll(*local);

    EXPECT_FALSE(ScanexDelivery::createIfReady(local));
    local->file("mosaic.ready")->touch();
    EXPECT_TRUE(ScanexDelivery::createIfReady(local));
    local->file("mosaic.broken")->touch();
    EXPECT_FALSE(ScanexDelivery::createIfReady(local));
}

Y_UNIT_TEST(get_geometry_sr)
{
    {
        const std::string path = DL_PATH + "/SPOT6_PMS_201708090316140_ORT_2438229101_X1.Y2.mif";
        dataset::VectorDataset ds = dataset::OpenDataset(path);
        EXPECT_EQ(ds.layersCount(), 1);
        auto geoms = ds.layerGeometries(0);
        EXPECT_EQ(geoms.size(), 1u);
        EXPECT_EQ(geoms[0].spatialReference(), geometry::geodeticSr());
        EXPECT_EQ(geoms[0].spatialReference(), ds.layerSpatialReference(0));
        EXPECT_TRUE(geoms[0].isPolygon());
    }
    {
        const std::string path =
            ArcadiaSourceRoot() +
            "/maps/factory/test_data/scanex_deliveries/13174627_geom/DIM_PHR1A_PMS-N_201709221034380_SEN_3004973101.X3.Y4.mif";
        dataset::VectorDataset ds = dataset::OpenDataset(path);
        EXPECT_EQ(ds.layersCount(), 1);
        auto geoms = ds.layerGeometries(0);
        EXPECT_EQ(geoms.size(), 1u);
        EXPECT_TRUE(geoms[0].spatialReference());
        EXPECT_NE(geoms[0].spatialReference(), geometry::geodeticSr());
        EXPECT_EQ(geoms[0].spatialReference(), ds.layerSpatialReference(0));
        EXPECT_TRUE(geoms[0].isMultiPolygon());
    }
    {
        const std::string path =
            ArcadiaSourceRoot() +
            "/maps/factory/test_data/scanex_deliveries/15675831_all_black/DIM_PHR1B_PMS-N_201805110911481_SEN_3229312101-001_R2C1.mif";
        dataset::VectorDataset ds = dataset::OpenDataset(path);
        EXPECT_EQ(ds.layersCount(), 1);
        auto geoms = ds.layerGeometries(0);
        EXPECT_EQ(geoms.size(), 1u);
        EXPECT_TRUE(geoms[0].spatialReference());
        EXPECT_NE(geoms[0].spatialReference(), geometry::geodeticSr());
        EXPECT_EQ(geoms[0].spatialReference(), ds.layerSpatialReference(0));
        EXPECT_TRUE(geoms[0].isPolygon());
    }
}

Y_UNIT_TEST(read_geometry)
{
    ScanexDelivery dl(storage::localStorage(DL_PATH));
    auto geom = dl.contourGeodetic();
    constexpr auto expected =
        "POLYGON ((111.806051333333 60.3197339621776,111.806051333333 60.3205963333334,112.148339247563 60.3205963333334,112.148407918044 60.1362847621838,112.085791258627 60.1364213306885,112.085780240081 60.1364213001296,111.995852791707 60.1357263430325,111.811861842458 60.136141672489,111.81174968018 60.1391700539895,111.810360149537 60.1829402692547,111.810221273008 60.1962724160383,111.810220357746 60.1963074032189,111.809247756006 60.2195109018759,111.808830991711 60.2363203951113,111.808828057921 60.2363781198401,111.808552287263 60.2399631383917,111.807857999477 60.2682900800526,111.807851532046 60.2683856331903,111.807578810148 60.2708401302707,111.806884992837 60.2988703496577,111.806878532046 60.2989656331903,111.806606160948 60.3014169730741,111.806329131665 60.3159610104444,111.806326295076 60.3160219786476,111.806051333333 60.3197339621776))";
    EXPECT_EQ(geom.wkt(), expected);
}

Y_UNIT_TEST(open_without_geom)
{
    const auto local = storage::localStorage("./tmp")->dir(this->Name_);
    const auto src = storage::localStorage(DL_PATH);
    ScanexDelivery srcDl(src);
    src->copy({srcDl.tifFile()->path()}, *local);

    ScanexDelivery dl(local);
    EXPECT_EQ(dl.tifFile()->path().native(), srcDl.tifFile()->path().native());
    EXPECT_FALSE(dl.hasGeometry());
    EXPECT_FALSE(dl.hasMD5());
    EXPECT_THROW((void) dl.contourGeodetic(), RuntimeError);
}

Y_UNIT_TEST(read_metadata)
{
    ScanexDelivery dl(storage::localStorage(DL_PATH));
    const auto meta = dl.metadata();
    std::unordered_map<std::string, std::string> expected{
        {"TIME", "03:16:14.0"},
        {"DATE", "2017-08-09"},
        {"SUN_ELEV", "43.893939000000003"},
        {"PROJECT", "One Atlas"},
        {"ELEV", "15.563884000000000"},
        {"SUN_AZIM", "153.008632000000006"},
        {"AZIM_ANGLE", "-2.495224000000000"},
        {"CLOUDS", "0.000000000000000"},
        {"NAME", "SPOT6_PMS_201708090316140_ORT_2438229101_X1.Y2"},
        {"SOURCE", "SPOT-6"},
        {"STRIP_ID", "DS_SPOT6_201708090316140_FR1_FR1_SV1_SV1_E112N60_01871"},
        {"IMAGE_ID",
            "AS_SP_51742_1_623_SO17016225-94-01_DS_SPOT6_201708090316140_FR1_FR1_SV1_SV1_E112N60_01871"}
    };
    EXPECT_THAT(meta.values(), UnorderedElementsAreArray(expected));
}

Y_UNIT_TEST(parse_metadata)
{
    ScanexDelivery dl(storage::localStorage(DL_PATH));
    const auto meta = dl.metadata();
    EXPECT_EQ(meta.time(), chrono::parseSqlDateTime("2017-08-09 03:16:14"));
    EXPECT_NEAR(meta.elev(), 15.563884, 1e-3);
    EXPECT_NEAR(meta.sunElev(), 43.893939000000003, 1e-3);
    EXPECT_NEAR(meta.sunAzim(), 153.008632000000006, 1e-3);
    EXPECT_NEAR(meta.azimAngle(), -2.495224000000000, 1e-3);
    EXPECT_NEAR(meta.clouds(), 0.0, 1e-3);
    EXPECT_EQ(meta.project(), "One Atlas");
    EXPECT_EQ(meta.name(), "SPOT6_PMS_201708090316140_ORT_2438229101_X1.Y2");
    EXPECT_EQ(meta.source(), "SPOT-6");
    EXPECT_EQ(meta.stripId(), "DS_SPOT6_201708090316140_FR1_FR1_SV1_SV1_E112N60_01871");
    EXPECT_EQ(meta.imageId(),
        "AS_SP_51742_1_623_SO17016225-94-01_DS_SPOT6_201708090316140_FR1_FR1_SV1_SV1_E112N60_01871");
}

Y_UNIT_TEST(parse_time)
{
    {
        ScanexDeliveryMetadata meta;
        meta.add("DATE", "2017-08-09");
        meta.add("TIME", "03:16:14.0");
        EXPECT_EQ(meta.time(), chrono::parseSqlDateTime("2017-08-09 03:16:14"));
    }
    {
        ScanexDeliveryMetadata meta;
        meta.add("DATE", "2021-05-17");
        meta.add("TIME", "08:47");
        EXPECT_EQ(meta.time(), chrono::parseSqlDateTime("2021-05-17 08:47:00"));
    }
}

} // suite

} //namespace maps::factory::delivery::tests
