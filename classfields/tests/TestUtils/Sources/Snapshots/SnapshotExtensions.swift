import SnapshotTesting
import UIKit
import XCTest

extension Snapshot {
    public static func compareWithSnapshot(
        _ viewController: UIViewController,
        interfaceStyles: [UIUserInterfaceStyle] = [.light, .dark],
        config: ViewImageConfig? = nil,
        size: CGSize? = nil,
        identifier: String? = nil,
        function: String = #function,
        file: String = #filePath,
        options: SnapshotIdentifier.Option = [.scale, .screen, .interfaceStyle]
    ) {
        for style in interfaceStyles {
            let traitCollection = UITraitCollection(userInterfaceStyle: style)

            let tookSnapshot = XCTestExpectation(description: "Took snapshot")
            var image: UIImage?

            if let config = config {
                Snapshotting.image(on: config, size: size, traits: traitCollection).snapshot(viewController).run { snapshot in
                    image = snapshot
                    tookSnapshot.fulfill()
                }
            } else {
                Snapshotting.image(size: size, traits: traitCollection).snapshot(viewController).run { snapshot in
                    image = snapshot
                    tookSnapshot.fulfill()
                }
            }

            _ = XCTWaiter.wait(for: [tookSnapshot], timeout: 5)

            guard let image = image else {
                XCTFail("Unable to take snapshot")
                return
            }

            Self.compareWithSnapshot(
                image: image,
                identifier: SnapshotIdentifier(
                    suite: SnapshotIdentifier.suiteName(from: file),
                    identifier: identifier ?? String(function.dropLast(2)),
                    options: options,
                    style: style
                )
            )
        }
    }
}
