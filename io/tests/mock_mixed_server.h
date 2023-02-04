#pragma once

#include <yandex_io/libs/ipc/mixed/i_mixed_server.h>

#include <yandex_io/protos/quasar_proto.pb.h>

#include <library/cpp/testing/gmock_in_unittest/gmock.h>

namespace quasar::mock {

    class MixedServer: public quasar::ipc::detail::mixed::IMixedServer {
    public:
        MOCK_METHOD(const std::string&, serviceName, (), (const, override));
        MOCK_METHOD(bool, isStarted, (), (const, override));
        MOCK_METHOD(void, addLocalConnection, (const std::shared_ptr<LocalConnection>&), (override));
        MOCK_METHOD(void, removeLocalConnection, (const std::shared_ptr<LocalConnection>&), (override));
        MOCK_METHOD(void, messageFromLocalConnection, (const ipc::SharedMessage&, LocalConnection&), (override));
        MOCK_METHOD(void, setMessageHandler, (MessageHandler), (override));
        MOCK_METHOD(void, setClientConnectedHandler, (ClientHandler), (override));
        MOCK_METHOD(void, setClientDisconnectedHandler, (ClientHandler), (override));
        MOCK_METHOD(void, listenService, (), (override));
        MOCK_METHOD(void, listenService, (const std::string&), ());
        MOCK_METHOD(int, listenTcpLocal, (int), (override));
        MOCK_METHOD(void, listenTcpHost, (const std::string&, int), (override));
        MOCK_METHOD(int, port, (), (const, override));
        MOCK_METHOD(int, getConnectedClientCount, (), (const, override));
        MOCK_METHOD(void, waitConnectionsAtLeast, (size_t), (override));
        MOCK_METHOD(bool, waitConnectionsAtLeast, (size_t, std::chrono::milliseconds), (override));
        MOCK_METHOD(void, waitListening, (), (const, override));
        MOCK_METHOD(void, sendToAll, (const ipc::SharedMessage&), (override));
        MOCK_METHOD(void, sendToAll, (ipc::Message &&), (override));
        MOCK_METHOD(void, shutdown, (), (override));

        MOCK_METHOD(void, onConnect, (), ());
        MOCK_METHOD(void, send, (ipc::Message &&), ());
        MOCK_METHOD(void, send, (const ipc::SharedMessage&), ());
        MOCK_METHOD(void, scheduleClose, (), ());
    };

} // namespace quasar::mock
