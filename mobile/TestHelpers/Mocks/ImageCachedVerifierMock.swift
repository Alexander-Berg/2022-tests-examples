//
//  ImageCachedVerifierMock.swift
//  14.12.2021
//

import Foundation
@testable import YandexDisk

final class ImageCachedVerifierMock: ImageCachedVerifier {
    func fileExists(inCache _: YDImageCacheType, at _: String) -> Bool {
        return false
    }
}
