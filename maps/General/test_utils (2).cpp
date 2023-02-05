#include <maps/infra/yacare/include/test_utils.h>
#include <maps/infra/yacare/include/frontend.h>
#include <maps/infra/yacare/include/request.h>
#include <maps/infra/yacare/include/response.h>
#include <maps/infra/yacare/include/spooler.h>
#include <maps/infra/yacare/utils.h>

namespace yacare {

namespace {

class MockFrontend final: public FrontendBase {
public:
    MockFrontend(const maps::http::MockRequest& requestMock, maps::http::MockResponse* responseMock)
        : requestMock_(requestMock)
        , responseMock_(*responseMock)
    {
    }

    Connection* accept() override {
        throw maps::LogicError() << "MockFrontend::accept() should not be invoked";
    }

    void parse(Connection* /* connection */, RequestBuilder& builder) override {
        std::istringstream bodyStream(requestMock_.body);

        for (const auto& [key, value]: requestMock_.headers) {
            builder.putenv(convertHttpHeaderToFastcgi(key), value);
        }
        builder.putenv(convertHttpHeaderToFastcgi("Host"), requestMock_.url.host());
        builder.putenv("REQUEST_METHOD", std::string{requestMock_.method});
        builder.putenv("PATH_INFO", requestMock_.url.path());
        builder.putenv("QUERY_STRING", requestMock_.url.params());
        builder.putenv("REMOTE_ADDR", "127.0.0.1");
        builder.putenv("CONTENT_LENGTH", std::to_string(requestMock_.body.size()));
        builder.readBody(
            [&](char* dest, size_t size) {
                return static_cast<bool>(bodyStream.read(dest, size));
            }
        );
    }

    void send(Connection* /* connection */, const Response& response) override {
        responseMock_.status = response.status().code();
        //FIXME: Should be fixed after https://st.yandex-team.ru/GEOINFRA-933
        for (const auto& [name, values]: response.headers()) {
            responseMock_.headers.emplace(name, values.front());
        }
        if (response.body()) {
            responseMock_.body.assign(response.body(), response.size());
        }
    }

    void stop() override {
        throw maps::LogicError() << "MockFrontend::stop() should not be invoked";
    }

private:
    const maps::http::MockRequest& requestMock_;
    maps::http::MockResponse& responseMock_;
};

} //anonymous namespace

maps::http::MockResponse performTestRequest(const maps::http::MockRequest& request) {
    maps::http::MockResponse response;
    MockFrontend frontend(request, &response);
    yacare::Task task(&frontend, nullptr);
    task.process();
    ASSERT(task.isProcessed());
    return response;
}

} //namespace yacare
