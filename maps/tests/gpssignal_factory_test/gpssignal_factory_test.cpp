#define BOOST_TEST_MAIN
#include <boost/test/test_tools.hpp>

#include <maps/analyzer/libs/gpssignal_parser/impl/tools.h>
#include <maps/analyzer/libs/gpssignal_parser/impl/gpssignal_xml_factory.h>

#include <maps/libs/common/include/cgiparser.h>

#include <boost/test/auto_unit_test.hpp>
#include <boost/assign/std/vector.hpp>

#include "helper.h"

using namespace boost::assign;
using namespace maps;
using namespace std;
using namespace boost::posix_time;
using namespace maps::analyzer;
using namespace maps::analyzer::data;

BOOST_AUTO_TEST_CASE(check_parse_auto)
{
    Helper helper;
    helper
        .type("auto")
        .lat("55.843")
        .lon("37.256")
        .avgSpeed("60")
        .direction("115.3")
        .uuid("662")
        .time("07122009:000009");

    GpsSignal signal = cgi::parseSignalFromString(helper.dict());

    helper.checkEqual(signal);

}

BOOST_AUTO_TEST_CASE(check_parse_from_xml_large_speed)
{
    Helper helper;
    helper
        .lat("55.843")
        .lon("37.256")
        .avgSpeed("4.29497e+09")
        .direction("115.1")
        .uuid("662")
        .uuid("662")
        .time("07122009:000009");

    xml::GpsSignalFactory factory;
    GpsSignal signal = factory.parseFromXml(helper.clid(), helper.uuid(),
        false, helper.xml());

    helper.checkEqual(signal);
}

BOOST_AUTO_TEST_CASE(check_parse_from_dict_clid)
{
    Helper helper;
    helper
        .lat("55.843")
        .lon("37.256")
        .avgSpeed("60")
        .direction("115")
        .clid("123")
        .uuid("662")
        .time("07122009:000009");

    xml::GpsSignalFactory factory;
    GpsSignal signal = cgi::parseSignalFromString(helper.dict());

    helper.checkEqual(signal);

    signal = factory.parseFromXml(helper.clid(), helper.uuid(),
        false, helper.xml());

    helper.checkEqual(signal);
}

BOOST_AUTO_TEST_CASE(check_parse_direction)
{
    Helper helper;
    helper
        .lat("55.843")
        .lon("37.256")
        .avgSpeed("60")
        .clid("123")
        .uuid("662")
        .time("07122009:000009");

    xml::GpsSignalFactory factory;
    GpsSignal signal = cgi::parseSignalFromString(helper.dict());
    BOOST_CHECK(!signal.data().has_direction());

    signal = factory.parseFromXml(
        helper.clid(), helper.uuid(), false, helper.xml());
    BOOST_CHECK(!signal.data().has_direction());

    helper.direction("115");
    signal = cgi::parseSignalFromString(helper.dict());
    BOOST_CHECK_EQUAL(helper.direction(), signal.direction());

    helper.direction("115.5");
    signal = cgi::parseSignalFromString(helper.dict());
    BOOST_CHECK_EQUAL(helper.direction(), signal.direction());

    vector<string> dirs;
    dirs += "115.5A", "A", "";
    for(size_t i = 0; i < dirs.size(); ++i) {
        helper.direction(dirs[i]);

        GpsSignal signalDict = cgi::parseSignalFromString(helper.dict());
        BOOST_CHECK(!signalDict.data().has_direction());

        GpsSignal signalXml = factory.parseFromXml(
            helper.clid(), helper.uuid(), false, helper.xml());
        BOOST_CHECK(!signalXml.data().has_direction());
    }
}

BOOST_AUTO_TEST_CASE(check_parse_incorrect_coords)
{
    Helper helper;
    helper
        .clid("123")
        .lat("55,843")
        .lon("37,256")
        .avgSpeed("60")
        .direction("115")
        .uuid("662")
        .time("07122009:000009");

    xml::GpsSignalFactory factory;
    BOOST_CHECK_THROW(cgi::parseSignalFromString(helper.dict()),
        maps::Exception);
    BOOST_CHECK_THROW(factory.parseFromXml(helper.clid(), helper.uuid(),
        false, helper.xml()), maps::Exception);
}

BOOST_AUTO_TEST_CASE(check_parse_incorrect_direction)
{
    Helper helper;
    helper
        .clid("123")
        .lat("55,843")
        .lon("37,256")
        .avgSpeed("6")
        .direction("115.2")
        .uuid("662")
        .time("07122009:000009");

    xml::GpsSignalFactory factory;

    BOOST_CHECK_THROW(cgi::parseSignalFromString(helper.dict()), maps::Exception);
    BOOST_CHECK_THROW(factory.parseFromXml(helper.clid(), helper.uuid(),
        false, helper.xml()), maps::Exception);

    helper.direction("115,2");
    BOOST_CHECK_THROW(cgi::parseSignalFromString(helper.dict()), maps::Exception);
    BOOST_CHECK_THROW(factory.parseFromXml(helper.clid(), helper.uuid(),
        false, helper.xml()), maps::Exception);

    helper.direction("115;2");
    BOOST_CHECK_THROW(cgi::parseSignalFromString(helper.dict()), maps::Exception);
    BOOST_CHECK_THROW(factory.parseFromXml(helper.clid(), helper.uuid(),
        false, helper.xml()), maps::Exception);

    helper.direction("115:2");
    BOOST_CHECK_THROW(cgi::parseSignalFromString(helper.dict()), maps::Exception);
    BOOST_CHECK_THROW(factory.parseFromXml(helper.clid(), helper.uuid(),
        false, helper.xml()), maps::Exception);
}


BOOST_AUTO_TEST_CASE(check_parse_incorrect_time)
{
    Helper helper;
    helper
        .clid("123")
        .lat("55,843")
        .lon("37,256")
        .avgSpeed("6")
        .direction("115.2")
        .uuid("662")
        .time("99999999:999999");

    xml::GpsSignalFactory factory;

    BOOST_CHECK_THROW(cgi::parseSignalFromString(helper.dict()), maps::Exception);
    BOOST_CHECK_THROW(factory.parseFromXml(helper.clid(), helper.uuid(), false,
                                           helper.xml()), maps::Exception);
}

