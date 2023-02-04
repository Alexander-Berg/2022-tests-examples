//
//  OfferCardComfortViewModelGeneratorTests.swift
//  Unit Tests
//
//  Created by Timur Guliamov on 18.04.2022.
//  Copyright © 2022 Yandex. All rights reserved.
//

// swiftlint:disable file_length

import XCTest
import YREModel
import YREModelObjc
@testable import YREOfferCardModule

// swiftlint:disable:next type_body_length
final class OfferCardComfortViewModelGeneratorTests: XCTestCase {
    func testSellApartmentPositive() {
        let building = Self.makeBuildingPositive()
        let apartment = Self.makeApartmentPositive()
        let offer = Self.makeOfferPositive(
            type: .sell, 
            category: .apartment,
            building: building,
            apartment: apartment
        )
        let viewModels = OfferCardComfortViewModelGenerator.makeViewModel(offer: offer)

        let titles = viewModels?.map(\.titleText)
        let expectedTitles = [
            "Закрытая территория",
            "Мусоропровод",
            "Лифт",
            "Охрана",
            "Сигнализация",
            "Кондиционер",
            "Встроенная техника",
            "Мебель на кухне",
            "Холодильник",
            "Посудомойка",
            "Стиральная машина",
            "Мебель",
            "Телевизор",
            "Интернет",
            "Телефон",
        ]

        XCTAssertEqual(titles, expectedTitles)
    }

    func testSellApartmentNegative() {
        let building = Self.makeBuildingNegative()
        let apartment = Self.makeApartmentNegative()
        let offer = Self.makeOfferNegative(
            type: .sell, 
            category: .apartment,
            building: building,
            apartment: apartment
        )
        let viewModels = OfferCardComfortViewModelGenerator.makeViewModel(offer: offer)

        let titles = viewModels?.map(\.titleText)
        let expectedTitles = [
            "Закрытой территории нет",
            "Мусоропровода нет",
            "Лифта нет",
            "Охраны нет",
            "Cигнализации нет",
            "Кондиционера нет",
            "Встроенной техники нет",
            "Мебели на кухне нет",
            "Холодильника нет",
            "Посудомойки нет",
            "Стиральной машины нет",
            "Мебели нет",
            "Телевизора нет",
            "Интернета нет",
            "Телефона нет",
        ]

        XCTAssertEqual(titles, expectedTitles)
    }

    func testSellApartmentUnknown() {
        let building = Self.makeBuildingUnknown()
        let apartment = Self.makeApartmentUnknown()
        let offer = Self.makeOfferUnknown(
            type: .sell, 
            category: .apartment,
            building: building,
            apartment: apartment
        )
        let viewModels = OfferCardComfortViewModelGenerator.makeViewModel(offer: offer)

        let titles = viewModels?.map(\.titleText)
        let expectedTitles: [String] = []

        XCTAssertEqual(titles, expectedTitles)
    }

    func testSellRoomPositive() {
        let building = Self.makeBuildingPositive()
        let apartment = Self.makeApartmentPositive()
        let offer = Self.makeOfferPositive(
            type: .sell, 
            category: .room,
            building: building,
            apartment: apartment
        )
        let viewModels = OfferCardComfortViewModelGenerator.makeViewModel(offer: offer)

        let titles = viewModels?.map(\.titleText)
        let expectedTitles = [
            "Закрытая территория",
            "Мусоропровод",
            "Лифт",
            "Охрана",
            "Сигнализация",
            "Кондиционер",
            "Встроенная техника",
            "Мебель на кухне",
            "Холодильник",
            "Посудомойка",
            "Стиральная машина",
            "Мебель",
            "Телевизор",
            "Интернет",
            "Телефон",
        ]

        XCTAssertEqual(titles, expectedTitles)
    }

