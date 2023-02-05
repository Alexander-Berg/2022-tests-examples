import MarketAPI
import MarketDTO
import MarketKeychain
import MarketModels
import MarketProtocols
import PromiseKit
import XCTest

@testable import BeruServices

class AntifraudServiceTests: XCTestCase {

    // MARK: - Tests

    func test_obtainJWS_whenFreshJWSFromKeychain() throws {
        // given
        let api = StubAntiraudAPI()
        let keychain = StubAntiraudKeychain(jwsToObtain: Constants.Future.rawJWS)
        let deviceCheckService = StubDeviceCheckService()

        let antifraudService = AntifraudServiceImpl(
            antifraudApi: api,
            antifraudKeychain: keychain,
            deviceCheckService: deviceCheckService
        )

        // when
        let result = antifraudService.obtainJWS().expect(in: self)

        // then
        let jws = try XCTUnwrap(result.get())

        let expectedJWS = JWS(
            expirationDate: Date(timeIntervalSince1970: Constants.Future.timestamp),
            rawRepresentation: Constants.Future.rawJWS
        )
        XCTAssertEqual(jws, expectedJWS)
        XCTAssertNil(keychain.jwsThatWasStored)
        XCTAssertTrue(keychain.jwsWasObtained)
        XCTAssertFalse(api.jwsWasObtained)
        XCTAssertFalse(deviceCheckService.tokenWasGenerated)
    }

    func test_obtainJWS_whenExpiredJWSFromKeychain() throws {
        // given
        let api = StubAntiraudAPI(jwsToObtain: Constants.Future.rawJWS)
        let keychain = StubAntiraudKeychain(jwsToObtain: Constants.Past.rawJWS)
        let deviceCheckService = StubDeviceCheckService(tokenToGenerate: "stub_token")

        let antifraudService = AntifraudServiceImpl(
            antifraudApi: api,
            antifraudKeychain: keychain,
            deviceCheckService: deviceCheckService
        )

        // when
        let result = antifraudService.obtainJWS().expect(in: self)

        // then
        let jws = try XCTUnwrap(result.get())

        let expectedJWS = JWS(
            expirationDate: Date(timeIntervalSince1970: Constants.Future.timestamp),
            rawRepresentation: Constants.Future.rawJWS
        )
        XCTAssertEqual(jws, expectedJWS)
        XCTAssertTrue(keychain.jwsWasObtained)
        XCTAssertEqual(keychain.jwsThatWasStored, Constants.Future.rawJWS)
        XCTAssertTrue(api.jwsWasObtained)
        XCTAssertTrue(deviceCheckService.tokenWasGenerated)
    }

    func test_obtainJWS_whenKeychainIsEmpty() throws {
        // given
        let api = StubAntiraudAPI(jwsToObtain: Constants.Future.rawJWS)
        let keychain = StubAntiraudKeychain()
        let deviceCheckService = StubDeviceCheckService(tokenToGenerate: "stub_token")

        let antifraudService = AntifraudServiceImpl(
            antifraudApi: api,
            antifraudKeychain: keychain,
            deviceCheckService: deviceCheckService
        )

        // when
        let result = antifraudService.obtainJWS().expect(in: self)

        // then
        let jws = try XCTUnwrap(result.get())

        let expectedJWS = JWS(
            expirationDate: Date(timeIntervalSince1970: Constants.Future.timestamp),
            rawRepresentation: Constants.Future.rawJWS
        )
        XCTAssertEqual(jws, expectedJWS)
        XCTAssertFalse(keychain.jwsWasObtained)
        XCTAssertEqual(keychain.jwsThatWasStored, Constants.Future.rawJWS)
        XCTAssertTrue(api.jwsWasObtained)
        XCTAssertTrue(deviceCheckService.tokenWasGenerated)
    }

    func test_obtainJWS_whenDeviceCheckTokenGenerateError() {
        // given
        let api = StubAntiraudAPI()
        let keychain = StubAntiraudKeychain()
        let deviceCheckService = StubDeviceCheckService()

        let antifraudService = AntifraudServiceImpl(
            antifraudApi: api,
            antifraudKeychain: keychain,
            deviceCheckService: deviceCheckService
        )

        // when
        let result = antifraudService.obtainJWS().expect(in: self)

        // then
        XCTAssertThrowsError(try result.get())
        XCTAssertFalse(keychain.jwsWasObtained)
        XCTAssertNil(keychain.jwsThatWasStored)
        XCTAssertFalse(api.jwsWasObtained)
        XCTAssertFalse(deviceCheckService.tokenWasGenerated)
    }

