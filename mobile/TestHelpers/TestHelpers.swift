//
//  TestHelpers.swift
//  YandexDisk
//
//  Created by Valeriy Popov on 28/06/2017.
//  Copyright © 2017 Yandex. All rights reserved.
//

import XCTest
#if !DEV_TEST
@testable import YandexDisk
#endif

extension XCTestCase {
    func loadJson(name: String) -> [String: Any] {
        guard let jsonDictionary =
            try? JSONSerialization.jsonObject(with: loadData(name + ".json"), options: []) as? [String: Any] else {
            fatalError("Unable to convert \(name).json to JSON dictionary")
        }
        return jsonDictionary
    }

    func loadData(_ filename: String) -> Data {
        guard let path = Bundle(for: type(of: self)).url(forResource: filename, withExtension: nil) else {
            fatalError("\(filename) not found")
        }
        guard let data = try? Data(contentsOf: path) else {
            fatalError("Unable to convert \(filename) to Data")
        }
        return data
    }

    func triggerAlertAction(_ action: UIAlertAction) {
        typealias AlertHandler = @convention(block) (UIAlertAction) -> Void
        let blockPtr = UnsafeRawPointer(
            Unmanaged<AnyObject>.passUnretained(action.value(forKey: "handler") as AnyObject)
                .toOpaque()
        )
        let handler = unsafeBitCast(blockPtr, to: AlertHandler.self)
        handler(action)
    }

    func waitForCondition(timeout: TimeInterval = 3.0, block: @escaping () -> Bool) {
        let exp = expectation(for: NSPredicate(block: { _, _ in return block() }), evaluatedWith: self, handler: nil)
        XCTWaiter().wait(for: [exp], timeout: timeout)
    }

    func turnOnExperiment(_ experiment: ABSupportedExperiments) {
        turnOnExperiments([experiment])
    }

    func turnOnExperiments(_ experiments: [ABSupportedExperiments]) {
        ABDebugFlagsManager.flags = experiments.map(\.name)
    }

    func turnOnFlags(_ flags: [Any]) {
        let _flags: [String] = flags.compactMap { flag in
            switch flag {
            case let exp as ABSupportedExperiments:
                return exp.name
            default:
                return nil
            }
        }

        ABDebugFlagsManager.flags = _flags
    }

    func turnOffExperiments() {
        ABDebugFlagsManager.flags = []
    }

    var someTestTask: URLSessionTask {
        return URLSession(configuration: URLSessionConfiguration.default).dataTask(with: URL(string: "http://ya.ru")!)
    }
}
