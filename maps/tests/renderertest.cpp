#include "../renderer.h"
#include "../config.h"
#include "../strcast.hpp"

#include <yandex/maps/pgpool2/pgpool.h>
#include <fastcgi2/logger.h>
#include <yandex/maps/cgiutils2/logaux.h>
#include <yandex/maps/cgiutils2/exception.h>
#include <yandex/maps/cgiutils2/cgiutils.h>

#include <iostream>
#include <list>
#include <boost/version.hpp>
#include <boost/scoped_ptr.hpp>
#include <boost/scoped_array.hpp>
#include <fstream>
#include <pqxx>

#define BOOST_TEST_DYN_LINK
#define BOOST_TEST_MAIN
#include <boost/test/unit_test.hpp> 
#include <boost/test/unit_test_log.hpp>
#include <boost/test/test_tools.hpp>

using namespace maps::wiki::renderer;

class DummyLogger : public fastcgi::Logger {
public:
    DummyLogger(){}
protected:
    virtual void log(const Level /*level*/, const char* /*format*/, va_list /*args*/) {
    }
};

BOOST_AUTO_TEST_CASE(test_StrCast)
{
    std::string str("test string");
    std::wstring wstr = boost::lexical_cast<std::wstring>(str);
    std::string str_new = boost::lexical_cast<std::string>(wstr);
    BOOST_CHECK_EQUAL(str, str_new);
}

BOOST_AUTO_TEST_CASE(test_config)
{
    boost::scoped_ptr<DummyLogger> logger(new DummyLogger()); 
    maps::cgiutils::g_initLogger(logger.get());
    
    Config cfg("./tests/config-test.xml");
    BOOST_CHECK_EQUAL(cfg.layers().size(), 2);

    std::string defLayer = cfg.defaultLayerName();
    BOOST_CHECK_EQUAL(
        defLayer, 
        "test"); 

    Config::Layer l = cfg.layers().find("test")->second;
    BOOST_CHECK_EQUAL(
        l.path.string(), 
        "./tests/layers/test");    
}

BOOST_AUTO_TEST_CASE(test_Renderer)
{
    boost::scoped_ptr<DummyLogger> logger(new DummyLogger()); 
    maps::cgiutils::g_initLogger(logger.get());
    Config cfg("./tests/config-test.xml");
    Config::Layer l = cfg.layers().find("test")->second;
    Renderer renderer(l.name, l.path);
    maps::xml3::Doc dbconfig((l.path / "db.xml").string());
    maps::xml3::Node connnode = dbconfig.node("/db/write");
    std::string connstring =
        "host=" + connnode.attr<std::string>("host") +
        " port=" + connnode.attr<std::string>("port") +
        " dbname=wiki user=mapadmin password=mapadmin";
    pqxx::connection Conn(connstring);

    pqxx::work transaction(Conn);
    try {
        transaction.exec(
"DROP SCHEMA IF EXISTS wikirenderer_test CASCADE;\n"
"CREATE SCHEMA wikirenderer_test\n"
"CREATE TABLE wikirenderer_test.data (id serial, \n"
"                                   the_geom geometry,\n"
"                                   type integer NOT NULL,\n"
"                                   CONSTRAINT test_pk PRIMARY KEY(id));\n"
"INSERT INTO wikirenderer_test.data(the_geom, type) \n"
"VALUES(ST_PolygonFromText('POLYGON((1 1,5 1,5 5,1 5,1 1))', 3395), 1);\n"
"INSERT INTO wikirenderer_test.data(the_geom, type) \n"
"VALUES(ST_PolygonFromText('POLYGON((7 7,7 8,8 8,8 7,7 7))', 3395), 2);\n"
                         );
        transaction.commit();
    } catch (const std::exception &e) {
        std::cerr << e.what() << std::endl;
        transaction.abort();
    }

    RequestParams reqParams("x=0&y=0&z=0", cfg);
    IRawDataPtr result  = renderer.render(reqParams, "", "", "");
    std::ifstream f("./first.png", std::ios_base::binary);
    std::vector<char> referenceData(result->size());
    f.read(&referenceData[0], result->size());
    f.seekg(0, std::ios::end);
    
    BOOST_CHECK_EQUAL(f.tellg(), 4619);
    BOOST_CHECK_EQUAL(result->size(), 4619);
    BOOST_CHECK(std::equal(referenceData.begin(), referenceData.end(), result->data()));
}