    func testSellRoomNegative() {
        let building = Self.makeBuildingNegative()
        let apartment = Self.makeApartmentNegative()
        let offer = Self.makeOfferNegative(
            type: .sell, 
            category: .room,
            building: building,
            apartment: apartment
        )
        let viewModels = OfferCardComfortViewModelGenerator.makeViewModel(offer: offer)

        let titles = viewModels?.map(\.titleText)
        let expectedTitles = [
            "Закрытой территории нет",
            "Мусоропровода нет",
            "Лифта нет",
            "Охраны нет",
            "Cигнализации нет",
            "Кондиционера нет",
            "Встроенной техники нет",
            "Мебели на кухне нет",
            "Холодильника нет",
            "Посудомойки нет",
            "Стиральной машины нет",
            "Мебели нет",
            "Телевизора нет",
            "Интернета нет",
            "Телефона нет",
        ]

        XCTAssertEqual(titles, expectedTitles)
    }

    func testSellRoomUnknown() {
        let building = Self.makeBuildingUnknown()
        let apartment = Self.makeApartmentUnknown()
        let offer = Self.makeOfferUnknown(
            type: .sell, 
            category: .room,
            building: building,
            apartment: apartment
        )
        let viewModels = OfferCardComfortViewModelGenerator.makeViewModel(offer: offer)

        let titles = viewModels?.map(\.titleText)
        let expectedTitles: [String] = []

        XCTAssertEqual(titles, expectedTitles)
    }

    func testSellHousePositive() {
        let house = Self.makeHousePositive()
        let offer = Self.makeOfferPositive(
            type: .sell, 
            category: .house,
            house: house
        )
        let viewModels = OfferCardComfortViewModelGenerator.makeViewModel(offer: offer)

        let titles = viewModels?.map(\.titleText)
        let expectedTitles = [
            "Газ",
            "Электричество",
            "Канализация",
            "Водопровод",
            "Возможность прописки",
            "Отопление",
            "Сауна",
            "Бассейн",
            "Кухня",
            "Бильярд",
        ]

        XCTAssertEqual(titles, expectedTitles)
    }

    func testSellHouseNegative() {
        let house = Self.makeHouseNegative()
        let offer = Self.makeOfferNegative(
            type: .sell, 
            category: .house,
            house: house
        )
        let viewModels = OfferCardComfortViewModelGenerator.makeViewModel(offer: offer)

        let titles = viewModels?.map(\.titleText)
        let expectedTitles = [
            "Газа нет",
            "Электричества нет",
            "Канализации нет",
            "Водопровода нет",
            "Возможности прописки нет",
            "Отопления нет",
            "Сауны нет",
            "Бассейна нет",
            "Кухни нет",
            "Бильярда нет",
        ]

        XCTAssertEqual(titles, expectedTitles)
    }

    func testSellHouseUnknown() {
        let house = Self.makeHouseUnknown()
        let offer = Self.makeOfferUnknown(
            type: .sell, 
            category: .house,
            house: house
        )
        let viewModels = OfferCardComfortViewModelGenerator.makeViewModel(offer: offer)

        let titles = viewModels?.map(\.titleText)
        let expectedTitles: [String] = []

        XCTAssertEqual(titles, expectedTitles)
    }

    func testSellGaragePositive() {
        let building = Self.makeBuildingPositive()
        let apartment = Self.makeApartmentPositive()
        let garage = Self.makeGaragePositive()
        let offer = Self.makeOfferPositive(
            type: .sell, 
            category: .garage,
            building: building,
            apartment: apartment,
            garage: garage
        )
        let viewModels = OfferCardComfortViewModelGenerator.makeViewModel(offer: offer)

        let titles = viewModels?.map(\.titleText)
        let expectedTitles = [
            "Пропускная система",
            "Видеонаблюдение",
            "Отопление",
            "Водопровод",
            "Электричество",
            "Подвал-погреб",
            "Охрана",
            "Доступ на объект 24/7",
            "Пожарная сигнализация",
            "Автоматические ворота",
            "Смотровая яма",
            "Автомойка",
            "Автосервис",
        ]

        XCTAssertEqual(titles, expectedTitles)
    }

