#include <maps/libs/geolib/include/point.h>
#include <yandex/maps/geolib3/proto.h>
#include <yandex/maps/geolib3/sproto.h>

#include <maps/libs/geolib/include/test_tools/io_operations.h>
#include <maps/libs/geolib/include/spatial_relation.h>

#include <maps/libs/common/include/exception.h>

#include <boost/test/unit_test.hpp>

#include <sstream>

using namespace maps::geolib3;
using namespace maps::geolib3::io;


bool spatialRelation(const LinearRing2& lhs,
        const LinearRing2&  rhs,
        SpatialRelation relation) {
    return spatialRelation(Polygon2(lhs, {}), Polygon2(rhs, {}), relation);
}

template<class T>
void testCoding(const T& value) {
    T transcodedProto = proto::decode(proto::encode(value));
    T transcodedSproto = maps::geolib3::sproto::decode(
            maps::geolib3::sproto::encode(value));
    if (!spatialRelation(transcodedProto, value, Equals)) {
        std::stringstream error;
        error << "Value has changed after decoding via proto. Was "
            << value << ", got " << transcodedProto;
        BOOST_FAIL(error.str());
    }
    if (!spatialRelation(transcodedSproto, value, Equals)) {
        std::stringstream error;
        error << "Value has changed after decoding via proto. Was "
            << value << ", got " << transcodedSproto;
        BOOST_FAIL(error.str());
    }
}

BOOST_AUTO_TEST_CASE(codec) {
    testCoding(Point2(0.0, 0.0));
    testCoding(Point2(10.0, -20.0));

    BOOST_CHECK_THROW(testCoding(Polyline2(PointsVector({}))), maps::Exception);

    PointsVector triangle{{0.0, 0.0}, {10.0, 5.0}, {5.0, 10.0}};

    testCoding(Polyline2(triangle));

    testCoding(LinearRing2(triangle));
    testCoding(Polygon2(triangle));
    testCoding(MultiPolygon2{{Polygon2{triangle}}});

    testCoding(BoundingBox({-10, -10}, {10, 10}));
}
