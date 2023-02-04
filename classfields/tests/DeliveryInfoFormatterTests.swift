//
//  DeliveryInfoFormatterTests.swift
//  YandexRealtyTests
//
//  Created by Pavel Zhuravlev on 26/03/2019.
//  Copyright © 2019 Yandex. All rights reserved.
//

import XCTest
import YREFormatters
import YREModel
import YREModelObjc

// swiftlint:disable file_length
// swiftlint:disable:next type_body_length
final class DeliveryInfoFormatterTests: XCTestCase {
    let validSiteFinishedDeliveryDate = YRECommissioningDate(year: NSNumber(value: 2100),
                                                             quarter: NSNumber(value: 1),
                                                             finished: .paramBoolTrue)

    let validSiteUnfinishedDeliveryDate = YRECommissioningDate(year: NSNumber(value: 2101),
                                                               quarter: NSNumber(value: 2),
                                                               finished: .paramBoolFalse)
    let unknownDeliveryDate = YRECommissioningDate(year: NSNumber(value: 2101),
                                                   quarter: NSNumber(value: 2),
                                                   finished: .paramBoolUnknown)

    // MARK: - Site

    //
    // MARK: Common cases
    
    // While there's no difference between these cases I prefer to combine them into one test method
    func testSiteAnyInfoWithUnknownState() {
        let formatter = DeliveryInfoFormatter()
        
        let infoWithDates = formatter.deliveryInfo(for: ConstantParamBuildingState.unknown,
                                                   flatStatus: SiteFlatStatus.unknown,
                                                   deliveryDates: [validSiteFinishedDeliveryDate])
        let infoWithoutDates = formatter.deliveryInfo(for: ConstantParamBuildingState.unknown,
                                                      flatStatus: SiteFlatStatus.unknown,
                                                      deliveryDates: [])
        
        let shortInfoWithDates = formatter.shortDeliveryInfo(for: ConstantParamBuildingState.unknown,
                                                             flatStatus: SiteFlatStatus.unknown,
                                                             deliveryDates: [validSiteFinishedDeliveryDate],
                                                             textStyle: .capitalizedFirstWord)
        let shortInfoWithoutDates = formatter.shortDeliveryInfo(for: ConstantParamBuildingState.unknown,
                                                                flatStatus: SiteFlatStatus.unknown,
                                                                deliveryDates: [],
                                                                textStyle: .capitalizedFirstWord)
        
        XCTAssertNil(infoWithDates)
        XCTAssertNil(infoWithoutDates)
        
        XCTAssertNil(shortInfoWithDates)
        XCTAssertNil(shortInfoWithoutDates)
    }
    
    // While there's no difference between these cases I prefer to combine them into one test method
    func testSiteAnyInfoWithHandOverState() {
        let formatter = DeliveryInfoFormatter()
        
        //
        // Common Info
        
        guard let infoWithDates = formatter.deliveryInfo(for: ConstantParamBuildingState.handOver,
                                                         flatStatus: SiteFlatStatus.unknown,
                                                         deliveryDates: [validSiteFinishedDeliveryDate]) else {
            XCTFail("Couldn't format site delivery info with HandOver state and having delivery dates")
            return
        }
        guard let infoWithoutDates = formatter.deliveryInfo(for: ConstantParamBuildingState.handOver,
                                                            flatStatus: SiteFlatStatus.unknown,
                                                            deliveryDates: []) else {
            XCTFail("Couldn't format site delivery info with HandOver state and having no delivery dates")
            return
        }
        
        XCTAssertEqual(infoWithDates, infoWithoutDates, "Site delivery info with HandOver state must be DeliveryDates-independent")
        XCTAssertEqual(infoWithDates, "Сдан")
        
        //
        // Short Info
        
        guard let shortInfoWithDates = formatter.shortDeliveryInfo(for: ConstantParamBuildingState.handOver,
                                                                   flatStatus: SiteFlatStatus.unknown,
                                                                   deliveryDates: [validSiteFinishedDeliveryDate],
                                                                   textStyle: .capitalizedFirstWord) else {
            XCTFail("Couldn't format site short delivery info with HandOver state and having delivery dates")
            return
        }
        guard let shortInfoWithoutDates = formatter.shortDeliveryInfo(for: ConstantParamBuildingState.handOver,
                                                                      flatStatus: SiteFlatStatus.unknown,
                                                                      deliveryDates: [],
                                                                      textStyle: .capitalizedFirstWord) else {
            XCTFail("Couldn't format site short delivery info with HandOver state and having no delivery dates")
            return
        }

        XCTAssertEqual(
            shortInfoWithDates,
            shortInfoWithoutDates,
            "Site short delivery info with HandOver state must be DeliveryDates-independent"
        )
        XCTAssertEqual(shortInfoWithoutDates, "Сдан")
    }
    
