//
//  OfferCardRootNodeTests.swift
//  Unit Tests
//
//  Created by Arkady Smirnov on 1/25/21.
//  Copyright © 2021 Yandex. All rights reserved.
//

import XCTest
import AsyncDisplayKit
import YREModel
import YREModelObjc
import YRESettings
import YREDesignKit
@testable import YREOfferCardModule
@testable import YRESnippets
@testable import YRECardComponents

final class OfferCardRootNodeTests: XCTestCase {
    // MosRu panel is not tested here due to bugs in layout
    // isFullTrustedOwner is changed to false in json files
    func testOfferCardRootNodeLayout() {
        OfferCardStub.allRootNodeCases.forEach { stub in
            self.testOfferCardRootNodeLayout(for: stub)
        }
    }

    private func testOfferCardRootNodeLayout(for stub: OfferCardStub) {
        guard let offer = stub.loadOffer() else {
            XCTFail("Couldn't load offer from stub.")
            return
        }
        
//        Commented 'cause something wrong with files that has height more than 8000.

//        let districtBlock: DistrictBlock?
//        if let location = offer.location {
//            districtBlock = DistrictBlockModelGenerator.model(location: location, showSchools: true)
//        }
//        else {
//            districtBlock = nil
//        }
//
//        let districtBlockViewModel = DistrictBlockViewModelGenerator.viewModelForDistrictBlock(
//            districtBlock,
//            districtBlockItemTapped: { _ in }
//        )

        let viewModel = OfferCardRootViewModelGenerator.makeViewModel(
            offer: offer,
            bigMapRegion: nil,
            geoRoutingViewModel: nil,
            districtViewModel: nil,
            archiveViewModel: nil,
            isUserOffer: false,
            isUserAuthorized: true,
            userID: nil,
            inFavorites: true
        )

        let node = OfferCardRootNode(
            viewModel: viewModel,
            staticMapViewProvider: { staticMapModuleFactory.makeStaticMapModule().view }
        )

        viewModel.cardBlocks?.forEach({ (cardBlock) in
            switch cardBlock {
                case .alfaBankMortgage:
                    // Commented 'cause something wrong with files that has height more than 8000.
                    // node.updateAlfaBankMortgageViewModel(viewModel: Self.alfaBankMortgageNodeModel)
                    break

                case .archive:
                    // Commented 'cause something wrong with files that has height more than 8000.
                    // node.updateArchiveViewModel(Self.archiveViewModel)
                    break

                case .excerpt:
                    // Commented 'cause something wrong with files that has height more than 8000.
                    // node.updateExcerptViewModel(ExcerptBlockViewModelGenerator.makePromo())
                    break

                case .manual:
                    break
//                 @arkadysmirnov: Commented 'cause something wrong with files that has height more than 8000.
//                 node.updateManualViewModel(viewModel: Self.manualNodeModel)

                case .similarOffers:
                    break
//                 @arkadysmirnov: Commented 'cause something wrong with files that has height more than 8000.
//                 node.updateSimilarOfferListViewModel(viewModel: Self.similatOfferList)

                case .purchaseExcerpt:
                    // Commented 'cause something wrong with files that has height more than 8000.
                    // node.updatePurchaseExcerptViewModel(Self.purchaseExcerptViewModel)
                    break

                case .paidExcerpts:
                    // Commented 'cause something wrong with files that has height more than 8000.
                    // node.updatePaidExcerptViewModel(Self.paidExcerptViewModel)
                    break
                    
                case .price, .highlights, .newSaleInfo, .location, .aboutApartment, .aboutFlat, .aboutNewFlat, .aboutHouse, .aboutObject,
                     .siteInfo, .lot, .aboutBuilding, .aboutDistrict, .description, .comfort, .equipment, .aboutComplex, .dealInfo,
                     .author, .serviceInfo, .buttons, .trustedOwner, .outdated,
                     .yandexRentPromo, .yandexRentPromoBanner, .userNote, .siteCallback, .communicationButtons, .documents:
                    break
            }
        })

        // @arkadysmirnov: Hack to layout node
        node.recursivelyEnsureDisplaySynchronously(true)

        self.assertSnapshot(node, function: "testOfferCardRootNodeLayout_" + stub.rawValue)
    }

