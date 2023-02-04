#include <maps/b2bgeo/libs/traffic_info/traffic_info.h>
#include <maps/b2bgeo/libs/traffic_info/common_mrapi.h>
#include <maps/b2bgeo/libs/traffic_info/vehicle_class.h>
#include <maps/b2bgeo/libs/traffic_info/signals.h>
#include <maps/b2bgeo/libs/cost_matrices/common.h>

#include <maps/doc/proto/yandex/maps/proto/driving_matrix/request.pb.h>
#include <maps/doc/proto/yandex/maps/proto/driving_matrix/matrix.pb.h>
#include <maps/doc/proto/yandex/maps/proto/driving_matrix/matrix_stream.pb.h>
#include <maps/libs/pbstream/include/yandex/maps/pb_stream2/writer.h>
#include <maps/libs/common/include/base64.h>

#include <library/cpp/testing/unittest/registar.h>
#include <library/cpp/testing/gmock_in_unittest/gmock.h>

#include <string>
#include <sstream>
#include <cmath>

using maps::geolib3::Point2;

namespace {

const std::vector<std::vector<std::pair<uint32_t, uint32_t>>> MAPS_RESPONSE_VALUES_EMPTY_MRAPI {
        {}, {}, {}, {}, {}, {}
};

const std::vector<std::vector<std::pair<uint32_t, uint32_t>>> MAPS_RESPONSE_VALUES_MRAPI {
        {{0, 0}, {20, 4}},
        {{25, 5}, {0, 0}}
};

const std::vector<std::vector<std::pair<uint32_t, uint32_t>>> MAPS_RESPONSE_ERROR_VALUES_MRAPI {
        {{0, 0}, {20, 20}, {20, 20}, {20, 20}},
        {{25, 25}, {0, 0}, {25, 25}, {25, 25}},
        {{25, 25}, {25, 25}, {0, 0}, {UINT32_MAX, UINT32_MAX}},
        {{20, 20}, {20, 20}, {20, 20}, {0, 0}}
};

const std::vector<std::vector<std::pair<uint32_t , uint32_t>>> MAPS_RESPONSE_ERROR_MANY_VALUES_MRAPI {
        {{0, 0}, {20, 20}, {20, 20}, {UINT32_MAX, UINT32_MAX}},
        {{25, 25}, {0, 0}, {25, 25}, {UINT32_MAX, UINT32_MAX}},
        {{25, 25}, {UINT32_MAX, -10}, {UINT32_MAX, UINT32_MAX}, {UINT32_MAX, UINT32_MAX}},
        {{UINT32_MAX, UINT32_MAX}, {UINT32_MAX, UINT32_MAX}, {UINT32_MAX, UINT32_MAX}, {UINT32_MAX, UINT32_MAX}}
};

const std::vector<Point2> ORIGINS = {
    {155.61, 37.76}, {55.83, 37.42}, {55.72, 37.55}, {55.71, 37.86}, {55.89, 37.49}, {37.61, 55.72}};
const auto DESTINATIONS = ORIGINS;

std::istringstream createMrapiResponseStream(const std::vector<std::vector<std::pair<uint32_t , uint32_t>>>& values)
{
    std::ostringstream ostream;
    maps::pb_stream2::Writer streamWriter(&ostream);
    for (const auto& row: values) {
        yandex::maps::proto::driving_matrix::Row pbRow;
        pbRow.mutable_duration()->Reserve(row.size());
        pbRow.mutable_distance()->Reserve(row.size());

        for (const auto& weight: row) {
            pbRow.add_distance(weight.first);
            pbRow.add_duration(weight.second);
        }

        streamWriter << pbRow;
    }
    streamWriter.close();
    return std::istringstream(ostream.str());
}

const std::string MRAPI_RESPONSE_MRAPI = maps::base64Decode("AQwKBQCfAdIDEgMAN2YBDAoFnwEAyQMSAzgAegENCgXSA8kDABIEcYQBAA==");

constexpr double EPS = 0.00001;

} // anonymous namespace

namespace ti = maps::b2bgeo::traffic_info;
namespace cm = maps::b2bgeo::cost_matrices;
namespace common = maps::b2bgeo::common;

