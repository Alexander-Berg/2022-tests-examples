//
//  YxOnboardingSlideViewTests.swift
//  YxSwissKnifeTests
//
//  Created by Andrey M. Sboev on 19/12/2018.
//  Copyright © 2018 Yandex. All rights reserved.
//

import Foundation
import XCTest

@testable import YxSwissKnife

public final class YxOnboardingSlideViewTest: XCTestCase {

    func testOnboardingView() {
        let configuration = YxScreenshotTestConfiguration(
            recordMode: false,
            useDrawHierarchyInRect: true,
            referenceDir: referenceDirPath
        )
        let view = YxOnboardingSlideView(frame: CGRect(x: 0, y: 0, width: 320, height: 800))
        let slide = Slide()
        if slide.image == nil {
            fatalError()
        }
        view.update(slide: slide)
        do {
            try view.check(
                conf: configuration,
                identifier: configuration.makeDefaultIdentifier(pack: "\(type(of: self))"),
                tolerance: 0
            )
        } catch {
            XCTFail("Comparison failed, but shouldn't")
        }
    }
}

public class Slide: YxOnboardingSlide {
    public var image: UIImage? {
        return UIImage(color: .white, size: CGSize(width: 220, height: 220))
    }

    public var title: String {
        return "Уведомления о смене погоды"
    }

    public var description: String {
        return "Мы подскажем вам, какая погода будет на завтра, сообщим о резкой смене погоды, дожде и о ближайших астрономических ялениях."
    }

    public var actionTitle: String {
        return "Sign In"
    }

    public var secondaryActionTitle: String {
        return "Later"
    }
}

extension UIImage {
    public convenience init?(color: UIColor, size: CGSize = CGSize(width: 1, height: 1)) {
        let rect = CGRect(origin: .zero, size: size)
        UIGraphicsBeginImageContextWithOptions(rect.size, false, 0.0)
        color.setFill()
        UIRectFill(rect)
        let image = UIGraphicsGetImageFromCurrentImageContext()
        UIGraphicsEndImageContext()

        guard let cgImage = image?.cgImage else { return nil }
        self.init(cgImage: cgImage)
    }
}

private let referenceDirKey = "SNAPSHOT_TEST_REF_DIR"
private let referenceDirPath = ProcessInfo.processInfo.environment[referenceDirKey]!
