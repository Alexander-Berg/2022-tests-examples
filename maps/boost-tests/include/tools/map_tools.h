#pragma once

namespace maps { namespace renderer { }}
namespace maps { namespace renderer5 { using namespace ::maps::renderer; }}

#include <yandex/maps/renderer5/core/IMapGui.h>

#include <yandex/maps/renderer5/core/syspaths.h>
#include <yandex/maps/renderer5/core/IMapPathResolver.h>
#include <yandex/maps/renderer5/core/IMapPathResolverFactory.h>
#include <yandex/maps/renderer5/core/IOperationProgress.h>
#include <maps/renderer/libs/image/include/png/create_png.h>
#include <yandex/maps/renderer5/core/fd.h>
#include <yandex/maps/renderer/io/io.h>
#include <maps/renderer/libs/base/include/random.h>

#include <string>
#include <algorithm>
#include <cmath>
#include <climits>

namespace maps { namespace renderer5 { namespace test {

using namespace renderer;
using base::Random;

namespace map {
void deleteFilesFromDir(const std::string& dir);

inline void deleteFilesFromTmpDir()
{
    deleteFilesFromDir(io::tempDirPath());
}

core::IMapGuiPtr createTestMapGui();
core::IMapGuiPtr loadMap(
    const std::string& xmlFileName, bool validateXml = true);
core::IMapGuiPtr openMap(
    const std::string& xmlFileName,
    bool validateXml = true,
    core::MapOpenFlags::Flags flags = core::MapOpenFlags::NONE);

/**
 * Creates test PathResolver,
 * init map path to syspaths::tempDirPath.
 */
core::IMapPathResolverPtr createTestPathResolver();
core::IMapPathResolverFactoryPtr createPathResolverFactory(
    const std::string& xmlFileName);

core::OperationProgressPtr createProgressStub();
}

inline unsigned reverseBytes(unsigned int val)
{
    return (((val >> 0*CHAR_BIT) & 0xFF) << 3*CHAR_BIT) |
           (((val >> 1*CHAR_BIT) & 0xFF) << 2*CHAR_BIT) |
           (((val >> 2*CHAR_BIT) & 0xFF) << 1*CHAR_BIT) |
           (((val >> 3*CHAR_BIT) & 0xFF) << 0*CHAR_BIT) ;
}

enum FileMode
{
    ReadOnly = 0,
    WriteRead
};

void chmodFile(const std::string& fileName, FileMode mode);

agg::rendering_buffer loadPngAsAgg(const std::string& fileName);

image::png::PngBuffer loadPng(const std::string& fileName);

void savePng(
    const agg::rendering_buffer& buffer,
    const std::string& fileName);

void savePng(const image::png::PngBuffer& png, const std::string& fileName);

bool equal(
    const agg::rendering_buffer& buffer1,
    const agg::rendering_buffer& buffer2);

bool equal(
    const image::png::PngBuffer& buffer1,
    const image::png::PngBuffer& buffer2);

uint64_t getFeatureCount(core::ISearchableLayer* layer);

std::vector<std::string> filesInDirectory(const std::string& directory);

} } } // namespace maps::renderer5::test
