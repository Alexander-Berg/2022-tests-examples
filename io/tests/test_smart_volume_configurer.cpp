#include <yandex_io/capabilities/alice/interfaces/i_alice_capability.h>
#include <yandex_io/capabilities/alice/interfaces/mocks/mock_i_alice_capability.h>

#include <yandex_io/libs/json_utils/json_utils.h>
#include <yandex_io/libs/telemetry/mock/mock_telemetry.h>

#include <yandex_io/modules/volume_manager/base/volume_manager.h>
#include <yandex_io/modules/smart_volume/smart_volume_configurer.h>

#include <yandex_io/tests/testlib/null_sdk/null_sdk_interface.h>
#include <yandex_io/tests/testlib/unittest_helper/unit_test_fixture.h>

#include <yandex_io/sdk/audio_source/i_audio_source_client.h>

#include <memory>

using namespace testing;
using namespace quasar;
using namespace YandexIO;

namespace {

    struct MockSDKInterface: public NullSDKInterface {
        MOCK_METHOD(std::shared_ptr<IAliceCapability>, getAliceCapability, (), (const, override));
        MOCK_METHOD(void, subscribeToDeviceConfig, (const std::string&), (override));
        MOCK_METHOD(void, subscribeToSystemConfig, (const std::string&), (override));
        MOCK_METHOD(void, addBackendConfigObserver, (std::weak_ptr<BackendConfigObserver>), (override));
    };

    struct MockAudioSourceClient: public IAudioSourceClient {
        MOCK_METHOD(void, subscribeToChannels, (RequestChannelType), ());
        MOCK_METHOD(void, unsubscribeFromChannels, (), ());
        MOCK_METHOD(void, start, (), ());
        MOCK_METHOD(void, addListener, (std::weak_ptr<Listener>));
    };

    struct MockVolumeManager: public VolumeManager {
        MockVolumeManager(std::shared_ptr<YandexIO::IDevice> device,
                          std::shared_ptr<quasar::ipc::IIpcFactory> ipcFactory,
                          std::shared_ptr<YandexIO::SDKInterface> sdk)
            : VolumeManager{device, ipcFactory, sdk}
        {
        }

        MOCK_METHOD(int, scaleFromAlice, (int), ());
        MOCK_METHOD(int, scaleToAlice, (int), ());
        MOCK_METHOD(int, minVolume, (), ());
        MOCK_METHOD(int, maxVolume, (), ());
        MOCK_METHOD(void, setVolumeImplementation, (int), ());
        MOCK_METHOD(int, volumeStep, (), ());
        MOCK_METHOD(int, initialVolume, (), ());
    };

    struct SmartVolumeFixture: public QuasarUnitTestFixture {
        void SetUp(NUnitTest::TTestContext& context) override {
            QuasarUnitTestFixture::SetUp(context);

            volumeManager = std::make_shared<NiceMock<MockVolumeManager>>(getDeviceForTests(), ipcFactoryForTests(), mockSDKInterface);

            EXPECT_CALL(*mockSDKInterface, subscribeToDeviceConfig("smart_volume"));
            EXPECT_CALL(*mockSDKInterface, subscribeToSystemConfig("smart_volume"));
            EXPECT_CALL(*mockSDKInterface, addBackendConfigObserver(_));

            smartVolumeConfigurer = SmartVolumeConfigurer::create(mockSDKInterface, volumeManager, mockAudioSourceClient);

            ON_CALL(*mockSDKInterface, getAliceCapability())
                .WillByDefault(Return(mockAliceCapability));
        }

    public:
        std::shared_ptr<MockSDKInterface> mockSDKInterface = std::make_shared<NiceMock<MockSDKInterface>>();
        std::shared_ptr<MockIAliceCapability> mockAliceCapability = std::make_shared<NiceMock<MockIAliceCapability>>();
        std::shared_ptr<MockAudioSourceClient> mockAudioSourceClient = std::make_shared<NiceMock<MockAudioSourceClient>>();
        std::shared_ptr<MockTelemetry> mockTelemetry = std::make_shared<NiceMock<MockTelemetry>>();
        std::shared_ptr<VolumeManager> volumeManager;
        std::shared_ptr<BackendConfigObserver> smartVolumeConfigurer;
    };

} // namespace

Y_UNIT_TEST_SUITE(SmartVolumeTest) {
    Y_UNIT_TEST_F(testCorrectConfig, SmartVolumeFixture) {
        Json::Value config;
        config["enabled"] = true;
        config["rmsToVolume"]["100"] = 150;

        EXPECT_CALL(*mockAliceCapability, addListener(_));

        smartVolumeConfigurer->onSystemConfig("smart_volume", jsonToString(config));

        config["enabled"] = false;

        EXPECT_CALL(*mockAliceCapability, removeListener(_));

        smartVolumeConfigurer->onSystemConfig("smart_volume", jsonToString(config));
    }

    Y_UNIT_TEST_F(testNoEnabled, SmartVolumeFixture) {
        EXPECT_CALL(*mockAliceCapability, addListener(_))
            .Times(0);

        smartVolumeConfigurer->onSystemConfig("smart_volume", jsonToString(Json::Value{}));
    }

    Y_UNIT_TEST_F(testNoRmsToVolume, SmartVolumeFixture) {
        Json::Value config;
        config["enabled"] = true;

        EXPECT_CALL(*mockAliceCapability, addListener(_))
            .Times(0);

        smartVolumeConfigurer->onSystemConfig("smart_volume", jsonToString(Json::Value{}));
    }
}
