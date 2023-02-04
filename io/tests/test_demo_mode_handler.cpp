#include "demo_mode_handler.h"

#include <yandex_io/modules/demo_mode/demo_provider_interface/null/null_demo_provider.h>

#include <yandex_io/capabilities/file_player/interfaces/mocks/mock_i_file_player_capability.h>

#include <yandex_io/libs/base/utils.h>
#include <yandex_io/tests/testlib/null_sdk/null_sdk_interface.h>
#include <yandex_io/tests/testlib/unittest_helper/unit_test_fixture.h>

#include <library/cpp/testing/gmock_in_unittest/gmock.h>
#include <library/cpp/testing/unittest/registar.h>

#include <util/folder/tempdir.h>

#include <fstream>
#include <future>

using testing::_;
using namespace testing;
using namespace quasar;
using namespace YandexIO;

namespace {

    class TestDemoProvider: public NullDemoProvider {
    public:
        std::promise<void> p;

        std::deque<DemoItem> getNextDemoItems() override {
            p.set_value();
            return {DemoItem("some_cache_path", "some_url")};
        }
    };

    class SDKMock: public NullSDKInterface {
    public:
        MOCK_METHOD(void, toggleSetupMode, (), (override));
        MOCK_METHOD(void, blockVoiceAssistant, (const std::string&, const std::optional<std::string>&), (override));
        MOCK_METHOD(void, unblockVoiceAssistant, (const std::string&), (override));
        MOCK_METHOD(std::shared_ptr<YandexIO::IFilePlayerCapability>, getFilePlayerCapability, (), (const, override));
    };

    class Fixture: public QuasarUnitTestFixtureWithoutIpc {
    public:
        std::shared_ptr<MockIFilePlayerCapability> filePlayerCapabilityMock;
        std::shared_ptr<TestDemoProvider> provider;
        std::shared_ptr<SDKMock> sdk;

        TTempDir tmpDir;
        std::string demoModePath;
        Fixture() {
            provider = std::make_shared<TestDemoProvider>();
            filePlayerCapabilityMock = std::make_shared<MockIFilePlayerCapability>();
            sdk = std::make_shared<SDKMock>();
            ON_CALL(*sdk, getFilePlayerCapability()).WillByDefault(Return(filePlayerCapabilityMock));
            demoModePath = tmpDir.Name() + "/demo_mode";
        }
    };

} // namespace

Y_UNIT_TEST_SUITE_F(DemoModeHandlerTest, Fixture) {
    Y_UNIT_TEST(testToggleDemoMode) {
        {
            testing::Sequence seq1;
            testing::Sequence seq2;
            // unblock in constructor
            EXPECT_CALL(*sdk, unblockVoiceAssistant("demo_mode")).InSequence(seq1);
            // first toggle
            EXPECT_CALL(*sdk, blockVoiceAssistant("demo_mode", _)).InSequence(seq1);
            EXPECT_CALL(*sdk, toggleSetupMode()).InSequence(seq1);
            // second toggle
            EXPECT_CALL(*sdk, unblockVoiceAssistant("demo_mode")).InSequence(seq1);
            EXPECT_CALL(*sdk, toggleSetupMode()).InSequence(seq1);
            // desctructor
            EXPECT_CALL(*filePlayerCapabilityMock, stopSoundFile(_)).InSequence(seq2);
        }
        std::shared_ptr<DemoModeHandler> demoModeHandler = std::make_shared<DemoModeHandler>(provider, sdk, demoModePath);
        std::shared_ptr<SoundCommandObserver> demoModeSoundObserver = demoModeHandler;
        demoModeSoundObserver->onCommand("demo_mode");
        UNIT_ASSERT(demoModeHandler->isDemoMode());
        UNIT_ASSERT(fileExists(demoModePath));
        demoModeSoundObserver->onCommand("demo_mode");
        UNIT_ASSERT(!demoModeHandler->isDemoMode());
        UNIT_ASSERT(!fileExists(demoModePath));
    }

    Y_UNIT_TEST(testMaindCrash) {
        {
            std::ofstream{demoModePath};
        }
        {
            testing::InSequence seq;
            EXPECT_CALL(*sdk, unblockVoiceAssistant("demo_mode"));
            EXPECT_CALL(*sdk, blockVoiceAssistant("demo_mode", _));
        }
        std::shared_ptr<DemoModeHandler> demoModeHandler = std::make_shared<DemoModeHandler>(provider, sdk, demoModePath);
        UNIT_ASSERT(demoModeHandler->isDemoMode());
    }

    Y_UNIT_TEST(testPlayPause) {
        std::shared_ptr<DemoModeHandler> demoModeHandler = std::make_shared<DemoModeHandler>(provider, sdk, demoModePath);
        std::promise<void> p;
        auto f = p.get_future();
        provider->p = std::move(p);
        {
            testing::InSequence seq;
            EXPECT_CALL(*filePlayerCapabilityMock, playSoundFile("some_cache_path/some_url",
                                                                 std::optional<quasar::proto::AudioChannel>(quasar::proto::AudioChannel::DIALOG_CHANNEL), _, _));
            EXPECT_CALL(*filePlayerCapabilityMock, stopSoundFile("some_cache_path/some_url"));
            EXPECT_CALL(*filePlayerCapabilityMock, playSoundFile("some_cache_path/some_url",
                                                                 std::optional<quasar::proto::AudioChannel>(quasar::proto::AudioChannel::DIALOG_CHANNEL), _, _));
            EXPECT_CALL(*filePlayerCapabilityMock, stopSoundFile("some_cache_path/some_url"));
            // call in destructor
            EXPECT_CALL(*filePlayerCapabilityMock, stopSoundFile(_));
        }
        demoModeHandler->play();
        f.wait();
        demoModeHandler->pause();
        p = std::promise<void>();
        f = p.get_future();
        provider->p = std::move(p);
        demoModeHandler->togglePlay();
        f.wait();
        demoModeHandler->togglePlay();
    }
}
