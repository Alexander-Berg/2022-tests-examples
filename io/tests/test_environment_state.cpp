#include <yandex_io/services/aliced/device_state/environment_state.h>

#include <yandex_io/libs/json_utils/json_utils.h>
#include <yandex_io/libs/logging/logging.h>
#include <yandex_io/libs/telemetry/null/null_metrica.h>
#include <yandex_io/protos/quasar_proto.pb.h>
#include <yandex_io/tests/testlib/unittest_helper/unit_test_fixture.h>

#include <library/cpp/testing/unittest/env.h>

#include <speechkit/SpeechKit.h>
#include <speechkit/core/include/speechkit/PlatformInfo.h>

#include <memory>

using namespace quasar;

namespace {
    class PlatformInfoMock: public SpeechKit::PlatformInfo {
    public:
        using SharedPtr = std::shared_ptr<PlatformInfoMock>;
        using WeakPtr = std::weak_ptr<PlatformInfoMock>;
        using PlatformInfo::PlatformInfo;

        PlatformInfoMock(std::string appId, std::string model, std::string version, std::vector<std::string> features)
            : appId_(std::move(appId))
            , model_(std::move(model))
            , version_(std::move(version))
            , supportedFeatures_(std::move(features))
        {
        }

        std::string getAppId() const override {
            return appId_;
        }

        std::string getAppName() const override {
            return "";
        }
        std::string getAppPlatform() const override {
            return "";
        }
        std::string getAppVersion() const override {
            return version_;
        }
        std::string getFirmwareVersion() const override {
            return "";
        }
        std::string getDeviceColor() const override {
            return "";
        }
        std::string getDeviceModel() const override {
            return model_;
        }
        std::string getDeviceRevision() const override {
            return "";
        }
        std::string getDeviceManufacturer() const override {
            return "";
        }
        std::string getOSVersion() const override {
            return "";
        }
        std::string getAppDirectory() const override {
            return "";
        }
        std::string getOlsonDbTimezoneName() const override {
            return "";
        }
        std::vector<std::string> getSupportedFeatures() const override {
            return supportedFeatures_;
        }
        BluetoothInfo getBluetoothInfo() const override {
            return {};
        }
        std::string getQuasmodromGroup() const override {
            return "";
        }
        std::string getQuasmodromSubgroup() const override {
            return "";
        }

    private:
        std::string appId_;
        std::string model_;
        std::string version_;
        std::vector<std::string> supportedFeatures_;
    };

    class MockTelemetry: public NullMetrica {
    public:
        MOCK_METHOD(void, putAppEnvironmentValue, (const std::string&, const std::string&), (override));
        MOCK_METHOD(void, deleteAppEnvironmentValue, (const std::string&), (override));
    };

    class EnvironmentStateFixture: public QuasarUnitTestFixture {
    public:
        EnvironmentStateFixture() {
            auto speechKit = SpeechKit::SpeechKit::getInstance();
            speechKit->setUuid("local_uuid");
            speechKit->setDeviceId("local_device_id");

            std::vector<std::string> features{"feature_1", "feature_2"};
            auto platformInfoMock = std::make_shared<PlatformInfoMock>("local_app", "local_model", "local_version", features);
            speechKit->setPlatformInfo(platformInfoMock);
        }
    };

    proto::DeviceGroupState createStandaloneGroup() {
        proto::DeviceGroupState deviceGroupState;
        deviceGroupState.set_local_role(proto::DeviceGroupState::Role::DeviceGroupState_Role_STAND_ALONE);
        return deviceGroupState;
    }

