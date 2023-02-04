// swift-tools-version:5.4

import PackageDescription
import Foundation

let packageURL = URL(fileURLWithPath: #filePath).deletingLastPathComponent()

func resources(for path: String) -> [String] {
    let resourceExtensions: Set<String> = ["xml", "json", "webp", "bundle", "mov", "pdf"]
    let testsFolder = packageURL.appendingPathComponent(path)
    guard let enumerator = FileManager.default.enumerator(atPath: testsFolder.path) else {
        return []
    }

    var results: [String] = []
    while let element = enumerator.nextObject() as? String {
        let url = URL(fileURLWithPath: element)

        guard resourceExtensions.contains(url.pathExtension) else { continue }

        results.append(element)
    }

    return results
}

let testsResources = resources(for: "Sources/Tests/")

let package = Package(
    name: "Tests",
    platforms: [.iOS(.v14)],
    products: [
    ],
    dependencies: [
        .package(path: "../../packages"),
        .package(path: "../../Resources"),
        .package(path: "../TestUtils"),
        .package(name: "SnapshotTesting", url: "https://github.com/pointfreeco/swift-snapshot-testing.git", from: "1.9.0"),
    ],
    targets: [
        .testTarget(
            name: "Tests",
            dependencies: [
                .product(name: "Snapshots", package: "TestUtils"),
                .product(name: "ApplicationDependencies", package: "packages"),
                .product(name: "AutoRuAssets", package: "Resources"),
                "SnapshotTesting"
            ],
            exclude: [
                "CarReport/CarReport.xctestplan",
                "Info.plist"
            ],
            resources: testsResources.map {
                .copy($0)
            }
        )
    ]
)
