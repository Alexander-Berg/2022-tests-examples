//
//  PedestrianRoutePartViewSizeProviderTests.swift
//  Tests
//
//  Created by Vsevolod Mashinson on 30.07.2020.
//  Copyright © 2020 Yandex LLC. All rights reserved.
//

import XCTest
import Foundation
import YandexMapsRx
import RxSwift
import RxCocoa
import YandexMapsUI
import YandexMapsUtils
import YandexMapsCommonTypes
@testable import YandexMapsDirections

struct PedestrianRoutePartInteractorMock: PedestrianRoutePartInteractor {
    
    enum NameKind: CaseIterable {
        case short
        case long
        
        var text: String {
            switch self {
            case .short: return "3 мин"
            case .long: return "5 часов 47 минут 6 секунд"
            }
        }
    }
    let time: LocalizedValue
    init(nameKind: NameKind) {
        switch nameKind {
        case .short: time = LocalizedValue(value: 3 * 60, text: nameKind.text)
        case .long: time = LocalizedValue(value: 5 * 3600 + 47 * 60 + 6, text: nameKind.text)
        }
       
    }
}

class PedestrianRoutePartViewSizeProviderTests: XCTestCase {

    func testSizes() {
        for nameKind in PedestrianRoutePartInteractorMock.NameKind.allCases {
            let interactor = PedestrianRoutePartInteractorMock(nameKind: nameKind)
            let viewModel = PedestrianRoutePartViewModel(interactor: interactor)
            let sizeProviderContentSize = PedestrianRoutePartViewSizeProvider(viewModel: viewModel).contentSize()
            let defaultSizeProviderContentSize = DefaultCommonViewSizeProvider(viewModel: viewModel).contentSize()
            
            let message = """
              nameKind = \(nameKind)
              expected = \(defaultSizeProviderContentSize)
              actual = \(sizeProviderContentSize)
              """
            let widthDiff = abs(sizeProviderContentSize.width - defaultSizeProviderContentSize.width)
            XCTAssertTrue(widthDiff <= 1.0 / UIScreen.main.scale, message)
            XCTAssertTrue(sizeProviderContentSize.height == defaultSizeProviderContentSize.height, message)
        }
    }
}
