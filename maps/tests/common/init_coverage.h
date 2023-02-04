#pragma once
#include <boost/test/auto_unit_test.hpp>
#include <library/cpp/testing/common/env.h>
#include <maps/analyzer/libs/common/include/region_map.h>
#include <filesystem>

#include <maps/analyzer/libs/geoinfo/include/geoid.h>

namespace fs = std::filesystem;

const std::string COVERAGE_DIR = "coverage5";

const std::string PATH_TO_COVERAGE_DIR = "maps/analyzer/data/coverage";

const maps::geoinfo::GeoId& getGeoIdTrf() {
    static const maps::geoinfo::GeoId geoId(BinaryPath(PATH_TO_COVERAGE_DIR).c_str(), "trf");
    return geoId;
}

struct CopyCoverageFiles
{
    CopyCoverageFiles()
    {
        fs::path coverage_dir(COVERAGE_DIR);
        if (fs::exists(coverage_dir)) {
            fs::remove_all(coverage_dir);
        }
        fs::create_directory(coverage_dir);
        fs::create_symlink(
            static_cast<std::string>(BinaryPath("maps/analyzer/data/coverage/geoid.mms.1")),
            coverage_dir / "geoid.mms.1"
        );
        fs::create_symlink(
            static_cast<std::string>(BinaryPath("maps/analyzer/data/coverage/trf.mms.1")),
            coverage_dir / "trf.mms.1"
        );
        maps::analyzer::setDefaultRegionMapLayersConfig(COVERAGE_DIR);
    }
};

struct RemoveCoverageFiles
{
    RemoveCoverageFiles()
    {
        fs::path coverage_dir(COVERAGE_DIR);
        if (fs::exists(coverage_dir))
        {
            fs::remove_all(coverage_dir);
        }
        fs::create_directory(coverage_dir);
        fs::create_symlink(
            static_cast<std::string>(BinaryPath("nonexistent/file")),
            coverage_dir / "geoid.mms.1"
        );
        fs::create_symlink(
            static_cast<std::string>(BinaryPath("nonexistent/file")),
            coverage_dir / "trf.mms.1"
        );

        maps::analyzer::setDefaultRegionMapLayersConfig(COVERAGE_DIR);
    }
};
