#include "map_test.h"
#include "max_error_tiles.h"
#include "rect_splitter.h"

#include <maps/libs/config/include/config.h>
#include <maps/libs/log8/include/log8.h>
#include <yandex/maps/renderer/io/io.h>

#include <queue>

namespace maps::renderer::check {

namespace {

class NonAggregateMapTestWrapper : public IMapTest
{
public:
    NonAggregateMapTestWrapper(const TileLoaders& tileLoader,
                               std::unique_ptr<INonAggregateMapTest> mapTest,
                               const std::string& locale,
                               size_t maxTilesToStore)
        : IMapTest(tileLoader)
        , mapTest_(std::move(mapTest))
        , locale_(locale)
        , maxErrorTiles_(maxTilesToStore)
    {}

    void processSubRectAsync(const TileRange& rect) override
    {
        std::vector<TileTestResult> processedTiles;
        std::vector<TileTestResult> failedToLoadTiles;

        for (const auto& t : rect) {
            base::ZoomRange zoomRange(t.z(), t.z());
            TileTestResult res = {t, testTile(t, zoomRange)};
            if (res.error.val > 0)
                processedTiles.push_back(res);
        }

        maxErrorTiles_.add(processedTiles);
    }

    TestReport createReport() const override
    {
        TestReport report;
        report.maxErrorTiles = maxErrorTiles_.get();

        if (!report.maxErrorTiles.empty()){
            // Use max value as aggregate error
            report.aggregateError = report.maxErrorTiles.front().error.val;
        }

        return report;
    }

private:
    TileError testTile(const Tile &tileId,
                       const base::ZoomRange& ftZoomRange)
    {
        return mapTest_->testTile(tileLoader_.testTile(tileId, ftZoomRange, locale_),
                                  tileLoader_.goldenTile(tileId, ftZoomRange, locale_));
    }

    std::unique_ptr<INonAggregateMapTest> mapTest_;
    std::string locale_;

    MaxErrorTiles maxErrorTiles_;
};

} // namespace

IMapTest::IMapTest(const TileLoaders& tileLoader)
    : tileLoader_(tileLoader)
{
}

std::unique_ptr<IMapTest>
convert(std::unique_ptr<INonAggregateMapTest> test,
        const TileLoaders& tileLoader,
        const std::string& locale,
        size_t maxTilesToStore)
{
    return std::make_unique<NonAggregateMapTestWrapper>(tileLoader,
                                                        std::move(test),
                                                        locale,
                                                        maxTilesToStore);
}

} // namespace maps::renderer::check
