#include <library/cpp/testing/unittest/registar.h>
#include <library/cpp/testing/gmock_in_unittest/gmock.h>

#include <maps/automotive/remote_access/libs/data_types/car_information.h>
#include <maps/automotive/remote_access/libs/time/format.h>

#include <boost/format.hpp>

Y_UNIT_TEST_SUITE(test_car_information) {

using namespace maps::automotive;

const CarInformation DUMMY_CAR {
    .name_ = "car_name",
    .brand_ = "car_brand",
    .model_ = "car_model",
    .year_ = 2019,
    .plate_ = "A123BC",
    .uuid_ = "ef4e2b48-2b0d-4449-8852-bd8c8b6442c3",
};

constexpr const char* DUMMY_DEVICE_JSON_TEMPLATE = R"(
[
    {
        %1%
        %2%
        "auto_marka": "",
        "auto_model": "",
        "car_type": 0,
        "color": "rgb(255,255,255) ",
        "features": {
            "active_security": 1,
            "auto_check": 1,
            "autostart": 1,
            "beep": 1,
            "bluetooth": 1,
            "channel": 1,
            "connection": 1,
            "custom_phones": 1,
            "events": 1,
            "extend_props": 1,
            "heater": 1,
            "keep_alive": 1,
            "light": 1,
            "notification": 1,
            "schedule": 1,
            "sensors": 1,
            "tracking": 1,
            "trunk": 1,
            "value_100": 1
        },
        "firmware": "1.76",
        "id": 1,
        "is_shared": false,
        "model": "X-1911BT",
        "name": "RAV4",
        "owner_id": -1,
        "permissions": {
            "control": 3,
            "detach": 3,
            "events": 3,
            "oauth": 3,
            "rules": 3,
            "settings": 3,
            "settings_save": 3,
            "status": 3,
            "tanks": 3,
            "tanks_save": 3,
            "tracks": 3
        },
        "photo": "Yp4Zg7CeauNa8ywWUGWNg==",
        "start_ownership": 1558342217,
        "tanks": [],
        "type": "alarm",
        "voice_version": "1.23F361"
    }
]
    )";

struct DummyDeviceConfiguration {
    std::string fuelTank = R"("fuel_tank": 50,)";
    std::string phone = R"("phone": "+79876543210",)";
};

std::string getDeviceJson(DummyDeviceConfiguration configuration = DummyDeviceConfiguration()) {
    return (
        boost::format(DUMMY_DEVICE_JSON_TEMPLATE)
        % configuration.fuelTank
        % configuration.phone
    ).str();
}

const std::string DUMMY_DEVICE_JSON = getDeviceJson();

constexpr const char* DUMMY_UPDATES_JSON = R"(
{
    "lenta": [

    ],
    "stats": {
        "1": {
            %1%
            %3%
            %4%
            %5%
            %6%
            "bit_state_1": "134218625",
            "brelok": 0,
            "bunker": 0,
            "cabin_temp": 15,
            "dtime": 1572934711,
            "dtime_rec": 1572934711,
            "engine_remains": 0,
            "engine_rpm": 0,
            "engine_temp": 13,
            "evaq": 0,
            "ex_status": 0,
            "gsm_level": 2,
            "land": 0,
            "metka": 0,
            "mileage": "2797.537411",
            "move": 0,
            "online": 1,
            "out_temp": 0,
            "relay": 0,
            "rot": 360,
            "smeter": 0,
            "speed": 0,
            "tconsum": 0,
            "voltage": 11.4,
            "props": []
        }
    },
    "time": {
        "1": {
            %2%
            "onlined": 1572934711,
            "online": 1572934711,
            "setting": 1572934711
        }
    },
    "ts": 1572934711
}
    )";

struct DummyUpdatesConfiguration {
    std::string tanks = R"("tanks": [],)";
    std::string command = "";
    std::string x = R"("x": 55.73312,)";
    std::string y = R"("y": 37.589004,)";
    std::string balance = R"(
        "balance": {
            "cur": "RUB",
            "value": "464.520000"
        },)";
    std::string fuel = R"("fuel": 22,)";
};

std::string getUpdatesJson(DummyUpdatesConfiguration configuration = DummyUpdatesConfiguration()) {
    return (
        boost::format(DUMMY_UPDATES_JSON)
        % configuration.tanks
        % configuration.command
        % configuration.x
        % configuration.y
        % configuration.balance
        % configuration.fuel
    ).str();
}

constexpr const char* PANDORA_CAR_ID_STR = "1";

const double DUMMY_CAR_TANK = 50;
const double DUMMY_CAR_FUEL_PERCENTAGE = 22;
const double DUMMY_CAR_FUEL_LEVEL = DUMMY_CAR_TANK * DUMMY_CAR_FUEL_PERCENTAGE / 100.0;

double getExpectedKilometers(double fuelLevel, uint32_t consumption) {
    return (fuelLevel / consumption) * 100.0;
}

