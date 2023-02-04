#pragma once

#include <yandex/maps/wiki/groupedit/common.h>

#include <string>

namespace maps {
namespace wiki {
namespace groupedit {
namespace tests {

std::string wkt2wkb(const std::string& wkt);

struct SetLogLevelFixture
{
    SetLogLevelFixture();
};

const TUserId TEST_UID = 111;

} // namespace tests
} // namespace groupedit
} // namespace wiki
} // namespace maps
