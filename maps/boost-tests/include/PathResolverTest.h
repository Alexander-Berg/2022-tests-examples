#pragma once

#include <boost/test/unit_test.hpp>

namespace maps { namespace renderer5 { namespace test { namespace map
{
    // test on task: MAPSCORE-3023
    void resolvingResourcePathTest();

    // test on task: MAPSRENDER-476
    void patchRelativePathTest();

    boost::unit_test::test_suite* initPathResolverTestSuite();

} } } } // maps::renderer5::test::map
