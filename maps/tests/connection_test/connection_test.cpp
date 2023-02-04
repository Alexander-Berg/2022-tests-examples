#define BOOST_AUTO_TEST_MAIN

#include <boost/test/auto_unit_test.hpp>
#include <boost/test/test_tools.hpp>
#include "maps/analyzer/libs/http/impl/connection.h"

namespace http = maps::analyzer::http;

const std::string URL = "urltoconnect.net";

void handler(const http::Response&) {}

class Handler
{
public:
    Handler(size_t& callTimes) : callTimes_(callTimes) {}

    void operator()(const http::Response&) { callTimes_++; }

    size_t callTimes() const { return callTimes_; }

private:
    size_t& callTimes_;
};

BOOST_AUTO_TEST_CASE(handler_func_test)
{
    std::unique_ptr<http::Request> request (new http::Request());
    http::Connection connection(URL, std::move(request), handler, http::Timeouts());
    connection.complete();
}

BOOST_AUTO_TEST_CASE(handler_class_test)
{
    size_t handleTimes = 0;
    Handler h(handleTimes);
    std::unique_ptr<http::Request> request (new http::Request());
    http::Connection connection(URL, std::move(request), h, http::Timeouts());
    connection.complete();

    BOOST_REQUIRE_EQUAL(handleTimes, 1);
}

BOOST_AUTO_TEST_CASE(request_test)
{
    std::unique_ptr<http::Request> post(new http::Request());
    post->addPost("name", "value");
    http::Connection connection("URL", std::move(post), handler, http::Timeouts());

    // TODO: check connection.handle() options
}

BOOST_AUTO_TEST_CASE(non_multipart_request_test)
{
    std::unique_ptr<http::Request> post(new http::Request());
    post->setBody("body");
    http::Connection connection("URL", std::move(post), handler, http::Timeouts());

    // TODO: check connection.handle() options
}

BOOST_AUTO_TEST_CASE(mixed_request_test)
{
    std::unique_ptr<http::Request> multipart(new http::Request());
    multipart->addPost("name", "value");
    BOOST_CHECK_THROW(multipart->setBody("body"),
                      maps::Exception);

    std::unique_ptr<http::Request> other(new http::Request());
    other->setBody("body");
    BOOST_CHECK_THROW(other->addPost("name", "value"),
                      maps::Exception);
}

BOOST_AUTO_TEST_CASE(easy_handle_test)
{
    std::unique_ptr<http::Request> request (new http::Request());
    http::Connection conn("", std::move(request), handler, http::Timeouts());
    BOOST_CHECK_EQUAL(
        &conn, http::EasyHandle::connectionFromCurl(conn.easy().handle()));
}
