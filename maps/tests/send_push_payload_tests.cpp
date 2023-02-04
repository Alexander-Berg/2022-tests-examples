#include <library/cpp/testing/unittest/registar.h>

#include <maps/automotive/parking/fastcgi/parking_api/lib/send_push.h>
#include <maps/automotive/parking/lib/carpark_finder/parking_info.h>

#include <maps/libs/json/include/value.h>

#include <iostream>


maps::json::Value checkPayload(const std::string& payloadStr, const std::string& expectedTitle) {
    try {
        auto payload = maps::json::Value::fromString(payloadStr);

        // Common checks
        UNIT_ASSERT(payload.hasField("payload"));
        UNIT_ASSERT(payload["payload"].hasField("yandexnavi.action"));

        auto action = payload["payload"]["yandexnavi.action"].toString();

        UNIT_ASSERT(payload.hasField("repack"));
        UNIT_ASSERT(payload["repack"].hasField("apns"));

        auto apns = payload["repack"]["apns"];

        UNIT_ASSERT_EQUAL(apns["aps"]["alert"].toString(), expectedTitle);

        UNIT_ASSERT(payload["repack"].hasField("fcm"));
        auto fcm = payload["repack"]["fcm"];

        UNIT_ASSERT_EQUAL(fcm["notification"]["title"].toString(), expectedTitle);
        UNIT_ASSERT_EQUAL(fcm["notification"]["sound"].toString(), "default");

        // Tags
        UNIT_ASSERT(payload.hasField("tags"));
        auto tags = payload["tags"];

        UNIT_ASSERT_EQUAL(tags[0].toString(), "ru.yandex.mobile.navigator");
        UNIT_ASSERT_EQUAL(tags[1].toString(), "ru.yandex.yandexnavi");

        std::cerr << "Payload" << payloadStr;

        return payload;
    } catch (std::exception& e) {
        std::cerr << "Failed to parse/validate JSON payload: " << payloadStr << std::endl;
        throw;
    }
}

maps::automotive::parking::ParkingInfo parkingInfo(const std::string& name, const std::string& price) {
    yandex::maps::proto::search::business::GeoObjectMetadata dummyMetadata;

    maps::automotive::parking::ParkingInfo parkingInfo(dummyMetadata,
                                                       {1,2},
                                                       {{1,2}, {3, 4}});

    parkingInfo.name_ = name;
    parkingInfo.pricePerFirstHour_ = price;

    return parkingInfo;
}


Y_UNIT_TEST_SUITE(push_payload) {
    maps::geolib3::Point2 somePoint{1,2};

    Y_UNIT_TEST(test_simlple) {
        auto payloadStr = preparePushPayload(parkingInfo("Some parking area", "100 Rub"), somePoint, true);

        auto payload = checkPayload(payloadStr, "Some parking area, тариф: 100 Rub/час");
    }

    Y_UNIT_TEST(test_title_escaping) {
        auto payloadStr = preparePushPayload(parkingInfo(R"(Some "Named" area)", "102 Rub"), somePoint, true);

        auto payload = checkPayload(payloadStr, "Some \"Named\" area, тариф: 102 Rub/час");
    }

    Y_UNIT_TEST(test_title_utf8) {
        auto payloadStr = preparePushPayload(parkingInfo("Парковочная зона № 3201", "60 ₽"), somePoint, true);

        auto payload = checkPayload(payloadStr, "Парковочная зона № 3201, тариф: 60 ₽/час");
    }
}
