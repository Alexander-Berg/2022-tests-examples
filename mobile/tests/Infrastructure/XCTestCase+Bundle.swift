import XCTest

extension XCTestCase {

    /// Возвращает `Data` ресурса, лежащего в бандле XCTestCase'а.
    ///
    /// Bundle файл и XCTestCase класс должны иметь одинаковые имена.
    ///
    /// ExampleTests.swift
    /// ExampleTests.bundle
    ///
    /// - Parameters:
    ///   - resourceName: Имя ресурса
    ///   - resourceExtension: Расширение ресурса
    ///
    /// - Returns: nil в случае если Bundle не был найден или не удалось загрузить ресурс.
    func dataFromBundle(resourceName: String, resourceExtension: String = "json") -> Data? {
        let currentBundle = Bundle(for: type(of: self))
        let resourcesBundle = Bundle(
            url: currentBundle.url(forResource: "TestsResources", withExtension: "bundle") ?? URL(fileURLWithPath: "")
        )
        let testCaseBundleName = String(describing: type(of: self))

        guard
            let bundleUrl = resourcesBundle?.url(forResource: testCaseBundleName, withExtension: "bundle"),
            let resourceUrl = Bundle(url: bundleUrl)?.url(forResource: resourceName, withExtension: resourceExtension),
            let data = try? Data(contentsOf: resourceUrl)
        else {
            print("❌ Cannot load resource with name \(resourceName) from bundle with name \(testCaseBundleName) ❌")

            return nil
        }

        return data
    }

    /// Возвращает Dictionary для json файла, лежащего в бандле тест кейса
    ///
    /// - Returns: nil если файл не удалось найти или сериализовать
    func loadJson<T>(with filename: String) -> T? {
        guard let jsonData = dataFromBundle(resourceName: filename, resourceExtension: "json") else { return nil }

        do {
            guard let json = try JSONSerialization.jsonObject(with: jsonData, options: []) as? T else {
                print("❌ Wrong format of json data ❌")
                return nil
            }
            return json
        } catch {
            print("❌ Unable to serialize json file with name \(filename) ❌")
            return nil
        }
    }
}
