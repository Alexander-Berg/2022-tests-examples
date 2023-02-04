import AutoRuProtoModels
import Snapshots
import XCTest
@testable import AutoRuGarageCard

final class GarageCardTitleCellTests: BaseUnitTest {
    func test_TitleCell() throws {
        let vehicleInfo: Auto_Api_Vin_Garage_Vehicle = try .init(jsonString: Self.carInfoJson)

        let layout = TitleCell(vehicleInfo: vehicleInfo, shouldHideVINNumber: false)
        Snapshot.compareWithSnapshot(layout: layout, maxWidth: DeviceWidth.iPhone11)
    }

    func test_TitleCell_onSale() throws {
        let vehicleInfo: Auto_Api_Vin_Garage_Vehicle = try .init(jsonString: Self.carInfoJson)

        let layout = TitleCell(
            vehicleInfo: vehicleInfo,
            shouldHideVINNumber: false,
            onSaleLabelTap: {}
        )
        Snapshot.compareWithSnapshot(layout: layout, maxWidth: DeviceWidth.iPhone11)
    }

    func test_TitleCellVin() throws {
        let vehicleInfo: Auto_Api_Vin_Garage_Vehicle = try .init(jsonString: Self.carInfoJsonVIN)

        let layout = TitleCell(vehicleInfo: vehicleInfo, shouldHideVINNumber: false)
        Snapshot.compareWithSnapshot(layout: layout, maxWidth: DeviceWidth.iPhone11)
    }

    func test_TitleCellVin_onSale() throws {
        let vehicleInfo: Auto_Api_Vin_Garage_Vehicle = try .init(jsonString: Self.carInfoJsonVIN)

        let layout = TitleCell(
            vehicleInfo: vehicleInfo,
            shouldHideVINNumber: false,
            onSaleLabelTap: {}
        )
        Snapshot.compareWithSnapshot(layout: layout, maxWidth: DeviceWidth.iPhone11)
    }

    func test_publicCardTitleHasNoSensitiveInfo() throws {
        Step("Заголовок без VIN и ГРЗ")
        var vehicleInfo = try Auto_Api_Vin_Garage_Vehicle(jsonString: Self.carInfoJsonVIN)

        vehicleInfo.documents.licensePlate = "А777АА77"
        vehicleInfo.documents.vin = "WVW3B37777777"

        let layout = TitleCell(vehicleInfo: vehicleInfo, shouldHideVINNumber: true)
        Snapshot.compareWithSnapshot(layout: layout, maxWidth: DeviceWidth.iPhone11)
    }

    func test_publicCardTitleHasNoSensitiveInfo_onSale() throws {
        Step("Заголовок без VIN и ГРЗ")
        var vehicleInfo = try Auto_Api_Vin_Garage_Vehicle(jsonString: Self.carInfoJsonVIN)

        vehicleInfo.documents.licensePlate = "А777АА77"
        vehicleInfo.documents.vin = "WVW3B37777777"

        let layout = TitleCell(
            vehicleInfo: vehicleInfo,
            shouldHideVINNumber: true,
            onSaleLabelTap: {}
        )
        Snapshot.compareWithSnapshot(layout: layout, maxWidth: DeviceWidth.iPhone11)
    }

    static let carInfoJson =
    """
        {
            "carInfo":{
                "bodyType":"ALLROAD_5_DOORS",
                "engineType":"GASOLINE",
                "transmission":"MECHANICAL",
                "drive":"ALL_WHEEL_DRIVE",
                "mark":"UAZ",
                "model":"PATRIOT",
                "horsePower":128,
                "markInfo":{
                    "code":"UAZ",
                    "name":"УАЗ",
                    "ruName":"УАЗ",
                    "logo":{
                        "name":"catalog_mark_icon",
                        "sizes":{
                            "black-logo":"//avatars.mds.yandex.net/get-verba/216201/2a00000179b45e7046a18784eacd764b5190/logo",
                            "orig":"//avatars.mds.yandex.net/get-verba/1540742/2a00000179b45e958b940518a0f725ecac7e/dealer_logo"
                        }
                    }
                },
                "modelInfo":{
                    "code":"PATRIOT",
                    "name":"Patriot",
                    "ruName":"Патриот"
                },
                "configuration":{
                    "doorsCount":5
                },
                "techParam":{
                    "displacement":2693
                },
                "steeringWheel":"LEFT"
            },
            "documents":{
                "year":2012,
                "licensePlate":"P777KY190"
            },
            "color":{
                "id":"FAFBFB",
                "name":"Белый"
            }
        }
    """

    static let carInfoJsonVIN =
    """
        {
            "carInfo":{
                "bodyType":"SEDAN",
                "engineType":"GASOLINE",
                "transmission":"AUTOMATIC",
                "drive":"FORWARD_CONTROL",
                "mark":"KIA",
                "model":"RIO",
                "superGenId":"20508999",
                "configurationId":"20511785",
                "techParamId":"20511786",
                "complectationId":"21009525",
                "horsePower":123,
                "markInfo":{
                    "code":"KIA",
                    "name":"Kia",
                    "ruName":"Киа",
                    "logo":{
                        "name":"catalog_mark_icon",
                        "sizes":{
                            "orig":"//avatars.mds.yandex.net/get-verba/3587101/2a0000017a6114778ce634401ce1a0fb0a1d/dealer_logo",
                            "black-logo":"//avatars.mds.yandex.net/get-verba/1540742/2a00000179b3c7aff2a6a6a4c1c7ff205114/logo"
                        }
                    }
                },
                "modelInfo":{
                    "code":"RIO",
                    "name":"Rio",
                    "ruName":"Рио"
                },
                "superGen":{
                    "id":"20508999",
                    "name":"III Рестайлинг",
                    "ruName":"3 Рестайлинг",
                    "yearFrom":2015,
                    "yearTo":2017,
                    "priceSegment":"MEDIUM",
                    "purposeGroup":"FAMILY"
                },
                "techParam":{
                    "id":"20511786",
                    "name":"",
                    "nameplate":"",
                    "displacement":1591,
                    "engineType":"GASOLINE",
                    "gearType":"FORWARD_CONTROL",
                    "transmission":"AUTOMATIC",
                    "power":123,
                    "powerKvt":90.0,
                    "humanName":"1.6 AT (123 л.с.)"
                },
                "steeringWheel":"LEFT"
            },
            "documents":{
                "ownersNumber":2,
                "year":2016,
                "vin":"Z94CC41BBHR432003"
            },
            "color":{
                "id":"040001",
                "name":"Черный"
            }
        }
    """
}
