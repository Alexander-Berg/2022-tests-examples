#include "tests/boost-tests/include/tools/map_tools.h"

#include "core/ISearchableLayer.h"
#include "core/feature.h"

#include <yandex/maps/renderer5/core/Map.h>
#include <yandex/maps/renderer5/core/MapPathResolverFactory.h>
#include <yandex/maps/renderer5/core/OperationProgress.h>
#include <yandex/maps/renderer5/core/StylesLibrary.h>
#include <yandex/maps/renderer5/core/FeatureCapabilities.h>
#include <maps/renderer/libs/image/include/image_storage.h>
#include <yandex/maps/renderer/io/resource.h>
#include <maps/renderer/libs/base/include/platform.h>
#include <yandex/maps/renderer/proj/tile.h>
#include <boost/filesystem.hpp>
#include <boost/make_shared.hpp>

#include <algorithm>
#include <fstream>

#ifdef REN_PLATFORM_WIN
#   include <io.h>
#endif
#include <sys/stat.h>

namespace fs = boost::filesystem;

namespace maps { namespace renderer5 { namespace test {

using namespace ::maps::renderer;

namespace {
class TestPathResolver : public core::IMapPathResolver
{
public:
    virtual std::string mapPath() const
    {
        return io::tempDirPath();
    }

    virtual std::string resolveMapPath(const std::string& filename) const
    {
        return io::path::absolute(filename, mapPath());
    }

    virtual std::string resolveFontPath(const std::string& filename) const
    {
        return filename;
    }

    virtual std::string resolveDataPath(const std::string & filename) const
    {
        return io::path::absolute(filename, mapPath());
    }

    virtual std::string patchResourcePath(
        const std::string& path,
        const core::IMapPathResolver&) const
    {
        return path;
    }
};

void prepareDirToDelete(const fs::path& dir)
{
    fs::directory_iterator dIt(dir);
    fs::directory_iterator dItEnd;

    for (; dIt != dItEnd; ++dIt) {
        const fs::path& p = dIt->path();
        if (fs::is_directory(p))
            prepareDirToDelete(p);
        else
            chmodFile(p.string(), WriteRead);
    }
}
}

void chmodFile(const std::string& fileName, FileMode mode)
{
#if defined(_WIN32)
    int res = 0;

    if (mode == ReadOnly)
        res = _chmod(fileName.c_str(), _S_IREAD);
    else
        res = _chmod(fileName.c_str(), _S_IWRITE);

    if (res != 0)
        REN_THROW() << base::lastError();
#else
    if (mode == ReadOnly)
        chmod(fileName.c_str(), 0444);
    else
        chmod(fileName.c_str(), 0644);
#endif
}

namespace map {
void deleteFilesFromDir(const std::string& dir)
{
    fs::path dirPath(dir);

    prepareDirToDelete(dirPath);

    fs::directory_iterator dIt(dirPath);
    fs::directory_iterator dItEnd;
    for (; dIt != dItEnd; ++dIt) {
        fs::remove_all(dIt->path());
    }
}

core::IMapPathResolverPtr createTestPathResolver()
{
    return boost::make_shared<TestPathResolver>();
}

namespace {
class ResolverFactory : public core::MapPathResolverFactory
{
public:
    ResolverFactory(const std::string& mapLocation)
        : mapLocation_(mapLocation)
    {}

    virtual core::IMapPathResolverPtr create(
        const std::string& xmlFileLocation,
        core::MapMode::Mode mode,
        bool createDirIfNotExists) override
    {
        return MapPathResolverFactory::create(
            xmlFileLocation.empty() ? mapLocation_ : xmlFileLocation,
            mode,
            createDirIfNotExists);
    }

protected:
    std::string mapLocation_;
};

} // anonymous

core::IMapPathResolverFactoryPtr createPathResolverFactory(
    const std::string& xmlFileName)
{
    return boost::make_shared<ResolverFactory>(xmlFileName);
}

core::IMapGuiPtr createTestMapGui()
{
    core::IMapGuiPtr mapGui = core::IMapGui::createMapGui();

    mapGui->map().env().stylesLibrary.reset(
        new core::StylesLibrary(""));

    return mapGui;
}

core::IMapGuiPtr loadMap(const std::string& xmlFileName, bool validateXml)
{
    core::IMapGuiPtr mapGui = map::createTestMapGui();
    mapGui->loadFromXml(xmlFileName, validateXml);
    return mapGui;
}

core::IMapGuiPtr openMap(const std::string& xmlFileName,
                         bool validateXml,
                         core::MapOpenFlags::Flags flags)
{
    core::IMapGuiPtr mapGui = map::loadMap(xmlFileName, validateXml);
    mapGui->open(map::createProgressStub(), flags);
    return mapGui;
}

core::OperationProgressPtr createProgressStub()
{
    return boost::make_shared<core::ProgressStub>();
}

} // namespace map

agg::rendering_buffer loadPngAsAgg(const std::string& fileName)
{
    static image::ImageStorage storage;
    auto img = *storage.getOrAddImage(
        fileName, [&] { return io::readResource(fileName); });
    return image::asRenBuffer(img);
}

image::png::PngBuffer loadPng(const std::string& fileName)
{
    return io::file::open(fileName).readAllToVector();
}

void savePng(
    const agg::rendering_buffer& buffer,
    const std::string& fileName)
{
    image::png::PngBuffer png;
    image::png::createPngRgba(buffer, 9, png);

    auto stream = io::file::create(fileName);
    stream->write(png.data(), png.size());
}

void savePng(const image::png::PngBuffer& png, const std::string& fileName)
{
    auto stream = io::file::create(fileName);
    stream->write(png.data(), png.size());
}

bool equal(
    const agg::rendering_buffer& buffer1,
    const agg::rendering_buffer& buffer2)
{
    return
        buffer1.width() == buffer2.width() &&
        buffer1.height() == buffer2.height() &&
        (memcmp(buffer1.buf(), buffer2.buf(), buffer1.width() * buffer1.height() * 4) == 0);
}

bool equal(
    const image::png::PngBuffer& buffer1,
    const image::png::PngBuffer& buffer2)
{
    static_assert(sizeof(image::png::PngBuffer::value_type) == 1, "memcmp should be updated");
    return buffer1.size() == buffer2.size() &&
        memcmp(buffer1.data(), buffer2.data(), buffer1.size()) == 0;
}

uint64_t getFeatureCount(core::ISearchableLayer* layer)
{
    core::FeatureCapabilities cap;
    auto it = layer->findFeatures(proj::EARTH_BOX, cap);
    uint64_t count = 0;
    for (it->reset(); it->hasNext(); it->next())
        ++count;
    return count;
}

std::vector<std::string> filesInDirectory(const std::string& dirName)
{
    std::vector<std::string> result;

    fs::path dirPath(dirName);

    fs::directory_iterator dIt(dirPath);
    fs::directory_iterator dItEnd;

    for (; dIt != dItEnd; ++dIt)
        if (dIt->status().type() == fs::file_type::regular_file)
            result.push_back(dIt->path().string());

    std::sort(result.begin(), result.end());

    return result;
}

} } } // namespace maps::renderer5::test
