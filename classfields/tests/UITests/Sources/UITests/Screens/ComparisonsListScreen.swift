import XCTest
import Snapshots

final class ComparisonsListScreen: BaseScreen {
    lazy var collectionView = findAll(.collectionView).firstMatch

    lazy var refreshingControl = self.collectionView.activityIndicators.firstMatch

    lazy var loginButton = find(by: "Войти").firstMatch
    lazy var openListingButton = find(by: "К объявлениям").firstMatch
    lazy var addOfferToComparisonButton = find(by: "Добавить").firstMatch

    lazy var offersComparison = collectionView.cells["offers"].firstMatch
    lazy var emptyComparison = collectionView.cells["offers_empty"].firstMatch
    lazy var offersFromFavoritesComparison = collectionView.cells["offers_favorites_empty"].firstMatch
}
