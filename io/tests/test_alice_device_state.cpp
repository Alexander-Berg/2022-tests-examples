#include <yandex_io/services/aliced/alice_config/alice_config.h>
#include <yandex_io/services/aliced/device_state/alice_device_state.h>

#include <yandex_io/libs/json_utils/json_utils.h>
#include <yandex_io/libs/logging/logging.h>
#include <yandex_io/sdk/proto/device_sdk.pb.h>
#include <yandex_io/tests/testlib/unittest_helper/unit_test_fixture.h>

#include <alice/megamind/protos/common/frame.pb.h>
#include <alice/megamind/protos/common/conditional_action.pb.h>

#include <yandex_io/protos/capabilities/device_state_capability.pb.h>

#include <library/cpp/testing/unittest/env.h>

using namespace quasar;

namespace {

    AliceConfig createAliceConfig(std::shared_ptr<YandexIO::IDevice> device, bool putBtNetworksToDeviceState)
    {
        Json::Value fileConfig;
        fileConfig["putBtNetworksToDeviceState"] = putBtNetworksToDeviceState;
        return {device, std::move(fileConfig)};
    }

    AliceDeviceState createDeviceState(std::string deviceId)
    {
        AliceDeviceState deviceState{std::move(deviceId), nullptr, nullptr, EnvironmentStateHolder("", nullptr)};
        return deviceState;
    }

} // namespace

