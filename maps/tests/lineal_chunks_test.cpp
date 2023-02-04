#include "../test_tools/io_std.h"

#include "../cut/cut_line.h"
#include "../cut/lineal_chunks.h"

#include <maps/libs/geolib/include/point.h>
#include <maps/libs/geolib/include/segment.h>

#include <boost/test/unit_test.hpp>

#include <initializer_list>
#include <set>
#include <vector>

using namespace maps::coverage5;
using namespace maps::coverage5::cut;
using namespace maps::geolib3;

typedef std::vector<LinealChunk> LinealChunksVector;

struct PointTest {
    double coord;
    Location location;
};

struct SegmentTest {
    double start;
    double end;
    std::set<Location> locations;
};

typedef std::vector<PointTest> PointTests;
typedef std::vector<SegmentTest> SegmentTests;

struct LinealChunksTestData {
    LinealChunksVector chunks;
    PointTests pointTests;
    SegmentTests segmentTests;
};

LinealChunksTestData g_linealchunksTestData[] {
    LinealChunksTestData {
        LinealChunksVector {
            {0.0,  INTERIOR},
            {1.25, EXTERIOR},
            {2.75, INTERIOR},
            {4.0, EXTERIOR}
        },
        PointTests {
            PointTest {-1.0, EXTERIOR},
            PointTest {0.0,  EXTERIOR},
            PointTest {1.0,  INTERIOR},
            PointTest {1.25, EXTERIOR},
            PointTest {2.0,  EXTERIOR},
            PointTest {2.75, EXTERIOR},
            PointTest {3.0,  INTERIOR},
            PointTest {4.0,  EXTERIOR},
            PointTest {5.0,  EXTERIOR}
        },
        SegmentTests {
            SegmentTest {-1.0,  1.0,    {EXTERIOR, INTERIOR} },
            SegmentTest {0.5,   1.0,    {INTERIOR} },
            SegmentTest {0.0,   0.5,    {INTERIOR} },
            SegmentTest {1.25,  2.75,   {EXTERIOR} },
            SegmentTest {-1.0,  1.25,   {EXTERIOR, INTERIOR} },
            SegmentTest {2.75,  4.0,    {INTERIOR} },
            SegmentTest {4.0,   5.0,    {EXTERIOR} },
            SegmentTest {2.75,  4.000005, {EXTERIOR, INTERIOR} },
            SegmentTest {5.0,   6.0,    {EXTERIOR} }
        }
    },
    LinealChunksTestData {
        LinealChunksVector {
            {-4.0, INTERIOR},
            {-3.000005, EXTERIOR},
            {-3.0, INTERIOR},
            {-1.0, BORDER},
            {1.0,  EXTERIOR},
            {2.0,  INTERIOR},
            {4.25, EXTERIOR},
            {6.0,  BORDER},
            {7.0,  EXTERIOR}
        },
        PointTests {
            PointTest {-5.0, EXTERIOR},
            PointTest {-4.0, EXTERIOR},
            PointTest {-3.0, EXTERIOR},
            PointTest {-2.5, INTERIOR},
            PointTest {-1.0, EXTERIOR},
            PointTest {0.0,  BORDER},
            PointTest {1.0,  EXTERIOR},
            PointTest {1.5,  EXTERIOR},
            PointTest {2.0,  EXTERIOR},
            PointTest {4.0,  INTERIOR},
            PointTest {4.25, EXTERIOR},
            PointTest {5.0,  EXTERIOR},
            PointTest {6.0,  EXTERIOR},
            PointTest {6.5,  BORDER},
            PointTest {7.0,  EXTERIOR},
            PointTest {8.0,  EXTERIOR}
        },
        SegmentTests {
            SegmentTest {-4.0, 1.0, {EXTERIOR, BORDER, INTERIOR} },
            SegmentTest {2.0, 6.0,  {EXTERIOR, INTERIOR} },
            SegmentTest {-4.0, 7.0, {EXTERIOR, BORDER, INTERIOR} }
        }
    }
};

BOOST_AUTO_TEST_CASE(test_chunks_on_cut)
{
    for (auto testData: g_linealchunksTestData) {
        StandaloneLinealChunks::ChunksVector chunksVector(
            testData.chunks.begin(), testData.chunks.end());
        StandaloneLinealChunks chunks(chunksVector);
        CutLine line(geom::X, 0.0);
        for (auto pointTest: testData.pointTests) {
            Point2 point(0.0, pointTest.coord);
            Location res = chunks.where(point, line);
            BOOST_CHECK_MESSAGE(res == pointTest.location,
                "Point: " << point <<
                "\n\texpected: " << pointTest.location <<
                "\n\treceived: " << res << "\n");
        }
        for (auto segmentTest: testData.segmentTests) {
            Point2 start(0.0, segmentTest.start);
            Point2 end(0.0, segmentTest.end);
            Segment2 segment(start, end);
            std::set<Location> res(chunks.where(segment, line));
            BOOST_CHECK_MESSAGE(res == segmentTest.locations,
                "Segment: " << segment <<
                "\n\texpected: " << segmentTest.locations <<
                "\n\treceived: " << res << "\n");
        }
    }
}
