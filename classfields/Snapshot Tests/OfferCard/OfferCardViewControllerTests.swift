//
//  OfferCardViewControllerTests.swift
//  Unit Tests
//
//  Created by Alexey Salangin on 08.06.2022.
//  Copyright © 2022 Yandex. All rights reserved.
//

import YREModuleFactory
import YREDesignKit
import XCTest

final class OfferCardViewControllerTests: XCTestCase {
    func testViewControllerWithCallAndChatButtons() {
        let stub = OfferCardStub.withChat
        self.testOfferCardViewController(with: stub)
    }

    func testViewControllerWithCallButton() {
        let stub = OfferCardStub.sellRoom
        self.testOfferCardViewController(with: stub)
    }

    func testViewControllerWithoutCommunicationButtons() {
        let stub = OfferCardStub.outdated
        self.testOfferCardViewController(with: stub)
    }

    private func testOfferCardViewController(with stub: OfferCardStub, function: String = #function) {
        guard let offer = stub.loadOffer() else {
            XCTFail("Couldn't create offer from stub.")
            return
        }
        let mock = MapScreenModuleDepsMock(anyOfferSearchService: AnyOfferSearchServiceMock(offer: offer))
        let parallel = YREParallelNavigationViewController(rootController: UIViewController())
        let module = mock.makeOfferCardModule(
            arguments: .offer(
                offer,
                offerData: OfferCardArguments.OfferData(
                    globalActionCategoryFilterSnapshot: nil,
                    globalSortType: .unspecified
                )
            ),
            navigationContext: .init(
                navigationController: parallel,
                isPresentedModally: false
            ),
            referrer: nil
        )
        let viewController = module.viewController

        viewController.beginAppearanceTransition(true, animated: false)
        viewController.endAppearanceTransition()

        self.assertSnapshot(viewController.view, function: function)
    }
}