    // While there's no difference between these cases I prefer to combine them into one test method
    func testSiteSuspendedState() {
        let formatter = DeliveryInfoFormatter()
        
        //
        // Common Info
        
        guard let infoWithDates = formatter.deliveryInfo(for: ConstantParamBuildingState.suspended,
                                                         flatStatus: SiteFlatStatus.unknown,
                                                         deliveryDates: [validSiteFinishedDeliveryDate]) else {
            XCTFail("Couldn't format site delivery info with Suspended state and having delivery dates")
            return
        }
        guard let infoWithoutDates = formatter.deliveryInfo(for: ConstantParamBuildingState.suspended,
                                                            flatStatus: SiteFlatStatus.unknown,
                                                            deliveryDates: []) else {
            XCTFail("Couldn't format site delivery info with Suspended state and having no delivery dates")
            return
        }
        
        XCTAssertEqual(infoWithoutDates, "Стройка заморожена")
        XCTAssertEqual(infoWithDates, "Есть сданные, но стройка заморожена")
        
        //
        // Short Info
        
        guard let shortInfoWithDates = formatter.shortDeliveryInfo(for: ConstantParamBuildingState.suspended,
                                                                   flatStatus: SiteFlatStatus.unknown,
                                                                   deliveryDates: [validSiteFinishedDeliveryDate],
                                                                   textStyle: .capitalizedFirstWord) else {
            XCTFail("Couldn't format site short delivery info with Suspended state and having delivery dates")
            return
        }
        guard let shortInfoWithoutDates = formatter.shortDeliveryInfo(for: ConstantParamBuildingState.suspended,
                                                                      flatStatus: SiteFlatStatus.unknown,
                                                                      deliveryDates: [],
                                                                      textStyle: .capitalizedFirstWord) else {
            XCTFail("Couldn't format site short delivery info with Suspended state and having no delivery dates")
            return
        }
        
        XCTAssertEqual(shortInfoWithoutDates, "Стройка заморожена")
        XCTAssertEqual(shortInfoWithDates, "Есть сданные, но стройка заморожена")
    }
    
    // While there's no difference between these cases I prefer to combine them into one test method
    func testSiteAnyInfoWithBuiltState() {
        let formatter = DeliveryInfoFormatter()
        
        //
        // Common Info
        
        guard let infoWithDates = formatter.deliveryInfo(for: ConstantParamBuildingState.built,
                                                         flatStatus: SiteFlatStatus.unknown,
                                                         deliveryDates: [validSiteFinishedDeliveryDate]) else {
            XCTFail("Couldn't format site delivery info with Built state and having delivery dates")
            return
        }
        guard let infoWithoutDates = formatter.deliveryInfo(for: ConstantParamBuildingState.built,
                                                            flatStatus: SiteFlatStatus.unknown,
                                                            deliveryDates: []) else {
            XCTFail("Couldn't format site delivery info with Built state and having no delivery dates")
            return
        }
        
        XCTAssertEqual(infoWithDates, infoWithoutDates, "Site delivery info with Built state must be DeliveryDates-independent")
        XCTAssertEqual(infoWithDates, "Построен")
        
        //
        // Short Info
        
        guard let shortInfoWithDates = formatter.shortDeliveryInfo(for: ConstantParamBuildingState.built,
                                                                   flatStatus: SiteFlatStatus.unknown,
                                                                   deliveryDates: [validSiteFinishedDeliveryDate],
                                                                   textStyle: .capitalizedFirstWord) else {
            XCTFail("Couldn't format site short delivery info with Built state and having delivery dates")
            return
        }
        guard let shortInfoWithoutDates = formatter.shortDeliveryInfo(for: ConstantParamBuildingState.built,
                                                                      flatStatus: SiteFlatStatus.unknown,
                                                                      deliveryDates: [],
                                                                      textStyle: .capitalizedFirstWord) else {
            XCTFail("Couldn't format site short delivery info with Built state and having no delivery dates")
            return
        }
        
        XCTAssertEqual(
            shortInfoWithDates,
            shortInfoWithoutDates,
            "Site short delivery info with Built state must be DeliveryDates-independent"
        )
        XCTAssertEqual(shortInfoWithDates, "Построен")
    }

    // MARK: - 'In project' cases

    func testSiteShortInfoWithInProjectState() {
        let formatter = DeliveryInfoFormatter()

        let validFinishedDeliveryDate = YRECommissioningDate(year: NSNumber(value: 2100),
                                                             quarter: NSNumber(value: 1),
                                                             finished: .paramBoolTrue)

        guard let infoWithDates = formatter.shortDeliveryInfo(for: ConstantParamBuildingState.inProject,
                                                              flatStatus: SiteFlatStatus.unknown,
                                                              deliveryDates: [validFinishedDeliveryDate],
                                                              textStyle: .capitalizedFirstWord) else {
            XCTFail("Couldn't format site delivery info with InProject state and having delivery dates")
            return
        }
        guard let infoWithoutDates = formatter.shortDeliveryInfo(for: ConstantParamBuildingState.inProject,
                                                                 flatStatus: SiteFlatStatus.unknown,
                                                                 deliveryDates: [],
                                                                 textStyle: .capitalizedFirstWord) else {
            XCTFail("Couldn't format site delivery info with InProject state and having no delivery dates")
            return
        }

        XCTAssertEqual(infoWithoutDates, "В проекте")
        XCTAssertEqual(infoWithDates, "В проекте, срок сдачи уточняется")
    }

    func testSiteInfoWithInProjectStateWithoutDates() {
        let formatter = DeliveryInfoFormatter()

        guard let info = formatter.deliveryInfo(for: ConstantParamBuildingState.inProject,
                                                flatStatus: SiteFlatStatus.unknown,
                                                deliveryDates: []) else {
            XCTFail("Couldn't format site delivery info with InProject state and having no delivery dates")
            return
        }

        XCTAssertEqual(info, "В проекте, срок сдачи уточняется")
    }

