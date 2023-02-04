#define _SCL_SECURE_NO_WARNINGS
#include <maps/renderer/libs/base/include/compiler.h>
DISABLE_CLANG_WARNING("-Wunused-parameter")

#include "rasterizer/RasterizerData.h"
#include <yandex/maps/renderer5/labeler/labeler.h>
#include "labeler/query_feature_sources.h"
#include "labeler/fill_text_layers.h"
#include <yandex/maps/renderer5/labeler/labeling_zone.h>
#include "labeler/i_labelable_layer.h"
#include "core/features_transformator.h"
#include <maps/renderer/libs/base/include/math/math.h>
#include <yandex/maps/renderer5/core/Map.h>
#include <yandex/maps/renderer5/core/OperationProgress.h>
#include <yandex/maps/renderer5/rasterizer/Rasterizer.h>
#include <yandex/maps/renderer5/rasterizer/svg_generator.h>
#include <yandex/maps/renderer5/styles/styles.h>
#include <yandex/maps/renderer5/postgres/LazyPostgresTransactionProvider.h>
#include <maps/renderer/libs/image/include/png/create_png.h>
#include <maps/renderer/libs/base/include/logger.h>
#include <yandex/maps/renderer5/core/syspaths.h>
#include <maps/renderer/libs/image/include/image_storage.h>
#include <yandex/maps/renderer/io/io.h>
#include <yandex/maps/renderer/proj/tile.h>
#include <maps/libs/common/include/exception.h>
#include <xmlwrapp/xmlwrapp.h>
#include <rapidjson/document.h>

#include <iostream>
#include <fstream>
#include <boost/smart_ptr/make_shared.hpp>
#include <boost/program_options.hpp>

using namespace maps;
using namespace maps::renderer;
using namespace maps::renderer5;

namespace po = boost::program_options;

const char* catalogXmlFileName = "catalog.xml";

struct RenderingZone {
    unsigned int width;
    unsigned int height;
    agg::trans_affine centerTransform;
    agg::trans_affine rotation;
    unsigned int zoom;
    double scale;
    rasterizer::Transform nonlinearTransform;

    RenderingZone() : width(512), height(512), zoom(0), scale(1.0) {}

    agg::trans_affine renderBoxTransform() const
    {
        agg::trans_affine tr = rotation;
        tr *= centerTransform;
        tr.scale(scale);
        tr.translate(0.5 * width, 0.5 * height);
        return tr;
    }

    agg::trans_affine labelBoxTransform() const
    {
        agg::trans_affine tr = centerTransform;
        tr.scale(scale);
        tr.translate(0.5 * width, 0.5 * height);
        return tr;
    }

    agg::trans_affine drawTransform() const
    {
        agg::trans_affine tr = centerTransform;
        tr.translate(0.5 * width, 0.5 * height);

        tr.translate(0, -0.5 * height);
        tr.scale(1, -1);
        tr.translate(0, 0.5 * height);

        double centerScale = 0.5 / scale - 0.5;
        tr.translate(centerScale * width, centerScale * height);
        return tr;
    }
};

rasterizer::Point testNonlinearTransform1(const rasterizer::Point& p)
{
    REN_THROW(core::NotImplementedException());
    //base::Vec2 p1(p.x, p.y);
    //rasterizer::Point result = { p1.x, p1.y };
    //return result;
}

core::RenderData& operator << (core::RenderData& data, const agg::trans_affine& transform)
{
    if (!transform.is_identity()) {
        for (auto& item : data) {
            feature::FeatureIterPtr transformedFeatures(new core::FeaturesTransformator(item.features, transform));
            item.features = transformedFeatures;
        }
    }
    return data;
}

