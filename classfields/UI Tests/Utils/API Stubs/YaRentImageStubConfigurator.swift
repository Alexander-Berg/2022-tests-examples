//
//  YaRentImageStubConfigurator.swift
//  UI Tests
//
//  Created by Leontyev Saveliy on 12.01.2022.
//  Copyright © 2022 Yandex. All rights reserved.
//

import Foundation
import XCTest

enum YaRentImageStubConfigurator {
    static func setupImageUploading(
        with dynamicStubs: HTTPDynamicStubs,
        uploadingExpectation: XCTestExpectation,
        expectedCount: Int
    ) {
        dynamicStubs.register(method: .POST, path: "/2.0/files/get-upload-url", filename: "files-get-uploading-url.debug")

        var uploadedPhotosCount = 0
        let uploadingMiddlware = MiddlewareBuilder()
            .callback({ _ in
                uploadedPhotosCount += 1
                if uploadedPhotosCount == expectedCount {
                    uploadingExpectation.fulfill()
                }
            })
            .respondWith(.ok(.contentsOfJSON("files-uploaded-rent-image.debug")))
            .build()

        dynamicStubs.register(method: .POST, path: "/test-upload-image-path", middleware: uploadingMiddlware)
    }
}