Y_UNIT_TEST(test_serialize_without_tanks)
{
    auto car = DUMMY_CAR;
    car.parseDetails(
        DUMMY_DEVICE_JSON,
        getUpdatesJson({.tanks = R"("tanks": [],)"}),
        PANDORA_CAR_ID_STR,
        CarInformation::LocationOnly::FALSE);

    const auto serializedCar = car.serialize();

    ASSERT_FALSE(serializedCar.hasField("avgFuelConsumption"));
    ASSERT_FALSE(serializedCar["fuelLevel"].hasField("kilometers"));
}

Y_UNIT_TEST(test_serialize_with_one_tank)
{
    auto car = DUMMY_CAR;
    car.parseDetails(
        DUMMY_DEVICE_JSON,
        getUpdatesJson({.tanks = R"(
        "tanks": [
            {
                "id": 1,
                "m": 1,
                "ras": 2,
                "ras_t": 1,
                "val": 30
            }
        ],)"}),
        PANDORA_CAR_ID_STR,
        CarInformation::LocationOnly::FALSE);

    const auto serializedCar = car.serialize();

    const auto expectedConsumption = 2;
    ASSERT_DOUBLE_EQ(serializedCar["avgFuelConsumption"].as<double>(), expectedConsumption);
    const auto expectedKilometers = getExpectedKilometers(DUMMY_CAR_FUEL_LEVEL, expectedConsumption);
    ASSERT_DOUBLE_EQ(serializedCar["fuelLevel"]["kilometers"].as<double>(), expectedKilometers);
}

Y_UNIT_TEST(test_serialize_with_two_tanks)
{
    auto car = DUMMY_CAR;
    car.parseDetails(
        DUMMY_DEVICE_JSON,
        getUpdatesJson({.tanks = R"(
        "tanks": [
            {
                "id": 1,
                "m": 1,
                "ras": 2,
                "ras_t": 1,
                "val": 30
            },
            {
                "id": 2,
                "m": 0,
                "ras": 4,
                "ras_t": 1,
                "val": 30
            }
        ],)"}),
        PANDORA_CAR_ID_STR,
        CarInformation::LocationOnly::FALSE);

    const auto serializedCar = car.serialize();

    const auto expectedConsumption = 6;
    ASSERT_DOUBLE_EQ(serializedCar["avgFuelConsumption"].as<double>(), expectedConsumption);
    const auto expectedKilometers = getExpectedKilometers(DUMMY_CAR_FUEL_LEVEL, expectedConsumption);
    ASSERT_DOUBLE_EQ(serializedCar["fuelLevel"]["kilometers"].as<double>(), expectedKilometers);
}

Y_UNIT_TEST(test_serialize_with_two_tanks_one_wrong)
{
    auto car = DUMMY_CAR;
    car.parseDetails(
        DUMMY_DEVICE_JSON,
        getUpdatesJson({.tanks = R"(
        "tanks": [
            {
                "id": 1,
                "m": 1,
                "ras": 2,
                "ras_t": 1,
                "val": 30
            },
            {
                "id": 2,
                "m": 0,
                "ras": 4,
                "ras_t": 2,
                "val": 30
            }
        ],)"}),
        PANDORA_CAR_ID_STR,
        CarInformation::LocationOnly::FALSE);

    const auto serializedCar = car.serialize();

    const auto expectedConsumption = 2;
    ASSERT_DOUBLE_EQ(serializedCar["avgFuelConsumption"].as<double>(), expectedConsumption);
    const auto expectedKilometers = getExpectedKilometers(DUMMY_CAR_FUEL_LEVEL, expectedConsumption);
    ASSERT_DOUBLE_EQ(serializedCar["fuelLevel"]["kilometers"].as<double>(), expectedKilometers);
}

Y_UNIT_TEST(test_serialize_with_one_wrong_tank)
{
    auto car = DUMMY_CAR;
    car.parseDetails(
        DUMMY_DEVICE_JSON,
        getUpdatesJson({.tanks = R"(
        "tanks": [
            {
                "id": 1,
                "m": 1,
                "ras": 2,
                "ras_t": 2,
                "val": 30
            }
        ],)"}),
        PANDORA_CAR_ID_STR,
        CarInformation::LocationOnly::FALSE);

    const auto serializedCar = car.serialize();

    ASSERT_FALSE(serializedCar.hasField("avgFuelConsumption"));
    ASSERT_FALSE(serializedCar["fuelLevel"].hasField("kilometers"));
}

Y_UNIT_TEST(test_serialize_with_last_command_timestamp)
{
    auto car = DUMMY_CAR;
    car.parseDetails(
        DUMMY_DEVICE_JSON,
        getUpdatesJson({.command = R"("command": 1572353026,)"}),
        PANDORA_CAR_ID_STR,
        CarInformation::LocationOnly::FALSE);

    const auto serializedCar = car.serialize();

    ASSERT_EQ(readTimeFromString(serializedCar["lastCommandTime"].as<std::string>()), 1572353026);
}

Y_UNIT_TEST(test_serialize_without_last_command_timestamp)
{
    auto car = DUMMY_CAR;
    car.parseDetails(
        DUMMY_DEVICE_JSON,
        getUpdatesJson({.command = ""}),
        PANDORA_CAR_ID_STR,
        CarInformation::LocationOnly::FALSE);

    const auto serializedCar = car.serialize();

    ASSERT_FALSE(serializedCar.hasField("lastCommandTime"));
}

Y_UNIT_TEST(test_serialize_with_ok_fields)
{
    auto car = DUMMY_CAR;
    car.parseDetails(
        getDeviceJson(),
        getUpdatesJson(),
        PANDORA_CAR_ID_STR,
        CarInformation::LocationOnly::FALSE);

    const auto serializedCar = car.serialize();
    ASSERT_DOUBLE_EQ(serializedCar["fuelLevel"]["litres"].as<double>(), DUMMY_CAR_FUEL_LEVEL);
    ASSERT_DOUBLE_EQ(serializedCar["location"]["lat"].as<double>(), 55.73312);
    ASSERT_DOUBLE_EQ(serializedCar["location"]["lon"].as<double>(), 37.589004);
    ASSERT_EQ(serializedCar["sim"]["number"].as<std::string>(), "+79876543210");
    ASSERT_DOUBLE_EQ(serializedCar["sim"]["balance"]["amount"].as<double>(), 464.52);
    ASSERT_EQ(serializedCar["sim"]["balance"]["currency"].as<std::string>(), "RUB");
}

Y_UNIT_TEST(test_serialize_with_empty_or_zero_fields)
{
    auto car = DUMMY_CAR;
    car.parseDetails(
        getDeviceJson({
            .fuelTank = R"("fuel_tank": 0,)",
            .phone = R"("phone": "",)",
        }),
        getUpdatesJson({
            .tanks = R"(
            "tanks": [
                {
                    "id": 1,
                    "m": 1,
                    "ras": 2,
                    "ras_t": 1,
                    "val": 30
                }
            ],)",
            .x = R"("x": 0,)",
            .y = R"("y": 0,)",
            .fuel = R"("fuel": 0,)",
        }),
        PANDORA_CAR_ID_STR,
        CarInformation::LocationOnly::FALSE);

    const auto serializedCar = car.serialize();

    ASSERT_FALSE(serializedCar.hasField("fuelLevel"));
    ASSERT_DOUBLE_EQ(serializedCar["avgFuelConsumption"].as<double>(), 2);
    ASSERT_FALSE(serializedCar.hasField("location"));
    ASSERT_FALSE(serializedCar.hasField("sim"));
}

Y_UNIT_TEST(test_serialize_with_missing_fields)
{
    auto car = DUMMY_CAR;
    car.parseDetails(
        getDeviceJson({
            .fuelTank = "",
            .phone = "",
        }),
        getUpdatesJson({
            .tanks = R"(
            "tanks": [
                {
                    "id": 1,
                    "m": 1,
                    "ras": 2,
                    "ras_t": 1,
                    "val": 30
                }
            ],)",
            .x = "",
            .y = "",
            .fuel = "",
        }),
        PANDORA_CAR_ID_STR,
        CarInformation::LocationOnly::FALSE);

    const auto serializedCar = car.serialize();

    ASSERT_FALSE(serializedCar.hasField("fuelLevel"));
    ASSERT_DOUBLE_EQ(serializedCar["avgFuelConsumption"].as<double>(), 2);
    ASSERT_FALSE(serializedCar.hasField("location"));
    ASSERT_FALSE(serializedCar.hasField("sim"));
}

Y_UNIT_TEST(test_serialize_with_wrong_balance_fields)
{
    for (const auto& balanceField : std::vector<std::string> {
        R"("balance": {
            "value": "464.520000"
        },)",
        R"("balance": {
            "cur": "RUB"
        },)",
        R"("balance": {},)",
        R"("balance": {
            "value": "464.520000",
            "cur": ""
        },)",
        R"("balance": {
            "value": "",
            "cur": "RUB"
        },)",
        ""
    }) {

        auto car = DUMMY_CAR;
        car.parseDetails(
            getDeviceJson(),
            getUpdatesJson({ .balance = balanceField, }),
            PANDORA_CAR_ID_STR,
            CarInformation::LocationOnly::FALSE);

        const auto serializedCar = car.serialize();

        ASSERT_FALSE(serializedCar.hasField("sim"));
    }
}

Y_UNIT_TEST(test_negative_balance_field)
{
    auto car = DUMMY_CAR;
    car.parseDetails(
        getDeviceJson(),
        getUpdatesJson({ .balance = R"("balance": {
            "value": "-0.02",
            "cur": "RUB"
        },)", }),
        PANDORA_CAR_ID_STR,
        CarInformation::LocationOnly::FALSE);

    const auto serializedCar = car.serialize();

    ASSERT_TRUE(serializedCar.hasField("sim"));
    ASSERT_DOUBLE_EQ(serializedCar["sim"]["balance"]["amount"].as<double>(), -0.02);
    ASSERT_EQ(serializedCar["sim"]["balance"]["currency"].as<std::string>(), "RUB");
}

}