RenderingZone loadPositionFrom(const std::string& fileName, core::Map& map)
{
    RenderingZone zone;
    auto file = io::file::open(fileName);
    if (file.size() != 6 * sizeof(double) || *file.read(0, 1).data() == '{') {
        double x = 0;
        double y = 0;
        double angle = 0;

        auto data = file.read(0, file.size());
        rapidjson::Document json;
        json.Parse<rapidjson::kParseDefaultFlags>(data.data());
        for (auto it = json.MemberBegin(); it != json.MemberEnd(); ++it) {
            std::string name(
                it->name.GetString(),
                it->name.GetString() + it->name.GetStringLength());
            if (name == "centerPosition") {
                std::string value(
                    it->value.GetString(),
                    it->value.GetString() + it->value.GetStringLength());
                std::istringstream ss(value);
                ss >> x >> y >> zone.zoom;
            } else if (name == "angle") {
                angle = it->value.GetDouble();
            } else if (name == "scale") {
                zone.scale = it->value.GetDouble();
            } else if (name == "width") {
                zone.width = it->value.GetUint();
            } else if (name == "height") {
                zone.height = it->value.GetUint();
            } else if (name == "nonlinearTransform") {
                if (it->value.GetInt() == 1)
                    zone.nonlinearTransform = testNonlinearTransform1;
            }
        }

        double scale = proj::pixelPerUnit(zone.zoom);
        zone.centerTransform.translate(-x, -y);
        zone.centerTransform.scale(scale);

        if (angle != 0.0) {
            zone.rotation.translate(-x, -y);
            zone.rotation.rotate(-angle * base::math::DEG2RAD);
            zone.rotation.translate(x, y);
        }

    } else { // old binary debug.position
        double transform[6];
        file.read(0, 6 * sizeof(double), transform);
        zone.centerTransform.load_from(transform);
        double scale = rasterizer::tools::scaleByMtx(zone.centerTransform);
        zone.zoom = static_cast<unsigned int>(proj::zoomFromPixelPerUnit(scale));
        zone.scale = scale / proj::pixelPerUnit(zone.zoom);

        // update debug.position
        file = io::Reader();
        agg::trans_affine tr = zone.centerTransform;
        tr.translate(-0.5 * zone.width, -0.5 * zone.height);
        tr.scale(-1 / scale);
        std::ofstream ofs;
        io::open(ofs, fileName, std::ofstream::out | std::ofstream::trunc);
        ofs << "{ ";
        ofs.precision(12);
        ofs << "\"centerPosition\": \"" << tr.tx << " " << tr.ty << " " << zone.zoom << "\"";
        if (std::abs(zone.scale - 1.0) > 0.01) {
            ofs.precision(6);
            ofs << ", \"scale\": " << zone.scale;
        }
        ofs << " }" << std::endl;
        ofs.close();
        std::cout << "debug.position updated" << std::endl;

        zone = loadPositionFrom(fileName, map);
    }
    return zone;
}

agg::rendering_buffer loadPngBuffer(
    const std::string& fileName,
    image::ImageLoader loadImage)
{
    auto img = loadImage(fileName);
    REN_REQUIRE(img, "Can't load image " + fileName);
    return image::asRenBuffer(*img);
}

agg::rendering_buffer loadSvgBuffer(
    const std::string& fileName,
    unsigned int width,
    image::ImageLoader loadImage)
{
    auto img = loadImage(fileName);
    REN_REQUIRE(img, "Can't load image " + fileName);
    img->setWidth(width);
    auto buffer = image::asRenBuffer(*img);

    agg::rgba8 bgColor = agg::rgba8_pre(255, 255, 255, 255);
    for (size_t row = 0; row < buffer.height(); ++row) {
        for (size_t col = 0; col < buffer.width(); ++col) {
            agg::int8u *pixel = buffer.buf() + 4 * (row*width + col);
            agg::comp_op_rgba_dst_over<agg::rgba8, agg::order_rgba>::blend_pix(
                pixel, bgColor.r, bgColor.g, bgColor.b, bgColor.a, 255);
        }
    }
    rasterizer::tools::demultiplyBuffer(buffer);
    return buffer;
}

bool isEqualBuffers(
    const agg::rendering_buffer& buffer1,
    const agg::rendering_buffer& buffer2)
{
    return buffer1.width() == buffer2.width() &&
        buffer1.height() == buffer2.height() &&
        memcmp(buffer1.buf(), buffer2.buf(),
            buffer1.width() * buffer1.height() * 4) == 0;
}

/**
 * Draw map into buffer.
 * Returns premultiply buffer.
 */
