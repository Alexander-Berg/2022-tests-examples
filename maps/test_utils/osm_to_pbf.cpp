#include "osm_to_pbf.h"

#include <contrib/libs/libosmium/include/osmium/io/any_input.hpp>
#include <contrib/libs/libosmium/include/osmium/io/any_output.hpp>

namespace maps::garden::modules::osm_to_yt::test {

void convertOsmToPbf(const std::string& inputFile, const std::string& outputFile) {
    osmium::io::File osmFile{inputFile};
    osmium::io::File pbfFile{outputFile};
    osmium::io::Reader reader{osmFile};
    osmium::io::Writer writer{pbfFile};
    while (!reader.eof()) {
        writer(reader.read());
    }
}

} // namespace maps::garden::modules::osm_to_yt::test
