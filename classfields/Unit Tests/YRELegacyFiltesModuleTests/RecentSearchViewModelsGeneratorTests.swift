//
//  RecentSearchViewModelGeneratorTests.swift
//  YandexRealty
//
//  Created by Alexey Salangin on 03/26/20.
//  Copyright © 2021 Yandex. All rights reserved.
//

// swiftlint:disable file_length

import XCTest
import YRELegacyFiltersModule
import YREModel
import YREModelObjc
import verticalios
@testable import YRELegacyFiltersCore
import YREFiltersModel

// https://st.yandex-team.ru/VSAPPS-8361#605330e90a2c92699f591e5a
final class RecentSearchViewModelGeneratorTests: XCTestCase { // swiftlint:disable:this type_body_length

    // 1
    func testBuyApartment() {
        let viewModel = Self.viewModel(
            action: .buy,
            category: .apartment,
            geoIntent: Self.moscowPlainGeoIntent
        ) { (filter: YREBuyApartamentFilter) in // swiftlint:disable:this closure_body_length
            filter.objectType.setValue(FilterObjectType.offer)
            filter.isNewFlat.value = false
            filter.roomsCount.setValues([kYREFilterRoomsTotal.studio, .total2])
            filter.price.value = YVNumberRangeMake(nil, 100000)
            filter.priceType.setValue(kYREFilterPriceType.perM2)
            filter.minMetroDistance.setValue(5)
            filter.distanceType.setValue(FilterDistanceType.byFoot)
            filter.areaTotal.value = YVNumberRangeMake(50, nil)
            filter.areaKitchen.value = YVNumberRangeMake(10, nil)
            filter.minCeilingHeight.value = NSNumber(value: 2.5)
            filter.renovation.setValues([kYREFilterRenovation.yreFilterRenovationCosmeticDone, .yreFilterRenovationEuro])
            filter.balconyType.setValue(kYREFilterBalconyType.yreFilterBalconyTypeBalcony)
            filter.bathroomType.setValue(kYREFilterBathroomType.yreFilterBathroomTypeMatched)
            filter.floor.value = YVNumberRangeMake(5, nil)
            filter.lastFloor.setValue(true)
            filter.exceptFirstFloor.setValue(true)
            filter.buildingConstructionType.setValues([kYREFilterBuildingConstructionType.brick, .monolitBrick])
            filter.buildYear.value = YVNumberRangeMake(2000, nil)
            filter.livingApartmentType.setValue(kYREFilterLivingApartmentType.nonLiving)
            filter.houseFloors.value = YVNumberRangeMake(7, nil)
            filter.parkingType.setValues([kYREFilterParkingType.yreFilterParkingTypeClosed])
            filter.cityRenovation.setValue(CityRenovationFilterType.exceptMatching)
            filter.pondType.setValues([FilterPondType.pond])
            filter.parkType.setValues([FilterParkType.park])
            filter.expectMetro.setValue(true)
            filter.photoRequired.setValue(true)
            filter.hasVideo.setValue(true)
            filter.supportsOnlineView.setValue(true)
            filter.offerPropertyType.setValue(true)
            filter.withExcerptsOnly.setValue(true)
            filter.tagsToInclude.setPrimitiveValues(
                [
                    YRETagFilterItem(tag: .init(identifier: "1", title: "дача")),
                    YRETagFilterItem(tag: .init(identifier: "2", title: "железная дорога")),
                    YRETagFilterItem(tag: .init(identifier: "3", title: "клубный дом")),
                ]
            )
            filter.tagsToExclude.setPrimitiveValues(
                [
                    YRETagFilterItem(tag: .init(identifier: "4", title: "старый фонд")),
                    YRETagFilterItem(tag: .init(identifier: "5", title: "СНП")),
                    YRETagFilterItem(tag: .init(identifier: "6", title: "газ")),
                ]
            )
        }

        let title = viewModel.title
        let expectedTitle = "Купить квартиру"
        XCTAssertEqual(title, expectedTitle)

        let parameters = viewModel.parameters
        let expectedParameters = [
            "Москва",
            "вторичка",
            "студия, 2 комн.",
            "до 100 тыс. ₽ за м²",
            "до метро не более 5 минут пешком",
            "общая площадь от 50 м²",
            "площадь кухни от 10 м²",
            "высота потолков: от 2.5 м",
            "ремонт: косметический или евро",
            "есть балкон",
            "санузел: совмещённый",
            "этаж от 5",
            "только последний этаж",
            "кроме первого этажа",
            "тип дома: кирпичный или кирпично-монолитный",
            "год постройки от 2000",
            "только апартаменты",
            "этажей в доме от 7",
            "парковка: закрытая",
            "не показывать дома под снос",
            "рядом парк, пруд",
            "рядом построят станцию метро",
            "только с фото",
            "есть видео",
            "можно посмотреть онлайн",
            "только от собственников",
            "проверено в Росреестре",
            "искать в описании объявления: «дача», «железная дорога», «клубный дом»",
            "не показывать, если в описании: «старый фонд», «СНП», «газ»",
        ]
        self.assertEqual(parameters, expectedParameters)
    }