    func testSellGarageNegative() {
        let building = Self.makeBuildingNegative()
        let apartment = Self.makeApartmentNegative()
        let garage = Self.makeGarageNegative()
        let offer = Self.makeOfferNegative(
            type: .sell, 
            category: .garage,
            building: building,
            apartment: apartment,
            garage: garage
        )
        let viewModels = OfferCardComfortViewModelGenerator.makeViewModel(offer: offer)

        let titles = viewModels?.map(\.titleText)
        let expectedTitles = [
            "Пропускной системы нет",
            "Видеонаблюдения нет",
            "Отопления нет",
            "Водопровода нет",
            "Электричества нет",
            "Подвала-погреба нет",
            "Охраны нет",
            "Доступа на объект 24/7 нет",
            "Пожарной сигнализации нет",
            "Автоматических ворот нет",
            "Смотровой ямы нет",
            "Автомойки нет",
            "Автосервиса нет",
        ]

        XCTAssertEqual(titles, expectedTitles)
    }

    func testSellGarageUnknown() {
        let building = Self.makeBuildingUnknown()
        let apartment = Self.makeApartmentUnknown()
        let garage = Self.makeGarageUnknown()
        let offer = Self.makeOfferUnknown(
            type: .sell, 
            category: .garage,
            building: building,
            apartment: apartment,
            garage: garage
        )
        let viewModels = OfferCardComfortViewModelGenerator.makeViewModel(offer: offer)

        let titles = viewModels?.map(\.titleText)
        let expectedTitles: [String] = []

        XCTAssertEqual(titles, expectedTitles)
    }
    
    func testShortRentApartmentPositive() {
        let building = Self.makeBuildingPositive()
        let apartment = Self.makeApartmentPositive()
        let offer = Self.makeOfferPositive(
            type: .rent, 
            category: .apartment,
            building: building,
            apartment: apartment
        )
        let viewModels = OfferCardComfortViewModelGenerator.makeViewModel(offer: offer)

        let titles = viewModels?.map(\.titleText)
        let expectedTitles = [
            "Можно с животными",
            "Можно с детьми",
            "Интернет",
            "Холодильник",
            "Мебель",
            "Мебель на кухне",
            "Стиральная машина",
            "Встроенная техника",
            "Телевизор",
            "Кондиционер",
            "Посудомойка",
            "Лифт",
            "Закрытая территория",
            "Охрана",
            "Телефон",
            "Сигнализация",
        ]

        XCTAssertEqual(titles, expectedTitles)
    }

    func testShortRentApartmentNegative() {
        let building = Self.makeBuildingNegative()
        let apartment = Self.makeApartmentNegative()
        let offer = Self.makeOfferNegative(
            type: .rent,
            category: .apartment,
            building: building,
            apartment: apartment
        )
        let viewModels = OfferCardComfortViewModelGenerator.makeViewModel(offer: offer)

        let titles = viewModels?.map(\.titleText)
        let expectedTitles = [
            "Без\u{00a0}животных",
            "Без\u{00a0}детей",
            "Интернета нет",
            "Холодильника нет",
            "Мебели нет",
            "Мебели на кухне нет",
            "Стиральной машины нет",
            "Встроенной техники нет",
            "Телевизора нет",
            "Кондиционера нет",
            "Посудомойки нет",
            "Лифта нет",
            "Закрытой территории нет",
            "Охраны нет",
            "Телефона нет",
            "Сигнализации нет",
        ]

        XCTAssertEqual(titles, expectedTitles)
    }

    func testShortRentApartmentUnknown() {
        let building = Self.makeBuildingUnknown()
        let apartment = Self.makeApartmentUnknown()
        let offer = Self.makeOfferUnknown(
            type: .rent, 
            category: .apartment,
            building: building,
            apartment: apartment
        )
        let viewModels = OfferCardComfortViewModelGenerator.makeViewModel(offer: offer)

        let titles = viewModels?.map(\.titleText)
        let expectedTitles: [String] = []

        XCTAssertEqual(titles, expectedTitles)
    }

