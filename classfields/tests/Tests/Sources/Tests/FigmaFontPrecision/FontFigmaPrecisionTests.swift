import UIKit
import XCTest
import SwiftUI
import AutoRuModernLayout
import AutoRuUtils
import AutoRuFontSchema
import AutoRuSwiftUICore
import Snapshots
import AutoRuYogaLayout

private struct TestCase {
    let id: String
    let font: FontConfiguration
}

final class FontFigmaPrecisionTests: BaseUnitTest {
    private static let cases: [TestCase] = [
        .init(id: "H1B", font: .h1.bold),
        .init(id: "H1R", font: .h1),

        .init(id: "H2B", font: .h2.bold),
        .init(id: "H2R", font: .h2),

        .init(id: "H3B", font: .h3.bold),
        .init(id: "H3R", font: .h3),

        .init(id: "H4B", font: .h4.bold),
        .init(id: "H4R", font: .h4),

        .init(id: "H5B", font: .h5.bold),
        .init(id: "H5M", font: .h5.medium),
        .init(id: "H5R", font: .h5),

        .init(id: "SubM", font: .sub.medium),
        .init(id: "SubR", font: .sub),

        .init(id: "BodyB", font: .body.bold),
        .init(id: "BodyM", font: .body.medium),
        .init(id: "BodyR", font: .body),

        .init(id: "Body2B", font: .body2.bold),
        .init(id: "Body2M", font: .body2.medium),
        .init(id: "Body2R", font: .body2),

        .init(id: "CaptionB", font: .caption.bold),
        .init(id: "CaptionM", font: .caption.medium),
        .init(id: "CaptionR", font: .caption)
    ]

    private static let singleLineText = "Lorem ipsum"
    private static let multilineText = "Lorem ipsum dolor sit amet,\nconsectetur adipiscing elit,\nsed do eiusmod"

    func test_uiKitFont() {
        for tc in Self.cases {
            let slName = "\(tc.id)_SL"
            let mlName = "\(tc.id)_ML"

            let slLayout = TestLayout(image: pdf(with: slName), text: Self.singleLineText, font: tc.font)
            let mlLayout = TestLayout(image: pdf(with: mlName), text: Self.multilineText, font: tc.font)

            Snapshot.compareWithSnapshot(
                layout: slLayout,
                backgroundColor: .white,
                interfaceStyle: [.light],
                identifier: "uikit_\(slName)"
            )

            Snapshot.compareWithSnapshot(
                layout: mlLayout,
                backgroundColor: .white,
                interfaceStyle: [.light],
                identifier: "uikit_\(mlName)"
            )
        }
    }

    func test_swiftUIFont() {
        for tc in Self.cases {
            let slName = "\(tc.id)_SL"
            let mlName = "\(tc.id)_ML"

            let sizeLog = ">> [\(tc.id)] real lineHeight = \(tc.font._uiFont.lineHeight); " +
                "desired lineHeight = \(tc.font.lineHeight); " +
                "pointSize = \(tc.font._uiFont.pointSize); " +
                "ascender = \(tc.font._uiFont.ascender); " +
                "descender = \(tc.font._uiFont.descender); " +
                "leading = \(tc.font._uiFont.leading); " +
                "capHeight = \(tc.font._uiFont.capHeight); " +
                "multiple = \(tc.font.lineHeight / tc.font._uiFont.lineHeight)"

            print(sizeLog)

            let slView = TestView(
                image: pdf(with: slName),
                text: Self.singleLineText,
                font: tc.font
            )

            let mlView = TestView(
                image: pdf(with: mlName),
                text: Self.multilineText,
                font: tc.font
            )

            Snapshot.compareWithSnapshot(
                view: slView,
                interfaceStyle: [.light],
                identifier: "swiftui_\(slName)"
            )

            Snapshot.compareWithSnapshot(
                view: mlView,
                interfaceStyle: [.light],
                identifier: "swiftui_\(mlName)"
            )
        }
    }

    private func pdf(with name: String) -> UIImage {
        pdf(from: Bundle.current.url(forResource: name, withExtension: "pdf")!) ?? .init()
    }

    private func pdf(from url: URL) -> UIImage? {
        guard let document = CGPDFDocument(url as CFURL) else { return nil }
        guard let page = document.page(at: 1) else { return nil }

        let pageRect = page.getBoxRect(.mediaBox)
        let renderer = UIGraphicsImageRenderer(size: pageRect.size)
        let img = renderer.image { ctx in
            UIColor.white.set()
            ctx.fill(pageRect)

            ctx.cgContext.translateBy(x: 0.0, y: pageRect.size.height)
            ctx.cgContext.scaleBy(x: 1.0, y: -1.0)

            ctx.cgContext.drawPDFPage(page)
        }

        return img
    }
}

private struct TestView: View {
    let image: UIImage
    let text: String
    let font: FontConfiguration

    var body: some View {
        ZStack(alignment: .topLeading) {
            Image(uiImage: image)

            Text(text)
                .fontConfiguration(font)
                .foregroundColor(.red)
                .background(Color.black.opacity(0.1))
                .background(Color.blue.opacity(0.1).padding(.leading, -8).padding(.trailing, -8))
                .padding([.top, .leading], 18)
        }
    }
}

private struct TestLayout: ModernLayout {
    let image: UIImage
    let text: String
    let font: FontConfiguration

    var body: LayoutConvertible {
        HStackLayout {
            image
                .overlay(
                    VStackLayout {
                        text.attributed()
                            .font(font)
                            .foregroundColor(UIColor.red)
                            .background(UIColor.black.withAlphaComponent(0.1))
                            .background(UIColor.blue.withAlphaComponent(0.1).margin(left: -8, right: -8))

                        SpacerLayout()
                    },
                    position: .init(top: 18, left: 18, bottom: 0, right: 0)
                )
        }
    }
}
