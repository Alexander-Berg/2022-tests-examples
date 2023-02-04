#include "logger.h"
#include "context.h"

#include <yandex/maps/cgiutils3/cgiutils.h>
#include <yandex/maps/cgiutils3/requestutils.h>
#include <yandex/maps/cgiutils3/exception.h>
#include <yandex/maps/cgiutils3/config_holder.h>

#include <fastcgi2/request.h>
#include <fastcgi2/stream.h>
#include <fastcgi2/component_factory.h>
#include <fastcgi2/data_buffer.h>

#include <memory>

#define BOOST_TEST_DYN_LINK 
#define BOOST_TEST_MAIN
#include <boost/test/unit_test.hpp>
#include <boost/test/unit_test_log.hpp>
#include <boost/test/test_tools.hpp>

class FastCGIParamNotFound: public maps::cgiutils::FastCGIImpl {
public:
    FastCGIParamNotFound(): maps::cgiutils::FastCGIImpl(NULL, "") {}
    void doHandleRequest(fastcgi::Request*) {throw maps::cgiutils::ParamNotFound();}
};

class FastCGIParamValueError: public maps::cgiutils::FastCGIImpl {
public:
    FastCGIParamValueError(): maps::cgiutils::FastCGIImpl(NULL, "") {}
    void doHandleRequest(fastcgi::Request*) {throw maps::cgiutils::ParamValueError();}
};

class FastCGIBadRequest: public maps::cgiutils::FastCGIImpl {
public:
    FastCGIBadRequest(): maps::cgiutils::FastCGIImpl(NULL, "") {}
    void doHandleRequest(fastcgi::Request*) {throw maps::cgiutils::BadRequest();}
};

class FastCGIServerError: public maps::cgiutils::FastCGIImpl {
public:
    FastCGIServerError(): maps::cgiutils::FastCGIImpl(NULL, "") {}
    void doHandleRequest(fastcgi::Request*) {throw maps::cgiutils::ServerError("");}
};

class FastCGIexception: public maps::cgiutils::FastCGIImpl {
public:
    FastCGIexception(): maps::cgiutils::FastCGIImpl(NULL, "") {}
    void doHandleRequest(fastcgi::Request*) {throw std::exception();}
};

class FastCGINotFound: public maps::cgiutils::FastCGIImpl {
public:
    FastCGINotFound(): maps::cgiutils::FastCGIImpl(NULL, "") {}
    void doHandleRequest(fastcgi::Request*) {throw maps::cgiutils::NotFound("", "");}
};

BOOST_AUTO_TEST_CASE(test_ParamNotFound)
{
    std::unique_ptr<LOGGER> log(new LOGGER());
    maps::cgiutils::g_initLogger(log.get());
        
    DummyLogger logger;
    FastCGIParamNotFound tmp; 
    fastcgi::Request req(&logger, NULL);
    TestHandlerContext context;

    tmp.handleRequest(&req, &context);

    BOOST_CHECK_EQUAL(req.status(), static_cast<unsigned int>(
                                maps::http::HTTPCodes::BAD_REQUEST));
}

BOOST_AUTO_TEST_CASE(test_ParamValueError)
{
    std::unique_ptr<LOGGER> log(new LOGGER());
    maps::cgiutils::g_initLogger(log.get());
        
    DummyLogger logger;
    FastCGIParamValueError tmp; 
    fastcgi::Request req(&logger, NULL);
    TestHandlerContext context;

    tmp.handleRequest(&req, &context);

    BOOST_CHECK_EQUAL(req.status(), static_cast<unsigned int>(
                                maps::http::HTTPCodes::BAD_REQUEST));
}

BOOST_AUTO_TEST_CASE(test_BadRequest)
{
    std::unique_ptr<LOGGER> log(new LOGGER());
    maps::cgiutils::g_initLogger(log.get());
        
    DummyLogger logger;
    FastCGIBadRequest tmp; 
    fastcgi::Request req(&logger, NULL);
    TestHandlerContext context;

    tmp.handleRequest(&req, &context);

    BOOST_CHECK_EQUAL(req.status(), static_cast<unsigned int>(
                                maps::http::HTTPCodes::BAD_REQUEST));
}

BOOST_AUTO_TEST_CASE(test_ServerError)
{
    std::unique_ptr<LOGGER> log(new LOGGER());
    maps::cgiutils::g_initLogger(log.get());
        
    DummyLogger logger;
    FastCGIServerError tmp; 
    fastcgi::Request req(&logger, NULL);
    TestHandlerContext context;
    
    tmp.handleRequest(&req, &context);

    BOOST_CHECK_EQUAL(req.status(), 500);
}

BOOST_AUTO_TEST_CASE(test_exception)
{
    std::unique_ptr<LOGGER> log(new LOGGER());
    maps::cgiutils::g_initLogger(log.get());
        
    DummyLogger logger;
    FastCGIexception tmp; 
    fastcgi::Request req(&logger, NULL);
    TestHandlerContext context;
    
    tmp.handleRequest(&req, &context);

    BOOST_CHECK_EQUAL(req.status(), 500);
}

BOOST_AUTO_TEST_CASE(test_NotFound)
{
    std::unique_ptr<LOGGER> log(new LOGGER());
    maps::cgiutils::g_initLogger(log.get());
        
    DummyLogger logger;
    FastCGINotFound tmp; 
    fastcgi::Request req(&logger, NULL);
    TestHandlerContext context;
    
    tmp.handleRequest(&req, &context);

    BOOST_CHECK_EQUAL(req.status(), 404);
}
