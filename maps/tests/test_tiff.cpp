#include <maps/factory/libs/common/include/tiff.h>

#include <maps/factory/libs/unittest/tests_common.h>

#include <library/cpp/testing/unittest/registar.h>
#include <library/cpp/testing/unittest/env.h>
#include <library/cpp/testing/gmock_in_unittest/gmock.h>

namespace maps::factory::tests {

Y_UNIT_TEST_SUITE(test_tiff) {

Y_UNIT_TEST(test_reading_line_by_line)
{
    /*
     * Comprehensive corpus of tiff images for testing can be found at
     * http://www.libtiff.org/images.html
     */
    std::string TEST_TIF_PATH = ArcadiaSourceRoot() + "/maps/factory/libs/common/tests/data/white_24x24.tif";
    Tiff tiff(TEST_TIF_PATH);

    EXPECT_FALSE(tiff.isTiled());
    EXPECT_EQ(tiff.width(), 32u);
    EXPECT_EQ(tiff.height(), 32u);
    EXPECT_EQ(tiff.bytesPerChannel(), 1u);
    EXPECT_EQ(tiff.channelsNumber(), 3u);

    std::vector<uint8_t> scanline(tiff.scanlineSize(), 0);
    for (size_t line = 0; line < tiff.height(); ++line) {
        tiff.readScanline(line, scanline);
    }

    EXPECT_FALSE(tiff.isProjected());
}

Y_UNIT_TEST(test_projected_tiff)
{
    Tiff ikonosTiff(IKONOS_PATH);
    ASSERT_TRUE(ikonosTiff.isProjected());
    EXPECT_EQ(ikonosTiff.projectionCode(), 3395);
    EXPECT_DOUBLE_EQ(ikonosTiff.projectionResolution().x(), 152.87405656999999);
    EXPECT_DOUBLE_EQ(ikonosTiff.projectionResolution().y(), -152.87405656999999);

    Tiff wvTiff(WV_PATH);
    ASSERT_TRUE(wvTiff.isProjected());
    EXPECT_EQ(wvTiff.projectionCode(), 32644);
    EXPECT_DOUBLE_EQ(wvTiff.projectionResolution().x(), 2);
    EXPECT_DOUBLE_EQ(wvTiff.projectionResolution().y(), -2);

    Tiff srtmTiff(SRTM_PATH);
    ASSERT_TRUE(srtmTiff.isProjected());
    EXPECT_EQ(srtmTiff.projectionCode(), 4326);
    EXPECT_DOUBLE_EQ(srtmTiff.projectionResolution().x(), 0.000833333333333333);
    EXPECT_DOUBLE_EQ(srtmTiff.projectionResolution().y(), -0.000833333333333333);
}

Y_UNIT_TEST(test_read_rpc)
{
    Tiff ikonosTiff(IKONOS_PATH);
    EXPECT_FALSE(ikonosTiff.hasRpcFactors());

    Tiff wvTiff(WV_PATH);
    EXPECT_FALSE(wvTiff.hasRpcFactors());

    Tiff srtmTiff(SRTM_PATH);
    EXPECT_FALSE(srtmTiff.hasRpcFactors());

    Tiff mosTiff(MOSCOW_PATH);
    EXPECT_TRUE(mosTiff.hasRpcFactors());

    constexpr double HEIGHT_OFF = 180;
    constexpr double HEIGHT_SCALE = 500;
    constexpr double LAT_OFF = 55.7493;
    constexpr double LAT_SCALE = 0.0967;
    constexpr double LINE_OFF = 172.128288001591;
    constexpr double LINE_SCALE = 179.979513698971;
    constexpr double LONG_OFF = 37.6165;
    constexpr double LONG_SCALE = 0.1186;
    constexpr double SAMP_OFF = 128.064070477525;
    constexpr double SAMP_SCALE = 128.081155938199;
    const double rpc[RPC_FACTORS_COUNT]{
        -1.0, -1.0,
        LINE_OFF, SAMP_OFF, LAT_OFF, LONG_OFF, HEIGHT_OFF,
        LINE_SCALE, SAMP_SCALE, LAT_SCALE, LONG_SCALE, HEIGHT_SCALE,
        0.001647906, 0.01412227, -1.0227, -0.008543032, 3.975077e-06, -2.220698e-05, -1.634952e-05,
        -0.0006056075, -0.0008460824, 1.221767e-07, 2.697713e-07, 1.954889e-08, -4.314291e-08, -2.789951e-08,
        0, -6.104538e-07, 4.243625e-07, -1.631958e-07, 5.23632e-08, 0,
        1, -1.084474e-05, 0.0008198225, -2.285952e-06, -4.271315e-08, 3.513939e-07, 1.628854e-08,
        3.041846e-07, 5.918966e-07, -3.922506e-07, 1.380131e-08, 0, 0, 0, 0, 2.552477e-08, 0, 0,
        -1.373797e-07, 0,
        0.003263633, 0.9929979, 0.0286472, 0.02036295, -0.001303936, 0.000410203, -0.000401439, -0.001601264,
        5.128982e-05, 3.735305e-06, 7.502895e-07, 1.710501e-06, -2.674206e-06, -5.502481e-07, 4.208422e-06,
        6.029727e-08, -1.587999e-07, -1.339433e-06, -2.897306e-07, -1.495995e-08,
        1, -0.001621713, 0.001208017, -0.000462364, 1.020493e-07, -1.848817e-07, 8.078164e-07, 1.999287e-06,
        1.758746e-06, -8.627872e-07, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
    };
    EXPECT_THAT(mosTiff.rpcFactors(), ElementsAreArray(rpc));
}

} //Y_UNIT_TEST_SUITE

} //namespace maps::factory::tests
