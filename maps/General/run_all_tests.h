#pragma once

#include "tile_loader.h"

namespace maps::renderer::check {

struct Options
{
    Options();

    // tiles source can be url (e.g.
    // https://core-renderer-tilesgen.testing.maps.yandex.net/vmap2/tiles)
    // or list of compiled maps files (e.g. cis1.tar,aao.tar,na.tar, not implemented)
    Options(const std::string& goldenTilesSrc,
            const std::string& testTilesSrc,
            size_t numThreads,
            const std::string& reportFile,
            const std::vector<std::string>& testList);

    std::unique_ptr<TileLoader> goldenTileLoader;
    std::unique_ptr<TileLoader> testTileLoader;

    size_t numThreads;

    std::string reportFile;

    std::vector<std::string> testList;
};

Options parseCmd(int argc, char* argv[]);

void runAllTests(const Options& options);

} // namespace maps::renderer::check
