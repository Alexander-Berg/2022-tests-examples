import XCTest
import Foundation
import AutoRuProtoModels

final class UserReviewsEditorTests: BaseTest {
    private let deeplink = "https://auto.ru/my/reviews/"
    private let reviewId = "4943020671716068231"

    override func setUp() {
        super.setUp()
        setupServer()
    }

    private func setupServer() {
        mocker
            .mock_base()
            .mock_user()
            .setForceLoginMode(.forceLoggedIn)
    }

    func testTraverseWizard() {
        mocker
            .mock_getEmptyUserReviews()
            .mock_makeUserReview()
            .mock_wizardReferenceCatalogCarsSuggest()
            .mock_getUsefulFeatures()
            .mock_getInitialUserReview()
            .mock_putUserReviewErrors()
            .startMock()

        let requestPutWasCalled = api.reviews.subject(.auto).reviewId(reviewId).put.expect { request, _ in
            let isCorrectBody: Bool = {
                request.pro.contains("Проходимость") &&
                request.pro.contains("Престиж") &&
                request.contra.contains("Материалы") &&
                !request.contra.contains("Безопасность") &&
                request.tags.first?.value == "5"
            }()

            return isCorrectBody ? .ok : .fail(reason: "Ошибка отправки параметра в визарде")
        }

        launch(on: .userReviewsScreen,
               options: AppLaunchOptions(launchType: .deeplink(deeplink)))
            .tap(.addReviewBigButton)
            .should(provider: .categoryPicker, .exist)
            .focus { picker in
                picker.tap(.auto)
            }
            .should(provider: .wizardMarkPicker, .exist)
            .focus { picker in
                picker.tap(.wizardItem("BMW"))
            }
            .should(provider: .wizardModelPicker, .exist)
            .focus { picker in
                picker.tap(.wizardItem("6 серии"))
            }
            .should(provider: .wizardYearPicker, .exist)
            .focus { picker in
                picker.tap(.wizardItem("2020"))
            }
            .should(provider: .wizardGenerationPicker, .exist)
            .focus { picker in
                picker.tap(.wizardItem("с 2020 по 2020, IV (G32) Рестайлинг"))
            }
            .should(provider: .wizardBodyTypePicker, .exist)
            .focus { picker in
                picker.tap(.wizardItem("Лифтбек"))
            }
            .should(provider: .wizardEngineTypePicker, .exist)
            .focus { picker in
                picker.tap(.wizardItem("Бензин"))
            }
            .should(provider: .wizardDriveTypePicker, .exist)
            .focus { picker in
                picker.tap(.wizardItem("Полный"))
            }
            .should(provider: .wizardTransmissionTypePicker, .exist)
            .focus { picker in
                picker.tap(.wizardItem("Автоматическая"))
            }
            .should(provider: .wizardModificationPicker, .exist)
            .focus { picker in
                picker.tap(.wizardItem("640i xDrive / 3.0 / AT / Бензин / 333 л.с."))
            }
            .should(provider: .wizardOwningTimePicker, .exist)
            .focus { picker in
                picker.tap(.wizardItem("6-12 месяцев"))
            }
            .should(provider: .wizardPlusesPicker, .exist)
            .focus { picker in
                picker
                    .wait(for: 2)
                    .tap(.more)
                    .tap(.offRoad)
                    .type("Престиж")
                    .wait(for: 1)
                    .tap(.nextButton)
            }
            .should(provider: .wizardMinusesPicker, .exist)
            .focus { picker in
                picker
                    .tap(.safety)
                    .type("Материалы")
                    .wait(for: 1)
                    .tap(.safety)
                    .tap(.nextButton)
            }
            .should(provider: .wizardAppearancePicker, .exist)
            .focus { picker in
                picker
                    .tap(.starButton)
                    .wait(for: 1)
            }
            .should(provider: .wizardSafetyPicker, .exist)
            .focus { picker in
                picker
                    .tap(.starButton)
                    .wait(for: 1)
            }
            .should(provider: .wizardComfortPicker, .exist)
            .focus { picker in
                picker
                    .tap(.starButton)
                    .wait(for: 1)
            }
            .should(provider: .wizardReliabilityPicker, .exist)
            .focus { picker in
                picker
                    .tap(.starButton)
                    .wait(for: 1)
            }
            .should(provider: .wizardDriveabilityPicker, .exist)
            .focus { picker in
                picker
                    .tap(.starButton)
                    .wait(for: 1)
            }
            .should(provider: .userReviewEditorScreen, .exist)
            .focus { screen in
                screen
                    .tap(.saveButton)
            }
            .wait(for: [requestPutWasCalled])
    }

