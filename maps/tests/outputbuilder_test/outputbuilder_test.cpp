#define BOOST_TEST_MAIN
#include "../test_tools.h"

#include <boost/test/auto_unit_test.hpp>

#include <maps/analyzer/libs/common/include/exception.h>
#include <maps/analyzer/services/jams_analyzer/modules/outputbuilder/lib/output_builder.h>

namespace ma = maps::analyzer;


BOOST_AUTO_TEST_CASE( no_resources )
{
    OutputBuilder outputBuilder(makeOutputBuilderConfig("outputbuilder.conf")); // no exception
    BOOST_CHECK_THROW(OutputBuilder(makeOutputBuilderConfig("outputbuilder.conf", NO_GRAPH_DATA)), ma::NoExternalResourceError);
    BOOST_CHECK_THROW(OutputBuilder(makeOutputBuilderConfig("outputbuilder.conf", NO_EDGES_PERS_IDX)), ma::NoExternalResourceError);
    BOOST_CHECK_THROW(OutputBuilder(makeOutputBuilderConfig("outputbuilder.conf", NO_GEODATA)), ma::NoExternalResourceError);
}
