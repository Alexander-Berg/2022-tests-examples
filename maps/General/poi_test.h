#pragma once

#include "map_test.h"
#include "max_error_tiles.h"
#include "test_creator.h"

namespace maps::renderer::check {

class PoiTest : public IMapTest
{
public:
    PoiTest(const TileLoaders& tileLoader,
            bool requestMultizoomTiles,
            bool onlyClickable);

    void processSubRectAsync(const TileRange& rect) override;

    TestReport createReport() const override;

private:
    std::atomic<size_t> numPoiGolden_;
    std::atomic<size_t> numPoiTest_;
    bool requestMultizoomTiles_;
    bool onlyClickable_;
    MaxErrorTiles maxErrorTiles_;
};

class PoiTestCreator : public TestCreator
{
public:
    std::unique_ptr<IMapTest> getTest(json::ValueRef testConfig,
                                      const TileLoaders& tileLoader) const override;
};

class ClickablePoiTestCreator : public TestCreator
{
public:
    std::unique_ptr<IMapTest> getTest(json::ValueRef testConfig,
                                      const TileLoaders& tileLoader) const override;
};

} // namespace maps::renderer::check
