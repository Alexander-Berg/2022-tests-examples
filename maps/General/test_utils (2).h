#pragma once

#include <maps/libs/http/include/connection.h>
#include <maps/libs/http/include/header_map.h>
#include <maps/libs/http/include/request_methods.h>
#include <maps/libs/http/include/url.h>

#include <memory>
#include <string>

namespace maps::http {

/*
 * FIXME:
 *   This class is quite similar to Request.
 *   We should think on how to merge them.
 */
struct MockRequest
{
    MockRequest(Method method_, URL url_)
        : method(std::move(method_))
        , url(std::move(url_))
    {
    }

    Method method;
    URL url;
    std::string body;
    HeaderMap headers;

    const std::string& header(const std::string& key) const;
};

/*
 * FIXME:
 *   This class is quite similar to Response.
 *   We should think on how to merge them.
 */
struct MockResponse
{
    std::string body;
    int status = 200;
    HeaderMap headers;

    MockResponse() = default;
    MockResponse(std::string body_);

    static MockResponse fromFile(const std::string& filepath);
    static MockResponse fromArcadia(const std::string& filepath);
    static MockResponse withStatus(int status_);
};

/**
 * @return true if there is at least one mock set up
 */
bool hasMocks();

/**
 * @return true if a mock is executing in the current thread
 */
bool isInsideMock();

/**
 * @return The result of the mock matching the URL from the request.
 * @exception maps::LogicError There is no matching mock.
 */
Connection matchRequest(const MockRequest& request);

struct MockHandle final
{
    MockHandle(std::string url);
    MockHandle(MockHandle&&) = default;
    MockHandle& operator= (MockHandle&&) = default;
    MockHandle(const MockHandle&) = delete;
    MockHandle& operator= (const MockHandle&) = delete;
    ~MockHandle(); // removes mock

    std::string url;
};

using MockFunction = std::function<MockResponse(const MockRequest& request)>;

/**
 * Adds a mock function for using in unit tests.
 *
 * After creating any mock, all HTTP requests from tested code are performed
 * using mocks (LogicError is thrown if a mock is not found). In other word,
 * adding a mock enables test mode globally in the http library where
 * HTTP requests over network from tested code are disabled.
 *
 * Thread safe. HTTP requests can be performed in the other thread then
 * the mocks handling thread. All calls of any MockFunction, addMock and
 * ~MockHandle (remove mock) do not execute at the same time.
 *
 * A HTTP request inside a mock function is performed over the network,
 * mocks are never called recursive. The behavior can be used to implement
 * input data loading/preparing for tests like ya make tests canonization.
 *
 * See Request::performAsync implementation for details.
 *
 * @param url URL without parameters and a fragment, e.g. http://example.com/test
 *
 * Usage example:
 * \code{.cpp}
 * Y_UNIT_TEST(test) {
 *     auto mockHandle = http::addMock(
 *         "http://example.com/test",
 *         [](const http::MockRequest&) {
 *             return http::MockResponse("Hello, world!");
 *         });
 *
 *     // do stuff
 * }
 * \endcode
 */
[[nodiscard]] MockHandle addMock(const URL& url, MockFunction mock);

} //namespace maps::http
