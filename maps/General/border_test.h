#pragma once

#include "map_test.h"
#include "max_error_tiles.h"
#include "test_creator.h"

namespace maps::renderer::check {

class BorderTest : public IMapTest
{
public:
    BorderTest(const TileLoaders& tileLoader,
               const std::map<std::string, bool>& localeToBorder);

    void processSubRectAsync(const TileRange& rect) override;

    TestReport createReport() const override;

private:
    std::map<std::string, bool> localeToBorder_;
    MaxErrorTiles maxErrorTiles_;
};

class BorderTestCreator : public TestCreator
{
public:
    std::unique_ptr<IMapTest> getTest(json::ValueRef testConfig,
                                      const TileLoaders& tileLoader) const override;
};

} // namespace maps::renderer::check
