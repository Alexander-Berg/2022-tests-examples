#pragma once

#include "mapcompiler/compilation_setting.h"

#include <yandex/maps/renderer5/core/Map.h>

#include <maps/renderer/libs/base/include/geom/box.h>

#include <string>

namespace maps { namespace renderer5 { namespace test {

struct CompilationSetting
{
    const char* srcFileName;
    const char* dstFileName;
    const std::string repositoryPath;
    bool forNavi;
    bool useRG;
    bool updateExtent;
    bool isolateTransaction;
    bool packFiles;
    std::string dboptions;
    int zmin;
    int zmax;
    renderer::base::BoxD window;
};

mapcompiler::Options prepareOptions(CompilationSetting& cmp);

void checkResources(core::Map& map, bool forNavi);

void normalizeSourceFileNames(core::Map& map);

void prepareSrcMap(core::Map& src, CompilationSetting& cmp);

void prepareDstMap(core::Map& dst, CompilationSetting& cmp, core::Map& src);

void prepareSetting(
    mapcompiler::CompilationSetting& setting,
    const mapcompiler::Options& options,
    const renderer::base::BoxD& window,
    core::Map& src,
    core::Map& dst);

mapcompiler::Id2Checksum readChecksum(const std::string &fileName);

std::string writeChecksum(const mapcompiler::Id2Checksum& sum, const std::string &name);

struct CompilationResult
{
    core::ILayerPtr rootLayer;
    mapcompiler::Id2Checksum checksum;
};

CompilationResult compile(CompilationSetting& cmp);


} } } // namespace maps::renderer5::test
