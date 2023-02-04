#include <library/cpp/testing/unittest/registar.h>
#include <library/cpp/testing/gmock_in_unittest/gmock.h>
#include <maps/libs/common/include/base64.h>

#include <maps/automotive/remote_access/pandora_emulator/lib/pandora_emulator.h>
#include <maps/automotive/remote_access/pandora_emulator/lib/factory_singleton.h>

#include <maps/automotive/remote_access/pandora_emulator/lib/in_memory_storage.h>
#include <maps/automotive/remote_access/pandora_emulator/lib/db_storage.h>

#include <cstdlib>
#include <future>

class TPandoraEmulatorTest : public NUnitTest::TTestBase {
public:
    TPandoraEmulatorTest()
    {
        using namespace maps::automotive::pandora_emulator;
        const auto factory = getFactory();
        const auto dbSettingPath = std::getenv("PANDORA_EMULATOR_DATABASE_CONFIG");
        if (dbSettingPath) {
            factory->addSingleton<DbStorage>()->initialize(maps::json::Value::fromFile(dbSettingPath));
        } else {
            factory->addSingleton<InMemoryStorage>();
        }
    }
};

Y_UNIT_TEST_SUITE_IMPL(test_pandora_emulator, TPandoraEmulatorTest) {

using namespace maps::automotive;
using namespace maps::automotive::pandora;
using namespace maps::automotive::pandora_emulator;

namespace {

const uint64_t CAR_ID = 9876543230;
const std::string CAR_ID_STR = std::to_string(CAR_ID);

std::string getOauthToken(PandoraEmulator& emulator)
{
    const std::string oauthCredentials("oauthClient:superSecretKey");
    const TArrayRef<const char> oauthDataArray(oauthCredentials.data(), oauthCredentials.size());
    const auto authCredentials = maps::base64Encode(oauthDataArray);

    const auto oauthResponse = emulator.postOauth(Request({
        {"grant_type", "client_credentials"},
        {"scope", "tracks,events,settings"},
    }, {
        {"Authorization", "Basic " + authCredentials},
    }));
    return oauthResponse.body["access_token"].as<std::string>();
}

std::string getAuthorizedAccessToken(
    PandoraEmulator& emulator,
    const std::string& carIdStr = CAR_ID_STR,
    const std::string& password = "password")
{
    const auto accessToken = getOauthToken(emulator);

    emulator.postLogin(Request({
        {"login", carIdStr},
        {"password", password},
        {"access_token", accessToken},
    }));

    return accessToken;
}

Request createSendCommandRequest(PandoraEmulator::Command command, const std::string& accessToken)
{
    return Request({
        {"access_token", accessToken},
        {"id", CAR_ID_STR},
        {"command", std::to_string(static_cast<uint8_t>(command))},
    });
}

Response getUpdates(
    PandoraEmulator& emulator,
    const std::string& accessToken,
    uint64_t ts,
    const std::string& carIdStr = CAR_ID_STR)
{
    return emulator.getUpdates(Request({
        {"access_token", accessToken},
        {"id", carIdStr},
        {"ts", std::to_string(ts)},
    }));
}

Response getLenta(
    PandoraEmulator& emulator,
    const std::string& accessToken,
    std::optional<std::chrono::seconds> from = std::nullopt,
    std::optional<std::chrono::seconds> to = std::nullopt)
{
    Request getLentaRequest({
        {"access_token", accessToken},
        {"id", CAR_ID_STR}
    });
    if (from) {
        getLentaRequest.addParameter("from", std::to_string(from->count()));
    }
    if (to) {
        getLentaRequest.addParameter("to", std::to_string(to->count()));
    }
    return emulator.getLenta(getLentaRequest);
}

BitState getBitState(PandoraEmulator& emulator, const std::string& accessToken)
{
    const auto response = getUpdates(emulator, accessToken, 0);
    const auto& stats = response.body["stats"][CAR_ID_STR];
    return BitState(std::stoull(stats["bit_state_1"].as<std::string>()));
}

struct CommandEventDescriptor {
    PandoraEmulator::Command command;
    pandora::EventId1 eventId1;
    pandora::EventId2 eventId2;
};

struct CommandStateDescriptor {
    PandoraEmulator::Command command;
    BitState::FlagIndex flagIndex;
    bool state;
};

template<typename T = int64_t>
struct EditSettingDescriptor {
    std::string settingName;
    T primaryValue = 1;
    T clearValue = 0;
};

}

Y_UNIT_TEST(test_user_and_car_add_works)
{
    PandoraEmulator emulator;

    const auto carId = 1;
    const auto userId = 1;
    const auto userPassword = "pwd";

    getFactory()->getInterface<Storage>()->removeUser(userId);

    emulator.postCarAndUser(
        Request(
            {
                {"car_id", std::to_string(carId)},
                {"user_id", std::to_string(userId)},
                {"user_password", userPassword}
            }));

    const auto token = getAuthorizedAccessToken(emulator, std::to_string(carId), userPassword);
    ASSERT_GT(token.size(), 0u);
}

Y_UNIT_TEST(test_all_cars_available)
{
    PandoraEmulator emulator;

    const auto carsCount = 10;
    for (auto carId = CAR_ID; carId < CAR_ID + carsCount; ++carId) {
        const auto carIdStr = std::to_string(carId);
        const auto accessToken = getAuthorizedAccessToken(emulator, carIdStr);
        const auto updates = getUpdates(emulator, accessToken, 0, carIdStr);
        ASSERT_TRUE(updates.body["stats"].hasField(carIdStr));
    }
}

Y_UNIT_TEST(test_granted_tokens_are_responded)
{
    PandoraEmulator emulator;

    std::set<std::string> grantedTokens;
    const size_t tokenCount = 10;
    for (size_t i = 0; i < tokenCount; ++i) {
        const auto token = getOauthToken(emulator);
        grantedTokens.insert(token);
    }

    std::set<std::string> respondedGrantedTokens;
    const auto tokens = emulator.getTokens(Request());
    const auto& grantedTokensJson = tokens.body["granted_tokens"];
    for (const auto& token : grantedTokensJson) {
        const auto tokenStr = token.as<std::string>();
        if (grantedTokens.count(tokenStr) != 0) {
            respondedGrantedTokens.insert(tokenStr);
        }
    }
    ASSERT_EQ(grantedTokens, respondedGrantedTokens);
}

Y_UNIT_TEST(test_assigned_tokens_are_responded)
{
    PandoraEmulator emulator;

    std::unordered_map<std::string, uint64_t> assignedCars;

    const auto carsCount = 10;
    for (auto carId = CAR_ID; carId < CAR_ID + carsCount; ++carId) {
        const auto carIdStr = std::to_string(carId);
        const auto accessToken = getAuthorizedAccessToken(emulator, carIdStr);
        assignedCars.emplace(accessToken, carId);
    }

    std::unordered_map<std::string, uint64_t> respondedAssignedCars;
    const auto tokens = emulator.getTokens(Request());
    const auto& assignedCarsJson = tokens.body["assigned_cars"];
    for (const auto& token : assignedCarsJson.fields()) {
        const auto userId = assignedCarsJson[token].as<uint64_t>();
        if (assignedCars.count(token) != 0) {
            respondedAssignedCars.emplace(token, userId);
        }
    }
    ASSERT_EQ(assignedCars, respondedAssignedCars);
}

Y_UNIT_TEST(test_all_commands_succeed)
{
    std::vector<PandoraEmulator::Command> commands = {
        PandoraEmulator::Lock,
        PandoraEmulator::Unlock,
        PandoraEmulator::StopEngine,
        PandoraEmulator::StartEngine,
        PandoraEmulator::EnableTracking,
        PandoraEmulator::DisableTracking,
        PandoraEmulator::Extra1,
        PandoraEmulator::Extra2,
        PandoraEmulator::KeepConnection,
        PandoraEmulator::CloseConnection,
        PandoraEmulator::Check,
        PandoraEmulator::EnableActiveGuard,
        PandoraEmulator::DisableActiveGuard,
        PandoraEmulator::EnableHeating,
        PandoraEmulator::DisableHeating,
        PandoraEmulator::Beep,
        PandoraEmulator::Blink,
        PandoraEmulator::EnableTimerChannel,
        PandoraEmulator::DisableTimerChannel,
        PandoraEmulator::Trunk,
        PandoraEmulator::EnableServiceMode,
        PandoraEmulator::DisableServiceMode
    };

    PandoraEmulator emulator;
    const auto accessToken = getAuthorizedAccessToken(emulator);
    for (const auto& command : commands) {
        const auto response = emulator.postCommand(createSendCommandRequest(command, accessToken));

        ASSERT_EQ(response.status.code(), yacare::HTTPStatus::OK.code());
        ASSERT_EQ(response.body["action_result"][CAR_ID_STR].as<std::string>(), "sent");
    }
}

Y_UNIT_TEST(test_commands_cause_events)
{
    std::vector<CommandEventDescriptor> commandsAndEvents = {
        {PandoraEmulator::Lock, pandora::EventId1::Lock, pandora::EventId2Code::Lock::InternetService },
        {PandoraEmulator::Unlock, pandora::EventId1::Unlock, pandora::EventId2Code::Unlock::InternetService},
        {PandoraEmulator::StopEngine, pandora::EventId1::EngineStop, pandora::EventId2Code::EngineStop::InternetService },
        {PandoraEmulator::StartEngine, pandora::EventId1::EngineStart, pandora::EventId2Code::EngineStart::InternetService},
        {PandoraEmulator::EnableTracking, pandora::EventId1::TrackingEnable, pandora::EventId2Code::TrackingEnable::InternetService },
        {PandoraEmulator::DisableTracking, pandora::EventId1::TrackingDisable, pandora::EventId2Code::TrackingDisable::InternetService },
        {PandoraEmulator::EnableActiveGuard, pandora::EventId1::ActiveGuardEnable, pandora::EventId2Code::ActiveGuardEnable::InternetService },
        {PandoraEmulator::DisableActiveGuard, pandora::EventId1::ActiveGuardDisable, pandora::EventId2Code::ActiveGuardDisable::InternetService },
        {PandoraEmulator::EnableHeating, pandora::EventId1::HeatingEnable, pandora::EventId2Code::HeatingEnable::InternetService },
        {PandoraEmulator::DisableHeating, pandora::EventId1::HeatingDisable, pandora::EventId2Code::HeatingDisable::InternetService },
        {PandoraEmulator::EnableServiceMode, pandora::EventId1::ServiceModeEnable, pandora::EventId2Code::ServiceModeEnable::WatchCommand },
    };

    PandoraEmulator emulator;
    const auto accessToken = getAuthorizedAccessToken(emulator);
    for (const auto& commandAndEvent : commandsAndEvents) {
        if (commandAndEvent.command == PandoraEmulator::EnableServiceMode) {
            auto bitState = getBitState(emulator, accessToken);
            bitState.reset(BitState::Locked);
            bitState.set(BitState::Engine);
            Request editRequest({
                {"car_id", CAR_ID_STR},
                {"bitState", std::to_string(bitState.getState())}
            });
            emulator.postCarInfo(editRequest);
        }

        emulator.postCommand(createSendCommandRequest(commandAndEvent.command, accessToken));
        const auto response = emulator.getUpdates(Request({
            {"access_token", accessToken},
            {"id", CAR_ID_STR},
            {"ts", "1"}
        }));
        const auto& eventRecordObj = response.body["lenta"][0]["obj"];
        ASSERT_EQ(eventRecordObj["eventid1"].as<uint8_t>(), static_cast<uint8_t>(commandAndEvent.eventId1));
        ASSERT_EQ(eventRecordObj["eventid2"].as<uint8_t>(), static_cast<uint8_t>(commandAndEvent.eventId2));
    }
}

Y_UNIT_TEST(test_commands_change_state)
{
    std::vector<CommandStateDescriptor> commandsAndStates = {
        {PandoraEmulator::Lock, BitState::Locked, true },
        {PandoraEmulator::Unlock, BitState::Locked, false},
        {PandoraEmulator::StopEngine, BitState::Engine, false},
        {PandoraEmulator::StartEngine, BitState::Engine, true},
        {PandoraEmulator::EnableTracking, BitState::Tracking, true},
        {PandoraEmulator::DisableTracking, BitState::Tracking, false},
        {PandoraEmulator::EnableActiveGuard, BitState::ActiveSecure, true},
        {PandoraEmulator::DisableActiveGuard, BitState::ActiveSecure, false},
        {PandoraEmulator::EnableHeating, BitState::Heat, true},
        {PandoraEmulator::DisableHeating, BitState::Heat, false},
        {PandoraEmulator::EnableServiceMode, BitState::ServiceOn, true},
        {PandoraEmulator::DisableServiceMode, BitState::ServiceOn, false}
    };

    PandoraEmulator emulator;
    const auto accessToken = getAuthorizedAccessToken(emulator);
    for (const auto& commandAndState : commandsAndStates) {
        emulator.postCommand(createSendCommandRequest(commandAndState.command, accessToken));
        ASSERT_EQ(getBitState(emulator, accessToken).get(commandAndState.flagIndex), commandAndState.state);
    }
}

Y_UNIT_TEST(test_car_start_changes_rpm)
{
    PandoraEmulator emulator;
    const auto accessToken = getAuthorizedAccessToken(emulator);
    emulator.postCommand(createSendCommandRequest(PandoraEmulator::StartEngine, accessToken));
    const auto startedRpm = getUpdates(emulator, accessToken, 0).body["stats"][CAR_ID_STR]["engine_rpm"].as<int>();
    ASSERT_GT(startedRpm, 0);
    emulator.postCommand(createSendCommandRequest(PandoraEmulator::StopEngine, accessToken));
    const auto stoppedRpm = getUpdates(emulator, accessToken, 0).body["stats"][CAR_ID_STR]["engine_rpm"].as<int>();
    ASSERT_EQ(stoppedRpm, 0);
}

Y_UNIT_TEST(test_trunk_opens_and_closes)
{
    PandoraEmulator emulator;
    const auto accessToken = getAuthorizedAccessToken(emulator);
    const auto oldTrunkState = getBitState(emulator, accessToken).get(BitState::Trunk);
    emulator.postCommand(createSendCommandRequest(PandoraEmulator::Trunk, accessToken));
    ASSERT_EQ(getBitState(emulator, accessToken).get(BitState::Trunk), !oldTrunkState);
    emulator.postCommand(createSendCommandRequest(PandoraEmulator::Trunk, accessToken));
    ASSERT_EQ(getBitState(emulator, accessToken).get(BitState::Trunk), oldTrunkState);
}

Y_UNIT_TEST(test_edit_settings)
{
    std::vector<EditSettingDescriptor<>> valuesToSet = {
        {"enstart_enable"},
        {"engine_start_temp"},
        {"engine_start_voltage"},
        {"engine_start_by_period"},
        {"engine_start_by_day"},
        {"engine_start_temp_value", -20},
        {"engine_start_period", 10, 1},
        {"engine_stop_time"},
        {"engine_stop_temp"},
        {"engine_stop_time_value", 10},
        {"engine_stop_temp_value", 30},
        {"enstart_day0"},
        {"enstart_day0_time1", 500}
    };
    EditSettingDescriptor<double> voltageToSet = {
        "engine_start_voltage_value", 11.1, 10.2
    };
    const auto daysInWeek = 7;
    const auto schedulesInPandora = 2;
    for (unsigned int dayIndex = 1; dayIndex <= daysInWeek; ++dayIndex) {
        for (unsigned int scheduleIndex = 1; scheduleIndex <= schedulesInPandora; ++scheduleIndex) {
            valuesToSet.emplace_back(EditSettingDescriptor<>{
                "enstart_day" + std::to_string(dayIndex) + std::to_string(scheduleIndex)
            });
            const auto startTime = 500 + (scheduleIndex * 10 + dayIndex);
            valuesToSet.emplace_back(EditSettingDescriptor<>{
                "enstart_day" + std::to_string(dayIndex) + "_time" + std::to_string(scheduleIndex),
                500 + startTime
            });
        }
    }

    PandoraEmulator emulator;
    const auto accessToken = getAuthorizedAccessToken(emulator);

    auto performRequestAndCheck = [&](auto getIntParameter, auto getDoubleParameter) {
        Request editRequest({
            {"access_token", accessToken},
            {"id", CAR_ID_STR},
        });
        for (const auto& valueToSet : valuesToSet) {
            editRequest.addParameter(
                valueToSet.settingName,
                std::to_string(getIntParameter(valueToSet)));
        }
        editRequest.addParameter(voltageToSet.settingName, std::to_string(getDoubleParameter(voltageToSet)));
        emulator.putSettings(editRequest);

        const auto getSettingsResponse = emulator.getSettings(Request({
            {"access_token", accessToken},
            {"id", CAR_ID_STR},
        }));
        ASSERT_EQ(getSettingsResponse.status.code(), yacare::HTTPStatus::OK.code());
        const auto& alarmSettings = getSettingsResponse.body["device_settings"][CAR_ID_STR][0]["alarm_settings"];
        for (const auto& valueToSet : valuesToSet) {
            ASSERT_EQ(alarmSettings[valueToSet.settingName].as<int64_t>(), getIntParameter(valueToSet));
        }
        ASSERT_DOUBLE_EQ(alarmSettings[voltageToSet.settingName].as<double>(), getDoubleParameter(voltageToSet));
    };

    performRequestAndCheck(
        [](const EditSettingDescriptor<>& descriptor) { return descriptor.clearValue; },
        [](const EditSettingDescriptor<double>& descriptor) { return descriptor.clearValue; }
    );

    performRequestAndCheck(
        [](const EditSettingDescriptor<>& descriptor) { return descriptor.primaryValue; },
        [](const EditSettingDescriptor<double>& descriptor) { return descriptor.primaryValue; }
    );
}

Y_UNIT_TEST(test_login_success_with_good_credentials)
{
    PandoraEmulator emulator;
    const auto accessToken = getOauthToken(emulator);

    const auto response = emulator.postLogin(Request({
        {"access_token", accessToken},
        {"login", CAR_ID_STR},
        {"password", "password"},
    }));
    ASSERT_EQ(response.status.code(), yacare::HTTPStatus::OK.code());
    ASSERT_EQ(response.body["status"].as<std::string>(), "success");
}

Y_UNIT_TEST(test_login_failure_with_bad_credentials)
{
    PandoraEmulator emulator;
    const auto accessToken = getOauthToken(emulator);

    ASSERT_THROW(emulator.postLogin(Request({
        {"access_token", accessToken},
        {"login", ""},
        {"password", ""},
    })), PandoraError);
}

Y_UNIT_TEST(test_login_failure_with_bad_token)
{
    PandoraEmulator emulator;
    const auto accessToken = "bad_token";

    ASSERT_THROW(emulator.postLogin(Request({
        {"access_token", accessToken},
        {"login", CAR_ID_STR},
        {"password", "password"},
    })), PandoraError);
}

Y_UNIT_TEST(test_cant_perform_requests_without_login)
{
    PandoraEmulator emulator;
    const auto accessToken = getOauthToken(emulator);
    ASSERT_THROW(getUpdates(emulator, accessToken, 0), PandoraError);
    ASSERT_THROW(emulator.postCommand(createSendCommandRequest(PandoraEmulator::Lock, accessToken)), PandoraError);
    ASSERT_THROW(emulator.getSettings(Request({
        {"access_token", accessToken},
        {"id", CAR_ID_STR}
    })), PandoraError);
    ASSERT_THROW(emulator.putSettings(Request({
        {"access_token", accessToken},
        {"id", CAR_ID_STR},
        {"engine_stop_time_value", "11"}
    })), PandoraError);
}

Y_UNIT_TEST(test_lenta_limits)
{
    PandoraEmulator emulator;
    const auto accessToken = getAuthorizedAccessToken(emulator);

    const auto allLentaResponse = getLenta(emulator, accessToken);
    const auto& allLenta = allLentaResponse.body["lenta"];
    const std::chrono::seconds maxTs(allLenta[0]["obj"]["dtime_rec"].as<int64_t>());
    const std::chrono::seconds minTs(allLenta[allLenta.size() - 1]["obj"]["dtime_rec"].as<int64_t>());

    const auto maxLentaResponse = getLenta(emulator, accessToken, maxTs, maxTs + std::chrono::hours(1));
    const auto& maxLenta = maxLentaResponse.body["lenta"];
    ASSERT_LT(maxLenta.size(), allLenta.size());
    const auto minTimeInMaxLenta = maxLenta[maxLenta.size() - 1]["obj"]["dtime_rec"].as<int64_t>();
    ASSERT_EQ(minTimeInMaxLenta, maxTs.count());
    ASSERT_GT(minTimeInMaxLenta, minTs.count());

    const auto minLentaResponse = getLenta(emulator, accessToken, minTs - std::chrono::hours(1), minTs);
    const auto& minLenta = minLentaResponse.body["lenta"];
    ASSERT_LT(minLenta.size(), allLenta.size());
    const auto maxTimeInMinLenta = minLenta[0]["obj"]["dtime_rec"].as<int64_t>();
    ASSERT_EQ(maxTimeInMinLenta, minTs.count());
    ASSERT_LT(maxTimeInMinLenta, maxTs.count());
}

Y_UNIT_TEST(test_updates_doesnt_return_lenta_when_ts_is_0)
{
    PandoraEmulator emulator;
    const auto accessToken = getAuthorizedAccessToken(emulator);
    const auto updatesResponse = getUpdates(emulator, accessToken, 0);
    ASSERT_EQ(updatesResponse.body["lenta"].size(), size_t(0));
}

Y_UNIT_TEST(test_updates_lenta_limits)
{
    PandoraEmulator emulator;
    const auto accessToken = getAuthorizedAccessToken(emulator);

    const auto allLentaResponse = getLenta(emulator, accessToken);
    const auto& allLenta = allLentaResponse.body["lenta"];
    const std::chrono::seconds maxTs(allLenta[0]["obj"]["dtime_rec"].as<int64_t>());
    const std::chrono::seconds minTs(allLenta[allLenta.size() - 1]["obj"]["dtime_rec"].as<int64_t>());

    const auto maxUpdatesResponse = getUpdates(emulator, accessToken, maxTs.count());
    const auto& maxUpdatesLenta = maxUpdatesResponse.body["lenta"];
    ASSERT_EQ(maxUpdatesLenta[maxUpdatesLenta.size() - 1]["obj"]["dtime_rec"].as<int64_t>(), maxTs.count());

    const auto minUpdatesResponse = getUpdates(emulator, accessToken, (minTs - std::chrono::seconds(1)).count());
    ASSERT_EQ(minUpdatesResponse.body["lenta"].size(), allLenta.size());
}

Y_UNIT_TEST(test_car_lock_works)
{
    auto storage = getFactory()->getInterface<Storage>();
    const auto usingDatabase = dynamic_cast<DbStorage*>(storage.get());
    const std::chrono::milliseconds sleepDuration(usingDatabase ? 500 : 100);
    const size_t operationsCount = 5;
    PandoraEmulator emulator;

    const auto accessToken = getAuthorizedAccessToken(emulator);
    const auto userId = storage->getUserIdByToken(accessToken).value();
    const auto carId = storage->getCarIdOwnedByUser(userId).value();

    const auto carOperationFunc = [&](size_t id) {
        INFO() << "Start get car " << id;
        auto operation = storage->getStorageOperation();
        const auto mutableCar = storage->getMutableCarById(operation, carId);
        INFO() << "Start operation " << id;
        std::this_thread::sleep_for(sleepDuration);
        storage->save(std::move(operation), *mutableCar);
        INFO() << "End operation " << id;
    };

    auto begin = std::chrono::steady_clock::now();

    std::vector<std::future<void>> operations;
    for (size_t i = 0; i < operationsCount; ++i) {
        operations.push_back(std::async(std::launch::async, carOperationFunc, i));
    }
    for (auto& operation : operations) {
        operation.wait();
    }

    const auto end = std::chrono::steady_clock::now();
    const auto actualDuration = std::chrono::duration_cast<std::chrono::milliseconds>(end - begin);
    const auto expectedDuration = std::chrono::duration_cast<std::chrono::milliseconds>(
        sleepDuration * operationsCount);
    ASSERT_GE(actualDuration.count(), expectedDuration.count());
}

}