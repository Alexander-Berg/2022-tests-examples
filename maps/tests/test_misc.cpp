#include "../config.h"
#include "../format.h"
#include "../strcast.hpp"
#include "../tuple.h"

#define BOOST_TEST_MAIN
#define BOOST_TEST_DYN_LINK
#include <boost/test/unit_test.hpp>

namespace maps{
    namespace wiki{
        template<> std::string attribute<int>(const int& i, const std::string& name)
        {
            std::stringstream os;
            if(name == "HEX")
                os << std::hex << i;
            if(name == "DEC")
                os << i;
            if(name == "OCT")
                os << std::oct << i;
            return os.str();
        }
    }
}

/*struct InitConfig
{
    InitConfig()
        : cfgHolder_("../../configs/servant/servant-coretest.cfg")
    { }

    ConfigHolder cfgHolder_;
};
BOOST_GLOBAL_FIXTURE(InitConfig);

Config* config()
{
    return TConfig(maps::wiki::cfg());
}*/

BOOST_AUTO_TEST_CASE(format_test)
{
    const std::string golden = "127 decimal is 7f hex and 177 oct";
    maps::wiki::Format fmtStr("#DEC# decimal is #HEX# hex and #OCT# oct", '#');
    std::stringstream strTest;
    fmtStr(127, strTest);
    BOOST_CHECK_EQUAL(strTest.str(), golden);
    maps::wiki::Format fmtXML(
        "<?xml version=\"1.0\" encoding=\"utf-8\"?><Format><A>DEC</A><T> decimal is </T><A>HEX</A><T> hex and </T><A>OCT</A><T> oct</T></Format>");
    std::stringstream xmlTest;
    fmtXML(127, xmlTest);
    BOOST_CHECK_EQUAL(xmlTest.str(), golden);
}

BOOST_AUTO_TEST_CASE(test_ExtendedXmlDocVal)
{
    ExtendedXmlDoc doc("tests/data/cfg.base");
    BOOST_CHECK_EQUAL("item2", doc.get<std::string>("/config/item1/item2"));
    BOOST_CHECK_THROW(doc.get<std::string>("/config/absent"), maps::xml3::NodeNotFound);

    BOOST_CHECK_EQUAL("item2", doc.get<std::string>("/config/item1/item2", "Default"));
    BOOST_CHECK_EQUAL("Default", doc.get<std::string>("/config/absent", "Default"));

    BOOST_CHECK_EQUAL("attr1", doc.getAttr<std::string>("/config/item1", "attr1"));

    BOOST_CHECK_THROW(doc.getAttr<std::string>("/config/item1", "absent"), maps::xml3::AttributeNotFound);

    BOOST_CHECK_EQUAL("attr1", doc.getAttr<std::string>("/config/item1", "attr1", "Default"));
    BOOST_CHECK_EQUAL("Default", doc.getAttr<std::string>("/config/item1", "absent", "Default"));

    ExtendedXmlDoc extDoc("tests/data/cfg.ext");

    BOOST_CHECK_EQUAL("item2", extDoc.get<std::string>("/config/item1/item2"));
    BOOST_CHECK_EQUAL("itemExt", extDoc.get<std::string>("/config/item1/itemExt"));

    BOOST_CHECK_EQUAL("attr2", extDoc.getAttr<std::string>("/config/item1", "attr1"));

}

BOOST_AUTO_TEST_CASE(test_stringcast)
{
    std::string str = "加入营销计划 ТЕСТ!"; 
    std::wstring wstr = boost::lexical_cast<std::wstring>(str);
    std::string str_out = boost::lexical_cast<std::string>(wstr);
    
    BOOST_CHECK_EQUAL(str, str_out);
    BOOST_CHECK_EQUAL(wstr.size(), 12);
    
    std::cout << str << std::endl;
}

BOOST_AUTO_TEST_CASE(test_parseCSV)
{
    std::string str = " test1 , \"test \\\"2\\\"\", test3";
    maps::wiki::StringVec parsed = maps::wiki::parseCSV(str, ',', '\\');
    BOOST_CHECK_EQUAL(parsed.size(), 3);
    BOOST_CHECK_EQUAL(parsed[0],"test1");
    BOOST_CHECK_EQUAL(parsed[1],"test \"2\"");
    BOOST_CHECK_EQUAL(parsed[2],"test3");
}

BOOST_AUTO_TEST_CASE(test_caseInsensitveLess)
{
   const std::string& s1 = "СОБАКА";
   const std::string& s2 = "собака";
   const std::string& s3 = "кошка";
   BOOST_CHECK(!maps::wiki::caseInsensetiveLess()(s1, s2));
   BOOST_CHECK(!maps::wiki::caseInsensetiveLess()(s2, s1));
   BOOST_CHECK(maps::wiki::caseInsensetiveLess()(s3, s2));
   BOOST_CHECK(maps::wiki::caseInsensetiveLess()(s3, s1));
}

BOOST_AUTO_TEST_CASE(test_natSort)
{
   std::multiset<std::string, maps::wiki::natural_sort<maps::wiki::caseInsensetiveLess> > values;
   BOOST_CHECK_NO_THROW(values.insert("221367968"));
   BOOST_CHECK_NO_THROW(values.insert("891246963659"));
}

BOOST_AUTO_TEST_CASE(test_tuple_tilde)
{
    maps::wiki::Tuple origTuple("", boost::none);
    origTuple.setValue("test~~test", "~test~");
    origTuple.setValue("~test~", "test~~test");
    maps::wiki::Tuple restoredTuple(origTuple.encoded(), boost::none);

    auto origIt = origTuple.begin();
    auto restoredIt = restoredTuple.begin();
    for (;
            origIt != origTuple.end() && restoredIt != restoredTuple.end();
            ++origIt, ++restoredIt) {
        BOOST_CHECK(*origIt == *restoredIt);
    }
    BOOST_CHECK(origIt == origTuple.end());
    BOOST_CHECK(restoredIt == restoredTuple.end());
}


