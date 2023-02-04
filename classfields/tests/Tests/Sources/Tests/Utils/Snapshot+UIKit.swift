import UIKit
import SwiftUI
import XCTest
import AutoRuYogaLayout
import AutoRuLayout
import AutoRuSharedUI
import AutoRuAppearance
import AutoRuTableController
import AutoRuNavigationContainer
import SnapshotTesting
import Snapshots
import Foundation
import CoreGraphics

enum DeviceWidth {
    static let iPhone11: CGFloat = 414
}

enum DeviceHeight {
    static let iPhone11: CGFloat = 896
}

protocol ViewControllerScrollContaining: UIViewController {
    var scrollView: UIScrollView { get }
}

extension Snapshot {
    static let transparencyReplacementColor = UIColor(red: 234 / 255, green: 0 / 255, blue: 1.0, alpha: 1.0)

    static let defaultInterfaceStyleSetup: [InterfaceStyle] = [.light, .dark]

    static func compareWithSnapshot(
        view: UIView,
        interfaceStyle: [InterfaceStyle] = Self.defaultInterfaceStyleSetup,
        identifier: String = #function,
        perPixelTolerance: Double = Self.defaultPerPixelTolerance,
        overallTolerance: Double = Self.defaultOverallTolerance,
        file: StaticString = #file
    ) {
        interfaceStyle.forEach {
            self.compareWithSnapshot(
                view: view,
                interfaceStyle: $0,
                identifier: identifier,
                perPixelTolerance: perPixelTolerance,
                overallTolerance: overallTolerance,
                file: file
            )
        }
    }

    static func compareWithSnapshot(
        layoutSpec: LayoutSpec,
        maxWidth: CGFloat = .nan,
        maxHeight: CGFloat = .nan,
        backgroundColor: UIColor = Self.transparencyReplacementColor,
        interfaceStyle: [InterfaceStyle] = Self.defaultInterfaceStyleSetup,
        identifier: String = #function,
        perPixelTolerance: Double = Self.defaultPerPixelTolerance,
        overallTolerance: Double = Self.defaultOverallTolerance,
        file: StaticString = #file
    ) {
        let view = UIView()
        view.backgroundColor = backgroundColor

        let layout = layoutSpec.makeLayoutWith(sizeConstraints: .init(width: maxWidth, height: maxHeight))
        layout.setup(in: view)

        self.compareWithSnapshot(
            view: view,
            interfaceStyle: interfaceStyle,
            identifier: identifier,
            perPixelTolerance: perPixelTolerance,
            overallTolerance: overallTolerance,
            file: file
        )
    }

    static func compareWithSnapshot(
        layout: LayoutConvertible,
        maxWidth: CGFloat = .nan,
        maxHeight: CGFloat = .nan,
        backgroundColor: UIColor = Self.transparencyReplacementColor,
        interfaceStyle: [InterfaceStyle] = Self.defaultInterfaceStyleSetup,
        identifier: String = #function,
        perPixelTolerance: Double = Self.defaultPerPixelTolerance,
        overallTolerance: Double = Self.defaultOverallTolerance,
        file: StaticString = #file
    ) {
        let creator = BasicViewHierarchyCreator(
            rootComponent: layout.getLayout(),
            boundingSize: CGSize(width: maxWidth, height: maxHeight)
        )

        let view = creator.createView()
        view.backgroundColor = backgroundColor

        self.compareWithSnapshot(
            view: view,
            interfaceStyle: interfaceStyle,
            identifier: identifier,
            perPixelTolerance: perPixelTolerance,
            overallTolerance: overallTolerance,
            file: file
        )
    }

    static func compareWithSnapshot(
        cellHelper: CellHelper,
        maxWidth: CGFloat = .nan,
        maxHeight: CGFloat = .nan,
        backgroundColor: UIColor = Self.transparencyReplacementColor,
        interfaceStyle: [InterfaceStyle] = Self.defaultInterfaceStyleSetup,
        identifier: String = #function,
        file: StaticString = #file
    ) {
        let view = cellHelper.createCellView(width: maxWidth)
        view.backgroundColor = backgroundColor

        self.compareWithSnapshot(
            view: view,
            interfaceStyle: interfaceStyle,
            identifier: identifier,
            file: file
        )
    }

