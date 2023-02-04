import UIKit
import XCTest
import CoreGraphics
import Foundation

public struct SnapshotIdentifier {
    public let suite: String
    public let identifier: String
    public let options: Option
    public var style: UIUserInterfaceStyle

    public init(
        suite: String,
        identifier: String,
        options: Option = [.scale, .screen, .interfaceStyle],
        style: UIUserInterfaceStyle = .unspecified
    ) {
        guard let identifier = SnapshotIdentifier.sanitizeName(identifier) else {
            fatalError("Invalid snapshot name")
        }
        self.suite = suite
        self.identifier = identifier
        self.options = options
        self.style = style
    }

    public static func make(style: UIUserInterfaceStyle = .unspecified, _ file: String = #file, identifier: String = #function) -> SnapshotIdentifier {
        return SnapshotIdentifier(
            suite: suiteName(from: file),
            identifier: String(identifier.dropLast(2)),
            style: style
        )
    }

    public static func make(identifier: String, style: UIUserInterfaceStyle, _ file: String = #filePath) -> SnapshotIdentifier {
        SnapshotIdentifier(
            suite: suiteName(from: file),
            identifier: identifier,
            style: style
        )
    }

    public static func suiteName(from file: StaticString) -> String {
        return URL(fileURLWithPath: "\(file)").deletingPathExtension().lastPathComponent
    }

    public static func suiteName(from file: String) -> String {
        return URL(fileURLWithPath: "\(file)").deletingPathExtension().lastPathComponent
    }

    public struct Option: OptionSet {
        public let rawValue: Int

        public init(rawValue: Int) {
            self.rawValue = rawValue
        }

        public static let scale = Option(rawValue: 1 << 0)
        public static let screen = Option(rawValue: 1 << 1)
        public static let device = Option(rawValue: 1 << 2)
        public static let os = Option(rawValue: 1 << 3)
        public static let interfaceStyle = Option(rawValue: 1 << 4)
    }
    
    private static let arcadiaPathRegex: NSRegularExpression = makeArcadiaPathRegex()
    private static func makeArcadiaPathRegex() -> NSRegularExpression {
        // https://a.yandex-team.ru/arc_vcs/vcs/migration/scripts/check.py?rev=r5511015#L7
        let regexPattern = "[^\\/a-zA-Z0-9_\\- .~,()\\[\\]\\{\\}+=#$@!]"

        // swiftlint:disable:next force_try
        let result = try! NSRegularExpression(
            pattern: regexPattern,
            options: []
        )
        return result
    }

    private static func sanitizeName(_ value: String) -> String? {
        guard let value = value.applyingTransform(.latinToCyrillic, reverse: true) else {
            return nil
        }
        let result = self.arcadiaPathRegex.stringByReplacingMatches(
            in: value,
            options: [],
            range: .init(location: 0, length: value.count),
            withTemplate: ""
        )
        return result
    }
}

public enum Snapshot {
    public static let defaultPerPixelTolerance = 0.04
    public static let defaultOverallTolerance = 0.01

    public static func compareWithSnapshot(
        image: UIImage,
        identifier: String = #function,
        style: UIUserInterfaceStyle = .unspecified,
        perPixelTolerance: Double = Self.defaultPerPixelTolerance,
        overallTolerance: Double = Self.defaultOverallTolerance,
        ignoreEdges: UIEdgeInsets = .zero,
        file: StaticString = #file,
        line: UInt = #line
    ) {
        self.compareWithSnapshot(
            image: image,
            identifier: .init(suite: SnapshotIdentifier.suiteName(from: file), identifier: identifier, style: style),
            perPixelTolerance: perPixelTolerance,
            overallTolerance: overallTolerance,
            ignoreEdges: ignoreEdges,
            file: file,
            line: line
        )
    }

