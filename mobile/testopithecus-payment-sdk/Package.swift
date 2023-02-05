// swift-tools-version:5.0
// The swift-tools-version declares the minimum version of Swift required to build this package.

import PackageDescription

let package = Package(
  name: "testopithecus-payment-sdk",
  platforms: [.iOS(.v11), .macOS(.v10_12)],
  dependencies: [
    .package(url: "https://github.com/mxcl/PromiseKit", from: "6.13.2"),
    .package(url: "https://github.com/PromiseKit/Foundation.git", from: "3.3.2"),
  ],
  targets: [
    .target(
      name: "testopithecus-payment-sdk",
      dependencies: ["PromiseKit", "PMKFoundation"],
      path: "Sources",
      sources: ["common/generated", "common/generated-native-support", "eventus-common/generated", "xflags/generated", "testopithecus-common/generated", "testopithecus-payment-sdk/generated", "payment-sdk/generated"]
    )
  ],
  swiftLanguageVersions: [.v5]
)
