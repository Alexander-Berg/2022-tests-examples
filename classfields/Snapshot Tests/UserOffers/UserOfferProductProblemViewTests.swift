//
//  UserOfferProductProblemViewTests.swift
//  Unit Tests
//
//  Created by Alexey Salangin on 2/2/21.
//  Copyright © 2021 Yandex. All rights reserved.
//

import XCTest
@testable import YREUserOffersModule
import YREDesignKit

final class UserOfferProductProblemViewTests: XCTestCase {
    func testViewLayout() {
        let generator = UserOfferProductProblemViewModelGenerator.self
        let pendingPurchaseInfo = UserOfferPendingPurchaseInfoGenerator.generate(
            productType: .promotion,
            productStatus: .active,
            pendingActivationCount: 300,
            pendingPaymentCount: 1000
        )

        var views = [UIView]()

        let turboViewModels = generator.generateForTurbo(
            notEnoughFunds: true,
            renewalError: .paymentRejected,
            pendingPurchaseInfo: pendingPurchaseInfo
        )

        let productViewModels = generator.generateForProduct(
            productRenewalStatus: .warning,
            turboRenewalStatus: .inProgress,
            notEnoughFunds: true,
            renewalError: .insufficientFunds,
            pendingPurchaseInfo: pendingPurchaseInfo
        )

        let turboViews = turboViewModels.map { UserOfferProductProblemView(viewModel: $0) }
        views += turboViews

        let productViews = productViewModels.map { UserOfferProductProblemView(viewModel: $0) }
        views += productViews

        let viewController = UIViewController()
        viewController.view.backgroundColor = ColorScheme.Background.primary

        views.forEach { $0.frame = viewController.view.frame }

        let stackView = UIStackView()
        stackView.axis = .vertical
        stackView.spacing = 8
        stackView.distribution = .fill
        stackView.addArrangedSubviews(views)
        viewController.view.addSubview(stackView)
        stackView.yre_edgesToSuperview(insets: .init(top: 0, left: 0, bottom: .nan, right: 0))

        self.assertSnapshot(viewController.view)
    }
}
