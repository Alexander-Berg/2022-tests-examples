#pragma once

#include <maps/infopoint/lib/point/infopoint.h>

namespace infopoint::tests {

std::string toXml(
    const infopoint::Infopoint &point,
    const std::string& suppliersConfFile);

std::string toProtobuf(
    const infopoint::Infopoint &point,
    const std::string& suppliersConfFile);

} // namespace infopoint::tests
