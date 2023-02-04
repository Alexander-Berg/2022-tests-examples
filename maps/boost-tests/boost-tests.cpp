#include "include/RendererTest.h"

#include <yandex/maps/renderer5/core/syspaths.h>
#include <yandex/maps/renderer/io/io.h>
#include <maps/renderer/libs/base/include/compiler.h>
#include <maps/libs/common/include/exception.h>

// Disable tests with mocks until the issue with lucid build is resolved
#if !defined(YANDEX_MAPS_UBUNTU_VERSION) || YANDEX_MAPS_UBUNTU_VERSION > 1004
#include <yandex/maps/gmock/boost_test_init.h>
#endif

#include <maps/libs/common/include/exception.h>

#include <boost/filesystem.hpp>
#include <boost/test/unit_test_monitor.hpp>
#include <xmlwrapp/xmlwrapp.h>

using namespace maps::renderer5;
using namespace maps::renderer;
using namespace boost::unit_test;

namespace {
class GlobalContext
{
public:
    GlobalContext()
    {
        // load xml catalog file
        static const std::string catalogXmlFileName = "catalog.xml";
        if (boost::filesystem::exists(catalogXmlFileName))
            xml::set_xml_catalog(catalogXmlFileName);

        io::setTempDirPath("tmp");
        core::syspaths::setXsltPatchDir("schema/xslt");

#ifdef REN_PLATFORM_WIN
        core::syspaths::setFontsPath("../../renderer-fonts/fonts");
#endif

        io::dir::create(io::tempDirPath());

        boost::unit_test::unit_test_monitor
            .register_exception_translator<maps::Exception>(
                [](const maps::Exception& ex) {
                    std::stringstream ss;
                    ss << ex;
                    BOOST_ERROR(ss.str());
                });
    }

    ~GlobalContext()
    {}
};
}  // namespace

test_suite* init_main_unit_test_suite()
{
    test_suite* suite = BOOST_TEST_SUITE("Full test suite");

    suite->add(maps::renderer5::test::init_renderer_suite());

    return suite;
}

#if !defined(BOOST_TEST_DYN_LINK)

test_suite* init_unit_test_suite(int argc, char** const argv)
{
    return init_main_unit_test_suite();
}

#else

bool init_unit_test()
{
    boost::unit_test::framework::master_test_suite().add(init_main_unit_test_suite());

    return true;
}

int main( int argc, char* argv[])
{
    return ::boost::unit_test::unit_test_main( &init_unit_test, argc, argv );
}

#endif

BOOST_GLOBAL_FIXTURE( GlobalContext );
