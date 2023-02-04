// swift-tools-version:5.5
import PackageDescription

let package = Package(
    name: "mediator",
    platforms: [
       .macOS(.v10_15)
    ],
    dependencies: [
        // 💧 A server-side Swift web framework.
        .package(url: "https://github.com/vapor/vapor.git", from: "4.0.0"),
    ],
    targets: [
        .target(
            name: "App",
            dependencies: [
                "Mediation",
                .product(name: "Vapor", package: "vapor")
            ],
            swiftSettings: [
                // Enable better optimizations when building in Release configuration. Despite the use of
                // the `.unsafeFlags` construct required by SwiftPM, this flag is recommended for Release
                // builds. See <https://github.com/swift-server/guides/blob/main/docs/building.md#building-for-production> for details.
                .unsafeFlags(["-cross-module-optimization"], .when(configuration: .release))
            ]
        ),
        .target(
            name: "Mediation",
            dependencies: [
                "Teamcity"
            ]
        ),
        .target(name: "Teamcity"),
        .executableTarget(name: "Run", dependencies: [.target(name: "App")]),
        .testTarget(
            name: "AppTests",
            dependencies: [
                .target(name: "App"),
                .product(name: "XCTVapor", package: "vapor"),
            ]
        ),
        .testTarget(
            name: "MediationTests",
            dependencies: [
                .target(name: "Mediation"),
                .target(name: "Teamcity")
            ]
        )
    ]
)
