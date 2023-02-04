#include <maps/factory/libs/image/statistics.h>

#include <maps/factory/libs/dataset/vrt_dataset.h>

#include <maps/factory/libs/unittest/tests_common.h>

namespace maps::factory::dataset::tests {
using namespace maps::factory::tests;

Y_UNIT_TEST_SUITE(covariance_stage_should) {

using Eigen::RowVector3d;
using Eigen::Matrix3d;

Y_UNIT_TEST(calcluate_std_and_mean_for_single_band_dataset)
{
    TDataset ds = OpenDataset(SRTM_PATH);

    ImageCovariance<1, float> stage;
    ds.ForEachBlock<float>({}, [&](FloatImageBase& img, auto&&) { stage(img); });

    auto&& cov = stage.result();
    EXPECT_EQ(cov.weight(), 1201 * 1201);
    EXPECT_NEAR(cov.mean()(0), 175.01045201, 1e-8);
    EXPECT_NEAR(cov.cov()(0, 0), 582.33837221, 1e-8);
}

Y_UNIT_TEST(calcluate_covariance_for_equal_bands)
{
    TDataset ds = OpenDataset(SRTM_PATH);
    VrtDataset vrt(ds.size());
    vrt.copyInfoFrom(ds);
    vrt.add(ds);
    vrt.add(ds);
    vrt.add(ds);

    ImageCovariance<3, int16_t> stage;
    vrt.ForEachBlock<int16_t>({}, [&](Int16ImageBase& img, auto&&) { stage(img); });

    auto&& cov = stage.result();
    EXPECT_EQ(cov.weight(), 1201 * 1201);
    EXPECT_THAT(cov.mean(), EigEq(RowVector3d::Constant(175.01045201), 1e-8));
    EXPECT_THAT(cov.cov(), EigEq(Matrix3d::Constant(582.33837221), 1e-8));
}

Y_UNIT_TEST(calcluate_covariance_with_skipping_rows)
{
    using CovStage = ImageCovariance<3, uint8_t>;
    TDataset ds = OpenDataset(IKONOS_PATH);

    CovStage stageSkipNone(SkipRows::None);
    CovStage stageSkipAnyZero(SkipRows::AnyZero);
    CovStage stageSkipAllZero(SkipRows::AllZero);
    CovStage stageSkipLastZero(SkipRows::LastBandZero);
    ds.ForEachBlock<uint8_t>({}, [&](UInt8ImageBase& img, auto&&) {
        stageSkipNone(img);
        stageSkipAnyZero(img);
        stageSkipAllZero(img);
        stageSkipLastZero(img);
    });

    {
        // Check stats for all rows.
        auto&& cov = stageSkipNone.result();
        EXPECT_EQ(cov.weight(), 15360);
        EXPECT_THAT(cov.mean(), EigEq(RowVector3d{107.484, 97.6266, 86.879}, 1e-3));
        Matrix3d expectedCov;
        expectedCov <<
            1731.06, 1524.75, 1387.16,
            1524.75, 1362.81, 1241.62,
            1387.16, 1241.62, 1138.63;
        EXPECT_THAT(cov.cov(), EigEq(expectedCov, 1e-2));
    }

    DoubleImage data = ds.Read<double>();
    {
        // Check stats without rows with any zero channel.
        OnlineCovariance<3> expected;
        for (Index index = 0; index < data.indices(); ++index) {
            if ((data.pix(index).array() == 0).any()) {
                continue;
            }
            expected.addRow(data.pix(index).matrix());
        }
        auto&& cov = stageSkipAnyZero.result();
        EXPECT_EQ(cov.weight(), expected.weight());
        EXPECT_THAT(cov.mean(), EigEq(expected.mean(), 1e-8));
        EXPECT_THAT(cov.cov(), EigEq(expected.cov(), 1e-8));
    }
    {
        // Check stats without rows with all zero channels.
        OnlineCovariance<3> expected;
        for (Index index = 0; index < data.indices(); ++index) {
            if ((data.pix(index).array() == 0).all()) {
                continue;
            }
            expected.addRow(data.pix(index).matrix());
        }
        auto&& cov = stageSkipAllZero.result();
        EXPECT_EQ(cov.weight(), expected.weight());
        EXPECT_THAT(cov.mean(), EigEq(expected.mean(), 1e-8));
        EXPECT_THAT(cov.cov(), EigEq(expected.cov(), 1e-8));
    }
    {
        // Check stats for rows with non-zero last column.
        OnlineCovariance<3> expected;
        for (Index index = 0; index < data.indices(); ++index) {
            if (data.val(index, 2) > 0) {
                expected.addRow(data.pix(index).matrix());
            }
        }
        auto&& cov = stageSkipLastZero.result();
        EXPECT_EQ(cov.weight(), expected.weight());
        EXPECT_THAT(cov.mean(), EigEq(expected.mean(), 1e-8));
        EXPECT_THAT(cov.cov(), EigEq(expected.cov(), 1e-8));
    }
}

} // suite

Y_UNIT_TEST_SUITE(histogram_stage_should) {

using Row = Eigen::Array<double, 1, 3>;

Y_UNIT_TEST(compute_percentiles_for_rgb_image)
{
    TDataset ds = OpenDataset(IKONOS_PATH);

    ImageHistogram<3, float> stage(255);
    ds.ForEachBlock<float>({}, [&](FloatImageBase& img, auto&&) { stage(img); });

    auto&& hist = stage.result();
    EXPECT_THAT(hist.computeMin(), EigEq(Row{0, 0, 0}));
    EXPECT_THAT(hist.computeMax(), EigEq(Row{249, 250, 251}));
    EXPECT_THAT(hist.computePercentile(0.1), EigEq(Row{22, 30, 24}));
    EXPECT_THAT(hist.computePercentile(0.9), EigEq(Row{141, 127, 115}));
}

Y_UNIT_TEST(compute_percentiles_skipping_zero_pixels)
{
    TDataset ds = OpenDataset(IKONOS_PATH);

    ImageHistogram<3, float> stage(2047, SkipRows::AnyZero);
    ds.ForEachBlock<float>({}, [&](FloatImageBase& img, auto&&) { stage(img); });

    auto&& hist = stage.result();
    EXPECT_THAT(hist.computeMin(), EigEq(Row{1, 1, 1}));
    EXPECT_THAT(hist.computeMax(), EigEq(Row{249, 250, 251}));
    EXPECT_THAT(hist.computePercentile(0.1), EigEq(Row{84, 76, 66}));
    EXPECT_THAT(hist.computePercentile(0.9), EigEq(Row{141, 128, 115}));
}

} // suite

} //namespace maps::factory::dataset::tests
