#include <yandex_io/sdk/private/endpoint_storage/endpoint.h>
#include <yandex_io/sdk/private/endpoint_storage/connection_registry.h>
#include <yandex_io/sdk/private/endpoint_storage/mocks/mock_connection_registry.h>
#include <yandex_io/sdk/private/endpoint_storage/converters/remote_object_id_factory.h>
#include <yandex_io/sdk/private/endpoint_storage/converters/converters.h>
#include <yandex_io/sdk/private/endpoint_storage/converters/remoting_message_builder.h>

#include <yandex_io/sdk/private/remoting/remoting_message_router.h>
#include <yandex_io/sdk/private/remoting/mocks/mock_i_remoting_connection.h>
#include <yandex_io/sdk/interfaces/mocks/mock_i_capability.h>
#include <yandex_io/sdk/interfaces/mocks/mock_i_directive_handler.h>
#include <yandex_io/sdk/interfaces/mocks/mock_i_endpoint.h>

#include <alice/protos/endpoint/capability.pb.h>

#include <yandex_io/libs/protobuf_utils/debug.h>
#include <yandex_io/libs/logging/logging.h>
#include <yandex_io/tests/testlib/unittest_helper/unit_test_fixture.h>

#include <library/cpp/testing/gmock_in_unittest/gmock.h>
#include <library/cpp/testing/unittest/registar.h>

#include <google/protobuf/util/message_differencer.h>
#include <google/protobuf/any.pb.h>
#include <google/protobuf/timestamp.pb.h>

#include <memory>

using namespace YandexIO;
using namespace quasar;
using namespace testing;

namespace {

    MATCHER_P(VerifyEndpointId, endpointId, "description") {
        const std::shared_ptr<IEndpoint>& endpoint = arg;
        if (endpoint->getId() != endpointId) {
            *result_listener << "Invalid EndpointID. Got: " << endpoint->getId() << ", Expected: " << endpointId;
            return false;
        }
        return true;
    }

    MATCHER_P(VerifyCapabilityId, capabilityId, "description") {
        const std::shared_ptr<ICapability>& capability = arg;
        if (capability->getId() != capabilityId) {
            *result_listener << "Invalid CapabilityId. Got: " << capability->getId() << ", Expected: " << capabilityId;
            return false;
        }
        return true;
    }

    MATCHER_P(VerifySameRemoteMessage, targetMessage, "description") {
        const proto::Remoting& message = arg;
        return google::protobuf::util::MessageDifferencer::Equals(message, targetMessage);
    }

    MATCHER_P(VerifySameRemoteEndpointMessageExceptReqId, targetMessage, "description") {
        proto::Remoting message = arg;
        if (!message.endpoint_method().has_id()) {
            *result_listener << "Incoming message doesn't have id: " << shortUtf8DebugString(message);
            return false;
        }
        message.mutable_endpoint_method()->clear_id();
        auto targetCopy = targetMessage;
        targetCopy.mutable_endpoint_method()->clear_id();

        return google::protobuf::util::MessageDifferencer::Equals(message, targetCopy);
    }

    MATCHER_P(VerifyEndpointStatus, targetStatus, "description") {
        const proto::Remoting& message = arg;
        if (!message.has_endpoint_method()) {
            *result_listener << "Incoming message doesn't have endpoint_method: " << shortUtf8DebugString(message);
            return false;
        }
        const auto& method = message.endpoint_method();
        if (method.method() != quasar::proto::Remoting::EndpointMethod::SYNC_STATE) {
            *result_listener << "Expected SYNC_STATE method. Got: " << shortUtf8DebugString(method);
            return false;
        }
        if (!method.has_state()) {
            *result_listener << "Incoming doesn't have State: " << shortUtf8DebugString(method);
            return false;
        }
        return method.state().status().status() == targetStatus;
    }

    MATCHER_P(VerifyIgnoredConnections, target, "description") {
        const std::set<std::string>& ignoredConnections = arg;
        return target == ignoredConnections;
    }

