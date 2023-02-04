#define BOOST_TEST_MAIN
#include <boost/test/auto_unit_test.hpp>

#include <parsers/parser.h>

struct TestParser : Parser
{
    static void checkCrcTest(const yacare::QueryParams& input, bool isGet)
    {
        checkCrc(input, isGet);
    }
};

BOOST_AUTO_TEST_CASE(check_parse_crc)
{
    auto correct = yacare::QueryParams{
        { "latitude", { "55.696087" }},
        {"longitude", {"37.531010"}},
        {"avg_speed", {"1"}},
        {"direction", {"346"}},
        {"uuid", {"3b96bb1db7f30fb041efba8c5a11282d"}},
        {"time", {"19022010:132240"}},
        {"packetid", {"2231575487"}}
    };
    auto incorrect = correct;
    incorrect["packetid"] = { "12345" };

    BOOST_CHECK_NO_THROW(TestParser::checkCrcTest(correct, true));
    correct["type"] = { "auto" };
    BOOST_CHECK_NO_THROW(TestParser::checkCrcTest(correct, true));
    correct["clid"] = { "auto" };
    BOOST_CHECK_NO_THROW(TestParser::checkCrcTest(correct, true));

    BOOST_CHECK_THROW(TestParser::checkCrcTest(incorrect, true), yacare::errors::BadRequest);
}
