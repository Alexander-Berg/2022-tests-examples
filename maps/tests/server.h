#pragma once

#include <Poco/Mutex.h>
#include <Poco/Net/HTTPRequest.h>
#include <Poco/Net/HTTPRequestHandler.h>
#include <Poco/Net/HTTPRequestHandlerFactory.h>
#include <Poco/Net/HTTPServer.h>
#include <Poco/Net/HTTPServerRequest.h>
#include <Poco/Net/HTTPServerResponse.h>

#include <iostream>
#include <mutex>
#include <string>

#include <cstdint>

//! User defined HTTP request handler type
typedef std::function<void(Poco::Net::HTTPServerRequest&,
                           Poco::Net::HTTPServerResponse&)> RequestHandlerF;

class TestRequestHandler : public Poco::Net::HTTPRequestHandler {
public:
    TestRequestHandler(const RequestHandlerF& handler) : handler_(handler) {}

    void handleRequest(Poco::Net::HTTPServerRequest& req,
                       Poco::Net::HTTPServerResponse& resp)
    {
        handler_(req, resp);
    }

private:
    const RequestHandlerF& handler_;
};

class TestRequestHandlerFactory
    : public Poco::Net::HTTPRequestHandlerFactory {
public:
    TestRequestHandlerFactory(const RequestHandlerF& handler)
        : handler_(handler)
    {
    }

    virtual Poco::Net::HTTPRequestHandler*
    createRequestHandler(const Poco::Net::HTTPServerRequest&)
    {
        return new TestRequestHandler(handler_);
    }

private:
    const RequestHandlerF& handler_;
};

class TestServer {
public:
    TestServer(RequestHandlerF&& handler)
        : handler_(std::move(handler))
        , pocoServer_(new TestRequestHandlerFactory(handler_),
                      0, // choose free port
                      new Poco::Net::HTTPServerParams)
    {
        pocoServer_.start();
    }

    std::uint16_t port() { return pocoServer_.port(); }

    ~TestServer() { pocoServer_.stop(); }

private:
    const RequestHandlerF handler_;
    Poco::Net::HTTPServer pocoServer_;
};

class RequestsCollection : private std::vector<std::string> {
public:
    using std::vector<std::string>::vector;

    std::string& operator[](std::size_t idx)
    {
        std::unique_lock<std::mutex> lock(mutex_);
        return std::vector<std::string>::at(idx);
    }

    std::size_t size()
    {
        std::unique_lock<std::mutex> lock(mutex_);
        return std::vector<std::string>::size();
    }

    void push(std::string elem)
    {
        std::unique_lock<std::mutex> lock(mutex_);
        return std::vector<std::string>::push_back(elem);
    }

private:
    std::mutex mutex_;
};

RequestHandlerF makeSuccessHandler(RequestsCollection& recievedReqs)
{
    return [&](Poco::Net::HTTPServerRequest& req,
               Poco::Net::HTTPServerResponse& resp) {
        recievedReqs.push(req.getMethod() + " " + req.getURI());
        resp.setStatus(Poco::Net::HTTPResponse::HTTP_OK);
        resp.send().flush();
    };
};

RequestHandlerF makeNotFoundHandler(RequestsCollection& recievedReqs,
                                    const char* invAltayId)

{
    return [&recievedReqs, invAltayId](Poco::Net::HTTPServerRequest& req,
                                       Poco::Net::HTTPServerResponse& resp) {
        if (req.getURI().find(invAltayId) == std::string::npos) {
            recievedReqs.push(req.getMethod() + " " + req.getURI());
            resp.setStatus(Poco::Net::HTTPResponse::HTTP_OK);
        }
        else {
            resp.setStatus(Poco::Net::HTTPResponse::HTTP_NOT_FOUND);
        }
        resp.send().flush();
    };
};

RequestHandlerF makeServerErrorHandler()
{
    return [](Poco::Net::HTTPServerRequest&,
              Poco::Net::HTTPServerResponse& resp) {
        resp.setStatus(Poco::Net::HTTPResponse::HTTP_INTERNAL_SERVER_ERROR);
        resp.send().flush();
    };
};

RequestHandlerF makeTimeoutHandler(const std::chrono::seconds&& timeout)
{
    return [=](Poco::Net::HTTPServerRequest&,
               Poco::Net::HTTPServerResponse& resp) {
        resp.setStatus(Poco::Net::HTTPResponse::HTTP_OK);
        std::this_thread::sleep_for(timeout);
        resp.send().flush();
    };
};