    // 2
    func testBuyApartmentInSite() {
        let viewModel = Self.viewModel(
            action: .buy,
            category: .apartment,
            geoIntent: Self.twoPolygonsGeoIntent
        ) { (filter: YREBuyApartamentFilter) in
            filter.objectType.setValue(FilterObjectType.site)
            filter.roomsCount.setValues([kYREFilterRoomsTotal.total1, .total2, .total3])
            filter.price.value = YVNumberRangeMake(10_000_000, nil)
            filter.minMetroDistance.setValue(60)
            filter.distanceType.setValue(FilterDistanceType.onTransport)
            filter.decoration.setValues([kYREFilterDecoration.rough, .turnkey])
            filter.deliveryDate.setPrimitiveValue(YREFilterDeliveryDate(year: 2022, quarter: 3, finished: false))
            filter.buildingClass.setValues([kYREFilterBuildingClass.comfortPlus, .elite])
            filter.developer.setPrimitiveValues([
                YREDeveloperFilterItem(identifier: "0", name: "Компания ТехноСтройОлимп")
            ])
            filter.hasSpecialProposal.setValue(true)
            filter.hasSiteMortgage.setValue(true)
            filter.hasInstallment.setValue(true)
            filter.fz214.setValue(true)
            filter.hasSiteMaternityFunds.setValue(true)
            filter.hasMilitarySiteMortgage.setValue(true)
            filter.showOutdated.setValue(true)
        }

        let title = viewModel.title
        let expectedTitle = "Купить квартиру"
        XCTAssertEqual(title, expectedTitle)

        let parameters = viewModel.parameters
        let expectedParameters = [
            "2 нарисованные области",
            "новостройка",
            "1–3 комн.",
            "от 10 млн ₽",
            "до метро не более 1 часа на транспорте",
            "отделка: черновая или под ключ",
            "срок сдачи: до 3 квартала 2022",
            "класс жилья: комфорт+ или элитный",
            "застройщик: Компания ТехноСтройОлимп",
            "скидки",
            "ипотека",
            "рассрочка",
            "214 ФЗ",
            "материнский капитал",
            "военная ипотека",
            "ЖК с закрытыми продажами"
        ]
        self.assertEqual(parameters, expectedParameters)
    }

    // 3
    func testBuyApartmentInSamolet() {
        let viewModel = Self.viewModel(
            action: .buy,
            category: .apartment,
            geoIntent: Self.twoPolygonsGeoIntent
        ) { (filter: YREBuyApartamentFilter) in
            filter.objectType.setValue(FilterObjectType.site)
            filter.onlySamolet.setValue(true)
        }

        let title = viewModel.title
        let expectedTitle = "Купить квартиру"
        XCTAssertEqual(title, expectedTitle)

        let parameters = viewModel.parameters
        let expectedParameters = [
            "2 нарисованные области",
            "новостройка",
            "ЖК от Самолет"
        ]
        self.assertEqual(parameters, expectedParameters)
    }

