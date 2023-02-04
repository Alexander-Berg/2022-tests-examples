#include <yandex_io/services/brickd/brick_endpoint.h>

#include <yandex_io/libs/base/utils.h>
#include <yandex_io/libs/device/device.h>
#include <yandex_io/libs/ipc/i_connector.h>
#include <yandex_io/libs/ipc/i_server.h>
#include <yandex_io/libs/json_utils/json_utils.h>
#include <yandex_io/libs/logging/logging.h>
#include <yandex_io/protos/quasar_proto.pb.h>
#include <yandex_io/tests/testlib/test_utils.h>
#include <yandex_io/tests/testlib/unittest_helper/unit_test_fixture.h>

using namespace quasar;
using namespace quasar::TestUtils;

namespace {

    class BrickdFixture: public QuasarUnitTestFixture {
    public:
        using Base = QuasarUnitTestFixture;

        void SetUp(NUnitTest::TTestContext& context) override {
            Base::SetUp(context);

            Json::Value& config = getDeviceForTests()->configuration()->getMutableConfig(testGuard);
            config["brickd"]["subscriptionModeByDefault"] = true;

            toBrickd = createIpcConnectorForTests("brickd");

            mockSyncd = createIpcServerForTests("syncd");
            mockSyncd->listenService();
        }

        void TearDown(NUnitTest::TTestContext& context) override {
            Base::TearDown(context);
        }

        void createEndpoint()
        {
            brickEndpoint = std::make_shared<BrickEndpoint>(getDeviceForTests(),
                                                            ipcFactoryForTests());

            /* Connect to endpoint */
            toBrickd->connectToService();
            toBrickd->waitUntilConnected(std::chrono::seconds(1));

            mockSyncd->waitConnectionsAtLeast(1);
        }

    protected:
        YandexIO::Configuration::TestGuard testGuard;

        /* Actual mock servers */
        std::shared_ptr<ipc::IServer> mockSyncd;

        std::shared_ptr<ipc::IConnector> toBrickd;

        std::shared_ptr<BrickEndpoint> brickEndpoint;
    };

} // namespace

Y_UNIT_TEST_SUITE_F(BrickEndpointTest, BrickdFixture)
{
    Y_UNIT_TEST(testNotSubscriptionByDefault)
    {
        Json::Value& config = getDeviceForTests()->configuration()->getMutableConfig(testGuard);
        config["brickd"]["subscriptionModeByDefault"] = false;

        createEndpoint();

        auto fromBrickd = createIpcConnectorForTests("brickd");
        std::promise<proto::BrickStatus> brickStatusPromise;
        fromBrickd->setMessageHandler([&](const auto& msg) {
            if (msg->has_brick_status()) {
                YIO_LOG_DEBUG("Brick status received");
                brickStatusPromise.set_value(msg->brick_status());
            }
        });
        fromBrickd->connectToService();
        fromBrickd->waitUntilConnected(std::chrono::seconds(1));

        proto::BrickStatus latestBrickStatus = brickStatusPromise.get_future().get();
        UNIT_ASSERT_EQUAL(latestBrickStatus, proto::BrickStatus::NOT_BRICK);
    }

    Y_UNIT_TEST(testOnAuthFailed)
    {
        createEndpoint();

        auto authFailedReceived = std::make_shared<std::promise<void>>();
        brickEndpoint->onQuasarMessageReceivedCallback = [&](const proto::QuasarMessage& message) {
            if (message.has_auth_failed()) {
                YIO_LOG_DEBUG("Auth failed received");
                authFailedReceived->set_value();
            }
        };

        proto::QuasarMessage message;
        message.mutable_auth_failed();
        mockSyncd->sendToAll(std::move(message));
        authFailedReceived->get_future().get();

        auto fromBrickd = createIpcConnectorForTests("brickd");
        std::promise<proto::BrickStatus> brickStatusPromise;
        fromBrickd->setMessageHandler([&](const auto& msg) {
            if (msg->has_brick_status()) {
                YIO_LOG_DEBUG("Brick status received");
                brickStatusPromise.set_value(msg->brick_status());
            }
        });
        fromBrickd->connectToService();
        fromBrickd->waitUntilConnected(std::chrono::seconds(1));

        proto::BrickStatus latestBrickStatus = brickStatusPromise.get_future().get();
        UNIT_ASSERT_EQUAL(latestBrickStatus, proto::BrickStatus::UNKNOWN_BRICK_STATUS);
    }

    Y_UNIT_TEST(testOnSubscriptionNotSubscription)
    {
        std::promise<proto::BrickStatus> brickStatusPromise;
        toBrickd->setMessageHandler([&](const auto& msg) {
            if (msg->has_brick_status()) {
                YIO_LOG_DEBUG("Brick status received");
                brickStatusPromise.set_value(msg->brick_status());
            }
        });

        createEndpoint();
        std::promise<void> subscriptionStateReceived;
        brickEndpoint->onQuasarMessageReceivedCallback = [&](const proto::QuasarMessage& message) {
            if (message.has_subscription_state()) {
                YIO_LOG_DEBUG("Subscription state received");
                subscriptionStateReceived.set_value();
            }
        };

        proto::BrickStatus latestBrickStatus = brickStatusPromise.get_future().get();
        UNIT_ASSERT_EQUAL(latestBrickStatus, proto::BrickStatus::UNKNOWN_BRICK_STATUS);
        brickStatusPromise = std::promise<proto::BrickStatus>();

        proto::QuasarMessage message;
        message.mutable_subscription_state()->set_subscription_info("{\"mode\":\"transaction\", \"howdy\":\"good\"}");
        message.mutable_subscription_state()->set_passport_uid("some nice uid");
        message.mutable_subscription_state()->set_last_update_time(std::time(nullptr));
        message.mutable_subscription_state()->set_is_saved_state(false);
        mockSyncd->sendToAll(std::move(message));
        subscriptionStateReceived.get_future().get();

        latestBrickStatus = brickStatusPromise.get_future().get();
        UNIT_ASSERT_EQUAL(latestBrickStatus, proto::BrickStatus::NOT_BRICK);
    }
}

