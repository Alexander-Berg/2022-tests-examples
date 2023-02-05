// swift-tools-version:5.0
// The swift-tools-version declares the minimum version of Swift required to build this package.

import PackageDescription

let package = Package(
  name: "testopithecus-common",
  platforms: [.iOS(.v11), .macOS(.v10_12)],
  dependencies: [
    .package(url: "https://github.com/mxcl/PromiseKit", from: "6.13.2"),
    .package(url: "https://github.com/PromiseKit/Foundation.git", from: "3.3.2"),
  ],
  targets: [
    .target(
      name: "testopithecus-common",
      dependencies: ["PromiseKit", "PMKFoundation"],
      path: "Sources",
      sources: ["common/generated", "common/generated-native-support", "common/native/Promises", "eventus-common/generated", "eventus/generated", "testopithecus-common/generated"]
    )
  ],
  swiftLanguageVersions: [.v5]
)
