//
//  TestExtensions.swift
//  YandexDiskTests
//
//  Created by Mariya Kachalova on 30.07.2018.
//  Copyright © 2018 Yandex. All rights reserved.
//

import Foundation
@testable import YandexDisk

extension IndexSet {
    static func indexSetFromIndexes(_ indexes: [Int]) -> IndexSet {
        let mutableIndexSet = NSMutableIndexSet()
        indexes.forEach(mutableIndexSet.add(_:))
        return mutableIndexSet as IndexSet
    }
}

extension IndexPath {
    typealias SectionRow = (section: Int, row: Int)

    static func from(tuple: SectionRow) -> IndexPath {
        return IndexPath(row: tuple.row, section: tuple.section)
    }

    static func from(tuples: [SectionRow]) -> [IndexPath] {
        return tuples.map { from(tuple: $0) }
    }
}

extension Error {
    var testCode: Int { return (self as NSError).code }
    var testDomain: String { return (self as NSError).domain }
    var testUserInfo: [AnyHashable: Any] { return (self as NSError).userInfo }
}

extension Swift.Decodable {
    static func testDecode(parameters: [String: Any], userInfo: [CodingUserInfoKey: Any]? = nil) throws -> Self {
        let decoder = DecoderBuilder.jsonDecoder(strategy: .convertFromSnakeCase, userInfo: userInfo)
        return try decoder.decode(Self.self, from: try! JSONSerialization.data(withJSONObject: parameters))
    }
}

extension EndpointType {
    func testDecode(parameters: [String: Any]) throws -> E {
        let decoder = DecoderBuilder.jsonDecoder(strategy: .convertFromSnakeCase, userInfo: decoderUserInfo)
        return try decoder.decode(E.self, from: try! JSONSerialization.data(withJSONObject: parameters))
    }
}
