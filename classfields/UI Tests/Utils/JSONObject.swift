//
//  JSONObject.swift
//  UI Tests
//
//  Created by Dmitry Barillo on 06.07.2022.
//  Copyright © 2022 Yandex. All rights reserved.
//

import Foundation

@dynamicMemberLookup
enum JSONObject: Codable, Hashable {
    case null
    case bool(Bool)
    case double(Double)
    case string(String)
    case array([JSONObject])
    case object([String: JSONObject])

    init?(data: Data) {
        guard let json = try? JSONSerialization.jsonObject(with: data, options: []) else { assertionFailure(); return nil }
        guard let jsonObject = Self.toJSONObject(json) else { assertionFailure(); return nil }
        self = jsonObject
    }

    init(dictionary: [String: Any]) {
        self = .object(dictionary.reduce(into: [String: JSONObject](), { $0[$1.0] = Self.toJSONObject($1.1) }))
    }

    init(from decoder: Decoder) throws {
        let container = try decoder.singleValueContainer()

        if let double = try? container.decode(Double.self) {
            self = .double(double)
        }
        else if let string = try? container.decode(String.self) {
            self = .string(string)
        }
        else if let bool = try? container.decode(Bool.self) {
            self = .bool(bool)
        }
        else if let array = try? container.decode([JSONObject].self) {
            self = .array(array)
        }
        else if let dict = try? container.decode([String: JSONObject].self) {
            self = .object(dict)
        }
        else if container.decodeNil() {
            self = .null
        }
        else {
            assertionFailure("Unknown type")
            self = .null
        }
    }

    func encode(to encoder: Encoder) throws {
        var container = encoder.singleValueContainer()

        switch self {
            case .double(let double):
                try container.encode(double)
            case .string(let string):
                try container.encode(string)
            case .bool(let bool):
                try container.encode(bool)
            case .array(let array):
                try container.encode(array)
            case .object(let dict):
                try container.encode(dict)
            case .null:
                try container.encodeNil()
        }
    }

    subscript(dynamicMember input: String) -> JSONObject? {
        get {
            return self[input]
        }
        set {
            self[input] = newValue
        }
    }

    subscript(key: String) -> JSONObject? {
        get {
            guard case .object(let dict) = self else { return nil }
            return dict[key]
        }
        set {
            guard case .object(var dict) = self else { return }
            dict[key] = newValue
            self = .object(dict)
        }
    }

    subscript(index: Int) -> JSONObject? {
        get {
            guard case .array(let array) = self else { return nil }
            guard array.indices.contains(index) else {
                assertionFailure("\(Self.self): Index out of bounds")
                return nil
            }
            return array[index]
        }
        set {
            guard case .array(var array) = self else { return }
            guard array.indices.contains(index) else {
                assertionFailure("\(Self.self): Index out of bounds")
                return
            }
            guard let newValue = newValue else {
                assertionFailure("\(Self.self): 'nil' cannot be assigned to array value")
                return
            }

            array[index] = newValue
            self = .array(array)
        }
    }

    func contains(_ jsonPart: JSONObject) -> Bool {
        Self.contains(jsonPart: jsonPart, in: self)
    }

    // MARK: - Private

    private static func toJSONObject(_ object: Any) -> JSONObject? {
        switch object {
            case is NSNull:
                return .null

            case is Bool:
                guard let boolValue = object as? Bool else { assertionFailure(); return nil }
                return .bool(boolValue)

            case is Double:
                guard let doubleValue = object as? Double else { assertionFailure(); return nil }
                return .double(doubleValue)

            case is String:
                guard let stringValue = object as? String else { assertionFailure(); return nil }
                return .string(stringValue)

            case is [Any]:
                guard let arrayObject = object as? [Any] else { assertionFailure(); return nil }
                return .array(
                    arrayObject.compactMap { self.toJSONObject($0) }
                )

            case is [String: Any]:
                guard let dictinaryObject = object as? [String: Any] else { assertionFailure(); return nil }
                return .object(
                    dictinaryObject.reduce(into: [String: JSONObject](), { $0[$1.key] = self.toJSONObject($1.value) })
                )

            default:
                assertionFailure("Unsupported object")
                return nil
        }
    }

