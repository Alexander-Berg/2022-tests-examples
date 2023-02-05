#include "../config.h"
#include "../wikimap_i.h"
#include "../factory.h"
#include "../linearelement.h"
#include "../log.h"
#include "../xmlobject.h"
#include "../properties_impl.h"
#include "../user.h"

#include <yandex/maps/tileutils4/tile.h>
#include <maps/libs/xml/include/xml.h>
#include <yandex/string_helpers.h>

#include <iostream>
#include <string>
#include <sstream>
#include <fstream>

#include <pqxx>

#define BOOST_TEST_DYN_LINK
#define BOOST_TEST_MAIN
#include <boost/test/unit_test.hpp>
#include <boost/version.hpp>
#include <boost/scoped_ptr.hpp>
#include <boost/pool/detail/singleton.hpp>
#include <boost/regex.hpp>

//TODO: more robust tests

using namespace maps::wiki;

typedef boost::shared_ptr<Yandex_Maps_WikiMapCore_i> TServant;
typedef const Config* TConfig;

const maps::wiki::TZoom testZoom = 17;

struct InitConfig
{
    InitConfig()
        : cfgHolder_("../../configs/servant/servant-coretest.cfg")
    { }

    ConfigHolder cfgHolder_;
};
BOOST_GLOBAL_FIXTURE(InitConfig);

TConfig config()
{
    return TConfig(maps::wiki::cfg());
}

TServant createServant(TConfig config) {
    TServant serv(new Yandex_Maps_WikiMapCore_i(config));
    return serv;
}

std::string loadFile(const std::string& fileName)
{
    std::string line;
    std::ifstream fs( fileName.c_str(), std::ios_base::in);
    std::getline(fs,line);
    return line;
}

std::string loadEtalon(const std::string& etalonFileName)
{
    return loadFile(etalonFileName);
}

std::string removeVersionData(const std::string& original)
{
    boost::regex exp1("<wm:objectId>[^<]*</wm:objectId>");
    boost::regex exp2("<wm:id>[^<]*</wm:id>");
    boost::regex exp4("<wm:state>[^<]*</wm:state>");
    boost::regex exp5("<wm:Revision>.*?</wm:Revision>");
    boost::regex exp6("<wm:token>.*?</wm:token>");
    boost::regex exp7("<wm:AttributeLookup.*?</wm:AttributeLookup>");

    std::string ret = boost::regex_replace(original, exp1, "<OBJECTID/>");
    ret = boost::regex_replace(ret, exp2, "<ID/>");
    ret = boost::regex_replace(ret, exp4, "<STATE/>");
    ret = boost::regex_replace(ret, exp5, "<EDITNOTES/>");
    ret = boost::regex_replace(ret, exp6, "<TOKEN/>");
    ret = boost::regex_replace(ret, exp7, "<ATTRIBUTELOOKUP/>");
    return ret;
}

void saveEtalon(const std::string& etalonFileName, const std::string& etalon)
{
    std::cout << "Etalon update!" << std::endl;
    std::string line;
    std::ofstream fs( etalonFileName.c_str(), std::ios_base::out);
    fs<<etalon;
}