    // 4
    func testBuyRoom() {
        let viewModel = Self.viewModel(
            action: .buy,
            category: .room,
            geoIntent: Self.commuteGeoIntent
        ) { (filter: YREBuyRoomFilter) in
            filter.price.value = YVNumberRangeMake(500000, 1_000_000)
            filter.areaLiving.value = YVNumberRangeMake(30, nil)
            filter.roomsCount.setValues(
                [
                    kYREFilterRoomsTotal.total2,
                    .total3,
                    .total4,
                    .total5,
                    .total7Plus
                ]
            )
            filter.cityRenovation.setValue(CityRenovationFilterType.onlyMatching)
        }

        let title = viewModel.title
        let expectedTitle = "Купить комнату"
        XCTAssertEqual(title, expectedTitle)

        let parameters = viewModel.parameters
        let expectedParameters = [
            "10 минут на транспортe до ул. Ленина, 4",
            "площадь комнаты от 30 м²",
            "от 500 тыс. до 1 млн ₽",
            "2–5, 7+ комн.",
            "показать только дома под снос",
        ]
        self.assertEqual(parameters, expectedParameters)
    }

    // 5
    func testBuyHouse() {
        let viewModel = Self.viewModel(
            action: .buy,
            category: .house,
            geoIntent: nil
        ) { (filter: YREBuyHouseFilter) in
            filter.objectType.setValue(FilterObjectType.offer)
            filter.primarySale.setValue(false)
            filter.houseType.setValues([kYREFilterHouseType.townhouse, .part])
            filter.houseArea.value = YVNumberRangeMake(100, nil)
            filter.price.value = YVNumberRangeMake(nil, 200000)
            filter.priceType.setValue(kYREFilterPriceType.perM2)
            filter.hasGasSupply.setValue(true)
            filter.hasElectricitySupply.setValue(true)
            filter.hasWaterSupply.setValue(true)
            filter.hasSewerageSupply.setValue(true)
            filter.hasHeatingSupply.setValue(true)
        }

        let title = viewModel.title
        let expectedTitle = "Купить дом"
        XCTAssertEqual(title, expectedTitle)

        let parameters = viewModel.parameters
        let expectedParameters = [
            "Область на карте",
            "вторичка",
            "до 200 тыс. ₽ за м²",
            "площадь дома от 100 м²",
            "таунхаус или часть дома",
            "водопровод",
            "отопление",
            "электричество",
            "газ",
            "канализация",
        ]
        self.assertEqual(parameters, expectedParameters)
    }

    // 6
    func testBuyHouseInVillage() {
        let viewModel = Self.viewModel(
            action: .buy,
            category: .house,
            geoIntent: Self.polygonGeoIntent
        ) { (filter: YREBuyHouseFilter) in
            filter.objectType.setValue(FilterObjectType.village)
            filter.villageOfferType.setValues([FilterVillageOfferType.cottage, .townhouse])
            filter.wallsType.setValues([FilterWallsType.timberFraming, .frame])
            filter.lotArea.value = YVNumberRangeMake(300, nil)
            filter.deliveryDate.setPrimitiveValue(YREFilterDeliveryDate(year: 2023, quarter: 1, finished: false))
            filter.villageDeveloper.setPrimitiveValues([
                YREDeveloperFilterItem(identifier: "0", name: "Земелюшка")
            ])
            filter.landType.setValues([FilterLandType.dnp, .izhs, .snt])
            filter.hasRailwayStation.setValue(true)
            filter.villageClass.setValues([FilterVillageClass.econom, .elite])
        }

        let title = viewModel.title
        let expectedTitle = "Купить дом, таунхаус"
        XCTAssertEqual(title, expectedTitle)

        let parameters = viewModel.parameters
        let expectedParameters = [
            "Нарисованная область",
            "коттеджный посёлок",
            "площадь участка от 300 соток",
            "материал стен: фахверк или каркасно-щитовые",
            "срок сдачи: до 1 квартала 2023",
            "класс посёлка: эконом или элитный",
            "застройщик: Земелюшка",
            "тип земли: ДНП, ИЖС, СНТ",
            "рядом есть ж/д станция",
        ]
        self.assertEqual(parameters, expectedParameters)
    }

