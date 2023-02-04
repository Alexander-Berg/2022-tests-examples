#include <maps/wikimap/mapspro/services/autocart/libs/auto_toloker/include/auto_toloker.h>
#include <maps/libs/common/include/exception.h>
#include <maps/libs/common/include/file_utils.h>

#include <library/cpp/testing/unittest/env.h>
#include <library/cpp/testing/gmock_in_unittest/gmock.h>
#include <library/cpp/testing/unittest/registar.h>
#include <library/cpp/testing/unittest/tests_data.h>

#include <opencv2/opencv.hpp>

#include <vector>
#include <utility>

using namespace testing;

namespace maps::wiki::autocart {

namespace tests {

namespace {

using TestDataPair = std::pair<std::string, float>;

const TestDataPair GOOD_BUILDING_TEST_DATA{"good_building", 0.9f};
const TestDataPair BAD_BUILDING_TEST_DATA{"bad_building", 0.1f};

cv::Mat loadImage(const std::string& name, int flags) {
    const std::string IMAGES_DIR
        = "maps/wikimap/mapspro/services/autocart/libs/auto_toloker/tests/images";
    std::string imagePath = IMAGES_DIR + "/" + name;
    cv::Mat image = cv::imread(BinaryPath(imagePath).data(), flags);
    REQUIRE(image.data != nullptr, "Can't load image " << name);
    return image;
}

} // namespace

Y_UNIT_TEST_SUITE(basic_tests)
{

Y_UNIT_TEST(auto_toloker_on_reference_images)
{
    AutoToloker toloker;
    // good building
    cv::Mat goodImage = loadImage(GOOD_BUILDING_TEST_DATA.first + ".jpg", cv::IMREAD_COLOR);
    cv::Mat goodMask = loadImage(GOOD_BUILDING_TEST_DATA.first + ".png", cv::IMREAD_GRAYSCALE);

    float goodConfidence = toloker.classify(goodImage, goodMask);

    EXPECT_THAT(goodConfidence, Ge(GOOD_BUILDING_TEST_DATA.second));

    // bad building
    cv::Mat badImage = loadImage(BAD_BUILDING_TEST_DATA.first + ".jpg", cv::IMREAD_COLOR);
    cv::Mat badMask = loadImage(BAD_BUILDING_TEST_DATA.first + ".png", cv::IMREAD_GRAYSCALE);

    float badConfidence = toloker.classify(badImage, badMask);

    EXPECT_THAT(BAD_BUILDING_TEST_DATA.second, Ge(badConfidence));
}

} // Y_UNIT_TEST_SUITE(basic_tests)

} // namespace test

} // namespace maps::wiki::autocart
