//
//  TestError.swift
//  Utils
//
//  Created by Aleksey Makhutin on 11.08.2021.
//  Copyright © 2021 Yandex. All rights reserved.
//

import Foundation

public enum TestError {
    public static let networkError = NSError(domain: NSURLErrorDomain, code: -1009, userInfo: nil)
    public static let someError = NSError(domain: "", code: 1, userInfo: nil)
    public static let paymentRequired402: NSError = {
        let response = URL(string: "https://www.some.ru")
            .map { HTTPURLResponse(url: $0, statusCode: 402, httpVersion: nil, headerFields: nil) }
        let userInfo: [String: Any] = [AFNetworkingOperationFailingURLResponseErrorKey: response as Any]
        return NSError(domain: AFURLResponseSerializationErrorDomain, code: NSURLErrorBadServerResponse, userInfo: userInfo)
    }()
}
