#pragma once

#include "../test_types/mock_storage.h"


namespace maps {
namespace wiki {
namespace topo {
namespace test {

extern const double POINT_CMP_EPS;

bool operator==(const geolib3::Point2& p1, const geolib3::Point2& p2);

bool compare(const geolib3::Point2& p1, const geolib3::Point2& p2,
    double eps = POINT_CMP_EPS);

bool operator==(const SplitPoint& sp1, const SplitPoint& sp2);

bool compare(const SplitPoint& sp1, const SplitPoint& sp2,
    double eps = POINT_CMP_EPS);

bool operator==(const geolib3::Polyline2& l1, const geolib3::Polyline2& l2);

bool compare(const geolib3::Polyline2& l1, const geolib3::Polyline2& l2,
    double eps = POINT_CMP_EPS);

boost::optional<size_t> diffPoint(const geolib3::Polyline2& l1,
    const geolib3::Polyline2& l2, double eps = POINT_CMP_EPS);

} // namespace test
} // namespace topo
} // namespace wiki
} // namespace maps
