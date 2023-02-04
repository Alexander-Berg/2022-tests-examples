#pragma once

#include <boost/test/unit_test.hpp>

namespace maps { namespace renderer5 { namespace test { namespace map
{
    void createAndCloseMapTest();

    void destroyDynamicMapTest();

    boost::unit_test::test_suite * initMapDestroySuite();

} } } } // namespace maps::renderer5::test::map