    func testSiteInfoWithInProjectStateWithValidUnfinishedDate() {
        let formatter = DeliveryInfoFormatter()

        let unfinishedDeliveryDate = YRECommissioningDate(year: NSNumber(value: 2101),
                                                          quarter: NSNumber(value: 2),
                                                          finished: .paramBoolFalse)

        guard let info = formatter.deliveryInfo(for: ConstantParamBuildingState.inProject,
                                                flatStatus: SiteFlatStatus.unknown,
                                                deliveryDates: [unfinishedDeliveryDate]) else {
            XCTFail("Couldn't format site delivery info with InProject state and having a single valid unfinished delivery date")
            return
        }

        XCTAssertEqual(info, "В проекте, 2-й\u{00A0}квартал 2101\u{00A0}г.")
    }

    func testSiteInfoWithInProjectStateWithWrongUnfinishedDate() {
        let formatter = DeliveryInfoFormatter()

        let wrongUnfinishedDeliveryDate = YRECommissioningDate(year: NSNumber(value: 2101),
                                                               quarter: NSNumber(value: 0), // wrong quarter
                                                               finished: .paramBoolFalse)

        guard let info = formatter.deliveryInfo(for: ConstantParamBuildingState.inProject,
                                                flatStatus: SiteFlatStatus.unknown,
                                                deliveryDates: [wrongUnfinishedDeliveryDate]) else {
            XCTFail("Couldn't format site delivery info with InProject state and having a single wrong unfinished delivery date")
            return
        }

        XCTAssertEqual(info, "В проекте, срок сдачи уточняется")
    }

    func testSiteInfoWithInProjectStateWithTwoUnfinishedDates() {
        let formatter = DeliveryInfoFormatter()

        let deliveryDate1 = YRECommissioningDate(year: NSNumber(value: 2100),
                                                 quarter: NSNumber(value: 2),
                                                 finished: .paramBoolFalse)

        let deliveryDate2 = YRECommissioningDate(year: NSNumber(value: 2101),
                                                 quarter: NSNumber(value: 1),
                                                 finished: .paramBoolFalse)

        guard let info = formatter.deliveryInfo(for: ConstantParamBuildingState.inProject,
                                                flatStatus: SiteFlatStatus.unknown,
                                                deliveryDates: [deliveryDate1, deliveryDate2]) else {
            XCTFail("Couldn't format site delivery info with InProject state and having two unfinished dates")
            return
        }

        XCTAssertEqual(info, "В проекте, 2-й\u{00A0}квартал 2100\u{00A0}г. — 1-й\u{00A0}квартал 2101\u{00A0}г.")
    }

    func testSiteInfoWithInProjectStateWithTwoUnfinishedDatesInWrongOrder() {
        let formatter = DeliveryInfoFormatter()

        let deliveryDate1 = YRECommissioningDate(year: NSNumber(value: 2101),
                                                 quarter: NSNumber(value: 1),
                                                 finished: .paramBoolFalse)

        let deliveryDate2 = YRECommissioningDate(year: NSNumber(value: 2100),
                                                 quarter: NSNumber(value: 2),
                                                 finished: .paramBoolFalse)

        guard let info = formatter.deliveryInfo(for: ConstantParamBuildingState.inProject,
                                                flatStatus: SiteFlatStatus.unknown,
                                                deliveryDates: [deliveryDate1, deliveryDate2]) else {
            XCTFail("Couldn't format site delivery info with InProject state and having unfinished delivery dates in wrong order")
            return
        }

        XCTAssertEqual(info, "В проекте, 2-й\u{00A0}квартал 2100\u{00A0}г. — 1-й\u{00A0}квартал 2101\u{00A0}г.")
    }

    //
    // MARK: Special cases
    
    func testSiteShortInfoWithUnfinishedState() {
        let formatter = DeliveryInfoFormatter()
        
        let validFinishedDeliveryDate = YRECommissioningDate(year: NSNumber(value: 2100),
                                                             quarter: NSNumber(value: 1),
                                                             finished: .paramBoolTrue)
        
        guard let infoWithDates = formatter.shortDeliveryInfo(for: ConstantParamBuildingState.unfinished,
                                                              flatStatus: SiteFlatStatus.unknown,
                                                              deliveryDates: [validFinishedDeliveryDate],
                                                              textStyle: .capitalizedFirstWord) else {
            XCTFail("Couldn't format site delivery info with Unfinished state and having delivery dates")
            return
        }
        guard let infoWithoutDates = formatter.shortDeliveryInfo(for: ConstantParamBuildingState.unfinished,
                                                                 flatStatus: SiteFlatStatus.unknown,
                                                                 deliveryDates: [],
                                                                 textStyle: .capitalizedFirstWord) else {
            XCTFail("Couldn't format site delivery info with Unfinished state and having no delivery dates")
            return
        }
        
        XCTAssertEqual(infoWithoutDates, "Строится")
        XCTAssertEqual(infoWithDates, "Строится, есть сданные")
    }
    
