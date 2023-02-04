import XCTest
import AutoRuProtoModels
import AutoRuAppConfig
@testable import AutoRuOfferAdvantage

final class OfferScoreImprovementActionTests: BaseUnitTest {
    override class func setUp() {
        super.setUp()

        AppSettings.sharedInstance.experimentProvider.update(expValues: [:])
    }

    func test_mosRuExpEnabled() {
        let scoring: Auto_Api_TransparencyScoring = .with { scoring in
            scoring.mosruScore = 0.0
        }

        XCTAssertEqual(scoring.availableActions, [.mosRu])

        let scoring1: Auto_Api_TransparencyScoring = .with { scoring in
            scoring.mosruScore = 1.0
        }

        XCTAssertEqual(scoring1.availableActions, [])
    }

    func test_provenOwner() {
        let scoring: Auto_Api_TransparencyScoring = .with { scoring in
            scoring.provenOwnerScore = 0.0
        }

        XCTAssertEqual(scoring.availableActions, [.provenOwner])

        let scoring1: Auto_Api_TransparencyScoring = .with { scoring in
            scoring.provenOwnerScore = 1.0
        }

        XCTAssertEqual(scoring1.availableActions, [])
    }

    func test_3photos() {
        let scoring: Auto_Api_TransparencyScoring = .with { scoring in
            scoring.threePhotoScore = 0.0
        }

        XCTAssertEqual(scoring.availableActions, [.threePhoto])

        let scoring1: Auto_Api_TransparencyScoring = .with { scoring in
            scoring.threePhotoScore = 1.0
        }

        XCTAssertEqual(scoring1.availableActions, [])
    }

    func test_photoWithGRZ() {
        let scoring: Auto_Api_TransparencyScoring = .with { scoring in
            scoring.photoWithGrzScore = 0.0
        }

        XCTAssertEqual(scoring.availableActions, [.photoWithGRZ])

        let scoring1: Auto_Api_TransparencyScoring = .with { scoring in
            scoring.photoWithGrzScore = 1.0
        }

        XCTAssertEqual(scoring1.availableActions, [])
    }

    func test_vinTth() {
        let scoring: Auto_Api_TransparencyScoring = .with { scoring in
            scoring.sameVinTthScore = 0.0
        }

        XCTAssertEqual(scoring.availableActions, [.sameVinTth])

        let scoring1: Auto_Api_TransparencyScoring = .with { scoring in
            scoring.sameVinTthScore = 1.0
        }

        XCTAssertEqual(scoring1.availableActions, [])
    }

    func test_longDescription() {
        let scoring: Auto_Api_TransparencyScoring = .with { scoring in
            scoring.longDescriptionScore = 0.0
        }

        XCTAssertEqual(scoring.availableActions, [.longDescription])

        let scoring1: Auto_Api_TransparencyScoring = .with { scoring in
            scoring.longDescriptionScore = 1.0
        }

        XCTAssertEqual(scoring1.availableActions, [])
    }
}