    private static func contains(jsonPart: JSONObject, in json: JSONObject) -> Bool {
        switch (json, jsonPart) {
            case (.double, .double),
                (.string, .string),
                (.bool, .bool),
                (.null, .null):
                return json == jsonPart

            case let (.array(arrayObject), .array(arrayObjectPart)):
                return arrayObject.contains(arrayObjectPart)

            case let (.object(dictObject), .object(dictObjectPart)):
                return dictObject.contains(dictObjectPart)

            default:
                return false
        }
    }
}

extension JSONObject {
    var asNumber: Double? {
        guard case .double(let double) = self else { return nil }
        return double
    }

    var asString: String? {
        guard case .string(let string) = self else { return nil }
        return string
    }

    var asBool: Bool? {
        guard case .bool(let bool) = self else { return nil }
        return bool
    }

    var isNull: Bool {
        guard case .null = self else { return false }
        return true
    }
}

extension JSONObject: ExpressibleByIntegerLiteral {
    init(integerLiteral value: Int) {
        self = .double(Double(value))
    }
}

extension JSONObject: ExpressibleByFloatLiteral {
    init(floatLiteral value: Double) {
        self = .double(value)
    }
}

extension JSONObject: ExpressibleByStringLiteral {
    init(stringLiteral value: String) {
        self = .string(value)
    }
}

extension JSONObject: ExpressibleByBooleanLiteral {
    init(booleanLiteral value: Bool) {
        self = .bool(value)
    }
}

extension JSONObject: ExpressibleByArrayLiteral {
    init(arrayLiteral elements: JSONObject...) {
        self = .array(elements)
    }
}

extension JSONObject: ExpressibleByDictionaryLiteral {
    init(dictionaryLiteral elements: (String, JSONObject)...) {
        self = .object(elements.reduce(into: [String: JSONObject](), { $0[$1.0] = $1.1 }))
    }
}

extension JSONObject: ExpressibleByNilLiteral {
    init(nilLiteral: ()) {
        self = .null
    }
}

extension JSONObject: CustomStringConvertible, CustomDebugStringConvertible {
    var debugDescription: String {
        return self.prettyPrinted(level: 0)
    }

    var description: String {
        return self.debugDescription
    }

    private func prettyPrinted(level: Int) -> String {
        let tabs = String(repeating: "\t", count: level)
        let tabsPlusOne = String(repeating: "\t", count: level + 1)

        switch self {
            case .double(let double):
                return "\(double)"

            case .string(let string):
                return "\"\(string)\""

            case .bool(let bool):
                return "\(bool)"

            case .array(let array):
                let list = array.reduce(into: "", {
                    $0.append("\(tabsPlusOne)\($1.prettyPrinted(level: level + 1)),\n")
                })
                return "[\n\(list)\(tabs)]"

            case .object(let dict):
                let list = dict.reduce(into: "", {
                    $0.append("\(tabsPlusOne)\"\($1.key)\": \($1.value.prettyPrinted(level: level + 1)),\n")
                })
                return "{\n\(list)\(tabs)}"

            case .null:
                return "null"
        }
    }
}

extension Array where Element == JSONObject {
    fileprivate func contains(_ elements: [Array.Element]) -> Bool {
        var mutableArray = self
        for element in elements {
            guard let actualElement = mutableArray.first(where: { $0.contains(element) }) else { return false }
            guard let index = mutableArray.firstIndex(of: actualElement) else { return false }
            mutableArray.remove(at: index)
        }
        return true
    }
}

extension Dictionary where Key == String, Value == JSONObject {
    fileprivate func contains(_ keysAndValues: [Key: Value]) -> Bool {
        let actualKeys = Set(self.keys)
        let expectedKeys = Set(keysAndValues.keys)

        guard expectedKeys.isSubset(of: actualKeys) else { return false }

        for key in expectedKeys {
            guard let actualElement = self[key] else { return false }
            guard let expectedElement = keysAndValues[key] else { return false }
            guard actualElement.contains(expectedElement) else { return false }
        }
        return true
    }
}
