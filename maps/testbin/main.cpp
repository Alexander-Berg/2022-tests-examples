#include <maps/photos/libs/region_alert/include/region_alert.h>
#include <maps/libs/common/include/exception.h>
#include <iostream>

int main()
{
    maps::photos::region_alert::initRegionAlert("services.xml");
    REQUIRE(maps::photos::region_alert::regionAlert(maps::geolib3::Point2(37.1764, 55.7277)), "alert expected");
    REQUIRE(!maps::photos::region_alert::regionAlert(maps::geolib3::Point2(0, 0)), "empty zone expected");
}
