#include "tests/boost-tests/include/tools/map_tools.h"

#include <yandex/maps/renderer5/rasterizer/Rasterizer.h>
#include <maps/renderer/libs/image/include/png/create_png.h>
#include <yandex/maps/renderer5/core/Map.h>
#include <yandex/maps/renderer5/core/MapPathResolverFactory.h>
#include <yandex/maps/renderer5/styles/Pattern.h>
#include <yandex/maps/renderer5/labeler/label_generator.h>
#include <yandex/maps/renderer/proj/tile.h>

#include <boost/test/unit_test.hpp>
#include <iostream>

using namespace maps::renderer5;
using namespace maps::renderer5::rasterizer;
using namespace maps::renderer5::test;
using namespace maps::renderer;

namespace {

const std::string roadsMap =
    "tests/manual-tests/rasterizer/label_roads_with_arrows/map.xml";

const std::string TilesDirectory = "tests/boost-tests/referenceTiles/";

std::string getFileName(unsigned int tileSize, double scaleFactor)
{
    std::stringstream fileName;
    fileName.setf(std::ios::fixed, std:: ios::floatfield);
    fileName.precision(1);
    fileName << TilesDirectory << "roads_" << tileSize << "_s"
        << scaleFactor << ".png";
    return fileName.str();
}

std::string getFileName(unsigned int scale)
{
    std::stringstream fileName;
    fileName.setf(std::ios::fixed, std:: ios::floatfield);
    fileName << TilesDirectory << "roads_" << scale << ".png";
    return fileName.str();
}

void saveNewPng(agg::rendering_buffer& buffer, const std::string& fileName)
{
    size_t pos = fileName.find_last_of(L'.');
    std::stringstream newFileName;
    newFileName << fileName.substr(0, pos).c_str()
        << "_new.png";

    savePng(buffer, newFileName.str());
}

void saveDiff(
    const agg::rendering_buffer& buffer1,
    const agg::rendering_buffer& buffer2,
    const std::string& fileName)
{
    if ((buffer1.width() != buffer2.width()) || (buffer1.height() != buffer2.height()))
        return;

    const unsigned int tileW = buffer1.width();
    const unsigned int tileH = buffer1.height();

    std::unique_ptr<agg::int8u[]> cmpBuffer(
        new agg::int8u[tileW*tileH*4]);

    agg::rendering_buffer cmpRenderingBuffer(
        cmpBuffer.get(),
        tileW,
        tileH,
        tileW*4);
    cmpRenderingBuffer.copy_from(buffer1);
    for (size_t row = 0; row < tileH; ++row)
    {
        for (size_t col = 0; col < tileW; ++col)
        {
            agg::rgba8 *pixelCmp =
                reinterpret_cast<agg::rgba8 *>(cmpRenderingBuffer.buf() + 4*(row*tileW + col));

            const float blendWhite = 0.7f;
            pixelCmp->r = static_cast<agg::rgba8::value_type>(pixelCmp->r*(1 - blendWhite) + 255*blendWhite);
            pixelCmp->g = static_cast<agg::rgba8::value_type>(pixelCmp->g*(1 - blendWhite) + 255*blendWhite);
            pixelCmp->b = static_cast<agg::rgba8::value_type>(pixelCmp->b*(1 - blendWhite) + 255*blendWhite);

            const agg::rgba8 *pixelBuf1 =
                reinterpret_cast<const agg::rgba8 *>(buffer1.buf() + 4*(row*tileW + col));
            const agg::rgba8 *pixelBuf2 =
                reinterpret_cast<const agg::rgba8 *>(buffer2.buf() + 4*(row*tileW + col));
            if (memcmp(pixelBuf1, pixelBuf2, sizeof(agg::rgba8)))
            {
                pixelCmp->r = 0xff;
                pixelCmp->g = 0x00;
                pixelCmp->b = 0x00;
                pixelCmp->a = 0xff;
            }
        }
    }

    size_t pos = fileName.find_last_of(L'.');
    std::stringstream diffFileName;
    diffFileName << fileName.substr(0, pos).c_str()
        << "_diff.png";

    savePng(cmpRenderingBuffer, diffFileName.str());
}

agg::rendering_buffer makeBuffer(uint32_t width, uint32_t height, std::vector<agg::int8u>* data)
{
    data->clear();
    data->resize(width * height * 4);
    return agg::rendering_buffer(data->data(), width, height, width * 4);
}

} // anonymous

