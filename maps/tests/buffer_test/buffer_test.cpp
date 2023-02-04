#define BOOST_AUTO_TEST_MAIN

#include <boost/test/auto_unit_test.hpp>
#include <boost/test/test_tools.hpp>

#include "maps/analyzer/libs/http/impl/receive_buffer.h"
#include <string>
#include <iostream>

namespace bio = boost::iostreams;
namespace http = maps::analyzer::http;

BOOST_AUTO_TEST_CASE(sequence_test)
{
    http::ReceiveBuffer buffer;
    std::iostream io(&buffer);

    std::string str;
    io << "hello" << std::flush;

    BOOST_REQUIRE_EQUAL(io.rdbuf()->in_avail(), 5);
    std::string result;
    io >> result;
    BOOST_REQUIRE_EQUAL(result, "hello");
}