    // 7
    func testBuyHouseInVillageLot() {
        let viewModel = Self.viewModel(
            action: .buy,
            category: .house,
            geoIntent: Self.moscowPlainGeoIntent
        ) { (filter: YREBuyHouseFilter) in
            filter.objectType.setValue(FilterObjectType.village)
            filter.price.value = YVNumberRangeMake(100000, nil)
            filter.priceType.setValue(kYREFilterPriceType.perAre)
            filter.houseArea.value = YVNumberRangeMake(1000, nil)
            filter.deliveryDate.setPrimitiveValue(YREFilterDeliveryDate(year: 2022, quarter: 4, finished: false))
            filter.landType.setValues([FilterLandType.izhs, .mzhs])
            filter.lotArea.value = YVNumberRangeMake(1000, nil)
        }

        let title = viewModel.title
        let expectedTitle = "Купить дом"
        XCTAssertEqual(title, expectedTitle)

        let parameters = viewModel.parameters
        let expectedParameters = [
            "Москва",
            "коттеджный посёлок",
            "от 100 тыс. ₽",
            "площадь участка от 1000 соток",
            "срок сдачи: до 4 квартала 2022",
            "тип земли: ИЖС или МЖС",
        ]
        self.assertEqual(parameters, expectedParameters)
    }

    // 8
    func testBuyLot() {
        let viewModel = Self.viewModel(
            action: .buy,
            category: .lot,
            geoIntent: Self.moscowPlainGeoIntent
        ) { (filter: YREBuyLotFilter) in
            filter.price.value = YVNumberRangeMake(nil, 2_000_000)
            filter.primarySale.setValue(false)
            filter.igs.setValue(true)
            filter.snt.setValue(true)
            filter.expectMetro.setValue(true)
            filter.offerPropertyType.setValue(true)
            filter.tagsToInclude.setPrimitiveValues(
                [
                    YRETagFilterItem(tag: .init(identifier: "1", title: "с пластиковыми окнами")),
                ]
            )
            filter.tagsToExclude.setPrimitiveValues(
                [
                    YRETagFilterItem(tag: .init(identifier: "2", title: "в стиле лофт")),
                    YRETagFilterItem(tag: .init(identifier: "3", title: "в общежитии"))
                ]
            )
        }

        let title = viewModel.title
        let expectedTitle = "Купить участок"
        XCTAssertEqual(title, expectedTitle)

        let parameters = viewModel.parameters
        let expectedParameters = [
            "Москва",
            "вторичка",
            "до 2 млн ₽",
            "ИЖС",
            "СНТ",
            "только от собственников",
            "рядом построят станцию метро",
            "искать в описании объявления: «с пластиковыми окнами»",
            "не показывать, если в описании: «в стиле лофт», «в общежитии»",
        ]
        self.assertEqual(parameters, expectedParameters)
    }

