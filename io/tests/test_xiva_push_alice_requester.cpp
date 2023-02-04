#include <yandex_io/modules/xiva_alice_request/xiva_push_alice_requester.h>

#include <yandex_io/libs/logging/logging.h>
#include <yandex_io/libs/json_utils/json_utils.h>

#include <yandex_io/capabilities/alice/interfaces/mocks/mock_i_alice_capability.h>
#include <yandex_io/tests/testlib/unittest_helper/unit_test_fixture.h>
#include <yandex_io/tests/testlib/null_sdk/null_sdk_interface.h>

using namespace testing;

namespace {
    class SDKMock: public YandexIO::NullSDKInterface {
    public:
        MOCK_METHOD(void, addPushNotificationObserver, (std::weak_ptr<YandexIO::PushNotificationObserver>), (override));
        MOCK_METHOD(std::shared_ptr<YandexIO::IAliceCapability>, getAliceCapability, (), (const, override));
    };

    MATCHER_P(VerifyServerAction, eventJson, "description") {
        const std::shared_ptr<YandexIO::VinsRequest>& request = arg;
        // should contain provided json with server action
        if (request->getEvent() != eventJson) {
            return false;
        }
        if (!request->getIsEnqueued()) {
            return false;
        }
        YIO_LOG_INFO("Checked server action: " << request->toString());
        return true;
    }

    MATCHER(VerifyReminder, "description") {
        const std::shared_ptr<YandexIO::VinsRequest>& request = arg;
        if (!request->getIsReminder()) {
            return false;
        }
        if (!request->getIsEnqueued()) {
            return false;
        }

        YIO_LOG_INFO("Checked reminder action: " << request->toString());
        return true;
    }

    MATCHER_P(VerifyTextAction, text, "description") {
        const std::shared_ptr<YandexIO::VinsRequest>& request = arg;
        Json::Value target;
        target["type"] = "text_input";
        target["text"] = text;
        // should contain text_input event with proper text
        if (request->getEvent() != target) {
            return false;
        }
        if (!request->getIsEnqueued()) {
            return false;
        }
        YIO_LOG_INFO("Checked text action: " << request->toString());
        return true;
    }
} // namespace

Y_UNIT_TEST_SUITE_F(AliceCapabilityTest, QuasarUnitTestFixture) {
    Y_UNIT_TEST(testXivaPush) {

        SDKMock sdk;
        const auto aliceCapapility = std::make_shared<YandexIO::MockIAliceCapability>();

        EXPECT_CALL(sdk, getAliceCapability()).WillOnce(Return(aliceCapapility));
        EXPECT_CALL(sdk, addPushNotificationObserver(_)).Times(1);
        const auto requester = YandexIO::XivaPushAliceRequester::install(sdk);

        {
            Json::Value serverActionPayload;
            serverActionPayload["some"] = "payload";
            serverActionPayload["int"] = 8;
            serverActionPayload["name"] = "some_server_action";

            EXPECT_CALL(*aliceCapapility, startRequest(VerifyServerAction(serverActionPayload), _)).Times(1);
            requester->onPushNotification("server_action", quasar::jsonToString(serverActionPayload));
        }

        {
            const std::string reminder = "{\"reminder\":{\"uri\":\"dialog://?directives=%5B%7B%22name%22%3A%22update_form%22%2C%22payload%22%3A%7B%22form_update%22%3A%7B%22name%22%3A%22personal_assistant.scenarios.alarm_reminder%22%2C%22slots%22%3A%5B%7B%22name%22%3A%22push_reminder_id%22%2C%22optional%22%3Atrue%2C%22type%22%3A%22string%22%2C%22value%22%3A%222e5d48f9-ede9db72-e6f96ca7-10092218%22%7D%5D%7D%2C%22resubmit%22%3Atrue%7D%2C%22type%22%3A%22server_action%22%7D%5D\"}}";
            EXPECT_CALL(*aliceCapapility, startRequest(VerifyReminder(), _)).Times(1);
            requester->onPushNotification("reminder", reminder);
        }

        {
            Json::Value textActionPayload;
            textActionPayload["to_alice"][0] = "привет";

            EXPECT_CALL(*aliceCapapility, startRequest(VerifyTextAction("привет"), _)).Times(1);
            requester->onPushNotification("text_action", quasar::jsonToString(textActionPayload));
        }

    }
}
