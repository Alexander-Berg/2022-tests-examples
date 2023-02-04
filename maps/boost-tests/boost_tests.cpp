#define BOOST_TEST_MODULE "Tilerenderer tests"

#include "include/tools.h"
#include "include/connection_options.h"

#include <yandex/maps/renderer5/core/syspaths.h>
#include <yandex/maps/renderer/io/io.h>

#include <boost/filesystem.hpp>
#include <boost/test/unit_test.hpp>
#include <xmlwrapp/xmlwrapp.h>

using namespace maps::renderer;
using namespace maps::renderer5;
using namespace maps::tilerenderer4::test;

#define ROUTINE_SCOPE(f) namespace { f }
#include "include/routine.hpp"
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
        io::dir::create(io::tempDirPath());

        // db initialization is turned off by default.
        // we will see this work. If all test work for a long time, these lines
        // of code will be deleted. And routine.hpp also.
//         const std::string scriptName = "tests/boost-tests/sqlscripts/init.sql";
//         prepareDb(options, scriptName);
    }
};

}  // namespace

const std::string maps::tilerenderer4::test::options =
    " host=     pg94.maps.dev.yandex.net"
    " user=     renderer"
    " password= renderer"
    " port=     5432"
    " dbname=   renderer_test"
    " options=  --search_path=renderer_autotest,public";

const std::string maps::tilerenderer4::test::options2 =
    " host=     pg94.maps.dev.yandex.net"
    " user=     renderer"
    " password= renderer"
    " port=     5432"
    " dbname=   renderer_test"
    " options=  --search_path=renderer_autotest2,public";

BOOST_GLOBAL_FIXTURE( GlobalContext );
