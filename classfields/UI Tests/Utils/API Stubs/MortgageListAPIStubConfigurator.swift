//
//  MortgageListAPIStubConfigurator.swift
//  UI Tests
//
//  Created by Fedor Solovev on 29.03.2021.
//  Copyright © 2021 Yandex. All rights reserved.
//

import Foundation

final class MortgageListAPIStubConfigurator {
    enum StubKind {
        case normal
        case initialError
        case filterError
    }

    static func setupMortgageProgramSearch(using dynamicStubs: HTTPDynamicStubs) {
        dynamicStubs.setupStub(remotePath: "/2.0/mortgage/program/search", filename: "mortgage-program.debug")
    }

    static func setupMortgageProgramSearchWithFormAtService(using dynamicStubs: HTTPDynamicStubs) {
        dynamicStubs.setupStub(
            remotePath: "/2.0/mortgage/program/search",
            filename: "mortgage-program-withFormAtService.debug"
        )
    }
}
