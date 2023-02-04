import UIKit
import RxSwift
import AutoRuUtils
import AutoRuFetchableImage
import CoreGraphics

extension FetchableImage {
    static func testImage(withFixedSize size: CGSize) -> FetchableImage {
        .generate("test image \(size.width)x\(size.height)") { _ in
            UIGraphicsImageRenderer(size: size).image { ctx in
                let rect = CGRect(origin: .zero, size: size)
                ctx.cgContext.setFillColor(UIColor.yellow.cgColor)
                ctx.fill(rect)

                ctx.cgContext.saveGState()
                ctx.cgContext.move(to: CGPoint(x: 0, y: size.height))
                ctx.cgContext.addLine(to: CGPoint(x: size.width, y: size.height))
                ctx.cgContext.addLine(to: CGPoint(x: size.width, y: 0))
                ctx.cgContext.closePath()

                ctx.cgContext.setFillColor(UIColor.green.cgColor)
                ctx.cgContext.drawPath(using: .fill)
                ctx.cgContext.restoreGState()

                ctx.cgContext.setLineWidth(10)
                ctx.cgContext.setStrokeColor(UIColor.red.cgColor)
                ctx.cgContext.addPath(CGPath(rect: rect.insetBy(dx: 5, dy: 5), transform: nil))
                ctx.cgContext.drawPath(using: .stroke)
            }
        }
    }

    static var infineLoadingImage: FetchableImage {
        .generateAsync("infineLoadingImage") { options, callback in
            Disposables.create {
                _ = callback
            }
        }
    }
}
