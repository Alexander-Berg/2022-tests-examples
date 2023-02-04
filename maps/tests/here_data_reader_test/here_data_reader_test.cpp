#include "../common.h"

#include <maps/analyzer/libs/tie_here_jams/include/here_data_reader.h>
#include <maps/analyzer/libs/common/include/region_map.h>

#include <library/cpp/testing/unittest/env.h>
#include <library/cpp/testing/unittest/registar.h>
#include <library/cpp/testing/unittest/gtest.h>

#include <util/system/fs.h>
#include <util/folder/path.h>

#include <string>
#include <unordered_set>

namespace tie_here_jams = maps::analyzer::tie_here_jams;

const std::string LOCATION_TABLE_ID = "33";

// customization operator<< of tie_here_jams::Direction for unittests. Fix linker problems
template <>
void Out<tie_here_jams::Direction>(IOutputStream& out, tie_here_jams::Direction direction)
{
    out << ToString(static_cast<size_t>(direction));
}

Y_UNIT_TEST_SUITE(HereDataReaderTest)
{
    Y_UNIT_TEST(DirectedPointTest)
    {
        auto directedPoint = tie_here_jams::DirectedPoint::fromTrafficCD("+933+00004");
        EXPECT_EQ(directedPoint.pointId, "00004");
        EXPECT_EQ(directedPoint.direction, tie_here_jams::Direction::Positive);

        directedPoint = tie_here_jams::DirectedPoint::fromTrafficCD("-933+00004");
        EXPECT_EQ(directedPoint.pointId, "00004");
        EXPECT_EQ(directedPoint.direction, tie_here_jams::Direction::Positive);

        directedPoint = tie_here_jams::DirectedPoint::fromTrafficCD("+933-00004");
        EXPECT_EQ(directedPoint.pointId, "00004");
        EXPECT_EQ(directedPoint.direction, tie_here_jams::Direction::Negative);

        directedPoint = tie_here_jams::DirectedPoint::fromTrafficCD("-933-00004");
        EXPECT_EQ(directedPoint.pointId, "00004");
        EXPECT_EQ(directedPoint.direction, tie_here_jams::Direction::Negative);

        directedPoint = tie_here_jams::DirectedPoint::fromTrafficCD("-933P00004");
        EXPECT_EQ(directedPoint.pointId, "00004");
        EXPECT_EQ(directedPoint.direction, tie_here_jams::Direction::Positive);

        directedPoint = tie_here_jams::DirectedPoint::fromTrafficCD("-933P00004");
        EXPECT_EQ(directedPoint.pointId, "00004");
        EXPECT_EQ(directedPoint.direction, tie_here_jams::Direction::Positive);

        directedPoint = tie_here_jams::DirectedPoint::fromTrafficCD("-933N00004");
        EXPECT_EQ(directedPoint.pointId, "00004");
        EXPECT_EQ(directedPoint.direction, tie_here_jams::Direction::Negative);

        directedPoint = tie_here_jams::DirectedPoint::fromTrafficCD("-933N00004");
        EXPECT_EQ(directedPoint.pointId, "00004");
        EXPECT_EQ(directedPoint.direction, tie_here_jams::Direction::Negative);
    }

    Y_UNIT_TEST(HereDataReader)
    {
        tie_here_jams::HereReader hereReader;

        hereReader.addPartitionReader(TRAFFIC_DBF_FILE, STREETS_SHP_FILE, STREETS_DBF_FILE);

        tie_here_jams::DirectedPoint2Polyline directedPoint2Polyline = hereReader.getAllFeatures(
            LOCATION_TABLE_ID
        );

        EXPECT_EQ(CANON_DIRECTED_POINT_TO_POLYLINE.size(), directedPoint2Polyline.size());
        for (const auto& [canonPoint, canonPolyline] : CANON_DIRECTED_POINT_TO_POLYLINE) {
            auto iter = directedPoint2Polyline.find(canonPoint);
            EXPECT_NE(iter, directedPoint2Polyline.end());
            EXPECT_EQUAL_POLYLINES(iter->second, canonPolyline);
        }
    }
}
