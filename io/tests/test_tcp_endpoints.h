#pragma once

#include <yandex_io/libs/ipc/datacratic/tcp_connector.h>
#include <yandex_io/libs/ipc/datacratic/tcp_endpoint.h>

#include <functional>
#include <string>

namespace quasar::TestUtils {

    struct MockTCPEndpoint: public ipc::detail::datacratic::TCPEndpoint {
        std::string answer;

        explicit MockTCPEndpoint(const std::string& name);
        ~MockTCPEndpoint();

        void setRequestHandler(RequestHandler requestHandler);
    };

    struct TestTCPConnector: public ipc::detail::datacratic::TCPConnector {
        std::function<void(const std::string& message)> onMessage;

    private:
        void handleMessageReceived(const std::string& response) override;
    };

} // namespace quasar::TestUtils