    // 9
    func testBuyLotInVillage() {
        let viewModel = Self.viewModel(
            action: .buy,
            category: .lot,
            geoIntent: Self.moscowPlainGeoIntent
        ) { (filter: YREBuyLotFilter) in
            filter.objectType.setValue(FilterObjectType.village)
            filter.villageOfferType.setValues([FilterVillageOfferTypes.townhouse, .cottage])
            filter.lotArea.value = YVNumberRangeMake(300, nil)
            filter.deliveryDate.setPrimitiveValue(YREFilterDeliveryDate(year: 2023, quarter: 1, finished: false))
            filter.villageClass.setValues([FilterVillageClass.econom, .elite])
            filter.villageDeveloper.setPrimitiveValues([
                YREDeveloperFilterItem(identifier: "0", name: "Земелюшка")
            ])
            filter.landType.setValues([FilterLandType.dnp, .izhs, .snt])
            filter.hasRailwayStation.setValue(true)
        }

        let title = viewModel.title
        let expectedTitle = "Купить участок"
        XCTAssertEqual(title, expectedTitle)

        let parameters = viewModel.parameters
        let expectedParameters = [
            "Москва",
            "коттеджный посёлок",
            "площадь участка от 300 соток",
            "срок сдачи: до 1 квартала 2023",
            "класс посёлка: эконом или элитный",
            "застройщик: Земелюшка",
            "тип земли: ДНП, ИЖС, СНТ",
            "рядом есть ж/д станция",
        ]
        self.assertEqual(parameters, expectedParameters)
    }

    // 10
    func testBuyCommercialLot() {
        let viewModel = Self.viewModel(
            action: .buy,
            category: .commercial,
            geoIntent: Self.moscowPlainGeoIntent
        ) { (filter: YREBuyCommercialFilter) in
            filter.commercialType.setValues([FilterCommercialType.land])
            filter.lotArea.value = YVNumberRangeMake(200, nil)
            filter.price.value = YVNumberRangeMake(nil, 500000)
            filter.priceType.setValue(kYREFilterPriceType.perOffer)
        }

        let title = viewModel.title
        let expectedTitle = "Купить участок"
        XCTAssertEqual(title, expectedTitle)

        let parameters = viewModel.parameters
        let expectedParameters = [
            "Москва",
            "до 500 тыс. ₽",
            "площадь участка от 200 соток",
        ]
        self.assertEqual(parameters, expectedParameters)
    }

    // 11
    func testBuyCommercialMultiple() {
        let viewModel = Self.viewModel(
            action: .buy,
            category: .commercial,
            geoIntent: Self.moscowPlainGeoIntent
        ) { (filter: YREBuyCommercialFilter) in
            filter.commercialType.setValues(
                [
                    FilterCommercialType.office,
                    .warehouse,
                    .retail,
                ]
            )
            filter.areaTotal.value = YVNumberRangeMake(250, nil)
            filter.entranceType.setValue(kYREFilterEntranceType.common)
            filter.renovation.setValues([kYREFilterRenovation.yreFilterRenovationDesigner, .yreFilterRenovationCosmeticDone])
            filter.commercialPlanType.setValue(FilterCommercialPlanType.openSpace)
            filter.hasVentilation.setValue(true)
            filter.furniture.setValue(true)
            filter.aircondition.setValue(true)
            filter.commercialBuildingType.setValue(kYREFilterCommercialBuildingType.shoppingCenter)
            filter.hasTwentyFourSeven.setValue(true)
            filter.hasParking.setValue(true)
        }

        let title = viewModel.title
        let expectedTitle = "Купить офис, склад, торговое помещение"
        XCTAssertEqual(title, expectedTitle)

        let parameters = viewModel.parameters
        let expectedParameters = [
            "Москва",
            "площадь от 250 м²",
            "вход в помещение: общий",
            "ремонт: дизайнерский или косметический",
            "планировка: open space",
            "вентиляция",
            "есть мебель",
            "кондиционер",
            "тип здания: торговый центр",
            "доступ 24/7",
            "парковка",
        ]
        self.assertEqual(parameters, expectedParameters)
    }

