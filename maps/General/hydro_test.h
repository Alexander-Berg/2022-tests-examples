#pragma once

#include "map_test.h"
#include "test_creator.h"

namespace maps::renderer::check {

class HydroTest : public INonAggregateMapTest
{
public:
    TileError testTile(const TileData& testTile,
                       const TileData& goldenTile) override;
};

class HydroTestCreator : public TestCreator
{
public:
    std::unique_ptr<IMapTest> getTest(json::ValueRef testConfig,
                                      const TileLoaders& tileLoader) const override;
};

} // namespace maps::renderer::check