Y_UNIT_TEST_SUITE(AliceDeviceStateTest) {
    Y_UNIT_TEST_F(checkDeviceIdField, QuasarUnitTestFixture) {
        std::string deviceId("deadbeef");
        AliceConfig aliceConfig(createAliceConfig(getDeviceForTests(), false));
        AliceDeviceState deviceState(createDeviceState(deviceId));

        const Json::Value result{deviceState.formatJson()};
        UNIT_ASSERT_EQUAL(result["device_id"].asString(), deviceId);
    }

    Y_UNIT_TEST_F(checkMicsMutedField, QuasarUnitTestFixture) {
        AliceConfig aliceConfig(createAliceConfig(getDeviceForTests(), false));
        AliceDeviceState deviceState(createDeviceState(""));

        Json::Value result{deviceState.formatJson()};
        UNIT_ASSERT(!result.isMember("mics_muted"));

        NAlice::TCapabilityHolder capability;
        capability.MutableDeviceStateCapability()->MutableState()->MutableDeviceState()->SetMicsMuted(true);
        deviceState.onCapabilityStateChanged(nullptr, capability);

        result = deviceState.formatJson();
        UNIT_ASSERT(result["mics_muted"].asBool());

        capability.MutableDeviceStateCapability()->MutableState()->MutableDeviceState()->SetMicsMuted(false);
        deviceState.onCapabilityStateChanged(nullptr, capability);
        result = deviceState.formatJson();
        UNIT_ASSERT(!result["mics_muted"].asBool());
    }

    Y_UNIT_TEST_F(checkDndEnabledField, QuasarUnitTestFixture) {
        AliceConfig aliceConfig(createAliceConfig(getDeviceForTests(), false));
        AliceDeviceState deviceState(createDeviceState(""));

        Json::Value result;
        result = deviceState.formatJson();
        UNIT_ASSERT(!result["dnd_enabled"].asBool());

        deviceState.setDndEnabled(true);
        result = deviceState.formatJson();
        UNIT_ASSERT(result["dnd_enabled"].asBool());

        deviceState.setDndEnabled(false);
        result = deviceState.formatJson();
        UNIT_ASSERT(!result["dnd_enabled"].asBool());
    }

    Y_UNIT_TEST_F(checkBatteryPowerLeftField, QuasarUnitTestFixture) {
        AliceConfig aliceConfig(createAliceConfig(getDeviceForTests(), false));
        AliceDeviceState deviceState(createDeviceState(""));

        Json::Value result;
        result = deviceState.formatJson();
        UNIT_ASSERT(!result.isMember("battery"));

        NAlice::TCapabilityHolder capability;
        capability.MutableDeviceStateCapability()->MutableState()->MutableDeviceState()->MutableBattery()->SetPercent(5);
        deviceState.onCapabilityStateChanged(nullptr, capability);

        result = deviceState.formatJson();
        UNIT_ASSERT_EQUAL(result["battery"]["percent"].asInt(), 5);
    }

    Y_UNIT_TEST_F(checkVolumeStateFields, QuasarUnitTestFixture) {
        AliceConfig aliceConfig(createAliceConfig(getDeviceForTests(), false));
        AliceDeviceState deviceState(createDeviceState(""));

        Json::Value result;
        result = deviceState.formatJson();
        UNIT_ASSERT(!result.isMember("sound_level"));
        UNIT_ASSERT(!result.isMember("sound_muted"));
        UNIT_ASSERT(!result.isMember("sound_max_level"));

        NAlice::TCapabilityHolder capability;
        capability.MutableDeviceStateCapability()->MutableState()->MutableDeviceState()->SetSoundMuted(false);
        capability.MutableDeviceStateCapability()->MutableState()->MutableDeviceState()->SetSoundLevel(3);
        capability.MutableDeviceStateCapability()->MutableState()->MutableDeviceState()->SetSoundMaxLevel(10);
        deviceState.onCapabilityStateChanged(nullptr, capability);

        result = deviceState.formatJson();
        UNIT_ASSERT_EQUAL(result["sound_level"].asInt(), 3);
        UNIT_ASSERT_EQUAL(result["sound_max_level"].asInt(), 10);
        UNIT_ASSERT(!result["sound_muted"].asBool());
    }

    Y_UNIT_TEST_F(checkVideoPausedState, QuasarUnitTestFixture) {
        AliceConfig aliceConfig(createAliceConfig(getDeviceForTests(), false));
        AliceDeviceState deviceState(createDeviceState(""));

        Json::Value result;
        result = deviceState.formatJson();
        UNIT_ASSERT(!result.isMember("video"));

        NAlice::TCapabilityHolder capability;
        capability.MutableDeviceStateCapability()->MutableState()->MutableDeviceState()->MutableVideo()->MutablePlayer()->SetPause(true);
        deviceState.onCapabilityStateChanged(nullptr, capability);

        result = deviceState.formatJson();
        UNIT_ASSERT(result.isMember("video"));
        UNIT_ASSERT(result["video"].isMember("player"));
        UNIT_ASSERT(result["video"]["player"].isMember("pause"));
        UNIT_ASSERT(result["video"]["player"]["pause"].asBool());

        capability.MutableDeviceStateCapability()->MutableState()->MutableDeviceState()->MutableVideo()->MutablePlayer()->SetPause(false);
        deviceState.onCapabilityStateChanged(nullptr, capability);

        result = deviceState.formatJson();
        UNIT_ASSERT(result.isMember("video"));
        UNIT_ASSERT(result["video"].isMember("player"));
        UNIT_ASSERT(result["video"]["player"].isMember("pause"));
        UNIT_ASSERT(!result["video"]["player"]["pause"].asBool());
    }

    Y_UNIT_TEST_F(checkTvInterfaceState, QuasarUnitTestFixture) {
        AliceConfig aliceConfig(createAliceConfig(getDeviceForTests(), false));
        AliceDeviceState deviceState(createDeviceState(""));

        Json::Value result;
        result = deviceState.formatJson();
        YIO_LOG_INFO("1: " << jsonToString(result));
        UNIT_ASSERT(!result.isMember("video"));

        // check legacy screen state
        NAlice::TCapabilityHolder state;
        state.MutableDeviceStateCapability()->MutableState()->MutableDeviceState()->MutableVideo()->SetCurrentScreen("search_results");
        deviceState.onCapabilityStateChanged(nullptr, state);
        result = deviceState.formatJson();
        YIO_LOG_INFO("2: " << jsonToString(result));
        UNIT_ASSERT(result.isMember("video"));
        UNIT_ASSERT(result["video"].isMember("current_screen") && result["video"]["current_screen"].asString() == "search_results");
        UNIT_ASSERT(!result["video"].isMember("tv_interface_state"));

        // check empty tv interface state
        state.Clear();
        auto* videoState = state.MutableDeviceStateCapability()->MutableState()->MutableDeviceState()->MutableVideo();
        videoState->MutableTvInterfaceState();
        deviceState.onCapabilityStateChanged(nullptr, state);
        result = deviceState.formatJson();
        YIO_LOG_INFO("3: " << jsonToString(result));
        UNIT_ASSERT(result.isMember("video"));
        UNIT_ASSERT(!result["video"].isMember("current_screen"));
        UNIT_ASSERT(result["video"].isMember("tv_interface_state"));

        // fill some interface values

        videoState->SetCurrentScreen("video_player");
        videoState->MutableTvInterfaceState()->SetInitialReqId("request_id");
        auto searchScreen = videoState->mutable_tvinterfacestate()->mutable_searchresultsscreen();
        searchScreen->add_suggests("suggest_1");
        searchScreen->set_searchquery("query");
        auto galleries = searchScreen->add_galleries();
        galleries->set_title("title");
        galleries->set_visible(true);
        deviceState.onCapabilityStateChanged(nullptr, state);
        result = deviceState.formatJson();
        YIO_LOG_INFO("4: " << jsonToString(result));
        UNIT_ASSERT(result.isMember("video"));
        UNIT_ASSERT(result["video"].isMember("current_screen") && result["video"]["current_screen"].asString() == "video_player");
        UNIT_ASSERT(result["video"].isMember("tv_interface_state"));

        const auto video = result["video"];
        UNIT_ASSERT(video["tv_interface_state"].isMember("initial_reqid"));
        UNIT_ASSERT(video["tv_interface_state"]["initial_reqid"].asString() == "request_id");
        UNIT_ASSERT(video["tv_interface_state"].isMember("search_results_screen"));

        const auto screen = video["tv_interface_state"]["search_results_screen"];
        UNIT_ASSERT(screen.isMember("suggests") && screen["suggests"].isArray() && screen["suggests"].size() == 1);
        UNIT_ASSERT(screen["suggests"][0].asString() == "suggest_1");
        UNIT_ASSERT(screen.isMember("search_query") && screen["search_query"].asString() == "query");
        UNIT_ASSERT(screen.isMember("galleries") && screen["galleries"].isArray() && screen["galleries"].size() == 1);

        const auto g = screen["galleries"][0];
        UNIT_ASSERT(g.isMember("title") && g["title"].asString() == "title");
        UNIT_ASSERT(g.isMember("visible") && g["visible"].asBool());
        UNIT_ASSERT(!g.isMember("items"));

        // drop tv state and check if it was removed
        videoState->ClearTvInterfaceState();
        deviceState.onCapabilityStateChanged(nullptr, state);
        result = deviceState.formatJson();
        YIO_LOG_INFO("5: " << jsonToString(result));
        UNIT_ASSERT(result.isMember("video"));
        UNIT_ASSERT(!result["video"].isMember("tv_interface_state"));
    }

    Y_UNIT_TEST_F(checkPackagesState, QuasarUnitTestFixture) {
        AliceConfig aliceConfig(createAliceConfig(getDeviceForTests(), false));
        AliceDeviceState deviceState(createDeviceState(""));

        Json::Value result;
        result = deviceState.formatJson();

        UNIT_ASSERT(!result.isMember("packages"));

        NAlice::TCapabilityHolder capability;
        auto* state = capability.MutableDeviceStateCapability()->MutableState()->MutableDeviceState()->MutablePackagesState();
        auto* newPackage = state->AddInstalled();
        newPackage->SetMainActivity("main.activity");
        newPackage->MutablePackageInfo()->SetName("ru.yandex.test.package");
        newPackage->MutablePackageInfo()->SetHumanReadableName("Test Package");

        auto* newPackage2 = state->AddInstalled();
        newPackage2->SetMainActivity("main.activity2");
        newPackage2->MutablePackageInfo()->SetName("ru.yandex.test.package2");
        newPackage2->MutablePackageInfo()->SetHumanReadableName("Test Package2");

        deviceState.onCapabilityStateChanged(nullptr, capability);
        result = deviceState.formatJson();
        UNIT_ASSERT(result.isMember("packages"));
        const auto packages = result["packages"];
        UNIT_ASSERT(packages.isMember("installed") && packages["installed"].isArray());
        const auto installed = packages["installed"];
        UNIT_ASSERT(installed.size() == 2);

        {
            const auto package = installed[0];
            UNIT_ASSERT(package.isMember("main_activity") && package["main_activity"].asString() == "main.activity");
            UNIT_ASSERT(package.isMember("package_info"));
            const auto packageInfo = package["package_info"];
            UNIT_ASSERT(packageInfo.isMember("human_readable_name") &&
                        packageInfo["human_readable_name"].asString() == "Test Package");
            UNIT_ASSERT(packageInfo.isMember("name") && packageInfo["name"].asString() == "ru.yandex.test.package");
        }

        {
            auto package = installed[1];
            UNIT_ASSERT(package.isMember("main_activity") && package["main_activity"].asString() == "main.activity2");
            UNIT_ASSERT(package.isMember("package_info"));
            const auto packageInfo = package["package_info"];
            UNIT_ASSERT(packageInfo.isMember("human_readable_name") &&
                        packageInfo["human_readable_name"].asString() == "Test Package2");
            UNIT_ASSERT(packageInfo.isMember("name") && packageInfo["name"].asString() == "ru.yandex.test.package2");
        }
    }

    Y_UNIT_TEST_F(checkActiveActions, QuasarUnitTestFixture) {
        AliceConfig aliceConfig(createAliceConfig(getDeviceForTests(), false));
        AliceDeviceState deviceState(createDeviceState(""));

        NAlice::TDoNothingSemanticFrame frame;
        NAlice::TTypedSemanticFrame typedFrame;
        typedFrame.mutable_donothingsemanticframe()->CopyFrom(frame);
        NAlice::TConditionalAction conditionalAction;
        conditionalAction.mutable_conditionalsemanticframe()->CopyFrom(typedFrame);
        NAlice::TDeviceState::TActiveActions::TScreenConditionalActions screenConditionalActions;
        screenConditionalActions.add_conditionalactions()->CopyFrom(conditionalAction);
        NAlice::TDeviceState::TActiveActions activeActions;
        activeActions.mutable_screenconditionalactions()->insert(google::protobuf::MapPair(TString("main"), screenConditionalActions));
        deviceState.setActiveActions(activeActions);

        auto deviceStateJson = deviceState.formatJson();
        UNIT_ASSERT(deviceStateJson.isMember("active_actions"));
        UNIT_ASSERT(deviceStateJson["active_actions"].isMember("screen_conditional_action"));
        UNIT_ASSERT(deviceStateJson["active_actions"]["screen_conditional_action"].isMember("main"));
        UNIT_ASSERT(deviceStateJson["active_actions"]["screen_conditional_action"]["main"].isMember("conditional_actions"));
        UNIT_ASSERT_EQUAL(deviceStateJson["active_actions"]["screen_conditional_action"]["main"]["conditional_actions"].size(), 1);
        UNIT_ASSERT(deviceStateJson["active_actions"]["screen_conditional_action"]["main"]["conditional_actions"][0].isMember("conditional_semantic_frame"));
        UNIT_ASSERT(deviceStateJson["active_actions"]["screen_conditional_action"]["main"]["conditional_actions"][0]["conditional_semantic_frame"].isMember("do_nothing_semantic_frame"));
    }

    Y_UNIT_TEST_F(checkActiveActionsEvironmentState, QuasarUnitTestFixture) {
        AliceConfig aliceConfig(createAliceConfig(getDeviceForTests(), false));
        AliceDeviceState deviceState(createDeviceState(""));

        deviceState.setActiveActionSemanticFrame("{\"main\": {\"typed_semantic_frame\": \"some_frame\"}}");

        auto deviceStateJson = deviceState.formatJson();
        UNIT_ASSERT(deviceStateJson.isMember("active_actions"));
        UNIT_ASSERT(deviceStateJson["active_actions"].isMember("semantic_frames"));
        UNIT_ASSERT(deviceStateJson["active_actions"]["semantic_frames"].isMember("main"));
        UNIT_ASSERT(deviceStateJson["active_actions"]["semantic_frames"]["main"].isMember("typed_semantic_frame"));
        UNIT_ASSERT_EQUAL(deviceStateJson["active_actions"]["semantic_frames"]["main"]["typed_semantic_frame"], "some_frame");
    }

    Y_UNIT_TEST_F(checkActiveActionsEvironmentStateOverridesActiveActions, QuasarUnitTestFixture) {
        AliceConfig aliceConfig(createAliceConfig(getDeviceForTests(), false));
        AliceDeviceState deviceState(createDeviceState(""));
        deviceState.getEnvironmentState().updateLocalDevice();

        NAlice::TDoNothingSemanticFrame frame;
        NAlice::TTypedSemanticFrame typedFrame;
        typedFrame.mutable_donothingsemanticframe()->CopyFrom(frame);
        NAlice::TSemanticFrameRequestData frameData;
        frameData.mutable_typedsemanticframe()->CopyFrom(typedFrame);
        NAlice::TDeviceState::TActiveActions activeActions;
        activeActions.mutable_semanticframes()->insert(google::protobuf::MapPair(TString("main"), frameData));

        NAlice::TConditionalAction conditionalAction;
        conditionalAction.mutable_conditionalsemanticframe()->CopyFrom(typedFrame);
        NAlice::TDeviceState::TActiveActions::TScreenConditionalActions screenConditionalActions;
        screenConditionalActions.add_conditionalactions()->CopyFrom(conditionalAction);
        activeActions.mutable_screenconditionalactions()->insert(google::protobuf::MapPair(TString("main"), screenConditionalActions));

        deviceState.setActiveActions(activeActions);
        deviceState.setActiveActionSemanticFrame("{\"main\": {\"typed_semantic_frame\": \"some_frame\"}}");

        auto deviceStateJson = deviceState.formatJson();
        UNIT_ASSERT(deviceStateJson.isMember("active_actions"));
        UNIT_ASSERT(deviceStateJson["active_actions"].isMember("semantic_frames"));
        UNIT_ASSERT(deviceStateJson["active_actions"]["semantic_frames"].isMember("main"));
        UNIT_ASSERT(deviceStateJson["active_actions"]["semantic_frames"]["main"].isMember("typed_semantic_frame"));
        UNIT_ASSERT_EQUAL(deviceStateJson["active_actions"]["semantic_frames"]["main"]["typed_semantic_frame"], "some_frame");

        UNIT_ASSERT(deviceStateJson.isMember("active_actions"));
        UNIT_ASSERT(deviceStateJson["active_actions"].isMember("screen_conditional_action"));
        UNIT_ASSERT(deviceStateJson["active_actions"]["screen_conditional_action"].isMember("main"));
        UNIT_ASSERT(deviceStateJson["active_actions"]["screen_conditional_action"]["main"].isMember("conditional_actions"));
        UNIT_ASSERT_EQUAL(deviceStateJson["active_actions"]["screen_conditional_action"]["main"]["conditional_actions"].size(), 1);
        UNIT_ASSERT(deviceStateJson["active_actions"]["screen_conditional_action"]["main"]["conditional_actions"][0].isMember("conditional_semantic_frame"));
        UNIT_ASSERT(deviceStateJson["active_actions"]["screen_conditional_action"]["main"]["conditional_actions"][0]["conditional_semantic_frame"].isMember("do_nothing_semantic_frame"));

        auto environmentStateJson = deviceState.getEnvironmentState().formatJson();
        UNIT_ASSERT(environmentStateJson.isMember("devices"));
        UNIT_ASSERT_EQUAL(environmentStateJson["devices"].size(), 1);
        UNIT_ASSERT(environmentStateJson["devices"][0].isMember("device_state"));
        UNIT_ASSERT(environmentStateJson["devices"][0]["device_state"].isMember("active_actions"));
        UNIT_ASSERT(environmentStateJson["devices"][0]["device_state"]["active_actions"].isMember("screen_conditional_action"));
        UNIT_ASSERT(environmentStateJson["devices"][0]["device_state"]["active_actions"]["screen_conditional_action"].isMember("main"));
        UNIT_ASSERT(environmentStateJson["devices"][0]["device_state"]["active_actions"]["screen_conditional_action"]["main"].isMember("conditional_actions"));
        UNIT_ASSERT_EQUAL(environmentStateJson["devices"][0]["device_state"]["active_actions"]["screen_conditional_action"]["main"]["conditional_actions"].size(), 1);
        UNIT_ASSERT(environmentStateJson["devices"][0]["device_state"]["active_actions"]["screen_conditional_action"]["main"]["conditional_actions"][0].isMember("conditional_semantic_frame"));
        UNIT_ASSERT(environmentStateJson["devices"][0]["device_state"]["active_actions"]["screen_conditional_action"]["main"]["conditional_actions"][0]["conditional_semantic_frame"].isMember("do_nothing_semantic_frame"));
    }
}
