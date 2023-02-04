import AutoRuProtoModels
import UIKit
@testable import AutoRuBackendLayout
import AutoRuStandaloneCarHistory
import Snapshots

@testable import AutoRuCellHelpers

protocol CarReportCardBlockTest: BaseUnitTest {
    func snapshot(functionName: String)
}

extension CarReportCardBlockTest {
    func snapshot(functionName: String) {
        let url = Bundle.current.url(forResource: functionName, withExtension: "xml")!
        let xml = try! String(contentsOf: url)

        let parser = BackendLayoutParser(xmlData: xml.data(using: .utf8)!)
        let item = parser.parse()!

        var layoutOutput: BackendLayoutOutput?

        if let output = self as? BackendLayoutOutput {
            layoutOutput = output
        }

        let builder = BaseTableModelBuilder()
        BackendTable.cell(for: item, on: builder, output: layoutOutput)

        if let cellHelper = builder.build().first?.items.first?.cellHelper {
            Snapshot.compareWithSnapshot(cellHelper: cellHelper,
                                         maxWidth: DeviceWidth.iPhone11,
                                         identifier: "\(String(describing: Self.self))_\(functionName)",
                                         file: "CarReportCardBlockTest")
        }
    }
}

