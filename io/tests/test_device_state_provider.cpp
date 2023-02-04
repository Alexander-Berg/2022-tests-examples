#include <yandex_io/interfaces/device_state/connector/device_state_provider.h>

#include <yandex_io/libs/ipc/mock/simple_connector.h>
#include <yandex_io/protos/quasar_proto.pb.h>
#include <yandex_io/tests/testlib/test_utils.h>

#include <library/cpp/testing/unittest/registar.h>

using namespace quasar;
using namespace quasar::TestUtils;

namespace {

    proto::QuasarMessage messageNone =
        [] {
            proto::QuasarMessage message;
            message.mutable_update_state()->set_state(proto::UpdateState::NONE);
            return message;
        }();

    proto::QuasarMessage messageDownloading =
        [] {
            proto::QuasarMessage message;
            message.mutable_update_state()->set_state(proto::UpdateState::DOWNLOADING);
            return message;
        }();

    proto::QuasarMessage messageDownloadingCritical =
        [] {
            proto::QuasarMessage message;
            message.mutable_update_state()->set_state(proto::UpdateState::DOWNLOADING);
            message.mutable_update_state()->set_is_critical(true);
            return message;
        }();

    proto::QuasarMessage messageCsConfiguring =
        [] {
            proto::QuasarMessage message;
            message.set_configuration_state(proto::ConfigurationState::CONFIGURING);
            return message;
        }();

    proto::QuasarMessage messageCsConfigured =
        [] {
            proto::QuasarMessage message;
            message.set_configuration_state(proto::ConfigurationState::CONFIGURED);
            return message;
        }();

    proto::QuasarMessage messageMix1 =
        [] {
            proto::QuasarMessage message;
            message.set_configuration_state(proto::ConfigurationState::CONFIGURED);
            message.mutable_update_state()->set_state(proto::UpdateState::DOWNLOADING);
            message.mutable_update_state()->set_is_critical(true);
            return message;
        }();

    proto::QuasarMessage messageMix2 =
        [] {
            proto::QuasarMessage message;
            message.set_configuration_state(proto::ConfigurationState::CONFIGURING);
            message.mutable_update_state()->set_state(proto::UpdateState::NONE);
            return message;
        }();

} // namespace

