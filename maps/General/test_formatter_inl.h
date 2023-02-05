#ifndef YANDEX_MAPS_WIKI_TOPO_TEST_FORMATTER_H
#error "Direct inclusion of test_formatter_inl.h is forbidden"
#endif

#include <yandex/maps/geotex/document_blocks/picture.h>
#include <yandex/maps/geotex/document_blocks/table.h>
#include <yandex/maps/geotex/document_blocks/text.h>

#include <boost/algorithm/string.hpp>

namespace maps {
namespace wiki {
namespace topo {
namespace doc {

template <class TestData>
typename TestFormatter<TestData>::TestPrintData
TestFormatter<TestData>::createTestPrintData(const TestData& testData) const
{
    LayerPtrList layersBefore = geomLayersBefore(testData);
    layersBefore.splice(layersBefore.end(), highlightLayersBefore(testData));

    LayerPtrList layersAfter = geomLayersAfter(testData);
    layersAfter.splice(layersAfter.end(), highlightLayersAfter(testData));

    ScenePair scenes {layersToScene(layersBefore), layersToScene(layersAfter)};

    auto bbox = scenes.before->bbox();
    auto afterBBox = scenes.after->bbox();
    if (!bbox) {
        bbox = afterBBox;
    } else if (afterBBox) {
        bbox = geolib3::expand(*bbox, *afterBBox);
    }
    REQUIRE(bbox, "No geom in both test scenes");

    return {scenes, *bbox, customGeomViewRects(testData)};
}

template <class TestData>
void
TestFormatter<TestData>::format(const TestData& testData)
{
    std::string name = testData.name();
    boost::replace_all(name, "_", "\\_");
    document_.addSection(geotex::Document::SectionKind::Section, name);
    document_.appendBlock(std::make_shared<geotex::TextBlock>(testData.description()));
    document_.addParskip(geotex::Document::ParskipSize::Large);
    auto testPrintData = createTestPrintData(testData);
    format(testPrintData.scenes, testPrintData.mainViewRect);
    document_.addParskip(geotex::Document::ParskipSize::Medium);
    for (const auto& sceneRect : testPrintData.detalizedRects) {
        document_.appendBlock(std::make_shared<geotex::TextBlock>("Near point ():"));
        document_.addParskip(geotex::Document::ParskipSize::Small);
        format(testPrintData.scenes, sceneRect);
        document_.addParskip(geotex::Document::ParskipSize::Medium);
    }
    document_.forcePagebreak();
}

template <class TestData>
void
TestFormatter<TestData>::format(
    const ScenePair& scenePair, const geolib3::BoundingBox& sceneRect) const
{
    const double LABELS_MAX_ABS_SIZE = 1.5; // cm

    const double pictureSize = document_.pageRect().width() / 2.0;

    geotex::Size size{pictureSize, pictureSize};

    auto beforePicture = std::make_shared<geotex::PictureBlock>(
        scenePair.before, sceneRect,
        size,
        /* bool enableGrid = */ true,
        LABELS_MAX_ABS_SIZE);

    auto afterPicture = std::make_shared<geotex::PictureBlock>(
        scenePair.after, sceneRect,
        size,
        /* bool enableGrid = */ true,
        LABELS_MAX_ABS_SIZE);

    std::vector<geotex::TableBlock::ColumnHAlign> columnFormatting = {
        geotex::TableBlock::ColumnHAlign::Center,
        geotex::TableBlock::ColumnHAlign::Center
    };
    geotex::TableBlock::RowValuesVector row = {
        {beforePicture, geotex::TableBlock::ColumnVAlign::Center},
        {afterPicture, geotex::TableBlock::ColumnVAlign::Center}
    };
    auto table = std::make_shared<geotex::TableBlock>(columnFormatting);
    table->appendRow(row);
    document_.appendBlock(table);
    document_.addParskip(geotex::Document::ParskipSize::Medium);
}

} // namespace doc
} // namespace topo
} // namespace wiki
} // namespace maps
