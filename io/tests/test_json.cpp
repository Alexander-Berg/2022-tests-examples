#include <yandex_io/libs/json_utils/json_utils.h>

#include <library/cpp/testing/unittest/registar.h>

Y_UNIT_TEST_SUITE_F(TestJsonUtils, NUnitTest::TBaseFixture) {
    Y_UNIT_TEST(tryParseJson)
    {
        UNIT_ASSERT_EQUAL(std::nullopt, quasar::tryParseJson(""));
        UNIT_ASSERT_EQUAL(Json::Value(Json::objectValue), *quasar::tryParseJson("{}"));
    }

    Y_UNIT_TEST(testTransform)
    {
        auto dummy = [](const std::string& s) -> std::string {
            return "_" + s + "_";
        };

        Json::Value from;
        from["a"] = "value";
        from["b"]["a"] = 1;
        from["b"]["b"] = "v";
        from["b"]["c"] = Json::Value::null;
        Json::Value jsonArray;
        jsonArray.append(Json::Value::null);
        jsonArray.append("a");
        jsonArray.append(1);
        from["c"] = jsonArray;

        UNIT_ASSERT_VALUES_EQUAL(jsonArray[2].asInt(), 1);

        Json::Value to = quasar::transform(from, dummy);
        UNIT_ASSERT_VALUES_EQUAL(to["a"].asString(), "_value_");
        UNIT_ASSERT_VALUES_EQUAL(to["b"]["a"].asInt(), 1);
        UNIT_ASSERT_VALUES_EQUAL(to["b"]["b"].asString(), "_v_");
        UNIT_ASSERT(to["b"]["c"].isNull());
        auto jsonArray2 = to["c"];
        UNIT_ASSERT(jsonArray2[0].isNull());
        UNIT_ASSERT_VALUES_EQUAL(jsonArray2[1].asString(), "_a_");
        UNIT_ASSERT_VALUES_EQUAL(jsonArray2[2].asInt(), 1);
    }

    Y_UNIT_TEST(testJsonMerge)
    {
        Json::Value from;
        from["key1"] = "value1";
        from["key2"]["key3"] = 3;

        Json::Value to;
        to["key2"]["key4"]["key5"] = 5;
        to["key6"] = "value6";

        Json::Value to2 = to;

        quasar::jsonMerge(from, to);
        UNIT_ASSERT_VALUES_EQUAL(to["key1"].asString(), "value1");
        UNIT_ASSERT_VALUES_EQUAL(to["key2"]["key3"].asInt(), 3);
        UNIT_ASSERT_VALUES_EQUAL(to["key2"]["key4"]["key5"].asInt(), 5);
        UNIT_ASSERT_VALUES_EQUAL(to["key6"].asString(), "value6");

        quasar::jsonMergeArrays(from, to2);
        UNIT_ASSERT_VALUES_EQUAL(to2["key1"].asString(), "value1");
        UNIT_ASSERT_VALUES_EQUAL(to2["key2"]["key3"].asInt(), 3);
        UNIT_ASSERT_VALUES_EQUAL(to2["key2"]["key4"]["key5"].asInt(), 5);
        UNIT_ASSERT_VALUES_EQUAL(to2["key6"].asString(), "value6");
    }

    Y_UNIT_TEST(testJsonMerge2)
    {
        Json::Value from;
        from["key1"] = "value1";
        from["key2"]["key3"] = 3;

        Json::Value to;
        to["key6"] = "value6";

        Json::Value to2 = to;

        quasar::jsonMerge(from, to);
        UNIT_ASSERT_VALUES_EQUAL(to["key1"].asString(), "value1");
        UNIT_ASSERT_VALUES_EQUAL(to["key2"]["key3"].asInt(), 3);
        UNIT_ASSERT_VALUES_EQUAL(to["key6"].asString(), "value6");

        quasar::jsonMergeArrays(from, to2);
        UNIT_ASSERT_VALUES_EQUAL(to2["key1"].asString(), "value1");
        UNIT_ASSERT_VALUES_EQUAL(to2["key2"]["key3"].asInt(), 3);
        UNIT_ASSERT_VALUES_EQUAL(to2["key6"].asString(), "value6");
    }

    Y_UNIT_TEST(testJsonMergeArrayLegacy)
    {
        Json::Value from = quasar::parseJson(R"({"key1":"value1","key2":[1,2,3]})");

        Json::Value to = quasar::parseJson(R"({"key2":[4,5], "key3": "value3"})");

        quasar::jsonMerge(from, to);
        auto result = quasar::jsonToString(to);
        UNIT_ASSERT_VALUES_EQUAL(result, R"({"key1":"value1","key2":[1,2,3],"key3":"value3"})"
                                         "\n");
    }

    Y_UNIT_TEST(testJsonMergeArrayImproved)
    {
        Json::Value from = quasar::parseJson(R"({"key1":"value1","key2":[1,2,3]})");
        Json::Value to = quasar::parseJson(R"({"key2":[4,5], "key3": "value3"})");

        quasar::jsonMergeArrays(from, to);
        auto result = quasar::jsonToString(to);
        UNIT_ASSERT_VALUES_EQUAL(result, R"({"key1":"value1","key2":[4,5,1,2,3],"key3":"value3"})"
                                         "\n");

        from = quasar::parseJson(R"({"key1":"value1","key2":[1,2,3]})");
        to = quasar::parseJson(R"({"key2":[1,2], "key3": "value3"})");

        quasar::jsonMergeArrays(from, to);
        result = quasar::jsonToString(to);
        UNIT_ASSERT_VALUES_EQUAL(result, R"({"key1":"value1","key2":[1,2,1,2,3],"key3":"value3"})"
                                         "\n");
    }

    Y_UNIT_TEST(testJsonMergeNull)
    {
        Json::Value from = quasar::parseJson(R"({"key1":"value1","key2":[1,2,3]})");
        Json::Value to;
        Json::Value to2;

        quasar::jsonMerge(from, to);
        auto result = quasar::jsonToString(to);
        UNIT_ASSERT_VALUES_EQUAL(result, R"({"key1":"value1","key2":[1,2,3]})"
                                         "\n");

        quasar::jsonMergeArrays(from, to2);
        result = quasar::jsonToString(to2);
        UNIT_ASSERT_VALUES_EQUAL(result, R"({"key1":"value1","key2":[1,2,3]})"
                                         "\n");
    }

    Y_UNIT_TEST(testJsonMergeClean)
    {
        Json::Value from;
        from["key1"] = "value1";
        from["key2"]["key3"] = 3;
        from["keyNull"] = Json::nullValue;

        Json::Value to;
        to["keyNull"] = "value6"; // will be cleared

        Json::Value to2 = to;

        quasar::jsonMerge(from, to);
        UNIT_ASSERT_VALUES_EQUAL(to["key1"].asString(), "value1");
        UNIT_ASSERT_VALUES_EQUAL(to["key2"]["key3"].asInt(), 3);
        UNIT_ASSERT(!to.isMember("keyNull"));

        quasar::jsonMergeArrays(from, to2);
        UNIT_ASSERT_VALUES_EQUAL(to2["key1"].asString(), "value1");
        UNIT_ASSERT_VALUES_EQUAL(to2["key2"]["key3"].asInt(), 3);
        UNIT_ASSERT(!to2.isMember("keyNull"));
    }

    Y_UNIT_TEST(testTryGetVector)
    {
        {
            Json::Value json;
            json["a"] = Json::arrayValue;
            json["a"].append(123);
            json["a"].append(456);

            auto v = quasar::tryGetVector<int>(json, "a");
            UNIT_ASSERT_VALUES_EQUAL(v.size(), 2);
            UNIT_ASSERT_VALUES_EQUAL(v[0], 123);
            UNIT_ASSERT_VALUES_EQUAL(v[1], 456);
        }

        {
            Json::Value json;
            json["a"] = Json::arrayValue;
            json["a"].append(true);
            json["a"].append(false);

            auto v = quasar::tryGetVector<bool>(json, "a");
            UNIT_ASSERT_VALUES_EQUAL(v.size(), 2);
            UNIT_ASSERT_VALUES_EQUAL(v[0], true);
            UNIT_ASSERT_VALUES_EQUAL(v[1], false);
        }

        {
            Json::Value json;
            json["a"] = Json::arrayValue;
            json["a"].append("AAAA");
            json["a"].append("BBBB");

            auto v = quasar::tryGetVector<std::string>(json, "a");
            UNIT_ASSERT_VALUES_EQUAL(v.size(), 2);
            UNIT_ASSERT_VALUES_EQUAL(v[0], "AAAA");
            UNIT_ASSERT_VALUES_EQUAL(v[1], "BBBB");
        }

        {
            Json::Value json;
            json["a"] = Json::arrayValue;
            json["a"].append("AAAA");
            json["a"].append("BBBB");

            auto v = quasar::tryGetVector<double>(json, "a"); // error
            UNIT_ASSERT_VALUES_EQUAL(v.size(), 0);
        }
    }

    Y_UNIT_TEST(testFindXPath)
    {
        Json::Value json = quasar::parseJson(R"JSON({"account_config":{"alwaysOnMicForShortcuts":false,"childContentAccess":"children","contentAccess":"without","jingle":false,"smartActivation":true,"spotter":"alisa"},"device_config":{"name":"Yandex mini 3"},"system_config":{"YandexMusic":{"failedRequestsToNotify":3,"failedRequestsToSuicide":5,"pingIntervalSec":30,"use_net_clock":true},"adbd":{"remoteActivation":true},"alarmSpotterConfig":"default","appmetrikaReportEnvironment":{"quasmodrom_group":"production","quasmodrom_subgroup":""},"biometryGroupEnabled":true,"brokenMicDetector":{"enabled":true,"rmsRatioExceededCount":25,"rmsRatioThreshold":0.8},"calld":{"enabled":true},"dictExperiments":{"ether":"https://yandex.ru/portal/station/main"},"experiments":["video_disable_webview_searchscreen"],"fluent-bit":{"enabled":true,"variables":{"clickdaemonPort":"443","httpIPv6":"Off","httpTls":"On"}},"forceUseAudioDevice":true,"glagoldEnabled":true,"mediad":{"use_radio2":false},"metricad":{"consumers":{"appmetrica":{"eventBlacklist":["systemMetrics"]},"clickdaemon":{"enabled":true}}},"multiroom":{"beacon_enabled":true},"multiroomd":{"allow_non_account_device":true,"beacon_enabled":true},"notification_prefs":{"notifications_enabled":true},"ntpd":{"syncCheckPeriodSec":7200,"syncEnabled":true,"syncInitialPeriodSec":1000},"onlineSpotterEnabled":true,"pings":[{"host":"quasar.yandex.ru","intervalMs":10000,"timeoutMs":10000},{"gateway":true,"intervalMs":5000,"timeoutMs":5000},{"host":"8.8.8.8","intervalMs":10000,"timeoutMs":10000}],"pushd":{"xivaServices":"quasar-realtime,messenger-prod"},"quasmodrom_group":"production","quasmodrom_subgroup":"","uniProxyUrl":"wss://uniproxy.alice.yandex.net/uni.ws","voiceDialogSettings":{"dnsCacheEnabled":true,"echoPayloadBytes":2500,"echoPingIntervalMs":30000,"lastMsgTimeout":6000,"requestStatAckTimeoutMs":5000,"soundLoggingFormat":"opus","spotterLoggingHeadMillis":1500,"spotterLoggingRareEventHeadMillis":9005,"spotterLoggingRareEventPercent":15,"spotterLoggingRareEventTailMillis":1000,"spotterLoggingTailMillis":500},"withLocalVins":false}})JSON");

        auto remoteActivation = quasar::findXPath(json, "system_config/adbd/remoteActivation");
        UNIT_ASSERT(remoteActivation);
        UNIT_ASSERT_VALUES_EQUAL(remoteActivation->isBool(), true);
        UNIT_ASSERT_VALUES_EQUAL(remoteActivation->asBool(), true);

        auto echoPayloadBytes = quasar::findXPath(json, "system_config/voiceDialogSettings/echoPayloadBytes");
        UNIT_ASSERT(echoPayloadBytes);
        UNIT_ASSERT_VALUES_EQUAL(echoPayloadBytes->isInt64(), true);
        UNIT_ASSERT_VALUES_EQUAL(echoPayloadBytes->asInt64(), 2500);

        auto echoPayloadBytes1 = quasar::findXPath(json, "/system_config/voiceDialogSettings/echoPayloadBytes");
        UNIT_ASSERT_VALUES_EQUAL(echoPayloadBytes, echoPayloadBytes1);

        auto echoPayloadBytes2 = quasar::findXPath(json, "/system_config/voiceDialogSettings/echoPayloadBytes/");
        UNIT_ASSERT_VALUES_EQUAL(echoPayloadBytes, echoPayloadBytes2);
    }

    Y_UNIT_TEST(testJsonRemoveArrayDuplicates)
    {
        Json::Value src = quasar::parseJson(R"([1,2,3,1,2,4,1,2,5,1,2,3,6,7])");
        auto result = quasar::jsonToString(quasar::jsonRemoveArrayDuplicates(src));
        UNIT_ASSERT_VALUES_EQUAL(result, R"([1,2,3,4,5,6,7])"
                                         "\n");
    }

    Y_UNIT_TEST(getOtional)
    {
        Json::Value src = quasar::parseJson(R"JSON({"A":true,"B":false,"C":"test"})JSON");
        auto a = quasar::getOptionalBool(src, "A");
        UNIT_ASSERT(a.has_value());
        UNIT_ASSERT(*a);
        auto b = quasar::getOptionalBool(src, "B");
        UNIT_ASSERT(b.has_value());
        UNIT_ASSERT(!*b);
        auto c = quasar::getOptionalBool(src, "C");
        UNIT_ASSERT(!c.has_value());
        auto d = quasar::getOptionalBool(src, "D");
        UNIT_ASSERT(!d.has_value());
    }
}
