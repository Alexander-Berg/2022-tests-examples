#pragma once

#include "map_test.h"
#include "max_error_tiles.h"
#include "test_creator.h"

namespace maps::renderer::check {

class MeshTest : public IMapTest
{
public:
    explicit MeshTest(const TileLoaders& tileLoader);

    void processSubRectAsync(const TileRange& rect) override;

    TestReport createReport() const override;

private:
    std::atomic<size_t> numMeshesGolden_;
    std::atomic<size_t> numMeshesTest_;

    MaxErrorTiles maxErrorTiles_;
};

class MeshTestCreator : public TestCreator
{
public:
    std::unique_ptr<IMapTest> getTest(json::ValueRef testConfig,
                                      const TileLoaders& tileLoader) const override;
};

} // namespace maps::renderer::check