//TODO: rewrite
/*BOOST_AUTO_TEST_CASE(test_SetReadOnly)
{
    TServant serv = createServant(config());
    serv->SetState(58414218, 3843371, "read-only","","");
    std::string result1 = XMLString2String(serv->GetObject(3843371, 0, 0, 13, config()->wikimapsUser(), "", true));
    serv->SetState(58414218, 3843371, "editable","","");
    std::string result3 = XMLString2String(serv->GetObject(3843371, 0, 0, 13, config()->wikimapsUser(), "", true));
    maps::xml3::Doc doc1 = maps::xml3::Doc::fromString(result1);
    doc1.addNamespace("wm", "http://maps.yandex.ru/wikimap/1.x");
    BOOST_CHECK_NO_THROW(doc1.nodes("//wm:readOnly[.='true']"));
    maps::xml3::Doc doc2 = maps::xml3::Doc::fromString(result1);
    doc2.addNamespace("wm", "http://maps.yandex.ru/wikimap/1.x");
    BOOST_CHECK_NO_THROW(doc2.nodes("//wm:readOnly[.='false']"));
}

//TODO: Rewrite
BOOST_AUTO_TEST_CASE(test_SaveObject)
{
    TServant serv = createServant(config());

    //"Australian" road
    std::string result = XMLString2String( serv->GetObject(3843371, 0, 0, 13, config()->wikimapsUser(), "", true));
    //std::cout << result << std::endl;
    std::string regetResult = XMLString2String( serv->GetObject(3843371, 0, 0, 13, config()->wikimapsUser(), "", true) );
    //std::string saveResult = XMLString2String( serv->SaveObject(19362792, 0,0,testZoom, *xmlRequest, true) );
    //std::cout << saveResult << std::endl;

    BOOST_CHECK(result != regetResult);
    if(result == regetResult){
        std::cout << "EQUAL" << std::endl << result << std::endl << regetResult << std::endl;
    }
    result = removeVersionData(result);
    regetResult = removeVersionData(regetResult);
    BOOST_CHECK(result == regetResult);
    if(result != regetResult){
        std::cout << "NOTEQUAL" << std::endl << result << std::endl << regetResult << std::endl;
    }
}

//TODO: rewrite
BOOST_AUTO_TEST_CASE(test_GetObjectsByTile)
{
    std::string etalon = loadEtalon( std::string("data/test_getobjectsbytile.xml") );
    TServant serv = createServant(config());
    //GeoCoord(23.09185,40.439784);
    //2570572.98347465 4902335.98850007;
    maps::tileutils4::Tile tl(
        maps::tileutils4::RealCoord(2570572.98347465, 4902335.98850007).toTileCS(), 12);
    std::string result = XMLString2String( serv->GetObjectsByTile(tl.coord().x(), tl.coord().y(), 12, true, ""));
    //std::cout << result << std::endl;
    //std::cout << etalon << std::endl;
    //saveEtalon("data/test_getobjectsbytile.xml", result);
    BOOST_CHECK(result == etalon);
}

BOOST_AUTO_TEST_CASE(test_SetObjectState_History)
{
    TServant serv = createServant(config());
    //"Sydney"
    // TODO: Write separate test
    std::string result = XMLString2String( serv->GetObjectHistory(3844354, 3, 1, 0, "", 0) );
    //std::cout << result << std::endl;
    maps::xml3::Doc doc = maps::xml3::Doc::fromString(result);
    doc.addNamespace("gml", "http://www.opengis.net/gml");
    doc.addNamespace("wm", "http://maps.yandex.ru/wikimap/1.x");
    result = XMLString2String( serv->SetState(config()->wikimapsUser(), 3844354, "approved", "", ""));
    BOOST_CHECK(result.find("token") != std::string::npos);
    result = XMLString2String( serv->SetState(config()->wikimapsUser(), 3844791, "approved", "", ""));
    BOOST_CHECK(result.find("token") != std::string::npos);
}

//TODO: check that revisions list is the same in GetObjectHistory and GetRevisions
BOOST_AUTO_TEST_CASE(test_GetRevisions)
{
    TServant serv = createServant(config());
    //"Sydney"
    std::string result = XMLString2String( serv->GetObjectHistory(3843371, 5, 1, 0, "",0) );
    maps::xml3::Doc doc = maps::xml3::Doc::fromString(result);
    doc.addNamespace("gml", "http://www.opengis.net/gml");
    doc.addNamespace("wm", "http://maps.yandex.ru/wikimap/1.x");

    maps::xml3::Nodes nodes = doc.nodes("//wm:Revision/wm:id");
    std::stringstream r;
    for(size_t i = 0; i < nodes.size(); i++){
        if(i)
            r << " ";
        r << nodes[i].value<unsigned long int>();
    }
    result = XMLString2String( serv->GetRevisions(r.str().c_str(), "",0));
    //std::cout << r.str() << std::endl;
    //std::cout << result << std::endl;
}

BOOST_AUTO_TEST_CASE(test_LinearElement)
{
    maps::xml3::Doc doc("data/test_road.xml");
    doc.addNamespace("gml", "http://www.opengis.net/gml");
    doc.addNamespace("ym", "http://maps.yandex.ru/ymaps/1.x");
    doc.addNamespace("wm", "http://maps.yandex.ru/wikimap/1.x");
    maps::pgpool2::PgTransaction work = config()->connMgr().readTransaction();
    maps::wiki::XmlObject xmlObject(doc.node("//ym:GeoObject"), *work);
    std::auto_ptr<maps::wiki::LinearElement> linearElement(dynamic_cast<maps::wiki::LinearElement*>(
        maps::wiki::GeoObjectFactory().createObject( xmlObject))
        );
    BOOST_CHECK(linearElement.get());
    maps::wiki::Geom point(maps::wiki::createPoint("37.46017626097202 55.68844238911158"));
    std::auto_ptr<maps::wiki::Junction> junction(dynamic_cast<maps::wiki::Junction*>(
        maps::wiki::GeoObjectFactory().createObject(linearElement->junctionLayer(), *work)
        ));
    BOOST_CHECK(junction.get());
    junction->setPosition(point);
    maps::wiki::JunctionRef& ref = linearElement->setJunction(junction.get());
    BOOST_CHECK_EQUAL(ref.kind(), maps::wiki::JunctionRef::middle);
    BOOST_CHECK_EQUAL(ref.index(), 1);
}

BOOST_AUTO_TEST_CASE(test_XmlFilters)
{
    maps::xml3::Doc doc("data/test_filter_railwayst.xml");
    doc.addNamespace("gml", "http://www.opengis.net/gml");
    doc.addNamespace("ym", "http://maps.yandex.ru/ymaps/1.x");
    doc.addNamespace("wm", "http://maps.yandex.ru/wikimap/1.x");
    maps::pgpool2::PgTransaction work = config()->connMgr().readTransaction();
    maps::wiki::XmlObject xmlObject(doc.node("//ym:GeoObject"), *work);
    maps::wiki::XmlObjectProperties xmlObjectProperties(xmlObject);
    maps::wiki::StringMap filterOutput = config()->wikimaps()->filtersGroup("xmlfilters").apply(xmlObjectProperties);
    maps::wiki::StringMap::const_iterator it = filterOutput.find("layer_id");
    BOOST_CHECK(it != filterOutput.end() && it->second == "33");
}*/

