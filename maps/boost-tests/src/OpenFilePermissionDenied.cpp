#include "tests/boost-tests/include/tools/map_tools.h"
#include <yandex/maps/renderer/io/io.h>
#include <yandex/maps/renderer/io/errors.h>
#include <boost/test/unit_test.hpp>

#include <sys/stat.h>

using namespace maps::renderer;
using namespace maps::renderer5;

namespace {

const char* srcFileName = "tests/boost-tests/maps/StaticMapCreation.xml";
const char* dstFileName = "tmp/StaticMapCreation.xml";

}  // namespace

BOOST_AUTO_TEST_SUITE(open_file)

BOOST_AUTO_TEST_CASE(readPermissionDenied)
{
#ifdef REN_PLATFORM_LINUX
    test::map::deleteFilesFromTmpDir();
    io::file::write(io::file::open(srcFileName), dstFileName);
    BOOST_REQUIRE(chmod(dstFileName, 0000) == 0);
    BOOST_CHECK_THROW(io::file::open(dstFileName), io::errors::CantOpenFile);
    test::map::deleteFilesFromTmpDir();
#endif  // #ifdef REN_PLATFORM_LINUX
}

BOOST_AUTO_TEST_CASE( writePermissionDenied )
{
    test::map::deleteFilesFromTmpDir();
    io::file::write(io::file::open(srcFileName), dstFileName);
    test::chmodFile(dstFileName, test::ReadOnly);
    BOOST_CHECK_THROW(io::file::create(dstFileName), io::errors::CantCreateFile);
    test::map::deleteFilesFromTmpDir();
}

BOOST_AUTO_TEST_SUITE_END()
