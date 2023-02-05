#pragma once

#include "map_test.h"
#include "max_error_tiles.h"
#include "test_creator.h"

namespace maps::renderer::check {

class BuildingTest : public IMapTest
{
public:
    BuildingTest(const TileLoaders& tileLoader,
                 bool onlyClickable);

    void processSubRectAsync(const TileRange& rect) override;

    TestReport createReport() const override;

private:
    std::atomic<size_t> numBldGolden_;
    std::atomic<size_t> numBldTest_;
    bool onlyClickable_;
    MaxErrorTiles maxErrorTiles_;
};

class ClickableBuildingTestCreator : public TestCreator
{
public:
    std::unique_ptr<IMapTest> getTest(json::ValueRef testConfig,
                                      const TileLoaders& tileLoader) const override;
};

} // namespace maps::renderer::check