    func test_obtainJWS_whenAPIObtainJWSError() {
        // given
        let api = StubAntiraudAPI()
        let keychain = StubAntiraudKeychain()
        let deviceCheckService = StubDeviceCheckService(tokenToGenerate: "stub_token")

        let antifraudService = AntifraudServiceImpl(
            antifraudApi: api,
            antifraudKeychain: keychain,
            deviceCheckService: deviceCheckService
        )

        // when
        let result = antifraudService.obtainJWS().expect(in: self)

        // then
        XCTAssertThrowsError(try result.get())
        XCTAssertFalse(keychain.jwsWasObtained)
        XCTAssertNil(keychain.jwsThatWasStored)
        XCTAssertFalse(api.jwsWasObtained)
        XCTAssertTrue(deviceCheckService.tokenWasGenerated)
    }
}

// MARK: - Nested Types

private extension AntifraudServiceTests {
    enum Constants {
        // 2999-01-01T00:00:00.000Z
        enum Future {
            static let timestamp: TimeInterval = 32_472_144_000
            static let rawJWS = "eyJhbGciOiJIUzI1NiJ9"
                + ".eyJleHAiOiAzMjQ3MjE0NDAwMCwidXVpZCI6IjQ2MzhkZGFhZDIwNjRkN2Q4ODE3NTU1OWZmYjJjYjcwIn0="
                + ".LylgMAcbNC0VMTAYy5Q="
        }

        // 2020-01-01T00:00:00.000Z
        enum Past {
            static let timestamp: TimeInterval = 1_577_836_800
            static let rawJWS = "eyJhbGciOiJIUzI1NiJ9"
                + ".eyJleHAiOiAxNTc3ODM2ODAwLCJ1dWlkIjoiNDYzOGRkYWFkMjA2NGQ3ZDg4MTc1NTU5ZmZiMmNiNzAifQs=="
                + ".LylgMAcbNC0VMTAYy5Q="
        }
    }
}

// MARK: - StubAntiraudKeychain

private class StubAntiraudKeychain: AntifraudKeychain {

    // MARK: - Properties

    var jwsWasObtained = false
    var jwsThatWasStored: String?
    private let jwsToObtain: String?

    // MARK: - Lifecycle

    init(jwsToObtain: String? = nil) {
        self.jwsToObtain = jwsToObtain
    }

    // MARK: - AntifraudKeychain

    func obtainJWS() -> Promise<String> {
        if let jws = jwsToObtain {
            jwsWasObtained = true
            return .value(jws)
        } else {
            return Promise(error: MarketKeychainError.itemIsMissing)
        }
    }

    func storeJWS(_ jws: String) {
        jwsThatWasStored = jws
    }
}

// MARK: - StubAntiraudAPI

private class StubAntiraudAPI: AntifraudAPI {

    // MARK: - Properties

    var jwsWasObtained = false
    private let jwsToObtain: String?

    // MARK: - Lifecycle

    init(jwsToObtain: String? = nil) {
        self.jwsToObtain = jwsToObtain
    }

    // MARK: - AntifraudKeychain

    func obtainJWS(with params: ResolveJWSParams) -> Promise<String> {
        if let jws = jwsToObtain {
            jwsWasObtained = true
            return .value(jws)
        } else {
            return Promise(error: ServiceError.unknown())
        }
    }
}

// MARK: - StubDeviceCheckService

private class StubDeviceCheckService: DeviceCheckService {

    // MARK: - Properties

    var tokenWasGenerated = false
    private let tokenToGenerate: String?

    // MARK: - Lifecycle

    init(tokenToGenerate: String? = nil) {
        self.tokenToGenerate = tokenToGenerate
    }

    // MARK: - DeviceCheckService

    func generateDeviceToken() -> Promise<String> {
        if let token = tokenToGenerate {
            tokenWasGenerated = true
            return .value(token)
        } else {
            return Promise(error: ServiceError.unknown())
        }
    }
}
