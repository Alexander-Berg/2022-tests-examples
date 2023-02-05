// Copyright 2022 Yandex LLC. All rights reserved.

@testable import IoT
import SnapshotTesting
import XCTest

class SplashViewTests: XCTestCase {
  func testSplashView() throws {
    SplashView_Previews.testableSamples.forEach {
      $0.test(batch: .regular)
    }
  }
}
