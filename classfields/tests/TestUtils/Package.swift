// swift-tools-version:5.4

import PackageDescription

let package = Package(
    name: "TestUtils",
    platforms: [.iOS(.v14)],
    products: [
        .library(name: "Snapshots", targets: ["Snapshots"])
    ],
    dependencies: [
        .package(name: "SnapshotTesting", url: "https://github.com/pointfreeco/swift-snapshot-testing.git", from: "1.9.0")
    ],
    targets: [
        .target(
            name: "Snapshots",
            dependencies: ["SnapshotTesting"],
            linkerSettings: [.linkedFramework("XCTest")]
        )
    ]
)