    func testLongRentApartmentPositive() {
        let building = Self.makeBuildingPositive()
        let apartment = Self.makeApartmentPositive()
        let offer = Self.makeOfferPositive(
            type: .rent, 
            category: .apartment,
            price: YREPrice(currency: .RUB, value: NSNumber(value: 110000), unit: .perOffer, period: .perMonth),
            building: building,
            apartment: apartment
        )
        let viewModels = OfferCardComfortViewModelGenerator.makeViewModel(offer: offer)

        let titles = viewModels?.map(\.titleText)
        let expectedTitles = [
            "Можно с животными",
            "Можно с детьми",
            "Интернет",
            "Холодильник",
            "Мебель",
            "Мебель на кухне",
            "Стиральная машина",
            "Встроенная техника",
            "Телевизор",
            "Кондиционер",
            "Посудомойка",
            "Лифт",
            "Закрытая территория",
            "Мусоропровод",
            "Охрана",
            "Телефон",
            "Сигнализация",
        ]

        XCTAssertEqual(titles, expectedTitles)
    }

    func testLongRentApartmentNegative() {
        let building = Self.makeBuildingNegative()
        let apartment = Self.makeApartmentNegative()
        let offer = Self.makeOfferNegative(
            type: .rent,
            category: .apartment,
            price: YREPrice(currency: .RUB, value: NSNumber(value: 110000), unit: .perOffer, period: .perMonth),
            building: building,
            apartment: apartment
        )
        let viewModels = OfferCardComfortViewModelGenerator.makeViewModel(offer: offer)

        let titles = viewModels?.map(\.titleText)
        let expectedTitles = [
            "Без\u{00a0}животных",
            "Без\u{00a0}детей",
            "Интернета нет",
            "Холодильника нет",
            "Мебели нет",
            "Мебели на кухне нет",
            "Стиральной машины нет",
            "Встроенной техники нет",
            "Телевизора нет",
            "Кондиционера нет",
            "Посудомойки нет",
            "Лифта нет",
            "Закрытой территории нет",
            "Мусоропровода нет",
            "Охраны нет",
            "Телефона нет",
            "Сигнализации нет",
        ]

        XCTAssertEqual(titles, expectedTitles)
    }

    func testLongRentApartmentUnknown() {
        let building = Self.makeBuildingUnknown()
        let apartment = Self.makeApartmentUnknown()
        let offer = Self.makeOfferUnknown(
            type: .rent, 
            category: .apartment,
            price: YREPrice(currency: .RUB, value: NSNumber(value: 110000), unit: .perOffer, period: .perMonth),
            building: building,
            apartment: apartment
        )
        let viewModels = OfferCardComfortViewModelGenerator.makeViewModel(offer: offer)

        let titles = viewModels?.map(\.titleText)
        let expectedTitles: [String] = []

        XCTAssertEqual(titles, expectedTitles)
    }


    func testShortRentRoomPositive() {
        let building = Self.makeBuildingPositive()
        let apartment = Self.makeApartmentPositive()
        let offer = Self.makeOfferPositive(
            type: .rent, 
            category: .room,
            building: building,
            apartment: apartment
        )
        let viewModels = OfferCardComfortViewModelGenerator.makeViewModel(offer: offer)

        let titles = viewModels?.map(\.titleText)
        let expectedTitles = [
            "Можно с животными",
            "Можно с детьми",
            "Интернет",
            "Холодильник",
            "Мебель",
            "Мебель на кухне",
            "Стиральная машина",
            "Встроенная техника",
            "Телевизор",
            "Кондиционер",
            "Посудомойка",
            "Лифт",
            "Закрытая территория",
            "Охрана",
            "Телефон",
            "Сигнализация",
        ]

        XCTAssertEqual(titles, expectedTitles)
    }

    func testShortRentRoomNegative() {
        let building = Self.makeBuildingNegative()
        let apartment = Self.makeApartmentNegative()
        let offer = Self.makeOfferNegative(
            type: .rent, 
            category: .room,
            building: building,
            apartment: apartment
        )
        let viewModels = OfferCardComfortViewModelGenerator.makeViewModel(offer: offer)

        let titles = viewModels?.map(\.titleText)
        let expectedTitles = [
            "Без\u{00a0}животных",
            "Без\u{00a0}детей",
            "Интернета нет",
            "Холодильника нет",
            "Мебели нет",
            "Мебели на кухне нет",
            "Стиральной машины нет",
            "Встроенной техники нет",
            "Телевизора нет",
            "Кондиционера нет",
            "Посудомойки нет",
            "Лифта нет",
            "Закрытой территории нет",
            "Охраны нет",
            "Телефона нет",
            "Сигнализации нет",
        ]

        XCTAssertEqual(titles, expectedTitles)
    }

