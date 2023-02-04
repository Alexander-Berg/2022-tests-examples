//
//  UserOfferListTests.swift
//  Unit Tests
//
//  Created by Denis Mamnitskii on 14.07.2021.
//  Copyright © 2021 Yandex. All rights reserved.
//

import XCTest
import YREDesignKit
import YREComponents
import YRECoreUtils
import YREFiltersModel
import YREModelObjc
import YREWeb
import YREServiceLayer
@testable import YREUserOffersModule

class UserOfferListTests: XCTestCase {
    func testAuthorizedEmptyState() {
        let controller = Self.makeController()
        guard let authorizedViewModel = Self.makeAuthorizedViewModel() else {
            XCTFail("Cannot generate authorized view model")
            return
        }
        controller.setAuthorizedStateWith(authorizedViewModel)
        self.assertSnapshot(controller.view)
    }

    func testNonAuthorizedState() {
        let controller = Self.makeController()
        controller.setNonAuthorizedState()
        self.assertSnapshot(controller.view)
    }

    func testBannedState() {
        let view = UserIsBannedNotificationView()
        view.frame = Self.frame { _ in UIScreen.main.bounds.height }
        self.assertSnapshot(view)
    }

    func testNothingFoundState() {
        let controller = Self.makeController()
        guard let authorizedViewModel = Self.makeAuthorizedViewModel(with: {
            $0.categoryType.setValue(UserOffersFilterAction.sell)
        }) else {
            XCTFail("Cannot generate authorized view model")
            return
        }
        controller.setAuthorizedStateWith(authorizedViewModel)
        self.assertSnapshot(controller.view)
    }

    func testPromoBanner() {
        let viewModel = PromoBannerContentViewModel(title: "Включить сообщения",
                                                    subtitle: "Ни один покупатель не ускользнёт от вас",
                                                    actionButtonTitle: "Настройки",
                                                    hideButtonTitle: "Скрыть",
                                                    initialSwitchValue: false)
        let view = PromoBannerView(viewModel: viewModel)

        let size = view.systemLayoutSizeFitting(
            UIScreen.main.bounds.size,
            withHorizontalFittingPriority: .required,
            verticalFittingPriority: .defaultLow
        )
        view.frame = CGRect(origin: .zero, size: size)

        self.assertSnapshot(view)
    }
}

extension UserOfferListTests {
    static private func makeController() -> YREUserOffersListViewController {
        let controller = YREUserOffersListViewController()
        controller.forceDisplayScreenLoading = false
        return controller
    }

    static private func makeAuthorizedViewModel(
        with filterTransformer: (YREUserOffersFilter) -> Void = { _ in }
    ) -> UserOffersListAuthorizedStateViewModel? {
        let filterParameterFactory = YREUserOffersFilterParameterFactory()
        let filter = YREUserOffersFilter(parameterFactory: filterParameterFactory)
        filterTransformer(filter)
        let filterState = filter.stateSerializer.state(from: filter)
        let search = YREMutableUserOffersSearch(filterSnapshot: filterState, sortType: .withIssuesFirst)

        return UserOffersListAuthorizedStateViewModelGenerator.generate(
            userType: .unspecified,
            shouldShowUserChatsPromo: false,
            userChatsPromoSwitchValue: false,
            search: search
        )
    }
}
