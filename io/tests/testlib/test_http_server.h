#pragma once

#include <functional>
#include <map>
#include <string>
#include <vector>

namespace quasar::TestUtils {

    class TestHttpServer {
    public:
        struct RestParams: public std::vector<std::pair<std::string, std::string>> {
            using std::vector<std::pair<std::string, std::string>>::vector;
            bool hasValue(const std::string& key) const;
            std::string getValue(const std::string& key) const;
        };

        struct Headers {
            bool hasHeader(const std::string& key) const;
            std::string getHeader(const std::string& key) const;
            std::string tryGetHeader(const std::string& key) const;
            std::map<std::string, std::string> headers;
            std::string resource;
            RestParams queryParams;
            std::string query;
            std::string verb;
        };
        class HttpServer;
        class HttpCallback;
        class HttpRequest;
        class HttpConnection {
        public:
            void doReplay(int code, const std::string& contentType, const std::string& contentBody)
            {
                doReplay(code, contentType, contentBody, {});
            }
            virtual void doReplay(int code, const std::string& contentType, const std::string& contentBody, std::vector<std::pair<std::string, std::string>> extraHeaders) = 0;
            virtual void doError(const std::string& text) = 0;
            virtual void close() = 0;
        };

        ~TestHttpServer();

        using HandlePayload = std::function<void(const Headers&, const std::string&, HttpConnection&)>;

        int start(int port, bool keepAliveEnabled = true);
        void stop();
        int port() const;

        HandlePayload onHandlePayload;

        int port_{0};
        std::shared_ptr<HttpCallback> httpCallback_;
        std::shared_ptr<HttpServer> httpServer_;
    };

} // namespace quasar::TestUtils
