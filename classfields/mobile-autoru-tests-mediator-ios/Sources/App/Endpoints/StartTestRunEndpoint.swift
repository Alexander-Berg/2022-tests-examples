import Foundation
import Mediation
import Vapor

extension SplitStrategyType: Content { }

private struct EndpointRequest: Content {
    let sourceBranch: String
    let sourceBuildId: Int
    let sourcePullRequestId: Int?
    let strategy: SplitStrategyType
    let maxAgentsCount: Int
    let maxBatchSize: Int
    let maxRetriesCount: Int
    let tests: [String]
    let initiatorBuildNumber: String
    let initiatorBuildId: Int
    let testsRunPoolConfigID: String?
    let testsRunResultConfigID: String?
}

private struct EndpointResponse: Content {
    let status: ResponseCode
}

func startTestRun(req: Request) -> EventLoopFuture<Response> {
    do {
        let payload = try req.content.decode(EndpointRequest.self)

        req.application.mediation?.startTestRun(
            sourceBranch: payload.sourceBranch,
            sourceBuildID: payload.sourceBuildId,
            sourcePullRequestID: payload.sourcePullRequestId,
            strategyType: payload.strategy.type,
            tests: payload.tests.map { TestCase($0) },
            maxBatchSize: payload.maxBatchSize,
            maxAgentsCount: payload.maxAgentsCount,
            maxRetriesCount: payload.maxRetriesCount,
            initiatorBuildID: payload.initiatorBuildId,
            initiatorBuildNumber: payload.initiatorBuildNumber,
            testsRunPoolConfigID: payload.testsRunPoolConfigID ?? "VerticalMobile_AutoRU_MobileAutoruClientIos_RunTestsPool",
            testsRunResultConfigID: payload.testsRunResultConfigID ?? "VerticalMobile_AutoRU_MobileAutoruClientIos_RunTestsResult"
        )
    } catch DecodingError.keyNotFound(let codingKeys, _) {
        return ErrorResponse(status: .invalidParameter, message: "Invalid parameter '\(codingKeys.stringValue)'")
            .encodeResponse(status: .badRequest, for: req)
    } catch {
        return ErrorResponse(status: .internalError, message: "Internal error: \(error)")
            .encodeResponse(status: .internalServerError, for: req)
    }

    return EndpointResponse(status: .mediationRunOk).encodeResponse(for: req)
}
