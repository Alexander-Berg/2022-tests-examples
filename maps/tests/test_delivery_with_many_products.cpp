#include <maps/factory/libs/delivery/dg_delivery.h>

#include <maps/factory/libs/unittest/tests_common.h>
#include <maps/factory/libs/storage/local_storage.h>

namespace maps::factory::delivery::tests {
using namespace maps::factory::tests;

Y_UNIT_TEST_SUITE(acomp_multiple_products_delivery_should) {

const std::string src = ArcadiaSourceRoot() + "/maps/factory/test_data/dg_deliveries/059011233030_01";

Y_UNIT_TEST(parse_delivery_index_file)
{
    DgDelivery dl{src};
    EXPECT_EQ(dl.metadataVersion(), "28.3");
    EXPECT_EQ(dl.orderNumber(), "059011233030_01");
    EXPECT_EQ(dl.areaDescription(), "AOI 3");
    EXPECT_EQ(dl.collectionStartTime(), chrono::parseIsoDateTime("2017-06-06T08:26:50.285179Z"));
    const auto& files = dl.files(DgFiles::Indexed);
    ASSERT_EQ(files.size(), 79u);
    EXPECT_EQ(files.count("GIS_FILES/17OCT20081845-M2AS-059011233030_01_P001_PIXEL_SHAPE.shp"), 1u);
    EXPECT_EQ(files.count("059011233030_01_README.XML"), 1u);
}

Y_UNIT_TEST(get_all_products)
{
    DgDelivery dl{src};
    auto products = dl.products();
    ASSERT_EQ(products.size(), 2u);

    {
        auto&& p = products[0];
        EXPECT_EQ(p.id(), DgProductId(dl.orderNumber(), "P001"));
        EXPECT_THAT(p.descriptors(), ElementsAre("MUL", "PAN"));

        auto pan = p.pan();
        EXPECT_EQ(pan.satelliteId(), SatelliteId::GeoEye01);
        EXPECT_TRUE(pan.hasAtmosphericCompensation());
        EXPECT_THAT(pan.productComponentIndexFile().string(),
            EndsWith("059011233030_01_P001_PAN/17OCT20081845-P2AS-059011233030_01_P001.XML"));
        EXPECT_THAT(pan.pixelShapeFile().string(),
            EndsWith("GIS_FILES/17OCT20081845-P2AS-059011233030_01_P001_PIXEL_SHAPE.shp"));

        auto mul = p.mul();
        EXPECT_EQ(mul.satelliteId(), SatelliteId::GeoEye01);
        EXPECT_TRUE(mul.hasAtmosphericCompensation());
        EXPECT_THAT(mul.productComponentIndexFile().string(),
            EndsWith("059011233030_01_P001_MUL/17OCT20081845-M2AS-059011233030_01_P001.XML"));
        EXPECT_THAT(mul.pixelShapeFile().string(),
            EndsWith("GIS_FILES/17OCT20081845-M2AS-059011233030_01_P001_PIXEL_SHAPE.shp"));
    }
    {
        auto&& p = products[1];
        EXPECT_THAT(p.descriptors(), ElementsAre("MUL", "PAN"));
        EXPECT_EQ(p.id(), DgProductId(dl.orderNumber(), "P002"));

        auto pan = p.pan();
        EXPECT_EQ(pan.satelliteId(), SatelliteId::GeoEye01);
        EXPECT_TRUE(pan.hasAtmosphericCompensation());
        EXPECT_THAT(pan.productComponentIndexFile().string(),
            EndsWith("059011233030_01_P002_PAN/17JUN06082657-P2AS-059011233030_01_P002.XML"));
        EXPECT_THAT(pan.pixelShapeFile().string(),
            EndsWith("GIS_FILES/17JUN06082657-P2AS-059011233030_01_P002_PIXEL_SHAPE.shp"));

        auto mul = p.mul();
        EXPECT_EQ(mul.satelliteId(), SatelliteId::GeoEye01);
        EXPECT_TRUE(mul.hasAtmosphericCompensation());
        EXPECT_THAT(mul.productComponentIndexFile().string(),
            EndsWith("059011233030_01_P002_MUL/17JUN06082657-M2AS-059011233030_01_P002.XML"));
        EXPECT_THAT(mul.pixelShapeFile().string(),
            EndsWith("GIS_FILES/17JUN06082657-M2AS-059011233030_01_P002_PIXEL_SHAPE.shp"));
    }
}

Y_UNIT_TEST(get_extra_files)
{
    EXPECT_THAT(toStrings(DgDelivery{src}.files(DgFiles::Extra)), ElementsAre("EXTRA.TXT"));
}

Y_UNIT_TEST(get_ftp_files)
{
    EXPECT_THAT(toStrings(DgDelivery{src}.files(DgFiles::FtpSpecific)),
        ElementsAre("../059011233030_01.MAN", "../059011233030_01_EOT.TXT"));
}

Y_UNIT_TEST(copy_one_of_many_products)
{
    auto dst1 = storage::localStorage("./tmp")->dir(Name_)->dir("p1");
    auto dst2 = storage::localStorage("./tmp")->dir(Name_)->dir("p2");
    DgDelivery dl{src};
    auto dstDl1 = dl.copyProductTo(dl.products().at(0), dst1);
    auto dstDl2 = dl.copyProductTo(dl.products().at(1), dst2);

    auto files1 = toStrings(dst1->list(storage::Select::FilesRecursive));
    EXPECT_THAT(files1, UnorderedElementsAreArray(toStrings(dstDl1.files(DgFiles::Indexed))));
    EXPECT_THAT(files1, SizeIs(49u));

    auto files2 = toStrings(dst2->list(storage::Select::FilesRecursive));
    EXPECT_THAT(files2, UnorderedElementsAreArray(toStrings(dstDl2.files(DgFiles::Indexed))));
    EXPECT_THAT(files2, SizeIs(49u));
    EXPECT_THAT(files2, Not(UnorderedElementsAreArray(files1)));

    EXPECT_FALSE(dl.isEqual(dstDl1));
    EXPECT_FALSE(dl.isEqual(dstDl2));
    EXPECT_FALSE(dstDl1.isEqual(dstDl2));

    std::vector<std::string> common{
        "GIS_FILES/059011233030_01_PRODUCT_SHAPE.dbf",
        "GIS_FILES/059011233030_01_PRODUCT_SHAPE.shp",
        "GIS_FILES/059011233030_01_PRODUCT_SHAPE.shx",
        "GIS_FILES/059011233030_01_PRODUCT_SHAPE.prj",
        "GIS_FILES/059011233030_01_STRIP_SHAPE.dbf",
        "GIS_FILES/059011233030_01_STRIP_SHAPE.shp",
        "GIS_FILES/059011233030_01_STRIP_SHAPE.shx",
        "GIS_FILES/059011233030_01_STRIP_SHAPE.prj",
        "GIS_FILES/059011233030_01_TILE_SHAPE.dbf",
        "GIS_FILES/059011233030_01_TILE_SHAPE.shp",
        "GIS_FILES/059011233030_01_TILE_SHAPE.shx",
        "GIS_FILES/059011233030_01_TILE_SHAPE.prj",
        "GIS_FILES/059011233030_01_ORDER_SHAPE.dbf",
        "GIS_FILES/059011233030_01_ORDER_SHAPE.shp",
        "GIS_FILES/059011233030_01_ORDER_SHAPE.shx",
        "GIS_FILES/059011233030_01_ORDER_SHAPE.prj",
        "059011233030_01_LAYOUT.JPG",
        "059011233030_01_README.TXT",
        "059011233030_01_README.XML",
    };

    std::vector<std::string> exp1{
        "GIS_FILES/17OCT20081845-M2AS-059011233030_01_P001_PIXEL_SHAPE.shp",
        "GIS_FILES/17OCT20081845-M2AS-059011233030_01_P001_PIXEL_SHAPE.shx",
        "GIS_FILES/17OCT20081845-M2AS-059011233030_01_P001_PIXEL_SHAPE.dbf",
        "GIS_FILES/17OCT20081845-P2AS-059011233030_01_P001_PIXEL_SHAPE.shp",
        "GIS_FILES/17OCT20081845-P2AS-059011233030_01_P001_PIXEL_SHAPE.shx",
        "GIS_FILES/17OCT20081845-P2AS-059011233030_01_P001_PIXEL_SHAPE.dbf",
        "GIS_FILES/17OCT20081845-M2AS-059011233030_01_P001_PIXEL_SHAPE.prj",
        "GIS_FILES/17OCT20081845-P2AS-059011233030_01_P001_PIXEL_SHAPE.prj",
        "059011233030_01_P001_MUL/17OCT20081845-M2AS-059011233030_01_P001-BROWSE.JPG",
        "059011233030_01_P001_MUL/17OCT20081845-M2AS-059011233030_01_P001.IMD",
        "059011233030_01_P001_MUL/17OCT20081845-M2AS-059011233030_01_P001.RPB",
        "059011233030_01_P001_MUL/17OCT20081845-M2AS-059011233030_01_P001.TIL",
        "059011233030_01_P001_MUL/17OCT20081845-M2AS-059011233030_01_P001.XML",
        "059011233030_01_P001_MUL/17OCT20081845-M2AS-059011233030_01_P001_README.TXT",
        "059011233030_01_P001_MUL/INTERNAL.TXT",
        "059011233030_01_P001_MUL/17OCT20081845-M2AS_R1C1-059011233030_01_P001.TIF",
        "059011233030_01_P001_MUL/17OCT20081845-M2AS_R1C2-059011233030_01_P001.TIF",
        "059011233030_01_P001_MUL/17OCT20081845-M2AS_R2C1-059011233030_01_P001.TIF",
        "059011233030_01_P001_MUL/17OCT20081845-M2AS_R2C2-059011233030_01_P001.TIF",
        "059011233030_01_P001_PAN/17OCT20081845-P2AS-059011233030_01_P001-BROWSE.JPG",
        "059011233030_01_P001_PAN/17OCT20081845-P2AS-059011233030_01_P001.IMD",
        "059011233030_01_P001_PAN/17OCT20081845-P2AS-059011233030_01_P001.RPB",
        "059011233030_01_P001_PAN/17OCT20081845-P2AS-059011233030_01_P001.TIL",
        "059011233030_01_P001_PAN/17OCT20081845-P2AS-059011233030_01_P001.XML",
        "059011233030_01_P001_PAN/17OCT20081845-P2AS-059011233030_01_P001_README.TXT",
        "059011233030_01_P001_PAN/INTERNAL.TXT",
        "059011233030_01_P001_PAN/17OCT20081845-P2AS_R1C1-059011233030_01_P001.TIF",
        "059011233030_01_P001_PAN/17OCT20081845-P2AS_R1C2-059011233030_01_P001.TIF",
        "059011233030_01_P001_PAN/17OCT20081845-P2AS_R2C1-059011233030_01_P001.TIF",
        "059011233030_01_P001_PAN/17OCT20081845-P2AS_R2C2-059011233030_01_P001.TIF",
    };

    std::vector<std::string> exp2{
        "GIS_FILES/17JUN06082657-M2AS-059011233030_01_P002_PIXEL_SHAPE.shp",
        "GIS_FILES/17JUN06082657-M2AS-059011233030_01_P002_PIXEL_SHAPE.shx",
        "GIS_FILES/17JUN06082657-M2AS-059011233030_01_P002_PIXEL_SHAPE.dbf",
        "GIS_FILES/17JUN06082657-P2AS-059011233030_01_P002_PIXEL_SHAPE.shp",
        "GIS_FILES/17JUN06082657-P2AS-059011233030_01_P002_PIXEL_SHAPE.shx",
        "GIS_FILES/17JUN06082657-P2AS-059011233030_01_P002_PIXEL_SHAPE.dbf",
        "GIS_FILES/17JUN06082657-M2AS-059011233030_01_P002_PIXEL_SHAPE.prj",
        "GIS_FILES/17JUN06082657-P2AS-059011233030_01_P002_PIXEL_SHAPE.prj",
        "059011233030_01_P002_MUL/17JUN06082657-M2AS-059011233030_01_P002-BROWSE.JPG",
        "059011233030_01_P002_MUL/17JUN06082657-M2AS-059011233030_01_P002.IMD",
        "059011233030_01_P002_MUL/17JUN06082657-M2AS-059011233030_01_P002.RPB",
        "059011233030_01_P002_MUL/17JUN06082657-M2AS-059011233030_01_P002.TIL",
        "059011233030_01_P002_MUL/17JUN06082657-M2AS-059011233030_01_P002.XML",
        "059011233030_01_P002_MUL/17JUN06082657-M2AS-059011233030_01_P002_README.TXT",
        "059011233030_01_P002_MUL/INTERNAL.TXT",
        "059011233030_01_P002_MUL/17JUN06082657-M2AS_R1C1-059011233030_01_P002.TIF",
        "059011233030_01_P002_MUL/17JUN06082657-M2AS_R1C2-059011233030_01_P002.TIF",
        "059011233030_01_P002_MUL/17JUN06082657-M2AS_R2C1-059011233030_01_P002.TIF",
        "059011233030_01_P002_MUL/17JUN06082657-M2AS_R2C2-059011233030_01_P002.TIF",
        "059011233030_01_P002_PAN/17JUN06082657-P2AS-059011233030_01_P002-BROWSE.JPG",
        "059011233030_01_P002_PAN/17JUN06082657-P2AS-059011233030_01_P002.IMD",
        "059011233030_01_P002_PAN/17JUN06082657-P2AS-059011233030_01_P002.RPB",
        "059011233030_01_P002_PAN/17JUN06082657-P2AS-059011233030_01_P002.TIL",
        "059011233030_01_P002_PAN/17JUN06082657-P2AS-059011233030_01_P002.XML",
        "059011233030_01_P002_PAN/17JUN06082657-P2AS-059011233030_01_P002_README.TXT",
        "059011233030_01_P002_PAN/INTERNAL.TXT",
        "059011233030_01_P002_PAN/17JUN06082657-P2AS_R1C1-059011233030_01_P002.TIF",
        "059011233030_01_P002_PAN/17JUN06082657-P2AS_R1C2-059011233030_01_P002.TIF",
        "059011233030_01_P002_PAN/17JUN06082657-P2AS_R2C1-059011233030_01_P002.TIF",
        "059011233030_01_P002_PAN/17JUN06082657-P2AS_R2C2-059011233030_01_P002.TIF",
    };

    std::vector<std::string> all = common;
    all.insert(all.end(), exp1.begin(), exp1.end());
    all.insert(all.end(), exp2.begin(), exp2.end());

    exp1.insert(exp1.end(), common.begin(), common.end());
    exp2.insert(exp2.end(), common.begin(), common.end());

    EXPECT_THAT(files1, UnorderedElementsAreArray(exp1));
    EXPECT_THAT(files2, UnorderedElementsAreArray(exp2));
    EXPECT_THAT(toStrings(dl.files(DgFiles::Indexed)), UnorderedElementsAreArray(all));
}

} // Y_UNIT_TEST_SUITE

} //namespace maps::factory::delivery::tests