BOOST_AUTO_TEST_CASE(test_User)
{
    maps::pgpool2::PgTransaction work = config()->connMgr().writeTransaction();
    work->exec(
"delete from social.user_settings where uid between 1 and 7; \
insert into social.user_settings (uid, registration_date, banned) VALUES(1, '2009-10-06 14:02:58.151703', 0); \
insert into social.user_settings (uid, registration_date, banned,ban_date) VALUES(2,'2009-10-06 14:02:58.151703', 2,'2009-10-08 14:02:58.151703' ); \
insert into social.user_settings (uid, registration_date, banned,ban_date,ban_end_date) VALUES(3, '2009-10-06 14:02:58.151703', 1,'2009-10-08 14:02:58.151703', '2030-10-08 14:02:58.151703'); \
insert into social.user_settings (uid, registration_date, banned,ban_date,ban_end_date) VALUES(4, '2009-10-06 14:02:58.151703', 1,'2013-12-01 14:02:58.151703', NOW() - interval '6 day' -interval '22 hour'); \
insert into social.user_settings (uid, registration_date, banned,ban_date,ban_end_date) VALUES(5,  '2009-10-06 14:02:58.151703', 1,'2013-12-01 14:02:58.151703', NOW()-interval '100 second'); \
insert into social.user_settings (uid, registration_date, banned,ban_date,ban_end_date) VALUES(6,  '2009-10-06 14:02:58.151703', 1,'2013-12-01 14:02:58.151703', NOW()-interval '7 day'-interval '100 second'); \
insert into social.user_settings (uid, registration_date, banned) VALUES(7,  NOW()-interval '2 day' - interval '100 second', 0);"
    );
    work->commit();

    work = config()->connMgr().readTransaction();
    User user1(1), user2(2), user3(3), user4(4), user5(5), user6(6), user7(7) ;

    BOOST_CHECK( user1.isUnderSuspicion(*work) == false);
    BOOST_CHECK( user2.isUnderSuspicion(*work) == true);
    BOOST_CHECK( user3.isUnderSuspicion(*work) == true);
    BOOST_CHECK( user4.isUnderSuspicion(*work) == true);
    BOOST_CHECK( user5.isUnderSuspicion(*work) == true);
    BOOST_CHECK( user6.isUnderSuspicion(*work) == false);
    BOOST_CHECK( user7.isUnderSuspicion(*work) == false);

}


maps::wiki::TOid
primaryObjectId(const std::string& xml)
{
    //std::cout << xml << std::endl;
    maps::xml3::Doc doc = maps::xml3::Doc::fromString(xml);
    doc.addNamespace("wm", "http://maps.yandex.ru/wikimap/1.x");
    maps::xml3::Node node = doc.node("//wm:ObjectMetaData[wm:EditContext/wm:status='primary']", false);
    node.addNamespace("wm", "http://maps.yandex.ru/wikimap/1.x");
    maps::xml3::Node idnode = node.node("wm:id", false);
    return idnode.value<TOid>();
}

