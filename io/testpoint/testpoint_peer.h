#pragma once

#include <yandex_io/libs/ipc/i_ipc_factory.h>
#include <yandex_io/protos/functional_tests.pb.h>

namespace YandexIO {
    class TestpointPeer {
    public:
        explicit TestpointPeer(quasar::ipc::IIpcFactory& factory);
        ~TestpointPeer();

        void sendMessage(quasar::ipc::SharedMessage message);
        void sendMessage(quasar::proto::TestpointMessage testpointMessage);

    private:
        void processTestpointMessage(const quasar::proto::TestpointMessage& message);

    private:
        std::shared_ptr<quasar::ipc::IConnector> connector_;
    };
} // namespace YandexIO

std::ostream& operator<<(std::ostream& os, const quasar::proto::TestEvent& event);
