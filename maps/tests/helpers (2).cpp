#include "helpers.h"

#include <boost/test/unit_test.hpp>
#include <boost/test/unit_test_log.hpp>
#include <boost/test/test_tools.hpp>

#include <maps/libs/geolib/include/spatial_relation.h>
#include <maps/libs/geolib/include/serialization.h>

#include <map>

namespace maps {
namespace wiki {
namespace geom_tools {
namespace test {

void
checkPolygons(
    const std::string& message,
    const GeolibPolygonVector& expected, const GeolibPolygonVector& received)
{
    BOOST_CHECK_MESSAGE(expected.size() == received.size(),
        message << " result size mismatch, expected " << expected.size()
            << ", received " << received.size());

    std::map<size_t, size_t> expectedToReceivedMatch;
    std::map<size_t, size_t> receivedToExpectedMatch;

    for (size_t i = 0; i < expected.size(); ++i) {
        size_t j;
        for (j = 0; j < received.size(); ++j) {
            if (!geolib3::spatialRelation(expected[i], received[j], geolib3::Equals)) {
                continue;
            }
            expectedToReceivedMatch.insert({i, j});
            receivedToExpectedMatch.insert({j, i});
            break;
        }
        BOOST_CHECK_MESSAGE(j < received.size(),
            message << ": " << i << "-th expected polygon is missing: "
                << geolib3::WKT::toString(expected[i]));
    }

    for (size_t j = 0; j < received.size(); ++j) {
        BOOST_CHECK_MESSAGE(receivedToExpectedMatch.count(j),
            message << ": " << j << "-th received polygon is unexpected: "
                << geolib3::WKT::toString(received[j]));
    }
}

} // namespace test
} // namespace geom_tools
} // namespace wiki
} // namespace maps
