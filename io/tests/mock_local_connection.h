#pragma once

#include <yandex_io/libs/ipc/mixed/mixed_server.h>

#include <yandex_io/protos/quasar_proto.pb.h>

#include <library/cpp/testing/gmock_in_unittest/gmock.h>

namespace quasar::mock {

    class LocalConnection: public quasar::ipc::detail::mixed::MixedServer::LocalConnection,
                           public std::enable_shared_from_this<quasar::mock::LocalConnection> {
    public:
        std::shared_ptr<IClientConnection> share() override {
            auto self = shared_from_this();
            if (!self) {
                abort();
            }
            return self;
        }
        MOCK_METHOD(void, onConnect, (), (override));
        MOCK_METHOD(void, onDisconnect, (), (override));
        MOCK_METHOD(void, send, (ipc::Message &&), (override));
        MOCK_METHOD(void, send, (const ipc::SharedMessage&), (override));
        MOCK_METHOD(void, scheduleClose, (), (override));
    };

} // namespace quasar::mock
