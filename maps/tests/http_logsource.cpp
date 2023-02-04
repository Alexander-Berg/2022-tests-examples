#include <thread>

#include <library/cpp/http/client/client.h>
#include <library/cpp/http/coro/server.h>
#include <library/cpp/http/server/response.h>
#include <library/cpp/testing/unittest/registar.h>
#include <library/cpp/testing/unittest/tests_data.h>

#include <maps/libs/common/include/exception.h>
#include <maps/infra/roquefort/lib/logsource.h>

using namespace maps::roquefort;

class HttpLogSourceFixture : public NUnitTest::TBaseFixture {
public:
    using TRequestCb = NCoroHttp::THttpServer::TCallbacks::TRequestCb;
    using TRequestContext = NCoroHttp::THttpServer::TRequestContext;

    static constexpr std::string_view POST_PATH = "/post-logs/by/unit-test";

    void SetUp(NUnitTest::TTestContext&) override {
        logSource_ = std::make_shared<HttpLogSource>(/*batchQueueSize=*/16);
    }

    void TearDown(NUnitTest::TTestContext&) override {
        UNIT_ASSERT(!server_.IsRunning());
    }

    static TRequestCb makeLogsCallback(HttpLogSource* logsource)
    {
        return [logsource](TRequestContext& request)
        {
            bool accepted = false;
            try {
                accepted = logsource->acceptLogs(*request.Input);
            } catch (const std::exception& e) {
                ERROR() << "Unhandled exception in HttpLogSource::accept(): " << e.what();
            }
            auto code = accepted ? HTTP_OK : HTTP_SERVICE_UNAVAILABLE;
            *request.Output << THttpResponse(code);
        };
    }

    static TRequestCb chainCallbacks(TRequestCb logsCb, TRequestCb nextCb)
    {
        return [logsCb = std::move(logsCb), nextCb = std::move(nextCb)](
            NCoroHttp::THttpServer::TRequestContext& request)
        {
            std::istringstream iss{request.Input->FirstLine()};
            iss.setf(std::ios::skipws);
            std::string method, version, path;
            iss >> method >> path >> version;
            if (method == "POST" && path == POST_PATH)
                logsCb(request);
            else
                nextCb(request);
        };
    }

    // Returns assigned port
    ui16 start(TRequestCb cb) {
        ui16 port = portManager_.GetPort();
        serverWorker_ = std::thread([this, port, nextCb = std::move(cb)]() mutable {
            try {
                auto logsCb = makeLogsCallback(logSource_.get());
                auto requestCb = chainCallbacks(std::move(logsCb), std::move(nextCb));
                Cerr << "server worker started" << Endl;
                server_.RunCycle(
                    NCoroHttp::THttpServer::TConfig().SetPort(port),
                    NCoroHttp::THttpServer::TCallbacks().SetRequestCb(requestCb));
            } catch (const std::exception& e) {
                Cerr << "server worker: " << e.what() << Endl;
            } catch (const yexception& e) {
                Cerr << "server worker: " << e.what() << Endl;
            }
        });
        std::this_thread::sleep_for(std::chrono::milliseconds{100});
        return port;
    }

    void join() {
        Cerr << "Shutting down" << Endl;
        server_.ShutDown();
        serverWorker_.join();
    }

    static NHttpFetcher::TResultRef post(
        const ui16 port,
        const std::string& postData)
    {
        std::ostringstream url;
        url << "http://[::1]:" << port << POST_PATH;
        auto response = NHttp::Fetch({
            TString{url.str()},
            NHttp::TFetchOptions().SetPostData(TString{postData})
        });
        UNIT_ASSERT_EQUAL(response->Code, HTTP_OK);
        return response;
    }

    static std::vector<std::string> collectLines(std::optional<LogDose> dose)
    {
        if (dose == std::nullopt)
            return {};

        std::vector<std::string> lines;
        std::optional<std::string> nextLine;
        while ((nextLine = dose->line()) != std::nullopt) {
            lines.push_back(std::move(nextLine.value()));
        }
        return lines;
    }

    HttpLogSource* logSource() {
        return logSource_.get();
    }

private:
    std::shared_ptr<HttpLogSource> logSource_;
    TPortManager portManager_;
    NCoroHttp::THttpServer server_;
    std::thread serverWorker_;
};

Y_UNIT_TEST_SUITE_F(HttpLogSourceTestSuite, HttpLogSourceFixture)
{
    Y_UNIT_TEST(TestPassDown)
    {
        bool passedDown = false;
        TRequestCb requestCb = [&passedDown](TRequestContext& context)
        {
            passedDown = true;
            *context.Output << THttpResponse();
        };
        ui16 port = start(std::move(requestCb));

        std::ostringstream url;
        url << "http://[::1]:" << port << "/";
        auto response = NHttp::Fetch({TString{url.str()}});
        UNIT_ASSERT_EQUAL(response->Code, HTTP_OK);
        join();

        UNIT_ASSERT(passedDown);
    }

    Y_UNIT_TEST(TestOneLineConsumed)
    {
        ui16 port = start([](TRequestContext& request){
            *request.Output << THttpResponse(HTTP_BAD_REQUEST);
        });

        post(port, "line_one\n");

        UNIT_ASSERT_VALUES_EQUAL(
            collectLines(logSource()->dose()),
            (std::vector<std::string>{"line_one"}));

        join();
    }

    Y_UNIT_TEST(TestTwoPostsEachOneLine)
    {
        ui16 port = start([](TRequestContext& request){
            *request.Output << THttpResponse(HTTP_BAD_REQUEST);
        });

        post(port, "line_one\n");

        UNIT_ASSERT_VALUES_EQUAL(
            collectLines(logSource()->dose()),
            (std::vector<std::string>{"line_one"}));

        post(port, "line_two\n");

        UNIT_ASSERT_VALUES_EQUAL(
            collectLines(logSource()->dose()),
            (std::vector<std::string>{"line_two"}));

        join();
    }

    Y_UNIT_TEST(TestBatchTwoLines)
    {
        ui16 port = start([](TRequestContext& request){
            *request.Output << THttpResponse(HTTP_BAD_REQUEST);
        });

        post(port, "line_one\nline_two\n");

        UNIT_ASSERT_VALUES_EQUAL(
            collectLines(logSource()->dose()),
            (std::vector<std::string>{"line_one", "line_two"}));

        join();
    }

    Y_UNIT_TEST(TestTwoBatchesEachTwoLines)
    {
        ui16 port = start([](TRequestContext& request){
            *request.Output << THttpResponse(HTTP_BAD_REQUEST);
        });

        post(port, "line_one\nline_two\n");

        UNIT_ASSERT_VALUES_EQUAL(
            collectLines(logSource()->dose()),
            (std::vector<std::string>{"line_one", "line_two"}));

        post(port, "line_three\nline_four\n");

        UNIT_ASSERT_VALUES_EQUAL(
            collectLines(logSource()->dose()),
            (std::vector<std::string>{"line_three", "line_four"}));

        join();
    }

    Y_UNIT_TEST(TestBatchNoEolAtEnd)
    {
        ui16 port = start([](TRequestContext& request){
            *request.Output << THttpResponse(HTTP_BAD_REQUEST);
        });

        post(port, "line_one\nline_two");

        UNIT_ASSERT_VALUES_EQUAL(
            collectLines(logSource()->dose()),
            (std::vector<std::string>{"line_one", "line_two"}));

        join();
    }
}