    func testSiteInfoWithUnfinishedStateWithoutDates() {
        let formatter = DeliveryInfoFormatter()
        
        guard let info = formatter.deliveryInfo(for: ConstantParamBuildingState.unfinished,
                                                flatStatus: SiteFlatStatus.unknown,
                                                deliveryDates: []) else {
            XCTFail("Couldn't format site delivery info with Unfinished state and having no delivery dates")
            return
        }
        
        XCTAssertEqual(info, "Строится")
    }
    
    func testSiteInfoWithUnfinishedStateWithValidUnfinishedDate() {
        let formatter = DeliveryInfoFormatter()
        
        let unfinishedDeliveryDate = YRECommissioningDate(year: NSNumber(value: 2101),
                                                          quarter: NSNumber(value: 2),
                                                          finished: .paramBoolFalse)
        
        guard let info = formatter.deliveryInfo(for: ConstantParamBuildingState.unfinished,
                                                flatStatus: SiteFlatStatus.unknown,
                                                deliveryDates: [unfinishedDeliveryDate]) else {
            XCTFail("Couldn't format site delivery info with Unfinished state and having a single valid unfinished delivery date")
            return
        }
        
        XCTAssertEqual(info, "2-й\u{00A0}квартал 2101\u{00A0}г.")
    }
    
    func testSiteInfoWithUnfinishedStateWithWrongUnfinishedDate() {
        let formatter = DeliveryInfoFormatter()
        
        let wrongUnfinishedDeliveryDate = YRECommissioningDate(year: NSNumber(value: 2101),
                                                               quarter: NSNumber(value: 0), // wrong quarter
                                                               finished: .paramBoolFalse)
        
        guard let info = formatter.deliveryInfo(for: ConstantParamBuildingState.unfinished,
                                                flatStatus: SiteFlatStatus.unknown,
                                                deliveryDates: [wrongUnfinishedDeliveryDate]) else {
            XCTFail("Couldn't format site delivery info with Unfinished state and having a single wrong unfinished delivery date")
            return
        }
        
        XCTAssertEqual(info, "Строится")
    }
    
    func testSiteInfoWithUnfinishedStateWithFinishedDateOnly() {
        let formatter = DeliveryInfoFormatter()
        
        let validFinishedDeliveryDate = YRECommissioningDate(year: NSNumber(value: 2100),
                                                             quarter: NSNumber(value: 1),
                                                             finished: .paramBoolTrue)
        
        guard let info = formatter.deliveryInfo(for: ConstantParamBuildingState.unfinished,
                                                flatStatus: SiteFlatStatus.unknown,
                                                deliveryDates: [validFinishedDeliveryDate]) else {
            XCTFail("Couldn't format site delivery info with Unfinished state and having a single finished delivery date")
            return
        }
        
        XCTAssertEqual(info, "Строится, есть сданные")
    }
    
    func testSiteInfoWithUnfinishedStateWithFinishedAndUnfinishedDates() {
        let formatter = DeliveryInfoFormatter()
        
        let validFinishedDeliveryDate = YRECommissioningDate(year: NSNumber(value: 2100),
                                                             quarter: NSNumber(value: 1),
                                                             finished: .paramBoolTrue)
        
        let validUnfinishedDeliveryDate = YRECommissioningDate(year: NSNumber(value: 2101),
                                                               quarter: NSNumber(value: 2),
                                                               finished: .paramBoolFalse)
        
        guard let info = formatter.deliveryInfo(for: ConstantParamBuildingState.unfinished,
                                                flatStatus: SiteFlatStatus.unknown,
                                                deliveryDates: [validFinishedDeliveryDate, validUnfinishedDeliveryDate]) else {
            XCTFail("Couldn't format site delivery info with Unfinished state and having finished and unfinished delivery dates")
            return
        }
        
        XCTAssertEqual(info, "2-й\u{00A0}квартал 2101\u{00A0}г., есть\u{00A0}сданные")
    }
    
    func testSiteInfoWithUnfinishedStateWithTwoUnfinishedAndWithoutFinishedDates() {
        let formatter = DeliveryInfoFormatter()
        
        let deliveryDate1 = YRECommissioningDate(year: NSNumber(value: 2100),
                                                 quarter: NSNumber(value: 2),
                                                 finished: .paramBoolFalse)
        
        let deliveryDate2 = YRECommissioningDate(year: NSNumber(value: 2101),
                                                 quarter: NSNumber(value: 1),
                                                 finished: .paramBoolFalse)
        
        guard let info = formatter.deliveryInfo(for: ConstantParamBuildingState.unfinished,
                                                flatStatus: SiteFlatStatus.unknown,
                                                deliveryDates: [deliveryDate1, deliveryDate2]) else {
            XCTFail("Couldn't format site delivery info with Unfinished state and having two unfinished dates")
            return
        }
        
        XCTAssertEqual(info, "2-й\u{00A0}квартал 2100\u{00A0}г. — 1-й\u{00A0}квартал 2101\u{00A0}г.")
    }
    