BOOST_AUTO_TEST_SUITE( rendering_tests )

BOOST_AUTO_TEST_CASE( render_scale )
{
    auto mapGui = map::openMap(roadsMap);

    unsigned int zoom = 17;

    labeler::LabelGenerator lg(mapGui->map(), mapGui->map().labelableLayers());
    lg.placeLabels(map::createProgressStub(), mapGui->getExtent(), zoom);

    Rasterizer rast;

    for (unsigned int i = 0; i < 4; ++i) {
        for (size_t j = 0; j < 4; ++j) {
            unsigned int linSize = 128 * (1 + i);
            double scaleFactor = 0.5 * (1 + j);

            std::vector<agg::int8u> data;
            auto renBuffer = makeBuffer(linSize, linSize, &data);

            agg::rgba8 bgColor = agg::rgba8_pre(255, 255, 255, 255);
            rasterizer::Rasterizer::fillBuffer(renBuffer, bgColor);

            double cx = -11312;
            double cy = -3309;

            agg::trans_affine mtx;
            mtx.translate(-cx, -cy);
            mtx.scale(proj::pixelPerUnit(zoom));
            double scale = 0.5 / scaleFactor * linSize;
            mtx.translate(scale, scale);

            bool isFull = rast.drawMap2(map::createProgressStub(),
                renBuffer, mtx, mapGui->map(), nullptr, scaleFactor);

            auto refTile = loadPngAsAgg(getFileName(linSize, scaleFactor));

            if (!equal(renBuffer, refTile)) {
                saveNewPng(renBuffer, getFileName(linSize, scaleFactor));
                saveDiff(renBuffer, refTile, getFileName(linSize, scaleFactor));
                BOOST_ERROR("equal(renBuffer, refTile)");
            }
        }
    }
}

BOOST_AUTO_TEST_CASE( render_with_custom_zoom )
{
    auto mapGui = map::openMap(roadsMap);

    unsigned int z = 17;

    labeler::LabelGenerator lg(mapGui->map(), mapGui->map().labelableLayers());
    lg.placeLabels(map::createProgressStub(), mapGui->getExtent(), z);

    Rasterizer rast;

    double cx = -11312;
    double cy = -3309;

    for (unsigned int zoom = 16; zoom <= 18; zoom++) {
        std::vector<agg::int8u> data;
        auto renBuffer = makeBuffer(512, 512, &data);

        agg::rgba8 bgColor = agg::rgba8_pre(255, 255, 255, 255);
        rasterizer::Rasterizer::fillBuffer(renBuffer, bgColor);

        agg::trans_affine mtx;
        mtx.translate(-cx, -cy);
        mtx.scale(proj::pixelPerUnit(z));
        mtx.translate(0.5 * 512, 0.5 * 512);

        Renderer renderer;
        renderer.setRenderingBuffer(renBuffer);
        renderer.setScaleFactor(1.0);
        renderer.setDistToSimplify(0.125); // dynamicDistToSimplify

        bool isFull = rast.drawMap3(renderer, map::createProgressStub(),
            renBuffer.width(), renBuffer.height(), mtx, mapGui->map(), nullptr,
            Transform(), Transform(), zoom);

        auto refTile = loadPngAsAgg(getFileName(zoom));

        if (!equal(renBuffer, refTile)) {
            saveNewPng(renBuffer, getFileName(zoom));
            saveDiff(renBuffer, refTile, getFileName(zoom));
            BOOST_ERROR("equal(renBuffer, refTile)");
        }
    }
}

BOOST_AUTO_TEST_SUITE_END()
