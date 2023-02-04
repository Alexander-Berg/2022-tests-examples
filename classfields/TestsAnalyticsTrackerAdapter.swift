//
//  TestsAnalyticsTrackerAdapter.swift
//  YREAnalytics
//
//  Created by Dmitry Barillo on 09.06.2022.
//  Copyright © 2022 Yandex. All rights reserved.
//

import Foundation

public final class TestsAnalyticsTrackerAdapter: YREAnalyticsTrackerAdapter {
    public init(path: String) {
        // swiftlint:disable:next force_unwrapping
        self.targetURL = URL(string: "http://127.0.0.1:8080/analytics/\(path)")!
    }

    // MARK: - YREAnalyticsTrackerAdapter

    public func reportEvent(withName name: String, withPayload payload: [AnyHashable: Any]?) {
        do {
            let urlRequest = try self.makeURLRequest(
                targetURL: self.targetURL,
                eventName: name,
                withPayload: payload
            )

            URLSession.shared.dataTask(with: urlRequest, completionHandler: { _, _, _ in
                // do nothing
            }).resume()
        }
        catch {
            assertionFailure("TestsAnalyticsTrackerAdapter: Invalid json")
        }
    }

    private let targetURL: URL

    private func makeURLRequest(
        targetURL: URL,
        eventName: String,
        withPayload payload: [AnyHashable: Any]?
    ) throws -> URLRequest {
        let jsonObject: [AnyHashable: Any] = [
            "eventName": eventName as NSString,
            "payload": payload as Any
        ]
        let body = try JSONSerialization.data(withJSONObject: jsonObject, options: [])
        var request = URLRequest(url: targetURL)
        request.httpMethod = "POST"
        request.httpBody = body

        return request
    }
}
