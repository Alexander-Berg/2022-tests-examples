#include <maps/factory/libs/dataset/create_raster_dataset.h>
#include <maps/factory/libs/image/image.h>
#include <maps/factory/libs/dataset/memory_file.h>
#include <maps/factory/libs/dataset/rpc_transformation.h>
#include <maps/factory/libs/dataset/fill_nodata.h>

#include <maps/factory/libs/unittest/tests_common.h>

#include <maps/libs/common/include/file_utils.h>

#include <util/system/env.h>

#include <boost/filesystem/operations.hpp>

#include <contrib/libs/gdal/gcore/gdal.h>
#include <contrib/libs/gdal/port/cpl_string.h>

namespace maps::factory::dataset::tests {
using namespace maps::factory::tests;
namespace fs = boost::filesystem;

namespace {
const int fix = fixGdalDataFolderForTests();

const std::string MOSCOW_RPC_META[] = {
    "HEIGHT_OFF=180",
    "HEIGHT_SCALE=500",
    "LAT_OFF=55.7493",
    "LAT_SCALE=0.0967",
    "LINE_DEN_COEFF=1 -1.084474e-05 0.0008198225 -2.285952e-06 -4.271315e-08 3.513939e-07 1.628854e-08 3.041846e-07 5.918966e-07 -3.922506e-07 1.380131e-08 0 0 0 0 2.552477e-08 0 0 -1.373797e-07 0",
    "LINE_NUM_COEFF=0.001647906 0.01412227 -1.0227 -0.008543032 3.975077e-06 -2.220698e-05 -1.634952e-05 -0.0006056075 -0.0008460824 1.221767e-07 2.697713e-07 1.954889e-08 -4.314291e-08 -2.789951e-08 0 -6.104538e-07 4.243625e-07 -1.631958e-07 5.23632e-08 0",
    "LINE_OFF=172.128288001591",
    "LINE_SCALE=179.979513698971",
    "LONG_OFF=37.6165",
    "LONG_SCALE=0.1186",
    "SAMP_DEN_COEFF=1 -0.001621713 0.001208017 -0.000462364 1.020493e-07 -1.848817e-07 8.078164e-07 1.999287e-06 1.758746e-06 -8.627872e-07 0 0 0 0 0 0 0 0 0 0",
    "SAMP_NUM_COEFF=0.003263633 0.9929979 0.0286472 0.02036295 -0.001303936 0.000410203 -0.000401439 -0.001601264 5.128982e-05 3.735305e-06 7.502895e-07 1.710501e-06 -2.674206e-06 -5.502481e-07 4.208422e-06 6.029727e-08 -1.587999e-07 -1.339433e-06 -2.897306e-07 -1.495995e-08",
    "SAMP_OFF=128.064070477525",
    "SAMP_SCALE=128.081155938199",
};

const auto IKONOS_WKT =
    "PROJCS[\"unnamed\",GEOGCS[\"WGS 84\","
    "DATUM[\"WGS_1984\",SPHEROID[\"WGS 84\",6378137,298.257223563,"
    "AUTHORITY[\"EPSG\",\"7030\"]],"
    "AUTHORITY[\"EPSG\",\"6326\"]],"
    "PRIMEM[\"Greenwich\",0],"
    "UNIT[\"degree\",0.0174532925199433,"
    "AUTHORITY[\"EPSG\",\"9122\"]],"
    "AUTHORITY[\"EPSG\",\"4326\"]],"
    "PROJECTION[\"Mercator_1SP\"],"
    "PARAMETER[\"central_meridian\",0],"
    "PARAMETER[\"scale_factor\",1],"
    "PARAMETER[\"false_easting\",0],"
    "PARAMETER[\"false_northing\",0],"
    "UNIT[\"metre\",1,"
    "AUTHORITY[\"EPSG\",\"9001\"]],"
    "AXIS[\"Easting\",EAST],"
    "AXIS[\"Northing\",NORTH]]";

const auto MOSCOW_WKT =
    "PROJCS[\"WGS 84 / UTM zone 37N\","
    "GEOGCS[\"WGS 84\","
    "DATUM[\"WGS_1984\","
    "SPHEROID[\"WGS 84\",6378137,298.257223563,"
    "AUTHORITY[\"EPSG\",\"7030\"]],"
    "AUTHORITY[\"EPSG\",\"6326\"]],"
    "PRIMEM[\"Greenwich\",0,"
    "AUTHORITY[\"EPSG\",\"8901\"]],"
    "UNIT[\"degree\",0.0174532925199433,"
    "AUTHORITY[\"EPSG\",\"9122\"]],"
    "AUTHORITY[\"EPSG\",\"4326\"]],"
    "PROJECTION[\"Transverse_Mercator\"],"
    "PARAMETER[\"latitude_of_origin\",0],"
    "PARAMETER[\"central_meridian\",39],"
    "PARAMETER[\"scale_factor\",0.9996],"
    "PARAMETER[\"false_easting\",500000],"
    "PARAMETER[\"false_northing\",0],"
    "UNIT[\"metre\",1,"
    "AUTHORITY[\"EPSG\",\"9001\"]],A"
    "XIS[\"Easting\",EAST],"
    "AXIS[\"Northing\",NORTH],"
    "AUTHORITY[\"EPSG\",\"32637\"]]";

} // namespace

Y_UNIT_TEST_SUITE(dataset_should) {

std::string getTmpTif(TCurrentTestCase* ctc)
{
    const std::string tmp = "./tmp/";
    fs::create_directory(tmp);
    return tmp + ctc->Name_ + ".tif";
}

Y_UNIT_TEST(convert_path_to_virtual_fs)
{
    EXPECT_EQ(VsiPath("/my/file.tif"), "/my/file.tif");
    EXPECT_EQ(VsiPath("file.tif"), "file.tif");
    EXPECT_EQ(VsiPath("http://cloud/file.tif"), "/vsicurl/http://cloud/file.tif");
    EXPECT_EQ(VsiPath("https://cloud/file.tif"), "/vsicurl/https://cloud/file.tif");
    EXPECT_EQ(VsiPath("ftp://cloud/file.tif"), "/vsicurl/ftp://cloud/file.tif");
    EXPECT_EQ(VsiPath("s3://cloud/file.tif"), "/vsicurl/https://cloud/file.tif");
}

Y_UNIT_TEST(read_size)
{
    EXPECT_THAT(OpenDataset(IKONOS_PATH).get().size(), EigEq(Array2i{128, 120}));
    EXPECT_THAT(OpenDataset(SRTM_PATH).get().size(), EigEq(Array2i{1201, 1201}));
}

Y_UNIT_TEST(read_type)
{
    EXPECT_EQ(OpenDataset(IKONOS_PATH).get().type(), TDataType::Byte);
    EXPECT_EQ(OpenDataset(SRTM_PATH).get().type(), TDataType::Int16);
}

Y_UNIT_TEST(read_interleave)
{
    EXPECT_EQ(OpenDataset(IKONOS_PATH).get().Interleave(), EInterleave::Pixel);
    EXPECT_EQ(OpenDataset(SRTM_PATH).get().Interleave(), EInterleave::Band);
    EXPECT_EQ(OpenDataset(MOSCOW_PATH).get().Interleave(), EInterleave::Band);
    EXPECT_EQ(OpenDataset{WV_PATH}.get().Interleave(), EInterleave::Band);
}

Y_UNIT_TEST(read_bands_count)
{
    EXPECT_EQ(OpenDataset(IKONOS_PATH).get().bands(), 3);
    EXPECT_EQ(OpenDataset(SRTM_PATH).get().bands(), 1);
}

Y_UNIT_TEST(read_geo_transform)
{
    TDataset ds = OpenDataset(IKONOS_PATH);
    EXPECT_THAT(ds.Site().Origin(), EigEq(Vector2d(7688994.652491, 7296807.069118), 1e-5));
    EXPECT_THAT(ds.Site().PixelSize(), EigEq(Vector2d(152.8740565, -152.874056), 1e-5));
}

Y_UNIT_TEST(apply_geo_transform)
{
    TDataset ds = OpenDataset(IKONOS_PATH);
    const Affine2d tr = ds.Site().PixToProj();

    const Vector2d topLeftPix = Vector2d::Zero();
    const Vector2d bottomRightPix = ds.size().cast<double>();
    const Vector2d topLeftCoord = tr * topLeftPix;
    const Vector2d bottomRightCoord = tr * bottomRightPix;

    EXPECT_THAT(topLeftCoord, EigEq(Vector2d(7688994.6, 7296807), 0.1));
    EXPECT_THAT(bottomRightCoord, EigEq(Vector2d(7708562.5, 7278462.2), 0.1));
    EXPECT_THAT(geoTransform(geoTransform(tr)).matrix(), EigEq(tr.matrix()));
}

Y_UNIT_TEST(read_projection)
{
    EXPECT_EQ(OpenDataset(IKONOS_PATH).get().projection(),
        SpatialRef(IKONOS_WKT));
    EXPECT_EQ(OpenDataset(MOSCOW_PATH).get().projection(),
        SpatialRef(MOSCOW_WKT)
    );

    EXPECT_EQ(OpenDataset(SRTM_PATH).get().projection(), geodeticSr());
}

Y_UNIT_TEST(read_projection_name)
{
    EXPECT_EQ(OpenDataset(SRTM_PATH).get().projection().name(), "WGS 84");
    EXPECT_EQ(OpenDataset{WV_PATH}.get().projection().name(), "WGS 84 / UTM zone 44N");
    EXPECT_EQ(OpenDataset(IKONOS_PATH).get().projection().name(), "unnamed");
    EXPECT_EQ(OpenDataset(MOSCOW_PATH).get().projection().name(), "WGS 84 / UTM zone 37N");
}

Y_UNIT_TEST(read_nodata_value)
{
    EXPECT_EQ(OpenDataset(SRTM_PATH).get().Nodata(), DEM_NODATA_VALUE);
    EXPECT_EQ(OpenDataset{WV_PATH}.get().Nodata(), std::nullopt);
    EXPECT_EQ(OpenDataset(IKONOS_PATH).get().Nodata(), std::nullopt);
}

Y_UNIT_TEST(read_domains)
{
    TDataset ds = OpenDataset(MOSCOW_PATH);
    const auto domains = ds.Domains();
    const std::string expected[] = {
        "", "RPC", "IMAGE_STRUCTURE", "DERIVED_SUBDATASETS",
    };
    EXPECT_THAT(domains, UnorderedElementsAreArray(expected));
}

Y_UNIT_TEST(read_rpc_coeffs)
{
    TDataset ds = OpenDataset(MOSCOW_PATH);
    EXPECT_THAT(ds.MetadataItems("RPC"), UnorderedElementsAreArray(MOSCOW_RPC_META));
    EXPECT_EQ(ds.Rpc(), TRpc::fromGdalMetadata(MOSCOW_RPC_META));
}

Y_UNIT_TEST(read_block_size)
{
    EXPECT_THAT(OpenDataset(MOSCOW_PATH).get().BlockSize(), EigEq(Array2i(256, 16)));
}

Y_UNIT_TEST(get_initial_file_path)
{
    EXPECT_THAT(OpenDataset(MOSCOW_PATH).get().Description(), MOSCOW_PATH);
}

Y_UNIT_TEST(reopen)
{
    TDataset ds = OpenDataset(MOSCOW_PATH);
    TDataset reopenedDs = ds.reopen();
    EXPECT_EQ(reopenedDs.Description(), MOSCOW_PATH);
    EXPECT_EQ(reopenedDs.projection(), ds.projection());
    EXPECT_THAT(reopenedDs.Site().PixToProj().matrix(), EigEq(ds.Site().PixToProj().matrix()));
    EXPECT_THAT(reopenedDs.size(), EigEq(ds.size()));
}

Y_UNIT_TEST(copy)
{
    const auto path = getTmpTif(this);
    TDataset ds = OpenDataset(MOSCOW_PATH);
    TDataset copiedDs = CreateTiff(path).copyFrom(ds);
    EXPECT_EQ(copiedDs.Description(), path);
    EXPECT_EQ(copiedDs.projection(), ds.projection());
    EXPECT_THAT(copiedDs.Site().PixToProj().matrix(), EigEq(ds.Site().PixToProj().matrix()));
    EXPECT_THAT(copiedDs.size(), EigEq(ds.size()));
}

Y_UNIT_TEST(read_data_from_the_corner)
{
    TDataset ds = OpenDataset(SRTM_PATH);
    UInt16Image img(Size2i(4, 2), 1);

    ds.Read(img, Box2d(Point2d(0, 0), Point2d(4, 2)));

    UInt16Image expected(img.size(), 1);
    expected.band(0) <<
        230, 228, 224, 223,
        236, 235, 230, 226;
    EXPECT_EQ(img, expected);
}

Y_UNIT_TEST(read_pixel_from_the_corner)
{
    TDataset ds = OpenDataset(SRTM_PATH);
    const Vector2d size = ds.Dim().Size().cast<double>();
    UInt16Image img(Size2i(1, 1), 1);

    ds.Read(img, Box2d(size, size));

    UInt16Image expected(img.size(), 1);
    expected.band(0) <<
        183;
    EXPECT_EQ(img, expected);
}

Y_UNIT_TEST(read_right_corner_with_subpixel_shift)
{
    TDataset ds = OpenDataset(SRTM_PATH);
    UInt16Image img(Size2i(2, 2), 1);

    ds.Read(img, Box2d(Point2d(ds.size().x() - 2.5, 0), Point2d(1201, 2)));

    UInt16Image expected(Size2i(2, 2), 1);
    expected.band(0) <<
        177, 179,
        177, 175;
    EXPECT_EQ(img, expected);
}

Y_UNIT_TEST(read_data)
{
    TDataset ds = OpenDataset(SRTM_PATH);
    UInt16Image img(Size2i(3, 4), 1);

    ds.Read(img, Box2d(Point2d(5, 6), Point2d(8, 10)));

    UInt16Image expected(img.size(), 1);
    expected.band(0) <<
        238, 235, 231,
        236, 235, 231,
        236, 236, 235,
        236, 235, 234;
    EXPECT_EQ(img, expected);
}

Y_UNIT_TEST(read_upsampled_data)
{
    TDataset ds = OpenDataset(SRTM_PATH);
    UInt16Image img(Size2i(4, 4), 1);

    ds.Read(img, Box2d(Point2d(2, 2), Point2d(4, 4)), ResampleAlg::Bilinear);

    UInt16Image expected(img.size(), 1);
    expected.band(0) <<
        233, 231, 229, 227,
        235, 234, 231, 229,
        239, 237, 234, 231,
        241, 240, 237, 234;
    EXPECT_EQ(img, expected);
}

Y_UNIT_TEST(read_downsampled_data)
{
    TDataset ds = OpenDataset(SRTM_PATH);
    UInt16Image img(Size2i(2, 2), 1);

    ds.Read(img, Box2d(Point2d(0, 0), Point2d(4, 4)), ResampleAlg::Average);

    UInt16Image expected(img.size(), 1);
    expected.band(0) <<
        232, 226,
        234, 234;
    EXPECT_EQ(img, expected);
}

Y_UNIT_TEST(read_all_bands)
{
    TDataset ds = OpenDataset(IKONOS_PATH);
    UInt8Image img(Size2i(4, 2), ds.bands());

    ds.Read(img, Box2d(Point2d(0, 0), Point2d(4, 2)));

    UInt8Image expected(img.size(), 3);
    expected.band(0) <<
        138, 138, 136, 136,
        118, 122, 101, 68;
    expected.band(1) <<
        126, 128, 123, 125,
        109, 112, 95, 69;
    expected.band(2) <<
        114, 118, 113, 115,
        97, 101, 86, 62;
    EXPECT_EQ(img, expected);
}

Y_UNIT_TEST(create_tiff)
{
    const auto path = getTmpTif(this);
    TDataset srtm = OpenDataset(SRTM_PATH);
    {
        TDataset ds = CreateTiff(path)
            .setSize({50, 100})
            .setType(TDataType::UInt16)
            .setBands(2)
            .setBlockSize({256, 128})
            .setOverwrite(true);
        ds.copyInfoFrom(srtm);
        ds.fillAll(10);
        const Array2i block(10, 10);
        UInt16Image img(block, 1);
        img.setConstant(20);
        ds.Write(img, Box2d(Point2d(5, 10), Point2d(15, 20)), ResampleAlg::NearestNeighbour, 0);
        img.setConstant(30);
        ds.Write(img, Box2d(Point2d(10, 10), Point2d(20, 20)), ResampleAlg::NearestNeighbour, 1);
        EXPECT_EQ(ds.Description(), path);
        ds.flush();
    }
    {
        TDataset ds = OpenDataset(path);
        EXPECT_THAT(ds.Site().PixToProj().matrix(), EigEq(srtm.Site().PixToProj().matrix()));
        EXPECT_THAT(ds.size(), EigEq(Array2i(50, 100)));
        EXPECT_THAT(ds.BlockSize(), EigEq(Array2i(256, 128)));

        const Array2i block(10, 10);
        UInt16Image img(block, 1);
        UInt16Image expected(block, 1);

        ds.Read(img, Box2d(Point2d(5, 10), Point2d(15, 20)), ResampleAlg::NearestNeighbour, 0);
        expected.setConstant(20);
        EXPECT_EQ(img, expected);

        ds.Read(img, Box2d(Point2d(10, 10), Point2d(20, 20)), ResampleAlg::NearestNeighbour, 1);
        expected.setConstant(30);
        EXPECT_EQ(img, expected);

        ds.Read(img, Box2d(Point2d(20, 20), Point2d(30, 30)), ResampleAlg::NearestNeighbour, 0);
        expected.setConstant(10);
        EXPECT_EQ(img, expected);
    }
}

Y_UNIT_TEST(read_write_tiff)
{
    const auto path = getTmpTif(this);
    const Array2i size(5, 4);
    const int bands = 2;
    TDataset ds = CreateTiff(path).setSize(size).setBands(bands).setType(TDataType::UInt16);

    UInt16Image expected = UInt16Image::random(size, bands);
    ds.Write(expected);

    UInt16Image img = ds.Read<uint16_t>();
    EXPECT_EQ(img, expected);
}

Y_UNIT_TEST(read_write_in_memory)
{
    const Array2i size(5, 4);
    const int bands = 2;
    TDataset ds = CreateInMemory().setSize(size).setBands(bands).setType(TDataType::UInt16);

    UInt16Image expected = UInt16Image::random(size, bands);
    ds.Write(expected);

    UInt16Image img = ds.Read<uint16_t>();
    EXPECT_EQ(img, expected);
}

Y_UNIT_TEST(remove_all_related_files)
{
    const auto path = getTmpTif(this);
    CreateTiff(path).setSize({300, 300}).get()
                    .fillAll(10);
    OpenDataset(path).get().buildOverviews({.block = 32});
    ASSERT_TRUE(fs::exists(path));
    ASSERT_TRUE(fs::exists(path + ".ovr"));
    OpenDataset(path).get().remove();
    EXPECT_FALSE(fs::exists(path));
    EXPECT_FALSE(fs::exists(path + ".ovr"));
}

Y_UNIT_TEST(get_bounds)
{
    {
        TDataset ds = OpenDataset(SRTM_PATH);
        EXPECT_THAT(ds.Site().ProjBounds().min(), EigEq(Vector2d(37, 55), 1e-3));
        EXPECT_THAT(ds.Site().ProjBounds().max(), EigEq(Vector2d(38, 56), 1e-3));
    }
    {
        TDataset ds = OpenDataset(MOSCOW_PATH);
        EXPECT_THAT(ds.Site().ProjBounds().min(), EigEq(Vector2d(405643.999, 6169016.999), 1e-3));
        EXPECT_THAT(ds.Site().ProjBounds().max(), EigEq(Vector2d(420627.499, 6189127.999), 1e-3));
    }
}

Y_UNIT_TEST(get_pixel_size_in_other_cs)
{
    {
        TDataset ds = OpenDataset(SRTM_PATH);
        EXPECT_THAT(ds.Site().PixelSizeIn(4326), EigEq(ds.Site().PixelSize(), 1e-12));
        EXPECT_THAT(ds.Site().PixelSizeIn(geodeticSr()), EigEq(ds.Site().PixelSize(), 1e-12));
        EXPECT_THAT(ds.Site().PixelSizeIn(mercatorSr()), EigEq(Vector2d(92.7662, -163.4253), 1e-3));
        EXPECT_THAT(ds.Site().PixelSizeIn(3395), EigEq(Vector2d(92.7662, -163.4253), 1e-3));
        EXPECT_THAT(ds.Site().PixelSizeIn(32637), EigEq(Vector2d(50.6357, -93.8643), 1e-4));
    }
    {
        TDataset ds = OpenDataset(MOSCOW_PATH);
        EXPECT_THAT(ds.Site().PixelSizeIn(4326), EigEq(Vector2d(0.00095074, -0.00051465), 1e-3));
        EXPECT_THAT(ds.Site().PixelSizeIn(geodeticSr()), EigEq(Vector2d(0.00095074, -0.00051465), 1e-3));
        EXPECT_THAT(ds.Site().PixelSizeIn(3395), EigEq(Vector2d(105.835, -101.576), 1e-3));
        EXPECT_THAT(ds.Site().PixelSizeIn(mercatorSr()), EigEq(Vector2d(105.835, -101.576), 1e-3));
        EXPECT_THAT(ds.Site().PixelSizeIn(32637), EigEq(ds.Site().PixelSize(), 1e-8));
    }
}

Y_UNIT_TEST(get_driver_name_from_extension)
{
    EXPECT_EQ(TDriver::FromPath("foo.tif").Name(), "GTiff");
    EXPECT_EQ(TDriver::FromPath("foo.shp").Name(), "ESRI Shapefile");
    EXPECT_EQ(TDriver::FromPath("foo.geojson").Name(), "GeoJSON");
    EXPECT_EQ(TDriver::FromPath("all.vrt").Name(), "VRT");
    EXPECT_EQ(TDriver::FromPath("/vsicurl/http://s3.mds.yandex.net/foo-bar/all.vrt").Name(), "VRT");
    EXPECT_EQ(TDriver::FromPath("/vsicurl/http://s3.mds.yandex.net/foo-bar/tile.tif").Name(), "GTiff");
    EXPECT_THROW(TDriver::FromPath("foo.bar"), RuntimeError);
}

Y_UNIT_TEST(read_tiff_from_memory)
{
    MemoryFile mem = MemoryFile::unique("read_tiff_from_memory.tif");
    mem.write(common::readFileToVector(SRTM_PATH));
    TDataset memDs = OpenDataset(mem.path());
    TDataset ds = OpenDataset(SRTM_PATH);
    EXPECT_EQ(memDs.projection(), ds.projection());
    EXPECT_THAT(memDs.Site().PixToProj().matrix(), EigEq(ds.Site().PixToProj().matrix()));
}

Y_UNIT_TEST(write_tiff_to_memory)
{
    MemoryFile mem = MemoryFile::unique("write_tiff_to_memory.tif");
    Int32Image img = Int32Image::random({10, 10}, 2);
    {
        TDataset memDs = CreateTiff(mem.path())
            .setSize(img.size()).setBands(img.bands()).setType(TDataType::Int32);
        memDs.Write(img);
    }
    TDataset ds = OpenDataset{mem.path()};
    EXPECT_EQ(ds.size().x(), 10);
    EXPECT_EQ(ds.size().y(), 10);
    EXPECT_EQ(ds.Read<int>(), img);
}

Y_UNIT_TEST(read_pixel)
{
    TDataset ds = OpenDataset(MOSCOW_PATH);
    EXPECT_FALSE(ds.TryReadPixel({-1, 1}));
    EXPECT_THROW(Y_UNUSED(ds.ReadPixel({-1, 1})), RuntimeError);
    EXPECT_TRUE(ds.TryReadPixel({0, 0}));
    EXPECT_DOUBLE_EQ(ds.ReadPixel({0, 0}), 0);
    EXPECT_TRUE(ds.TryReadPixel({20, 10}));
    EXPECT_DOUBLE_EQ(ds.ReadPixel({20, 10}), 230);
}

Y_UNIT_TEST(read_pixel_with_fractional_shift)
{
    TDataset ds = OpenDataset(SRTM_PATH);
    const auto read = [&](double x, double y) { return ds.ReadPixel(Vector2d(x, y)); };
    EXPECT_EQ(read(0, 0), 230);
    EXPECT_EQ(read(0.1, 0.1), 230);
    EXPECT_EQ(read(0.5, 0.5), 230);
    EXPECT_EQ(read(0.9, 0.9), 230);
    EXPECT_EQ(read(1, 0), 228);
    EXPECT_EQ(read(1.1, 0), 228);
    EXPECT_EQ(read(0, 1), 236);
    EXPECT_EQ(read(0, 1.1), 236);
    EXPECT_EQ(read(1, 1), 235);
    EXPECT_EQ(read(1.1, 1.1), 235);
}

Y_UNIT_TEST(read_pixel_in_dataset_projection)
{
    TDataset ds = OpenDataset(MOSCOW_PATH);
    Eigen::ArrayXd p(1);
    EXPECT_DOUBLE_EQ(ds.ReadPixelInProjection({405688.5, 6189090.2}), 0);
    EXPECT_DOUBLE_EQ(ds.ReadPixelInProjection({406039.6, 6188737.9}), 1);
    EXPECT_DOUBLE_EQ(ds.ReadPixelInProjection({406059.8, 6188713.5}), 39);
    EXPECT_DOUBLE_EQ(ds.ReadPixelInProjection({406109.2, 6188662.9}), 39);
    EXPECT_DOUBLE_EQ(ds.ReadPixelInProjection({406118.2, 6188653.2}), 314);
}

Y_UNIT_TEST(calc_statistics)
{
    const auto path = getTmpTif(this);
    TDataset dsSrc = OpenDataset(IKONOS_PATH);
    TDataset ds = CreateTiff(path).copyFrom(dsSrc);
    const auto stats = ds.calculateStatistics();
    const auto img = ds.Read<double>();
    ASSERT_EQ(stats.size(), static_cast<size_t>(img.bands()));
    for (long b = 0; b < img.bands(); ++b) {
        const auto band = img.col(b);
        const double max = band.maxCoeff();
        const double min = band.minCoeff();
        const double mean = band.mean();
        const double std = std::sqrt((band.array() - mean).square().sum() / band.rows());
        EXPECT_DOUBLE_EQ(stats[b].max, max);
        EXPECT_DOUBLE_EQ(stats[b].min, min);
        EXPECT_DOUBLE_EQ(stats[b].mean, mean);
        EXPECT_NEAR(stats[b].std, std, 1e-8);
    }
}

Y_UNIT_TEST(save_and_load_statistics)
{
    const auto path = getTmpTif(this);
    TDataset dsSrc = OpenDataset(IKONOS_PATH);
    RasterStatistics stats{};
    RasterStatistics loadedStats{};
    {
        TDataset ds = CreateTiff(path).copyFrom(dsSrc);
        stats = ds.calculateStatistics();
        ds.saveStatisticsToMetadata(stats);
    }
    {
        TDataset ds = OpenDataset(path);
        loadedStats = ds.getStatisticsFromMetadata();
    }
    ASSERT_EQ(stats.size(), loadedStats.size());
    for (size_t b = 0; b < stats.size(); ++b) {
        EXPECT_NEAR(stats[b].max, loadedStats[b].max, 1e-8);
        EXPECT_NEAR(stats[b].min, loadedStats[b].min, 1e-8);
        EXPECT_NEAR(stats[b].mean, loadedStats[b].mean, 1e-8);
        EXPECT_NEAR(stats[b].std, loadedStats[b].std, 1e-8);
    }
}

Y_UNIT_TEST(load_rpc_coeffs)
{
    TDataset ds = OpenDataset(MOSCOW_PATH);
    auto rpcMeta = ds.MetadataItems("RPC");
    auto rpc = TRpc::fromGdalMetadata(rpcMeta);

    CPLStringList list;
    for (auto& m: rpcMeta) { list.AddString(m.c_str()); }

    GDALRPCInfo expected{};
    EXPECT_TRUE(GDALExtractRPCInfo(list, &expected));

    GDALRPCInfo result = toGdalRpcInfo(rpc);

    EXPECT_EQ(rpc.pixOffset().x(), 128.064070477525);
    EXPECT_EQ(rpc.pixOffset().y(), 172.128288001591);

    EXPECT_EQ(result.dfSAMP_OFF, rpc.pixOffset().x());
    EXPECT_EQ(result.dfLINE_OFF, rpc.pixOffset().y());

    EXPECT_EQ(result.dfLINE_OFF, expected.dfLINE_OFF);
    EXPECT_EQ(result.dfSAMP_OFF, expected.dfSAMP_OFF);
    EXPECT_EQ(result.dfLAT_OFF, expected.dfLAT_OFF);
    EXPECT_EQ(result.dfLONG_OFF, expected.dfLONG_OFF);
    EXPECT_EQ(result.dfHEIGHT_OFF, expected.dfHEIGHT_OFF);
    EXPECT_EQ(result.dfLINE_SCALE, expected.dfLINE_SCALE);
    EXPECT_EQ(result.dfSAMP_SCALE, expected.dfSAMP_SCALE);
    EXPECT_EQ(result.dfLAT_SCALE, expected.dfLAT_SCALE);
    EXPECT_EQ(result.dfLONG_SCALE, expected.dfLONG_SCALE);
    EXPECT_EQ(result.dfHEIGHT_SCALE, expected.dfHEIGHT_SCALE);

    EXPECT_THAT(result.adfLINE_NUM_COEFF, ElementsAreArray(expected.adfLINE_NUM_COEFF));
    EXPECT_THAT(result.adfLINE_DEN_COEFF, ElementsAreArray(expected.adfLINE_DEN_COEFF));
    EXPECT_THAT(result.adfSAMP_NUM_COEFF, ElementsAreArray(expected.adfSAMP_NUM_COEFF));
    EXPECT_THAT(result.adfSAMP_DEN_COEFF, ElementsAreArray(expected.adfSAMP_DEN_COEFF));

    EXPECT_EQ(result.dfMIN_LONG, expected.dfMIN_LONG);
    EXPECT_EQ(result.dfMIN_LAT, expected.dfMIN_LAT);
    EXPECT_EQ(result.dfMAX_LONG, expected.dfMAX_LONG);
    EXPECT_EQ(result.dfMAX_LAT, expected.dfMAX_LAT);
}

Y_UNIT_TEST(fill_nodata)
{
    constexpr int v = 8;
    constexpr int bands = 2;
    const Size2i sz(5, 5);

    TDataset ds = CreateInMemory().setSize(sz).setBands(bands).setType(TDataType::Int32);
    ds.fillAll(0);
    ds.setNodata(0);

    Int32Image rectImg({2, 2}, 1);
    rectImg.setConstant(v);
    ds.Write(rectImg, Box2d(Point2d(1, 1), Point2d(3, 3)), ResampleAlg::NearestNeighbour, 0);
    ds.Write(rectImg, Box2d(Point2d(2, 2), Point2d(4, 4)), ResampleAlg::NearestNeighbour, 1);

    FillNodata().setBand(0).setDistancePix(1.5).fill(ds);
    FillNodata().setInMemory(true).setBand(1).setDistancePix(1).fill(ds);

    Int32Image expected(sz, bands);
    expected.band(0) <<
        v, v, v, v, 0,
        v, v, v, v, 0,
        v, v, v, v, 0,
        v, v, v, v, 0,
        0, 0, 0, 0, 0;
    expected.band(1) <<
        0, 0, 0, 0, 0,
        0, 0, v, v, 0,
        0, v, v, v, v,
        0, v, v, v, v,
        0, 0, v, v, 0;
    Int32Image img = ds.Read<int>();
    EXPECT_EQ(img, expected);
}

Y_UNIT_TEST(fill_nodata_in_all_bands)
{
    constexpr int v = 8;
    constexpr int bands = 4;
    const Size2i sz(5, 5);

    TDataset ds = CreateInMemory().setSize(sz).setBands(bands).setType(TDataType::UInt16);
    ds.fillAll(0);
    ds.setNodata(0);

    UInt16Image rectImg({1, 1}, bands);
    rectImg.setConstant(v);
    ds.Write(rectImg, Box2d(Point2d(0, 0), Point2d(1, 1)));

    FillNodata().setDistancePix(sz.x() * 2).fill(ds);

    UInt16Image expected(sz, bands);
    expected.setConstant(v);
    UInt16Image img = ds.Read<uint16_t>();
    EXPECT_EQ(img, expected);
}

Y_UNIT_TEST(calculate_overview_levels)
{
    EXPECT_THAT(overviewLevels({26219, 3684}, 256), ElementsAre(2, 4, 8, 16, 32, 64, 128));
    EXPECT_THAT(overviewLevels({6555, 921}, 256), ElementsAre(2, 4, 8, 16, 32));
    EXPECT_THAT(overviewLevels({24696, 20364}, 256), ElementsAre(2, 4, 8, 16, 32, 64, 128));
}

Y_UNIT_TEST(read_dem_height)
{
    const std::string gtxPath = ArcadiaSourceRoot() + "/contrib/libs/proj/data/egm96_15.gtx";
    TDataset ds = OpenDataset(gtxPath);
    Eigen::ArrayXd pix(1);
    EXPECT_EQ(ds.projection(), geodeticSr());
    EXPECT_NEAR(ds.ReadPixelInProjection({37.669126, 55.796218}), 14.25422, 1e-4);
    EXPECT_NEAR(ds.ReadPixelInProjection({122.291609, 63.058368}), -15.10683, 1e-4);
    EXPECT_NEAR(ds.ReadPixelInProjection({57.624333, 39.485177}), -22.38739, 1e-4);
}

Y_UNIT_TEST(all_blocks_read_write)
{
    const auto path = getTmpTif(this);
    fs::copy(MOSCOW_PATH, path);
    TDataset ds = OpenDataset(path).setWritable();

    ds.ForEachBlock<int>({.io = EDataDirection::ReadWrite, .block = 1},
        [&](ImageBase<int>& img, const Point2i&) {
            EXPECT_EQ(img.width(), 1);
            EXPECT_EQ(img.height(), 1);
            EXPECT_EQ(img.bands(), 1);
            img.val(0, 0, 0) += 10;
        });

    TDataset srcDs = OpenDataset(MOSCOW_PATH);
    Image<int> expected = srcDs.Read<int>();
    expected.array() += 10;
    EXPECT_EQ(ds.Read<int>(), expected);
}

Y_UNIT_TEST(all_blocks_read_write_with_overlap)
{
    const auto path = getTmpTif(this);
    fs::copy(MOSCOW_PATH, path);
    TDataset ds = OpenDataset(path).setWritable();

    ds.ForEachBlock<int>({.io = EDataDirection::ReadWrite, .block = 1, .overlap = 2},
        [&](ImageBase<int>& img, const Point2i&) {
            EXPECT_EQ(img.width(), 1 + 2 * 2);
            EXPECT_EQ(img.height(), 1 + 2 * 2);
            EXPECT_EQ(img.bands(), 1);
            img.array() -= 10;
            img.val(2, 2, 0) += 20;
        });

    TDataset srcDs = OpenDataset(MOSCOW_PATH);
    Image<int> expected = srcDs.Read<int>();
    expected.array() += 10;
    EXPECT_EQ(ds.Read<int>(), expected);
}

Y_UNIT_TEST(all_blocks_read_with_overlap)
{
    TDataset ds = OpenDataset(MOSCOW_PATH);

    ds.ForEachBlock<int>({.block = 1, .overlap = 1},
        [&](const ImageBase<int>& img, const Point2i&) {
            EXPECT_EQ(img.width(), 3);
            EXPECT_EQ(img.height(), 3);
            EXPECT_EQ(img.bands(), 1);
        });
}

Y_UNIT_TEST(all_blocks_copy_file)
{
    const auto path = getTmpTif(this);
    TDataset srcDs = OpenDataset(SRTM_PATH);
    TDataset dstDs = CreateTiff(path).like(srcDs);

    srcDs.ForEachBlock<int>({}, [&](const ImageBase<int>& img, const Point2i& origin) {
        dstDs.Write(img, origin);
    });

    EXPECT_EQ(dstDs.Read<int>(), srcDs.Read<int>());
}

Y_UNIT_TEST(calculate_overview_size)
{
    const auto path = getTmpTif(this);
    for (auto&& srcPath: {SRTM_PATH, MOSCOW_PATH, IKONOS_PATH}) {
        TDataset srcDs = OpenDataset(srcPath);
        TDataset ds = srcDs.driver().CopyDataset(srcDs, path);
        ds.deleteOnClose();
        ds.buildOverviews({.block = 32});
        const int ovrs = ds.overviewsCount();
        EXPECT_GT(ovrs, 1);
        for (int ovr = 0; ovr < ovrs; ++ovr) {
            const Size2i ovrSize = overviewSize(ds.size(), ovr);
            EXPECT_THAT(ovrSize, EigEq(ds.getOverviewDataset(ovr).size())) << srcPath;
        }
    }
}

} //suite

} //namespace maps::factory::dataset::tests
