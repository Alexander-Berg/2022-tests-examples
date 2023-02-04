@testable import AutoRuNetwork
import AutoRuProtoModels
import AutoRuUtils
import AutoRuNetworkUtils
import XCTest

final class APIMethodWithTamperTests: BaseUnitTest {
    static let params = APIMethodParams(
        uuid: "g5cc1b3c24cd9lgo13k0noqhpd09qqdf.5509d34a817b8f2b2fc2a2052600bffb",
        sid: "",
        jwt: nil,
        serverFeatures: "",
        location: nil,
        persistentHeaders: [:],
        testIDs: []
    )

    func test_putRequest_dummyTamper() {
        let methodDescription = MethodDescription(
            name: "endpoint/method",
            requestMethod: .PUT,
            httpBody: .Protobuf(Auto_Api_EmailDelivery())
        )

        let method = PublicProtobufAPIMethod<Auto_Api_SuccessResponse>(methodDescription: methodDescription)
        let request = try! method.generateRequest(params: Self.params)

        XCTAssertNotNil(request.allHTTPHeaderFields!["X-Timestamp"])
        XCTAssert(request.allHTTPHeaderFields!["X-Timestamp"]!.isValidMD5)
    }

    func test_getRequest_generatedTamper() {
        let params: QueryParameters = [
            "c": .value("D"),
            "a": .value("b")
        ]

        let methodDescription = MethodDescription(
            name: "endpoint/method",
            requestMethod: .GET,
            URLParameters: params
        )

        let method = PublicProtobufAPIMethod<Auto_Api_SuccessResponse>(methodDescription: methodDescription)
        let request = try! method.generateRequest(params: Self.params)

        // a=bc=dg5cc1b3c24cd9lgo13k0noqhpd09qqdf.5509d34a817b8f2b2fc2a2052600bffbjptQLMkU@v.Cu2z-JmBuvHtestSalt => 63533e71cc3583e043a4becc6cac4722

        XCTAssertNotNil(request.allHTTPHeaderFields!["X-Timestamp"])
        XCTAssertEqual(request.allHTTPHeaderFields!["X-Timestamp"]!, "998b5cfebce0f8166a6c97042f14209e")
    }

    func test_getRequestNoParameters_generatedTamper() {
        let methodDescription = MethodDescription(
            name: "endpoint/method",
            requestMethod: .GET
        )

        let method = PublicProtobufAPIMethod<Auto_Api_SuccessResponse>(methodDescription: methodDescription)
        let request = try! method.generateRequest(params: Self.params)

        // g5cc1b3c24cd9lgo13k0noqhpd09qqdf.5509d34a817b8f2b2fc2a2052600bffbjptQLMkU@v.Cu2z-JmBuvHtestSalt => 293b46a9284fd93c9a502725f79387f7

        XCTAssertNotNil(request.allHTTPHeaderFields!["X-Timestamp"])
        XCTAssertEqual(request.allHTTPHeaderFields!["X-Timestamp"]!, "e3bcabd8c4ad17461a617919b66567e8")
    }

    func test_postRequestForceTamper_generatedTamper() {
        let params: QueryParameters = [
            "c": .value("D"),
            "a": .value("b")
        ]

        let methodDescription = MethodDescription(
            name: "endpoint/method",
            requestMethod: .POST,
            URLParameters: params,
            httpBody: .Multipart(data: .InMemory("asdf".data(using: .utf8)!), contentType: .MultipartFormData(""))
        )

        let method = PublicProtobufAPIMethod<Auto_Api_SuccessResponse>(methodDescription: methodDescription)
        let request = try! method.generateRequest(params: Self.params)

        // a=bc=dg5cc1b3c24cd9lgo13k0noqhpd09qqdf.5509d34a817b8f2b2fc2a2052600bffbjptQLMkU@v.Cu2z-JmBuvHtestSalt => 63533e71cc3583e043a4becc6cac4722

        XCTAssertNotNil(request.allHTTPHeaderFields!["X-Timestamp"])
        XCTAssertEqual(request.allHTTPHeaderFields!["X-Timestamp"]!, "ff7d14565c4f4e9d1f481e8511f038cc")
    }
}
