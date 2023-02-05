//
//  MasstransitRoutePartIconViewSizeProviderTests.swift
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
import YandexMapsAssets
@testable import YandexMapsDirections

struct MasstransitRoutePartIconInteractorMock: MasstransitRoutePartIconInteractor {
    
    enum NameKind: CaseIterable {
        case short
        case long
        case empty
        
        var name: String {
            switch self {
            case .short: return "924"
            case .long: return "маршрутка 46 тимирязевского округа"
            case .empty: return ""
            }
        }
    }
    
    var state: ReadonlyVariable<MasstransitRoutePartIconState> {
        return stateImpl.asReadonly()
    }
   
    let isTruncateTransportName: Bool
    
    init(hasIcon: Bool, nameKind: NameKind, isManyVariants: Bool, isTruncateTransportName: Bool) {
        let state = MasstransitRoutePartIconState(
            transportType: hasIcon ? .bus : .underground,
            name: nameKind.name,
            color: DayNightColor(singleColor: UIColor.red),
            isManyVariants: isManyVariants
        )
        self.isTruncateTransportName = isTruncateTransportName
        stateImpl = BehaviorRelay<MasstransitRoutePartIconState>(value: state)
    }

    private let stateImpl: BehaviorRelay<MasstransitRoutePartIconState>
   
}

class MasstransitRoutePartIconViewSizeProviderTests: XCTestCase {

    func testSizes() {
        for hasIcon in [false, true] {
            for nameKind in MasstransitRoutePartIconInteractorMock.NameKind.allCases {
                for isManyVariants in [false, true] {
                    for isTruncateTransportName in [false, true] {
                        let interactor = MasstransitRoutePartIconInteractorMock(
                            hasIcon: hasIcon,
                            nameKind: nameKind,
                            isManyVariants: isManyVariants,
                            isTruncateTransportName: isTruncateTransportName
                        )
                        
                        let viewModel = MasstransitRoutePartIconViewModel(interactor: interactor)
                        let sizeProviderContentSize = MasstransitRoutePartIconViewSizeProvider(viewModel: viewModel)
                           .contentSize()
                       
                        let defaultSizeProviderContentSize = DefaultCommonViewSizeProvider(viewModel: viewModel)
                           .contentSize()
                        let message = """
                            hasIcon = \(hasIcon)
                            nameKind = \(nameKind)
                            isManyVariants = \(isManyVariants)
                            isTruncateTransportName = \(isTruncateTransportName)
                            expected = \(defaultSizeProviderContentSize)
                            actual = \(sizeProviderContentSize)
                            """
                        let widthDiff = abs(sizeProviderContentSize.width - defaultSizeProviderContentSize.width)
                        XCTAssertTrue(widthDiff <= 1.0 / UIScreen.main.scale, message)
                        XCTAssertTrue(sizeProviderContentSize.height == defaultSizeProviderContentSize.height, message)
                    }
                }
            }
        }
    }
}
