#include "matchers.h"

#include <maps/renderer/libs/image/include/image_engine.h>
#include <yandex/maps/proto/renderer/vmap2/tile.pb.h>
#include <yandex/maps/proto/renderer/vmap3/tile.pb.h>

#include <google/protobuf/text_format.h>

namespace maps::renderer::cartograph::test_util {

bool isVec2Tile(const std::string& data)
{
    yandex::maps::proto::renderer::vmap2::Tile tile;
    return tile.ParseFromString(TString{data});
}

bool isVec3Tile(const std::string& data)
{
    yandex::maps::proto::renderer::vmap3::Tile tile;
    return tile.ParseFromString(TString{data});
}

bool isVec2TextTile(const std::string& data)
{
    yandex::maps::proto::renderer::vmap2::Tile tile;
    return google::protobuf::TextFormat::ParseFromString(TString{data}, &tile);
}

bool isVec3TextTile(const std::string& data)
{
    yandex::maps::proto::renderer::vmap3::Tile tile;
    return google::protobuf::TextFormat::ParseFromString(TString{data}, &tile);
}

bool isPngImage(const std::string& data)
{
    return image::guessImageType(data) == image::ImageType::Png;
}

bool isSvgImage(const std::string& data)
{
    return image::guessImageType(data) == image::ImageType::Svg;
}

} // namespace maps::renderer::cartograph::test_util
