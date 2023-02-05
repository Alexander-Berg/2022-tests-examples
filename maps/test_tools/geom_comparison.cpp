#include "geom_comparison.h"

#include "../geom_tools/geom_io.h"

#include <maps/libs/geolib/include/common.h>
#include <maps/libs/geolib/include/distance.h>

namespace maps {
namespace wiki {
namespace topo {
namespace test {

const double POINT_CMP_EPS = geolib3::EPS;

namespace {

class Compare {
public:
    Compare(double eps = POINT_CMP_EPS) : EPS_(eps) {}

    inline bool operator()(
        const geolib3::Point2& p1, const geolib3::Point2& p2) const
    {
        return geolib3::distance(p1, p2) < EPS_;
    }
    inline bool operator()(const SplitPoint& sp1, const SplitPoint& sp2) const
    {
        return sp1.nodeId == sp2.nodeId && (*this)(sp1.geom, sp2.geom);
    }
    inline bool operator()(const SplitPoint* sp1, const SplitPoint* sp2) const
    {
        return (*this)(*sp1, *sp2);
    }

private:
    const double EPS_;
};

} // namespace

bool operator==(const geolib3::Point2& p1, const geolib3::Point2& p2)
{
    return Compare()(p1, p2);
}

bool compare(const geolib3::Point2& p1, const geolib3::Point2& p2, double eps)
{
    return Compare(eps)(p1, p2);
}

bool operator==(const SplitPoint& sp1, const SplitPoint& sp2)
{
    return Compare()(sp1, sp2);
}

bool compare(const SplitPoint& sp1, const SplitPoint& sp2, double eps)
{
    return Compare(eps)(sp1, sp2);
}


bool operator==(const geolib3::Polyline2& l1, const geolib3::Polyline2& l2)
{
    return compare(l1, l2);
}

bool compare(const geolib3::Polyline2& l1, const geolib3::Polyline2& l2,
    double eps)
{
    Compare compare(eps);

    if (l1.pointsNumber() != l2.pointsNumber()) {
        return false;
    }
    for (size_t i = 0; i < l1.pointsNumber(); ++i) {
        if (!(compare(l1.pointAt(i), l2.pointAt(i)))) {
            return false;
        }
    }
    return true;
}

boost::optional<size_t> diffPoint(
    const geolib3::Polyline2& l1, const geolib3::Polyline2& l2,
    double eps)
{
    Compare compare(eps);

    if (l1.pointsNumber() != l2.pointsNumber()) {
        return std::min(l1.pointsNumber(), l2.pointsNumber());
    }
    for (size_t i = 0; i < l1.pointsNumber(); ++i) {
        if (!(compare(l1.pointAt(i), l2.pointAt(i)))) {
            return i;
        }
    }
    return boost::none;
}

} // namespace test
} // namespace topo
} // namespace wiki
} // namespace maps
