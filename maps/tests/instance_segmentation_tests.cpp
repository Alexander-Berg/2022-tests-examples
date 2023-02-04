#include <library/cpp/testing/unittest/registar.h>
#include <library/cpp/testing/unittest/gtest.h>

#include <maps/wikimap/mapspro/services/autocart/libs/detection/include/instance_segmentation.h>

namespace maps {
namespace wiki {
namespace autocart {
namespace tests {

Y_UNIT_TEST_SUITE(merge_instances_tests)
{

    Y_UNIT_TEST(not_merge_distant_instances)
    {
        cv::Mat maskTemplate(50, 50, CV_8UC1, cv::Scalar::all(255));
        geolib3::BoundingBox bbox1(geolib3::Point2(0, 0),
                                   geolib3::Point2(50, 50));
        geolib3::BoundingBox bbox2(geolib3::Point2(100, 100),
                                   geolib3::Point2(150, 150));
        BldInstance instance1(bbox1, maskTemplate);
        BldInstance instance2(bbox2, maskTemplate);
        std::vector<BldInstance> expectedInstances = {instance1, instance2};

        auto mergedInstances = mergeInstances({instance1, instance2});

        EXPECT_EQ(mergedInstances.size(), 2);
        for (size_t i = 0; i < 2; i++) {
            EXPECT_EQ(cv::countNonZero(mergedInstances[i].mask != expectedInstances[i].mask), 0);
            EXPECT_EQ(mergedInstances[i].bbox.lowerCorner().x(), expectedInstances[i].bbox.lowerCorner().x());
            EXPECT_EQ(mergedInstances[i].bbox.lowerCorner().y(), expectedInstances[i].bbox.lowerCorner().y());
            EXPECT_EQ(mergedInstances[i].bbox.height(), expectedInstances[i].bbox.height());
            EXPECT_EQ(mergedInstances[i].bbox.width(), expectedInstances[i].bbox.width());
        }
    }

    Y_UNIT_TEST(merge_close_instances)
    {
        cv::Mat maskTemplate(100, 100, CV_8UC1, cv::Scalar::all(255));
        geolib3::BoundingBox bbox1(geolib3::Point2(0, 0),
                                   geolib3::Point2(100, 100));
        geolib3::BoundingBox bbox2(geolib3::Point2(20, 20),
                                   geolib3::Point2(120, 120));
        BldInstance instance1(bbox1, maskTemplate);
        BldInstance instance2(bbox2, maskTemplate);

        geolib3::BoundingBox expectedBBox(geolib3::Point2(0, 0),
                                          geolib3::Point2(120, 120));
        cv::Mat expectedMask(120, 120, CV_8UC1, cv::Scalar::all(0));
        maskTemplate.copyTo(expectedMask(cv::Rect(cv::Point(0, 0), cv::Size(100, 100))));
        maskTemplate.copyTo(expectedMask(cv::Rect(cv::Point(20, 20), cv::Size(100, 100))));

        auto mergedInstances = mergeInstances({instance1, instance2});

        EXPECT_EQ(mergedInstances.size(), 1);
        EXPECT_EQ(cv::countNonZero(mergedInstances[0].mask != expectedMask), 0);
        EXPECT_DOUBLE_EQ(mergedInstances[0].bbox.lowerCorner().x(), expectedBBox.lowerCorner().x());
        EXPECT_DOUBLE_EQ(mergedInstances[0].bbox.lowerCorner().y(), expectedBBox.lowerCorner().y());
        EXPECT_DOUBLE_EQ(mergedInstances[0].bbox.height(), expectedBBox.height());
        EXPECT_DOUBLE_EQ(mergedInstances[0].bbox.width(), expectedBBox.width());
    }

} //Y_UNIT_TEST_SUITE(merge_instances_tests)

Y_UNIT_TEST_SUITE(vectorize_instances_tests)
{

    Y_UNIT_TEST(vectorize_complex_instances)
    {
        cv::Mat maskTemplate(100, 100, CV_8UC1, cv::Scalar::all(255));
        geolib3::BoundingBox bbox(geolib3::Point2(0, 0),
                                  geolib3::Point2(150, 150));
        cv::Mat mask(150, 150, CV_8UC1, cv::Scalar::all(0));
        maskTemplate.copyTo(mask(cv::Rect(cv::Point(0, 0), cv::Size(100, 100))));
        maskTemplate.copyTo(mask(cv::Rect(cv::Point(50, 50), cv::Size(100, 100))));
        BldInstance instance(bbox, mask);
        std::vector<cv::Point2f> expectedPoints = {
                                                    {0., 0.}, {0., 99.}, {48., 99.},
                                                    {48., 150.}, {150., 150.}, {150., 51.},
                                                    {99., 51.}, {99., 0.}
                                                  };

        std::vector<std::vector<cv::Point2f>> polygons = vectorizeInstances({instance});

        EXPECT_EQ(polygons.size(), 1);
        EXPECT_EQ(polygons[0].size(), expectedPoints.size());
        for (size_t i = 0; i < expectedPoints.size(); i++) {
            EXPECT_DOUBLE_EQ(polygons[0][i].x, expectedPoints[i].x);
            EXPECT_DOUBLE_EQ(polygons[0][i].y, expectedPoints[i].y);
        }
    }

} //Y_UNIT_TEST_SUITE(vectorize_instances_tests)

Y_UNIT_TEST_SUITE(make_batches_tests)
{

Y_UNIT_TEST(base_test)
{
    const size_t batchSize = 5;
    const int horizCellsCnt = 4;
    const int vertCellsCnt = 3;

    std::vector<std::vector<std::pair<int, int>>> batches
        = makeCropBatches(horizCellsCnt, vertCellsCnt, batchSize);

    ASSERT_EQ(batches.size(), 3);

    ASSERT_EQ(batches[0].size(), 5);
    EXPECT_EQ(batches[0][0], std::make_pair(0, 0));
    EXPECT_EQ(batches[0][1], std::make_pair(0, 1));
    EXPECT_EQ(batches[0][2], std::make_pair(0, 2));
    EXPECT_EQ(batches[0][3], std::make_pair(1, 0));
    EXPECT_EQ(batches[0][4], std::make_pair(1, 1));

    ASSERT_EQ(batches[1].size(), 5);
    EXPECT_EQ(batches[1][0], std::make_pair(1, 2));
    EXPECT_EQ(batches[1][1], std::make_pair(2, 0));
    EXPECT_EQ(batches[1][2], std::make_pair(2, 1));
    EXPECT_EQ(batches[1][3], std::make_pair(2, 2));
    EXPECT_EQ(batches[1][4], std::make_pair(3, 0));

    ASSERT_EQ(batches[2].size(), 2);
    EXPECT_EQ(batches[2][0], std::make_pair(3, 1));
    EXPECT_EQ(batches[2][1], std::make_pair(3, 2));
}

} //Y_UNIT_TEST_SUITE(make_batches_tests)

} //namespace tests
} //namespace autocart
} //namespace wiki
} //namespace maps