    func testSiteInfoWithUnfinishedStateWithTwoUnfinishedAndWithFinishedDates() {
        let formatter = DeliveryInfoFormatter()
        
        let deliveryDate1 = YRECommissioningDate(year: NSNumber(value: 2100),
                                                 quarter: NSNumber(value: 2),
                                                 finished: .paramBoolFalse)
        
        let deliveryDate2 = YRECommissioningDate(year: NSNumber(value: 2101),
                                                 quarter: NSNumber(value: 1),
                                                 finished: .paramBoolFalse)
        
        let finishedDeliveryDate = YRECommissioningDate(year: NSNumber(value: 2103),
                                                        quarter: NSNumber(value: 3),
                                                        finished: .paramBoolTrue)
        
        guard let info = formatter.deliveryInfo(for: ConstantParamBuildingState.unfinished,
                                                flatStatus: SiteFlatStatus.unknown,
                                                deliveryDates: [deliveryDate1, deliveryDate2, finishedDeliveryDate]) else {
            XCTFail("Couldn't format site delivery info with Unfinished state and having two unfinished and one finished dates")
            return
        }
        
        XCTAssertEqual(info, "2-й\u{00A0}квартал 2100\u{00A0}г. — 1-й\u{00A0}квартал 2101\u{00A0}г., есть\u{00A0}сданные")
    }
    
    func testSiteInfoWithUnfinishedStateWithTwoUnfinishedDatesInWrongOrder() {
        let formatter = DeliveryInfoFormatter()
        
        let deliveryDate1 = YRECommissioningDate(year: NSNumber(value: 2101),
                                                 quarter: NSNumber(value: 1),
                                                 finished: .paramBoolFalse)
        
        let deliveryDate2 = YRECommissioningDate(year: NSNumber(value: 2100),
                                                 quarter: NSNumber(value: 2),
                                                 finished: .paramBoolFalse)
        
        guard let info = formatter.deliveryInfo(for: ConstantParamBuildingState.unfinished,
                                                flatStatus: SiteFlatStatus.unknown,
                                                deliveryDates: [deliveryDate1, deliveryDate2]) else {
            XCTFail("Couldn't format site delivery info with Unfinished state and having unfinished delivery dates in wrong order")
            return
        }
        
        XCTAssertEqual(info, "2-й\u{00A0}квартал 2100\u{00A0}г. — 1-й\u{00A0}квартал 2101\u{00A0}г.")
    }
    
    func testSiteInfoWithUnfinishedStateWithTwoEqualUnfinishedDates() {
        let formatter = DeliveryInfoFormatter()
        
        let deliveryDate1 = YRECommissioningDate(year: NSNumber(value: 2100),
                                                 quarter: NSNumber(value: 2),
                                                 finished: .paramBoolFalse)
        
        let deliveryDate2 = YRECommissioningDate(year: NSNumber(value: 2100),
                                                 quarter: NSNumber(value: 2),
                                                 finished: .paramBoolFalse)
        
        guard let info = formatter.deliveryInfo(for: ConstantParamBuildingState.unfinished,
                                                flatStatus: SiteFlatStatus.unknown,
                                                deliveryDates: [deliveryDate1, deliveryDate2]) else {
            XCTFail("Couldn't format site delivery info with Unfinished state and having two equal unfinished delivery dates")
            return
        }
        
        XCTAssertEqual(info, "2-й\u{00A0}квартал 2100\u{00A0}г.")
    }

    func testSiteInfoWithUnknownStateWithFlatStatusSoonAvailable() {
        let formatter = DeliveryInfoFormatter()

        guard let info = formatter.deliveryInfo(for: ConstantParamBuildingState.unknown,
                                                flatStatus: SiteFlatStatus.soonAvailable,
                                                deliveryDates: []) else {
            XCTFail("Couldn't format site delivery info with Unknown state and flat status soon available")
            return
        }

        XCTAssertEqual(info, "Скоро в продаже")
    }

    //
    // MARK: Styles
    
