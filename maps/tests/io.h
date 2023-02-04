#pragma once

#include <yandex/maps/wiki/graph/common.h>
#include <yandex/maps/wiki/common/string_utils.h>

namespace maps {
namespace wiki {
namespace graph {
namespace tests {

template<typename Collection>
std::string printCollection(const Collection& collection)
{
    return "[" + common::join(collection, ",") + "]";
}

} // namespace tests
} // namespace graph
} // namespace wiki
} // namespace maps
