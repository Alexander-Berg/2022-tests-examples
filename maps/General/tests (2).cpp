#define DEBUG_MEMORY
#define DEBUG_MEMORY_FREED
#define DEBUG_MEMORY_LOCATION

#include "utils.h"
#include "polyline.h"
#include "simplify.h"
#include "simplify_block.h"
#include "config.h"
#include "xmltool.h"
#include <maps/libs/common/include/exception.h>
#include <maps/libs/xml/include/xml.h>

#include <fstream>
#include <sstream>
#include <stdexcept>

#include <iostream>
#define BOOST_AUTO_TEST_MAIN
#define BOOST_TEST_DYN_LINK
#define BOOST_TEST_MAIN

#include <boost/test/auto_unit_test.hpp>
#include <boost/test/test_tools.hpp>


#ifndef BOOST_AUTO_UNIT_TEST
#define BOOST_AUTO_UNIT_TEST BOOST_AUTO_TEST_CASE
#endif



BOOST_AUTO_UNIT_TEST(startup)
{
   maps::xsltext::SimplifierConfig::init(NULL);
}

BOOST_AUTO_UNIT_TEST(readDoubles)
{
    const std::string test = "  3.0\n5 4.0 009  25     16  \n";
    std::vector<double> doublev;
    BOOST_CHECK_NO_THROW(maps::xslt::readDoubles(test, doublev));
    BOOST_CHECK(doublev.size() == 6);
    BOOST_CHECK(123.0 == strtod("     123", NULL));
    BOOST_CHECK(doublev[0] * doublev[0] + doublev[2] * doublev[2] ==
        doublev[4]);
    BOOST_CHECK_THROW(maps::xslt::readDoubles("test01", doublev), maps::Exception);
}

BOOST_AUTO_UNIT_TEST(simplify_encode)
{
    const std::string test =
        "55.556900 37.433200 55.558100 37.434800"
        " 55.559200 37.435800 55.563100 37.441200"
        " 55.565900 37.445200 55.565900 37.445200 "
        " 55.568500";
    std::vector<double> doublev;
    BOOST_CHECK_NO_THROW(maps::xslt::readDoubles(test, doublev));
    maps::lod::Polyline inputCurve;
    maps::curve::readCurve(test, inputCurve);
    maps::curve::simplify(inputCurve,
        0,
        maps::xsltext::config()->mapZoomLevels(),
        maps::xsltext::config()->verySmall());
    BOOST_CHECK("JLtPA3AvOwLUv08DsDU7AiDETwOYOTsCXNNPA7BOOwJM3k8DUF47Ag=="
        == maps::curve::encode(inputCurve));
    BOOST_CHECK("AHHJA" == maps::curve::levels(inputCurve));
    maps::curve::setLevel(inputCurve, maps::detail::minLevel);
    BOOST_CHECK("AAAAA" == maps::curve::levels(inputCurve));
}


namespace maps
{
namespace xsltext
{

namespace insertlodInternal{
xmlNodePtr
generateLODNode(
    xmlNodePtr node
    , size_t minSimplifySize
    , size_t mapZoomLevels
    , double verySmall
    );
}}}

BOOST_AUTO_UNIT_TEST(insertlod)
{
    //! The purpose of this test is leak check
    //! with valgrind
    const std::string testFile = "isertlod.xml";
    xmlDoc* doc =  readXML(testFile);
    xmlAddChild(doc->children,
        maps::xsltext::insertlodInternal::generateLODNode(
            findDescendantByName(doc->children, "GeoObjectCollection")
            , 1
            , 23
            , 0.001
            )
        );
    BOOST_CHECK_NO_THROW(saveXML(doc));
    xmlFreeDoc(doc);
}

namespace xscript
{
class SimplifyBlockTest
{
public:
    static void doTest()
    {
        const std::string test =
        "55.556900 37.433200 55.558100 37.434800"
        " 55.559200 37.435800 55.563100 37.441200"
        " 55.565900 37.445200";
        maps::lod::Polyline inputCurve;
        maps::curve::readCurve(test, inputCurve);
        std::string fmt = xscript::SimplifyBlock::formatCurve(inputCurve);
        BOOST_CHECK(test == fmt);
    }
};
}

BOOST_AUTO_UNIT_TEST(simplify_block_test)
{
    xscript::SimplifyBlockTest::doTest();
}

BOOST_AUTO_UNIT_TEST(shutdown)
{
   maps::xsltext::SimplifierConfig::destroy();
}