    // 12
    func testBuyGarage() {
        let viewModel = Self.viewModel(
            action: .buy,
            category: .garage,
            geoIntent: Self.moscowPlainGeoIntent
        ) { (filter: YREBuyGarageFilter) in
            filter.garageType.setValues(
                [
                    FilterGarageType.box,
                    .garage,
                    .parkingPlace,
                ]
            )
            filter.hasElectricitySupply.setValue(true)
            filter.hasWaterSupply.setValue(true)
            filter.hasHeatingSupply.setValue(true)
            filter.hasSecurity.setValue(true)
            filter.offerPropertyType.setValue(true)
        }

        let title = viewModel.title
        let expectedTitle = "Купить бокс, гараж, машиноместо"
        XCTAssertEqual(title, expectedTitle)

        let parameters = viewModel.parameters
        let expectedParameters = [
            "Москва",
            "электричество",
            "отопление",
            "водопровод",
            "охрана",
            "только от собственников"
        ]
        self.assertEqual(parameters, expectedParameters)
    }

    // 13
    func testRentApartment() {
        let viewModel = Self.viewModel(
            action: .rent,
            category: .apartment,
            geoIntent: Self.moscowPlainGeoIntent
        ) { (filter: YRERentApartamentFilter) in
            filter.rentTime.setValue(kYREFilterRentTime.timeLong)
            filter.roomsCount.setValues([kYREFilterRoomsTotal.studio])
            filter.price.value = YVNumberRangeMake(nil, 30000)
            filter.yandexRent.setValue(true)
            filter.areaTotal.value = YVNumberRangeMake(nil, 100)
            filter.areaKitchen.value = YVNumberRangeMake(nil, 50)
            filter.lastFloor.setValue(true)
            filter.furniture.setValue(false)
            filter.fridge.setValue(true)
            filter.washingMachine.setValue(true)
            filter.aircondition.setValue(true)
            filter.tv.setValue(true)
            filter.dishwasher.setValue(true)
            filter.animal.setValue(true)
            filter.kids.setValue(true)
            filter.buildingConstructionType.setValues([kYREFilterBuildingConstructionType.brick, .mono, .panel, .block])
            filter.buildingEpoch.setValues([FilterBuildingEpoch.epochKhrushchev, .epochStalin])
            filter.buildYear.value = YVNumberRangeMake(nil, 2030)
            filter.noFee.setValue(true)
        }

        let title = viewModel.title
        let expectedTitle = "Снять квартиру"
        XCTAssertEqual(title, expectedTitle)

        let parameters = viewModel.parameters
        let expectedParameters = [
            "Москва",
            "Яндекс.Аренда",
            "студия",
            "до 30 тыс. ₽",
            "год постройки до 2030",
            "тип дома: кирпичный, монолитный, панельный, блочный, хрущёвка или сталинка",
            "нет мебели",
            "холодильник",
            "телевизор",
            "стиральная машина",
            "посудомоечная машина",
            "кондиционер",
            "можно с детьми",
            "можно с животными",
            "общая площадь до 100 м²",
            "площадь кухни до 50 м²",
            "только последний этаж",
            "без комиссии агенту",
        ]
        self.assertEqual(parameters, expectedParameters)
    }

    // 14
    func testRentApartmentShort() {
        let viewModel = Self.viewModel(
            action: .rent,
            category: .apartment,
            geoIntent: Self.moscowPlainGeoIntent
        ) { (filter: YRERentApartamentFilter) in
            filter.rentTime.setValue(kYREFilterRentTime.timeShort)
            filter.price.value = YVNumberRangeMake(nil, 1500)
            filter.roomsCount.setValues(
                [
                    kYREFilterRoomsTotal.total4Plus,
                ]
            )
        }

        let title = viewModel.title
        let expectedTitle = "Снять квартиру посуточно"
        XCTAssertEqual(title, expectedTitle)

        let parameters = viewModel.parameters
        let expectedParameters = [
            "Москва",
            "4+ комн.",
            "до 1,5 тыс. ₽",
        ]
        self.assertEqual(parameters, expectedParameters)
    }

