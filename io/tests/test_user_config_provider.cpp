#include <yandex_io/interfaces/user_config/connector/user_config_provider.h>

#include <yandex_io/libs/ipc/mock/simple_connector.h>
#include <yandex_io/libs/json_utils/json_utils.h>
#include <yandex_io/protos/quasar_proto.pb.h>
#include <yandex_io/tests/testlib/test_utils.h>

#include <library/cpp/testing/unittest/registar.h>

#include <map>

using namespace quasar;
using namespace quasar::TestUtils;

namespace {
    const std::string uberJson =
        R"__({
  "account_config": {
    "A0": "a0-account",
    "D0": "d0-accout",
    "D1": "d1-accout",
    "S0": "s0-accout",
    "S2": "s2-accout"
  },
  "device_config": {
    "D0": "d0-device",
    "D2": "d2-device",
    "S0": "s0-device",
    "S2": "s2-device",
    "S3": "s3-device"
  },
  "system_config": {
    "D1": "d1-system",
    "S1": "s1-system",
    "S2": "s2-system"
  }
})__";

    const proto::QuasarMessage message1 =
        [] {
            auto nowTimestamp = std::chrono::duration_cast<std::chrono::seconds>(std::chrono::system_clock::now().time_since_epoch());

            proto::QuasarMessage message;
            message.mutable_user_config_update()->set_passport_uid("123");
            message.mutable_user_config_update()->set_config(R"({"account_config":{"alwaysOnMicForShortcuts":true,"childContentAccess":"children","contentAccess":"without","jingle":false,"smartActivation":true,"spotter":"alisa"},"device_config":{"allow_non_self_calls":false,"name":"Yandex mini 1","tof":true},"system_config":{"YandexMusic":{"pingIntervalSec":30,"use_net_clock":true,"websocketLogs":["fatal","rerror","warn","fail","debug_handshake","debug_close"]},"adbd":{"remoteActivation":true},"appmetrikaReportEnvironment":{"quasmodrom_group":"beta","quasmodrom_subgroup":""},"biometryGroupEnabled":true,"breakSpeechkitSessionByTimeout":false,"brokenMicDetector":{"enabled":true,"rmsRatioExceededCount":25,"rmsRatioThreshold":0.8},"calld":{"enabled":true,"forceAllowNonSelfCalls":true},"dictExperiments":{"ether":"https://yandex.ru/portal/station/main"},"disabledCommandPhrases":["включи"],"enableCommandSpotters":true,"fluent-bit":{"enabled":true,"variables":{"clickdaemonPort":"443","httpTls":"On","kernelLogsDst":"http"}},"forceUseAudioDevice":true,"gateway_pinger":true,"glagoldEnabled":true,"mediad":{"use_radio2":true},"metricad":{"consumers":{"appmetrica":{"eventBlacklist":["systemMetrics"]},"clickdaemon":{"enabled":true,"eventWhitelist":"yiodStart"}}},"multiroomd":{"allow_non_account_device":true,"beacon_enabled":true,"use_glagol_cluster_protocol":true,"use_insecure_protocol":true},"no_tests":true,"notification_prefs":{"notifications_enabled":true},"ntpd":{"syncEnabled":true},"onlineSpotterEnabled":true,"optimizeAppEnvironment":true,"pings":[{"host":"quasar.yandex.ru","intervalMs":10000,"timeoutMs":10000},{"gateway":true,"intervalMs":5000,"timeoutMs":5000},{"host":"8.8.8.8","intervalMs":10000,"timeoutMs":10000}],"prefetchBlacklist":["personal_assistant.scenarios.get_news__next_block"],"prefetchEnabled":true,"pushd":{"xivaServices":"quasar-realtime,messenger-prod"},"quasmodrom_group":"beta","quasmodrom_subgroup":"","setLocationAccuracy":true,"spotter":{"activation/yandex":{"crc":2455577078,"format":"targz","type":"activation","url":"https://quasar.s3.yandex.net/spotters/qesjb_ru-RU-quasar-yandex-svdf-04Sep20-zero-buf-blocker-wide.tar.gz","word":"yandex"}},"spotterLoggingHeadMillis":9000,"spotterLoggingTailMillis":1000,"uniProxyUrl":"wss://uniproxy.alice.yandex.net/v2/uni.ws","useAudioClientTtsPlayer":true,"voiceDialogSettings":{"additionalSpotterLoggingHeadMillis":9000,"additionalSpotterLoggingTailMillis":1000,"diagPeriod":2000,"diagUrl":"https://uniproxy.alice.yandex.net","dnsCacheEnabled":true,"echoPayloadBytes":10000,"echoPingIntervalMs":30000,"lastMsgTimeout":0,"logSoundUntilEndOfUtterance":false,"resetAfterTrigger":false,"sendPingPongTime":10,"spotterLoggingHeadMillis":9005,"spotterLoggingTailMillis":1000,"subThresholdSendRate":300000}},"auxiliary_device_config":{"alice4business":{"max_volume":10}}})");
            message.mutable_subscription_state()->set_passport_uid("123");
            message.mutable_subscription_state()->set_subscription_info("default");
            message.mutable_subscription_state()->set_last_update_time(static_cast<int64_t>(nowTimestamp.count()));
            return message;
        }();

    const proto::QuasarMessage message1ConfigChanged =
        [] {
            proto::QuasarMessage message(message1);
            message.mutable_user_config_update()->set_config(R"({"account_config":{"alwaysOnMicForShortcuts":false,"childContentAccess":"children","contentAccess":"without","jingle":false,"smartActivation":true,"spotter":"alisa"},"device_config":{"name":"Yandex mini 3"},"system_config":{"YandexMusic":{"failedRequestsToNotify":3,"failedRequestsToSuicide":5,"pingIntervalSec":30,"use_net_clock":true},"adbd":{"remoteActivation":true},"alarmSpotterConfig":"default","appmetrikaReportEnvironment":{"quasmodrom_group":"production","quasmodrom_subgroup":""},"biometryGroupEnabled":true,"brokenMicDetector":{"enabled":true,"rmsRatioExceededCount":25,"rmsRatioThreshold":0.8},"calld":{"enabled":true},"dictExperiments":{"ether":"https://yandex.ru/portal/station/main"},"experiments":["enable_multiroom"],"fluent-bit":{"enabled":true,"variables":{"clickdaemonPort":"443","httpIPv6":"Off","httpTls":"On"}},"forceUseAudioDevice":true,"glagoldEnabled":true,"mediad":{"use_radio2":false},"metricad":{"consumers":{"appmetrica":{"eventBlacklist":["systemMetrics"]},"clickdaemon":{"enabled":true,"eventWhitelist":"yiodStart"}}},"multiroom":{"beacon_enabled":true},"multiroomd":{"allow_non_account_device":true,"beacon_enabled":true},"notification_prefs":{"notifications_enabled":true},"ntpd":{"syncCheckPeriodSec":7200,"syncEnabled":true,"syncInitialPeriodSec":1000},"onlineSpotterEnabled":true,"pings":[{"host":"quasar.yandex.ru","intervalMs":10000,"timeoutMs":10000},{"gateway":true,"intervalMs":5000,"timeoutMs":5000},{"host":"8.8.8.8","intervalMs":10000,"timeoutMs":10000}],"pushd":{"xivaServices":"quasar-realtime,messenger-prod"},"quasmodrom_group":"production","quasmodrom_subgroup":"","uniProxyUrl":"wss://uniproxy.alice.yandex.net/uni.ws","voiceDialogSettings":{"dnsCacheEnabled":true,"echoPayloadBytes":2500,"echoPingIntervalMs":30000,"lastMsgTimeout":6000,"requestStatAckTimeoutMs":5000,"soundLoggingFormat":"opus","spotterLoggingHeadMillis":1500,"spotterLoggingRareEventHeadMillis":9005,"spotterLoggingRareEventPercent":15,"spotterLoggingRareEventTailMillis":1000,"spotterLoggingTailMillis":500},"withLocalVins":false}, "auxiliary_device_config":{"alice4business":{"max_volume":10}}})");
            return message;
        }();

    const proto::QuasarMessage message1SubscriptionChanged =
        [] {
            auto nowTimestamp = std::chrono::duration_cast<std::chrono::seconds>(std::chrono::system_clock::now().time_since_epoch());

            proto::QuasarMessage message;
            message.mutable_subscription_state()->set_passport_uid("123");
            message.mutable_subscription_state()->set_subscription_info("custom");
            message.mutable_subscription_state()->set_last_update_time(static_cast<int64_t>(nowTimestamp.count()));
            return message;
        }();

    const proto::QuasarMessage message2AuthFailed =
        [] {
            proto::QuasarMessage message;
            message.mutable_auth_failed();
            return message;
        }();

    const proto::QuasarMessage messageDevicesList1 =
        [] {
            proto::QuasarMessage message;
            auto deviceList = message.mutable_account_devices_list();

            auto device = deviceList->add_account_devices();
            device->set_id("AAAAAAAAAAAAAAAA");
            device->set_platform("yandexnano");
            device->set_name("KOLONKA");
            device->set_server_certificate("SUPERSECRET");
            device->set_server_private_key("DER_PAROL");

            return message;
        }();

    const proto::QuasarMessage messageDevicesList2 =
        [] {
            proto::QuasarMessage message;
            auto deviceList = message.mutable_account_devices_list();

            auto device = deviceList->add_account_devices();
            device->set_id("AAAAAAAAAAAAAAAA");
            device->set_platform("yandexnano");
            device->set_name("KOLONKA");
            device->set_server_certificate("SUPERSECRET");
            device->set_server_private_key("DER_PAROL");

            device = deviceList->add_account_devices();
            device->set_id("BBBBBBBBBBBBB");
            device->set_platform("yandexplatform");
            device->set_name("NAME");
            device->set_server_certificate("DRUGOI SUPERSECRET");
            device->set_server_private_key("DRUGOI DER_PAROL");

            return message;
        }();

} // namespace

Y_UNIT_TEST_SUITE(UserConfigProvider) {

    Y_UNIT_TEST(testUserConfigMerge)
    {
        Json::Value json = parseJson(uberJson);
        UserConfig userConfig;
        userConfig.account = json["account_config"];
        userConfig.device = json["device_config"];
        userConfig.system = json["system_config"];

        auto mergedJson = userConfig.merged("");
        UNIT_ASSERT_VALUES_EQUAL(mergedJson["A0"].asString(), "a0-account");
        UNIT_ASSERT_VALUES_EQUAL(mergedJson["D0"].asString(), "d0-device");
        UNIT_ASSERT_VALUES_EQUAL(mergedJson["D1"].asString(), "d1-system");
        UNIT_ASSERT_VALUES_EQUAL(mergedJson["D2"].asString(), "d2-device");
        UNIT_ASSERT_VALUES_EQUAL(mergedJson["S0"].asString(), "s0-device");
        UNIT_ASSERT_VALUES_EQUAL(mergedJson["S1"].asString(), "s1-system");
        UNIT_ASSERT_VALUES_EQUAL(mergedJson["S2"].asString(), "s2-system");
        UNIT_ASSERT_VALUES_EQUAL(mergedJson["S3"].asString(), "s3-device");
    }

    Y_UNIT_TEST(testUserConfig0)
    {
        auto connector = std::make_shared<ipc::mock::SimpleConnector>(nullptr);
        auto userConfigProvider = std::make_shared<UserConfigProvider>(connector);

        auto userConfig = userConfigProvider->userConfig().value();
        UNIT_ASSERT(userConfig == nullptr);

        connector->pushMessage(message1);
        userConfig = userConfigProvider->userConfig().value();
        UNIT_ASSERT(userConfig != nullptr);
        UNIT_ASSERT_VALUES_EQUAL((int)userConfig->auth, (int)UserConfig::Auth::SUCCESS);
    }

    Y_UNIT_TEST(testUserConfig1)
    {
        auto connector = std::make_shared<ipc::mock::SimpleConnector>([](auto* self) { self->pushMessage(message1); });
        auto userConfigProvider = std::make_shared<UserConfigProvider>(connector);

        auto userConfig = userConfigProvider->userConfig().value();
        UNIT_ASSERT(userConfig != nullptr);

        UNIT_ASSERT_VALUES_EQUAL((int)userConfig->auth, (int)UserConfig::Auth::SUCCESS);
        UNIT_ASSERT_VALUES_EQUAL(userConfig->passportUid, "123");
        UNIT_ASSERT_VALUES_EQUAL(userConfig->account["childContentAccess"].asString(), "children");
        UNIT_ASSERT_VALUES_EQUAL(userConfig->device["name"].asString(), "Yandex mini 1");
        UNIT_ASSERT_VALUES_EQUAL(userConfig->system["biometryGroupEnabled"].asBool(), true);
        UNIT_ASSERT_VALUES_EQUAL(userConfig->auxiliary["alice4business"]["max_volume"].asInt(), 10);
        UNIT_ASSERT_VALUES_EQUAL(userConfig->subscriptionInfo, "default");
        UNIT_ASSERT(userConfig->updateTime.time_since_epoch().count() > 0);
    }

    Y_UNIT_TEST(testUserConfig2)
    {
        auto connector = std::make_shared<ipc::mock::SimpleConnector>([](auto* self) { self->pushMessage(message1); });
        auto userConfigProvider = std::make_shared<UserConfigProvider>(connector);

        int stage = 1;
        std::map<int, int> stageCounter;
        userConfigProvider->userConfig().connect(
            [&](auto userConfig) {
                stageCounter[stage] += 1;
                if (stage == 1) {
                    UNIT_ASSERT_VALUES_EQUAL(userConfig->device["name"].asString(), "Yandex mini 1");
                    UNIT_ASSERT_VALUES_EQUAL(userConfig->subscriptionInfo, "default");
                } else if (stage == 2) {
                    UNIT_ASSERT_VALUES_EQUAL(userConfig->device["name"].asString(), "Yandex mini 3");
                    UNIT_ASSERT_VALUES_EQUAL(userConfig->subscriptionInfo, "default");
                } else if (stage == 3) {
                    UNIT_ASSERT_VALUES_EQUAL(userConfig->device["name"].asString(), "Yandex mini 3");
                    UNIT_ASSERT_VALUES_EQUAL(userConfig->subscriptionInfo, "custom");
                } else {
                    UNIT_ASSERT(false);
                }
            }, Lifetime::immortal);
        stage = 2;
        connector->pushMessage(message1ConfigChanged);

        stage = 3;
        connector->pushMessage(message1SubscriptionChanged);

        UNIT_ASSERT_VALUES_EQUAL(stageCounter[1], 1);
        UNIT_ASSERT_VALUES_EQUAL(stageCounter[2], 1);
        UNIT_ASSERT_VALUES_EQUAL(stageCounter[3], 1);
    }

    Y_UNIT_TEST(testJsonChangedSignal)
    {
        auto connector = std::make_shared<ipc::mock::SimpleConnector>([](auto* self) { self->pushMessage(message1); });
        auto userConfigProvider = std::make_shared<UserConfigProvider>(connector);

        int stage = 1;
        std::map<IUserConfigProvider::ConfigScope, int> stageCounter;
        userConfigProvider->jsonChangedSignal(IUserConfigProvider::ConfigScope::ACCOUNT, "alwaysOnMicForShortcuts").connect([&](auto pJson) {
            stageCounter[IUserConfigProvider::ConfigScope::ACCOUNT] += 1;
            if (stage == 1) {
                UNIT_ASSERT_VALUES_EQUAL(pJson->isBool(), true);
                UNIT_ASSERT_VALUES_EQUAL(pJson->asBool(), true);
            } else if (stage == 2) {
                UNIT_ASSERT_VALUES_EQUAL(pJson->isBool(), true);
                UNIT_ASSERT_VALUES_EQUAL(pJson->asBool(), false);
            } else if (stage == 3) {
                UNIT_ASSERT_VALUES_EQUAL(pJson->isNull(), true);
            } else {
                UNIT_ASSERT(false);
            }
        }, Lifetime::immortal);
        userConfigProvider->jsonChangedSignal(IUserConfigProvider::ConfigScope::DEVICE, "name").connect([&](auto pJson) {
            stageCounter[IUserConfigProvider::ConfigScope::DEVICE] += 1;
            if (stage == 1) {
                UNIT_ASSERT_VALUES_EQUAL(pJson->isString(), true);
                UNIT_ASSERT_VALUES_EQUAL(pJson->asString(), "Yandex mini 1");
            } else if (stage == 2) {
                UNIT_ASSERT_VALUES_EQUAL(pJson->isString(), true);
                UNIT_ASSERT_VALUES_EQUAL(pJson->asString(), "Yandex mini 3");
            } else if (stage == 3) {
                UNIT_ASSERT_VALUES_EQUAL(pJson->isNull(), true);
            } else {
                UNIT_ASSERT(false);
            }
        }, Lifetime::immortal);
        userConfigProvider->jsonChangedSignal(IUserConfigProvider::ConfigScope::SYSTEM, "YandexMusic/websocketLogs").connect([&](auto pJson) {
            stageCounter[IUserConfigProvider::ConfigScope::SYSTEM] += 1;
            if (stage == 1) {
                UNIT_ASSERT_VALUES_EQUAL(pJson->isArray(), true);
                UNIT_ASSERT_VALUES_EQUAL((*pJson)[4].asString(), "debug_handshake");
            } else if (stage == 2) {
                UNIT_ASSERT_VALUES_EQUAL(pJson->isNull(), true);
            } else {
                UNIT_ASSERT(false);
            }
        }, Lifetime::immortal);

        stage = 2;
        connector->pushMessage(message1ConfigChanged);

        stage = 3;
        connector->pushMessage(message2AuthFailed);

        UNIT_ASSERT_VALUES_EQUAL((int)userConfigProvider->userConfig().value()->auth, (int)UserConfig::Auth::FAILED);
        UNIT_ASSERT_VALUES_EQUAL(stageCounter[IUserConfigProvider::ConfigScope::ACCOUNT], 3);
        UNIT_ASSERT_VALUES_EQUAL(stageCounter[IUserConfigProvider::ConfigScope::DEVICE], 3);
        UNIT_ASSERT_VALUES_EQUAL(stageCounter[IUserConfigProvider::ConfigScope::SYSTEM], 2);
    }

    Y_UNIT_TEST(testJsonChangedSignal2)
    {
        auto connector = std::make_shared<ipc::mock::SimpleConnector>(nullptr);
        auto userConfigProvider = std::make_shared<UserConfigProvider>(connector);

        auto userConfig = userConfigProvider->userConfig().value();
        UNIT_ASSERT(userConfig == nullptr);

        std::atomic<int> stage{1};
        std::atomic<bool> ok{false};
        userConfigProvider->jsonChangedSignal(IUserConfigProvider::ConfigScope::SYSTEM, "unexpectable_section").connect([&](auto pJson) {
            UNIT_ASSERT_VALUES_EQUAL(stage.load(), 2);
            UNIT_ASSERT(pJson);
            UNIT_ASSERT(pJson->isNull());
            ok = true;
        }, Lifetime::immortal);

        stage = 2;
        connector->pushMessage(message1);
        userConfig = userConfigProvider->userConfig().value();
        UNIT_ASSERT(userConfig != nullptr);

        UNIT_ASSERT(ok);
    }

    Y_UNIT_TEST(testJsonChangedSignalMerged)
    {
        auto connector = std::make_shared<ipc::mock::SimpleConnector>(nullptr);
        auto userConfigProvider = std::make_shared<UserConfigProvider>(connector);

        auto userConfig = userConfigProvider->userConfig().value();
        UNIT_ASSERT(userConfig == nullptr);

        std::atomic<int> stage{1};
        std::atomic<bool> unexp{false};
        userConfigProvider->jsonChangedSignal(IUserConfigProvider::ConfigScope::MERGED, "unexpectable_section").connect([&](auto pJson) {
            UNIT_ASSERT_VALUES_EQUAL(stage.load(), 2);
            UNIT_ASSERT(pJson);
            UNIT_ASSERT(pJson->isNull());
            unexp = true;
        }, Lifetime::immortal);

        std::atomic<int> A0{0};
        userConfigProvider->jsonChangedSignal(IUserConfigProvider::ConfigScope::MERGED, "A0").connect([&](auto pJson) {
            UNIT_ASSERT(pJson);
            if (stage.load() == 2) {
                UNIT_ASSERT_VALUES_EQUAL(pJson->asString(), "a0-account");
                ++A0;
            } else if (stage.load() == 3) {
                UNIT_ASSERT_VALUES_EQUAL(pJson->asString(), "a0-system");
                ++A0;
            } else {
                UNIT_ASSERT(false);
            }
        }, Lifetime::immortal);

        std::atomic<int> D2{0};
        userConfigProvider->jsonChangedSignal(IUserConfigProvider::ConfigScope::MERGED, "D2").connect([&](auto pJson) {
            UNIT_ASSERT(pJson);
            if (stage.load() == 2) {
                UNIT_ASSERT_VALUES_EQUAL(pJson->asString(), "d2-device");
                ++D2;
            } else {
                UNIT_ASSERT(false);
            }
        }, Lifetime::immortal);

        std::atomic<int> S2{0};
        userConfigProvider->jsonChangedSignal(IUserConfigProvider::ConfigScope::MERGED, "S2").connect([&](auto pJson) {
            UNIT_ASSERT(pJson);
            if (stage.load() == 2) {
                auto mergedJson = userConfigProvider->userConfig().value()->merged("");
                UNIT_ASSERT_VALUES_EQUAL(mergedJson["S2"].asString(), "s2-system");
                UNIT_ASSERT_VALUES_EQUAL(pJson->asString(), "s2-system");
                ++S2;
            } else if (stage.load() == 3) {
                auto mergedJson = userConfigProvider->userConfig().value()->merged("");
                UNIT_ASSERT_VALUES_EQUAL(mergedJson["S2"].asString(), "s2-device");
                UNIT_ASSERT_VALUES_EQUAL(pJson->asString(), "s2-device");
                ++S2;
            } else {
                UNIT_ASSERT(false);
            }
        }, Lifetime::immortal);
        stage = 2;
        Json::Value json = parseJson(uberJson);
        proto::QuasarMessage m = message1;
        m.mutable_user_config_update()->set_config(jsonToString(json, true));
        connector->pushMessage(m);
        userConfig = userConfigProvider->userConfig().value();
        UNIT_ASSERT(userConfig != nullptr);

        UNIT_ASSERT(unexp.load());
        UNIT_ASSERT_VALUES_EQUAL(A0.load(), 1);
        UNIT_ASSERT_VALUES_EQUAL(D2.load(), 1);
        UNIT_ASSERT_VALUES_EQUAL(S2.load(), 1);

        stage = 3;
        json["system_config"]["A0"] = "a0-system";
        json["system_config"].removeMember("S2");
        m.mutable_user_config_update()->set_config(jsonToString(json, true));
        connector->pushMessage(m);
        UNIT_ASSERT_VALUES_EQUAL(A0.load(), 2);
        UNIT_ASSERT_VALUES_EQUAL(D2.load(), 1);
        UNIT_ASSERT_VALUES_EQUAL(S2.load(), 2);
    }

    Y_UNIT_TEST(testAccountDevicesChangedSignal)
    {
        auto connector = std::make_shared<ipc::mock::SimpleConnector>([](auto* self) { self->pushMessage(message1); });
        auto userConfigProvider = std::make_shared<UserConfigProvider>(connector);

        auto userConfig = userConfigProvider->userConfig().value();
        UNIT_ASSERT(userConfig != nullptr);

        int userConfigCounter = 0;
        int accountDevicesCounter = 0;

        userConfigProvider->userConfig().connect(
            [&](auto... /*args*/) {
                ++userConfigCounter;
            }, Lifetime::immortal);
        userConfigProvider->accountDevicesChangedSignal().connect(
            [&](auto list) {
                ++accountDevicesCounter;

                if (accountDevicesCounter == 1) {
                    UNIT_ASSERT_VALUES_EQUAL(list->size(), 0);
                } else if (accountDevicesCounter == 2) {
                    UNIT_ASSERT_VALUES_EQUAL(list->size(), 1);
                    UNIT_ASSERT_VALUES_EQUAL(list->front().deviceId, "AAAAAAAAAAAAAAAA");
                    UNIT_ASSERT_VALUES_EQUAL(list->front().platform, "yandexnano");
                    UNIT_ASSERT_VALUES_EQUAL(list->front().name, "KOLONKA");
                    UNIT_ASSERT_VALUES_EQUAL(list->front().serverCertificate, "SUPERSECRET");
                    UNIT_ASSERT_VALUES_EQUAL(list->front().serverPrivateKey, "DER_PAROL");
                } else if (accountDevicesCounter == 3) {
                    UNIT_ASSERT_VALUES_EQUAL(list->size(), 2);
                    UNIT_ASSERT_VALUES_EQUAL(list->front().deviceId, "AAAAAAAAAAAAAAAA");

                    UNIT_ASSERT_VALUES_EQUAL(list->back().deviceId, "BBBBBBBBBBBBB");
                    UNIT_ASSERT_VALUES_EQUAL(list->back().platform, "yandexplatform");
                    UNIT_ASSERT_VALUES_EQUAL(list->back().name, "NAME");
                    UNIT_ASSERT_VALUES_EQUAL(list->back().serverCertificate, "DRUGOI SUPERSECRET");
                    UNIT_ASSERT_VALUES_EQUAL(list->back().serverPrivateKey, "DRUGOI DER_PAROL");
                } else {
                    UNIT_ASSERT(false);
                }
            }, Lifetime::immortal);

        connector->pushMessage(messageDevicesList1);
        connector->pushMessage(messageDevicesList2);
        connector->pushMessage(message1SubscriptionChanged); // Trigger user config changes but not account device list changed

        UNIT_ASSERT_VALUES_EQUAL(userConfigCounter, 4);
        UNIT_ASSERT_VALUES_EQUAL(accountDevicesCounter, 3);
    }

    Y_UNIT_TEST(testAccountDevicesChangedSignalNullValue)
    {
        auto connector = std::make_shared<ipc::mock::SimpleConnector>(nullptr);
        auto userConfigProvider = std::make_shared<UserConfigProvider>(connector);

        auto userConfig = userConfigProvider->userConfig().value();
        UNIT_ASSERT(userConfig == nullptr);

        int userConfigCounter = 0;
        int accountDevicesCounter = 0;

        userConfigProvider->userConfig().connect(
            [&](auto... /*args*/) {
                ++userConfigCounter;
            }, Lifetime::immortal);
        userConfigProvider->accountDevicesChangedSignal().connect(
            [&](auto list) {
                ++accountDevicesCounter;

                if (accountDevicesCounter == 1) {
                    UNIT_ASSERT_VALUES_EQUAL(list->size(), 0);
                } else if (accountDevicesCounter == 2) {
                    UNIT_ASSERT_VALUES_EQUAL(list->size(), 1);
                    UNIT_ASSERT_VALUES_EQUAL(list->front().deviceId, "AAAAAAAAAAAAAAAA");
                    UNIT_ASSERT_VALUES_EQUAL(list->front().platform, "yandexnano");
                    UNIT_ASSERT_VALUES_EQUAL(list->front().name, "KOLONKA");
                    UNIT_ASSERT_VALUES_EQUAL(list->front().serverCertificate, "SUPERSECRET");
                    UNIT_ASSERT_VALUES_EQUAL(list->front().serverPrivateKey, "DER_PAROL");
                } else if (accountDevicesCounter == 3) {
                    UNIT_ASSERT_VALUES_EQUAL(list->size(), 2);
                    UNIT_ASSERT_VALUES_EQUAL(list->front().deviceId, "AAAAAAAAAAAAAAAA");

                    UNIT_ASSERT_VALUES_EQUAL(list->back().deviceId, "BBBBBBBBBBBBB");
                    UNIT_ASSERT_VALUES_EQUAL(list->back().platform, "yandexplatform");
                    UNIT_ASSERT_VALUES_EQUAL(list->back().name, "NAME");
                    UNIT_ASSERT_VALUES_EQUAL(list->back().serverCertificate, "DRUGOI SUPERSECRET");
                    UNIT_ASSERT_VALUES_EQUAL(list->back().serverPrivateKey, "DRUGOI DER_PAROL");
                } else {
                    UNIT_ASSERT(false);
                }
            }, Lifetime::immortal);

        UNIT_ASSERT_VALUES_EQUAL(userConfigCounter, 0);
        UNIT_ASSERT_VALUES_EQUAL(accountDevicesCounter, 0);
        connector->pushMessage(message1);
        UNIT_ASSERT_VALUES_EQUAL(userConfigCounter, 1);
        UNIT_ASSERT_VALUES_EQUAL(accountDevicesCounter, 1);

        connector->pushMessage(messageDevicesList1);
        connector->pushMessage(messageDevicesList2);
        connector->pushMessage(message1SubscriptionChanged); // Trigger user config changes but not account device list changed

        UNIT_ASSERT_VALUES_EQUAL(userConfigCounter, 4);
        UNIT_ASSERT_VALUES_EQUAL(accountDevicesCounter, 3);
    }

}
