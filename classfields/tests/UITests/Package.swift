// swift-tools-version:5.5

import PackageDescription

let package = Package(
    name: "UITests",
    platforms: [.iOS(.v14)],
    products: [
    ],
    dependencies: [
        .package(path: "../TestUtils"),
        .package(path: "../../AutoRuProtoModels"),
        .package(path: "../TestResources"),
        .package(path: "../../AppServer")
    ],
    targets: [
        .testTarget(
            name: "UITests",
            dependencies: [
                "AutoRuProtoModels",
                .product(name: "Snapshots", package: "TestUtils"),
                "TestResources",
                "AppServer"
            ],
            exclude: [
                "Info.plist"
            ]
        )
    ]
)