Y_UNIT_TEST_SUITE(ParseMatrix)
{

ti::FallbackResult dummyFallback(int, int) {
    return {};
}

Y_UNIT_TEST(parseMrapiOutput) {
    maps::b2bgeo::localization::initialize();

    auto stream1 = createMrapiResponseStream(MAPS_RESPONSE_VALUES_EMPTY_MRAPI);
    auto matrix = ti::detail::parseMrapiOutput<yandex::maps::proto::driving_matrix::Row>(
            stream1,
            cm::RoutingMode::DRIVING,
            dummyFallback,
            false
    ).getMatrix();

    ASSERT_EQ(matrix.stride, 0u);
    ASSERT_EQ(matrix.matrix.size(), 0u);

    auto stream2 = createMrapiResponseStream(MAPS_RESPONSE_VALUES_MRAPI);
     matrix = ti::detail::parseMrapiOutput<yandex::maps::proto::driving_matrix::Row>(
             stream2,
             cm::RoutingMode::DRIVING,
             dummyFallback,
             false
     ).getMatrix();

    ASSERT_EQ(matrix.stride, 2u);
    ASSERT_EQ(matrix.matrix.size(), 4u);

    auto stream3 = createMrapiResponseStream(MAPS_RESPONSE_ERROR_VALUES_MRAPI);
    matrix = ti::detail::parseMrapiOutput<yandex::maps::proto::driving_matrix::Row>(
            stream3,
            cm::RoutingMode::TRUCK,
            dummyFallback,
            false
    ).getMatrix();

    ASSERT_EQ(matrix.stride, 4u);
    ASSERT_EQ(matrix.matrix.size(), 16u);

    ASSERT_EQ(matrix.matrix.at(2 * 4 + 3).distance, -1);
    ASSERT_EQ(matrix.matrix.at(2 * 4 + 3).duration, -1);

    auto stream4 = createMrapiResponseStream(MAPS_RESPONSE_ERROR_MANY_VALUES_MRAPI);
    UNIT_ASSERT_EXCEPTION_CONTAINS(
            ti::detail::parseMrapiOutput<yandex::maps::proto::driving_matrix::Row>(
                    stream4,
                    cm::RoutingMode::DRIVING,
                    dummyFallback,
                    false
            ),
            ti::TooManyInvalidCellsException,
            "Example of coordinates from some orders"
    );

    std::istringstream stream5(MRAPI_RESPONSE_MRAPI);
    matrix = ti::detail::parseMrapiOutput<yandex::maps::proto::driving_matrix::Row>(
            stream5,
            cm::RoutingMode::TRUCK,
            dummyFallback,
            false
    ).getMatrix();

    ASSERT_EQ(matrix.stride, 3u);
    ASSERT_EQ(matrix.matrix.size(), 9u);

    EXPECT_NEAR(matrix.matrix.at(0).duration, 0, EPS);
    EXPECT_NEAR(matrix.matrix.at(1).duration, 55, EPS);
    EXPECT_NEAR(matrix.matrix.at(2).duration, 102, EPS);
    EXPECT_NEAR(matrix.matrix.at(3).duration, 56, EPS);
    EXPECT_NEAR(matrix.matrix.at(4).duration, 0, EPS);
    EXPECT_NEAR(matrix.matrix.at(5).duration, 122, EPS);
    EXPECT_NEAR(matrix.matrix.at(6).duration, 113, EPS);
    EXPECT_NEAR(matrix.matrix.at(7).duration, 132, EPS);
    EXPECT_NEAR(matrix.matrix.at(8).duration, 0, EPS);

    EXPECT_NEAR(matrix.matrix.at(0).distance, 0, EPS);
    EXPECT_NEAR(matrix.matrix.at(1).distance, 159, EPS);
    EXPECT_NEAR(matrix.matrix.at(2).distance, 466, EPS);
    EXPECT_NEAR(matrix.matrix.at(3).distance, 159, EPS);
    EXPECT_NEAR(matrix.matrix.at(4).distance, 0, EPS);
    EXPECT_NEAR(matrix.matrix.at(5).distance, 457, EPS);
    EXPECT_NEAR(matrix.matrix.at(6).distance, 466, EPS);
    EXPECT_NEAR(matrix.matrix.at(7).distance, 457, EPS);
    EXPECT_NEAR(matrix.matrix.at(8).distance, 0, EPS);
}

Y_UNIT_TEST(createMrapiInput) {
    const TString s = ti::createMrapiInput<yandex::maps::proto::driving_matrix::Request>(
            ORIGINS,
            DESTINATIONS);
    yandex::maps::proto::driving_matrix::Request requestPb;
    Y_PROTOBUF_SUPPRESS_NODISCARD requestPb.ParseFromString(s);

    ASSERT_EQ(requestPb.srcllSize(), ORIGINS.size());
    ASSERT_EQ(requestPb.dstllSize(), ORIGINS.size());

    for (size_t i = 0; i < DESTINATIONS.size(); ++i) {
        ASSERT_EQ(requestPb.dstll(i).lon(), DESTINATIONS[i].x());
        ASSERT_EQ(requestPb.dstll(i).lat(), DESTINATIONS[i].y());
    }

    for (size_t i = 0; i < ORIGINS.size(); ++i) {
        ASSERT_EQ(requestPb.srcll(i).lon(), ORIGINS[i].x());
        ASSERT_EQ(requestPb.srcll(i).lat(), ORIGINS[i].y());
    }
}
}
