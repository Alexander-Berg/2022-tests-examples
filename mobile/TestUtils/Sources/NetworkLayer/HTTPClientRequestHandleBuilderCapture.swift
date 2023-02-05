//
//  HTTPClientRequestHandleBuilderCapture.swift
//  TestUtils
//
//  Created by Timur Turaev on 30.06.2021.
//  Copyright © 2021 Yandex. All rights reserved.
//

import Foundation
import NetworkLayer

@objc(YOHTTPClientRequestHandleBuilderCapture)
public final class HTTPClientRequestHandleBuilderCapture: HTTPClientRequestHandleBuilderFixture {
    public override func buildRequestHandle(for request: YORequest,
                                            successBlock: @escaping (HTTPURLResponse?, Data) -> Void,
                                            failureBlock: @escaping (HTTPURLResponse?, Error) -> Void) -> YORequestHandle? {
        let successBlockWithCapturing: (HTTPURLResponse?, Data) -> Void = { urlResponse, responseData in
            let currentFixtureName = self.fixtureNameForCurrentRunCount
            self.runCount += 1

            let requestData = try! JSONSerialization.data(withJSONObject: request.json, options: .prettyPrinted)

            FixtureHelper.saveFixture(name: currentFixtureName, data: requestData, kind: .request, bundle: nil)
            FixtureHelper.saveFixture(name: currentFixtureName, data: responseData, kind: .response, bundle: nil)

            successBlock(urlResponse, responseData)
        }

        let failureBlockWithCapturing: (HTTPURLResponse?, Error) -> Void = { urlResponse, responseError in
            let currentFixtureName = self.fixtureNameForCurrentRunCount
            self.runCount += 1

            let requestData = try! JSONSerialization.data(withJSONObject: request.json, options: .prettyPrinted)
            let responseData = try! NSKeyedArchiver.archivedData(withRootObject: responseError, requiringSecureCoding: false)

            FixtureHelper.saveFixture(name: currentFixtureName, data: requestData, kind: .request, bundle: nil)
            FixtureHelper.saveFixture(name: currentFixtureName, data: responseData, kind: .failure, bundle: nil)

            failureBlock(urlResponse, responseError)
        }

        return super.buildRequestHandle(for: request,
                                        successBlock: successBlockWithCapturing,
                                        failureBlock: failureBlockWithCapturing)
    }

    public override func buildUploadRequestHandle(from request: YOUploadRequest,
                                                  successBlock: @escaping (Data) -> Void,
                                                  failureBlock: @escaping (Error) -> Void) -> YOUploadRequestHandle? {
        let successBlockWithCapturing: (Data) -> Void = { responseData in
            let currentFixtureName = self.fixtureNameForCurrentRunCount
            self.runCount += 1

            let requestData = try! JSONSerialization.data(withJSONObject: request.json, options: .prettyPrinted)

            FixtureHelper.saveFixture(name: currentFixtureName, data: requestData, kind: .request, bundle: nil)
            FixtureHelper.saveFixture(name: currentFixtureName, data: responseData, kind: .response, bundle: nil)

            successBlock(responseData)
        }

        return super.buildUploadRequestHandle(from: request,
                                              successBlock: successBlockWithCapturing,
                                              failureBlock: failureBlock)
    }
}
