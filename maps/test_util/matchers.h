#include <gmock/gmock-matchers.h>
#include <string>

namespace maps::renderer::cartograph::test_util {

bool isVec2Tile(const std::string& data);
bool isVec3Tile(const std::string& data);
bool isVec2TextTile(const std::string& data);
bool isVec3TextTile(const std::string& data);
bool isPngImage(const std::string& data);
bool isSvgImage(const std::string& data);

MATCHER(IsVec2Tile, "")
{
    return isVec2Tile(arg) && !arg.empty();
}

MATCHER(IsVec3Tile, "")
{
    return isVec3Tile(arg) && !arg.empty();
}

MATCHER(IsVec2TextTile, "")
{
    return isVec2TextTile(arg) && !arg.empty();
}

MATCHER(IsVec3TextTile, "")
{
    return isVec3TextTile(arg) && !arg.empty();
}

MATCHER(IsPngImage, "")
{
    return isPngImage(arg);
}

MATCHER(IsSvgImage, "")
{
    return isSvgImage(arg);
}

} // namespace maps::renderer::cartograph::test_util
