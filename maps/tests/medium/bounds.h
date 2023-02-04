#pragma once
#include <maps/libs/geolib/include/polygon.h>

#include <string>
#include <vector>

namespace maps::wiki::social::tests {

const auto BOUNDS_1 = "[37.580, 55.820, 37.583, 55.821]";
const auto BOUNDS_2 = "[37.578, 55.803, 37.578, 55.803]";

// Each bounds is contained only in one region.
const geolib3::Polygon2 REGION_CONTAINING_BOUNDS_1({
    {4176550., 7487262.}, // 37.5185870, 55.8198637
    {4177887., 7482446.}, // 37.5305975, 55.7954992
    {4185512., 7485179.}, // 37.5990940, 55.8093274
    {4184385., 7490090.}  // 37.5889700, 55.8341633
});

const geolib3::Polygon2 REGION_CONTAINING_BOUNDS_2({
    {4182758., 7484049.}, // 37.5743544, 55.8036105
    {4182906., 7483782.}, // 37.5756839, 55.8022596
    {4183417., 7483911.}, // 37.5802743, 55.8029123
    {4183331., 7484245.}  // 37.5795018, 55.8046022
});

} // namespace maps::wiki::social::tests