    private static let purchaseExcerptViewModel: PurchaseExcerptBlockNode.ViewModel? = {
        let paidExcerpt = PaidExcerpt(id: "String", date: Date(timeIntervalSince1970: 0))
        let paidExcerptsInfo = PaidExcerptsInfo(
            reports: [paidExcerpt],
            purchasePrice: .init(base: 1000, effective: 900, modifiers: [.discount(percent: 10, amount: 100)])
        )
        return PurchaseExcerptBlockViewModelGenerator.viewModel(from: paidExcerptsInfo)
    }()

    private static let paidExcerptViewModel: PaidExcerptBlockNode.ViewModel? = {
        let paidExcerpt = PaidExcerpt(id: "String", date: Date(timeIntervalSince1970: 0))
        return PaidExcerptBlockViewModelGenerator.makePaidExcerptBlockNodeViewModel(from: [paidExcerpt])
    }()

    private static let manualNodeModel: ManualNodeModel? = {
        guard let url = URL(string: "/") else { return nil }

        let imageContent = BlogPost.ImageContent(sizeName: "orig", url: url)
        let blogPost = BlogPost(title: "Статья", url: url, titleImage: [imageContent])
        let items = ManualViewModelGenerator.makeItems(blogPosts: [blogPost])
        return ManualNodeModel(items: items)
    }()

    private static let archiveViewModel: OfferCardArchiveViewModel = {
        return OfferCardArchiveViewModel(
            title: "title",
            state: .noItems(text: "По этому адресу нет истории объявлений")
        )
    }()
    
    private static let similatOfferList: OfferCardSimilarOfferListViewModel = {
        let snippetViewModel = OfferSnippetViewModel(
            placeholderImage: nil,
            previewImages: nil,
            imageURLs: nil,
            flatPlanURLs: nil,
            floorPlanURLs: nil,
            youtubeVideo: nil,
            virtualTour: nil,
            selectedImageIndex: 0,
            isCallButtonHidden: false,
            isCallButtonEnabled: true,
            isWriteButtonHidden: true,
            callButtonNormalTitle: "Позвонить",
            callButtonDisabledTitle: "Позвонить",
            writeButtonTitle: "Написать",
            isFavoritesButtonHidden: false,
            isFavoritesButtonSelected: false,
            additionalActions: [],
            isRequestingPhones: false,
            userNote: nil)
        let style = OfferSnippetView.Style.make(isViewed: false)
        let item = OfferSnippetListViewModel.ItemViewModel(viewModel: snippetViewModel, style: style)
        let model = OfferSnippetListViewModel(items: [item])
        let state = OfferCardSimilarOfferListViewModel.State.success(model)
        return OfferCardSimilarOfferListViewModel(state: state)
    }()

    private static let alfaBankMortgageNodeModel: AlfaBankMortgageNodeModel = {
        let formInput = MortgageFormInput(cost: 100000, downpayment: 1000, period: 12, supportMortgage: true)
        let errorItem = AlfaBankMortgageViewModel.TopInfo.Item(title: "error title", value: "some value", kind: .error)
        let topInfo = AlfaBankMortgageViewModel.TopInfo.failure(errorItem: errorItem)

        let alfaBankViewModel = AlfaBankMortgageView.ViewModel(
            titleText: "Ипотека на спецусловиях",
            submitButtonText: "Получить предложение",
            infoHeader: .init(
                imageAsset: Asset.Images.Site.alphaBankLogo,
                title: "Специальные условия для пользователей Яндекс.Недвижимости"
            ),
            topInfo: topInfo,
            calculatorParams: nil,
            formInput: formInput
        )

        let alfaBankMortgageNodeModel = AlfaBankMortgageNodeModel(viewModel: alfaBankViewModel) { _ in return alfaBankViewModel }

        return alfaBankMortgageNodeModel
    }()
}
