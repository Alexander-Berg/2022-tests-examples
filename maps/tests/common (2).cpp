#include "common.h"

#include <maps/libs/geolib/include/variant.h>
#include <maps/libs/common/include/exception.h>
#include <maps/libs/log8/include/log8.h>

namespace maps {
namespace wiki {
namespace groupedit {
namespace tests {

std::string wkt2wkb(const std::string& wkt)
{
    auto geometryVariant = geolib3::WKT::read<geolib3::SimpleGeometryVariant>(wkt);
    return geolib3::WKB::toString(geometryVariant);
}

SetLogLevelFixture::SetLogLevelFixture()
{ log8::setLevel(log8::Level::FATAL); }

} // namespace tests
} // namespace groupedit
} // namespace wiki
} // namespace maps