bool
checkObjectNotes(TServant serv, maps::wiki::TOid id, const std::string& spaceDelimetedNotes) {
    std::stringstream s(spaceDelimetedNotes);
    std::vector<std::string> notesToLookFor;
    std::copy(std::istream_iterator<std::string>(s),
        std::istream_iterator<std::string>(), std::back_inserter(notesToLookFor));
    std::string result = XMLString2String(serv->GetObject(id,
        30.270735863596215, 51.26952410358761, 13,
        82282794, "", true));
    //std::cout << result << std::endl;
    maps::xml3::Doc docWithNotes = maps::xml3::Doc::fromString(result);
    docWithNotes.addNamespace("wm", "http://maps.yandex.ru/wikimap/1.x");
    maps::xml3::Node node =
        docWithNotes.node("//wm:ObjectMetaData[wm:EditContext/wm:status='primary']", false);
    maps::xml3::Nodes n = node.nodes("//wm:Revision/wm:notes/wm:note", false);
    BOOST_CHECK_EQUAL(n.size(), notesToLookFor.size());
    bool ret = true;
    for(size_t x = 0; x < n.size(); x++){
        std::string note = n[x].value<std::string>();
        if(std::find(notesToLookFor.begin(), notesToLookFor.end(), note) == notesToLookFor.end()){
            std::cout << "Note '" << note << "' not found in '" << spaceDelimetedNotes << "'" << std::endl;
            ret = false;
        }
    }
    return ret;
}

void
eliminateGarbage(TConfig config)
{
    maps::pgpool2::PgTransaction work = config->connMgr().writeTransaction();
    work->exec("update core.objects set state=2 where ST_DWithin(the_geom, ST_Transform(SetSrid(ST_MakePoint(30.270735863596215, 51.26952410358761), 4326), 3395), 10000);");
    work->commit();
}

//TODO: Rewrite
// BOOST_AUTO_TEST_CASE(test_editnotes)
// {
//     eliminateGarbage(config());
//     TServant serv = createServant(config());
//     std::string template_request1 = loadFile("data/editnotes.xml");
//     Yandex::XMLString* xmlRequest = String2XMLString(template_request1);
//     std::string result1 = XMLString2String(serv->GetObjectTemplate(
//         config()->wikimapsUser(),
//         13,
//         *xmlRequest,
//         30.270735863596215, 51.26952410358761));
//    //std::cout << result1 << std::endl;
//    xmlRequest = String2XMLString(result1);
//     std::string result2 = XMLString2String(serv->SaveObject(
//         config()->wikimapsUser(),
//         30.270735863596215, 51.26952410358761,13,
//         *xmlRequest, true,""));
//     //std::cout << "#######" << result2 <<  "#######" << std::endl;
//     TOid id = primaryObjectId(result2);
//     std::cout << "TEST_EDITNOTES WORK OBJECT ID IS:" << id << std::endl;
//     BOOST_CHECK(checkObjectNotes(serv, id, "created"));
//     maps::xml3::Doc doc = maps::xml3::Doc::fromString(result2);
//     doc.addNamespace("gml", "http://www.opengis.net/gml");
//     doc.addNamespace("wm", "http://maps.yandex.ru/wikimap/1.x");
//     maps::xml3::Nodes geoms = doc.nodes("//gml:MultiGeometry");
//     for(size_t n = 0; n < geoms.size(); n ++){
//         maps::xml3::Node geom = geoms[n];
//         geom.remove();
//     }
//     std::string nogeom;
//     doc.save(nogeom);
//     //std::cout << nogeom << std::endl;
//     xmlRequest = String2XMLString(nogeom);
//     std::string result3 = XMLString2String(serv->SaveObject(
//         config()->wikimapsUser(),
//         30.270735863596215, 51.26952410358761,13,
//         *xmlRequest, true,""));
//     //std::cout << result3 <<std::endl;
//     BOOST_CHECK(checkObjectNotes(serv, id, "modified modified_attributes modified_description"));
//     serv->SetState(config()->wikimapsUser(), id, "deleted", "", "");
//     BOOST_CHECK(checkObjectNotes(serv, id, "deleted"));
//     serv->SetState(config()->wikimapsUser(), id, "draft", "", "");
//     BOOST_CHECK(checkObjectNotes(serv, id, "restored"));
//     serv->SetState(config()->wikimapsUser(), id, "approved", "", "");
//     BOOST_CHECK(checkObjectNotes(serv, id, "approved"));

//     serv->SetState(config()->wikimapsUser(), id, "deleted", "", "");
//     BOOST_CHECK(checkObjectNotes(serv, id, "deleted"));
// }
