#include <yandex_io/interfaces/updates/connector/updates_provider.h>

#include <yandex_io/libs/base/named_callback_queue.h>
#include <yandex_io/libs/ipc/mock/simple_connector.h>
#include <yandex_io/libs/logging/logging.h>
#include <yandex_io/protos/quasar_proto.pb.h>
#include <yandex_io/tests/testlib/test_utils.h>

#include <library/cpp/testing/unittest/registar.h>

using namespace quasar;
using namespace quasar::TestUtils;

Y_UNIT_TEST_SUITE(UpdatesProvider) {

    Y_UNIT_TEST(testUdatesState)
    {
        auto updatedConnector = std::make_shared<ipc::mock::SimpleConnector>(nullptr);
        auto callbackQueue = std::make_shared<NamedCallbackQueue>("testUpdatesProvider");
        UpdatesProvider updatesProvider(updatedConnector, callbackQueue);

        auto updatesState = updatesProvider.updatesState().value();

        UNIT_ASSERT(updatesState);
        UNIT_ASSERT_VALUES_EQUAL((int)updatesState->progress, (int)UpdatesState2::Progress::UNDEFINED);
        UNIT_ASSERT_VALUES_EQUAL((int)updatesState->critical, (int)UpdatesState2::Critical::UNDEFINED);
        UNIT_ASSERT_VALUES_EQUAL(updatesState->readyToApplyUpdateFlag, false);
    }

    Y_UNIT_TEST(testCritAndNoCrit)
    {
        auto updatedConnector = std::make_shared<ipc::mock::SimpleConnector>(nullptr);
        auto callbackQueue = std::make_shared<NamedCallbackQueue>("testUpdatesProvider");
        UpdatesProvider updatesProvider(updatedConnector, callbackQueue);

        proto::QuasarMessage message;
        message.mutable_no_critical_update_found();
        updatedConnector->pushMessage(message);
        flushCallbackQueue(callbackQueue);

        auto updatesState = updatesProvider.updatesState().value();
        UNIT_ASSERT_VALUES_EQUAL((int)updatesState->progress, (int)UpdatesState2::Progress::NONE);
        UNIT_ASSERT_VALUES_EQUAL((int)updatesState->critical, (int)UpdatesState2::Critical::NO);
        UNIT_ASSERT_VALUES_EQUAL(updatesState->readyToApplyUpdateFlag, false);

        message.Clear();
        message.mutable_start_critical_update();
        updatedConnector->pushMessage(message);
        flushCallbackQueue(callbackQueue);

        updatesState = updatesProvider.updatesState().value();
        UNIT_ASSERT_VALUES_EQUAL((int)updatesState->progress, (int)UpdatesState2::Progress::NONE);
        UNIT_ASSERT_VALUES_EQUAL((int)updatesState->critical, (int)UpdatesState2::Critical::YES);
        UNIT_ASSERT_VALUES_EQUAL(updatesState->readyToApplyUpdateFlag, false);

        message.Clear();
        message.mutable_ready_to_apply_update();
        updatedConnector->pushMessage(message);
        flushCallbackQueue(callbackQueue);
        updatesState = updatesProvider.updatesState().value();
        UNIT_ASSERT_VALUES_EQUAL((int)updatesState->progress, (int)UpdatesState2::Progress::NONE);
        UNIT_ASSERT_VALUES_EQUAL((int)updatesState->critical, (int)UpdatesState2::Critical::YES);
        UNIT_ASSERT_VALUES_EQUAL(updatesState->readyToApplyUpdateFlag, true);
    }

    Y_UNIT_TEST(testReadyApplyUpdateSignal)
    {
        auto updatedConnector = std::make_shared<ipc::mock::SimpleConnector>(nullptr);
        auto callbackQueue = std::make_shared<NamedCallbackQueue>("testUpdatesProvider");
        UpdatesProvider updatesProvider(updatedConnector, callbackQueue);

        std::atomic<int> step = 0;
        updatesProvider.readyApplyUpdateSignal().connect(
            [&](bool flag) {
                YIO_LOG_INFO("readyApplyUpdateSignal flag = " << flag);
                if (step == 0) {
                    UNIT_ASSERT_VALUES_EQUAL(flag, false);
                } else if (step == 1) {
                    UNIT_ASSERT_VALUES_EQUAL(flag, true);
                } else if (step == 2) {
                    UNIT_ASSERT_VALUES_EQUAL(flag, false);
                } else {
                    UNIT_ASSERT(false);
                }
                step = step + 1;
            }, Lifetime::immortal);
        flushCallbackQueue(callbackQueue);
        UNIT_ASSERT_VALUES_EQUAL(step.load(), 1); // First signal will be processed immediately after connect
        UNIT_ASSERT_VALUES_EQUAL(updatesProvider.updatesState().value()->readyToApplyUpdateFlag, false);

        proto::QuasarMessage message;
        message.mutable_ready_to_apply_update();
        updatedConnector->pushMessage(message);
        flushCallbackQueue(callbackQueue);
        UNIT_ASSERT_VALUES_EQUAL(step.load(), 2);
        UNIT_ASSERT_VALUES_EQUAL(updatesProvider.updatesState().value()->readyToApplyUpdateFlag, true);

        message.Clear();
        message.mutable_update_state()->set_state(proto::UpdateState_State::UpdateState_State_NONE);
        updatedConnector->pushMessage(message);
        flushCallbackQueue(callbackQueue);
        UNIT_ASSERT_VALUES_EQUAL(step.load(), 3); // Clear readyToApplyUpdateFlag after UpdateState_State_NONE
        UNIT_ASSERT_VALUES_EQUAL(updatesProvider.updatesState().value()->readyToApplyUpdateFlag, false);
    }

    Y_UNIT_TEST(testDownloadProgress)
    {
        auto updatedConnector = std::make_shared<ipc::mock::SimpleConnector>(nullptr);
        auto callbackQueue = std::make_shared<NamedCallbackQueue>("testUpdatesProvider");
        UpdatesProvider updatesProvider(updatedConnector, callbackQueue);

        proto::QuasarMessage message;
        message.mutable_update_state()->set_state(proto::UpdateState_State::UpdateState_State_DOWNLOADING);
        message.mutable_update_state()->set_download_progress(50);
        message.mutable_update_state()->set_is_critical(true);
        updatedConnector->pushMessage(message);
        flushCallbackQueue(callbackQueue);

        auto updatesState = updatesProvider.updatesState().value();
        UNIT_ASSERT_VALUES_EQUAL((int)updatesState->progress, (int)UpdatesState2::Progress::DOWNLOADING);
        UNIT_ASSERT_VALUES_EQUAL((int)updatesState->critical, (int)UpdatesState2::Critical::YES);

        message.Clear();
        message.mutable_update_state()->set_state(proto::UpdateState_State::UpdateState_State_DOWNLOADING);
        message.mutable_update_state()->set_download_progress(50);
        message.mutable_update_state()->set_is_critical(false);
        updatedConnector->pushMessage(message);
        flushCallbackQueue(callbackQueue);

        updatesState = updatesProvider.updatesState().value();
        UNIT_ASSERT_VALUES_EQUAL((int)updatesState->progress, (int)UpdatesState2::Progress::DOWNLOADING);
        UNIT_ASSERT_VALUES_EQUAL((int)updatesState->critical, (int)UpdatesState2::Critical::NO);
    }

    Y_UNIT_TEST(testApplyingOta)
    {
        auto updatedConnector = std::make_shared<ipc::mock::SimpleConnector>(nullptr);
        auto callbackQueue = std::make_shared<NamedCallbackQueue>("testUpdatesProvider");
        UpdatesProvider updatesProvider(updatedConnector, callbackQueue);

        proto::QuasarMessage message;
        message.mutable_update_state()->set_state(proto::UpdateState_State::UpdateState_State_APPLYING);
        message.mutable_update_state()->set_is_critical(true);
        updatedConnector->pushMessage(message);
        flushCallbackQueue(callbackQueue);

        auto updatesState = updatesProvider.updatesState().value();
        UNIT_ASSERT_VALUES_EQUAL((int)updatesState->progress, (int)UpdatesState2::Progress::APPLYING);
        UNIT_ASSERT_VALUES_EQUAL((int)updatesState->critical, (int)UpdatesState2::Critical::YES);

        message.Clear();
        message.mutable_update_state()->set_state(proto::UpdateState_State::UpdateState_State_APPLYING);
        message.mutable_update_state()->set_is_critical(false);
        updatedConnector->pushMessage(message);
        flushCallbackQueue(callbackQueue);

        updatesState = updatesProvider.updatesState().value();
        UNIT_ASSERT_VALUES_EQUAL((int)updatesState->progress, (int)UpdatesState2::Progress::APPLYING);
        UNIT_ASSERT_VALUES_EQUAL((int)updatesState->critical, (int)UpdatesState2::Critical::NO);
    }

    Y_UNIT_TEST(testCheckUpdates)
    {
        bool received = false;
        auto callbackQueue = std::make_shared<NamedCallbackQueue>("testUpdatesProvider");
        auto updatedConnector = std::make_shared<ipc::mock::SimpleConnector>(nullptr);
        updatedConnector->setSendMessageMethod(
            [&](const auto& msg) {
                if (!received) {
                    received = true;
                    UNIT_ASSERT_VALUES_EQUAL(msg->has_check_updates(), true);
                } else {
                    UNIT_ASSERT(false);
                }
                return true;
            });
        UpdatesProvider updatesProvider(updatedConnector, callbackQueue);
        updatesProvider.checkUpdates();
        flushCallbackQueue(callbackQueue);

        UNIT_ASSERT_VALUES_EQUAL(received, true);
    }

    Y_UNIT_TEST(testConfirmUpdateApply)
    {
        bool received = false;
        auto callbackQueue = std::make_shared<NamedCallbackQueue>("testUpdatesProvider");
        auto updatedConnector = std::make_shared<ipc::mock::SimpleConnector>(nullptr);
        updatedConnector->setSendMessageMethod(
            [&](const auto& msg) {
                if (!received) {
                    received = true;
                    UNIT_ASSERT_VALUES_EQUAL(msg->has_confirm_update_apply(), true);
                } else {
                    UNIT_ASSERT(false);
                }
                return true;
            });
        UpdatesProvider updatesProvider(updatedConnector, callbackQueue);
        updatesProvider.confirmUpdateApply();
        flushCallbackQueue(callbackQueue);

        UNIT_ASSERT_VALUES_EQUAL(received, true);
    }

    Y_UNIT_TEST(testPostponeUpdateApply)
    {
        bool received = false;
        auto callbackQueue = std::make_shared<NamedCallbackQueue>("testUpdatesProvider");
        auto updatedConnector = std::make_shared<ipc::mock::SimpleConnector>(nullptr);
        updatedConnector->setSendMessageMethod(
            [&](const auto& msg) {
                if (!received) {
                    received = true;
                    UNIT_ASSERT_VALUES_EQUAL(msg->has_postpone_update_apply(), true);
                } else {
                    UNIT_ASSERT(false);
                }
                return true;
            });
        UpdatesProvider updatesProvider(updatedConnector, callbackQueue);
        updatesProvider.postponeUpdateApply();
        flushCallbackQueue(callbackQueue);

        UNIT_ASSERT_VALUES_EQUAL(received, true);
    }

    Y_UNIT_TEST(testWaitUpdateState)
    {
        std::atomic<UpdatesState2::Critical> critical = UpdatesState2::Critical::UNDEFINED;
        auto callbackQueue = std::make_shared<NamedCallbackQueue>("testUpdatesProvider");
        auto updatedConnector = std::make_shared<ipc::mock::SimpleConnector>(nullptr);
        UpdatesProvider updatesProvider(updatedConnector, callbackQueue);
        updatedConnector->setSendMessageMethod(
            [&](const auto& /*msg*/) {
                UNIT_ASSERT("UpdatesProvider may send any message to updated");
                return true;
            });

        // Setup initial state
        proto::QuasarMessage message;
        message.mutable_no_critical_update_found();
        updatedConnector->pushMessage(message);
        flushCallbackQueue(callbackQueue);

        // We should wait for any new events
        std::atomic<int> callbackCounter1{0};
        updatesProvider.waitUpdateState(
            [&](auto c) {
                UNIT_ASSERT(callbackQueue->isWorkingThread());
                UNIT_ASSERT(c != UpdatesState2::Critical::UNDEFINED);
                critical = c;
                ++callbackCounter1;
            }, std::chrono::minutes{10});

        // Be sure that callback queue is empty
        flushCallbackQueue(callbackQueue);

        // No new updated events no actions
        UNIT_ASSERT_VALUES_EQUAL(callbackCounter1.load(), 0);
        UNIT_ASSERT_VALUES_EQUAL((int)critical.load(), (int)UpdatesState2::Critical::UNDEFINED);

        // Send the same event
        updatedConnector->pushMessage(message);
        waitUntil([&] { return critical.load() != UpdatesState2::Critical::UNDEFINED; });
        UNIT_ASSERT_VALUES_EQUAL(callbackCounter1.load(), 1);
        UNIT_ASSERT_VALUES_EQUAL((int)critical.load(), (int)UpdatesState2::Critical::NO);

        // Reset state
        critical = UpdatesState2::Critical::UNDEFINED;
        flushCallbackQueue(callbackQueue);

        std::atomic<int> callbackCounter2{0};
        updatesProvider.waitUpdateState(
            [&](auto c) {
                UNIT_ASSERT(callbackQueue->isWorkingThread());
                critical = c;
                ++callbackCounter2;
            }, std::chrono::minutes{10});

        // No new updated events no actions
        UNIT_ASSERT_VALUES_EQUAL(callbackCounter1.load(), 1); // <-- Old callback will not fire
        UNIT_ASSERT_VALUES_EQUAL(callbackCounter2.load(), 0);
        UNIT_ASSERT_VALUES_EQUAL((int)critical.load(), (int)UpdatesState2::Critical::UNDEFINED);

        updatedConnector->pushMessage(message);
        waitUntil([&] { return critical.load() != UpdatesState2::Critical::UNDEFINED; });
        UNIT_ASSERT_VALUES_EQUAL(callbackCounter1.load(), 1); // <-- Old callback does not fire
        UNIT_ASSERT_VALUES_EQUAL(callbackCounter2.load(), 1); // <-- New callback does fire
        UNIT_ASSERT_VALUES_EQUAL((int)critical.load(), (int)UpdatesState2::Critical::NO);
    }

    Y_UNIT_TEST(testWaitUpdateStateTimeout)
    {
        auto callbackQueue = std::make_shared<NamedCallbackQueue>("testUpdatesProvider");
        auto updatedConnector = std::make_shared<ipc::mock::SimpleConnector>(nullptr);
        UpdatesProvider updatesProvider(updatedConnector, callbackQueue);
        updatedConnector->setSendMessageMethod(
            [&](const auto& /*msg*/) {
                UNIT_ASSERT("UpdatesProvider may send any message to updated");
                return true;
            });

        // Setup initial state
        proto::QuasarMessage message;
        message.mutable_no_critical_update_found();
        updatedConnector->pushMessage(message);
        flushCallbackQueue(callbackQueue);

        // We should wait for any new events
        std::atomic<int> callbackCounter{0};
        updatesProvider.waitUpdateState(
            [&](auto c) {
                UNIT_ASSERT(callbackQueue->isWorkingThread());
                UNIT_ASSERT(c == UpdatesState2::Critical::UNDEFINED); // Timeout!
                ++callbackCounter;
            }, std::chrono::minutes{0});

        // Be sure about timeout
        std::this_thread::sleep_for(std::chrono::seconds{1});

        // Be sure that callback queue is empty
        flushCallbackQueue(callbackQueue);

        // No new updated events no actions
        UNIT_ASSERT_VALUES_EQUAL(callbackCounter.load(), 1); // Timeout received

        // Send the same event
        updatedConnector->pushMessage(message);
        flushCallbackQueue(callbackQueue);
        UNIT_ASSERT_VALUES_EQUAL(callbackCounter.load(), 1); // Oneshot callback expired
    }

} // Y_UNIT_TEST_SUITE(UpdatesProvider)
