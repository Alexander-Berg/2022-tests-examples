#include <yandex_io/sdk/private/endpoint_storage/capability_proxy.h>
#include <yandex_io/sdk/private/endpoint_storage/mocks/mock_connection_registry.h>
#include <yandex_io/sdk/private/endpoint_storage/converters/remote_object_id_factory.h>
#include <yandex_io/sdk/private/endpoint_storage/converters/remoting_message_builder.h>

#include <yandex_io/sdk/private/remoting/remoting_message_router.h>
#include <yandex_io/sdk/private/remoting/mocks/mock_i_remoting_connection.h>

#include <yandex_io/sdk/interfaces/mocks/mock_i_capability.h>

#include <alice/protos/endpoint/capability.pb.h>

#include <yandex_io/tests/testlib/unittest_helper/unit_test_fixture.h>

#include <library/cpp/testing/gmock_in_unittest/gmock.h>
#include <library/cpp/testing/unittest/registar.h>

#include <google/protobuf/util/message_differencer.h>
#include <google/protobuf/any.pb.h>
#include <google/protobuf/timestamp.pb.h>

#include <memory>

using namespace YandexIO;
using namespace quasar;

namespace {

    NAlice::TCapabilityHolder buildTestCapabilityProto() {
        // build some state
        NAlice::TCapabilityHolder capability;
        auto animationCapability = capability.mutable_animationcapability();
        auto meta = animationCapability->mutable_meta();
        meta->add_supporteddirectives(NAlice::TCapability::DrawAnimationDirectiveType);
        return capability;
    }

    MATCHER_P(VerifySameProtobufMessage, targetMessage, "description") {
        const auto& message = arg;
        return google::protobuf::util::MessageDifferencer::Equals(message, targetMessage);
    }

    MATCHER_P(VerifyIgnoredConnections, target, "description") {
        const std::set<std::string>& ignoredConnections = arg;
        return target == ignoredConnections;
    }

    MATCHER_P(VerifySameCapabilityEvents, targetEvents, "description") {
        const std::vector<NAlice::TCapabilityEvent>& events = arg;

        if (events.size() != targetEvents.size()) {
            return false;
        }
        for (size_t i = 0; i < targetEvents.size(); ++i) {
            if (!google::protobuf::util::MessageDifferencer::Equals(events[i], targetEvents[i])) {
                return false;
            }
        }
        return true;
    }

} // namespace

Y_UNIT_TEST_SUITE_F(TestCapabilityProxy, QuasarUnitTestFixture) {
    Y_UNIT_TEST(TestEventsAreExecutedAndRouted) {
        const auto router = std::make_shared<RemotingMessageRouter>();
        const auto connections = std::make_shared<MockConnectionsRegistry>();
        const auto connection = std::make_shared<MockIRemotingConnection>();
        const auto capabilityListener = std::make_shared<MockICapabilityListener>();
        const std::string connectionName = "EndpointStorageProxy";
        const std::string endpointId = "endpointId";

        connections->putConnection(connectionName, connection);

        auto capabilityProto = buildTestCapabilityProto();

        const auto capability = std::make_shared<CapabilityProxy>(
            "capability_id", capabilityProto, nullptr, router, connections);
        capability->init(endpointId);

        capability->addListener(capabilityListener);

        // incoming connection should be ignored
        const std::set<std::string> ignoredConnections{connectionName};

        // test onCapabilityStateChanged
        {
            // update state
            capabilityProto.mutable_animationcapability()->mutable_meta()->add_supporteddirectives(NAlice::TCapability::SetTemperatureKDirectiveType);
            const auto remoting = RemotingMessageBuilder::buildCapabilityStateChanged(endpointId, capability, capabilityProto);
            EXPECT_CALL(*connections, broadcastMessage(VerifySameProtobufMessage(remoting), VerifyIgnoredConnections(ignoredConnections)));
            EXPECT_CALL(*capabilityListener, onCapabilityStateChanged(testing::_, VerifySameProtobufMessage(capabilityProto)));

            const std::shared_ptr<IRemoteObject> capabilityRemoteObject = capability;
            capabilityRemoteObject->handleRemotingMessage(remoting, connection);
        }

        // test onCapabilityEvents
        {
            NAlice::TCapabilityEvent event;
            event.mutable_timestamp()->set_seconds(1);
            event.mutable_timestamp()->set_nanos(2);
            event.mutable_event()->PackFrom(NAlice::TButtonCapability::TButtonClickEvent());
            const std::vector<NAlice::TCapabilityEvent> events = {event};

            const auto remoting = RemotingMessageBuilder::buildCapabilityEvents(endpointId, capability, events);
            EXPECT_CALL(*connections, broadcastMessage(VerifySameProtobufMessage(remoting), VerifyIgnoredConnections(ignoredConnections)));
            EXPECT_CALL(*capabilityListener, onCapabilityEvents(testing::_, VerifySameCapabilityEvents(events)));

            const std::shared_ptr<IRemoteObject> capabilityRemoteObject = capability;
            capabilityRemoteObject->handleRemotingMessage(remoting, connection);
        }

    }
}
