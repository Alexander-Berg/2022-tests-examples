#include "test_http_server.h"

#include <yandex_io/libs/logging/logging.h>

#include <boost/algorithm/string.hpp>

#include <library/cpp/cgiparam/cgiparam.h>
#include <library/cpp/http/misc/parsed_request.h>
#include <library/cpp/http/server/http.h>
#include <library/cpp/http/server/response.h>

using namespace quasar::TestUtils;

class TestHttpServer::HttpRequest: public TRequestReplier {
public:
    HttpRequest(TestHttpServer* testHttpServer);
    bool DoReply(const TReplyParams& params) override;

private:
    TestHttpServer* testHttpServer_;
};

class HttpConnectionImpl: public TestHttpServer::HttpConnection {
public:
    HttpConnectionImpl(TestHttpServer::HttpRequest* httpRequest, const TRequestReplier::TReplyParams& params)
        : httpRequest_(httpRequest)
        , params_(params)
    {
    }

    void doReplay(int code, const std::string& contentType, const std::string& contentBody, std::vector<std::pair<std::string, std::string>> extraHeaders) override {
        if (replayed_) {
            throw std::runtime_error("Already replayed!");
        }
        THttpHeaders headers;
        for (const auto& p : extraHeaders) {
            headers.AddHeader(p.first.c_str(), p.second.c_str());
        }
        params_.Output << THttpResponse(static_cast<HttpCodes>(code))
                              .AddMultipleHeaders(headers)
                              .SetContentType(contentType)
                              .SetContent(TString(contentBody));
        params_.Output.Flush();
        YIO_LOG_DEBUG("HTTP replay: content-type=" << contentType << ", size=" << contentBody.size() << ", content{{{" << contentBody << "}}}");
        replayed_ = true;
    }

    void doError(const std::string& text) override {
        doReplay(500, "text/plain", text, {});
    }

    void close() override {
        params_.Output << "HTTP/1.1";
        httpRequest_->ResetConnection();
        closed_ = true;
        YIO_LOG_DEBUG("HTTP close");
    }

    bool isReplayed() const {
        return replayed_;
    }

    bool isClosed() const {
        return closed_;
    }

private:
    TestHttpServer::HttpRequest* httpRequest_;
    const TRequestReplier::TReplyParams& params_;
    bool closed_{false};
    bool replayed_{false};
};

TestHttpServer::HttpRequest::HttpRequest(TestHttpServer* testHttpServer)
    : testHttpServer_(testHttpServer)
{
}

bool TestHttpServer::HttpRequest::DoReply(const TReplyParams& params)
{
    if (!testHttpServer_->onHandlePayload) {
        YIO_LOG_DEBUG("HTTP -->> no payload handler <<--");
        params.Output << THttpResponse(HTTP_NOT_IMPLEMENTED)
                             .SetContentType("text/plain")
                             .SetContent("Not implemented");
        params.Output.Flush();
        return true;
    }

    Headers headers;
    for (const auto& header : params.Input.Headers()) {
        std::string KEY(header.Name());
        boost::to_upper(KEY);
        YIO_LOG_DEBUG("HTTP header: " << KEY << "=" << header.Value());
        headers.headers[KEY] = header.Value();
    }
    if (headers.headers.empty()) {
        YIO_LOG_DEBUG("HTTP: no headers");
    }
    TParsedHttpFull request(params.Input.FirstLine());
    headers.resource = std::string(request.Path);
    YIO_LOG_DEBUG("HTTP resource: " << headers.resource);

    TCgiParameters cgiParam(request.Cgi);
    for (const auto& p : cgiParam) {
        headers.queryParams.push_back(std::make_pair(std::string{p.first}, std::string{p.second}));
    }
    headers.query = request.Cgi;
    YIO_LOG_DEBUG("HTTP query: " << headers.query);

    headers.verb = std::string(request.Method);
    YIO_LOG_DEBUG("HTTP method: " << headers.verb);

    auto payload = params.Input.ReadAll();
    HttpConnectionImpl connection(this, params);
    testHttpServer_->onHandlePayload(headers, payload, connection);
    if (!connection.isReplayed() && !connection.isClosed()) {
        YIO_LOG_DEBUG("HTTP -->> no replay <<--");
        params.Output << THttpResponse(HTTP_NO_CONTENT);
        params.Output.Flush();
    }
    return true;
}

class TestHttpServer::HttpCallback: public THttpServer::ICallBack {
public:
    HttpCallback(TestHttpServer* testHttpServer)
        : testHttpServer_(testHttpServer)
    {
    }

    TClientRequest* CreateClient() override {
        return new HttpRequest(testHttpServer_);
    }

private:
    TestHttpServer* testHttpServer_;
};

class TestHttpServer::HttpServer: public THttpServer {
public:
    using THttpServer::THttpServer;
};

bool TestHttpServer::RestParams::hasValue(const std::string& key) const {
    for (const auto& p : *this) {
        if (p.first == key) {
            return true;
        }
    }
    return false;
}

std::string TestHttpServer::RestParams::getValue(const std::string& key) const {
    for (const auto& p : *this) {
        if (p.first == key) {
            return p.second;
        }
    }
    throw std::runtime_error("Key \"" + key + "\" not found");
}

std::string TestHttpServer::Headers::getHeader(const std::string& key) const {
    auto KEY = key;
    boost::to_upper(KEY);
    auto it = headers.find(KEY);
    if (it == headers.end()) {
        throw std::runtime_error("no header" + key);
    }
    return it->second;
}

bool TestHttpServer::Headers::hasHeader(const std::string& key) const {
    auto KEY = key;
    boost::to_upper(KEY);
    auto it = headers.find(KEY);
    return it != headers.end();
}

std::string TestHttpServer::Headers::tryGetHeader(const std::string& key) const {
    auto KEY = key;
    boost::to_upper(KEY);
    auto it = headers.find(KEY);
    if (it == headers.end()) {
        return "";
    }
    return it->second;
}

int TestHttpServer::start(int port, bool keepAliveEnabled)
{
    stop();

    port_ = port;
    THttpServer::TOptions httpOptions;
    httpOptions.Port = port;
    httpOptions.nThreads = 1;
    httpOptions.KeepAliveEnabled = keepAliveEnabled;
    httpCallback_ = std::make_shared<HttpCallback>(this);
    httpServer_ = std::make_shared<HttpServer>(httpCallback_.get(), httpOptions);
    httpServer_->Start();
    YIO_LOG_DEBUG("HTTP service port " << port);
    return port;
}

void TestHttpServer::stop()
{
    if (httpServer_) {
        httpServer_->Shutdown();
        httpServer_->Wait();
        httpServer_ = nullptr;
        httpCallback_ = nullptr;
    }
    port_ = 0;
}

TestHttpServer::~TestHttpServer()
{
    stop();
}

int TestHttpServer::port() const {
    return port_;
}
