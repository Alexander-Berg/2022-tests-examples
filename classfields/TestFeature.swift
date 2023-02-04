//
//  TestFeature.swift
//  YREFeatures
//
//  Created by Aleksey Gotyanov on 11/2/20.
//  Copyright © 2020 Yandex. All rights reserved.
//

extension Features {
    public struct TestFeature: FeatureType {
        public var id: FeatureID { .test }

        public init() { }

        public var title: String { "For test" }
    }
}
