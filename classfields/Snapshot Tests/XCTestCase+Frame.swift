//
//  XCTestCase+Frame.swift
//  Unit Tests
//
//  Created by Leontyev Saveliy on 19.01.2022.
//  Copyright © 2022 Yandex. All rights reserved.
//

import Foundation
import XCTest

extension XCTestCase {
    static func frame(by calculateHeight: @escaping (CGFloat) -> CGFloat) -> CGRect {
        let width = UIScreen.main.bounds.width
        let height = calculateHeight(width)
        let size: CGSize = .init(width: width, height: height)
        let frame: CGRect = .init(origin: .zero, size: size)
        return frame
    }
}
