import XCTest
import MedicineModels
@testable import MedicineCore

final class TelemedDoctorTests: XCTestCase {

    var store: EnvironmentStore!

    var state: AppState {
        self.store.store.state
    }

    override func setUp() {
        super.setUp()
        self.store = makeEnvStore(middleware: [
            TaxonomiesMiddleware(taxonomiesRepo: MockTaxonomiesRepo()).execute(_:_:_:)
        ])
    }

    override func tearDown() {
        super.tearDown()
    }

    func testLoadTaxonomyContracts() {
        let taxonomyID = Taxonomy.ID(value: "25")
        
        store.graph.taxonomies.loadContracts(for: taxonomyID)
        wait(for: 0.2)
        
        XCTAssertEqual(state.taxonomyState.contractsById.count, 1) // loaded just one taxonomy id
        
        let taxonomyNode = store.graph.taxonomies.taxonomy(by: taxonomyID)
        let contractsCount = taxonomyNode.doctors.value!.count
        XCTAssertGreaterThan(contractsCount, 0)
        XCTAssertEqual(contractsCount, state.taxonomyDoctorState.byId.count)
        
        XCTAssertNotNil(taxonomyNode.doctors.value?.first?.clinicGroup)
    }
    
    func testStateLoadContractDetail() {
//        let elementID: PlatformContractPolling.ID = .init(value: "81294")
//
//        let uuid = UUID()
//        store.store.dispatch(LoadPlatformContractAction.start(id: elementID))
//
//        wait(for: 1)
//        XCTAssertGreaterThan(state.taxonomyState.contractsById.count, 0)
//        XCTAssertEqual(state.taxonomyState.contractsById.count, state.platformDoctorsState.byId.count)
//        store.store.dispatch(LoadPlatformContractAction.started(id: elementID, requestID: uuid))
//
//        var detailContractById: Loadable<PlatformContract> = state.platformContractPollingState.detailContractById(elementID)
//        XCTAssertEqual(detailContractById.isLoading, Loadable<PlatformContract>.loading(requestID: uuid).isLoading)
//
//        let parsed = ResponseFromFileLoader.load(.contractDetail)
//        store.store.dispatch(LoadPlatformContractAction.finished(id: elementID, requestID: uuid, response: parsed))
//
//        let contract = parsed.data.first!.asPlatformContract!
//
//        detailContractById = state.platformContractPollingState.detailContractById(elementID)
//        XCTAssertEqual(detailContractById.value?.id, Loadable<PlatformContract>.loaded(contract).value?.id)
//        XCTAssertEqual(detailContractById.value?.attr.price, Loadable<PlatformContract>.loaded(contract).value?.attr.price)
//
//        XCTAssertNotNil(state.telemedDoctorState.byId[contract.doctor])
//        let doctor: TelemedDoctor = state.telemedDoctorState.byId[contract.doctor]!
//        XCTAssertEqual(doctor.id, contract.doctor)
//
//        XCTAssertEqual(doctor.clinicGroup, contract.clinicGroup)
//        XCTAssertNotNil(state.clinicGroupState.byId[doctor.clinicGroup])
    }
}

extension XCTestCase {

    func wait(for duration: TimeInterval) {
        let waitExpectation = expectation(description: "Waiting")

        let when = DispatchTime.now() + duration
        DispatchQueue.main.asyncAfter(deadline: when) {
            waitExpectation.fulfill()
        }

        waitForExpectations(timeout: duration + 0.5)
    }
}


class MockTaxonomiesRepo: TaxonomiesRepo {
    func fetchDoctors(taxonomyID: Taxonomy.ID, completion: @escaping (Result<DataIncludedResponse, Error>) -> Void) {
        DispatchQueue.main.async {
            completion(.success(ResponseFromFileLoader.load(.taxonomyDoctors)))
        }
    }
    
    func fetchTaxonomyDoctors(taxonomyID: Taxonomy.ID, completion: @escaping (Result<TaxonomyDoctorsResponse, Error>) -> Void) {
//        ResponseFromFileLoader.load(.taxonomyDoctors)
    }
    
    func telemedServices(taxonomyID: Taxonomy.ID, completion: @escaping (Result<DataIncludedResponse, Error>) -> Void) {
        
    }
}
