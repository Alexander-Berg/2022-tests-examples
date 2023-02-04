#include <yandex_io/services/aliced/capabilities/stereo_pair_capability/stereo_pair_capability.h>
#include <yandex_io/services/aliced/capabilities/stereo_pair_capability/stereo_pair_directives.h>

#include <yandex_io/interfaces/stereo_pair/mock/stereo_pair_provider.h>

#include <library/cpp/testing/gmock_in_unittest/gmock.h>
#include <library/cpp/testing/unittest/registar.h>

using namespace quasar;
using namespace testing;
using namespace YandexIO;

namespace {

    Y_UNIT_TEST_SUITE(StereoPairCapability) {

        Y_UNIT_TEST(testHandleDirective) {
            std::shared_ptr<mock::StereoPairProvider> mockStereoPairProvider = std::make_shared<mock::StereoPairProvider>();
            StereoPairCapability cap(mockStereoPairProvider);
            EXPECT_CALL(*mockStereoPairProvider, speakNotReadyNotification(_)).WillRepeatedly(Invoke([&](IStereoPairProvider::NotReadyReason /*reason*/) {
                UNIT_ASSERT(true);
            }));

            cap.handleDirective(YandexIO::Directive::createLocalAction(StereoPairDirectives::PLAYER_NOT_READY_NOTIFICATION, Json::Value()));
        }

    }

} // namespace
