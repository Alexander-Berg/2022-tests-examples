#pragma once

#include <util/generic/array_ref.h>
#include <maps/factory/libs/tileindex/impl/tile.h>
#include <maps/factory/libs/tileindex/release.h>

#include <maps/libs/geolib/include/bounding_box.h>
#include <maps/libs/geolib/include/multipolygon.h>
#include <maps/libs/geolib/include/polygon.h>
#include <maps/libs/geolib/include/serialization.h>
#include <maps/libs/geolib/include/spatial_relation.h>
#include <maps/libs/geolib/include/test_tools/random_geometry_factory.h>

#include <boost/optional.hpp>
#include <boost/optional/optional_io.hpp>
#include <library/cpp/testing/gmock_in_unittest/gmock.h>
#include <library/cpp/testing/unittest/registar.h>
#include <util/random/random.h>

namespace maps {
namespace geolib3 {

inline bool operator==(const Polygon2& lhs, const Polygon2& rhs)
{
    return spatialRelation(lhs, rhs, Equals);
}

inline bool operator==(const MultiPolygon2& lhs, const MultiPolygon2& rhs)
{
    return spatialRelation(lhs, rhs, Equals);
}

inline bool operator==(const Polygon2& lhs, const BoundingBox& rhs)
{
    return spatialRelation(lhs, rhs.polygon(), Equals);
}

inline std::ostream& operator<<(std::ostream& os, const Polygon2& rhs)
{
    return os << WKT::toString(rhs);
}

inline std::ostream& operator==(std::ostream& os, const MultiPolygon2& rhs)
{
    return os << WKT::toString(rhs);
}

} // namespace geolib3
} // namespace maps

namespace maps {
namespace tileindex {

inline bool operator==(const Release& lhs, const Release& rhs)
{
    return lhs.issue() == rhs.issue()
           && boost::equal(lhs.zooms(), rhs.zooms(), [&](Zoom zl, Zoom zr) {
               return zl == zr && lhs.geometry(zl) == rhs.geometry(zr);
           });
}

namespace impl {
namespace tests {

/// Get tile polygon in unit rectangle projection.
/// Coordinates are in [0, 1] range. Useful for testing.
/// @example @code
/// Tile       BBox
/// {0, 0, 0} -> {{0,    0   }, {1,   1  }}
/// {0, 0, 1} -> {{0,    0   }, {0.5, 0.5}}
/// {1, 3, 2} -> {{0.25, 0.75}, {0.5, 1.0}}
/// @endcode
struct UnitProjection {
    geolib3::Polygon2 operator()(const Tile& tile) const
    {
        double m = 1.0 / totalTilesAlongAxis(tile.zoom());
        geolib3::BoundingBox bb{{tile.x() * m, tile.y() * m},
            {(tile.x() + 1) * m, (tile.y() + 1) * m}};
        return bb.polygon();
    }
};

MATCHER(IsTrue, "") { return !!arg; }

MATCHER(IsFalse, "") { return !arg; }

/// True then optional is empty.
MATCHER(IsNothing, "") { return arg == boost::none; }

/// True then optional isn't empty and it's value is equal to given value.
MATCHER_P(OptEq, val, "") { return arg && *arg == val; }

/// Predicate checking that all elements in the container are different.
MATCHER(AllDifferent, "")
{
    using std::begin;
    using std::end;
    bool ok = true;
    for (auto&& item: arg) {
        ok = ok && std::count(begin(arg), end(arg), item) == 1;
    }
    return ok;
}

/// Predicate checking that then condition is true, maybe value is equal to
/// given value or nothing overwise.
MATCHER_P2(EqWhen, cond, val, "")
{
    return cond ? (arg && *arg == val) : !arg;
}

/// Prints actual and expected values to ostream and matches it's contents.
MATCHER_P(EqOstr, val, "")
{
    std::ostringstream ss1;
    std::ostringstream ss2;
    ss1 << arg;
    ss2 << val;
    return ss1.str() == ss2.str();
}

template <typename T>
T randomNumber()
{
    return ::RandomNumber<T>();
}

/// Random number in [0, max) range
template <typename T>
T randomNumber(T max)
{
    return ::RandomNumber<T>(max);
}

/// Random number in [min, max) range.
template <typename T>
T randomNumber(T min, T max)
{
    REQUIRE(min <= max, min << " <= " << max);
    if (min == max) {
        return min;
    }
    return min + randomNumber<T>(max - min);
}

template <typename T>
std::vector<T> randomData(size_t minSize = 0, size_t maxSize = 200)
{
    std::vector<T> data(randomNumber<size_t>(minSize, maxSize));
    for (auto& item: data) {
        item = randomNumber<T>();
    }
    return data;
}

/// Predicate checking that tile is inside other tile.
MATCHER_P(Within, tile, "") { return arg.within(tile); }

/// Generate the random tile with coordinates within valid range for the given
/// zoom.
inline Tile randomTile(Zoom zoom)
{
    uint64_t max = totalTilesAlongAxis(zoom);
    return {static_cast<Coord>(randomNumber(max)),
        static_cast<Coord>(randomNumber(max)), zoom};
}

template <typename TileRect = UnitProjection>
MercatorGeometry randomGeometry(
    size_t size = 2,
    size_t minPolygonSize = 3,
    size_t maxPolygonSize = 10,
    TileRect rect = {})
{
    static geolib3::test_tools::PseudoRandom rnd{
        static_cast<size_t>(time(nullptr))};
    static geolib3::test_tools::RandomGeometryFactory factory{
        rect(Tile::Earth()).boundingBox(), &rnd};
    return factory.getMultiPolygon(size, minPolygonSize, maxPolygonSize);
}

} // namespace tests
} // namespace impl
} // namespace tileindex
} // namespace maps