    func testSiteShortInfoWithUppercasedStyle() {
        let formatter = DeliveryInfoFormatter()
        
        guard let unfinishedInfo = formatter.shortDeliveryInfo(for: ConstantParamBuildingState.unfinished,
                                                               flatStatus: SiteFlatStatus.unknown,
                                                               deliveryDates: [],
                                                               textStyle: .uppercased) else {
            XCTFail("Couldn't format site short delivery info with Unfinished state and uppercased style")
            return
        }
        
        let deliveryDate = YRECommissioningDate(year: NSNumber(value: 2100),
                                                quarter: NSNumber(value: 1),
                                                finished: .paramBoolTrue)
        
        guard let unfinishedWithDatesInfo = formatter.shortDeliveryInfo(for: ConstantParamBuildingState.unfinished,
                                                                        flatStatus: SiteFlatStatus.unknown,
                                                                        deliveryDates: [deliveryDate],
                                                                        textStyle: .uppercased) else {
            XCTFail("Couldn't format site short delivery info with Unfinished state and uppercased style")
            return
        }
        
        guard let suspendedInfo = formatter.shortDeliveryInfo(for: ConstantParamBuildingState.suspended,
                                                              flatStatus: SiteFlatStatus.unknown,
                                                              deliveryDates: [],
                                                              textStyle: .uppercased) else {
            XCTFail("Couldn't format site short delivery info with Suspended state and uppercased style")
            return
        }
        
        guard let handOverInfo = formatter.shortDeliveryInfo(for: ConstantParamBuildingState.handOver,
                                                             flatStatus: SiteFlatStatus.unknown,
                                                             deliveryDates: [],
                                                             textStyle: .uppercased) else {
            XCTFail("Couldn't format site short delivery info with HandOver state and uppercased style")
            return
        }
        
        guard let builtInfo = formatter.shortDeliveryInfo(for: ConstantParamBuildingState.built,
                                                          flatStatus: SiteFlatStatus.unknown,
                                                          deliveryDates: [],
                                                          textStyle: .uppercased) else {
            XCTFail("Couldn't format site short delivery info with Built state and uppercased style")
            return
        }

        guard let inProjecInfo = formatter.shortDeliveryInfo(for: ConstantParamBuildingState.inProject,
                                                             flatStatus: SiteFlatStatus.unknown,
                                                             deliveryDates: [],
                                                             textStyle: .uppercased) else {
            XCTFail("Couldn't format site short delivery info with InProject state and uppercased style")
            return
        }

        guard let inProjectWithDatesInfo = formatter.shortDeliveryInfo(for: ConstantParamBuildingState.inProject,
                                                                       flatStatus: SiteFlatStatus.unknown,
                                                                       deliveryDates: [deliveryDate],
                                                                       textStyle: .uppercased) else {
            XCTFail("Couldn't format site short delivery info with InProject state and uppercased style")
            return
        }
        
        XCTAssertEqual(unfinishedInfo, "СТРОИТСЯ")
        XCTAssertEqual(unfinishedWithDatesInfo, "СТРОИТСЯ, ЕСТЬ СДАННЫЕ")
        XCTAssertEqual(suspendedInfo, "СТРОЙКА ЗАМОРОЖЕНА")
        XCTAssertEqual(handOverInfo, "СДАН")
        XCTAssertEqual(builtInfo, "ПОСТРОЕН")
        XCTAssertEqual(inProjecInfo, "В ПРОЕКТЕ")
        XCTAssertEqual(inProjectWithDatesInfo, "В ПРОЕКТЕ, СРОК СДАЧИ УТОЧНЯЕТСЯ")
    }

    // MARK: - Village

    private func makeDeliveryDateHelper(status: VillagePhaseStatus, isFinished: Bool) -> VillageDeliveryDate {
        return VillageDeliveryDate(name: "test", index: 0, status: status, year: 2100, quarter: 1, isFinished: isFinished)
    }

    // MARK: Common cases

    func testVillageWithUnknownState() {
        let formatter = DeliveryInfoFormatter()

        let shortInfoWithEmptyDates = formatter.shortDeliveryInfo(for: [], textStyle: .capitalizedFirstWord)

        let unknownDeliveryDates = [self.makeDeliveryDateHelper(status: .unknown, isFinished: false)]
        let shortInfoWithUnknownDates = formatter.shortDeliveryInfo(for: unknownDeliveryDates, textStyle: .capitalizedFirstWord)

        XCTAssertNil(shortInfoWithEmptyDates)
        XCTAssertNil(shortInfoWithUnknownDates)
    }

    func testVillageWithHandOverState() {
        let formatter = DeliveryInfoFormatter()

        // One delivery date
        let shortInfoWithHandOverStateOneDate = formatter.shortDeliveryInfo(
            for: [self.makeDeliveryDateHelper(status: .handOver, isFinished: true)],
            textStyle: .capitalizedFirstWord
        )

        // Multiple delivery dates
        // All dates should be in .handover status
        let shortInfoWithSameHandOverDates = formatter.shortDeliveryInfo(
            for: [
                self.makeDeliveryDateHelper(status: .handOver, isFinished: true),
                self.makeDeliveryDateHelper(status: .handOver, isFinished: true),
                self.makeDeliveryDateHelper(status: .handOver, isFinished: true)
            ],
            textStyle: .capitalizedFirstWord
        )

        let shortInfoWithDifferentHandOverDates = formatter.shortDeliveryInfo(
            for: [
                self.makeDeliveryDateHelper(status: .handOver, isFinished: true),
                self.makeDeliveryDateHelper(status: .handOver, isFinished: true),
                self.makeDeliveryDateHelper(status: .unknown, isFinished: true)
            ],
            textStyle: .capitalizedFirstWord
        )

        XCTAssertEqual(shortInfoWithHandOverStateOneDate, "Сдан")
        XCTAssertEqual(shortInfoWithSameHandOverDates, "Сдан")
        XCTAssertNil(shortInfoWithDifferentHandOverDates)
    }

    func testVillageWithBuiltState() {
        let formatter = DeliveryInfoFormatter()

        // One delivery date
        let shortInfoWithBuiltStateOneDate = formatter.shortDeliveryInfo(
            for: [self.makeDeliveryDateHelper(status: .built, isFinished: true)],
            textStyle: .capitalizedFirstWord
        )

        // Multiple delivery dates
        // All dates should be in .handover status
        let shortInfoWithSameBuiltDates = formatter.shortDeliveryInfo(
            for: [
                self.makeDeliveryDateHelper(status: .built, isFinished: true),
                self.makeDeliveryDateHelper(status: .built, isFinished: true),
                self.makeDeliveryDateHelper(status: .built, isFinished: true)
            ],
            textStyle: .capitalizedFirstWord
        )

        let shortInfoWithDifferentBuiltDates = formatter.shortDeliveryInfo(
            for: [
                self.makeDeliveryDateHelper(status: .built, isFinished: true),
                self.makeDeliveryDateHelper(status: .built, isFinished: true),
                self.makeDeliveryDateHelper(status: .unknown, isFinished: true)
            ],
            textStyle: .capitalizedFirstWord
        )

        XCTAssertEqual(shortInfoWithBuiltStateOneDate, "Построен")
        XCTAssertEqual(shortInfoWithSameBuiltDates, "Построен")
        XCTAssertNil(shortInfoWithDifferentBuiltDates)
    }

