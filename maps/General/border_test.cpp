#include "border_test.h"

namespace maps::renderer::check {

namespace {

namespace basemap = yandex::maps::proto::renderer::layers::basemap;

bool hasCountryTag(const std::vector<basemap::Tag>& tags)
{
    return std::find(tags.begin(), tags.end(), basemap::Tag::COUNTRY) != tags.end();
}

bool hasBorder(const TileData& tile)
{
    for (const auto& polyline : tile.polylineFeatures())
        if (hasCountryTag(polyline.tags))
            return true;

    return false;
}

} // namespace

BorderTest::BorderTest(const TileLoaders& tileLoader,
            const std::map<std::string, bool>& localeToBorder)
    : IMapTest(tileLoader)
    , localeToBorder_(localeToBorder)
    , maxErrorTiles_(5)
{
}

void BorderTest::processSubRectAsync(const TileRange& rect)
{
    std::vector<TileTestResult> processedTiles;

    for (const auto& t : rect) {
        auto zoomRange = base::ZoomRange(t.z(), t.z());
        TileTestResult tileResult = {t, {0, ""}};
        for (const auto& [locale, borderVal] : localeToBorder_) {
            if (borderVal != hasBorder(tileLoader_.testTile(t, zoomRange, locale))) {
                tileResult.error.val = 1.0f;
                if (!tileResult.error.problemDescr.empty())
                    tileResult.error.problemDescr += ", ";

                std::ostringstream sOut;
                sOut << locale << " (expected border " << borderVal
                     << ", got " << !borderVal << ")";
                tileResult.error.problemDescr += sOut.str();
            }
        }
        if (tileResult.error.val > 0)
            processedTiles.push_back(tileResult);
    }

    maxErrorTiles_.add(processedTiles);
}

TestReport BorderTest::createReport() const
{
    TestReport report;
    report.maxErrorTiles = maxErrorTiles_.get();
    report.aggregateError = report.maxErrorTiles.empty() ? 0.0f: 1.0f;
    return report;
}

std::unique_ptr<IMapTest> BorderTestCreator::getTest(json::ValueRef testConfig,
                                                     const TileLoaders& tileLoader) const
{
    std::map<std::string, bool> localeToBorder;
    auto expects = testConfig->FindMember("expects");
    if (expects != testConfig->MemberEnd()) {
        const auto& tests = expects->value;
        for (size_t i = 0; i != tests.Size(); ++i) {
            bool border = tests[i]["border"].GetBool();
            const auto& locales = tests[i]["locales"];
            for (size_t j = 0; j != locales.Size(); ++j) {
                localeToBorder[locales[j].GetString()] = border;
            }
        }
    }

    return std::make_unique<BorderTest>(tileLoader, localeToBorder);
}

} // namespace maps::renderer::check
