//
//  UserOfferSnippetTests.swift
//  Unit Tests
//
//  Created by Fedor Solovev on 16.03.2022.
//  Copyright © 2022 Yandex. All rights reserved.
//

import XCTest
import YRETestsUtils
import YREWeb
import YREModel
@testable import YREUserOffersModule

final class UserOfferSnippetTests: XCTestCase {
    func testUserOfferSnippetWithWarning() {
        let viewModel = UserOfferSnippetViewModel(type: .discrimination,
                                                  titleFormattingBlock: { _ in .init() },
                                                  imageFormattingBlock: nil,
                                                  noImageAsset: nil,
                                                  price: "123123",
                                                  isEditPriceButtonEnabled: true,
                                                  viewsString: nil,
                                                  statusString: "",
                                                  mosRuConnectionState: nil,
                                                  publishButtonTitle: nil,
                                                  productsViewModel: nil,
                                                  warning: "Длинная строка с предупреждением. Длинная строка с предупреждением.",
                                                  banReasonTitle: nil,
                                                  banReasonText: nil,
                                                  moreBanReasons: nil,
                                                  paymentProblemText: nil,
                                                  isAddPhotoButtonEnabled: false,
                                                  isProlongationButtonEnabled: false,
                                                  isRemoveButtonEnabled: true,
                                                  isPublishButtonEnabled: false,
                                                  isEditButtonEnabled: true,
                                                  inPlaceActionType: nil,
                                                  shouldDisplayLoadingStub: false,
                                                  shouldHideSeparator: false)

        let cell = UserOfferSnippetCell()
        cell.configure(viewModel: viewModel)
        let width = UIScreen.main.bounds.width
        cell.frame = .init(x: 0, y: 0, width: width, height: 300)

        self.assertSnapshot(cell)
    }

    func testUserOfferOnModeration() {
        Stub.Moderation.allCases
            .map(Stub.moderation)
            .forEach(self.testUserOfferSnippetLayout)
    }

    // MARK: - Private

    private func testUserOfferSnippetLayout(with stub: Stub) {
        let json = Self.loadStubJSON(from: stub.filename)
        let model = YREUnwrap(YREUnifiedUserOffer(json: json))
        let viewModel = UserOfferSnippetViewModelGenerator.generate(
            userOffer: model,
            inPlaceActionType: nil,
            shouldDisplayLoadingStub: false,
            shouldHideSeparator: false
        )

        let cell = UserOfferSnippetCell()
        cell.configure(viewModel: viewModel)

        let width = UIScreen.main.bounds.width
        let height = UserOfferSnippetCell.height(forConfiguredView: cell)
        cell.frame.size = CGSize(width: width, height: height)

        self.assertSnapshot(cell, function: "testUserOfferSnippetLayout_" + stub.identifier)
    }
}

extension UserOfferSnippetTests {
    private static func loadStubJSON(from filename: String) -> [AnyHashable: Any]? {
        let data = ResourceProvider.jsonData(from: filename, in: Bundle(for: Self.self))
        guard let jsonResponse = try? JSONSerialization.jsonObject(with: data, options: []) as? [AnyHashable: Any] 
        else { return nil }
        return jsonResponse["response"] as? [AnyHashable: Any]
    }
}

extension UserOfferSnippetTests {
    private enum Stub {
        enum Moderation: CaseIterable {
            case free
            case paid
            case unpaid
            case fromFeed
        }

        case moderation(Moderation)

        var filename: String {
            switch self {
                case .moderation(.free): return "userOffersCard-moderation-free.debug"
                case .moderation(.paid): return "userOffersCard-moderation-paid.debug"
                case .moderation(.unpaid): return "userOffersCard-moderation-unpaid.debug"
                case .moderation(.fromFeed): return "userOffersCard-feedOffer-moderation.debug"
            }
        }

        var identifier: String {
            switch self {
                case .moderation(.free): return "moderation_free"
                case .moderation(.paid): return "moderation_paid"
                case .moderation(.unpaid): return "moderation_unpaid"
                case .moderation(.fromFeed): return "moderation_fromFeed"
            }
        }
    }
}