namespace {

    class BrickdFixtureTestSubscription: public BrickdFixture {
    public:
        using Base = BrickdFixture;

        void SetUp(NUnitTest::TTestContext& context) override {
            Base::SetUp(context);

            toBrickd->setMessageHandler([&](const auto& msg) {
                if (msg->has_brick_status()) {
                    YIO_LOG_DEBUG("Brick status received");
                    brickStatusPromise.set_value(msg->brick_status());
                }
            });
        }

        void TearDown(NUnitTest::TTestContext& context) override {
            Base::TearDown(context);
        }

        void setMessageHandlers()
        {
            brickEndpoint->onQuasarMessageReceivedCallback = [&](const proto::QuasarMessage& message) {
                if (message.has_subscription_state()) {
                    YIO_LOG_DEBUG("Subscription state received");
                    subscriptionStateReceived.set_value();
                }
            };
        }

    protected:
        std::promise<proto::BrickStatus> brickStatusPromise;
        std::promise<void> subscriptionStateReceived;
    };

} // namespace

Y_UNIT_TEST_SUITE_F(BrickEndpointTestSubscription, BrickdFixtureTestSubscription)
{
    Y_UNIT_TEST(testOnSubscriptionMissingFields)
    {
        createEndpoint();
        setMessageHandlers();

        proto::BrickStatus latestBrickStatus = brickStatusPromise.get_future().get();
        UNIT_ASSERT_EQUAL(latestBrickStatus, proto::BrickStatus::UNKNOWN_BRICK_STATUS);
        brickStatusPromise = std::promise<proto::BrickStatus>();

        proto::QuasarMessage message;
        message.mutable_subscription_state()->set_subscription_info("{\"mode\":\"subscription\"}");
        message.mutable_subscription_state()->set_passport_uid("some nice uid");
        message.mutable_subscription_state()->set_last_update_time(std::time(nullptr));
        message.mutable_subscription_state()->set_is_saved_state(false);
        mockSyncd->sendToAll(std::move(message));
        subscriptionStateReceived.get_future().get();

        latestBrickStatus = brickStatusPromise.get_future().get();
        UNIT_ASSERT_EQUAL(latestBrickStatus, proto::BrickStatus::BRICK);
    }

    Y_UNIT_TEST(testOnSubscriptionNotEnabled)
    {
        createEndpoint();
        setMessageHandlers();

        proto::BrickStatus latestBrickStatus = brickStatusPromise.get_future().get();
        UNIT_ASSERT_EQUAL(latestBrickStatus, proto::BrickStatus::UNKNOWN_BRICK_STATUS);
        brickStatusPromise = std::promise<proto::BrickStatus>();

        proto::QuasarMessage message;
        message.mutable_subscription_state()->set_subscription_info("{\"mode\":\"subscription\", \"enabled\":false, \"ttl\": 1000}");
        message.mutable_subscription_state()->set_passport_uid("some nice uid");
        message.mutable_subscription_state()->set_last_update_time(std::time(nullptr));
        message.mutable_subscription_state()->set_is_saved_state(false);
        mockSyncd->sendToAll(std::move(message));
        subscriptionStateReceived.get_future().get();

        latestBrickStatus = brickStatusPromise.get_future().get();
        UNIT_ASSERT_EQUAL(latestBrickStatus, proto::BrickStatus::BRICK);
    }

    Y_UNIT_TEST(testOnSubscriptionNotEnabledWithSavedState)
    {
        createEndpoint();
        setMessageHandlers();

        proto::BrickStatus latestBrickStatus = brickStatusPromise.get_future().get();
        UNIT_ASSERT_EQUAL(latestBrickStatus, proto::BrickStatus::UNKNOWN_BRICK_STATUS);
        brickStatusPromise = std::promise<proto::BrickStatus>();

        proto::QuasarMessage message;
        message.mutable_subscription_state()->set_subscription_info("{\"mode\":\"subscription\", \"enabled\":false, \"ttl\": 1000}");
        message.mutable_subscription_state()->set_passport_uid("some nice uid");
        message.mutable_subscription_state()->set_last_update_time(std::time(nullptr));
        message.mutable_subscription_state()->set_is_saved_state(true);
        mockSyncd->sendToAll(std::move(message));
        subscriptionStateReceived.get_future().get();

        latestBrickStatus = brickStatusPromise.get_future().get();
        UNIT_ASSERT_EQUAL(latestBrickStatus, proto::BrickStatus::BRICK_BY_TTL);
    }

    Y_UNIT_TEST(testOnSubscriptionEnabled)
    {
        createEndpoint();
        setMessageHandlers();

        proto::BrickStatus latestBrickStatus = brickStatusPromise.get_future().get();
        UNIT_ASSERT_EQUAL(latestBrickStatus, proto::BrickStatus::UNKNOWN_BRICK_STATUS);
        brickStatusPromise = std::promise<proto::BrickStatus>();

        proto::QuasarMessage message;
        message.mutable_subscription_state()->set_subscription_info("{\"mode\":\"subscription\", \"enabled\":true, \"ttl\": 1000}");
        message.mutable_subscription_state()->set_passport_uid("some nice uid");
        message.mutable_subscription_state()->set_last_update_time(std::time(nullptr));
        message.mutable_subscription_state()->set_is_saved_state(false);
        mockSyncd->sendToAll(std::move(message));
        subscriptionStateReceived.get_future().get();

        latestBrickStatus = brickStatusPromise.get_future().get();
        UNIT_ASSERT_EQUAL(latestBrickStatus, proto::BrickStatus::NOT_BRICK);
    }

    Y_UNIT_TEST(testOnSubscriptionPeriodicBrick)
    {
        Json::Value& config = getDeviceForTests()->configuration()->getMutableConfig(testGuard);
        config["brickd"]["firstBrickCheckDelayMS"] = 2000;
        config["brickd"]["regularBrickCheckDelayMS"] = 1000;

        createEndpoint();
        setMessageHandlers();

        proto::BrickStatus latestBrickStatus = brickStatusPromise.get_future().get();
        UNIT_ASSERT_EQUAL(latestBrickStatus, proto::BrickStatus::UNKNOWN_BRICK_STATUS);
        brickStatusPromise = std::promise<proto::BrickStatus>();

        // subscription_info will be valid for 5 seconds
        proto::QuasarMessage message;
        message.mutable_subscription_state()->set_subscription_info("{\"mode\":\"subscription\", \"enabled\":true, \"ttl\": 5, \"howdy\":\"good\"}");
        message.mutable_subscription_state()->set_passport_uid("some nice uid");
        message.mutable_subscription_state()->set_last_update_time(std::time(nullptr));
        message.mutable_subscription_state()->set_is_saved_state(false);
        mockSyncd->sendToAll(std::move(message));
        subscriptionStateReceived.get_future().get();

        latestBrickStatus = brickStatusPromise.get_future().get();
        UNIT_ASSERT_EQUAL(latestBrickStatus, proto::BrickStatus::NOT_BRICK);
        brickStatusPromise = std::promise<proto::BrickStatus>();

        // after 5 seconds periodic check should brick the device
        latestBrickStatus = brickStatusPromise.get_future().get();
        UNIT_ASSERT_EQUAL(latestBrickStatus, proto::BrickStatus::BRICK);
        brickStatusPromise = std::promise<proto::BrickStatus>();
    }

    Y_UNIT_TEST(testOnSubscriptionPeriodicBrickWithSavedState)
    {
        Json::Value& config = getDeviceForTests()->configuration()->getMutableConfig(testGuard);
        config["brickd"]["firstBrickCheckDelayMS"] = 2000;
        config["brickd"]["regularBrickCheckDelayMS"] = 1000;

        createEndpoint();
        setMessageHandlers();

        proto::BrickStatus latestBrickStatus = brickStatusPromise.get_future().get();
        UNIT_ASSERT_EQUAL(latestBrickStatus, proto::BrickStatus::UNKNOWN_BRICK_STATUS);
        brickStatusPromise = std::promise<proto::BrickStatus>();

        // subscription_info will be valid for 5 seconds
        proto::QuasarMessage message;
        message.mutable_subscription_state()->set_subscription_info("{\"mode\":\"subscription\", \"enabled\":true, \"ttl\": 5, \"howdy\":\"good\"}");
        message.mutable_subscription_state()->set_passport_uid("some nice uid");
        message.mutable_subscription_state()->set_last_update_time(std::time(nullptr));
        message.mutable_subscription_state()->set_is_saved_state(true);
        mockSyncd->sendToAll(std::move(message));
        subscriptionStateReceived.get_future().get();

        latestBrickStatus = brickStatusPromise.get_future().get();
        UNIT_ASSERT_EQUAL(latestBrickStatus, proto::BrickStatus::NOT_BRICK);
        brickStatusPromise = std::promise<proto::BrickStatus>();

        // after 5 seconds periodic check should brick the device with status no internet
        latestBrickStatus = brickStatusPromise.get_future().get();
        UNIT_ASSERT_EQUAL(latestBrickStatus, proto::BrickStatus::BRICK_BY_TTL);
        brickStatusPromise = std::promise<proto::BrickStatus>();
    }

    Y_UNIT_TEST(testOnSubscriptionPeriodicBrickWithSteadyClock)
    {
        Json::Value& config = getDeviceForTests()->configuration()->getMutableConfig(testGuard);
        config["brickd"]["firstBrickCheckDelayMS"] = 2000;
        config["brickd"]["regularBrickCheckDelayMS"] = 1000;
        config["brickd"]["useSteadyClock"] = true;

        createEndpoint();
        setMessageHandlers();

        proto::BrickStatus latestBrickStatus = brickStatusPromise.get_future().get();
        UNIT_ASSERT_EQUAL(latestBrickStatus, proto::BrickStatus::UNKNOWN_BRICK_STATUS);
        brickStatusPromise = std::promise<proto::BrickStatus>();

        // subscription_info will be valid for 5 seconds
        proto::QuasarMessage message;
        message.mutable_subscription_state()->set_subscription_info("{\"mode\":\"subscription\", \"enabled\":true, \"ttl\": 5, \"howdy\":\"good\"}");
        message.mutable_subscription_state()->set_passport_uid("some nice uid");
        message.mutable_subscription_state()->set_last_update_time(std::time(nullptr));
        message.mutable_subscription_state()->set_is_saved_state(false);
        mockSyncd->sendToAll(std::move(message));
        subscriptionStateReceived.get_future().get();

        latestBrickStatus = brickStatusPromise.get_future().get();
        UNIT_ASSERT_EQUAL(latestBrickStatus, proto::BrickStatus::NOT_BRICK);
        brickStatusPromise = std::promise<proto::BrickStatus>();

        // after 5 seconds periodic check should brick the device
        latestBrickStatus = brickStatusPromise.get_future().get();
        UNIT_ASSERT_EQUAL(latestBrickStatus, proto::BrickStatus::BRICK);
        brickStatusPromise = std::promise<proto::BrickStatus>();
    }

    Y_UNIT_TEST(testOnSubscriptionFirstBrickCheck)
    {
        Json::Value& config = getDeviceForTests()->configuration()->getMutableConfig(testGuard);
        config["brickd"]["firstBrickCheckDelayMS"] = 5000;

        createEndpoint();
        setMessageHandlers();

        proto::BrickStatus latestBrickStatus = brickStatusPromise.get_future().get();
        UNIT_ASSERT_EQUAL(latestBrickStatus, proto::BrickStatus::UNKNOWN_BRICK_STATUS);
        brickStatusPromise = std::promise<proto::BrickStatus>();

        // after 5 seconds on first check should brick the device with status no internet (got no updates)
        latestBrickStatus = brickStatusPromise.get_future().get();
        UNIT_ASSERT_EQUAL(latestBrickStatus, proto::BrickStatus::BRICK_BY_TTL);
        brickStatusPromise = std::promise<proto::BrickStatus>();
    }

    Y_UNIT_TEST(testOnSubscriptionFirstBrickCheckWithSteadyClock)
    {
        Json::Value& config = getDeviceForTests()->configuration()->getMutableConfig(testGuard);
        config["brickd"]["firstBrickCheckDelayMS"] = 5000;
        config["brickd"]["useSteadyClock"] = true;

        createEndpoint();
        setMessageHandlers();

        proto::BrickStatus latestBrickStatus = brickStatusPromise.get_future().get();
        UNIT_ASSERT_EQUAL(latestBrickStatus, proto::BrickStatus::UNKNOWN_BRICK_STATUS);
        brickStatusPromise = std::promise<proto::BrickStatus>();

        // after 5 seconds periodic check should brick the device
        latestBrickStatus = brickStatusPromise.get_future().get();
        UNIT_ASSERT_EQUAL(latestBrickStatus, proto::BrickStatus::BRICK_BY_TTL);
        brickStatusPromise = std::promise<proto::BrickStatus>();
    }

    Y_UNIT_TEST(testOnSubscriptionFirstBrickCheckWithSteadyClockAndSavedState)
    {
        Json::Value& config = getDeviceForTests()->configuration()->getMutableConfig(testGuard);
        config["brickd"]["firstBrickCheckDelayMS"] = 5000;
        config["brickd"]["regularBrickCheckDelayMS"] = 1000;
        config["brickd"]["useSteadyClock"] = true;

        createEndpoint();
        setMessageHandlers();

        proto::BrickStatus latestBrickStatus = brickStatusPromise.get_future().get();
        UNIT_ASSERT_EQUAL(latestBrickStatus, proto::BrickStatus::UNKNOWN_BRICK_STATUS);
        brickStatusPromise = std::promise<proto::BrickStatus>();

        // subscription_info will be valid for 500 seconds - shouldn't brick by ttl
        proto::QuasarMessage message;
        message.mutable_subscription_state()->set_subscription_info("{\"mode\":\"subscription\", \"enabled\":true, \"ttl\": 500, \"howdy\":\"good\"}");
        message.mutable_subscription_state()->set_passport_uid("some nice uid");
        message.mutable_subscription_state()->set_last_update_time(std::time(nullptr));
        message.mutable_subscription_state()->set_is_saved_state(true);
        mockSyncd->sendToAll(std::move(message));
        subscriptionStateReceived.get_future().get();

        latestBrickStatus = brickStatusPromise.get_future().get();
        UNIT_ASSERT_EQUAL(latestBrickStatus, proto::BrickStatus::NOT_BRICK);
        brickStatusPromise = std::promise<proto::BrickStatus>();

        // after first check (5 seconds) should brick the device
        latestBrickStatus = brickStatusPromise.get_future().get();
        UNIT_ASSERT_EQUAL(latestBrickStatus, proto::BrickStatus::BRICK_BY_TTL);
        brickStatusPromise = std::promise<proto::BrickStatus>();
    }
}