    proto::DeviceGroupState createGroup(bool isLeader, bool isConnected, std::string connectedDeviceId, std::string connectedDevicePlatform) {
        proto::DeviceGroupState deviceGroupState;
        deviceGroupState.set_local_role(isLeader ? proto::DeviceGroupState::Role::DeviceGroupState_Role_LEADER
                                                 : proto::DeviceGroupState::Role::DeviceGroupState_Role_FOLLOWER);
        if (isLeader) {
            auto follower = deviceGroupState.mutable_follower();
            follower->set_connection_state(
                isConnected ? proto::DeviceGroupState::ConnectionState::DeviceGroupState_ConnectionState_CONNECTED
                            : proto::DeviceGroupState::ConnectionState::DeviceGroupState_ConnectionState_NONE);
            follower->set_device_id(TString(connectedDeviceId));
            follower->set_platform(TString(connectedDevicePlatform));
        } else {
            auto leader = deviceGroupState.mutable_leader();
            leader->set_connection_state(
                isConnected ? proto::DeviceGroupState::ConnectionState::DeviceGroupState_ConnectionState_CONNECTED
                            : proto::DeviceGroupState::ConnectionState::DeviceGroupState_ConnectionState_NONE);
            leader->set_device_id(TString(connectedDeviceId));
            leader->set_platform(TString(connectedDevicePlatform));
        }
        return deviceGroupState;
    }

    NAlice::TEnvironmentDeviceInfo createRemoteDevice(std::string connectedDeviceId, std::string connectedDevicePlatform) {
        NAlice::TEnvironmentDeviceInfo device;
        device.mutable_application()->set_uuid("remote_uuid");
        device.mutable_application()->set_deviceid(TString(connectedDeviceId));
        device.mutable_application()->set_devicemodel(TString(connectedDevicePlatform));
        device.mutable_application()->set_appid("remote_app");
        device.mutable_application()->set_appversion("remote_version");
        return device;
    }

