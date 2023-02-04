#include <maps/factory/libs/common/rpc.h>

#include <maps/factory/libs/unittest/tests_common.h>

namespace maps::factory::tests {

Y_UNIT_TEST_SUITE(rpc_should) {

const std::string RPC_META[] = {
    "HEIGHT_OFF=180"s,
    "HEIGHT_SCALE=500"s,
    "LAT_OFF=55.7493"s,
    "LAT_SCALE=0.0967"s,
    "LINE_DEN_COEFF=1 -1.084474e-05 0.0008198225 -2.285952e-06 -4.271315e-08 3.513939e-07 1.628854e-08 3.041846e-07 5.918966e-07 -3.922506e-07 1.380131e-08 0 0 0 0 2.552477e-08 0 0 -1.373797e-07 0"s,
    "LINE_NUM_COEFF=0.001647906 0.01412227 -1.0227 -0.008543032 3.975077e-06 -2.220698e-05 -1.634952e-05 -0.0006056075 -0.0008460824 1.221767e-07 2.697713e-07 1.954889e-08 -4.314291e-08 -2.789951e-08 0 -6.104538e-07 4.243625e-07 -1.631958e-07 5.23632e-08 0"s,
    "LINE_OFF=172.128288001591"s,
    "LINE_SCALE=179.979513698971"s,
    "LONG_OFF=37.6165"s,
    "LONG_SCALE=0.1186"s,
    "SAMP_DEN_COEFF=1 -0.001621713 0.001208017 -0.000462364 1.020493e-07 -1.848817e-07 8.078164e-07 1.999287e-06 1.758746e-06 -8.627872e-07 0 0 0 0 0 0 0 0 0 0"s,
    "SAMP_NUM_COEFF=0.003263633 0.9929979 0.0286472 0.02036295 -0.001303936 0.000410203 -0.000401439 -0.001601264 5.128982e-05 3.735305e-06 7.502895e-07 1.710501e-06 -2.674206e-06 -5.502481e-07 4.208422e-06 6.029727e-08 -1.587999e-07 -1.339433e-06 -2.897306e-07 -1.495995e-08"s,
    "SAMP_OFF=128.064070477525"s,
    "SAMP_SCALE=128.081155938199"s,
};

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
#define LINE_DEN_COEFF 1, -1.084474e-05, 0.0008198225, -2.285952e-06, -4.271315e-08, 3.513939e-07, 1.628854e-08, 3.041846e-07, 5.918966e-07, -3.922506e-07, 1.380131e-08, 0, 0, 0, 0, 2.552477e-08, 0, 0, -1.373797e-07, 0
#define LINE_NUM_COEFF 0.001647906, 0.01412227, -1.0227, -0.008543032, 3.975077e-06, -2.220698e-05, -1.634952e-05, -0.0006056075, -0.0008460824, 1.221767e-07, 2.697713e-07, 1.954889e-08, -4.314291e-08, -2.789951e-08, 0, -6.104538e-07, 4.243625e-07, -1.631958e-07, 5.23632e-08, 0
#define SAMP_DEN_COEFF 1, -0.001621713, 0.001208017, -0.000462364, 1.020493e-07, -1.848817e-07, 8.078164e-07, 1.999287e-06, 1.758746e-06, -8.627872e-07, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0
#define SAMP_NUM_COEFF 0.003263633, 0.9929979, 0.0286472, 0.02036295, -0.001303936, 0.000410203, -0.000401439, -0.001601264, 5.128982e-05, 3.735305e-06, 7.502895e-07, 1.710501e-06, -2.674206e-06, -5.502481e-07, 4.208422e-06, 6.029727e-08, -1.587999e-07, -1.339433e-06, -2.897306e-07, -1.495995e-08

constexpr double RPC_COEFF[92]{
    -1.0, -1.0,
    LINE_OFF, SAMP_OFF, LAT_OFF, LONG_OFF, HEIGHT_OFF,
    LINE_SCALE, SAMP_SCALE, LAT_SCALE, LONG_SCALE, HEIGHT_SCALE,
    LINE_NUM_COEFF, LINE_DEN_COEFF, SAMP_NUM_COEFF, SAMP_DEN_COEFF
};

Y_UNIT_TEST(check_is_null)
{
    EXPECT_TRUE(TRpc().isNull());
    EXPECT_FALSE(TRpc::fromGdalMetadata(RPC_META).isNull());
    EXPECT_FALSE(TRpc().toGdalMetadata().empty());
    EXPECT_TRUE(TRpc::fromGdalMetadata(TRpc().toGdalMetadata()).isNull());
    EXPECT_TRUE(TRpc::fromGdalMetadata(std::vector<std::string>()).isNull());
}

Y_UNIT_TEST(load_from_gdal_metadata)
{
    TRpc rpc = TRpc::fromGdalMetadata(RPC_META);

    Eigen::Matrix<double, 4, TRpc::ORDER> factors;
    factors << LINE_NUM_COEFF, LINE_DEN_COEFF, SAMP_NUM_COEFF, SAMP_DEN_COEFF;

    EXPECT_THAT(rpc.geoScale(), EigEq(Vector3d(LONG_SCALE, LAT_SCALE, HEIGHT_SCALE)));
    EXPECT_THAT(rpc.geoOffset(), EigEq(Vector3d(LONG_OFF, LAT_OFF, HEIGHT_OFF)));
    EXPECT_THAT(rpc.pixScale(), EigEq(Vector2d(SAMP_SCALE, LINE_SCALE)));
    EXPECT_THAT(rpc.pixOffset(), EigEq(Vector2d(SAMP_OFF, LINE_OFF)));
    EXPECT_THAT(rpc.factors(), EigEq(factors.transpose()));
    EXPECT_THAT(rpc.error(), EigEq(Vector2d(-1, -1)));
}

Y_UNIT_TEST(save_to_gdal_metadata)
{
    TRpc rpc = TRpc::fromGdalMetadata(RPC_META);
    EXPECT_THAT(rpc.toGdalMetadata(), ElementsAreArray(RPC_META));
}

Y_UNIT_TEST(load_from_tiff_tag)
{
    TRpc rpc = TRpc::fromTiffTag(RPC_COEFF);

    Eigen::Matrix<double, 4, TRpc::ORDER> factors;
    factors << LINE_NUM_COEFF, LINE_DEN_COEFF, SAMP_NUM_COEFF, SAMP_DEN_COEFF;

    EXPECT_THAT(rpc.geoScale(), EigEq(Vector3d(LONG_SCALE, LAT_SCALE, HEIGHT_SCALE)));
    EXPECT_THAT(rpc.geoOffset(), EigEq(Vector3d(LONG_OFF, LAT_OFF, HEIGHT_OFF)));
    EXPECT_THAT(rpc.pixScale(), EigEq(Vector2d(SAMP_SCALE, LINE_SCALE)));
    EXPECT_THAT(rpc.pixOffset(), EigEq(Vector2d(SAMP_OFF, LINE_OFF)));
    EXPECT_THAT(rpc.factors(), EigEq(factors.transpose()));
    EXPECT_THAT(rpc.error(), EigEq(Vector2d(-1, -1)));
}

Y_UNIT_TEST(save_to_tiff_tag)
{
    TRpc rpc = TRpc::fromTiffTag(RPC_COEFF);
    EXPECT_THAT(rpc.toTiffTag(), ElementsAreArray(RPC_COEFF));
}

Y_UNIT_TEST(check_equal)
{
    TRpc rpc0;
    TRpc rpc1 = TRpc::fromTiffTag(RPC_COEFF);
    TRpc rpc2 = TRpc::fromTiffTag(RPC_COEFF);
    TRpc rpc3 = TRpc::fromGdalMetadata(RPC_META);
    EXPECT_EQ(rpc1, rpc2);
    EXPECT_EQ(rpc2, rpc3);
    EXPECT_EQ(rpc1, rpc3);
    EXPECT_NE(rpc0, rpc1);
    EXPECT_NE(rpc0, rpc2);
    EXPECT_NE(rpc0, rpc3);
}

Y_UNIT_TEST(transform_points)
{
    constexpr size_t count = 4;
    const Vector3d lonLat[count]{
        {37.5195, 55.8016, 175},
        {37.7153, 55.8199, 164},
        {37.5130, 55.6753, 219},
        {37.7232, 55.6719, 151},
    };
    const Vector2d pix[count]{
        {26.560634243864016, 70.738327918866631},
        {236.83743931744937, 40.12849465695362},
        {14.673474905609723, 310.85770233008839},
        {240.03825117436162, 322.04248000253699},
    };

    TRpc rpc = TRpc::fromTiffTag(RPC_COEFF);
    TRpc rpcEmpty;
    for (size_t i = 0; i < count; ++i) {
        EXPECT_THAT(rpc * lonLat[i], EigEq(pix[i], 1e-10));
        EXPECT_THAT(rpcEmpty * lonLat[i], EigEq(Vector2d::Zero()));
    }
}

} // suite

} // namespace maps::factory::tests