    func testShortRentRoomUnknown() {
        let building = Self.makeBuildingUnknown()
        let apartment = Self.makeApartmentUnknown()
        let offer = Self.makeOfferUnknown(
            type: .rent, 
            category: .room,
            building: building,
            apartment: apartment
        )
        let viewModels = OfferCardComfortViewModelGenerator.makeViewModel(offer: offer)

        let titles = viewModels?.map(\.titleText)
        let expectedTitles: [String] = []

        XCTAssertEqual(titles, expectedTitles)
    }

    func testLongRentRoomPositive() {
        let building = Self.makeBuildingPositive()
        let apartment = Self.makeApartmentPositive()
        let offer = Self.makeOfferPositive(
            type: .rent, 
            category: .room,
            price: YREPrice(currency: .RUB, value: NSNumber(value: 110000), unit: .perOffer, period: .perMonth),
            building: building,
            apartment: apartment
        )
        let viewModels = OfferCardComfortViewModelGenerator.makeViewModel(offer: offer)

        let titles = viewModels?.map(\.titleText)
        let expectedTitles = [
            "Можно с животными",
            "Можно с детьми",
            "Интернет",
            "Холодильник",
            "Мебель",
            "Мебель на кухне",
            "Стиральная машина",
            "Встроенная техника",
            "Телевизор",
            "Кондиционер",
            "Посудомойка",
            "Лифт",
            "Закрытая территория",
            "Мусоропровод",
            "Охрана",
            "Телефон",
            "Сигнализация",
        ]

        XCTAssertEqual(titles, expectedTitles)
    }

    func testLongRentRoomNegative() {
        let building = Self.makeBuildingNegative()
        let apartment = Self.makeApartmentNegative()
        let offer = Self.makeOfferNegative(
            type: .rent,
            category: .room,
            price: YREPrice(currency: .RUB, value: NSNumber(value: 110000), unit: .perOffer, period: .perMonth),
            building: building,
            apartment: apartment
        )
        let viewModels = OfferCardComfortViewModelGenerator.makeViewModel(offer: offer)

        let titles = viewModels?.map(\.titleText)
        let expectedTitles = [
            "Без\u{00a0}животных",
            "Без\u{00a0}детей",
            "Интернета нет",
            "Холодильника нет",
            "Мебели нет",
            "Мебели на кухне нет",
            "Стиральной машины нет",
            "Встроенной техники нет",
            "Телевизора нет",
            "Кондиционера нет",
            "Посудомойки нет",
            "Лифта нет",
            "Закрытой территории нет",
            "Мусоропровода нет",
            "Охраны нет",
            "Телефона нет",
            "Сигнализации нет",
        ]

        XCTAssertEqual(titles, expectedTitles)
    }

    func testLongRentRoomUnknown() {
        let building = Self.makeBuildingUnknown()
        let apartment = Self.makeApartmentUnknown()
        let offer = Self.makeOfferUnknown(
            type: .rent, 
            category: .room,
            price: YREPrice(currency: .RUB, value: NSNumber(value: 110000), unit: .perOffer, period: .perMonth),
            building: building,
            apartment: apartment
        )
        let viewModels = OfferCardComfortViewModelGenerator.makeViewModel(offer: offer)

        let titles = viewModels?.map(\.titleText)
        let expectedTitles: [String] = []

        XCTAssertEqual(titles, expectedTitles)
    }

