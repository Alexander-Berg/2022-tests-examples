//
//  BaseUnitTest.swift
//  Tests
//
//  Created by Roman Bevza on 3/26/21.
//

import PINRemoteImage
import AutoRuUtils
import AutoRuFetchableImage
import XCTest
import SwiftProtobuf
import AutoRuSynchronization

class BaseUnitTest: XCTestCase {
    private static var _onceToken: Bool = false
    private static let lock = UnfairLock()

    override class func setUp() {
        setupPinImageSharedManager()
    }

    override func setUp() {
        super.setUp()

        FetchableImage.blockThreadUntilFinished = true
    }

    override func tearDown() {
        super.tearDown()
        FetchableImage.blockThreadUntilFinished = false
    }

    private static func setupPinImageSharedManager() {
        lock.lock()
        defer {
            _onceToken = true
            lock.unlock()
        }

        if !_onceToken {
            let config = URLSessionConfiguration.default
            config.protocolClasses = [StubImageURLProtocol.self]
            config.httpAdditionalHeaders = ["Accept": "image/webp"]
            PINRemoteImageManager.setSharedImageManagerWith(config) // this method can be called once
            PINRemoteImageManager.shared().cache.removeAllObjects()
            StubImageURLProtocol.isEnabled = false
        }
    }

    // used for replacing images using a stub webp image in the Tests bundle otherwise with magenta rectangle
    func setReplaceImagesWithStub(_ fileName: String? = nil) {
        StubImageURLProtocol.stubImageFileName = fileName
        StubImageURLProtocol.isEnabled = true
    }

    // returns back default downloading strategy
    func setReplaceImagesDefaultBehavior() {
        StubImageURLProtocol.isEnabled = false
        PINRemoteImageManager.shared().cache.removeAllObjects()
    }
}

final class StubImageURLProtocol: URLProtocol {
    static var stubImageFileName: String?
    static var isEnabled = true

    override class func canInit(with request: URLRequest) -> Bool {
        return Self.isEnabled
    }

    override class func canonicalRequest(for request: URLRequest) -> URLRequest {
        return request
    }

    override func stopLoading() { }

    override func startLoading() {
        let stubImageData: Data

        if let fileName = Self.stubImageFileName,
           let url = Bundle.current.url(forResource: fileName, withExtension: "webp"),
           let data = try? Data(contentsOf: url) {
            stubImageData = data
        } else {
            stubImageData = Data(
                base64Encoded: "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8yPD/DwAGyALuh4rYlgAAAABJRU5ErkJggg=="
            )!
        }

        self.client?.urlProtocol(self, didLoad: stubImageData)
        self.client?.urlProtocolDidFinishLoading(self)
    }
}
