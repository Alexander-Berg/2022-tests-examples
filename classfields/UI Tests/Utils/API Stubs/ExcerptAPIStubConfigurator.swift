//
//  ExcerptAPIStubConfigurator.swift
//  UI Tests
//
//  Created by Leontyev Saveliy on 24.12.2020.
//  Copyright © 2020 Yandex. All rights reserved.
//

import Foundation

final class ExcerptsListAPIStubConfigurator {
    enum StubKind: String {
        enum SingleReport: String {
            case doneWithOffer = "paid-report-user-me-doneReportWithOffer.debug"
            case errorWithOffer = "paid-report-user-me-errorReportWithOffer.debug"
        }
        
        enum LongList: String {
            case page1 = "paid-report-user-me-longListPage1.debug"
            case page2 = "paid-report-user-me-longListPage2.debug"
        }
        
        case emptyList = "paid-report-user-me-emptyList.debug"
    }
    
    static func setupEmptyList(using dynamicStubs: HTTPDynamicStubs) {
        dynamicStubs.setupStub(remotePath: self.path, filename: StubKind.emptyList.rawValue)
    }
    
    static func setupSingleReportList(using dynamicStubs: HTTPDynamicStubs, stub: StubKind.SingleReport) {
        dynamicStubs.setupStub(remotePath: self.path, filename: stub.rawValue)
    }
    
    static func setupLongList(using dynamicStubs: HTTPDynamicStubs) {
        let middleware = MiddlewareBuilder()
            .flatMap({ request, _, _ -> MiddlewareBuilder in
                let isFirstPage = request.queryParams.contains(where: { (key, value) -> Bool in
                    return key == "pageNum" && value == "0"
                })
                
                if isFirstPage {
                    return .respondWith(.ok(.contentsOfJSON(StubKind.LongList.page1.rawValue)))
                }
                else {
                    return .respondWith(.ok(.contentsOfJSON(StubKind.LongList.page2.rawValue)))
                }
            })
            .build()
        
        dynamicStubs.register(
            method: .GET,
            path: self.path,
            middleware: middleware,
            context: .init()
        )
    }
    
    private static let path = "/2.0/paid-report/user/me"
}
