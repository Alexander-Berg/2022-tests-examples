#include <maps/factory/libs/dataset/polygonize.h>

#include <maps/factory/libs/dataset/create_raster_dataset.h>

#include <maps/factory/libs/unittest/tests_common.h>

namespace maps::factory::dataset::tests {
using namespace maps::factory::tests;

Y_UNIT_TEST_SUITE(polygonize_should) {

constexpr auto boxWkt =
    "MULTIPOLYGON (((1.5 1.5,1.5 98.5,8.5 98.5,8.5 1.5,1.5 1.5)))";

constexpr auto panWkt =
    "MULTIPOLYGON ((("
    "419867.919388805 6189040.20605398,420538.653451034 6189040.20605398,420306.596721253 6169569.78379046,"
    "405733.69869529 6169107.68238575,406139.744503893 6188633.48115807,419867.919388805 6189040.20605398"
    ")))";

constexpr auto mulWkt =
    "MULTIPOLYGON ((("
    "7689223.96357595 7296577.75803337,7708333.2206472 7296577.75803337,7708333.2206472 7287593.27551315,"
    "7707524.84282068 7287708.7580598,7707342.16831395 7287654.98563343,7707263.10225121 7287481.75166745,"
    "7707263.10225121 7284806.45567748,7706116.54682693 7284806.45567748,7705954.39940383 7284739.29201572,"
    "7705887.23574208 7284577.14459262,7705887.23574208 7281901.84860265,7704740.6803178 7281901.84860265,"
    "7704578.5328947 7281834.6849409,7704511.36923295 7281672.53751779,7704511.36923295 7280525.98209352,"
    "7701836.07324297 7280525.98209352,7701673.92581987 7280458.81843176,7701606.76215812 7280296.67100866,"
    "7701606.76215812 7279150.11558439,7698931.46616814 7279150.11558439,7698773.62537009 7279087.1470861,"
    "7698702.47029006 7278932.82371167,7698744.96942921 7278807.58486511,7689223.96357595 7278694.23955734,"
    "7689223.96357595 7296577.75803337"
    ")))";

Y_UNIT_TEST(get_empty_boundary_for_black_image)
{
    TDataset ds = CreateInMemory()
        .setSize({100, 500}).setBands(1).setType(TDataType::Byte);
    ds.setIdentityGeoTransform();
    ds.fillAll(0);
    Geometry geom = polygonizeZeroBoundary(ds.ref());
    EXPECT_TRUE(geom.isEmpty());
}

Y_UNIT_TEST(get_rectangle_for_white_image)
{
    TDataset ds = CreateInMemory()
        .setSize({10, 100}).setBands(1).setType(TDataType::Byte);
    ds.setIdentityGeoTransform();
    ds.fillAll(255);
    Geometry geom = polygonizeZeroBoundary(ds.ref());
    EXPECT_GE(geom.similarity(*Geometry::fromWkt(boxWkt)), 0.9999);
}

Y_UNIT_TEST(get_grayscale_image_boundary)
{
    const auto path = ArcadiaSourceRoot() + "/maps/factory/test_data/geotif_corpus/moscow_32637.tif";
    TDataset ds = OpenDataset(path);
    Geometry geom = polygonizeZeroBoundary(ds.ref());
    EXPECT_GE(geom.similarity(*Geometry::fromWkt(panWkt)), 0.9999);
}

Y_UNIT_TEST(get_multiband_image_boundary)
{
    const auto path = ArcadiaSourceRoot() + "/maps/factory/test_data/geotif_corpus/ikonos_3395.tif";
    TDataset ds = OpenDataset(path);
    Geometry geom = polygonizeZeroBoundary(ds.ref());
    EXPECT_GE(geom.similarity(*Geometry::fromWkt(mulWkt)), 0.9999);
}

Y_UNIT_TEST(save_projection)
{
    const auto path = ArcadiaSourceRoot() + "/maps/factory/test_data/geotif_corpus/ikonos_3395.tif";
    TDataset ds = OpenDataset(path);
    Geometry geom = polygonizeZeroBoundary(ds.ref());
    EXPECT_EQ(geom.spatialReference(), ds.projection());
}

} // suite
} //namespace maps::factory::dataset::tests
