//
//  HTTPClientRequestHandleBuilderPlayer.swift
//  TestUtils
//
//  Created by Timur Turaev on 30.06.2021.
//  Copyright © 2021 Yandex. All rights reserved.
//

import Foundation
import NetworkLayer

@objc(YOHTTPClientRequestHandleBuilderPlayer)
public final class HTTPClientRequestHandleBuilderPlayer: HTTPClientRequestHandleBuilderFixture {
    private static let testErrorDomain = "ru.yandex.YandexMobileMailTests"

    private let bundle: Bundle?
    private let enableRequestValidate: Bool
    private let networkOperationDelay: Int

    @objc public init(bundle: Bundle?,
                      fixtureName: String,
                      enableRequestValidate: Bool,
                      networkOperationDelay: Int) {
        self.bundle = bundle
        self.enableRequestValidate = enableRequestValidate
        self.networkOperationDelay = networkOperationDelay

        super.init(fixtureName: fixtureName,
                   login: "login",
                   credentials: YOURLCredentials(token: "token"),
                   httpClientVaryingParameters: PlayerHTTPClientVaryingParameters())
    }

    public override func buildRequestHandle(for request: YORequest,
                                            successBlock: @escaping (HTTPURLResponse?, Data) -> Void,
                                            failureBlock: @escaping (HTTPURLResponse?, Error) -> Void) -> YORequestHandle? {
        let currentFixtureName = self.fixtureNameForCurrentRunCount
        self.runCount += 1

        if self.enableRequestValidate {
            if let jsonRequest = FixtureHelper.loadJsonFixture(name: currentFixtureName, kind: .request, bundle: self.bundle),
               !request.isEqualToJson(jsonRequest) {
                failureBlock(nil, NSError(domain: Self.testErrorDomain,
                                          code: -1,
                                          userInfo: [
                                            NSLocalizedDescriptionKey: "invalid request",
                                            "actual": request,
                                            "expected": jsonRequest
                                          ]))
                return nil
            }
        }

        let callResult: () -> Void = {
            if let response = FixtureHelper.loadFixture(name: currentFixtureName, kind: .response, bundle: self.bundle) {
                successBlock(nil, response)
            } else if let failureData = FixtureHelper.loadFixture(name: currentFixtureName, kind: .failure, bundle: self.bundle),
                      let error = try? NSKeyedUnarchiver.unarchiveTopLevelObjectWithData(failureData) as? NSError {
                failureBlock(nil, error)
            } else {
                failureBlock(nil, YOError(code: .mailServerError, message: "Fixture [\(currentFixtureName)] loading failed"))
            }
        }

        if self.networkOperationDelay == 0 {
            YOBlockPerformSyncIfOnMainThreadElseAsync(callResult)
        } else {
            DispatchQueue.main.asyncAfter(deadline: .now() + .milliseconds(self.networkOperationDelay), execute: callResult)
        }

        return FixtureRequestHandle(request: request)
    }

    public override func buildUploadRequestHandle(from request: YOUploadRequest,
                                                  successBlock: @escaping (Data) -> Void,
                                                  failureBlock: @escaping (Error) -> Void) -> YOUploadRequestHandle? {
        let currentFixtureName = self.fixtureNameForCurrentRunCount
        self.runCount += 1

        if self.enableRequestValidate {
            if let jsonRequest = FixtureHelper.loadJsonFixture(name: currentFixtureName, kind: .request, bundle: self.bundle),
               !request.isEqualToJson(jsonRequest) {
                failureBlock(NSError(domain: Self.testErrorDomain,
                                     code: -1,
                                     userInfo: [
                                        NSLocalizedDescriptionKey: "invalid request",
                                        "actual": request,
                                        "expected": jsonRequest
                                     ]))
                return nil
            }
        }

        if let response = FixtureHelper.loadFixture(name: currentFixtureName, kind: .response, bundle: self.bundle) {
            successBlock(response)
        } else {
            failureBlock(YOError(code: .mailServerError, message: "Fixture [\(currentFixtureName)] loading failed"))
        }

        return FixtureUploadRequestHandle(request: request)
    }
}

private final class PlayerHTTPClientVaryingParameters: HTTPClientVaryingParameters {
    func baseURLFor(_ login: String?) -> URL {
        return URL(string: "https://yandex.ru")!
    }

    func tabsAvailableForLogin(_ login: String?) -> Bool {
        return false
    }

    var client: String {
        return "iphone"
    }

    var appState: String? {
        return "foreground"
    }

    var uuid: String? {
        return nil
    }

    var userAgent: String {
        return "fake_user_agent"
    }
}
