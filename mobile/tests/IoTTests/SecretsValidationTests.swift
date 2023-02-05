// Copyright 2022 Yandex LLC. All rights reserved.

@testable import IoT
import XCTest

class SecretsValidationTests: XCTestCase {
  func testAMClientSecretValidation() {
    let secrets = Secrets.restored
    XCTAssertFalse(secrets.amClientSecret.isEmpty)
  }

  func testMetricaApiKeyValidation() {
    let secrets = Secrets.restored
    XCTAssertFalse(secrets.metricaApiKey.isEmpty)
  }

  func testAMClientIdValidation() {
    let secrets = Secrets.restored
    XCTAssertFalse(secrets.amClientId.isEmpty)
  }
}
