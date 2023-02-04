import Foundation

struct RunBuild: Codable {
    let branchName: String
    let buildType: BuildType
    let comment: Comment?
    let properties: Properties

    struct BuildType: Codable {
        let id: String
    }

    struct Comment: Codable {
        let text: String
    }

    struct Properties: Codable {
        let property: [Property]

        struct Property: Codable {
            let name: String
            let value: String
        }
    }
}
