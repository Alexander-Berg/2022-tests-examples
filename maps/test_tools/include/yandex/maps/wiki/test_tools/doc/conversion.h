#pragma once

#include "common.h"

#include <maps/libs/geolib/include/linear_ring.h>
#include <maps/libs/geolib/include/polygon.h>
#include <yandex/maps/geotex/objects/path.h>
#include <yandex/maps/geotex/objects/segment.h>
#include <yandex/maps/geotex/scene.h>

#include <list>
#include <memory>
#include <string>

namespace maps {
namespace wiki {
namespace test_tools {
namespace doc {

const std::string COLOR_CONTOUR = "black";
const std::string COLOR_INTERIOR = "white";
const std::string STYLE_CONTOUR = "very thick";

enum class ShowLineDirection { Hide, Show };

inline
geotex::LayerPtr toLayer
    ( const std::string& name
    , const geotex::ColorId& color
    , ShowLineDirection showLineDirection
    )
{
    return std::make_shared<geotex::Layer>(name, geotex::ObjectStyle
        ( geotex::ObjectStyle::NO_FILL
        , color
        , geotex::ObjectStyle::NO_PATTERN
        , geotex::ObjectStyle::NO_MARKER
        , STYLE_CONTOUR
        , showLineDirection == ShowLineDirection::Show
        ));
}

inline
geotex::LayerPtr toLayer
    ( const geolib3::LinearRing2& ring
    , const std::string& name
    , const geotex::ColorId& color
    )
{
    auto path = std::make_shared<geotex::Path>();
    for (size_t i = 0; i != ring.segmentsNumber(); ++i) {
        path->append(std::make_shared<geotex::Segment>(ring.segmentAt(i)));
    }
    auto res = std::make_shared<geotex::Layer>(name, geotex::ObjectStyle
        ( color
        , COLOR_CONTOUR
        , geotex::ObjectStyle::NO_PATTERN
        , geotex::ObjectStyle::NO_MARKER
        , STYLE_CONTOUR
        ));
    res->append(path);
    return res;
}

inline
LayerPtrList toLayers
    ( const geolib3::Polygon2& polygon
    , const std::string& name
    , const geotex::ColorId& color
    )
{
    LayerPtrList res;
    res.push_back(toLayer(polygon.exteriorRing(), name + " exterior", color));
    for (size_t i = 0; i != polygon.interiorRingsNumber(); ++i) {
        res.push_back(toLayer
            ( polygon.interiorRingAt(i)
            , name + " interior " + std::to_string(i)
            , COLOR_INTERIOR
            ));
    }
    return res;
}

} // namespace doc
} // namespace test_tools
} // namespace wiki
} // namespace maps
