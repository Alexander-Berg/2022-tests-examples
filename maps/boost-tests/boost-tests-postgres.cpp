#define BOOST_TEST_MODULE "Renderer postgres tests"

#include "include/postgres/connection_options.h"

#include <yandex/maps/renderer5/core/syspaths.h>
#include <yandex/maps/renderer/io/io.h>

#include <xmlwrapp/xmlwrapp.h>

#include <boost/test/unit_test.hpp>
#include <boost/filesystem.hpp>

using namespace maps::renderer;
using namespace maps::renderer5;

#define ROUTINE_SCOPE(f) namespace { f }
#include "include/postgres/routine.hpp"
#undef ROUTINE_SCOPE

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

        io::dir::create(io::tempDirPath());

        #ifdef REN_PLATFORM_WIN
            _setmaxstdio(2048);
        #endif

        // db initialization is turned off by default.
        // we will see this work. If all test work for a long time, these lines
        // of code will be deleted. And routine.hpp also.
        // const std::string scriptName = "tests/sqlscripts/init.sql";
        // prepareDb(test::postgres::options, scriptName);
    }
};
} // namespace

const std::string test::postgres::options =
    " host=     pg94.maps.dev.yandex.net"
    " user=     renderer"
    " password= renderer"
    " port=     5432"
    " dbname=   renderer_test"
    " options=  --search_path=renderer_autotest,public";

BOOST_GLOBAL_FIXTURE( GlobalContext );
