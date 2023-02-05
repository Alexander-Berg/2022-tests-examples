// Copyright 2022 Yandex LLC. All rights reserved.

import SnapshotTesting
import SwiftUI

public enum SnapshotBatch {
  case regular, full, small, fast
}

extension View {
  func test(
    batch: SnapshotBatch,
    testName: StaticString = #file,
    caseName: StaticString = #function,
    line: UInt = #line
  ) {
    let hostingController = UIHostingController(rootView: self)
    let testCase = caseName.description

    let testSnapshot = { config, mode in
      assertSnapshot(
        matching: hostingController,
        as: .image(on: config, traits: mode),
        file: testName,
        testName: testCase,
        line: line
      )
    }

    switch batch {
    case .regular:
      testSnapshot(ViewImageConfig.iPhone8, lightMode)
      testSnapshot(ViewImageConfig.iPhone8, darkMode)
    case .fast:
      testSnapshot(ViewImageConfig.iPhoneXr, lightMode)
    case .full:
      testSnapshot(ViewImageConfig.iPhoneXr, lightMode)
      testSnapshot(ViewImageConfig.iPhoneXr, darkMode)
      testSnapshot(ViewImageConfig.iPhoneSe, lightMode)
      testSnapshot(ViewImageConfig.iPhoneSe, darkMode)
      assertSnapshot(
        matching: hostingController,
        as: .image(
          drawHierarchyInKeyWindow: true,
          precision: 0.99,
          size: ViewImageConfig.iPhone8.size,
          traits: .current
        ),
        file: testName,
        testName: testCase,
        line: line
      )
    case .small:
      testSnapshot(ViewImageConfig.iPhoneSe, lightMode)
      testSnapshot(ViewImageConfig.iPhoneSe, darkMode)
    }
  }
}

private let lightMode = UITraitCollection(userInterfaceStyle: .light)
private let darkMode = UITraitCollection(userInterfaceStyle: .dark)
