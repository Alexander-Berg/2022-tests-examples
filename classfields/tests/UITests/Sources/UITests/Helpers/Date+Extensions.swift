import Foundation

extension Date {
    func adding(_ value: Int, component: Calendar.Component) -> Date {
        guard let result = Calendar.current.date(byAdding: component, value: value, to: self, wrappingComponents: true)
        else { fatalError() }

        return result
    }
}