    public static func compareWithSnapshot(
        image: UIImage,
        identifier: SnapshotIdentifier,
        perPixelTolerance: Double = Self.defaultPerPixelTolerance,
        overallTolerance: Double = Self.defaultOverallTolerance,
        ignoreEdges: UIEdgeInsets = .zero,
        file: StaticString = #file,
        line: UInt = #line
    ) {
        let snapshotPath = SnapshotHelper.snapshotPath(identifier: identifier)

        let activityName = "Сравниваем снепшот \"\(identifier.suite) -> \(SnapshotHelper.snapshotName(identifier: identifier))\""

        if SnapshotHelper.fileExists(path: snapshotPath), let referenceImage = SnapshotHelper.read(path: snapshotPath) {
            // Этой проверкой решаем ситуацию со скринами в landscape - при сохранении в png получаем картинку в портрете, так как сохраняется содержимое CGImage без учёта флага ориентации картинки.
            // При чтении файла с диска затем получим чисто портретную картинку, в то время как проверяемый скрин будет в landscape на уровне UIImage и portrait на уровне CGImage.
            // Конверт скринов в landscape перед сохранением проблемы не решит, так как у картинки с диска всегда будет одинаковое разрешение в UIImage и CGImage, а у скрина в landscape - разное -> одна из следующих проверок сломается.

            var equalSizes = false
            let rotatedSize = CGSize(width: image.pixelSize.height, height: image.pixelSize.width)
            if image.pixelSize == referenceImage.pixelSize || referenceImage.pixelSize == rotatedSize {
                equalSizes = true
            }

            if !equalSizes {
                let diffImage = image.diff(from: referenceImage)

                SnapshotHelper.collectNewOrUnmatchedSnapshot(image: image, identifier: identifier)

                SnapshotHelper.runActivity(name: activityName, reference: referenceImage, test: image, diff: diffImage) {
                    XCTFail("Тестовый и референсный скриншоты отличаются по размеру", file: file, line: line)
                }
            } else if !image.compare(
                with: referenceImage,
                perPixelTolerance: perPixelTolerance,
                overallTolerance: overallTolerance,
                ignoreEdges: ignoreEdges
            ) {
                let diffImage = image.diff(from: referenceImage)

                SnapshotHelper.collectNewOrUnmatchedSnapshot(image: image, identifier: identifier)

                SnapshotHelper.runActivity(name: activityName, reference: referenceImage, test: image, diff: diffImage) {
                    let message = "Тестовый и референсный скриншоты отличаются попиксельно. "
                        + "Порог: отличие в каждом пикселе = \(Int(perPixelTolerance * 100))%, всего = \(Int(overallTolerance * 100))%"
                    XCTFail(message, file: file, line: line)
                }
            } else {
                SnapshotHelper.runActivity(name: activityName, reference: referenceImage, test: image, diff: nil)
            }
        } else {
            do {
                let image: UIImage = UIImage(
                    cgImage: image.cgImage!.copy(colorSpace: CGColorSpace(name: CGColorSpace.sRGB)!)!
                )

                try SnapshotHelper.write(image: image, path: snapshotPath)

                SnapshotHelper.collectNewOrUnmatchedSnapshot(image: image, identifier: identifier)

                SnapshotHelper.runActivity(name: activityName, reference: image, test: nil, diff: nil) {
                    let message = "Референсный скриншот не найден. "
                        + "Создан новый референс '\(SnapshotHelper.snapshotName(identifier: identifier))', путь = \(snapshotPath). "
                        + "Перезапустите тест, чтобы сравнить скриншоты ещё раз"

                    XCTFail(message, file: file, line: line)
                }
            } catch {
                XCTFail("Скриншот не записан, повторите снова; \(error.localizedDescription)", file: file, line: line)
            }
        }
    }

}

public enum SnapshotHelper {
    private static let fileManager = FileManager.default

