#include <yandex_io/libs/json_utils/json_utils.h>
#include <yandex_io/modules/equalizer_controller/equalizer_controller.h>
#include <yandex_io/modules/equalizer_controller/tests/mocks/mock_equalizer_dispatcher.h>
#include <yandex_io/tests/testlib/test_callback_queue.h>
#include <yandex_io/tests/testlib/unittest_helper/unit_test_fixture.h>

#include <library/cpp/testing/unittest/registar.h>

using namespace YandexIO;
using namespace quasar;

class EqualizerControllerTestFixture: public QuasarUnitTestFixtureWithoutIpc {
public:
    void SetUp(NUnitTest::TTestContext& context) override {
        QuasarUnitTestFixtureWithoutIpc::SetUp(context);

        auto dispatcherFactory = std::make_unique<MockEqualizerDispatcherFactory>();
        dispatcherFactory_ = dispatcherFactory.get();
        controller_ = std::make_shared<EqualizerController>(
            std::make_unique<TestCallbackQueue>(),
            Json::Value{},
            std::weak_ptr<SDKInterface>{},
            getDeviceForTests()->telemetry(),
            std::move(dispatcherFactory));
    }

protected:
    std::shared_ptr<EqualizerController> controller_;
    MockEqualizerDispatcherFactory* dispatcherFactory_;
};

Y_UNIT_TEST_SUITE_F(EqualizerControllerTest, EqualizerControllerTestFixture) {
    Y_UNIT_TEST(testSetUserEqualizer) {
        Json::Value eqSystemConfig;
        eqSystemConfig["type"] = MockEqualizerDispatcherFactory::ADJUSTABLE_BANDS_EQUALIZER_TYPE;
        controller_->onSystemConfig("equalizer", quasar::jsonToString(eqSystemConfig));

        auto dispatcher = dispatcherFactory_->adjustableBandsDispatcher_;

        // expect that equalizer will be set after user enables it
        EXPECT_CALL(*dispatcher, setUserConfig).Times(1);

        Json::Value eqDeviceConfig;
        eqDeviceConfig["enabled"] = true;
        controller_->onDeviceConfig("equalizer", quasar::jsonToString(eqDeviceConfig));
    }

    Y_UNIT_TEST(testSetMediaCorrectionEqualizer) {
        Json::Value eqSystemConfig;
        eqSystemConfig["type"] = MockEqualizerDispatcherFactory::FIXED_BANDS_EQUALIZER_TYPE;
        controller_->onSystemConfig("equalizer", quasar::jsonToString(eqSystemConfig));

        auto dispatcher = dispatcherFactory_->fixedBandsDispatcher_;

        // expect that equalizer will be set after media correction mode enabled
        EXPECT_CALL(*dispatcher, setUserConfig).Times(1);

        Json::Value eqDeviceConfig;
        eqDeviceConfig["mediaCorrectionEnabled"] = true;
        controller_->onDeviceConfig("equalizer", quasar::jsonToString(eqDeviceConfig));

        const int bandCount = dispatcher->getFixedBandsConfiguration().size();

        // expect that equalizer with corresponding band count will be set after directive
        EXPECT_CALL(*dispatcher, setUserConfig(testing::Field(&EqualizerConfig::bands, testing::SizeIs(bandCount)))).Times(1);

        Json::Value directivePayload;
        auto& gainsJson = directivePayload["gains"];
        for (int i = 0; i < bandCount; ++i) {
            gainsJson.append(Json::Value(0.));
        }
        const auto directive = std::make_shared<YandexIO::Directive>(
            YandexIO::Directive::Data(EqualizerController::FIXED_BANDS_DIRECTIVE, "", directivePayload));
        controller_->handleDirective(directive);
    }

    Y_UNIT_TEST(testMediaCorrectionDefaults) {
        Json::Value eqSystemConfig;
        eqSystemConfig["type"] = MockEqualizerDispatcherFactory::FIXED_BANDS_EQUALIZER_TYPE;
        eqSystemConfig["mediaCorrectionDefault"] = true;
        controller_->onSystemConfig("equalizer", quasar::jsonToString(eqSystemConfig));

        auto dispatcher = dispatcherFactory_->fixedBandsDispatcher_;

        const int bandCount = dispatcher->getFixedBandsConfiguration().size();

        // expect that equalizer with corresponding band count will be set after directive
        // despite the absence media correction flag in device config
        EXPECT_CALL(*dispatcher, setUserConfig(testing::Field(&EqualizerConfig::bands, testing::SizeIs(bandCount)))).Times(1);

        Json::Value directivePayload;
        auto& gainsJson = directivePayload["gains"];
        for (int i = 0; i < bandCount; ++i) {
            gainsJson.append(Json::Value(0.));
        }
        const auto directive = std::make_shared<YandexIO::Directive>(
            YandexIO::Directive::Data(EqualizerController::FIXED_BANDS_DIRECTIVE, "", directivePayload));
        controller_->handleDirective(directive);

        testing::Mock::VerifyAndClearExpectations(dispatcher);

        // reset media correction default & check that equalizer wll not set after directive
        eqSystemConfig["mediaCorrectionDefault"] = false;
        controller_->onSystemConfig("equalizer", quasar::jsonToString(eqSystemConfig));

        EXPECT_CALL(*dispatcher, setUserConfig).Times(0);
        controller_->handleDirective(directive);
    }
}
