//
//  UserOfferPhotoTests.swift
//  Unit Tests
//
//  Created by Alexey Salangin on 1/13/21.
//  Copyright © 2021 Yandex. All rights reserved.
//

import Foundation

import XCTest
@testable import YREUserOfferPhotoModule
import YRECoreUtils

final class UserOfferPhotoTests: XCTestCase {
    func testPhotoUploadingInProgressLayout() {
        let viewController = UserOfferPhotoUploadingViewController()
        let progress = Progress(totalUnitCount: 100)
        progress.completedUnitCount = 40

        viewController.configure(for: .inProgress(progress))
        let navigationController = UINavigationController(rootViewController: viewController)

        let expectation = self.expectation(description: "Wait for the progress update.")
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.1) {
            expectation.fulfill()
        }

        self.wait(for: [expectation], timeout: 1.0)
        self.assertSnapshot(navigationController.view)
    }

    func testPhotoUploadingSucceedLayout() {
        let viewController = UserOfferPhotoUploadingViewController()
        viewController.configure(for: .succeed)
        let navigationController = UINavigationController(rootViewController: viewController)
        self.assertSnapshot(navigationController.view)
    }

    func testPhotoUploadingFailureLayout() {
        let viewController = UserOfferPhotoUploadingViewController()
        viewController.configure(for: .failed)
        let navigationController = UINavigationController(rootViewController: viewController)
        self.assertSnapshot(navigationController.view)
    }

    func testPhotoUpdatingIdleLayout() {
        let viewController = UserOfferPhotoUpdatingViewController()
        viewController.viewState = .idle
        let navigationController = UINavigationController(rootViewController: viewController)
        self.assertSnapshot(navigationController.view)
    }

// I didn't find a way to fix the position of ActivityIndicatorView.
//    func testPhotoUpdatingLayout() {
//        let viewController = UserOfferPhotoUpdatingViewController()
//        viewController.viewState = .updating
//        let navigationController = UINavigationController(rootViewController: viewController)
//        self.assertSnapshot(navigationController.view)
//    }

    func testPhotoUpdatingDoneLayout() {
        let viewController = UserOfferPhotoUpdatingViewController()
        viewController.viewState = .done
        let navigationController = UINavigationController(rootViewController: viewController)
        self.assertSnapshot(navigationController.view)
    }

    func testPhotoUpdatingErrorUploadingLayout() {
        let viewController = UserOfferPhotoUpdatingViewController()
        viewController.viewState = .errorUploading
        let navigationController = UINavigationController(rootViewController: viewController)
        self.assertSnapshot(navigationController.view)
    }

    func testPhotoUpdatingErrorUpdatingLayout() {
        let viewController = UserOfferPhotoUpdatingViewController()
        viewController.viewState = .errorUpdating
        let navigationController = UINavigationController(rootViewController: viewController)
        self.assertSnapshot(navigationController.view)
    }

    func testPhotoUpdatingUploadingLayout() {
        let viewController = UserOfferPhotoUpdatingViewController()
        let progress = Progress(totalUnitCount: 100)
        progress.completedUnitCount = 40

        viewController.viewState = .uploading(progress)
        let navigationController = UINavigationController(rootViewController: viewController)

        let expectation = self.expectation(description: "Wait for the progress update.")
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.1) {
            expectation.fulfill()
        }

        self.wait(for: [expectation], timeout: 1.0)
        self.assertSnapshot(navigationController.view)
    }
}
