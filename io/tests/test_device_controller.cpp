#include <yandex_io/services/aliced/alice_config/alice_config.h>
#include <yandex_io/services/aliced/device_state/alice_device_state.h>

#include <yandex_io/libs/delay_timings_policy/delay_timings_policy.h>
#include <yandex_io/libs/json_utils/json_utils.h>
#include <yandex_io/libs/logging/logging.h>
#include <yandex_io/services/aliced/device_controller.h>
#include <yandex_io/tests/testlib/test_callback_queue.h>
#include <yandex_io/tests/testlib/unittest_helper/unit_test_fixture.h>
#include <yandex_io/capabilities/alice/interfaces/mocks/mock_i_alice_capability.h>
#include <alice/protos/endpoint/capability.pb.h>

#include <library/cpp/testing/unittest/env.h>
#include <google/protobuf/timestamp.pb.h>

using namespace YandexIO;
using namespace quasar;

namespace {
    AliceConfig createAliceConfig(std::shared_ptr<YandexIO::IDevice> device) {
        Json::Value fileConfig;
        return {device, std::move(fileConfig)};
    }

    class MockICapability: public ICapability {
    public:
        MOCK_METHOD(NAlice::TCapabilityHolder, getState, (), (const, override));
        MOCK_METHOD(IDirectiveHandlerPtr, getDirectiveHandler, (), (override));
        MOCK_METHOD(void, addListener, (std::weak_ptr<IListener> wlistener), (override));
        MOCK_METHOD(void, removeListener, (std::weak_ptr<IListener> wlistener), (override));
    };

    class MockIEndpoint: public IEndpoint {
    public:
        const std::string id = "ID";

        MOCK_METHOD(const std::string&, getId, (), (const, override));
        MOCK_METHOD(NAlice::TEndpoint, getState, (), (const, override));
        MOCK_METHOD(NAlice::TEndpoint::TStatus, getStatus, (), (const, override));
        MOCK_METHOD(void, setStatus, (const NAlice::TEndpoint::TStatus&), (override));

        MOCK_METHOD(void, addCapability, (const std::shared_ptr<ICapability>& capability), (override));
        MOCK_METHOD(void, removeCapability, (const std::shared_ptr<ICapability>& capability), (override));
        MOCK_METHOD(std::list<std::shared_ptr<ICapability>>, getCapabilities, (), (const, override));
        MOCK_METHOD(void, addListener, (std::weak_ptr<IListener> wlistener), (override));
        MOCK_METHOD(void, removeListener, (std::weak_ptr<IListener> wlistener), (override));
    };
} // namespace