Y_UNIT_TEST_SUITE(DeviceStateProvider)
{

    Y_UNIT_TEST(testDefault)
    {
        auto firstrundConnector = std::make_shared<ipc::mock::SimpleConnector>(nullptr);
        auto networkdConnector = std::make_shared<ipc::mock::SimpleConnector>(nullptr);
        auto updatedConnector = std::make_shared<ipc::mock::SimpleConnector>(nullptr);

        DeviceStateProvider deviceStateProvider(firstrundConnector, networkdConnector, updatedConnector);
        auto deviceState = deviceStateProvider.deviceState().value();
        UNIT_ASSERT(deviceState != nullptr);
        UNIT_ASSERT_VALUES_EQUAL((int)deviceState->configuration, (int)DeviceState::Configuration::UNDEFINED);
        UNIT_ASSERT_VALUES_EQUAL((int)deviceState->update, (int)DeviceState::Update::UNDEFINED);
    }

    Y_UNIT_TEST(testNetwork)
    {
        auto firstrundConnector = std::make_shared<ipc::mock::SimpleConnector>(nullptr);
        auto networkdConnector = std::make_shared<ipc::mock::SimpleConnector>(nullptr);
        auto updatedConnector = std::make_shared<ipc::mock::SimpleConnector>(nullptr);

        DeviceStateProvider deviceStateProvider(firstrundConnector, networkdConnector, updatedConnector);
        proto::QuasarMessage message;
        message.mutable_network_status()->set_type(proto::CONNECTION_TYPE_WIFI);
        message.mutable_network_status()->set_status(proto::NetworkStatus::CONNECTED_NO_INTERNET);
        networkdConnector->pushMessage(message);

        auto deviceState = deviceStateProvider.deviceState().value();
        UNIT_ASSERT(deviceState != nullptr);
        UNIT_ASSERT_VALUES_EQUAL((int)deviceState->networkStatus.type, (int)NetworkStatus::Type::WIFI);
        UNIT_ASSERT_VALUES_EQUAL((int)deviceState->networkStatus.status, (int)NetworkStatus::Status::CONNECTED_NO_INTERNET);
    }

    Y_UNIT_TEST(testUpdate)
    {
        auto firstrundConnector = std::make_shared<ipc::mock::SimpleConnector>(nullptr);
        auto networkdConnector = std::make_shared<ipc::mock::SimpleConnector>(nullptr);
        auto updatedConnector = std::make_shared<ipc::mock::SimpleConnector>(nullptr);

        DeviceStateProvider deviceStateProvider(firstrundConnector, networkdConnector, updatedConnector);

        int stage = 1;
        int dsCounter = 0;
        int updateCounter = 0;
        deviceStateProvider.deviceState().connect([&](auto /*state*/) { ++dsCounter; }, Lifetime::immortal);
        deviceStateProvider.configurationChangedSignal().connect(
            [&](auto... /*args*/) {
                if (stage != 1) {
                    UNIT_ASSERT(false);
                }
            }, Lifetime::immortal);
        deviceStateProvider.updateChangedSignal().connect(
            [&](auto ds) {
                ++updateCounter;
                if (stage == 1) {
                    UNIT_ASSERT_VALUES_EQUAL((int)ds->configuration, (int)DeviceState::Configuration::UNDEFINED);
                    UNIT_ASSERT_VALUES_EQUAL((int)ds->update, (int)DeviceState::Update::UNDEFINED);
                } else if (stage == 2) {
                    UNIT_ASSERT_VALUES_EQUAL((int)ds->configuration, (int)DeviceState::Configuration::UNDEFINED);
                    UNIT_ASSERT_VALUES_EQUAL((int)ds->update, (int)DeviceState::Update::NO_CRITICAL);
                    // } else if (state == 3) { // Never HIT this!
                } else if (stage == 4) {
                    UNIT_ASSERT_VALUES_EQUAL((int)ds->configuration, (int)DeviceState::Configuration::UNDEFINED);
                    UNIT_ASSERT_VALUES_EQUAL((int)ds->update, (int)DeviceState::Update::HAS_CRITICAL);
                } else if (stage == 5) {
                    UNIT_ASSERT_VALUES_EQUAL((int)ds->configuration, (int)DeviceState::Configuration::UNDEFINED);
                    UNIT_ASSERT_VALUES_EQUAL((int)ds->update, (int)DeviceState::Update::NO_CRITICAL);
                } else {
                    UNIT_ASSERT(false);
                }
            }, Lifetime::immortal);

        stage = 2;
        updatedConnector->pushMessage(messageNone);

        stage = 3;
        updatedConnector->pushMessage(messageDownloading); // Internal state dosn't changed, no any signal

        stage = 4;
        updatedConnector->pushMessage(messageDownloadingCritical);

        stage = 5;
        updatedConnector->pushMessage(messageNone);

        UNIT_ASSERT_VALUES_EQUAL(updateCounter, 4);
        UNIT_ASSERT_VALUES_EQUAL(dsCounter, updateCounter);
    }

    Y_UNIT_TEST(testConfiguration)
    {
        auto firstrundConnector = std::make_shared<ipc::mock::SimpleConnector>(nullptr);
        auto networkdConnector = std::make_shared<ipc::mock::SimpleConnector>(nullptr);
        auto updatedConnector = std::make_shared<ipc::mock::SimpleConnector>(nullptr);

        DeviceStateProvider deviceStateProvider(firstrundConnector, networkdConnector, updatedConnector);

        int stage = 1;
        int dsCounter = 0;
        int cfgCounter = 0;
        deviceStateProvider.deviceState().connect([&](auto /*state*/) { ++dsCounter; }, Lifetime::immortal);
        deviceStateProvider.updateChangedSignal().connect(
            [&](auto... /*args*/) {
                if (stage != 1) {
                    UNIT_ASSERT(false);
                }
            }, Lifetime::immortal); // Never enter
        deviceStateProvider.configurationChangedSignal().connect(
            [&](auto ds) {
                ++cfgCounter;
                if (stage == 1) {
                    UNIT_ASSERT_VALUES_EQUAL((int)ds->configuration, (int)DeviceState::Configuration::UNDEFINED);
                    UNIT_ASSERT_VALUES_EQUAL((int)ds->update, (int)DeviceState::Update::UNDEFINED);
                } else if (stage == 2) {
                    UNIT_ASSERT_VALUES_EQUAL((int)ds->configuration, (int)DeviceState::Configuration::CONFIGURING);
                    UNIT_ASSERT_VALUES_EQUAL((int)ds->update, (int)DeviceState::Update::UNDEFINED);
                } else if (stage == 3) {
                    UNIT_ASSERT_VALUES_EQUAL((int)ds->configuration, (int)DeviceState::Configuration::CONFIGURED);
                    UNIT_ASSERT_VALUES_EQUAL((int)ds->update, (int)DeviceState::Update::UNDEFINED);
                } else if (stage == 4) {
                    UNIT_ASSERT_VALUES_EQUAL((int)ds->configuration, (int)DeviceState::Configuration::CONFIGURING);
                    UNIT_ASSERT_VALUES_EQUAL((int)ds->update, (int)DeviceState::Update::UNDEFINED);
                } else {
                    UNIT_ASSERT(false);
                }
            }, Lifetime::immortal);

        stage = 2;
        firstrundConnector->pushMessage(messageCsConfiguring);

        stage = 3;
        firstrundConnector->pushMessage(messageCsConfigured);

        stage = 4;
        firstrundConnector->pushMessage(messageCsConfiguring);

        UNIT_ASSERT_VALUES_EQUAL(cfgCounter, 4);
        UNIT_ASSERT_VALUES_EQUAL(dsCounter, cfgCounter);
    }

    Y_UNIT_TEST(testMix)
    {
        auto firstrundConnector = std::make_shared<ipc::mock::SimpleConnector>(nullptr);
        auto networkdConnector = std::make_shared<ipc::mock::SimpleConnector>(nullptr);
        auto updatedConnector = std::make_shared<ipc::mock::SimpleConnector>(nullptr);

        DeviceStateProvider deviceStateProvider(firstrundConnector, networkdConnector, updatedConnector);

        int stage = 1;
        int dsCounter = 0;
        int cfgCounter = 0;
        int updateCounter = 0;
        deviceStateProvider.deviceState().connect(
            [&](auto /*state*/) {
                ++dsCounter;
                std::set<int> allowed_stage = {1, 2, 3, 5, 6};
                if (!allowed_stage.count(stage)) {
                    UNIT_ASSERT_VALUES_EQUAL(stage, 0);
                }
            }, Lifetime::immortal);
        deviceStateProvider.configurationChangedSignal().connect(
            [&](auto... /*args*/) {
                ++cfgCounter;
                std::set<int> allowed_stage = {1, 3, 6};
                if (!allowed_stage.count(stage)) {
                    UNIT_ASSERT_VALUES_EQUAL(stage, 0);
                }
            }, Lifetime::immortal);
        deviceStateProvider.updateChangedSignal().connect(
            [&](auto... /*args*/) {
                ++updateCounter;
                std::set<int> allowed_stage = {1, 2, 5, 6};
                if (!allowed_stage.count(stage)) {
                    UNIT_ASSERT_VALUES_EQUAL(stage, 0);
                }
            }, Lifetime::immortal);

        stage = 2;
        updatedConnector->pushMessage(messageNone);

        stage = 3;
        firstrundConnector->pushMessage(messageCsConfigured);

        stage = 4;
        updatedConnector->pushMessage(messageDownloading); // Update state doesn't changed! No hit

        stage = 5;
        firstrundConnector->pushMessage(messageMix1); // Update changed but Configuration will not

        stage = 6;
        updatedConnector->pushMessage(messageMix2);

        UNIT_ASSERT_VALUES_EQUAL(dsCounter, 5);
        UNIT_ASSERT_VALUES_EQUAL(cfgCounter, 3);
        UNIT_ASSERT_VALUES_EQUAL(updateCounter, 4);
    }

} // Y_UNIT_TEST_SUITE(DeviceStateProvider)