    func testVillageWithUnfinishedStateShort() {
        let formatter = DeliveryInfoFormatter()

        // Unfinished, w/ finished
        let shortInfoUnfinisedWithSomeFinished = formatter.shortDeliveryInfo(
            for: [self.makeDeliveryDateHelper(status: .unfinished, isFinished: true)],
            textStyle: .capitalizedFirstWord
        )

        // Unfinished, w/o finished
        let shortInfoUnfinised = formatter.shortDeliveryInfo(
            for: [self.makeDeliveryDateHelper(status: .unfinished, isFinished: false)],
            textStyle: .capitalizedFirstWord
        )

        XCTAssertEqual(shortInfoUnfinised, "Строится")
        XCTAssertEqual(shortInfoUnfinisedWithSomeFinished, "Строится, есть сданные")
    }

    func testVillageWithSuspendedState() {
        let formatter = DeliveryInfoFormatter()

        // Suspended, w/ finished
        let shortInfoSuspendedWithSomeFinished = formatter.shortDeliveryInfo(
            for: [self.makeDeliveryDateHelper(status: .suspended, isFinished: true)],
            textStyle: .capitalizedFirstWord
        )

        // Suspended, w/o finished
        let shortInfoSuspended = formatter.shortDeliveryInfo(
            for: [self.makeDeliveryDateHelper(status: .suspended, isFinished: false)],
            textStyle: .capitalizedFirstWord
        )

        XCTAssertEqual(shortInfoSuspended, "Стройка заморожена")
        XCTAssertEqual(shortInfoSuspendedWithSomeFinished, "Есть сданные, но стройка заморожена")
    }

    func testVillageWithUnfinishedStateLong() {
        let formatter = DeliveryInfoFormatter()

        let dates = [
            VillageDeliveryDate(name: "test", index: 0, status: .unfinished, year: 2100, quarter: 2, isFinished: false),
            VillageDeliveryDate(name: "test", index: 0, status: .unfinished, year: 2101, quarter: 4, isFinished: false)
        ]

        let formattedStringCommon = formatter.deliveryInfo(for: dates)
        let formattedStringOfferList = formatter.deliveryInfoInOfferList(for: dates)

        XCTAssertEqual(formattedStringCommon, "2-й\u{00A0}квартал 2100\u{00A0}г. — 4-й\u{00A0}квартал 2101\u{00A0}г.")
        XCTAssertEqual(formattedStringOfferList, "Срок сдачи 2\u{00A0}кв. 2100 — 4\u{00A0}кв. 2101")
    }

    func testVillageWithUnfinishedButSomeFinishedStateLong() {
        let formatter = DeliveryInfoFormatter()

        let dates = [
            VillageDeliveryDate(name: "test", index: 0, status: .unfinished, year: 2100, quarter: 2, isFinished: false),
            VillageDeliveryDate(name: "test", index: 0, status: .unfinished, year: 2101, quarter: 4, isFinished: false),
            VillageDeliveryDate(name: "test", index: 0, status: .unfinished, year: 2100, quarter: 1, isFinished: true)
        ]

        let formattedStringCommon = formatter.deliveryInfo(for: dates)
        let formattedStringOfferList = formatter.deliveryInfoInOfferList(for: dates)

        XCTAssertEqual(formattedStringCommon, "2-й\u{00A0}квартал 2100\u{00A0}г. — 4-й\u{00A0}квартал 2101\u{00A0}г., есть\u{00A0}сданные")
        XCTAssertEqual(formattedStringOfferList, "Срок сдачи 2\u{00A0}кв. 2100 — 4\u{00A0}кв. 2101, есть\u{00A0}сданные")
    }

    func testVillageWithUnfinishedOneDateStateLong() {
        let formatter = DeliveryInfoFormatter()

        let dates = [
            VillageDeliveryDate(name: "test", index: 0, status: .unfinished, year: 2100, quarter: 2, isFinished: false)
        ]

        let formattedStringCommon = formatter.deliveryInfo(for: dates)
        let formattedStringOfferList = formatter.deliveryInfoInOfferList(for: dates)

        XCTAssertEqual(formattedStringCommon, "2-й\u{00A0}квартал 2100\u{00A0}г.")
        XCTAssertEqual(formattedStringOfferList, "Срок сдачи 2-й\u{00A0}квартал 2100\u{00A0}г.")
    }

    func testOfferPlanInfoWithUnknownState() {
        let formatter = DeliveryInfoFormatter()

        let infoWithDates = formatter.deliveryInfoInOfferPlanList(deliveryDates: [unknownDeliveryDate])
        let infoWithoutDates = formatter.deliveryInfoInOfferPlanList(deliveryDates: [])

        XCTAssertNil(infoWithDates)
        XCTAssertNil(infoWithoutDates)
    }

