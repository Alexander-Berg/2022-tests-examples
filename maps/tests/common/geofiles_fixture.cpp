#include <maps/infopoint/tests/common/fixture.h>

ArcadiaGeofilesFixture::ArcadiaGeofilesFixture()
{
    infopoint::resetCoverageFilePath(static_cast<std::string>(BinaryPath("maps/data/test/geoid/geoid.mms.1")));
    infopoint::resetGeodataFilePath(static_cast<std::string>(BinaryPath("maps/data/test/geobase/geodata4.bin")));
}

ArcadiaGeofilesFixture::~ArcadiaGeofilesFixture()
{
    infopoint::resetCoverageFilePath();
    infopoint::resetGeodataFilePath();
}