void drawMap(
    core::Map& map,
    const RenderingZone& zone,
    agg::rendering_buffer* mapRenderingBuffer,
    rasterizer::Painter* painter)
{
    core::OperationProgressPtr progressStub(new core::ProgressStub());

    base::BoxD renderBBox = rasterizer::boundingBoxByTransform(
        zone.width, zone.height, zone.renderBoxTransform());

    if (map.mode() == core::MapMode::Dynamic)
    {
        base::BoxD labelBBox = rasterizer::boundingBoxByTransform(
            zone.width, zone.height, zone.labelBoxTransform());

        double scale = proj::pixelPerUnit(zone.zoom);
        auto textLayers = renderer5::labeler::filterLabelableLayers(map.labelableLayers(), scale);
        for (const auto& layer : textLayers)
            layer->get<renderer5::labeler::ILabelableLayer>()->deleteAllFeatures();
        auto source = renderer5::labeler::queryFeatureSources(textLayers, map.fontLoader(), renderBBox, scale);
        source << zone.rotation;
        renderer5::labeler::LabelingSettings labelingSettings;
        labelingSettings.setNumberOfWorkingThreads(1u);
        renderer5::labeler::Labeler l(map, labelingSettings);
        auto labels = l.placeLabels(source,
            renderer5::labeler::LabelingZone(labelBBox, {zone.zoom, zone.zoom}), "", progressStub);
        labels << ~zone.rotation;
        renderer5::labeler::fillTextLayers(labels, labelingSettings, textLayers);
    }

    if (mapRenderingBuffer) {
        agg::rgba8 bgColor = agg::rgba8_pre(255, 255, 255, 255);
        rasterizer::Rasterizer::fillBuffer(*mapRenderingBuffer, bgColor);
    }

    boost::shared_ptr<rasterizer::RasterizerData> rasterizerData;
    rasterizer::Renderer renderer;
    agg::trans_affine tr = zone.drawTransform();
    rasterizerData = boost::make_shared<rasterizer::RasterizerData>(tr);
    rasterizerData->setImageLoader(map.imageLoader());
    rasterizerData->setFontLoader(map.fontLoader());
    rasterizerData->setNonlinearTransform(zone.nonlinearTransform);
    renderer.setRasterizerData(rasterizerData);
    if (mapRenderingBuffer)
        renderer.setRenderingBuffer(*mapRenderingBuffer);
    if (painter)
        renderer.setPainter(painter);
    renderer.setScaleFactor(zone.scale);
    if (map.mode() == core::MapMode::Dynamic)
        renderer.setDistToSimplify(0.125);

    rasterizer::Rasterizer r;
    core::RenderData renderData = r.queryFeatureSources(
        map.getRasterizableLayers(nullptr), map.mode(), renderBBox,
        rasterizer::ZoomRange{zone.zoom, zone.zoom});
    renderData << zone.rotation;

    core::Boundary boundary;
    r.draw(renderer, boundary, renderData, progressStub);

    if (mapRenderingBuffer)
        rasterizer::tools::demultiplyBuffer(*mapRenderingBuffer);
}

std::string makeNewFileName(
    const std::string& fileName,
    const std::string& suffix)
{
    size_t pos = fileName.find_last_of('.');
    std::stringstream newFileName;
    newFileName << fileName.substr(0, pos) << "_" << suffix;
    if (suffix.find(L'.') == std::string::npos)
        newFileName << fileName.substr(pos, std::string::npos);
    return newFileName.str();
}

void savePng(
    agg::rendering_buffer& renBuffer,
    const std::string& fileName,
    const std::string& suffix)
{
    auto newFileName = makeNewFileName(fileName, suffix);

    image::png::PngBuffer png;
    image::png::createPngRgba(renBuffer, 9, png);

    auto stream = io::file::create(newFileName);
    stream->write(png.data(), png.size());
}

