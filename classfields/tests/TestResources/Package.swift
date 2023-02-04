// swift-tools-version:5.4

import PackageDescription

let package = Package(
    name: "TestResources",
    platforms: [.iOS(.v14)],
    products: [
        .library(name: "TestResources", targets: ["TestResources"])
    ],
    dependencies: [
    ],
    targets: [
        .target(
            name: "TestResources",
            resources: [
                .process("Resources")
            ]
        )
    ]
)