    func testOfferPlanWithUnfinishedStateWithFinishedDateOnly() {
        let formatter = DeliveryInfoFormatter()

        let validFinishedDeliveryDate = YRECommissioningDate(year: NSNumber(value: 2100),
                                                             quarter: NSNumber(value: 1),
                                                             finished: .paramBoolTrue)

        guard let info = formatter.deliveryInfoInOfferPlanList(deliveryDates: [validFinishedDeliveryDate]) else {
            XCTFail("Couldn't format delivery info with Unfinished state and having a single finished delivery date")
            return
        }

        XCTAssertEqual(info, "Сдан")
    }

    func testOfferPlanWithUnfinishedStateWithFinishedAndUnfinishedDates() {
        let formatter = DeliveryInfoFormatter()

        let validFinishedDeliveryDate = YRECommissioningDate(year: NSNumber(value: 2100),
                                                             quarter: NSNumber(value: 1),
                                                             finished: .paramBoolTrue)

        let validUnfinishedDeliveryDate = YRECommissioningDate(year: NSNumber(value: 2101),
                                                               quarter: NSNumber(value: 2),
                                                               finished: .paramBoolFalse)
        let deliveryDates = [validFinishedDeliveryDate, validUnfinishedDeliveryDate]

        guard let info = formatter.deliveryInfoInOfferPlanList(deliveryDates: deliveryDates) else {
            XCTFail("Couldn't format delivery info with Unfinished state and having finished and unfinished delivery dates")
            return
        }

        XCTAssertEqual(info, "2-й\u{00A0}квартал 2101\u{00A0}г., есть\u{00A0}сданные")
    }

    func testOfferPlanWithUnfinishedStateWithTwoUnfinishedAndWithoutFinishedDates() {
        let formatter = DeliveryInfoFormatter()

        let deliveryDate1 = YRECommissioningDate(year: NSNumber(value: 2100),
                                                 quarter: NSNumber(value: 2),
                                                 finished: .paramBoolFalse)

        let deliveryDate2 = YRECommissioningDate(year: NSNumber(value: 2101),
                                                 quarter: NSNumber(value: 1),
                                                 finished: .paramBoolFalse)

        guard let info = formatter.deliveryInfoInOfferPlanList(deliveryDates: [deliveryDate1, deliveryDate2]) else {
            XCTFail("Couldn't format delivery info with Unfinished state and having two unfinished dates")
            return
        }

        XCTAssertEqual(info, "2\u{00A0}кв. 2100 — 1\u{00A0}кв. 2101")
    }

    func testOfferPlanWithUnfinishedStateWithTwoUnfinishedAndWithFinishedDates() {
        let formatter = DeliveryInfoFormatter()

        let deliveryDate1 = YRECommissioningDate(year: NSNumber(value: 2100),
                                                 quarter: NSNumber(value: 2),
                                                 finished: .paramBoolFalse)

        let deliveryDate2 = YRECommissioningDate(year: NSNumber(value: 2101),
                                                 quarter: NSNumber(value: 1),
                                                 finished: .paramBoolFalse)

        let finishedDeliveryDate = YRECommissioningDate(year: NSNumber(value: 2103),
                                                        quarter: NSNumber(value: 3),
                                                        finished: .paramBoolTrue)
        let deliveryDates = [deliveryDate1, deliveryDate2, finishedDeliveryDate]

        guard let info = formatter.deliveryInfoInOfferPlanList(deliveryDates: deliveryDates) else {
            XCTFail("Couldn't format delivery info with Unfinished state and having two unfinished and one finished dates")
            return
        }

        XCTAssertEqual(info, "2\u{00A0}кв. 2100 — 1\u{00A0}кв. 2101, есть\u{00A0}сданные")
    }

    func testOfferPlanWithUnfinishedStateWithTwoUnfinishedDatesInWrongOrder() {
        let formatter = DeliveryInfoFormatter()

        let deliveryDate1 = YRECommissioningDate(year: NSNumber(value: 2101),
                                                 quarter: NSNumber(value: 1),
                                                 finished: .paramBoolFalse)

        let deliveryDate2 = YRECommissioningDate(year: NSNumber(value: 2100),
                                                 quarter: NSNumber(value: 2),
                                                 finished: .paramBoolFalse)

        guard let info = formatter.deliveryInfoInOfferPlanList(deliveryDates: [deliveryDate1, deliveryDate2]) else {
            XCTFail("Couldn't format delivery info with Unfinished state and having unfinished delivery dates in wrong order")
            return
        }

        XCTAssertEqual(info, "2\u{00A0}кв. 2100 — 1\u{00A0}кв. 2101")
    }

    func testOfferPlanWithUnfinishedStateWithTwoEqualUnfinishedDates() {
        let formatter = DeliveryInfoFormatter()

        let deliveryDate1 = YRECommissioningDate(year: NSNumber(value: 2100),
                                                 quarter: NSNumber(value: 2),
                                                 finished: .paramBoolFalse)

        let deliveryDate2 = YRECommissioningDate(year: NSNumber(value: 2100),
                                                 quarter: NSNumber(value: 2),
                                                 finished: .paramBoolFalse)

        guard let info = formatter.deliveryInfoInOfferPlanList(deliveryDates: [deliveryDate1, deliveryDate2]) else {
            XCTFail("Couldn't format delivery info with Unfinished state and having two equal unfinished delivery dates")
            return
        }

        XCTAssertEqual(info, "2-й\u{00A0}квартал 2100\u{00A0}г.")
    }
}
