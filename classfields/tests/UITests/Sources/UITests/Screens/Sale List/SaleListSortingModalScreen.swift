import XCTest
import Snapshots

class SaleListSortingModalScreen: ModalScreen {
    lazy var byProvenOwner = find(by: "Сначала от собственников").firstMatch
}
