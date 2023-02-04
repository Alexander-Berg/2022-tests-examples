#include "geojson_to_shp.h"

#include <contrib/libs/gdal/ogr/ogrsf_frmts/ogrsf_frmts.h>
#include <contrib/libs/gdal/apps/gdal_utils.h>
#include <maps/libs/log8/include/log8.h>
#include <memory>

namespace maps::garden::modules::osm_coastlines_src::test {

void convertGeoJsonToSHP(const std::string& inFilePath, const std::string& outFilePath) {
    
    GDALAllRegister();

    GDALDataset* dataSource = GDALDataset::Open(inFilePath.c_str(), FALSE);

    CPLStringList argv;
    argv.AddString("-of");
    argv.AddString("ESRI Shapefile");
    GDALVectorTranslateOptions* opt = GDALVectorTranslateOptionsNew(argv.List(), nullptr);

    auto handle = GDALDataset::ToHandle(dataSource);
    auto newHandle = GDALVectorTranslate(outFilePath.c_str(), nullptr, 1, &handle, opt, nullptr);
    GDALClose(newHandle);
    GDALVectorTranslateOptionsFree(opt);

    REQUIRE(newHandle, "Failed to translate data from geojson to shp");
}

}
