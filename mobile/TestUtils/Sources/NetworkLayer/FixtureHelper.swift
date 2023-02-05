//
//  FixtureHelper.swift
//  TestUtils
//
//  Created by Timur Turaev on 30.06.2021.
//  Copyright © 2021 Yandex. All rights reserved.
//

import Foundation
import NetworkLayer

@objc(YOFixtureHelper)
public final class FixtureHelper: NSObject {
    @objc public static let invalidFixtureName = "invalidFixture"

    enum FixtureKind {
        case request
        case response
        case failure

        var fileName: String {
            switch self {
            case .request:
                return "request.json"
            case .response:
                return "response.json"
            case .failure:
                return "failure.json"
            }
        }
    }

    internal static func saveFixture(name: String, data: Data, kind: FixtureKind, bundle: Bundle?) {
        let path = self.pathFor(fixtureName: name, bundle: bundle, filename: kind.fileName)
        let url = URL(fileURLWithPath: path)
        print("✏️ Saving [\(kind)] fixture: \(path)")

        try? data.write(to: url, options: .atomic)
    }

    internal static func loadFixture(name: String, kind: FixtureKind, bundle: Bundle?) -> Data? {
        guard name != Self.invalidFixtureName else { return nil }

        let path = self.pathFor(fixtureName: name, bundle: bundle, filename: kind.fileName)
        let url = URL(fileURLWithPath: path)
        print("🔍 Loading [\(kind)] fixture: \(path)")

        return try? Data(contentsOf: url)
    }

    internal static func loadJsonFixture(name: String, kind: FixtureKind, bundle: Bundle?) -> [AnyHashable: Any]? {
        guard let data = self.loadFixture(name: name, kind: kind, bundle: bundle) else { return nil }

        return (try? JSONSerialization.jsonObject(with: data, options: [.fragmentsAllowed])) as? [AnyHashable: Any]
    }

    private static func pathFor(fixtureName: String, bundle: Bundle?, filename: String) -> String {
        let fileName = (fixtureName as NSString).appendingPathExtension(filename)!
        return (self.pathToFixtureDirectory(in: bundle) as NSString).appendingPathComponent(fileName)
    }

    private static func pathToFixtureDirectory(in bundle: Bundle?) -> String {
        if let bundle = bundle {
            return bundle.resourcePath!
        } else {
            return NSSearchPathForDirectoriesInDomains(.documentDirectory, .userDomainMask, true).last!
        }
    }
}

@objc(YOFixtureRequestHandle)
public final class FixtureRequestHandle: NSObject, YORequestHandle {
    public let request: YORequest

    @objc init(request: YORequest) {
        self.request = request
    }

    public func suspend() {
        // do nothing
    }

    public func resume() {
        // do nothing
    }

    public func cancel() {
        // do nothing
    }
}

@objc(YOFixtureUploadRequestHandle)
public final class FixtureUploadRequestHandle: NSObject, YOUploadRequestHandle {
    public let request: YOUploadRequest

    init(request: YOUploadRequest) {
        self.request = request
    }

    public func suspend() {
        // do nothing
    }

    public func resume() {
        // do nothing
    }

    public func cancel() {
        // do nothing
    }
}
