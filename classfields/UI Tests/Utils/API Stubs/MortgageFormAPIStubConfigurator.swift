//
//  MortgageFormAPIStubConfigurator.swift
//  UI Tests
//
//  Created by Timur Guliamov on 03.11.2021.
//  Copyright © 2021 Yandex. All rights reserved.
//

import Foundation

final class MortgageFormAPIStubConfigurator {
    enum MortgageDemandStubKind {
        enum Error {
            case phoneValidation
            case other
        }

        case successed
        case failed(Error)
    }

    enum MortgageDemandCommitStubKind {
        enum Error {
            case badCode
            case other
        }

        case successed
        case failed(Error)
    }

    static func setupMortgageMortgageDemand(
        using dynamicStubs: HTTPDynamicStubs,
        stubKind: MortgageDemandStubKind
    ) {
        dynamicStubs.setupStub(
            remotePath: "/2.0/mortgage/mortgage-demand",
            filename: Self.filenameForStubKind(stubKind),
            method: .POST
        )
    }

    static func setupMortgageMortgageDemandCommit(
        using dynamicStubs: HTTPDynamicStubs,
        stubKind: MortgageDemandCommitStubKind
    ) {
        dynamicStubs.setupStub(
            remotePath: "/2.0/mortgage/mortgage-demand/commit",
            filename: Self.filenameForStubKind(stubKind),
            method: .POST
        )
    }

    static func setupUserWithPhoneNumber(using dynamicStubs: HTTPDynamicStubs) {
        dynamicStubs.setupStub(
            remotePath: "/1.0/user",
            filename: "mortgage-user-withPhone.debug"
        )
    }

    static func setupUserEmail(using dynamicStubs: HTTPDynamicStubs) {
        dynamicStubs.setupStub(
            remotePath: "/2.0/banker/user/me/email",
            filename: "mortgage-user-email.debug"
        )
    }

    // MARK: - Private

    private static func filenameForStubKind(_ stubKind: MortgageDemandStubKind) -> String {
        switch stubKind {
            case .successed: return "mortgage-demand-success.debug"
            case .failed(let error):
                switch error {
                    case .phoneValidation: return "mortgage-demand-failed-phone.debug"
                    case .other: return "mortgage-demand-failed-other.debug"
                }
        }
    }

    private static func filenameForStubKind(_ stubKind: MortgageDemandCommitStubKind) -> String {
        switch stubKind {
            case .successed: return "commonEmptyResponse.debug"
            case .failed(let error):
                switch error {
                    case .badCode: return "mortgage-demand-commit-failed-badCode.debug"
                    case .other: return "mortgage-demand-commit-failed-other.debug"
                }
        }
    }
}