void saveDiff(
    const agg::rendering_buffer& buffer1,
    const agg::rendering_buffer& buffer2,
    const std::string& fileName,
    const std::string& suffix)
{
    if ((buffer1.width() != buffer2.width()) || (buffer1.height() != buffer2.height()))
        return;

    auto width = buffer1.width();
    auto height = buffer1.height();

    std::unique_ptr<agg::int8u[]> cmpBuffer(
        new agg::int8u[width * height * 4]);

    agg::rendering_buffer cmpRenderingBuffer(
        cmpBuffer.get(),
        width,
        height,
        width*4);
    cmpRenderingBuffer.copy_from(buffer1);
    for (size_t row = 0; row < height; ++row)
    {
        for (size_t col = 0; col < width; ++col)
        {
            agg::rgba8 *pixelCmp =
                reinterpret_cast<agg::rgba8 *>(cmpRenderingBuffer.buf() + 4*(row*width + col));

            const float blendWhite = 0.7f;
            pixelCmp->r = static_cast<agg::rgba8::value_type>(pixelCmp->r*(1 - blendWhite) + 255*blendWhite);
            pixelCmp->g = static_cast<agg::rgba8::value_type>(pixelCmp->g*(1 - blendWhite) + 255*blendWhite);
            pixelCmp->b = static_cast<agg::rgba8::value_type>(pixelCmp->b*(1 - blendWhite) + 255*blendWhite);

            const agg::rgba8 *pixelBuf1 =
                reinterpret_cast<const agg::rgba8 *>(buffer1.buf() + 4*(row*width + col));
            const agg::rgba8 *pixelBuf2 =
                reinterpret_cast<const agg::rgba8 *>(buffer2.buf() + 4*(row*width + col));
            if (memcmp(pixelBuf1, pixelBuf2, sizeof(agg::rgba8)))
            {
                pixelCmp->r = 0xff;
                pixelCmp->g = 0x00;
                pixelCmp->b = 0x00;
                pixelCmp->a = 0xff;
            }
        }
    }

    auto newFileName = makeNewFileName(fileName, suffix);
    image::png::PngBuffer png;
    image::png::createPngRgba(cmpRenderingBuffer, 9, png);
    auto stream = io::file::create(newFileName);
    stream->write(png.data(), png.size());
}

void checkValidState(core::Map& map)
{
    if (!map.valid()) {
        std::stringstream ss;
        ss << "Map is not valid, see details: ";
        const core::ValidateableEntity& entity = map;
        for (auto msg : entity.errorMessages())
            ss << std::endl << msg;
        for (auto msg : entity.warningMessages())
            ss  << std::endl << msg;
        REN_THROW() << ss.str();
    }
}


/**
 * If test has finished successfull, then return result of comparsion images,
 * otherwise throw exception.
 */
bool runManualTest(
    const std::string& mapXmlFileName,
    const std::string& positionFileName,
    const std::string& pngFileName,
    const std::string& svgFileName,
    bool saveResult)
{
    using namespace maps::renderer5::rasterizer;
    core::MapPtr map(new core::Map(core::MapMode::Dynamic));

    postgres::PostgresTransactionProviderPtr provider(
        new postgres::LazyPostgresTransactionProvider());

    map->setPostgresTransactionProvider(provider);
    map->loadFromXml(mapXmlFileName, true);
    map->open();
    checkValidState(*map);

    RenderingZone zone = loadPositionFrom(positionFileName, *map);

    bool testOk = true;

    std::unique_ptr<agg::int8u[]> mapBuffer(
        new agg::int8u[zone.width * zone.height * 4]);
    agg::rendering_buffer mapRenderingBuffer(
        mapBuffer.get(), zone.width, zone.height, zone.width * 4);

    if (!pngFileName.empty()) {
        drawMap(*map, zone, &mapRenderingBuffer, nullptr);

        if (io::exists(pngFileName)) {
            auto originalPng = loadPngBuffer(pngFileName, map->imageLoader());

            if (!isEqualBuffers(mapRenderingBuffer, originalPng)) {
                testOk = false;
                if (saveResult) {
                    savePng(mapRenderingBuffer, pngFileName, "new.png");
                    saveDiff(mapRenderingBuffer, originalPng, pngFileName, "diff.png");
                }
            }
        } else {
            testOk = false;
        }
    }

    if (!svgFileName.empty()) {
        auto tmpSvgFileName = io::tempFileName();
        std::ofstream stream;
        io::open(stream, tmpSvgFileName, std::ofstream::out | std::ofstream::trunc);
        rasterizer::SvgGenerator svg(stream);
        svg.setSize(zone.width, zone.height);
        svg.setTitle("Maps renderer test");
        svg.begin();
        drawMap(*map, zone, nullptr, &svg);
        svg.end();
        stream.close();

        auto newSvgRasterized = loadSvgBuffer(tmpSvgFileName, zone.width, map->imageLoader());

        if (saveResult) {
            io::file::write(io::file::open(tmpSvgFileName), makeNewFileName(svgFileName, "new.svg"));
            savePng(newSvgRasterized, svgFileName, "svg_new.png");
        }

        if (io::exists(svgFileName)) {
            auto originalSvg = loadSvgBuffer(svgFileName, zone.width, map->imageLoader());

            if (!isEqualBuffers(newSvgRasterized, originalSvg)) {
                testOk = false;
                if (saveResult)
                    saveDiff(newSvgRasterized, originalSvg, svgFileName, "svg_diff.png");
            }
        } else {
            testOk = false;
        }

        if (!pngFileName.empty() && saveResult) {
            saveDiff(newSvgRasterized, mapRenderingBuffer, svgFileName, "svg-png_diff.png");
        }
    }

    return testOk;
}

