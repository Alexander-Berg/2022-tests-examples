import Foundation

public enum BuildParameter: String {
    case initiatorBuildID = "initiator_build_id"
    case initiatorBuildNumber = "initiator_build_number"
    case sourceBuildID = "source_build_id"
    case sourceBranchName = "source_branch_name"
    case sourcePullRequestID = "source_pr_id"
    case finishedBuildsIDs = "finished_builds_ids"
    case mediatorBuildUUID = "mediator_build_uuid"
    case mediatorRunUUID = "mediator_run_uuid"

    public enum Value {
        case string(String)
        case int(Int)
        indirect case array([Value])

        var value: String {
            switch self {
            case .string(let value):
                return value
            case .int(let value):
                return "\(value)"
            case .array(let value):
                return value.map(\.value).joined(separator: ",")
            }
        }
    }
}
