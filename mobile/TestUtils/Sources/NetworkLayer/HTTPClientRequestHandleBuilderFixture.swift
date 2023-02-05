//
//  YOHTTPClientRequestHandleBuilderFixture.swift
//  TestUtils
//
//  Created by Timur Turaev on 30.06.2021.
//  Copyright © 2021 Yandex. All rights reserved.
//

import Foundation
import NetworkLayer

@objc(YOHTTPClientRequestHandleBuilderFixture)
public class HTTPClientRequestHandleBuilderFixture: HTTPClientAuthorizedRequestHandleBuilder {
    @objc public let fixtureName: String
    @objc public var runCount: Int

    @objc public init(fixtureName: String,
                      login: String,
                      credentials: YOURLCredentials,
                      httpClientVaryingParameters: HTTPClientVaryingParameters) {
        self.fixtureName = fixtureName
        self.runCount = 0

        super.init(login: login,
                   uuid: "deadbeef",
                   credentials: credentials,
                   httpClientVaryingParameters: httpClientVaryingParameters,
                   sharedContainerIdentifier: nil,
                   taskDidFinishCollectionMetricsBlock: nil,
                   authChallengeDisposition: AuthChallengeDisposition.allowEverything.authChallengeDispositionCompat)
    }

    @objc public var fixtureNameForCurrentRunCount: String {
        guard self.fixtureName != FixtureHelper.invalidFixtureName else { return self.fixtureName }

        return self.runCount == 0 ? self.fixtureName : "\(self.fixtureName)_\(self.runCount)"
    }
}