    Y_UNIT_TEST_SUITE(EnvironmentStateTest) {
        Y_UNIT_TEST_F(checkStateWithoutTandem, EnvironmentStateFixture) {
            EnvironmentStateHolder holder("", std::make_shared<NullMetrica>());
            const auto state = holder.formatJson();
            UNIT_ASSERT(state["groups"].size() == 0);
        }

        Y_UNIT_TEST_F(checkStateWithTandem, EnvironmentStateFixture) {
            EnvironmentStateHolder holder("", std::make_shared<NullMetrica>());

            const auto group = createGroup(true, false, "remote_device_id", "remote_model");
            holder.updateTandemGroup(group);
            const auto state = holder.formatJson();
            YIO_LOG_INFO("environment state: " << jsonToString(state));

            // check application
            UNIT_ASSERT_EQUAL(state["devices"][0]["application"]["app_id"].asString(), "local_app");
            UNIT_ASSERT_EQUAL(state["devices"][0]["application"]["app_version"].asString(), "local_version");
            UNIT_ASSERT_EQUAL(state["devices"][0]["application"]["device_id"].asString(), "local_device_id");
            UNIT_ASSERT_EQUAL(state["devices"][0]["application"]["device_model"].asString(), "local_model");
            UNIT_ASSERT_EQUAL(state["devices"][0]["application"]["uuid"].asString(), "local_uuid");

            // check tandem connected state
            UNIT_ASSERT(state["devices"][0].isMember("device_state"));
            UNIT_ASSERT(state["devices"][0]["device_state"].isMember("tandem_state"));
            UNIT_ASSERT(state["devices"][0]["device_state"]["tandem_state"].isMember("connected"));
            UNIT_ASSERT_EQUAL(state["devices"][0]["device_state"]["tandem_state"]["connected"].asBool(), false);

            // check tandem configuration
            UNIT_ASSERT_EQUAL(state["groups"][0]["type"].asString(), "tandem");
            UNIT_ASSERT_EQUAL(state["groups"][0]["devices"][0]["id"], "local_device_id");
            UNIT_ASSERT_EQUAL(state["groups"][0]["devices"][0]["platform"], "local_model");
            UNIT_ASSERT_EQUAL(state["groups"][0]["devices"][0]["role"], "leader");
            UNIT_ASSERT_EQUAL(state["groups"][0]["devices"][1]["id"], "remote_device_id");
            UNIT_ASSERT_EQUAL(state["groups"][0]["devices"][1]["platform"], "remote_model");
            UNIT_ASSERT_EQUAL(state["groups"][0]["devices"][1]["role"], "follower");
        }

        Y_UNIT_TEST_F(checkStateAfterUnpairing, EnvironmentStateFixture) {
            EnvironmentStateHolder holder("", std::make_shared<NullMetrica>());

            // check after pairing
            auto group = createGroup(true, false, "remote_device_id", "remote_model");
            holder.updateTandemGroup(group);
            auto state = holder.formatJson();
            YIO_LOG_INFO("environment state with group: " << jsonToString(state));

            // check after unpairing
            group = createStandaloneGroup();
            holder.updateTandemGroup(group);
            YIO_LOG_INFO("environment state is not empty after unpairing");
        }

        Y_UNIT_TEST_F(checkStateWithRemoteConnected, EnvironmentStateFixture) {
            EnvironmentStateHolder holder("", std::make_shared<NullMetrica>());

            const auto group = createGroup(true, true, "remote_device_id", "remote_model");
            holder.updateTandemGroup(group);
            holder.updateRemoteDevice(createRemoteDevice("remote_device_id", "remote_model"));

            const auto state = holder.formatJson();
            YIO_LOG_INFO("environment state: " << jsonToString(state));

            // check local application
            UNIT_ASSERT_EQUAL(state["devices"][0]["application"]["app_id"].asString(), "local_app");
            UNIT_ASSERT_EQUAL(state["devices"][0]["application"]["app_version"].asString(), "local_version");
            UNIT_ASSERT_EQUAL(state["devices"][0]["application"]["device_id"].asString(), "local_device_id");
            UNIT_ASSERT_EQUAL(state["devices"][0]["application"]["device_model"].asString(), "local_model");
            UNIT_ASSERT_EQUAL(state["devices"][0]["application"]["uuid"].asString(), "local_uuid");

            // check remote application
            UNIT_ASSERT_EQUAL(state["devices"][1]["application"]["app_id"].asString(), "remote_app");
            UNIT_ASSERT_EQUAL(state["devices"][1]["application"]["app_version"].asString(), "remote_version");
            UNIT_ASSERT_EQUAL(state["devices"][1]["application"]["device_id"].asString(), "remote_device_id");
            UNIT_ASSERT_EQUAL(state["devices"][1]["application"]["device_model"].asString(), "remote_model");
            UNIT_ASSERT_EQUAL(state["devices"][1]["application"]["uuid"].asString(), "remote_uuid");

            // check tandem connected state
            UNIT_ASSERT(state["devices"][0].isMember("device_state"));
            UNIT_ASSERT(state["devices"][0]["device_state"].isMember("tandem_state"));
            UNIT_ASSERT(state["devices"][0]["device_state"]["tandem_state"].isMember("connected"));
            UNIT_ASSERT_EQUAL(state["devices"][0]["device_state"]["tandem_state"]["connected"].asBool(), true);
        }

        Y_UNIT_TEST_F(checkStateWithRemoteDisconnected, EnvironmentStateFixture) {
            EnvironmentStateHolder holder("", std::make_shared<NullMetrica>());

            auto group = createGroup(true, true, "remote_device_id", "remote_model");
            holder.updateTandemGroup(group);
            holder.updateRemoteDevice(createRemoteDevice("remote_device_id", "remote_model"));

            auto state = holder.formatJson();
            YIO_LOG_INFO("environment state: " << jsonToString(state));

            // check local application
            UNIT_ASSERT_EQUAL(state["devices"][0]["application"]["app_id"].asString(), "local_app");
            UNIT_ASSERT_EQUAL(state["devices"][0]["application"]["app_version"].asString(), "local_version");
            UNIT_ASSERT_EQUAL(state["devices"][0]["application"]["device_id"].asString(), "local_device_id");
            UNIT_ASSERT_EQUAL(state["devices"][0]["application"]["device_model"].asString(), "local_model");
            UNIT_ASSERT_EQUAL(state["devices"][0]["application"]["uuid"].asString(), "local_uuid");
            UNIT_ASSERT(state["devices"].size() == 2);

            // check tandem connected state
            UNIT_ASSERT(state["devices"][0].isMember("device_state"));
            UNIT_ASSERT(state["devices"][0]["device_state"].isMember("tandem_state"));
            UNIT_ASSERT(state["devices"][0]["device_state"]["tandem_state"].isMember("connected"));
            UNIT_ASSERT_EQUAL(state["devices"][0]["device_state"]["tandem_state"]["connected"].asBool(), true);

            // disconnect and check
            group = createGroup(true, false, "remote_device_id", "remote_model");
            holder.updateTandemGroup(group);
            state = holder.formatJson();
            YIO_LOG_INFO("environment state: " << jsonToString(state));

            // check local application
            UNIT_ASSERT_EQUAL(state["devices"][0]["application"]["app_id"].asString(), "local_app");
            UNIT_ASSERT_EQUAL(state["devices"][0]["application"]["app_version"].asString(), "local_version");
            UNIT_ASSERT_EQUAL(state["devices"][0]["application"]["device_id"].asString(), "local_device_id");
            UNIT_ASSERT_EQUAL(state["devices"][0]["application"]["device_model"].asString(), "local_model");
            UNIT_ASSERT_EQUAL(state["devices"][0]["application"]["uuid"].asString(), "local_uuid");
            UNIT_ASSERT(state["devices"].size() == 1);

            // check tandem disconnected connected state
            UNIT_ASSERT(state["devices"][0].isMember("device_state"));
            UNIT_ASSERT(state["devices"][0]["device_state"].isMember("tandem_state"));
            UNIT_ASSERT(state["devices"][0]["device_state"]["tandem_state"].isMember("connected"));
            UNIT_ASSERT_EQUAL(state["devices"][0]["device_state"]["tandem_state"]["connected"].asBool(), false);
        }

        Y_UNIT_TEST_F(checkDeviceSupportedFeaturesField, EnvironmentStateFixture) {
            EnvironmentStateHolder holder("", std::make_shared<NullMetrica>());

            const auto group = createGroup(true, false, "remote_device_id", "remote_model");
            holder.updateTandemGroup(group);
            const auto state = holder.formatJson();
            YIO_LOG_INFO("environment state: " << jsonToString(state));

            UNIT_ASSERT_EQUAL(state["devices"][0]["supported_features"][0].asString(), "feature_1");
            UNIT_ASSERT_EQUAL(state["devices"][0]["supported_features"][1].asString(), "feature_2");
        }

        Y_UNIT_TEST_F(checkUnknownSubscriptionType, EnvironmentStateFixture) {
            EnvironmentStateHolder holder("", std::make_shared<NullMetrica>());

            const auto group = createGroup(true, true, "remote_device_id", "remote_model");
            holder.updateTandemGroup(group);

            const auto state = holder.formatJson();
            YIO_LOG_INFO("environment state: " << jsonToString(state));
            UNIT_ASSERT(!state["devices"][0]["device_state"].isMember("subscription_state"));
        }

        Y_UNIT_TEST_F(checkDontHaveSubscription, EnvironmentStateFixture) {
            EnvironmentStateHolder holder("", std::make_shared<NullMetrica>());

            const auto group = createGroup(true, true, "remote_device_id", "remote_model");
            holder.updateTandemGroup(group);

            proto::QuasarMessage message;
            message.mutable_subscription_state()->set_subscription_info("{\"mode\":\"transaction\"}");
            message.mutable_subscription_state()->set_passport_uid("some_passport_uid");
            message.mutable_subscription_state()->set_last_update_time(std::time(nullptr));
            holder.updateLocalSubscriptionType(message.subscription_state());

            const auto state = holder.formatJson();
            YIO_LOG_INFO("environment state: " << jsonToString(state));

            UNIT_ASSERT_EQUAL(state["devices"][0]["device_state"]["subscription_state"]["subscription"].asString(), "none");
        }

        Y_UNIT_TEST_F(checkHaveSubscription, EnvironmentStateFixture) {
            EnvironmentStateHolder holder("", std::make_shared<NullMetrica>());

            const auto group = createGroup(true, true, "remote_device_id", "remote_model");
            holder.updateTandemGroup(group);

            proto::QuasarMessage message;
            message.mutable_subscription_state()->set_subscription_info("{\"mode\":\"subscription\",\"enabled\":true,\"ttl\":500}");
            message.mutable_subscription_state()->set_passport_uid("some_passport_uid");
            message.mutable_subscription_state()->set_last_update_time(std::time(nullptr));
            holder.updateLocalSubscriptionType(message.subscription_state());

            const auto state = holder.formatJson();
            YIO_LOG_INFO("environment state: " << jsonToString(state));

            UNIT_ASSERT_EQUAL(state["devices"][0]["device_state"]["subscription_state"]["subscription"].asString(), "yandex_subscription");
        }

        Y_UNIT_TEST_F(testTandemInfoInReportEnvironment, EnvironmentStateFixture) {
            const auto telemtry = std::make_shared<MockTelemetry>();
            EnvironmentStateHolder holder("", telemtry);

            {
                EXPECT_CALL(*telemtry, putAppEnvironmentValue(EnvironmentStateHolder::TANDEM_DEVICE_CONNECTED_KEY, "1"));
                EXPECT_CALL(*telemtry, putAppEnvironmentValue(EnvironmentStateHolder::TANDEM_DEVICE_ID_KEY, "remote_leader"));

                quasar::proto::DeviceGroupState groupState;
                groupState.set_local_role(quasar::proto::DeviceGroupState_Role_FOLLOWER);
                groupState.mutable_leader()->set_device_id("remote_leader");
                groupState.mutable_leader()->set_connection_state(proto::DeviceGroupState::CONNECTED);
                holder.updateTandemGroup(groupState);
            }

            {
                EXPECT_CALL(*telemtry, putAppEnvironmentValue(EnvironmentStateHolder::TANDEM_DEVICE_CONNECTED_KEY, "0"));
                EXPECT_CALL(*telemtry, putAppEnvironmentValue(EnvironmentStateHolder::TANDEM_DEVICE_ID_KEY, "remote_leader"));

                quasar::proto::DeviceGroupState groupState;
                groupState.set_local_role(quasar::proto::DeviceGroupState_Role_FOLLOWER);
                groupState.mutable_leader()->set_device_id("remote_leader");
                groupState.mutable_leader()->set_connection_state(proto::DeviceGroupState::NONE);
                holder.updateTandemGroup(groupState);
            }

            {
                EXPECT_CALL(*telemtry, deleteAppEnvironmentValue(EnvironmentStateHolder::TANDEM_DEVICE_CONNECTED_KEY));
                EXPECT_CALL(*telemtry, deleteAppEnvironmentValue(EnvironmentStateHolder::TANDEM_DEVICE_ID_KEY));

                quasar::proto::DeviceGroupState groupState;
                groupState.set_local_role(quasar::proto::DeviceGroupState_Role_STAND_ALONE);
                holder.updateTandemGroup(groupState);
            }

            {
                EXPECT_CALL(*telemtry, putAppEnvironmentValue(EnvironmentStateHolder::TANDEM_DEVICE_CONNECTED_KEY, "1"));
                EXPECT_CALL(*telemtry, putAppEnvironmentValue(EnvironmentStateHolder::TANDEM_DEVICE_ID_KEY, "remote_follower"));

                quasar::proto::DeviceGroupState groupState;
                groupState.set_local_role(quasar::proto::DeviceGroupState_Role_LEADER);
                groupState.mutable_follower()->set_device_id("remote_follower");
                groupState.mutable_follower()->set_connection_state(proto::DeviceGroupState::CONNECTED);
                holder.updateTandemGroup(groupState);
            }
        }
    }
} // namespace
