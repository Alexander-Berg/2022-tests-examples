import Foundation
import AutoRuCellHelpers
import AutoRuTableController

extension BaseTableModelBuilder {
    /// Возвращает первый TableItem, который начинается с заданного префикса, в первой TableSection
    func tableItem(withIdStartsWith string: String) -> TableItem? {
        self.build().first?.items.first(where: { $0.identifier.starts(with: string) })
    }
}
