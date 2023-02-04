#include "../include/xmlwrappTest.h"
#include <maps/renderer/libs/base/include/string_convert.h>
#include <xmlwrapp/special_chars.h>

using namespace maps::renderer;
using namespace maps::renderer5;

void test::xmlwrapp::entities_test()
{
    {
        std::wstring ws = L"Привет! Abcdef!";
        std::string  s = base::ws2s(ws);
        std::string r = xml::decode_xml_entities(s);
        std::wstring ws_result = base::s2ws(r);

        BOOST_CHECK(ws == ws_result);
    }

    {
        std::wstring ws = L"Привет! &amp; Abcdef!";
        std::string  s = base::ws2s(ws);
        std::string r = xml::decode_xml_entities(s);
        std::wstring ws_result = base::s2ws(r);

        BOOST_CHECK(L"Привет! & Abcdef!" == ws_result);
    }
}

test_suite* test::xmlwrapp::init_suite()
{
    test_suite* suite = BOOST_TEST_SUITE("Xmlwrapp test suite");

    suite->add(
        BOOST_TEST_CASE(&entities_test));

    return suite;
}
