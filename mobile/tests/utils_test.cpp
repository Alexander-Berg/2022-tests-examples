#include <yandex/maps/navi/bookmarks/bookmark_utils.h>
#include <yandex/maps/navi/bookmarks/utils_factory.h>

#include <yandex/maps/mapkit/geometry/point.h>

#include <boost/format.hpp>
#include <boost/test/unit_test.hpp>

namespace yandex::maps::navi::bookmarks {

BOOST_AUTO_TEST_CASE(MakePinUriTest)
{
    const mapkit::geometry::Point point { 37.12345678, 54.12345678 };
    const std::string uri = boost::str(boost::format("ymapsbm1://pin?ll=%.8f%%2C%.8f") %
            point.longitude % point.latitude);

    BOOST_CHECK_EQUAL(*getBookmarkUtils()->makePinUri(point), uri);
}

BOOST_AUTO_TEST_CASE(MakeOrganizationUriTest)
{
    const std::string oid = "123123123";
    const std::string uri = "ymapsbm1://org?oid=" + oid;

    BOOST_CHECK_EQUAL(*getBookmarkUtils()->makeOrganizationUri(oid), uri);
}

}
