#include "testpoint_peer.h"

#include <yandex_io/protos/quasar_proto_forward.h>
#include <yandex_io/protos/quasar_proto.pb.h>
#include <yandex_io/libs/logging/logging.h>

using namespace YandexIO;

TestpointPeer::TestpointPeer(quasar::ipc::IIpcFactory& factory)
    : connector_(factory.createIpcConnector("testpoint"))
{
    connector_->setMessageHandler([this](const auto& msg) {
        if (msg->has_testpoint_message()) {
            processTestpointMessage(msg->testpoint_message());
        }
    });
    connector_->connectToService();
}

TestpointPeer::~TestpointPeer() {
    connector_->shutdown();
}

void TestpointPeer::sendMessage(quasar::ipc::SharedMessage message)
{
    connector_->sendMessage(message);
}

void TestpointPeer::sendMessage(quasar::proto::TestpointMessage testpointMessage) {
    const auto message = quasar::ipc::buildMessage([&](auto& msg) {
        msg.mutable_testpoint_message()->Swap(&testpointMessage);
    });
    connector_->sendMessage(message);
}

// NOLINTNEXTLINE(readability-convert-member-functions-to-static)
void TestpointPeer::processTestpointMessage(const quasar::proto::TestpointMessage& message) {
    if (message.has_test_event()) {
        YIO_LOG_INFO(message.test_event());
    }
}

std::ostream& operator<<(std::ostream& os, const quasar::proto::TestEvent& event) {
    switch (event.event()) {
        case quasar::proto::TestEvent::START: {
            os << "TEST START. TEST NAME: " << event.test_name();
            break;
        }
        case quasar::proto::TestEvent::END: {
            os << "TEST END. TEST NAME: " << event.test_name();
            break;
        }
        case quasar::proto::TestEvent::LOG: {
            os << "TEST LOG: " << event.log();
            break;
        }
        default: {
            os << "UNKNOWN EVENT. TEST NAME: " << event.test_name();
            break;
        }
    }
    return os;
}
