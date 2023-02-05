import MarketUITestMocks
import XCTest

final class SKUCardModelComplaintOpinionsTest: LocalMockTestCase {

    func testComplainOpinionCardModel() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-4162")
        Allure.addEpic("КМ")
        Allure.addFeature("Отзывы")
        Allure.addTitle("Проверяем корректность работы попапа жалоб на отзывы в SKU")

        app.launchEnvironment[TestLaunchEnvironmentKeys.waitForExperiments] = String(true)

        var sku: SKUPage!
        let expectedToastText = "Cпасибо! Мы проверим отзыв и удалим его при необходимости"

        var skuState = SKUInfoState()

        "Настраиваем стейт".ybm_run { _ in
            skuState.setSkuInfoState(with: .default)
            stateManager?.setState(newState: skuState)
        }

        "Открываем SKU с отзывами".ybm_run { _ in
            sku = goToDefaultSKUPage()
        }

        "Раскрываем блок отзывов".ybm_run { _ in
            sku.element.ybm_swipeCollectionView(toFullyReveal: sku.opinionsHeader)
            sku.opinionsHeader.tap()
        }

        "Мокаем состояние".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "SKUCardModel_OpinionComplaint")
        }

        for type in ComplaintType.allCases {

            "Открываем меню с жалобой".ybm_run { _ in
                sku.element.ybm_swipeCollectionView(
                    toFullyReveal: sku.openContextMenuButton,
                    inset: sku.stickyViewInset
                )
                sku.openContextMenuButton.tap()
            }

            "Открываем попап жалобы".ybm_run { _ in
                wait(forExistanceOf: ContextMenuPopupPage.currentPopup.element)

                let contextMenuPopup = ContextMenuPopupPage.currentPopup
                contextMenuPopup.complain.tap()
            }

            complainOpinionOn(page: sku, withComplaintType: type)

            "Проверяем текст тоста".ybm_run { _ in
                let toast = NotifyPopupPage.currentPopup
                XCTAssertEqual(toast.text.label, expectedToastText)
            }
        }
    }

    func testComplainOpinionAllOpinions() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-4163")
        Allure.addEpic("КМ")
        Allure.addFeature("Отзывы")
        Allure.addTitle("Проверяем корректность работы попапа жалоб на отзывы на странице всех Отзывов")

        app.launchEnvironment[TestLaunchEnvironmentKeys.waitForExperiments] = String(true)

        var sku: SKUPage!
        var opinions: OpinionsPage!
        let expectedToastText = "Cпасибо! Мы проверим отзыв и удалим его при необходимости"

        var skuState = SKUInfoState()

        "Настраиваем стейт".ybm_run { _ in
            skuState.setSkuInfoState(with: .default)
            stateManager?.setState(newState: skuState)
        }

        "Открываем SKU".ybm_run { _ in
            sku = goToDefaultSKUPage()
        }

        "Переходим в отзывы".ybm_run { _ in
            sku.element.ybm_swipeCollectionView(
                toFullyReveal: sku.opinionsFastLink.element,
                inset: sku.stickyViewInset
            )
            opinions = sku.opinionsFastLink.tap()
            wait(forExistanceOf: opinions.element)
        }

        "Мокаем состояние".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "SKUCardModel_OpinionComplaint")
        }

        for type in ComplaintType.allCases {

            "Открываем меню с жалобой".ybm_run { _ in
                opinions.openContextMenuButton.tap()
            }

            "Открываем попап жалобы".ybm_run { _ in
                let contextMenuPopup = ContextMenuPopupPage.currentPopup
                contextMenuPopup.complain.tap()
            }

            complainOpinionOn(page: opinions, withComplaintType: type)

            "Проверяем текст тоста".ybm_run { _ in
                let toast = NotifyPopupPage.currentPopup
                XCTAssertEqual(toast.text.label, expectedToastText)
            }
        }
    }

    func testComplainOpinionIndividualOpinion() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-4164")
        Allure.addEpic("КМ")
        Allure.addFeature("Отзывы")
        Allure.addTitle("Проверяем корректность работы попапа жалоб на отзывы на странице отдельного отзыва")

        app.launchEnvironment[TestLaunchEnvironmentKeys.waitForExperiments] = String(true)

        var sku: SKUPage!
        var opinionDetails: OpinionDetailsPage!
        let expectedToastText = "Cпасибо! Мы проверим отзыв и удалим его при необходимости"

        "Мокаем SKU c отзывами, у которых есть комментарии".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "SKUCardModel_ComplainCommentOpinion")
        }

        "Настраиваем стейт".ybm_run { _ in
            var skuState = SKUInfoState()
            skuState.setSkuInfoState(with: .default)
            stateManager?.setState(newState: skuState)
        }

        "Открываем SKU".ybm_run { _ in
            sku = goToDefaultSKUPage()
        }

        "Раскрываем блок отзывов и переходим на страницу отдельного отзыва".ybm_run { _ in
            sku.collectionView.ybm_swipeCollectionView(toFullyReveal: sku.showOpinonButton)
            sku.showOpinonButton.tap()
            opinionDetails = OpinionDetailsPage.current
        }

        "Мокаем состояние".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "SKUCardModel_OpinionComplaint")
        }

        for type in ComplaintType.allCases {

            "Открываем меню с жалобой".ybm_run { _ in
                opinionDetails.openContextMenuButton.tap()
            }

            "Открываем попап жалобы".ybm_run { _ in
                let contextMenuPopup = ContextMenuPopupPage.currentPopup
                contextMenuPopup.complain.tap()
            }

            complainOpinionOn(page: opinionDetails, withComplaintType: type)

            "Проверяем текст тоста".ybm_run { _ in
                let toast = NotifyPopupPage.currentPopup
                XCTAssertEqual(toast.text.label, expectedToastText)
            }
        }
    }

    func testComplainOpinionComment() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-4303")
        Allure.addEpic("КМ")
        Allure.addFeature("Отзывы")
        Allure.addTitle("Проверяем корректность работы попапа жалоб на комментарий к отзывам")

        var sku: SKUPage!
        var opinionDetails: OpinionDetailsPage!
        let expectedToastText = "Cпасибо! Мы проверим комментарий и удалим его при необходимости"

        "Мокаем состояние".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "SKUCardModel_ComplainCommentOpinion")
        }

        "Настраиваем стейт".ybm_run { _ in
            var skuState = SKUInfoState()
            skuState.setSkuInfoState(with: .default)
            stateManager?.setState(newState: skuState)
        }

        "Открываем SKU".ybm_run { _ in
            sku = goToDefaultSKUPage()
        }

        "Раскрываем блок отзывов и переходим на страницу отдельного отзыва".ybm_run { _ in
            sku.element.swipe(to: .down, until: sku.showOpinonButton.isVisible)
            sku.showOpinonButton.tap()
            opinionDetails = OpinionDetailsPage.current
        }

        for type in ComplaintType.allCases {
            wait(forVisibilityOf: opinionDetails.element)

            "Открываем меню с жалобой".ybm_run { _ in
                opinionDetails.openContextMenuButtonInComments.tap()
            }

            "Открываем попап жалобы".ybm_run { _ in
                wait(forVisibilityOf: ContextMenuPopupPage.currentPopup.element)

                let contextMenuPopup = ContextMenuPopupPage.currentPopup
                contextMenuPopup.complain.tap()
            }

            complainOpinionOn(page: opinionDetails, withComplaintType: type)

            "Проверяем текст тоста".ybm_run { _ in
                let toast = NotifyPopupPage.currentPopup
                XCTAssertEqual(toast.text.label, expectedToastText)
            }
        }
    }

    // MARK: - Private

    private func complainOpinionOn<Page: CollectionViewPage>(page: Page, withComplaintType type: ComplaintType) {
        switch type {
        case .spam:
            "Жалуемся на спам".ybm_run { _ in
                let complaintPopup = ComplaintPopupPage.currentPopup
                wait(forVisibilityOf: complaintPopup.collectionView)

                complaintPopup.spam.tap()
                complaintPopup.complainButton.tap()
                wait(forVisibilityOf: page.collectionView)
            }
        case .offensiveContent:
            "Жалуемся на оскорбительное высказываение".ybm_run { _ in
                let complaintPopup = ComplaintPopupPage.currentPopup
                wait(forVisibilityOf: complaintPopup.collectionView)

                complaintPopup.offensiveContent.tap()
                complaintPopup.complainButton.tap()
                wait(forVisibilityOf: page.collectionView)
            }
        case .other:
            "Выбираем другое".ybm_run { _ in
                let complaintPopup = ComplaintPopupPage.currentPopup
                wait(forVisibilityOf: complaintPopup.collectionView)
                complaintPopup.other.tap()
            }

            "Пишем причину и отправляем жалобу".ybm_run { _ in
                let formPage = QnAFormPage.current
                formPage.inputTextView.element.typeText("тупой отзыв")
                formPage.submitButton.tap()
                wait(forVisibilityOf: page.collectionView)
            }
        }
    }

}

// MARK: - Nested Types

private extension SKUCardModelComplaintOpinionsTest {

    enum ComplaintType: CaseIterable {
        case spam
        case offensiveContent
        case other
    }
}