    MATCHER_P(VerifyTargetConnection, target, "description") {
        const std::string& connection = arg;
        return target == connection;
    }

} // namespace

Y_UNIT_TEST_SUITE_F(TestEndpoint, QuasarUnitTestFixture) {
    Y_UNIT_TEST(EndpointHostAddRemoveCapabilityFromProxy) {
        // test checks that adding/removing requests from Proxy Endpoint
        // to Host Endpoints works (host add/remove capability)

        const auto router = std::make_shared<RemotingMessageRouter>();
        const auto connections = std::make_shared<MockConnectionsRegistry>();
        const auto connection = std::make_shared<MockIRemotingConnection>();
        const auto endpointListener = std::make_shared<MockIEndpointListener>();
        const std::set<std::string> targetIgnoredConnections{};
        const auto remoteCapability = std::make_shared<MockICapability>("remote_capability_1", nullptr);

        connections->putConnection("EndpointStorageProxy", connection);

        constexpr bool isHost = true;
        const auto endpoint = std::make_shared<Endpoint>(
            "id", NAlice::TEndpoint::SpeakerEndpointType,
            NAlice::TEndpoint::TDeviceInfo{}, nullptr, "StorageHost", isHost, "StorageHost",
            router, connections);
        endpoint->initRemoting();

        endpoint->addListener(endpointListener);

        // test add capability
        {
            // Host should broadcast message to all proxies
            EXPECT_CALL(*connections, broadcastMessage(_, VerifyIgnoredConnections(targetIgnoredConnections)));
            EXPECT_CALL(*endpointListener, onCapabilityAdded(VerifyEndpointId(endpoint->getId()), VerifyCapabilityId(remoteCapability->getId())));

            const auto remoting = RemotingMessageBuilder::buildAddCapability(endpoint, remoteCapability, "EndpointStorageProxy");
            const std::shared_ptr<IRemoteObject> endpointRemoteObject = endpoint;
            endpointRemoteObject->handleRemotingMessage(remoting, connection);
        }

        // test remove capability
        {
            // Host should broadcast message to all proxies
            EXPECT_CALL(*connections, broadcastMessage(_, VerifyIgnoredConnections(targetIgnoredConnections)));
            EXPECT_CALL(*endpointListener, onCapabilityRemoved(VerifyEndpointId(endpoint->getId()), VerifyCapabilityId(remoteCapability->getId())));

            const auto remoting = RemotingMessageBuilder::buildRemoveCapability(endpoint, remoteCapability);
            const std::shared_ptr<IRemoteObject> endpointRemoteObject = endpoint;
            endpointRemoteObject->handleRemotingMessage(remoting, connection);
        }
    }

    Y_UNIT_TEST(EndpointProxyAddRemoveCapabilityFromHost) {
        // test checks that adding/removing requests from Host Endpoint
        // to Proxy Endpoints works (Proxy add/remove capability)

        const auto router = std::make_shared<RemotingMessageRouter>();
        const auto connections = std::make_shared<ConnectionRegistry>();
        const auto connectionToHost = std::make_shared<MockIRemotingConnection>();
        const auto endpointListener = std::make_shared<MockIEndpointListener>();
        const auto remoteCapability = std::make_shared<MockICapability>("remote_capability_1", nullptr);

        const std::string endpointHostConnectionName = "EndpointStorageHost";
        connections->putConnection(endpointHostConnectionName, connectionToHost);

        constexpr bool isHost = false;
        const auto endpoint = std::make_shared<Endpoint>(
            "id", NAlice::TEndpoint::SpeakerEndpointType,
            NAlice::TEndpoint::TDeviceInfo{}, nullptr, "StorageProxy", isHost, endpointHostConnectionName,
            router, connections);
        endpoint->initRemoting();

        endpoint->addListener(endpointListener);

        // test add capability
        {
            EXPECT_CALL(*endpointListener, onCapabilityAdded(VerifyEndpointId(endpoint->getId()), VerifyCapabilityId(remoteCapability->getId())));

            const auto remoting = RemotingMessageBuilder::buildAddCapability(endpoint, remoteCapability, endpointHostConnectionName);
            const std::shared_ptr<IRemoteObject> endpointRemoteObject = endpoint;
            endpointRemoteObject->handleRemotingMessage(remoting, connectionToHost);
        }

        // test remove capability
        {
            EXPECT_CALL(*endpointListener, onCapabilityRemoved(VerifyEndpointId(endpoint->getId()), VerifyCapabilityId(remoteCapability->getId())));

            const auto remoting = RemotingMessageBuilder::buildRemoveCapability(endpoint, remoteCapability);
            const std::shared_ptr<IRemoteObject> endpointRemoteObject = endpoint;
            endpointRemoteObject->handleRemotingMessage(remoting, connectionToHost);
        }
    }

    Y_UNIT_TEST(EndpoitProxyRouteMessagesToHost) {
        // test checks that adding/removing requests from Proxy Endpoint
        // to another Proxy Endpoints are routed to Endpoint Host

        const auto router = std::make_shared<RemotingMessageRouter>();
        const auto connections = std::make_shared<MockConnectionsRegistry>();
        const auto connectionToHost = std::make_shared<MockIRemotingConnection>();
        const auto connectionToProxy = std::make_shared<MockIRemotingConnection>();
        const auto remoteCapability = std::make_shared<MockICapability>("remote_capability_1", nullptr);

        const std::string endpointHostConnectionName = "EndpointStorageHost";
        const std::string endpointProxyConnectionName = "ProxyConnetion";
        connections->putConnection(endpointHostConnectionName, connectionToHost);
        connections->putConnection(endpointProxyConnectionName, connectionToProxy);

        constexpr bool isHost = false;
        const auto endpoint = std::make_shared<Endpoint>(
            "id", NAlice::TEndpoint::SpeakerEndpointType,
            NAlice::TEndpoint::TDeviceInfo{}, nullptr, "StorageRouter", isHost, endpointHostConnectionName,
            router, connections);
        endpoint->initRemoting();

        const auto remoteObjectId = RemoteObjectIdFactory::createId(endpoint);
        // test add capability
        {
            const auto remoting = RemotingMessageBuilder::buildAddCapability(endpoint, remoteCapability, endpointProxyConnectionName);
            EXPECT_CALL(*connections, sendMessage(VerifySameRemoteMessage(remoting), VerifyTargetConnection(endpointHostConnectionName)));

            const std::shared_ptr<IRemoteObject> endpointRemoteObject = endpoint;
            endpointRemoteObject->handleRemotingMessage(remoting, connectionToProxy);
        }

        // test remove capability
        {
            const auto remoting = RemotingMessageBuilder::buildRemoveCapability(endpoint, remoteCapability);
            EXPECT_CALL(*connections, sendMessage(VerifySameRemoteMessage(remoting), VerifyTargetConnection(endpointHostConnectionName)));

            const std::shared_ptr<IRemoteObject> endpointRemoteObject = endpoint;
            endpointRemoteObject->handleRemotingMessage(remoting, connectionToProxy);
        }
    }

    Y_UNIT_TEST(EndpointHostBroadcastLocalEvents) {
        // test checks that adding/removing from EndpointHost via c++ addEndpoint/removeEndpoint api
        // forces Endpoint to brodcast events to proxies

        const auto router = std::make_shared<RemotingMessageRouter>();
        const auto connections = std::make_shared<MockConnectionsRegistry>();
        const auto capability = std::make_shared<MockICapability>("capability_1", nullptr);

        constexpr bool isHost = true;
        const auto endpoint = std::make_shared<Endpoint>(
            "id", NAlice::TEndpoint::SpeakerEndpointType,
            NAlice::TEndpoint::TDeviceInfo{}, nullptr, "StorageHost", isHost, "StorageHost",
            router, connections);
        endpoint->initRemoting();

        const std::set<std::string> ignoredConnections; // none of connections are ignored

        {
            const auto remoting = RemotingMessageBuilder::buildAddCapability(endpoint, capability, "StorageHost");
            EXPECT_CALL(*connections, broadcastMessage(VerifySameRemoteEndpointMessageExceptReqId(remoting), VerifyIgnoredConnections(ignoredConnections)));
            endpoint->addCapability(capability);
        }

        {
            const auto remoting = RemotingMessageBuilder::buildRemoveCapability(endpoint, capability);
            EXPECT_CALL(*connections, broadcastMessage(VerifySameRemoteEndpointMessageExceptReqId(remoting), VerifyIgnoredConnections(ignoredConnections)));
            endpoint->removeCapability(capability);
        }
    }

    Y_UNIT_TEST(EndpointProxyBroadcastLocalEvents) {
        // test checks that adding/removing from EndpointProxy via c++ addEndpoint/removeEndpoint api
        // forces Endpoint to send events to EndpointHost only

        const auto router = std::make_shared<RemotingMessageRouter>();
        const auto connections = std::make_shared<MockConnectionsRegistry>();
        const auto capability = std::make_shared<MockICapability>("capability_1", nullptr);
        const std::string endpointHostName = "StorageHost";

        constexpr bool isHost = false;
        const auto endpoint = std::make_shared<Endpoint>(
            "id", NAlice::TEndpoint::SpeakerEndpointType,
            NAlice::TEndpoint::TDeviceInfo{}, nullptr, "StorageProxy", isHost, endpointHostName,
            router, connections);
        endpoint->initRemoting();

        {
            const auto remoting = RemotingMessageBuilder::buildAddCapability(endpoint, capability, "StorageProxy");
            EXPECT_CALL(*connections, sendMessage(VerifySameRemoteEndpointMessageExceptReqId(remoting), VerifyTargetConnection(endpointHostName)));
            endpoint->addCapability(capability);
        }

        {
            const auto remoting = RemotingMessageBuilder::buildRemoveCapability(endpoint, capability);
            EXPECT_CALL(*connections, sendMessage(VerifySameRemoteEndpointMessageExceptReqId(remoting), VerifyTargetConnection(endpointHostName)));
            endpoint->removeCapability(capability);
        }
    }

    Y_UNIT_TEST(EndpointHostSetStatusBroadcast) {
        // test checks that setStatus c++ api
        // forces Endpoint to broadcast SYNC_STATE event to Proxies

        const auto router = std::make_shared<RemotingMessageRouter>();
        const auto connections = std::make_shared<MockConnectionsRegistry>();
        const std::string endpointHostName = "StorageHost";

        constexpr bool isHost = true;
        const auto endpoint = std::make_shared<Endpoint>(
            "id", NAlice::TEndpoint::SpeakerEndpointType,
            NAlice::TEndpoint::TDeviceInfo{}, nullptr, endpointHostName, isHost, endpointHostName,
            router, connections);
        endpoint->initRemoting();
        const std::set<std::string> ignoredConnections; // none of connections are ignored

        {
            testing::InSequence s;

            EXPECT_CALL(*connections, broadcastMessage(VerifyEndpointStatus(NAlice::TEndpoint::Offline), VerifyIgnoredConnections(ignoredConnections)));
            EXPECT_CALL(*connections, broadcastMessage(VerifyEndpointStatus(NAlice::TEndpoint::Online), VerifyIgnoredConnections(ignoredConnections)));

            NAlice::TEndpoint::TStatus status;
            status.set_status(NAlice::TEndpoint::Offline);
            endpoint->setStatus(status);

            status.set_status(NAlice::TEndpoint::Online);
            endpoint->setStatus(status);
        }
    }

    Y_UNIT_TEST(EndpointSendCapabilityEvents) {
        // test checks that after installing Capability into EndpointHost/EndpointProxy
        // Capability events are sent to all CapabilityProxy

        auto runTest = [](bool isHost) {
            const auto router = std::make_shared<RemotingMessageRouter>();
            const auto connections = std::make_shared<MockConnectionsRegistry>();
            const auto capability = std::make_shared<MockICapability>("capability_1", nullptr);
            const std::string endpointHostName = "StorageHost";

            const auto endpoint = std::make_shared<Endpoint>(
                "id", NAlice::TEndpoint::SpeakerEndpointType,
                NAlice::TEndpoint::TDeviceInfo{}, nullptr, endpointHostName, isHost, endpointHostName,
                router, connections);
            endpoint->initRemoting();

            endpoint->addCapability(capability);

            const std::set<std::string> ignoredConnections; // none of connections should be ignored
            // test onCapabilityStateChanged
            {
                // build some state
                NAlice::TCapabilityHolder state;
                {
                    auto animationCapability = state.mutable_animationcapability();
                    auto meta = animationCapability->mutable_meta();
                    meta->add_supporteddirectives(NAlice::TCapability::DrawAnimationDirectiveType);
                }

                const auto remoting = RemotingMessageBuilder::buildCapabilityStateChanged(endpoint->getId(), capability, state);
                EXPECT_CALL(*connections, broadcastMessage(VerifySameRemoteMessage(remoting), VerifyIgnoredConnections(ignoredConnections)));
                capability->notifyStateChanged(state);
            }

            // test onCapabilityEvents
            {
                NAlice::TCapabilityEvent event;
                event.mutable_timestamp()->set_seconds(1);
                event.mutable_timestamp()->set_nanos(2);
                event.mutable_event()->PackFrom(NAlice::TButtonCapability::TButtonClickEvent());

                const std::vector<NAlice::TCapabilityEvent> events = {event};

                const auto remoting = RemotingMessageBuilder::buildCapabilityEvents(endpoint->getId(), capability, events);
                EXPECT_CALL(*connections, broadcastMessage(VerifySameRemoteMessage(remoting), VerifyIgnoredConnections(ignoredConnections)));
                capability->notifyEvents(events);
            }
        };

        // same behaviour for EndpointHost and EndpointProxy
        runTest(true);  // endpoint is host
        runTest(false); // endpoint is proxy
    }

    Y_UNIT_TEST(EndpointProxyCreateDirectiveHandlerProxyWithConnectionToHost) {
        // This test checks that when EndpointHost makes AddCapability
        // EndpointProxy creates CapabilitProxy and DirectiveHandlerProxy
        // with connection to EndpointHost (and CapabilityHost)

        const auto router = std::make_shared<RemotingMessageRouter>();
        const auto connection = std::make_shared<MockIRemotingConnection>();
        const auto connections = std::make_shared<MockConnectionsRegistry>();
        // directive handler
        const auto directiveHandler = std::make_shared<MockIDirectiveHandler>();
        const std::string directiveHandlerName = "directiveHandlerName";
        const std::set<std::string> directiveNames = {"directive1", "directive2"};
        const std::string endpointId = "endpoint_id";
        ON_CALL(*directiveHandler, getEndpointId()).WillByDefault(ReturnRef(endpointId));
        ON_CALL(*directiveHandler, getHandlerName()).WillByDefault(ReturnRef(directiveHandlerName));
        ON_CALL(*directiveHandler, getSupportedDirectiveNames()).WillByDefault(ReturnRef(directiveNames));

        const auto capability = std::make_shared<MockICapability>("capability_id", directiveHandler);

        const std::string endpointHostName = "StorageHost";
        connections->putConnection(endpointHostName, connection);

        constexpr bool isHost = false;
        const auto endpoint = std::make_shared<Endpoint>(
            endpointId, NAlice::TEndpoint::SpeakerEndpointType,
            NAlice::TEndpoint::TDeviceInfo{}, nullptr, "StorageProxy", isHost, endpointHostName,
            router, connections);
        endpoint->initRemoting();

        // add capability to "EndpointHost". endpoint will create CapabilityProxy
        const auto remoting = RemotingMessageBuilder::buildAddCapability(endpoint, capability, "StorageProxy");
        const std::shared_ptr<IRemoteObject> endpointRemoteObject = endpoint;
        endpointRemoteObject->handleRemotingMessage(remoting, connection);

        {
            const auto directive1 = std::make_shared<Directive>(Directive::Data("directive1", "local_action"));
            const auto endpointRemoteObjectId = RemoteObjectIdFactory::createId(endpoint);
            const auto remoting = RemotingMessageBuilder::buildDirectiveHandlerMethod(
                endpointRemoteObjectId, directiveHandler, directive1,
                quasar::proto::Remoting::DirectiveHandlerMethod::HANDLE);
            // test that call on CapabilityProxy will forward message to EndpointHost (with CapabilitHost)
            EXPECT_CALL(*connections, sendMessage(VerifySameRemoteMessage(remoting), endpointHostName));
            endpoint->getCapabilities().front()->getDirectiveHandler()->handleDirective(directive1);
        }
    }

    Y_UNIT_TEST(EndpointProxyCreateDirectiveHandlerProxyWithConnectionToHostAfterRoute) {
        // This test checks that when EndpointProxy makes AddCapability
        // AddCapability message is Routed to EndpointHost
        // Then it wait's for EndpointHost "AddCapability" message
        // and creates CapabilityProxy and DirectiveHandlerProxy with connection
        // to EndpointProxy (with CapabilityHost)

        const auto router = std::make_shared<RemotingMessageRouter>();
        const auto connectionEndpointHost = std::make_shared<MockIRemotingConnection>();
        const auto connectionEndpointProxy = std::make_shared<MockIRemotingConnection>();
        const auto connections = std::make_shared<MockConnectionsRegistry>();
        // directive handler
        const auto directiveHandler = std::make_shared<MockIDirectiveHandler>();
        const std::string directiveHandlerName = "directiveHandlerName";
        const std::set<std::string> directiveNames = {"directive1", "directive2"};
        const std::string endpointId = "endpoint_id";
        ON_CALL(*directiveHandler, getEndpointId()).WillByDefault(ReturnRef(endpointId));
        ON_CALL(*directiveHandler, getHandlerName()).WillByDefault(ReturnRef(directiveHandlerName));
        ON_CALL(*directiveHandler, getSupportedDirectiveNames()).WillByDefault(ReturnRef(directiveNames));

        const auto capability = std::make_shared<MockICapability>("capability_id", directiveHandler);

        const std::string endpointHostName = "StorageHost";
        const std::string endpointProxyName = "StorageProxy";
        connections->putConnection(endpointHostName, connectionEndpointHost);
        connections->putConnection(endpointProxyName, connectionEndpointProxy);

        constexpr bool isHost = false;
        const auto endpoint = std::make_shared<Endpoint>(
            endpointId, NAlice::TEndpoint::SpeakerEndpointType,
            NAlice::TEndpoint::TDeviceInfo{}, nullptr, "StorageRouter", isHost, endpointHostName,
            router, connections);
        endpoint->initRemoting();

        {
            // add capability to "EndpointHost". endpoint will create CapabilityProxy
            const auto remoting = RemotingMessageBuilder::buildAddCapability(endpoint, capability, endpointProxyName);
            const std::shared_ptr<IRemoteObject> endpointRemoteObject = endpoint;
            // EndpointProxy get's addCapability from another proxy. It should route message to host
            EXPECT_CALL(*connections, sendMessage(VerifySameRemoteMessage(remoting), VerifyTargetConnection(endpointHostName)));
            endpointRemoteObject->handleRemotingMessage(remoting, connectionEndpointProxy);

            // Make "response" from EndpointHost. Proxy should broadcast it
            const std::set<std::string> ignoredConnections{endpointHostName};
            EXPECT_CALL(*connections, broadcastMessage(VerifySameRemoteMessage(remoting), VerifyIgnoredConnections(ignoredConnections)));
            endpointRemoteObject->handleRemotingMessage(remoting, connectionEndpointHost);
        }

        // Test that DirectiveHandleProxy created with connection to EndpointProxy
        {
            const auto directive1 = std::make_shared<Directive>(Directive::Data("directive1", "local_action"));
            const auto endpointRemoteObjectId = RemoteObjectIdFactory::createId(endpoint);
            const auto remoting = RemotingMessageBuilder::buildDirectiveHandlerMethod(
                endpointRemoteObjectId, directiveHandler, directive1,
                quasar::proto::Remoting::DirectiveHandlerMethod::HANDLE);
            // test that call on CapabilityProxy will forward message to EndpointPrxy (with CapabilitHost)
            EXPECT_CALL(*connections, sendMessage(VerifySameRemoteMessage(remoting), endpointProxyName));
            endpoint->getCapabilities().front()->getDirectiveHandler()->handleDirective(directive1);
        }
    }

    Y_UNIT_TEST(DropProxyCapabilities) {
        auto test = [](bool isHost) {
            YIO_LOG_INFO("Run test isHost=" << isHost);
            const auto router = std::make_shared<RemotingMessageRouter>();
            const auto connection = std::make_shared<MockIRemotingConnection>();
            const auto connections = std::make_shared<MockConnectionsRegistry>();

            const auto name = isHost ? "StorageHost" : "StorageProxy";
            const auto endpoint = std::make_shared<Endpoint>(
                "endpointId", NAlice::TEndpoint::SpeakerEndpointType,
                NAlice::TEndpoint::TDeviceInfo{}, nullptr, name, isHost, "StorageHost",
                router, connections);
            const auto connectionName = isHost ? "StorageProxy" : "StorageHost";
            connections->putConnection(connectionName, connection);
            endpoint->initRemoting();

            {
                // inject 2 proxy capabilities on init
                const auto capability1 = std::make_shared<MockICapability>("capability1", nullptr);
                const auto capability2 = std::make_shared<MockICapability>("capability2", nullptr);
                proto::Endpoint endpointProto;
                auto cap1 = endpointProto.add_capabilities();
                cap1->mutable_capability()->CopyFrom(convertCapabilityToProtobuf(endpoint, capability1));
                cap1->set_storage_name(isHost ? "StorageProxy" : "StorageHost");
                auto cap2 = endpointProto.add_capabilities();
                cap2->mutable_capability()->CopyFrom(convertCapabilityToProtobuf(endpoint, capability2));
                cap2->set_storage_name(isHost ? "StorageProxy" : "StorageHost");

                endpoint->initProxyCapabilities(endpointProto);

                // install 2 CapabilityProxy and 1 capability
                const std::shared_ptr<IRemoteObject> endpointRemoteObject = endpoint;
                const auto capability3 = std::make_shared<MockICapability>("capability3", nullptr);
                const auto remoting1 = RemotingMessageBuilder::buildAddCapability(endpoint, capability3, connectionName);
                endpointRemoteObject->handleRemotingMessage(remoting1, connection);
                const auto capability4 = std::make_shared<MockICapability>("capability4", nullptr);
                const auto remoting2 = RemotingMessageBuilder::buildAddCapability(endpoint, capability4, connectionName);
                endpointRemoteObject->handleRemotingMessage(remoting2, connection);

                const auto capability5 = std::make_shared<MockICapability>("capability5", nullptr);
                endpoint->addCapability(capability5);
                UNIT_ASSERT_VALUES_EQUAL(endpoint->getCapabilities().size(), 5);
            }

            endpoint->dropProxyCapabilities();
            UNIT_ASSERT_VALUES_EQUAL(endpoint->getCapabilities().size(), 1);
            UNIT_ASSERT_VALUES_EQUAL(endpoint->getCapabilities().front()->getId(), "capability5");
        };
        test(true);  // EndpointHost
        test(false); // EndpointProxy
    }
}
