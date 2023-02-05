#pragma once

#include "map_test.h"

namespace maps::renderer::check {

class TestCreator
{
public:
    virtual ~TestCreator() {}

    virtual
    std::unique_ptr<IMapTest> getTest(json::ValueRef testConfig,
                                      const TileLoaders& tileLoader) const = 0;
};

} // namespace maps::renderer::check
