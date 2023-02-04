#define BOOST_AUTO_TEST_MAIN

#include <boost/test/auto_unit_test.hpp>
#include <boost/test/test_tools.hpp>
#include "maps/analyzer/libs/http/impl/pipe.h"
#include <maps/analyzer/libs/http/include/logger.h>
#include <string>

namespace http = maps::analyzer::http;

class PipeReader : public http::PipeReader
{
public:
    void read(void* character) override
    {
        content_.push_back(*reinterpret_cast<char*>(character));
    }

private:
    std::string content_;
};

BOOST_AUTO_TEST_CASE(sequence_test)
{
    PipeReader reader;
    http::Logger logger;
    http::EventBase base(logger);
    http::Pipe pipe(base, reader);
    std::cout << base.handle() << std::endl;
}