    func testRentHousePositive() {
        let house = Self.makeHousePositive()
        let offer = Self.makeOfferPositive(
            type: .rent, 
            category: .house,
            house: house
        )
        let viewModels = OfferCardComfortViewModelGenerator.makeViewModel(offer: offer)

        let titles = viewModels?.map(\.titleText)
        let expectedTitles = [
            "Можно с животными",
            "Можно с детьми",
            "Кухня",
            "Бильярд",
            "Сауна",
            "Бассейн",
            "Канализация",
            "Электричество",
            "Газ",
            "Отопление",
            "Водопровод",
        ]

        XCTAssertEqual(titles, expectedTitles)
    }

    func testRentHouseNegative() {
        let house = Self.makeHouseNegative()
        let offer = Self.makeOfferNegative(
            type: .rent, 
            category: .house,
            house: house
        )
        let viewModels = OfferCardComfortViewModelGenerator.makeViewModel(offer: offer)

        let titles = viewModels?.map(\.titleText)
        let expectedTitles = [
            "Без\u{00a0}животных",
            "Без\u{00a0}детей",
            "Кухни нет",
            "Бильярда нет",
            "Сауны нет",
            "Бассейна нет",
            "Канализации нет",
            "Электричества нет",
            "Газа нет",
            "Отопления нет",
            "Водопровода нет",
        ]

        XCTAssertEqual(titles, expectedTitles)
    }

    func testRentHouseUnknown() {
        let house = Self.makeHouseUnknown()
        let offer = Self.makeOfferUnknown(
            type: .rent, 
            category: .house,
            house: house
        )
        let viewModels = OfferCardComfortViewModelGenerator.makeViewModel(offer: offer)

        let titles = viewModels?.map(\.titleText)
        let expectedTitles: [String] = []

        XCTAssertEqual(titles, expectedTitles)
    }

    func testRentGaragePositive() {
        let building = Self.makeBuildingPositive()
        let apartment = Self.makeApartmentPositive()
        let garage = Self.makeGaragePositive()
        let offer = Self.makeOfferPositive(
            type: .rent, 
            category: .garage,
            building: building,
            apartment: apartment,
            garage: garage
        )
        let viewModels = OfferCardComfortViewModelGenerator.makeViewModel(offer: offer)

        let titles = viewModels?.map(\.titleText)
        let expectedTitles = [
            "Пропускная система",
            "Видеонаблюдение",
            "Отопление",
            "Водопровод",
            "Электричество",
            "Подвал-погреб",
            "Охрана",
            "Доступ на объект 24/7",
            "Пожарная сигнализация",
            "Автоматические ворота",
            "Смотровая яма",
            "Автомойка",
            "Автосервис",
        ]

        XCTAssertEqual(titles, expectedTitles)
    }

    func testRentGarageNegative() {
        let building = Self.makeBuildingNegative()
        let apartment = Self.makeApartmentNegative()
        let garage = Self.makeGarageNegative()
        let offer = Self.makeOfferNegative(
            type: .rent, 
            category: .garage,
            building: building,
            apartment: apartment,
            garage: garage
        )
        let viewModels = OfferCardComfortViewModelGenerator.makeViewModel(offer: offer)

        let titles = viewModels?.map(\.titleText)
        let expectedTitles = [
            "Пропускной системы нет",
            "Видеонаблюдения нет",
            "Отопления нет",
            "Водопровода нет",
            "Электричества нет",
            "Подвала-погреба нет",
            "Охраны нет",
            "Доступа на объект 24/7 нет",
            "Пожарной сигнализации нет",
            "Автоматических ворот нет",
            "Смотровой ямы нет",
            "Автомойки нет",
            "Автосервиса нет",
        ]

        XCTAssertEqual(titles, expectedTitles)
    }

    func testRentGarageUnknown() {
        let building = Self.makeBuildingUnknown()
        let apartment = Self.makeApartmentUnknown()
        let garage = Self.makeGarageUnknown()
        let offer = Self.makeOfferUnknown(
            type: .rent, 
            category: .garage,
            building: building,
            apartment: apartment,
            garage: garage
        )
        let viewModels = OfferCardComfortViewModelGenerator.makeViewModel(offer: offer)

        let titles = viewModels?.map(\.titleText)
        let expectedTitles: [String] = []

        XCTAssertEqual(titles, expectedTitles)
    }
}
