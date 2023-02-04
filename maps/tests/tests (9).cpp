#include <maps/wikimap/mapspro/libs/tfrecord_writer/include/tfrecord_writer.h>
#include <maps/libs/common/include/exception.h>
#include <maps/libs/common/include/file_utils.h>

#include <opencv2/opencv.hpp>
#include <opencv2/imgcodecs/imgcodecs_c.h>

#include <library/cpp/testing/unittest/env.h>
#include <library/cpp/testing/gmock_in_unittest/gmock.h>
#include <library/cpp/testing/unittest/registar.h>
#include <library/cpp/testing/unittest/tests_data.h>

#include <util/stream/file.h>

#include <fstream>
#include <iostream>
#include <sstream>
#include <vector>
#include <utility>
#include <unordered_set>

using namespace testing;

namespace maps {
namespace wiki {
namespace tfrecord_writer {

namespace tests {

namespace {

cv::Mat loadImage(const std::string& name, int flags) {
    static const std::string IMAGES_DIR =
        "maps/wikimap/mapspro/libs/tfrecord_writer/tests/images/";
    auto imagePath = static_cast<std::string>(BinaryPath(IMAGES_DIR + name));
    cv::Mat image = cv::imread(imagePath, flags);
    REQUIRE(image.data != nullptr, "Can't load image " << name);
    return image;
}

std::vector<std::pair<cv::Mat, FasterRCNNObjects>>
loadFasterRCNNTestData() {
    return {
        {
            loadImage("image000.jpg", cv::IMREAD_COLOR),
            {
                {1, "test1", cv::Rect(0, 0, 100, 100)}
            }
        },
        {
            loadImage("image001.jpg", cv::IMREAD_COLOR),
            {
                {1, "test1", cv::Rect(0, 0, 100, 100)},
                {1, "test1", cv::Rect(100, 0, 100, 100)},
                {2, "test2", cv::Rect(0, 100, 100, 100)}
            }
        }
    };
}

std::vector<std::pair<cv::Mat, MaskRCNNObjects>>
loadMaskRCNNTestData() {
    return {
        {
            loadImage("image000.jpg", cv::IMREAD_COLOR),
            {
                {
                    1, "test1", cv::Rect(0, 0, 100, 100),
                    loadImage("mask000_0.png", cv::IMREAD_GRAYSCALE)
                }
            }
        },
        {
            loadImage("image001.jpg", cv::IMREAD_COLOR),
            {
                {
                    1, "test1", cv::Rect(0, 0, 100, 100),
                    loadImage("mask001_0.png", cv::IMREAD_GRAYSCALE)
                },
                {
                    1, "test1", cv::Rect(100, 0, 100, 100),
                    loadImage("mask001_1.png", cv::IMREAD_GRAYSCALE)
                },
                {
                    2, "test2", cv::Rect(0, 100, 100, 100),
                    loadImage("mask001_2.png", cv::IMREAD_GRAYSCALE)
                }
            }
        }
    };
}

std::vector<std::pair<cv::Mat, MultiLabelsObjects>>
loadMultiLabelsTestData() {
    return {
        {
            loadImage("image000.jpg", cv::IMREAD_COLOR),
            {
                {{1, 2, 3}, "test1", cv::Rect(0, 0, 100, 100)}
            }
        },
        {
            loadImage("image001.jpg", cv::IMREAD_COLOR),
            {
                {{1, 2}, "test1", cv::Rect(0, 0, 100, 100)}
            }
        }
    };
}


class TDummyOutputStream: public IOutputStream {
public:
    TDummyOutputStream()
    { }

    void DoWrite(const void*, size_t) override {
    }
};

template <typename Object>
void testTFRecordWriterMakeFile(
    const std::vector<std::pair<cv::Mat, std::list<Object>>>& testData)
{
    TDummyOutputStream os;
    TFRecordWriter<Object> writer(&os);
    for (const auto& [image, objects] : testData) {
        writer.AddRecord(image, objects);
    }
}

template <typename Object>
void testTFRecordWriterCounter(
    const std::vector<std::pair<cv::Mat, std::list<Object>>>& testData)
{
    TDummyOutputStream os;
    TFRecordWriter<Object> writer(&os);
    size_t objectsCount = 0;
    for (const auto& [image, objects] : testData) {
        writer.AddRecord(image, objects);
        objectsCount += objects.size();
    }
    EXPECT_THAT(writer.GetRecordsCount(), Eq(testData.size()));
    EXPECT_THAT(writer.GetObjectsCount(), Eq(objectsCount));
}

} // namespace

Y_UNIT_TEST_SUITE(basic_tests)
{

Y_UNIT_TEST(tfrecord_fasterrcnn_writer_make_file)
{
    testTFRecordWriterMakeFile<FasterRCNNObject>(loadFasterRCNNTestData());
}

Y_UNIT_TEST(tfrecord_writer_counter)
{
    testTFRecordWriterCounter<FasterRCNNObject>(loadFasterRCNNTestData());
}

Y_UNIT_TEST(tfrecord_maskrcnn_writer_make_file)
{
    testTFRecordWriterMakeFile<MaskRCNNObject>(loadMaskRCNNTestData());
}

Y_UNIT_TEST(tfrecord_maskrcnn_writer_counter)
{
    testTFRecordWriterCounter<MaskRCNNObject>(loadMaskRCNNTestData());
}

Y_UNIT_TEST(tfrecord_multilabels_writer_make_file)
{
    testTFRecordWriterMakeFile<MultiLabelsObject>(loadMultiLabelsTestData());
}

Y_UNIT_TEST(tfrecord_multilabels_counter)
{
    testTFRecordWriterCounter<MultiLabelsObject>(loadMultiLabelsTestData());
}

} // Y_UNIT_TEST_SUITE(basic_tests)

} // namespace test

} // namespace roadmarkdetector
} // namespace mrc
} // namespace maps
