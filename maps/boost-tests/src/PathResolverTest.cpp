#include "../include/PathResolverTest.h"

#include <boost/filesystem.hpp>

using namespace boost::unit_test;
using namespace maps::renderer5;
using namespace maps::renderer5::test;
namespace fs = boost::filesystem;

test_suite * map::initPathResolverTestSuite()
{
    test_suite * suite = BOOST_TEST_SUITE("Path resolver test suite");

    // TBD: copy tests from maps_branch_avlasyuk-MAPSRENDER-508/renderer-test

    return suite;
}
