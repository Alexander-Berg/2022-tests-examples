//
//  GarageRatingCellTests.swift
//  Tests
//
//  Created by Igor Shamrin on 26.07.2021.
//
import XCTest
import AutoRuProtoModels
import Snapshots
@testable import AutoRuGarageCard

final class GarageRatingCellTests: BaseUnitTest {

    func test_notExpended() throws {
        // arrange
        let featuesStub = GarageCardViewModel.Features.init(model: try .init(jsonString: Self.featuresModelJson),
                                                                       selectedIndex: 0,
                                                                       isExpanded: false,
                                                                       ratingFromReviews: 1.0,
                                                                       reviewsCount: 10)

        let layout = GarageRatingCell(modelName: "Model Name",
                                      features: featuesStub,
                                      openFeature: { _ in },
                                      updateFeatureSelectedIndex: { _ in },
                                      updateFeatureExpanded: { _ in })
        // assert
        Snapshot.compareWithSnapshot(layout: layout, maxWidth: DeviceWidth.iPhone11)
    }

    func test_expended() throws {
        // arrange
        let featuesStub = GarageCardViewModel.Features.init(model: try .init(jsonString: Self.featuresModelJson),
                                                                       selectedIndex: 0,
                                                                       isExpanded: true,
                                                                       ratingFromReviews: 1.0,
                                                                       reviewsCount: 10)

        let layout = GarageRatingCell(modelName: "Model Name",
                                      features: featuesStub,
                                      openFeature: { _ in },
                                      updateFeatureSelectedIndex: { _ in },
                                      updateFeatureExpanded: { _ in })
        // assert
        Snapshot.compareWithSnapshot(layout: layout, maxWidth: DeviceWidth.iPhone11)
    }

    func test_empty() throws {
        // arrange
        let layout = GarageRatingCell(modelName: "Model Name",
                                      features: nil,
                                      openFeature: { _ in },
                                      updateFeatureSelectedIndex: { _ in },
                                      updateFeatureExpanded: { _ in })
        // assert
        Snapshot.compareWithSnapshot(layout: layout, maxWidth: DeviceWidth.iPhone11)
    }

    static let featuresModelJson =
    """
    {
        "positive": [
            {
            "name": "????????????????????",
            "plusCount": 13,
            "minusCount": 3,
            "type": "RELIABILITY"
            },
            {
            "name": "????????????",
            "plusCount": 9,
            "minusCount": 1,
            "type": "DESIGN"
            },
            {
            "name": "????????????????????????",
            "plusCount": 8,
            "type": "PASSABILITY"
            },
            {
            "name": "???????????????? ????????????",
            "plusCount": 5,
            "minusCount": 2,
            "type": "ASSEMBLY_QUALITY"
            },
            {
            "name": "????????????????",
            "plusCount": 5,
            "minusCount": 3,
            "type": "SUSPENSION"
            },
            {
            "name": "????????????????",
            "plusCount": 4,
            "minusCount": 2,
            "type": "DYNAMICS"
            },
            {
            "name": "????????????????????????",
            "plusCount": 1,
            "type": "NOISE"
            },
            {
            "name": "????????????????",
            "plusCount": 1,
            "type": "TRUNK"
            }
        ],
        "negative": [
            {
            "name": "?????????????????? ????????????????????????",
            "plusCount": 2,
            "minusCount": 7,
            "type": "MAINTENANCE_COST"
            },
            {
            "name": "??????????????",
            "plusCount": 1,
            "minusCount": 6,
            "type": "COMFORT"
            },
            {
            "name": "?????????????? ??????????????",
            "minusCount": 2,
            "type": "GEAR"
            },
            {
            "name": "????????????????????",
            "minusCount": 2,
            "type": "VISIBILITY"
            },
            {
            "name": "??????????????????????",
            "minusCount": 1,
            "type": "MULTIMEDIA"
            },
            {
            "name": "?????????????????????????????? ????????????",
            "minusCount": 1,
            "type": "SPACE"
            }
        ],
        "controversy": [
            {
            "name": "???????????? ??????????????",
            "plusCount": 3,
            "minusCount": 3,
            "type": "FUEL"
            },
            {
            "name": "????????????????????????",
            "plusCount": 1,
            "minusCount": 1,
            "type": "SAFETY"
            },
            {
            "name": "??????????????????????????",
            "plusCount": 3,
            "minusCount": 3,
            "type": "STEERING"
            }
        ],
        "status": "SUCCESS"
    }
    """
}