    // 15
    func testRentRoom() {
        let viewModel = Self.viewModel(
            action: .rent,
            category: .room,
            geoIntent: Self.moscowPlainGeoIntent
        ) { (filter: YRERentRoomFilter) in
            filter.rentTime.setValue(kYREFilterRentTime.timeLong)
            filter.areaLiving.value = YVNumberRangeMake(10, 20)
            filter.roomsCount.setValues(
                [
                    kYREFilterRoomsTotal.total2,
                    .total3,
                    .total4,
                    .total5,
                    .total7Plus,
                ]
            )
        }

        let title = viewModel.title
        let expectedTitle = "Снять комнату"
        XCTAssertEqual(title, expectedTitle)

        let parameters = viewModel.parameters
        let expectedParameters = [
            "Москва",
            "площадь комнаты 10 - 20 м²",
            "2–5, 7+ комн. в квартире"
        ]
        self.assertEqual(parameters, expectedParameters)
    }

    // 16
    func testRentHouse() {
        let viewModel = Self.viewModel(
            action: .rent,
            category: .house,
            geoIntent: Self.moscowPlainGeoIntent
        ) { (filter: YRERentHouseFilter) in
            filter.rentTime.setValue(kYREFilterRentTime.timeShort)
            filter.price.value = YVNumberRangeMake(5000, 5000)
            filter.houseType.setValues([kYREFilterHouseType.whole])
            filter.lotArea.value = YVNumberRangeMake(5, 5)
            filter.houseArea.value = YVNumberRangeMake(120, 120)
        }

        let title = viewModel.title
        let expectedTitle = "Снять дом посуточно"
        XCTAssertEqual(title, expectedTitle)

        let parameters = viewModel.parameters
        let expectedParameters = [
            "Москва",
            "5 тыс. ₽",
            "отдельный дом",
            "площадь дома 120 м²",
            "площадь участка 5 соток",
        ]
        self.assertEqual(parameters, expectedParameters)
    }

    // 17
    func testRentCommercial() {
        let viewModel = Self.viewModel(
            action: .rent,
            category: .commercial,
            geoIntent: Self.moscowPlainGeoIntent
        ) { (filter: YRERentCommercialFilter) in
            filter.price.value = YVNumberRangeMake(1000, nil)
            filter.priceType.setValue(kYREFilterPriceType.perM2)
            filter.pricePeriod.setValue(kYREFilterPricePeriod.perYear)
        }

        let title = viewModel.title
        let expectedTitle = "Снять коммерческую"
        XCTAssertEqual(title, expectedTitle)

        let parameters = viewModel.parameters
        let expectedParameters = [
            "Москва",
            "от 1 тыс. ₽ за м² в год",
        ]
        self.assertEqual(parameters, expectedParameters)
    }

    // 18
    func testRentGarage() {
        let viewModel = Self.viewModel(
            action: .rent,
            category: .garage,
            geoIntent: Self.moscowPlainGeoIntent
        ) { (filter: YRERentGarageFilter) in
            filter.price.value = YVNumberRangeMake(5000, 10000)
            filter.hasUtilitiesIncluded.setValue(true)
            filter.hasElectricityIncluded.setValue(true)
            filter.hasElectricitySupply.setValue(true)
        }

        let title = viewModel.title
        let expectedTitle = "Снять гараж"
        XCTAssertEqual(title, expectedTitle)

        let parameters = viewModel.parameters
        let expectedParameters = [
            "Москва",
            "от 5 до 10 тыс. ₽",
            "электричество",
            "КУ включены",
            "электроэнергия включена",
        ]
        self.assertEqual(parameters, expectedParameters)
    }

    // MARK: - Private
    private static var moscowPlainGeoIntent: PlainGeoIntent {
        let boundingBox = YRECoordinateRegion(
            leftTop: MDCoords2D(lat: 56.02137, lon: 36.803_265),
            rightBottom: MDCoords2D(lat: 55.14263, lon: 37.96769)
        )
        let geoObject = YREGeoObject(
            name: "Москва",
            shortName: "Москва",
            address: "Россия, Москва и МО, Москва",
            searchParams: ["rgid": ["587795"]],
            scope: "Москва и МО",
            type: kYREConstantParamGeoObjectType.city,
            center: MDCoords2D(lat: 55.75322, lon: 37.62251),
            boundingBox: boundingBox
        )
        return PlainGeoIntent(geoObject: geoObject)
    }