    func testCloseWizard() {
        mocker
            .mock_getEmptyUserReviews()
            .mock_makeUserReview()
            .mock_wizardReferenceCatalogCarsSuggest()
            .startMock()

        launch(on: .userReviewsScreen,
               options: AppLaunchOptions(launchType: .deeplink(deeplink)))
            .tap(.addReviewBigButton)
            .should(provider: .categoryPicker, .exist)
            .focus { picker in
                picker.tap(.auto)
            }
            .should(provider: .wizardReviewScreen, .exist)
            .focus { container in
                container.tap(.closeButton)
            }
            .should(provider: .userReviewCloseAlert, .exist)
            .focus { alert in
                alert.tap(.agree)
            }
            .should(provider: .userReviewsScreen, .exist)
    }

    func testPublishWithErrors() {
        mocker
            .mock_getUserReviews()
            .mock_getUserReview()
            .mock_putUserReviewErrors()
            .startMock()

        let requestPutWasCalled = api.reviews.subject(.auto).reviewId(reviewId).put.expect { request, _ in
            request.status == .enabled ? .ok : .fail(reason: "В запросе неправильный статус")
        }

        launch(on: .userReviewsScreen,
               options: AppLaunchOptions(launchType: .deeplink("https://auto.ru/my/reviews/")))
            .tap(.editReviewButton)
            .should(provider: .userReviewEditorScreen, .exist)
            .focus { screen in
                screen
                    .tap(.publishButton)
            }
            .should(provider: .userReviewErrorAlert, .exist)
            .wait(for: [requestPutWasCalled])
    }

    func testPublishWithOk() {
        mocker
            .mock_getUserReviews()
            .mock_getUserReview()
            .mock_putUserReviewOk()
            .startMock()
        
        let requestPutWasCalled = api.reviews.subject(.auto).reviewId(reviewId).put.expect { request, _ in
            request.status == .enabled ? .ok : .fail(reason: "В запросе неправильный статус")
        }

        launch(on: .userReviewsScreen,
               options: AppLaunchOptions(launchType: .deeplink("https://auto.ru/my/reviews/")))
            .tap(.editReviewButton)
            .should(provider: .userReviewEditorScreen, .exist)
            .focus { screen in
                screen
                    .tap(.publishButton)
            }
            .wait(for: [requestPutWasCalled])
    }

    func testCloseEditor() {
        mocker
            .mock_getUserReviews()
            .mock_getUserReview()
            .startMock()

        launch(on: .userReviewsScreen,
               options: AppLaunchOptions(launchType: .deeplink("https://auto.ru/my/reviews/")))
            .tap(.editReviewButton)
            .should(provider: .userReviewEditorScreen, .exist)
            .focus { screen in
                screen
                    .tap(.closeButton)
            }
            .should(provider: .userReviewCloseAlert, .exist)
            .focus { alert in
                alert.tap(.agree)
            }
            .should(provider: .userReviewsScreen, .exist)
    }

    func testSaveButtonIsHidden() {
        mocker
            .mock_getUserReviews()
            .mock_getPublishedUserReview()
            .startMock()

        launch(on: .userReviewsScreen,
               options: AppLaunchOptions(launchType: .deeplink("https://auto.ru/my/reviews/")))
            .tap(.editReviewButton)
            .should(provider: .userReviewEditorScreen, .exist)
            .focus { screen in
                screen
                    .should(.saveButton, .be(.hidden))
            }
    }
}
