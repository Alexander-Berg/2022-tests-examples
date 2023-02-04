//
//  Predicate+HttpRequest.swift
//  UI Tests
//
//  Created by Dmitry Barillo on 08.07.2022.
//  Copyright © 2022 Yandex. All rights reserved.
//

import Foundation
import Swifter

extension Predicate where Target == HttpRequest {
    static func contains(queryItem: URLQueryItem) -> Self {
        let matcher: (HttpRequest) -> Bool = { request -> Bool in
            request.queryItems.contains(queryItem)
        }
        let description = "CONTAINS ITEM '\(queryItem.description)'"
        let predicate = Predicate(matcher: matcher,
                                  description: description)
        return predicate
    }

    static func notContains(queryItem: URLQueryItem) -> Self {
        let matcher: (HttpRequest) -> Bool = { request -> Bool in
            request.queryItems.contains(queryItem) == false
        }
        let description = "NOT CONTAINS ITEM '\(queryItem.description)'"
        let predicate = Predicate(matcher: matcher,
                                  description: description)
        return predicate
    }

    static func contains(queryKey: String) -> Self {
        let matcher: (HttpRequest) -> Bool = { request -> Bool in
            request.queryKeys.contains(queryKey)
        }
        let description = "CONTAINS KEY '\(queryKey)'"
        let predicate = Predicate(matcher: matcher,
                                  description: description)
        return predicate
    }

    static func notContains(queryKey: String) -> Self {
        let matcher: (HttpRequest) -> Bool = { request -> Bool in
            request.queryKeys.contains(queryKey) == false
        }
        let description = "NOT CONTAINS KEY '\(queryKey)'"
        let predicate = Predicate(matcher: matcher,
                                  description: description)
        return predicate
    }

    static func queryItems(
        contain requiredItems: Set<URLQueryItem> = [],
        notContainKeys prohibitedItemsKeys: Set<String> = [],
        notContain prohibitedItems: Set<URLQueryItem> = []
    ) -> Self {
        let matcher: (HttpRequest) -> Bool = { request -> Bool in
            requiredItems.isSubset(of: request.queryItems) &&
            prohibitedItemsKeys.isDisjoint(with: request.queryKeys) &&
            prohibitedItems.isDisjoint(with: request.queryItems)
        }

        let descriptionCI = requiredItems.isEmpty ? nil
            : "CONTAINS ITEMS: \(requiredItems.map { "'\($0.description)'" }.joined(separator: ", "))"
        let descriptionNCK = prohibitedItemsKeys.isEmpty ? nil
            : "NOT CONTAINS KEYS: \(prohibitedItemsKeys.map { "'\($0)'" }.joined(separator: ", "))"
        let descriptionNCI = prohibitedItems.isEmpty ? nil
            : "NOT CONTAINS ITEMS: \(prohibitedItems.map { "'\($0.description)'" }.joined(separator: ", "))"

        let description = [descriptionCI, descriptionNCK, descriptionNCI].compactMap { $0 }.joined(separator: "; ")
        let predicate = Predicate(matcher: matcher,
                                  description: description)
        return predicate
    }

    static func body<T: Codable & Equatable>(
        _ body: T
    ) -> Self {
        let matcher: (HttpRequest) -> Bool = { request -> Bool in
            let data = Data(request.body)
            let model = try? JSONDecoder().decode(T.self, from: data)
            return model == body
        }

        let jsonDescriptionData = (try? JSONEncoder().encode(body)) ?? Data()
        let jsonDescription = String(decoding: jsonDescriptionData, as: UTF8.self)

        let predicate = Predicate(matcher: matcher, description: "Тело запроса: \(jsonDescription)")
        return predicate
    }

    static func contains(jsonPart: JSONObject) -> Self {
        let matcher: (HttpRequest) -> Bool = { request -> Bool in
            let data = Data(request.body)
            guard let jsonObject = JSONObject(data: data) else { return false }
            return jsonObject.contains(jsonPart)
        }
        let description = "CONTAINS JSON: '\(jsonPart)'"
        let predicate = Predicate(matcher: matcher,
                                  description: description)
        return predicate
    }
}

extension HttpRequest {
    fileprivate var queryItems: [URLQueryItem] {
        self.queryParams.map(URLQueryItem.init)
    }

    fileprivate var queryKeys: [String] {
        self.queryParams.map { $0.0 }
    }
}

extension HttpRequest: CustomDebugStringConvertible {
    public var debugDescription: String {
        return self.queryItems.debugDescription
    }
}
