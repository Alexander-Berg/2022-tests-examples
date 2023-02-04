#include "logger.h"
#include "context.h"

#include <yandex/maps/cgiutils3/cgiutils.h>
#include <yandex/maps/cgiutils3/locale.h>
#include <yandex/maps/i18n/i18n.h>

#include <fastcgi2/request.h>

#include <memory>
#include <string.h>
#include <iostream>

#define BOOST_TEST_DYN_LINK 
#define BOOST_TEST_MAIN
#include <boost/test/unit_test.hpp>
#include <boost/test/unit_test_log.hpp>
#include <boost/test/test_tools.hpp>

using namespace maps::i18n;

class LocaleTester: public maps::cgiutils::FastCGIImpl {
public:
    LocaleTester(const std::string& expectedLocale)
        : maps::cgiutils::FastCGIImpl(NULL, "")
        , expectedLocale_(expectedLocale)
    {}

    void doHandleRequest(fastcgi::Request*)
    {
        BOOST_CHECK_EQUAL(
            expectedLocale_,
            maps::cgiutils::currentLocale().name());
    }

private:
    std::string expectedLocale_;
};


BOOST_AUTO_TEST_CASE(test_DefaultLocale)
{
    std::unique_ptr<LOGGER> log(new LOGGER());
    maps::cgiutils::g_initLogger(log.get());
        
    DummyLogger logger;
    LocaleTester tmp(bestLocale(defaultLocale()).name()); 
    fastcgi::Request req(&logger, NULL);
    TestHandlerContext context;

    tmp.handleRequest(&req, &context);
}
