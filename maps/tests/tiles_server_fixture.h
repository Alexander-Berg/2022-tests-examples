#pragma once

#include <maps/libs/http/include/http.h>
#include <maps/libs/concurrent/include/background_thread.h>
#include <maps/libs/process/include/process.h>
#include <library/cpp/testing/unittest/env.h>

#include <chrono>

namespace maps::wiki::autocart::pipeline::tests {

namespace {

const std::string TEST_TILES_SERVER_HOST = "127.0.0.1";
const std::string SERVER_BINARY_PATH
    = "maps/wikimap/mapspro/services/autocart/pipeline/libs/detection/tests/test_tiles_server/test_tiles_server";

} // namespace

class TilesServerFixture : public NUnitTest::TBaseFixture {
public:
    TilesServerFixture()
        : host_(TEST_TILES_SERVER_HOST),
          port_(std::to_string(GetRandomPort())),
          serverProcess_(runServer(host_, port_))
    {
        waitStartServer();
    }

    ~TilesServerFixture() {
        shutdownServer();
    }

    std::string url() const {
        return "http://" + host_ + ":" + port_;
    }

    std::string tileSourceUrl() const {
        return "http://" + host_ + ":" + port_ + "/tiles?l=sat";
    }

private:
    process::Process runServer(
        const std::string& host, const std::string& port)
    {
        return process::run(
            process::Command(
                {BinaryPath(SERVER_BINARY_PATH), host, port}));
    }

    void waitStartServer() {
        // wait 1 minute
        constexpr int MAX_RETRY_COUNT = 60;
        constexpr std::chrono::seconds RETRY_TIMEOUT(1);

        http::Client client;
        http::Request request(client, http::GET, url());
        int retryCount = 0;
        while (retryCount < MAX_RETRY_COUNT) {
            try {
                http::Response response = request.perform();
                if (response.status() == 200) {
                    return;
                }
            } catch(...) {
                continue;
            }
            retryCount++;
            std::this_thread::sleep_for(RETRY_TIMEOUT);
        }
        throw maps::RuntimeError("Tiles server is not running");
    }

    void shutdownServer() {
        if (serverProcess_.finished()) {
            return;
        }

        serverProcess_.sendSignal(SIGTERM);

        int waitSIGTERM = true;
        concurrent::BackgroundThread killProcessIfIgnoreSIGTERM(
            [&]() {
                if (!waitSIGTERM) {
                    serverProcess_.sendSignal(SIGKILL);
                }
                waitSIGTERM = false;
            },
            std::chrono::seconds(30)
        );

        killProcessIfIgnoreSIGTERM.start();
        serverProcess_.syncWait();
    }

    std::string host_;
    std::string port_;
    process::Process serverProcess_;
};

} // namespace maps::wiki::autocart::pipeline::tests
