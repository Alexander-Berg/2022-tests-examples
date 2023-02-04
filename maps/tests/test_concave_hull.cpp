#include <library/cpp/testing/unittest/registar.h>
#include <library/cpp/testing/gmock_in_unittest/gmock.h>

#include <maps/libs/concave_hull/include/concave_hull.h>

#include <maps/libs/geolib/include/bounding_box.h>
#include <maps/libs/geolib/include/convex_hull.h>
#include <maps/libs/geolib/include/distance.h>
#include <maps/libs/geolib/include/linear_ring.h>
#include <maps/libs/geolib/include/polygon.h>
#include <maps/libs/geolib/include/spatial_relation.h>

#include <maps/libs/geolib/include/test_tools/test_tools.h>
#include <maps/libs/geolib/include/test_tools/io_operations.h>

#include <iomanip>
#include <sstream>


namespace geo = maps::geolib3;
namespace gtt = maps::geolib3::test_tools;
namespace maps::geolib3 {

using io::operator <<;

} //namespace maps::geolib3


namespace maps::concave_hull {

namespace {

constexpr const double EPS = 0.00001;

template <class Value>
bool check(const Value& value, const Value& expected)
{
    static auto checker = []{
        gtt::ValueChecker<gtt::EmptyGeometry> checker{gtt::EmptyGeometry{}};
        checker.setPrecision(EPS);
        return checker;
    }();

    return checker.check("geometry", expected, value);
}


std::string easyview(const Hull& hull, std::string_view id)
{
    std::stringstream stream;
    stream << std::fixed << std::setprecision(7);
    for (const auto& ring : hull) {
        for (const auto& pt : ring) {
            stream << pt.x() << " " << pt.y() << " ";
        }
        stream << id << std::endl;
    }
    return stream.str();
}


void fillPolygon(
    geo::PointsVector& result,
    const geo::Polygon2& polygon,
    double step = 0.001)
{
    auto bbox = polygon.boundingBox();
    for (double x = bbox.minX(); x <= bbox.maxX(); x += step)
        for (double y = bbox.minY(); y <= bbox.maxY(); y += step)
        {
            geo::Point2 point{x, y};
            if (geo::spatialRelation(polygon, point, geo::Contains)) {
                result.push_back(point);
            }
        }
}

} // namespace


Y_UNIT_TEST_SUITE(TestConcaveHull)
{

Y_UNIT_TEST(TestSquare)
{
    geo::PointsVector points = {
        {1, 1},
        {1, 2},
        {2, 2},
        {2, 1},
    };

    Hull expected = {{
        {1, 1},
        {1, 2},
        {2, 2},
        {2, 1},
        {1, 1},
    }};
    EXPECT_TRUE(check(
        concaveHull(points, 0.75),
        expected
    ));

    EXPECT_TRUE(check(
        concaveHull(points, 0.7),
        {}
    ));
}


Y_UNIT_TEST(TestConcaveShape)
{
    geo::PointsVector points = {
        {27.500, 53.900},
        {27.505, 53.900},
        {27.505, 53.905},
        {27.510, 53.905},
        {27.510, 53.895},
        {27.500, 53.895},
    };
    EXPECT_TRUE(check(
        geoConcaveHull(points, 500),
        {}
    ));

    // https://yandex.by/maps/157/minsk/?z=15&ll=27.5,53.9&rl=27.5,53.9~0.00499912,0.00499987~0.00500137,0~-0.00500137,-0.00499987~-0.00499912,-0.00500047~0,0.00500047
    Hull expected600 = {{
        {27.500, 53.895},
        {27.500, 53.900},
        {27.505, 53.905},
        {27.510, 53.905},
        {27.505, 53.900},
        {27.500, 53.895}
    }};
    EXPECT_TRUE(check(
        geoConcaveHull(points, 600),
        expected600
    ));

    // https://yandex.by/maps/157/minsk/?z=15&ll=27.5,53.9&rl=27.5,53.9~0.00499912,0.00499987~0.00500137,0~-0.00500137,-0.00499987~0.00500137,-0.00500047~-0.0100005,0~0,0.00500047
    std::vector<std::vector<maps::geolib3::Point2>> expected800 = {{
        {27.500, 53.895},
        {27.500, 53.900},
        {27.505, 53.905},
        {27.510, 53.905},
        {27.505, 53.900},
        {27.510, 53.895},
        {27.500, 53.895}
    }};
    EXPECT_TRUE(check(
        geoConcaveHull(points, 800),
        expected800
    ));

    // https://yandex.by/maps/157/minsk/?z=15&ll=27.5,53.9&rl=27.5,53.9~0.00499912,0.00499987~0.00500137,0~0,-0.0100003~-0.0100005,0~0,0.00500047
    Hull expected1500 = {{
        {27.500, 53.895},
        {27.500, 53.900},
        {27.505, 53.905},
        {27.510, 53.905},
        {27.510, 53.895},
        {27.500, 53.895}
    }};
    EXPECT_TRUE(check(
        geoConcaveHull(points, 1500),
        expected1500
    ));
}


Y_UNIT_TEST(TestABC)
{
    // https://yandex.by/maps/213/moscow/?ll=37.723499%2C55.765796&rl=37.37543243%2C55.71541263~0.03958589%2C0.10124507~0.04441344%2C0.00163082~0.06179261%2C-0.09197665~-0.05117201%2C-0.00163469~-0.01448264%2C0.02614681~-0.02896529%2C-0.00108910~-0.01062060%2C-0.03105217~0.01158611%2C0.04629677~0.00675857%2C0.02122359~0.01641366%2C-0.02067925&z=10.508274978155818
    geo::Polygon2 polygonA{
        geo::LinearRing2{{
            {37.37543243, 55.71541263},
            {37.41501832, 55.81665770},
            {37.45943176, 55.81828852},
            {37.52122437, 55.72631187},
            {37.47005236, 55.72467718},
            {37.45556972, 55.75082399},
            {37.42660443, 55.74973489},
            {37.41598383, 55.71868272},
        }},
        {
            geo::LinearRing2{{
                {37.42756994, 55.76497949},
                {37.43432851, 55.78620308},
                {37.45074217, 55.76552383},
            }}
        }
    };

    // https://yandex.by/maps/213/moscow/?ll=37.723499%2C55.765796&rl=37.59200639%2C55.73152121~-0.02540588%2C0.08785320~0.04119873%2C0.00386561~0.02059937%2C-0.00618517~0.01235962%2C-0.01392023~-0.00411987%2C-0.01547279~-0.01716614%2C-0.00735174~0.02609253%2C-0.00309589~0.01304626%2C-0.01122465~0.00343323%2C-0.01819858~-0.01991272%2C-0.01007096~-0.03158569%2C-0.00271186~0.00137329%2C0.01743008~-0.00274658%2C0.01161571~0.01922607%2C0.00000000~0.00617981%2C-0.00580742~-0.03570557%2C0.03405779~-0.00549316%2C0.01237724~0.02265930%2C-0.00464100&z=11
    geo::Polygon2 polygonB{
        geo::LinearRing2{{
            {37.59200639, 55.73152121},
            {37.56660051, 55.81937441},
            {37.60779924, 55.82324002},
            {37.62839861, 55.81705485},
            {37.64075823, 55.80313462},
            {37.63663836, 55.78766183},
            {37.61947222, 55.78031009},
            {37.64556475, 55.77721420},
            {37.65861101, 55.76598955},
            {37.66204424, 55.74779097},
            {37.64213152, 55.73772001},
            {37.61054583, 55.73500815},
        }},
        {
            geo::LinearRing2{{
                {37.61191912, 55.75243823},
                {37.60917254, 55.76405394},
                {37.62839861, 55.76405394},
                {37.63457842, 55.75824652},
            }},
            geo::LinearRing2{{
                {37.59887285, 55.79230431},
                {37.59337969, 55.80468155},
                {37.61603899, 55.80004055},
            }}
        }
    };

    // https://yandex.by/maps/213/moscow/?ll=37.723499%2C55.765796&rl=37.75336808%2C55.80081410~-0.01579285%2C0.01005460~-0.01991272%2C0.00154663~-0.02059937%2C-0.00077331~-0.02197266%2C-0.01121469~-0.00343323%2C-0.01237860~0.00068665%2C-0.01393065~0.00411987%2C-0.01548437~0.00961304%2C-0.01161733~0.01991272%2C-0.00890897~0.04257202%2C0.00697241~0.01235962%2C0.01394107~-0.01373291%2C0.00696866~-0.01853943%2C-0.00813022~-0.02471924%2C0.00038719~-0.00549316%2C0.01703263~-0.00068665%2C0.01547773~0.01304626%2C0.00580256~0.01922607%2C0.00116041~0.00961304%2C-0.00348133&z=11
    geo::Polygon2 polygonC{
        geo::LinearRing2{{
            {37.75336808, 55.80081410},
            {37.73757523, 55.81086870},
            {37.71766251, 55.81241533},
            {37.69706314, 55.81164202},
            {37.67509048, 55.80042733},
            {37.67165725, 55.78804873},
            {37.67234390, 55.77411808},
            {37.67646377, 55.75863371},
            {37.68607681, 55.74701638},
            {37.70598953, 55.73810741},
            {37.74856155, 55.74507982},
            {37.76092117, 55.75902089},
            {37.74718826, 55.76598955},
            {37.72864883, 55.75785933},
            {37.70392959, 55.75824652},
            {37.69843643, 55.77527915},
            {37.69774978, 55.79075688},
            {37.71079604, 55.79655944},
            {37.73002211, 55.79771985},
            {37.73963515, 55.79423852},
        }},
        {}
    };

    geo::PointsVector points;
    fillPolygon(points, polygonA);
    fillPolygon(points, polygonB);
    fillPolygon(points, polygonC);

    // Original shape, all letters separate
    auto hull400 = geoConcaveHull(points, 400);
    // Use following command to see results in easyview:
    // grep 'EASYVIEW400' <./test-results/maps-libs-concave_hull-tests/testing_out_stuff/TestgeoConcaveHull.TestABC.out | easyview
    std::cout << easyview(hull400, "EASYVIEW400") << std::endl;
    EXPECT_EQ(hull400.size(), 6u);

    auto multipolygon400 = geoConcaveMultiPolygon(points, 400);
    ASSERT_EQ(multipolygon400.polygonsNumber(), 3u);
    EXPECT_EQ(multipolygon400.polygonAt(0).interiorRingsNumber(), 1u); // A
    EXPECT_EQ(multipolygon400.polygonAt(1).interiorRingsNumber(), 0u); // C
    EXPECT_EQ(multipolygon400.polygonAt(2).interiorRingsNumber(), 2u); // B

    auto filteredMultipolygon400 = geoConcaveMultiPolygon(points, 400, /*filterPoints*/ true);
    ASSERT_EQ(filteredMultipolygon400.polygonsNumber(), 3u);
    EXPECT_EQ(filteredMultipolygon400.polygonAt(0).interiorRingsNumber(), 1u); // A
    EXPECT_EQ(filteredMultipolygon400.polygonAt(1).interiorRingsNumber(), 0u); // C
    EXPECT_EQ(filteredMultipolygon400.polygonAt(2).interiorRingsNumber(), 2u); // B

    EXPECT_TRUE(geo::spatialRelation(multipolygon400, filteredMultipolygon400, geo::Equals));

     // 1 hole in B has disappeared
    auto hull800 = geoConcaveHull(points, 800);
    std::cout << easyview(hull800, "EASYVIEW800") << std::endl;
    EXPECT_EQ(hull800.size(), 5u);

    auto multipolygon800 = geoConcaveMultiPolygon(points, 800);
    ASSERT_EQ(multipolygon800.polygonsNumber(), 3u);
    EXPECT_EQ(multipolygon800.polygonAt(0).interiorRingsNumber(), 1u); // A
    EXPECT_EQ(multipolygon800.polygonAt(1).interiorRingsNumber(), 0u); // C
    EXPECT_EQ(multipolygon800.polygonAt(2).interiorRingsNumber(), 1u); // B

    auto filteredMultipolygon800 = geoConcaveMultiPolygon(points, 800, /*filterPoints*/ true);
    ASSERT_EQ(filteredMultipolygon800.polygonsNumber(), 3u);
    EXPECT_EQ(filteredMultipolygon800.polygonAt(0).interiorRingsNumber(), 1u); // A
    EXPECT_EQ(filteredMultipolygon800.polygonAt(1).interiorRingsNumber(), 0u); // C
    EXPECT_EQ(filteredMultipolygon800.polygonAt(2).interiorRingsNumber(), 1u); // B

    EXPECT_TRUE(geo::spatialRelation(multipolygon800, filteredMultipolygon800, geo::Equals));

    // B and C merged, B have no holes
    auto hull1000 = geoConcaveHull(points, 1000);
    std::cout << easyview(hull1000, "EASYVIEW1000") << std::endl;
    EXPECT_EQ(hull1000.size(), 3u);

    auto multipolygon1000 = geoConcaveMultiPolygon(points, 1000);
    ASSERT_EQ(multipolygon1000.polygonsNumber(), 2u);
    EXPECT_EQ(multipolygon1000.polygonAt(0).interiorRingsNumber(), 1u); // A
    EXPECT_EQ(multipolygon1000.polygonAt(1).interiorRingsNumber(), 0u); // BC

    auto filteredMultipolygon1000 = geoConcaveMultiPolygon(points, 1000, /*filterPoints*/ true);
    ASSERT_EQ(filteredMultipolygon1000.polygonsNumber(), 2u);
    EXPECT_EQ(filteredMultipolygon1000.polygonAt(0).interiorRingsNumber(), 1u); // A
    EXPECT_EQ(filteredMultipolygon1000.polygonAt(1).interiorRingsNumber(), 0u); // BC

    EXPECT_TRUE(geo::spatialRelation(multipolygon1000, filteredMultipolygon1000, geo::Equals));

    // No holes
    auto hull1200 = geoConcaveHull(points, 1200);
    std::cout << easyview(hull1200, "EASYVIEW1200") << std::endl;
    EXPECT_EQ(hull1200.size(), 2u);

    auto multipolygon1200 = geoConcaveMultiPolygon(points, 1200);
    ASSERT_EQ(multipolygon1200.polygonsNumber(), 2u);
    EXPECT_EQ(multipolygon1200.polygonAt(0).interiorRingsNumber(), 0u); // BC
    EXPECT_EQ(multipolygon1200.polygonAt(1).interiorRingsNumber(), 0u); // A

    auto filteredMultipolygon1200 = geoConcaveMultiPolygon(points, 1200, /*filterPoints*/ true);
    ASSERT_EQ(filteredMultipolygon1200.polygonsNumber(), 2u);
    EXPECT_EQ(filteredMultipolygon1200.polygonAt(0).interiorRingsNumber(), 0u); // BC
    EXPECT_EQ(filteredMultipolygon1200.polygonAt(1).interiorRingsNumber(), 0u); // A

    EXPECT_TRUE(geo::spatialRelation(multipolygon1200, filteredMultipolygon1200, geo::Equals));

    // Single polygon
    auto hull5000 = geoConcaveHull(points, 5000);
    std::cout << easyview(hull5000, "EASYVIEW5000") << std::endl;
    EXPECT_EQ(hull5000.size(), 1u);

    auto multipolygon5000 = geoConcaveMultiPolygon(points, 5000);
    ASSERT_EQ(multipolygon5000.polygonsNumber(), 1u);
    EXPECT_EQ(multipolygon5000.polygonAt(0).interiorRingsNumber(), 0u); // ABC

    auto filteredMultipolygon5000 = geoConcaveMultiPolygon(points, 5000, /*filterPoints*/ true);

    ASSERT_EQ(filteredMultipolygon5000.polygonsNumber(), 1u);
    EXPECT_EQ(filteredMultipolygon5000.polygonAt(0).interiorRingsNumber(), 0u); // ABC

    EXPECT_TRUE(geo::spatialRelation(multipolygon5000, filteredMultipolygon5000, geo::Equals));

    auto convexHull = geo::bufferedConvexHull(points, EPS);
    EXPECT_TRUE(geo::spatialRelation(convexHull, geo::Polygon2{hull5000.front()}, geo::Contains));
    EXPECT_TRUE(geo::spatialRelation(convexHull, multipolygon5000, geo::Contains));
    EXPECT_TRUE(geo::spatialRelation(multipolygon5000, multipolygon1200, geo::Contains));
    EXPECT_TRUE(geo::spatialRelation(multipolygon1200, multipolygon1000, geo::Contains));
    EXPECT_TRUE(geo::spatialRelation(multipolygon1000, multipolygon800, geo::Contains));
    EXPECT_TRUE(geo::spatialRelation(multipolygon800, multipolygon400, geo::Contains));
}


Y_UNIT_TEST(TestExactCircumcircleRadius)
{
    // The circumcircle radius is strictly equal to 10 here
    // But circumcircleRadius returns 10.00000000000003 or 9.99999999999994
    // depending on the edges order because or doubles rounding error.
    geo::PointsVector points = {
        {4181885, 7472669},
        {4181883, 7472667},
        {4181881, 7472661},
    };

    Hull expected = {{
        {4181885, 7472669},
        {4181881, 7472661},
        {4181883, 7472667},
        {4181885, 7472669},
    }};
    EXPECT_TRUE(check(
        concaveHull(points, 10 + EPS),
        expected
    ));

    EXPECT_TRUE(check(
        concaveHull(points, 10 - EPS),
        {}
    ));

    // We do not care wether we include or exclude the triangle here
    EXPECT_TRUE(check(
        concaveHull(points, 10),
        {}
    ));
}


Y_UNIT_TEST(TestTouchingCycles)
{
    geo::PointsVector points = {
        {0, 0},
        {1, 0},
        {1, 1},
        {1, 2},
        {2, 2},
        {3, 2},
        {3, 3},
    };

    Hull expected = {
        {
            {2, 2},
            {3, 3},
            {3, 2},
            {2, 2},
        },
        {
            {1, 1},
            {1, 2},
            {2, 2},
            {1, 1},
        },
        {
            {0, 0},
            {1, 1},
            {1, 0},
            {0, 0},
        },
    };
    EXPECT_TRUE(check(
        concaveHull(points, 0.75),
        expected
    ));
}


Y_UNIT_TEST(TestDisconnecterInterior)
{
    // Hole splits exterior to separate polygons
    //  *-*-*
    //  |/ \|
    //  *   *
    //  |\ /|
    //  *-*-*
    geo::PointsVector points = geo::PointsVector{
        {0, 0},
        {2, 0},
        {2, 1},
        {2, 2},
        {0, 1},
        {0, 2},
        {1, 0},
        {1, 2},

    };

    // concaveHull may return hole and exterior or 4 triangles
    Hull expectedHull = {
        geo::PointsVector{
            {1, 2},
            {2, 2},
            {2, 1},
            {1, 2}
        },
        geo::PointsVector{
            {0, 1},
            {0, 2},
            {1, 2},
            {0, 1}
        },
        geo::PointsVector{
            {1, 0},
            {2, 1},
            {2, 0},
            {1, 0}
        },
        geo::PointsVector{
            {0, 0},
            {0, 1},
            {1, 0},
            {0, 0}
        }
    };
    EXPECT_TRUE(check(
        concaveHull(points, 0.75),
        expectedHull
    ));

    // OGC Simple Features requires polygon interior to be connected
    // So the hole splits is to 4 polygones
    geo::MultiPolygon2 expectedMultipolygon{{
        geo::Polygon2{{
            {0, 0},
            {0, 1},
            {1, 0},
            {0, 0}
        }},
        geo::Polygon2{{
            {1, 0},
            {2, 1},
            {2, 0},
            {1, 0}
        }},
        geo::Polygon2{{
            {0, 1},
            {0, 2},
            {1, 2},
            {0, 1}
        }},
        geo::Polygon2{{
            {1, 2},
            {2, 2},
            {2, 1},
            {1, 2}
        }},
    }};

    EXPECT_TRUE(check(
        geo::convertGeodeticToMercator(
            geoConcaveMultiPolygon(
                geo::convertMercatorToGeodetic(points),
                0.75
            )
        ),
        expectedMultipolygon
    ));

    EXPECT_TRUE(check(
        geo::convertGeodeticToMercator(
            geoConcaveMultiPolygon(
                geo::convertMercatorToGeodetic(points),
                0.75,
                true /*filterPoints*/
            )
        ),
        expectedMultipolygon
    ));

}


Y_UNIT_TEST(TestInnerPolygon)
{
    // Holes split exterior to separate polygons and create new polygon inside
    //  *-*-*-*-*
    //  |/ \|/ \|
    //  *   *   *
    //  |\ /|\ /|
    //  *-*-*-*-*
    //  |/ \|/ \|
    //  *   *   *
    //  |\ /|\ /|
    //  *-*-*-*-*
    geo::PointsVector points = geo::convertMercatorToGeodetic(geo::PointsVector{
        {0, 0},
        {0, 1},
        {0, 2},
        {0, 3},
        {0, 4},
        {1, 0},
        {1, 2},
        {1, 4},
        {2, 0},
        {2, 1},
        {2, 2},
        {2, 3},
        {2, 4},
        {3, 0},
        {3, 2},
        {3, 4},
        {4, 0},
        {4, 1},
        {4, 2},
        {4, 3},
        {4, 4},
    });

    // 8 triangles and central square
    EXPECT_EQ(geoConcaveMultiPolygon(points, 0.75).polygonsNumber(), 9u);
    EXPECT_EQ(geoConcaveMultiPolygon(points, 0.75, /*filterPoints*/ true).polygonsNumber(), 9u);
}


Y_UNIT_TEST(TestSameLine)
{
    // 3 points on the same line
    // 120 seconds walking area near 27.555371,53.906276
    geo::PointsVector points{
        {27.5558393, 53.9060793},
        {27.5558101, 53.9060819},
        {27.5558393, 53.9058167},
        {27.5558393, 53.9062119},
        {27.5558101, 53.9062092},
        {27.5558685, 53.9060819}
    };

    geo::MultiPolygon2 expectedMultipolygon{{
        geo::Polygon2{{
            {27.5558101, 53.9060819},
            {27.5558101, 53.9062092},
            {27.5558393, 53.9062119},
            {27.5558685, 53.9060819},
            {27.5558393, 53.9058167}
        }}
    }};

    EXPECT_TRUE(check(
        geoConcaveMultiPolygon(
            points,
            400
        ),
        expectedMultipolygon
    ));

    EXPECT_TRUE(check(
        geoConcaveMultiPolygon(
            points,
            400,
            true /*filterPoints*/
        ),
        expectedMultipolygon
    ));
}

Y_UNIT_TEST(TestFilterInnerPoints)
{
    
    geo::Polygon2 polygon{{
        {-1.0005, -1.0005},
        {-1.0005, 1},
        {1, 1},
        {1, -1.0005}
    }};

    geo::PointsVector points;
    fillPolygon(points, polygon, 0.001); // 2000*2000 points
    EXPECT_EQ(points.size(), 2000u * 2000u);

    auto filteredPoints = filterInnerPoints(points, 0.005); 
    // replace all inner buckets with one point (i.e. 25 points -> 1 point)
    EXPECT_EQ(filteredPoints.size(), 2000u * 2000u - 398u * 398u * 24u);

    // Test with empty point set
    points.clear();
    filteredPoints = filterInnerPoints(points, 0.005);
    EXPECT_EQ(filteredPoints.size(), 0u);

    // Test with one point
    points.push_back({-1., 0.});
    filteredPoints = filterInnerPoints(points, 0.005);
    EXPECT_EQ(filteredPoints.size(), 1u);

    // Test with two equal points
    points.push_back({-1., 0.});
    filteredPoints = filterInnerPoints(points, 0.005);
    EXPECT_EQ(filteredPoints.size(), 2u);

    // Three points on one line in far different buckets
    points.pop_back();
    points.push_back({0., 0.});
    points.push_back({1., 0.});
    filteredPoints = filterInnerPoints(points, 0.005);
    EXPECT_EQ(filteredPoints.size(), 3u);

    // Three points on one line in one bucket
    filteredPoints = filterInnerPoints(points, 3.);
    EXPECT_EQ(filteredPoints.size(), 3u);
}

}

} // namespace maps::concave_hull
