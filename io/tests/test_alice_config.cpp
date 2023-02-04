#include <yandex_io/services/aliced/alice_config/alice_config.h>
#include <yandex_io/tests/testlib/unittest_helper/unit_test_fixture.h>

#include <yandex_io/libs/cryptography/digest.h>
#include <yandex_io/libs/json_utils/json_utils.h>

using namespace quasar;

namespace {
    template <class T>
    Json::Value makeFileConfig(const std::string& name, T value) {
        Json::Value config;
        config[name] = value;
        return config;
    }

    template <class T>
    std::string makeAccountConfig(const std::string& name, T value) {
        Json::Value config;
        config["account_config"] = makeFileConfig<T>(name, value);
        return jsonToString(config);
    }

    template <class T>
    std::string makeSystemConfig(const std::string& name, T value) {
        Json::Value config;
        config["system_config"] = makeFileConfig<T>(name, value);
        return jsonToString(config);
    }

    template <>
    Json::Value makeFileConfig(const std::string& name, std::unordered_set<std::string> value) {
        Json::Value config;
        config[name] = Json::arrayValue;
        for (const auto& str : value) {
            config[name].append(str);
        }

        return config;
    }
} // namespace

Y_UNIT_TEST_SUITE(AliceConfigTest) {

    Y_UNIT_TEST_F(checkUniProxyUrl, QuasarUnitTestFixtureWithoutIpc) {
        const ::SpeechKit::VoiceServiceSettings defaultSettings{SpeechKit::Language::russian};

        {
            AliceConfig config(getDeviceForTests(), Json::Value());
            UNIT_ASSERT(config.getUniProxyUrl() == defaultSettings.uniProxyUrl);
        }

        {
            AliceConfig config(getDeviceForTests(), makeFileConfig("uniProxyUrl", "url1"));
            UNIT_ASSERT(config.getUniProxyUrl() == "url1");
        }

        {
            AliceConfig config(getDeviceForTests(), makeFileConfig("uniProxyUrl", "url1"));
            config.setReceivedConfig(makeSystemConfig("uniProxyUrl", "url2"));
            UNIT_ASSERT(config.getUniProxyUrl() == "url2");
        }
    }

    Y_UNIT_TEST_F(checkStartDelay, QuasarUnitTestFixtureWithoutIpc) {
        {
            AliceConfig config(getDeviceForTests(), Json::Value());
            UNIT_ASSERT_EQUAL(config.getStartDelay(), std::chrono::seconds(0));
        }

        {
            AliceConfig config(getDeviceForTests(), makeFileConfig("startDelaySec", 6));
            UNIT_ASSERT_EQUAL(config.getStartDelay(), std::chrono::seconds(6));
        }
    }

    Y_UNIT_TEST_F(checkJingle, QuasarUnitTestFixtureWithoutIpc) {
        {
            AliceConfig config(getDeviceForTests(), Json::Value());
            UNIT_ASSERT(config.getJingle() == false);
        }

        {
            AliceConfig config(getDeviceForTests(), makeFileConfig("jingle", true));
            UNIT_ASSERT(config.getJingle() == true);
        }

        {
            AliceConfig config(getDeviceForTests(), makeFileConfig("jingle", true));
            config.setReceivedConfig(makeAccountConfig("jingle", false));
            UNIT_ASSERT(config.getJingle() == false);
        }
    }

    Y_UNIT_TEST_F(checkSpeechkitLogChannels, QuasarUnitTestFixtureWithoutIpc) {
        {
            const std::unordered_set<std::string> result{"vqe"};
            AliceConfig config(getDeviceForTests(), Json::Value());
            UNIT_ASSERT(config.getSpeechkitLogChannels() == result);
        }

        {
            const std::unordered_set<std::string> result{"vqe", "raw_0"};
            AliceConfig config(getDeviceForTests(), makeFileConfig("speechkitLogChannels", result));
            UNIT_ASSERT(config.getSpeechkitLogChannels() == result);
        }

        {
            const std::unordered_set<std::string> initValue{"vqe", "raw_0"};
            const std::unordered_set<std::string> result{"raw_0", "raw_1"};
            AliceConfig config(getDeviceForTests(), makeFileConfig("speechkitLogChannels", initValue));
            config.setReceivedConfig(makeSystemConfig("speechkitLogChannels", result));
            UNIT_ASSERT(config.getSpeechkitLogChannels() == result);
        }
    }

    Y_UNIT_TEST_F(checkGetSoundLogMaxParallelSendings, QuasarUnitTestFixtureWithoutIpc) {
        {
            AliceConfig config(getDeviceForTests(), Json::Value());
            UNIT_ASSERT(config.getSoundLogMaxParallelSendings() == 1);
        }

        {
            AliceConfig config(getDeviceForTests(), makeFileConfig("soundLogMaxParallelSendings", 2));
            UNIT_ASSERT(config.getSoundLogMaxParallelSendings() == 2);
        }

        {
            AliceConfig config(getDeviceForTests(), makeFileConfig("soundLogMaxParallelSendings", 2));
            config.setReceivedConfig(makeSystemConfig("soundLogMaxParallelSendings", 3));
            UNIT_ASSERT(config.getSoundLogMaxParallelSendings() == 3);
        }
    }

    Y_UNIT_TEST_F(checkRMSCorrection, QuasarUnitTestFixtureWithoutIpc) {
        {
            AliceConfig config(getDeviceForTests(), Json::Value());
            UNIT_ASSERT(config.getRMSCorrection() == 1.0);
        }

        {
            AliceConfig config(getDeviceForTests(), makeFileConfig("RMSCorrection", 2.0));
            UNIT_ASSERT(config.getRMSCorrection() == 2.0);
        }

        {
            AliceConfig config(getDeviceForTests(), makeFileConfig("RMSCorrection", 2.0));
            config.setReceivedConfig(makeSystemConfig("RMSCorrection", 3.0));
            UNIT_ASSERT(config.getRMSCorrection() == 3.0);
        }
    }

    Y_UNIT_TEST_F(checkGetVoiceServiceSettings, QuasarUnitTestFixtureWithoutIpc) {
        const proto::WifiList wifiList;
        ::SpeechKit::VoiceServiceSettings defaultSettings{SpeechKit::Language::russian};

        {
            AliceConfig config(getDeviceForTests(), Json::Value());

            config.setVqeInfo(YandexIO::ChannelData::VqeInfo{
                .type = "vqeType",
                .preset = "vqePreset"});
            const auto settings = config.getVoiceServiceSettings(
                "oauthToken", "passportUid", "activationSpotter", "interruptionSpotter", "additionalSpotter", wifiList, Json::objectValue);

            UNIT_ASSERT(settings.oauthToken == "oauthToken");
            UNIT_ASSERT(settings.biometryGroup == calcMD5Digest("passportUid"));
            UNIT_ASSERT(settings.activationPhraseSpotter.modelPath == "activationSpotter");
            UNIT_ASSERT(parseJson(settings.activationPhraseSpotter.soundLoggerSettings.payloadExtra)["VQEType"].asString() == "vqeType");
            UNIT_ASSERT(settings.interruptionPhraseSpotter.modelPath == "interruptionSpotter");
            UNIT_ASSERT(parseJson(settings.interruptionPhraseSpotter.soundLoggerSettings.payloadExtra)["VQEType"].asString() == "vqeType");
            UNIT_ASSERT(settings.additionalPhraseSpotter.modelPath.empty());
        }
    }

    Y_UNIT_TEST_F(checkConnectionTimeout, QuasarUnitTestFixtureWithoutIpc) {
        const proto::WifiList wifiList;
        ::SpeechKit::VoiceServiceSettings defaultSettings{SpeechKit::Language::russian};

        {
            AliceConfig config(getDeviceForTests(), Json::Value());
            const auto result = config.getVoiceServiceSettings("", "", "", "", "", wifiList, Json::objectValue);

            UNIT_ASSERT(result.connectionTimeout == defaultSettings.connectionTimeout);
        }

        {
            Json::Value fileConfig;
            fileConfig["voiceDialogSettings"]["connectionTimeout"] = 2000;

            AliceConfig config(getDeviceForTests(), fileConfig);
            const auto result = config.getVoiceServiceSettings("", "", "", "", "", wifiList, Json::objectValue);

            UNIT_ASSERT(result.connectionTimeout.count() == 2000);
        }

        {
            Json::Value quasmoConfig;
            quasmoConfig["system_config"]["voiceDialogSettings"]["connectionTimeout"] = 2000;

            AliceConfig config(getDeviceForTests(), Json::Value());
            config.setReceivedConfig(jsonToString(quasmoConfig));
            const auto result = config.getVoiceServiceSettings("", "", "", "", "", wifiList, Json::objectValue);

            UNIT_ASSERT(result.connectionTimeout.count() == 2000);
        }
    }

    Y_UNIT_TEST_F(checkStartingSilenceTimeout, QuasarUnitTestFixtureWithoutIpc) {
        const proto::WifiList wifiList;
        ::SpeechKit::VoiceServiceSettings defaultSettings{SpeechKit::Language::russian};

        {
            AliceConfig config(getDeviceForTests(), Json::Value());
            const auto result = config.getVoiceServiceSettings("", "", "", "", "", wifiList, Json::objectValue);

            UNIT_ASSERT(result.recognizer.startingSilenceTimeout == defaultSettings.recognizer.startingSilenceTimeout);
        }

        {
            Json::Value fileConfig;
            fileConfig["voiceDialogSettings"]["recognizer"]["startingSilenceTimeout"] = 2000;

            AliceConfig config(getDeviceForTests(), fileConfig);
            const auto result = config.getVoiceServiceSettings("", "", "", "", "", wifiList, Json::objectValue);

            UNIT_ASSERT(result.recognizer.startingSilenceTimeout.count() == 2000);
        }

        {
            Json::Value quasmoConfig;
            quasmoConfig["system_config"]["voiceDialogSettings"]["recognizer"]["startingSilenceTimeout"] = 2000;

            AliceConfig config(getDeviceForTests(), Json::Value());
            config.setReceivedConfig(jsonToString(quasmoConfig));
            const auto result = config.getVoiceServiceSettings("", "", "", "", "", wifiList, Json::objectValue);

            UNIT_ASSERT(result.recognizer.startingSilenceTimeout.count() == 2000);
        }
    }

    Y_UNIT_TEST_F(checkBackoffIntervals, QuasarUnitTestFixtureWithoutIpc) {
        const proto::WifiList wifiList;
        ::SpeechKit::VoiceServiceSettings defaultSettings{SpeechKit::Language::russian};

        {
            AliceConfig config(getDeviceForTests(), Json::Value());
            const auto result = config.getVoiceServiceSettings("", "", "", "", "", wifiList, Json::objectValue);

            UNIT_ASSERT(result.backoffMinInterval == defaultSettings.backoffMinInterval);
            UNIT_ASSERT(result.backoffMaxInterval == defaultSettings.backoffMaxInterval);
        }

        {
            Json::Value quasmoConfig;
            quasmoConfig["system_config"]["voiceDialogSettings"]["backoffMinIntervalSec"] = 10;
            quasmoConfig["system_config"]["voiceDialogSettings"]["backoffMaxIntervalSec"] = 600;

            AliceConfig config(getDeviceForTests(), Json::Value());
            config.setReceivedConfig(jsonToString(quasmoConfig));
            const auto result = config.getVoiceServiceSettings("", "", "", "", "", wifiList, Json::objectValue);

            using namespace std::literals::chrono_literals;
            UNIT_ASSERT(result.backoffMinInterval == 10s);
            UNIT_ASSERT(result.backoffMaxInterval == 600s);
        }
    }

    Y_UNIT_TEST_F(checkSettingsReset, QuasarUnitTestFixtureWithoutIpc) {
        const proto::WifiList wifiList;
        ::SpeechKit::VoiceServiceSettings defaultSettings{SpeechKit::Language::russian};

        {
            Json::Value quasmoConfig;
            quasmoConfig["account_config"]["jingle"] = true;
            quasmoConfig["system_config"]["uniProxyUrl"] = "url1";
            quasmoConfig["system_config"]["voiceDialogSettings"]["connectionTimeout"] = 2000;

            AliceConfig config(getDeviceForTests(), Json::Value());
            config.setReceivedConfig(jsonToString(quasmoConfig));
            auto result = config.getVoiceServiceSettings("", "", "", "", "", wifiList, Json::objectValue);
            UNIT_ASSERT(result.connectionTimeout.count() == 2000);
            UNIT_ASSERT(result.uniProxyUrl == "url1");
            UNIT_ASSERT(config.getJingle() == true);

            quasmoConfig["account_config"].clear();
            quasmoConfig["system_config"].clear();
            config.setReceivedConfig(jsonToString(quasmoConfig));
            result = config.getVoiceServiceSettings("", "", "", "", "", wifiList, Json::objectValue);
            UNIT_ASSERT(result.connectionTimeout == defaultSettings.connectionTimeout);
            UNIT_ASSERT(result.uniProxyUrl == defaultSettings.uniProxyUrl);
            UNIT_ASSERT(config.getJingle() == false);
        }
    }

    Y_UNIT_TEST_F(checkLanguage, QuasarUnitTestFixtureWithoutIpc) {
        {
            const auto defaultLanguage{SpeechKit::Language::russian};

            AliceConfig config(getDeviceForTests(), Json::Value{});

            const auto settings = config.getVoiceServiceSettings("token", "uid", "path", "path", "path", proto::WifiList{}, Json::objectValue);
            UNIT_ASSERT_VALUES_EQUAL(settings.recognizer.language.toString(), defaultLanguage.toString());
        }

        {
            const auto fileLanguage = SpeechKit::Language{"russian"};
            const auto quasmoLanguage = SpeechKit::Language{"english"};

            Json::Value fileConfig;
            fileConfig["voiceDialogSettings"]["language"] = fileLanguage.toString();

            AliceConfig config(getDeviceForTests(), fileConfig);

            auto settings = config.getVoiceServiceSettings("token", "uid", "path", "path", "path", proto::WifiList{}, Json::objectValue);
            UNIT_ASSERT_VALUES_EQUAL(settings.recognizer.language.toString(), fileLanguage.toString());

            Json::Value quasmoConfig;
            quasmoConfig["system_config"]["voiceDialogSettings"]["language"] = quasmoLanguage.toString();
            config.setReceivedConfig(jsonToString(quasmoConfig));

            settings = config.getVoiceServiceSettings("token", "uid", "path", "path", "path", proto::WifiList{}, Json::objectValue);
            UNIT_ASSERT_VALUES_EQUAL(settings.recognizer.language.toString(), quasmoLanguage.toString());
        }
    }
}
