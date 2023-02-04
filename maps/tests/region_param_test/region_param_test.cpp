#define BOOST_TEST_MAIN

#include <maps/analyzer/libs/common/include/region_param.h>

#include <library/cpp/testing/common/env.h>

#include <boost/test/unit_test.hpp>

#include <boost/test/floating_point_comparison.hpp>
#include <boost/assign/std/vector.hpp>

using namespace std;
using namespace boost::assign;
using namespace maps::xml3;
namespace ma = maps::analyzer;

const std::string TEST_DATA_ROOT = ArcadiaSourceRoot() + "/maps/analyzer/libs/common/tests/";

BOOST_AUTO_TEST_CASE(RegionParamTestDefault) {
    ma::RegionParam<int> x(5);
    BOOST_CHECK_EQUAL(x, 5);
    BOOST_CHECK_EQUAL(x[0], 5);
    BOOST_CHECK_EQUAL(x[10], 5);
}

BOOST_AUTO_TEST_CASE(RegionParamTest) {
    Doc doc = Doc(TEST_DATA_ROOT + "test_xml/region_param.xml");
    Node root = doc.root();
    ma::RegionParam<int> actTime(root.nodes("actual_time"));
    BOOST_CHECK_EQUAL(actTime[0], 10);
    BOOST_CHECK_EQUAL(actTime[1], 11);
    BOOST_CHECK_EQUAL(actTime[2], 30);
    BOOST_CHECK_EQUAL(actTime[12], 30);

    ma::RegionParam<string> zTime(root.nodes("z_time"));
    BOOST_CHECK_EQUAL(zTime[0], "desqt");
    BOOST_CHECK_EQUAL(zTime[1], "west");
    BOOST_CHECK_EQUAL(zTime[2], "dvenadcat");
    BOOST_CHECK_EQUAL(zTime[12], "dvenadcat");

    ma::RegionParam<double> defTime(root.nodes("def_time"));
    BOOST_CHECK_EQUAL(defTime[0], 12.5);
    BOOST_CHECK_EQUAL(defTime[2], 12.5);
    BOOST_CHECK_EQUAL(defTime, 12.5);
}

BOOST_AUTO_TEST_CASE(RegionParamTestThrow) {
    Doc doc = Doc(TEST_DATA_ROOT + "test_xml/region_param.xml");
    Node root = doc.root();
    ma::RegionParam<int> actTime(root.nodes("actual_time"));

    // removing assinging to `std::ignore` creates compilation error:
    // equality comparison result unused [-Wunused-comparison]
    BOOST_CHECK_THROW(std::ignore = actTime == ma::RegionParam<int>(0), maps::Exception);

    BOOST_CHECK_THROW(
        ma::RegionParam<double> defTimeInc(root.nodes("def_time_inc")),
        maps::Exception
        );

    BOOST_CHECK_THROW(
        ma::RegionParam<double> dr(root.nodes("dr")),
        maps::Exception
        );

    BOOST_CHECK_THROW(
        ma::RegionParam<double> nd(root.nodes("nd")),
        maps::Exception
        );

    BOOST_CHECK_THROW(
        ma::RegionParam<double> deft(root.nodes("z_time")),
        maps::Exception
        );

}