    private static let snapshotsRoot = URL(fileURLWithPath: "../../../", relativeTo: URL(fileURLWithPath: #filePath))

    private static let snapshotsDirectory = snapshotsRoot.appendingPathComponent("References")

    private static let newOrUnmatchedSnapshotsDirectory = snapshotsRoot.appendingPathComponent("NewReferences")

    static let isUITests: Bool = {
        if let bundleName = Bundle.main.infoDictionary?["CFBundleName"] as? String, bundleName.hasPrefix("UITests") {
            return true
        }
        return false
    }()

    static var screenSize: CGSize {
        if self.isUITests {
            return XCUIApplication(bundleIdentifier: "ru.AutoRu").windows.element(boundBy: 0).frame.size
        }

        return UIScreen.main.bounds.size
    }

    static func runActivity(
        name: String,
        reference: UIImage,
        test: UIImage?,
        diff: UIImage?,
        onFail: (() -> Void)? = nil
    ) {
        XCTContext.runActivity(named: name) { activity in
            attach(image: reference, to: activity, name: "Референсный скриншот")

            defer { onFail?() }

            guard diff != nil else {
                return
            }

            attach(image: test, to: activity, name: "Тестовый скриншот")
            attach(image: diff, to: activity, name: "Разница в скриншотах")
        }
    }

    private static func attach(image: UIImage?, to activity: XCTActivity, name: String) {
        guard let image = image else {
            return
        }

        let attachment = XCTAttachment(image: image)
        attachment.name = name
        attachment.lifetime = .deleteOnSuccess
        activity.add(attachment)
    }

    static func snapshotName(identifier: SnapshotIdentifier) -> String {
        var filename = identifier.identifier

        if identifier.options.contains(.device) {
            let device = UIDevice.current.model
            filename += "_\(device)"
        }

        if identifier.options.contains(.os) {
            let os = UIDevice.current.systemVersion
            filename += "_\(os)"
        }

        if identifier.options.contains(.screen) {
            let screen = self.screenSize
            filename += "_\(Int(screen.width))x\(Int(screen.height))"
        }

        if identifier.options.contains(.scale) {
            let scale = UIScreen.main.scale
            filename += "@\(Int(scale))x"
        }

        if identifier.options.contains(.interfaceStyle) {
            if identifier.style == .dark {
                filename += "_dark"
            }
        }

        return filename
    }

    static func snapshotPath(identifier: SnapshotIdentifier, baseDirectory: URL) -> String {
        let url = baseDirectory
            .appendingPathComponent(identifier.suite, isDirectory: true)
            .appendingPathComponent(SnapshotHelper.snapshotName(identifier: identifier))
            .appendingPathExtension("png")

        return url.path
    }

    public static func snapshotPath(identifier: SnapshotIdentifier) -> String {
        snapshotPath(identifier: identifier, baseDirectory: Self.snapshotsDirectory)
    }

    static func fileExists(path: String) -> Bool {
        return self.fileManager.fileExists(atPath: path)
    }

    static func write(image: UIImage, path: String) throws {
        let url = URL(fileURLWithPath: path)
        try self.fileManager.createDirectory(
            atPath: url.deletingLastPathComponent().path,
            withIntermediateDirectories: true,
            attributes: nil
        )

        guard let pngData = image.pngData() else {
            throw Error.pngRepresentationFailed
        }

        try pngData.write(to: url, options: [.atomicWrite])
    }

    static func read(path: String) -> UIImage? {
        return UIImage(contentsOfFile: path)
    }

    static func collectNewOrUnmatchedSnapshot(image: UIImage, identifier: SnapshotIdentifier) {
        guard TestsLaunchParametersHelper.shouldCollectNewOrUnmatchedSnapshots else { return }

        do {
            guard let pngData = image.pngData() else {
                throw Error.pngRepresentationFailed
            }

            XCTContext.runActivity(named: "Перегенерированный скриншот") { activity in
                let attachment = XCTAttachment(data: pngData)
                attachment.name = identifier.identifier + ".png"
                attachment.lifetime = .keepAlways
                activity.add(attachment)
            }
        } catch {
            XCTFail("Невозможно записать свежий снепшот")
        }
    }

    enum Error: Swift.Error {
        case pngRepresentationFailed
    }
}

extension UIImage {
    public struct PixelRepresentation: Equatable {
        let red: UInt8
        let green: UInt8
        let blue: UInt8
        let alpha: UInt8

        static func componentDiff(_ c1: UInt8, _ c2: UInt8) -> Double {
            return fabs(Double(c1) - Double(c2)) / 255.0
        }
    }

    public func cropping(insets: UIEdgeInsets) -> UIImage {
        let scale = self.scale
        let insets = UIEdgeInsets(
            top: insets.top * scale,
            left: insets.left * scale,
            bottom: insets.bottom * scale,
            right: insets.right * scale
        )

        let croppedSize = CGRect(
            origin: .zero,
            size: CGSize(width: self.size.width * scale, height: self.size.height * scale)
        ).inset(by: insets)

        return self.cgImage?
            .cropping(to: croppedSize)
            .flatMap { UIImage(cgImage: $0, scale: scale, orientation: .up) } ?? self
    }

    // perPixelTolerance - насколько каждый пиксель может не совпадать
    // overallTolerance - сколько пикселей может не совпадать
    fileprivate func compare(
        with image: UIImage,
        perPixelTolerance: Double,
        overallTolerance: Double,
        ignoreEdges: UIEdgeInsets = .zero
    ) -> Bool {
        guard var referenceImage = self.cgImage, var image = image.cgImage else {
            assert(false, "Unable to receive undelying images")
            return false
        }

        guard let defaultColorSpace = CGColorSpace(name: CGColorSpace.sRGB) else {
            assert(false, "Unable to get default color space")
            return false
        }

        if referenceImage.colorSpace != defaultColorSpace {
            referenceImage = referenceImage.copy(colorSpace: defaultColorSpace)!
        }

        if image.colorSpace != defaultColorSpace {
            image = image.copy(colorSpace: defaultColorSpace)!
        }

        assert(
            referenceImage.width * referenceImage.height != 0 && image.width * image.height != 0,
            "Size should be greater than zero"
        )

        assert(
            referenceImage.width == image.width && referenceImage.height == image.height,
            "Comparable images should have equal size"
        )

        // Crop images to remove ignored edge zones.
        let cropRect = CGRect(x: 0, y: 0, width: referenceImage.width, height: referenceImage.height)
            .inset(by: ignoreEdges)
        referenceImage = referenceImage.cropping(to: cropRect) ?? referenceImage
        image = image.cropping(to: cropRect) ?? image

        let minBytesPerRow = min(referenceImage.bytesPerRow, image.bytesPerRow)
        let imageSizeBytes = Int(image.height) * minBytesPerRow

        let alignment = MemoryLayout<PixelRepresentation>.alignment
        let referenceImagePixelsBuffer = UnsafeMutableRawPointer.allocate(byteCount: imageSizeBytes, alignment: alignment)
        let imagePixelsBuffer = UnsafeMutableRawPointer.allocate(byteCount: imageSizeBytes, alignment: alignment)

        for buffer in [referenceImagePixelsBuffer, imagePixelsBuffer] {
            buffer.initializeMemory(
                as: PixelRepresentation.self,
                repeating: PixelRepresentation(red: 0, green: 0, blue: 0, alpha: 0),
                count: imageSizeBytes / MemoryLayout<PixelRepresentation>.stride
            )
        }

        defer {
            referenceImagePixelsBuffer.deallocate()
            imagePixelsBuffer.deallocate()
        }

        guard
            let referenceImageContext = CGContext(
                data: referenceImagePixelsBuffer,
                width: image.width,
                height: image.height,
                bitsPerComponent: referenceImage.bitsPerComponent,
                bytesPerRow: minBytesPerRow,
                space: referenceImage.colorSpace ?? defaultColorSpace,
                bitmapInfo: CGImageAlphaInfo.premultipliedLast.rawValue
            ) else {
                XCTFail("Unable to init graphics context for referenceImage")
                return false
            }

        guard
            let imageContext = CGContext(
                data: imagePixelsBuffer,
                width: image.width,
                height: image.height,
                bitsPerComponent: image.bitsPerComponent,
                bytesPerRow: minBytesPerRow,
                space: image.colorSpace ?? defaultColorSpace,
                bitmapInfo: CGImageAlphaInfo.premultipliedLast.rawValue
            ) else {
                XCTFail("Unable to init graphics context for image")
                return false
            }

        let imageSize = CGSize(width: image.width, height: image.height)
        referenceImageContext.draw(referenceImage, in: CGRect(origin: .zero, size: imageSize))
        imageContext.draw(image, in: CGRect(origin: .zero, size: imageSize))

        if overallTolerance == 0.0 && perPixelTolerance == 0.0 {
            return memcmp(referenceImagePixelsBuffer, imagePixelsBuffer, imageSizeBytes) == 0
        }

        let result = self.comparePixels(
            lhs: referenceImagePixelsBuffer,
            rhs: imagePixelsBuffer,
            size: image.width * image.height,
            overallTolerance: overallTolerance,
            perPixelTolerance: perPixelTolerance
        )

        return result
    }

    private func comparePixels(
        lhs: UnsafeRawPointer,
        rhs: UnsafeRawPointer,
        size: Int,
        overallTolerance: Double,
        perPixelTolerance: Double
    ) -> Bool {
        func isPixelEqual(lhs: PixelRepresentation, rhs: PixelRepresentation, tolerance: Double) -> Bool {
            if lhs == rhs {
                return true
            } else if tolerance == 0.0 {
                return false
            }

            let redDiff = PixelRepresentation.componentDiff(lhs.red, rhs.red)
            let greenDiff = PixelRepresentation.componentDiff(lhs.green, rhs.green)
            let blueDiff = PixelRepresentation.componentDiff(lhs.blue, rhs.blue)

            return redDiff <= tolerance && greenDiff <= tolerance && blueDiff <= tolerance
        }

        var diffPixels = 0

        var i = 0
        while i < size {
            let lPx = lhs.load(fromByteOffset: MemoryLayout<PixelRepresentation>.size * i, as: PixelRepresentation.self)
            let rPx = rhs.load(fromByteOffset: MemoryLayout<PixelRepresentation>.size * i, as: PixelRepresentation.self)

            let isEqual = isPixelEqual(lhs: lPx, rhs: rPx, tolerance: perPixelTolerance)

            if !isEqual {
                diffPixels += 1

                let percent = Double(diffPixels) / Double(size)
                if percent > overallTolerance {
                    return false
                }
            }

            i += 1
        }

        return true
    }

    public func diff(from image: UIImage) -> UIImage {
        let size = CGSize(width: self.size.width * self.scale, height: self.size.height * self.scale)
        let imageSize = CGSize(width: image.size.width * image.scale, height: image.size.height * image.scale)

        let diffSize = CGSize(
            width: max(size.width, imageSize.width),
            height: max(size.height, imageSize.height)
        )

        UIGraphicsBeginImageContextWithOptions(diffSize, true, 0)
        defer {
            UIGraphicsEndImageContext()
        }

        guard let context = UIGraphicsGetCurrentContext() else {
            fatalError("Invalid context")
        }

        self.draw(in: CGRect(origin: .zero, size: size))
        context.setAlpha(0.5)
        context.beginTransparencyLayer(in: CGRect(origin: .zero, size: diffSize), auxiliaryInfo: nil)
        image.draw(in: CGRect(origin: .zero, size: imageSize))
        context.setBlendMode(.difference)
        context.setFillColor(UIColor.white.cgColor)
        context.fill(CGRect(origin: .zero, size: size))
        context.endTransparencyLayer()

        guard let diffImage = UIGraphicsGetImageFromCurrentImageContext() else {
            fatalError("Unable to draw images diff")
        }

        return diffImage
    }

    fileprivate var pixelSize: CGSize {
        size.applying(.init(scaleX: scale, y: scale))
    }
}

extension XCUIElementQuery {
    func withIdentifierPrefix(_ prefix: String) -> XCUIElementQuery {
        let predicate = NSPredicate(format: "identifier BEGINSWITH %@", prefix)
        return self.matching(predicate)
    }
}