Y_UNIT_TEST_SUITE(DeviceController) {
    Y_UNIT_TEST_F(testReportableCapability, QuasarUnitTestFixture) {
        auto mockAliceCapability = std::make_shared<MockIAliceCapability>();
        auto callbackQueue = std::make_shared<TestCallbackQueue>();

        auto cap = std::make_shared<MockICapability>();

        NAlice::TCapabilityHolder holder;
        holder.mutable_onoffcapability()->mutable_meta()->set_reportable(true);

        ON_CALL(*cap, getState()).WillByDefault(testing::Return(holder));

        AliceConfig aliceConfig(createAliceConfig(getDeviceForTests()));
        auto dc = std::make_shared<DeviceController>(callbackQueue, aliceConfig,
                                                     std::make_shared<LegacyIotCapability>(
                                                         ipcFactoryForTests()->createIpcConnector("iot")),
                                                     std::make_unique<quasar::BackoffRetriesWithRandomPolicy>(1));
        EXPECT_CALL(*mockAliceCapability, startRequest);
        dc->setAliceCapability(mockAliceCapability);
        dc->init(getDeviceForTests()->deviceId(), NAlice::TEndpoint::SpeakerEndpointType);
        auto endpoint = dc->getEndpointStorage()->createEndpoint("ID", NAlice::TEndpoint::SpeakerEndpointType, NAlice::TEndpoint::TDeviceInfo(), nullptr);
        endpoint->addCapability(cap);
        dc->getEndpointStorage()->addEndpoint(endpoint);
        std::shared_ptr<ICapability::IListener>(dc)->onCapabilityStateChanged(cap, NAlice::TCapabilityHolder());
        callbackQueue->pumpDelayedQueueUntilEmpty();
    }

    Y_UNIT_TEST_F(testNonreportableCapability, QuasarUnitTestFixture) {
        auto mockAliceCapability = std::make_shared<MockIAliceCapability>();
        auto callbackQueue = std::make_shared<TestCallbackQueue>();

        auto cap = std::make_shared<MockICapability>();

        NAlice::TCapabilityHolder holder;
        holder.mutable_onoffcapability()->mutable_meta()->set_reportable(false);

        ON_CALL(*cap, getState()).WillByDefault(testing::Return(holder));

        AliceConfig aliceConfig(createAliceConfig(getDeviceForTests()));

        auto dc = std::make_shared<DeviceController>(callbackQueue, aliceConfig,
                                                     std::make_shared<LegacyIotCapability>(
                                                         ipcFactoryForTests()->createIpcConnector("iot")),
                                                     std::make_unique<quasar::BackoffRetriesWithRandomPolicy>(1));
        EXPECT_CALL(*mockAliceCapability, startRequest).Times(0);
        dc->setAliceCapability(mockAliceCapability);
        dc->init(getDeviceForTests()->deviceId(), NAlice::TEndpoint::SpeakerEndpointType);
        auto endpoint = dc->getEndpointStorage()->createEndpoint("ID", NAlice::TEndpoint::SpeakerEndpointType, NAlice::TEndpoint::TDeviceInfo(), nullptr);
        endpoint->addCapability(cap);
        dc->getEndpointStorage()->addEndpoint(endpoint);
        std::shared_ptr<ICapability::IListener>(dc)->onCapabilityStateChanged(cap, NAlice::TCapabilityHolder());
        callbackQueue->pumpDelayedQueueUntilEmpty();
    }

    Y_UNIT_TEST_F(testEventSendingRetries, QuasarUnitTestFixture) {
        auto mockAliceCapability = std::make_shared<MockIAliceCapability>();
        auto callbackQueue = std::make_shared<TestCallbackQueue>();

        auto cap = std::make_shared<MockICapability>();

        NAlice::TCapabilityHolder holder;
        holder.mutable_onoffcapability()->mutable_meta()->set_reportable(true);

        ON_CALL(*cap, getState()).WillByDefault(testing::Return(holder));

        AliceConfig aliceConfig(createAliceConfig(getDeviceForTests()));

        auto dc = std::make_shared<DeviceController>(callbackQueue, aliceConfig,
                                                     std::make_shared<LegacyIotCapability>(
                                                         ipcFactoryForTests()->createIpcConnector("iot")),
                                                     std::make_unique<quasar::BackoffRetriesWithRandomPolicy>(1));

        std::shared_ptr<VinsRequest> request(nullptr);
        EXPECT_CALL(*mockAliceCapability, startRequest).Times(2).WillRepeatedly(testing::SaveArg<0>(&request));
        dc->setAliceCapability(mockAliceCapability);
        dc->init(getDeviceForTests()->deviceId(), NAlice::TEndpoint::SpeakerEndpointType);
        auto endpoint = dc->getEndpointStorage()->createEndpoint("ID", NAlice::TEndpoint::SpeakerEndpointType, NAlice::TEndpoint::TDeviceInfo(), nullptr);
        endpoint->addCapability(cap);
        dc->getEndpointStorage()->addEndpoint(endpoint);
        NAlice::TCapabilityEvent event;
        event.mutable_timestamp()->set_seconds(12345);
        std::shared_ptr<ICapability::IListener>(dc)->onCapabilityEvents(cap, {event});
        const auto originalEvent = request->getEvent();
        dc->onAliceRequestError(request, "errorCode", "errorText");
        callbackQueue->pumpDelayedCallback();
        const auto retriedEvent = request->getEvent();

        ASSERT_EQ(originalEvent, retriedEvent);
    }

    Y_UNIT_TEST_F(testEventSendingRetriesBatching, QuasarUnitTestFixture) {
        auto mockAliceCapability = std::make_shared<MockIAliceCapability>();
        auto callbackQueue = std::make_shared<TestCallbackQueue>();

        auto cap = std::make_shared<MockICapability>();

        NAlice::TCapabilityHolder holder;
        holder.mutable_onoffcapability()->mutable_meta()->set_reportable(true);

        ON_CALL(*cap, getState()).WillByDefault(testing::Return(holder));

        AliceConfig aliceConfig(createAliceConfig(getDeviceForTests()));

        auto dc = std::make_shared<DeviceController>(callbackQueue, aliceConfig,
                                                     std::make_shared<LegacyIotCapability>(
                                                         ipcFactoryForTests()->createIpcConnector("iot")),
                                                     std::make_unique<quasar::BackoffRetriesWithRandomPolicy>(1));

        std::shared_ptr<VinsRequest> request(nullptr);
        EXPECT_CALL(*mockAliceCapability, startRequest).Times(3).WillRepeatedly(testing::SaveArg<0>(&request));
        dc->setAliceCapability(mockAliceCapability);
        dc->init(getDeviceForTests()->deviceId(), NAlice::TEndpoint::SpeakerEndpointType);
        auto endpoint = dc->getEndpointStorage()->createEndpoint("ID", NAlice::TEndpoint::SpeakerEndpointType, NAlice::TEndpoint::TDeviceInfo(), nullptr);
        endpoint->addCapability(cap);
        dc->getEndpointStorage()->addEndpoint(endpoint);

        // First request
        NAlice::TCapabilityEvent event;
        event.mutable_timestamp()->set_seconds(12345);
        std::shared_ptr<ICapability::IListener>(dc)->onCapabilityEvents(cap, {event});
        const auto originalEvent = request->getEvent();

        // Error, should move request to "toBeSent"
        dc->onAliceRequestError(request, "errorCode", "errorText");

        // Second request
        event.mutable_timestamp()->set_seconds(123456);
        std::shared_ptr<ICapability::IListener>(dc)->onCapabilityEvents(cap, {event});
        const auto newEvent = request->getEvent();
        ASSERT_NE(originalEvent, newEvent);
        // Error, should move both events to the error thingy
        dc->onAliceRequestError(request, "errorCode", "errorText");

        callbackQueue->pumpDelayedCallback();
        const auto retriedEvent = request->getEvent();
        ASSERT_EQ(newEvent, retriedEvent);

        const auto retriedEventsJson = retriedEvent["payload"]["typed_semantic_frame"]["endpoint_events_batch_semantic_frame"]["batch"]["batch_value"]["batch"][0]["capability_events"];
        const auto originalEventsJson = originalEvent["payload"]["typed_semantic_frame"]["endpoint_events_batch_semantic_frame"]["batch"]["batch_value"]["batch"][0]["capability_events"][0];

        // Both events should be in a request
        ASSERT_EQ(retriedEventsJson.size(), 2u);
        ASSERT_TRUE(std::any_of(retriedEventsJson.begin(), retriedEventsJson.end(),
                                [&originalEventsJson](const auto& x) { return originalEventsJson == x; }));

        // Pump second delayed callback from onAliceRequestError so that we don't leak anything
        callbackQueue->pumpDelayedQueueUntilEmpty();
    }
}