    private static var polygonGeoIntent: PolygonGeoIntent {
        return PolygonGeoIntent(points: [])
    }

    private static var twoPolygonsGeoIntent: CompositeGeoIntent {
        return CompositeGeoIntent(geoIntents: [
            PolygonGeoIntent(points: []),
            PolygonGeoIntent(points: []),
        ])
    }

    private static var siteGeoIntent: PlainGeoIntent {
        let geoObject = YREGeoObject(
            name: "Город-парк «Переделкино Ближнее»",
            shortName: "Переделкино Ближнее",
            address: "пос. Внуковское, д. Рассказовка, мкр. Переделкино Ближнее",
            searchParams: nil,
            scope: nil,
            type: .zhk,
            center: nil,
            boundingBox: nil
        )
        return PlainGeoIntent(geoObject: geoObject)
    }

    private static var commuteGeoIntent: CommuteGeoIntent {
        let geoObject = CommuteGeoObject(address: "ул. Ленина, 4", location: .init(lat: 0.0, lon: 0.0))
        let configuration = CommuteConfiguration(
            geoObject: geoObject,
            transportType: .public,
            durationInMinutes: 10
        )
        return CommuteGeoIntent(commuteConfiguration: configuration)
    }

    private static func viewModel<Filter: YVFilter>(
        action: kYREFilterAction,
        category: kYREFilterCategory,
        geoIntent: YREGeoIntentProtocol?,
        filterTransformer: (Filter) -> Void = { _ in }
    ) -> RecentSearchViewModel {
        let stubViewModel = RecentSearchViewModel(
            search: YREMutableSearch(),
            title: "",
            parameters: []
        )

        let search = YREMutableSearch()
        guard let filterRoot = FilterRootFactory().makeFilterRoot(
            with: action,
            category: category
        ) else {
            XCTFail("Can not create filter root.")
            return stubViewModel
        }

        let searchTypeFilter = filterRoot.searchTypeFilter

        guard let actionCategoryFilter = filterRoot.actionCategoryFilter as? Filter else {
            XCTFail("Can not convert actionCategoryFilter to \(Filter.self)")
            return stubViewModel
        }
        filterTransformer(actionCategoryFilter)

        let searchTypeFilterState = searchTypeFilter.stateSerializer.state(from: searchTypeFilter)
        let actionCategoryFilterState = actionCategoryFilter.stateSerializer.state(from: actionCategoryFilter)

        search.lastSearchTypeFilterSnapshot = searchTypeFilterState
        search.lastActionCategoryFilterSnapshot = actionCategoryFilterState
        search.geoIntent = geoIntent

        let viewModel = RecentSearchViewModelsGenerator.makeViewModels(with: [search])[0]
        return viewModel
    }

    private func assertEqual(
        _ expected: @autoclosure () throws -> [String],
        _ received: @autoclosure () throws -> [String]
    ) {
        do {
            var expected = try expected()
            var received = try received()

            let maxLength = max(expected.count, received.count)

            if expected.count < maxLength {
                expected += [String](repeating: "nil", count: maxLength - expected.count)
            }

            if received.count < maxLength {
                received += [String](repeating: "nil", count: maxLength - received.count)
            }

            let differenceString: String = zip(expected, received).map {
                if $0.0 == $0.1 {
                    return "✅ \($0.0)\n✅ \($0.1)"
                }
                else {
                    return "🚫 \($0.0)\n🚫 \($0.1)"
                }
            }.joined(separator: "\n\n")

            XCTAssertTrue(expected == received, "Found difference:\n" + differenceString)
        }
        catch {
            XCTFail("Caught error while testing: \(error)")
        }
    }
}
