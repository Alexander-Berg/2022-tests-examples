// Copyright 2022 Yandex LLC. All rights reserved.

@testable import IoT
import SnapshotTesting
import XCTest

class WebViewTests: XCTestCase {
  func testWebView() throws {
    WebView_Previews.testableSamples.forEach {
      $0.test(batch: .regular)
    }
  }
}
