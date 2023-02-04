#include <library/cpp/testing/unittest/registar.h>
#include <yandex/maps/wiki/common/geom_utils.h>
#include <maps/libs/common/include/exception.h>
#include <maps/libs/geolib/include/point.h>

using namespace maps::wiki::common;

Y_UNIT_TEST_SUITE(geom_utils) {

Y_UNIT_TEST(comma_separated_to_bbox)
{
    {
        std::string str = "12,34,56,78";
        auto bbox = bboxFromCommaSeparatedCoords(str);

        UNIT_ASSERT(bbox.lowerCorner() == maps::geolib3::Point2(12, 34));
        UNIT_ASSERT(bbox.upperCorner() == maps::geolib3::Point2(56, 78));
    }
    {
        std::string str = "12,34";
        UNIT_ASSERT_EXCEPTION(bboxFromCommaSeparatedCoords(str),
                              maps::Exception);
    }
    {
        std::string str = "12,34,aaa,78";
        UNIT_ASSERT_EXCEPTION(bboxFromCommaSeparatedCoords(str),
                              std::invalid_argument);
    }
}

} // Y_UNIT_TEST_SUITE(geom_utils)