void init()
{
    // load xml catalog file
    //
    if (io::exists(catalogXmlFileName))
        xml::set_xml_catalog(catalogXmlFileName);

    // All paths are relative to maps/renderer/libs/renderer5
    io::setTempDirPath("tmp");
    core::syspaths::setXsltPatchDir("schema/xslt");
    core::syspaths::setFontsPath("../../renderer-fonts/fonts");

    io::dir::create(io::tempDirPath());
}

void deleteTestFiles()
{
    if (io::exists(io::tempDirPath()))
        REN_LOG_CATCH_WARNING(io::dir::remove(io::tempDirPath()));
}

struct Params
{
    std::string pathToTest;
    bool saveResult;
};

Params parseArgs(int argc, char** argv)
{
    Params result;

    po::options_description options("Option");

    po::options_description availableOptions("options");
    availableOptions.add_options()
        ("help,h", "produce help message")
        ("save-result", "save new png and diff, if test does not pass")
    ;

    po::options_description requiredArgs;
    requiredArgs.add_options()
        ("path-to-test", po::value<std::string>(&result.pathToTest), "path to test")
    ;

    options.add(availableOptions).add(requiredArgs);

    po::positional_options_description positionals;
    positionals.add("path-to-test", 1);

    po::variables_map vm;
    po::store(po::command_line_parser(argc, argv)
            .options(options).positional(positionals).run(), vm);
    po::notify(vm);

    if (vm.empty() || vm.count("help")) {
        std::cout << "Usage: " << argv[0] << " path-to-test [options]" << std::endl;
        std::cout << availableOptions << std::endl;
        exit(0);
    }

    result.saveResult = vm.count("save-result") > 0;
    REN_REQUIRE(!result.pathToTest.empty(), "path-to-test is not set");

    return result;
}

// return 1 in cases:
//   not equal,
//   catch thrown exceptions
int main(int argc, char* argv[])
{
    int result = 0;

    try
    {
        auto params = parseArgs(argc, argv);

        std::string mapXmlFileName;
        std::string positionFileName;
        std::string pngFileName;
        std::string svgFileName;

        if (!io::dir::isDirectory(params.pathToTest))
            REN_THROW() << "\"" << argv[1] << "\" is not directory with test";
        mapXmlFileName = params.pathToTest + "/map.xml";
        positionFileName = params.pathToTest + "/debug.position.json";
        pngFileName = params.pathToTest + "/screenshot.png";
        svgFileName = params.pathToTest + "/screenshot.svg";
        if (!io::exists(svgFileName))
            svgFileName.clear();

        if (!io::exists(mapXmlFileName))
            REN_THROW() << "Can not found map xml file \"" << mapXmlFileName << "\"";

        if (!io::exists(positionFileName ))
            REN_THROW() << "Can not found position file \"" << positionFileName << "\"";

        if (!pngFileName.empty())
            pngFileName = io::path::absolute(pngFileName);
        if (!svgFileName.empty())
            svgFileName = io::path::absolute(svgFileName);

        init();

        bool testResult = runManualTest(
            mapXmlFileName,
            positionFileName,
            pngFileName,
            svgFileName,
            params.saveResult);

        if (!testResult) {
            std::cout << "Images are not equal" << std::endl;
            result = 1;
        }
    }
    catch (const maps::Exception& ex)
    {
        std::cerr << std::endl << ex << std::endl;
        std::cout << ex.what() << std::endl;
        result = 1;
    }
    catch(const std::exception& ex)
    {
        std::cerr << std::endl << ex.what() << std::endl;
        std::cout << ex.what() << std::endl;
        result = 1;
    }

    deleteTestFiles();

    return result;
}

