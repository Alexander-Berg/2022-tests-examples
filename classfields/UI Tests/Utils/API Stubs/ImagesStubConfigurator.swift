//
//  ImagesStubConfigurator.swift
//  UI Tests
//
//  Created by Alexey Salangin on 24.11.2021.
//  Copyright © 2021 Yandex. All rights reserved.
//

import UIKit

enum ImagesStubConfigurator {
    static func setupColors(using dynamicStubs: HTTPDynamicStubs) {
        dynamicStubs.register(
            method: .GET,
            path: "/stubRed.png",
            middleware: middleware(with: .red)
        )

        dynamicStubs.register(
            method: .GET,
            path: "/stubOrange.png",
            middleware: middleware(with: .orange)
        )

        dynamicStubs.register(
            method: .GET,
            path: "/stubYellow.png",
            middleware: middleware(with: .yellow)
        )

        dynamicStubs.register(
            method: .GET,
            path: "/stubGreen.png",
            middleware: middleware(with: .green)
        )
    }

    static func setupYouTubeVideoPreview(using dynamicStubs: HTTPDynamicStubs) {
        let middleware = MiddlewareBuilder
            .respondWith(.ok(.contentsOfImage(UIImage(color: .purple))))
            .build()
        dynamicStubs.register(
            method: .GET,
            path: "/youtube-max-size-preview",
            middleware: middleware
        )
    }

    private static func middleware(with color: UIColor) -> MiddlewareProtocol {
        MiddlewareBuilder
            .respondWith(.ok(.contentsOfImage(UIImage(color: color))))
            .build()
    }
}
