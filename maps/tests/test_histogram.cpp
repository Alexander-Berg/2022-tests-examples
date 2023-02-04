#include <maps/factory/libs/common/histogram.h>

#include <maps/factory/libs/unittest/tests_common.h>

namespace maps::factory::tests {

Y_UNIT_TEST_SUITE(histogram_should) {

Y_UNIT_TEST(compute_minmax)
{
    Histogram hist(255);
    for (uint8_t i = 30; i < 40; ++i) {
        hist.add(i);
    }

    EXPECT_EQ(hist.computeMin(0), 30);
    EXPECT_EQ(hist.computeMax(0), 39);
    EXPECT_EQ(hist.total(0), 10u);

    for (int i = 0; i < 256; ++i) {
        hist.add(i);
    }

    EXPECT_EQ(hist.computeMin(0), 0);
    EXPECT_EQ(hist.computeMax(0), 255);
    EXPECT_EQ(hist.total(0), 266u);
}

Y_UNIT_TEST(compute_cutoff_limits)
{
    Histogram hist(255);
    for (uint8_t i = 100; i <= 200; ++i) {
        hist.add(i);
    }
    EXPECT_EQ(hist.total(0), 101u);
    EXPECT_EQ(hist.computePercentile(0.00, 0), 100);
    EXPECT_EQ(hist.computePercentile(0.02, 0), 102);
    EXPECT_EQ(hist.computePercentile(0.10, 0), 110);
    EXPECT_EQ(hist.computePercentile(0.90, 0), 190);
    EXPECT_EQ(hist.computePercentile(0.98, 0), 198);
    EXPECT_EQ(hist.computePercentile(1.00, 0), 200);
}

Y_UNIT_TEST(save_to_json)
{
    Histogram hist(255);
    for (uint8_t i = 10; i < 20; ++i) {
        hist.add(i);
    }
    const auto json = R"({"max":255,"bins":[[0,0,0,0,0,0,0,0,0,0,1,1,1,1,1,1,1,1,1,1]]})";
    EXPECT_EQ(hist.json(), json);
}

Y_UNIT_TEST(load_from_json)
{
    const auto json = R"({"max":255,"bins":[[0,0,0,0,0,0,0,0,0,0,1,1,1,1,1,1,1,1,1,1]]})";
    Histogram hist{json};
    EXPECT_EQ(hist.max(), 255);
    EXPECT_EQ(hist.total(0), 10u);

    Eigen::ArrayXi expected;
    expected.setZero(256);
    expected(Eigen::seq(10, 19)) = 1;
    EXPECT_THAT(hist.bins().cast<int>(), EigEq(expected));
}

Y_UNIT_TEST(merge_histograms)
{
    Histogram hist(255), other(255);
    for (uint8_t i = 10; i < 20; ++i) { hist.add(i); }
    for (uint8_t i = 15; i < 25; ++i) { other.add(i); }

    hist += other;

    const auto json = R"({"max":255,"bins":[[0,0,0,0,0,0,0,0,0,0,1,1,1,1,1,2,2,2,2,2,1,1,1,1,1]]})";
    EXPECT_EQ(hist.json(), json);
}

Y_UNIT_TEST(compute_max_bin)
{
    Histogram hist(255);
    for (uint8_t i = 5; i < 15; ++i) { hist.add(i); }
    for (uint8_t i = 10; i < 20; ++i) { hist.add(i); }
    for (uint8_t i = 12; i < 16; ++i) { hist.add(i); }

    EXPECT_DOUBLE_EQ(hist.computeMaxBin(0), 3);
}

Y_UNIT_TEST(normalize_histogram_total)
{
    Histogram hist(255);
    for (uint8_t i = 5; i < 15; ++i) { hist.add(i); }
    for (uint8_t i = 10; i < 20; ++i) { hist.add(i); }

    hist.normalizeTotal();

    EXPECT_DOUBLE_EQ(hist.total(0), 255);
    EXPECT_EQ(hist.json(),
        R"({"max":255,"bins":[[0,0,0,0,0,12.75,12.75,12.75,12.75,12.75,25.5,25.5,25.5,25.5,25.5,12.75,12.75,12.75,12.75,12.75]]})");
}

Y_UNIT_TEST(compute_cdf)
{
    Histogram<1> hist(30);
    for (uint8_t i = 5; i < 15; ++i) { hist.add(i); }
    for (uint8_t i = 10; i < 20; ++i) { hist.add(i); }

    auto cdf = hist.cdf();

    EXPECT_DOUBLE_EQ(cdf.total(0), 380);
    EXPECT_EQ(cdf.json(),
        R"({"max":30,"bins":[[0,0,0,0,0,1,2,3,4,5,7,9,11,13,15,16,17,18,19,20,20,20,20,20,20,20,20,20,20,20,20]]})");
}

Y_UNIT_TEST(normalize_cdf_max)
{
    Histogram<1> hist(30);
    for (uint8_t i = 5; i < 15; ++i) { hist.add(i); }
    for (uint8_t i = 10; i < 20; ++i) { hist.add(i); }

    auto cdf = hist.cdf();
    cdf.normalizeMax();

    EXPECT_DOUBLE_EQ(cdf.total(0), 380 * 30 / 20);
    EXPECT_DOUBLE_EQ(cdf.computeMaxBin(0), 30);
    EXPECT_EQ(cdf.json(),
        R"({"max":30,"bins":[[0,0,0,0,0,1.5,3,4.5,6,7.5,10.5,13.5,16.5,19.5,22.5,24,25.5,27,28.5,30,30,30,30,30,30,30,30,30,30,30,30]]})");
}

Y_UNIT_TEST(medial_filter)
{
    Histogram<1> hist(30);
    for (uint8_t i = 10; i < 20; ++i) { hist.add(i); }
    for (uint8_t i = 15; i < 25; ++i) { hist.add(i); }

    EXPECT_EQ(hist.medianFiltered().json(),
        R"({"max":30,"bins":[[0,0,0,0,0,0,0,0,0,0,1,1,1,1,1,2,2,2,2,2,1,1,1,1,1]]})");

    for (uint8_t i = 1; i < 30; i += 3) { hist.add(i); }

    EXPECT_EQ(hist.medianFiltered().json(),
        R"({"max":30,"bins":[[0,0,0,0,0,0,0,0,0,0,1,1,1,1,2,2,2,2,2,2,1,1,1,1,1,1]]})");
}

Y_UNIT_TEST(resize)
{
    Histogram<2> hist(10);
    for (uint8_t i = 1; i < 3; ++i) { hist.add(i, 0); }
    for (uint8_t i = 2; i < 5; ++i) { hist.add(i, 1); }

    EXPECT_EQ(hist.json(),
        "{\"max\":10,\"bins\":[[0,1,1],[0,0,1,1,1]]}");
    EXPECT_EQ(hist.resized<1>().json(),
        "{\"max\":10,\"bins\":[[0,1,1]]}");
    EXPECT_EQ(hist.resized<2>().json(),
        "{\"max\":10,\"bins\":[[0,1,1],[0,0,1,1,1]]}");
    EXPECT_EQ(hist.resized<3>().json(),
        "{\"max\":10,\"bins\":[[0,1,1],[0,0,1,1,1],[]]}");
}

Y_UNIT_TEST(flatten)
{
    Histogram<2> hist(10);
    Histogram<1> flatten(10);
    for (uint8_t i = 1; i < 3; ++i) {
        hist.add(i, 0);
        flatten.add(i, 0);
    }
    for (uint8_t i = 2; i < 5; ++i) {
        hist.add(i, 1);
        flatten.add(i, 0);
    }

    EXPECT_EQ(hist.flatten(), flatten);
}

} // suite

} // namespace maps::factory::tests