    static func compareWithSnapshot(
        viewController: UIViewController,
        size: CGSize = UIScreen.main.bounds.size,
        interfaceStyle: [InterfaceStyle] = Self.defaultInterfaceStyleSetup,
        identifier: String = #function,
        perPixelTolerance: Double = Self.defaultPerPixelTolerance,
        overallTolerance: Double = Self.defaultOverallTolerance,
        file: StaticString = #file
    ) {
        viewController.view.bounds = CGRect(origin: .zero, size: size)
        viewController.viewWillAppear(false)
        viewController.viewDidAppear(false)

        func compare(style: [InterfaceStyle]) {
            self.compareWithSnapshot(
                view: viewController.view,
                interfaceStyle: style,
                identifier: identifier,
                perPixelTolerance: perPixelTolerance,
                overallTolerance: overallTolerance,
                file: file
            )
        }

        for style in interfaceStyle {
            viewController.overrideUserInterfaceStyle = style.userInterfaceStyle
            compare(style: [style])
        }
    }

    static func compareWithSnapshot(
        scrollContaining viewController: ViewControllerScrollContaining,
        boundingSize: CGSize = UIScreen.main.bounds.size,
        interfaceStyle: [InterfaceStyle] = Self.defaultInterfaceStyleSetup,
        identifier: String = #function,
        file: StaticString = #file
    ) {
        viewController.view.bounds = CGRect(origin: .zero, size: boundingSize)
        viewController.viewWillAppear(false)
        viewController.viewDidAppear(false)

        viewController.view.setNeedsLayout()
        viewController.view.layoutIfNeeded()

        viewController.view.bounds = CGRect(origin: .zero, size: viewController.scrollView.contentSize)

        for style in interfaceStyle {
            viewController.overrideUserInterfaceStyle = style.userInterfaceStyle

            self.compareWithSnapshot(
                view: viewController.view,
                interfaceStyle: [style],
                identifier: identifier,
                file: file
            )
        }
    }

    // MARK: - Private

    private static func compareWithSnapshot(
        view: UIView,
        interfaceStyle: InterfaceStyle,
        identifier: String = #function,
        perPixelTolerance: Double = Self.defaultPerPixelTolerance,
        overallTolerance: Double = Self.defaultOverallTolerance,
        file: StaticString = #file
    ) {
        view.overrideUserInterfaceStyle = interfaceStyle.userInterfaceStyle

        view.sizeToFit()

        view.setNeedsLayout()
        view.layoutIfNeeded()

        let bounds = view.bounds

        XCTAssertFalse(bounds.width.isZero, "Нулевая ширина")
        XCTAssertFalse(bounds.height.isZero, "Нулевая высота")

        let tookSnapshot = XCTestExpectation(description: "Took snapshot")

        var image: UIImage?
        Snapshotting.image.snapshot(view).run { snapshot in
            image = snapshot
            tookSnapshot.fulfill()
        }

        _ = XCTWaiter.wait(for: [tookSnapshot], timeout: 5)

        guard let image = image else {
            XCTFail("Unable to take snapshot")
            return
        }

        let snapshot = image

        XCTContext.runActivity(
            named: "Проверка скриншота \(interfaceStyle == .light ? "со светлым" : "с темным") интерфейсом"
        ) { _ in
            Snapshot.compareWithSnapshot(
                image: snapshot,
                identifier: identifier,
                style: interfaceStyle.userInterfaceStyle,
                perPixelTolerance: perPixelTolerance,
                overallTolerance: overallTolerance,
                file: file
            )
        }
    }
}

extension Snapshot {
    static func compareWithSnapshot<T: View>(
        view: T,
        maxWidth: CGFloat? = nil,
        maxHeight: CGFloat? = nil,
        backgroundColor: UIColor = Self.transparencyReplacementColor,
        interfaceStyle: [InterfaceStyle] = Self.defaultInterfaceStyleSetup,
        identifier: String = #function,
        perPixelTolerance: Double = Self.defaultPerPixelTolerance,
        overallTolerance: Double = Self.defaultOverallTolerance,
        file: StaticString = #file
    ) {
        let controller = UIHostingController(
            rootView: TestWrapperView(width: maxWidth, height: maxHeight) { view }
        )
        controller.view.backgroundColor = backgroundColor

        Snapshot.compareWithSnapshot(
            view: controller.view,
            interfaceStyle: interfaceStyle,
            identifier: identifier,
            perPixelTolerance: perPixelTolerance,
            overallTolerance: overallTolerance,
            file: file
        )
    }

    private struct TestWrapperView<Content: View>: View {
        let width: CGFloat?
        let height: CGFloat?
        let content: () -> Content

        init(width: CGFloat?, height: CGFloat?, @ViewBuilder content: @escaping () -> Content) {
            self.width = width
            self.height = height
            self.content = content
        }

        var body: some View {
            HStack {
                content()
            }
            .frame(width: width, height: height)
        }
    }
}

extension UIUserInterfaceStyle {
    init(interfaceStyle: InterfaceStyle) {
        switch interfaceStyle {
        case .light:
            self = .light

        case .dark:
            self = .dark
        }
    }
}
