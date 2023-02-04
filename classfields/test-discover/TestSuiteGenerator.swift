//
//  TestSuiteGenerator.swift
//  test-discover
//
//  Created by Alexander Malnev on 7/2/20.
//

import Foundation
import SourceKittenFramework

struct TestSuiteData {
    var className: String
    var tests: Set<String>
}

public enum TestDiscover {
    public static func getTests(in directory: URL, suiteName: String, onlyInFiles files: [URL]? = nil) -> [String] {
        let filesToParse: [URL]
        if let files = files {
            filesToParse = files.filter { $0.path.hasPrefix(directory.path) }
        } else {
            filesToParse = getSwiftFiles(from: directory)
        }

        return getTests(in: filesToParse, suiteName: suiteName)
    }

    public static func getTests(in sources: [URL], suiteName: String) -> [String] {
        let filesToParse = sources.filter { $0.pathExtension == "swift" }

        let testClasses = filesToParse.compactMap { file in
            TestSuiteGenerator(path: file.path).testSuite()
        }

        return testClasses.flatMap { `class` in
            `class`.tests.sorted().map {
                "\(suiteName)/\(`class`.className)/\($0.dropLast(2))"
            }
        }
    }

    private static func getSwiftFiles(from directory: URL) -> [URL] {
        var result: [URL] = []

        func appendFilePaths(from path: URL) {
            let contents = (try? FileManager.default.contentsOfDirectory(at: path, includingPropertiesForKeys: [.isDirectoryKey])) ?? []

            for path in contents {
                guard !path.lastPathComponent.hasPrefix(".") else {
                    continue
                }

                if try! path.resourceValues(forKeys: [.isDirectoryKey]).isDirectory! {
                    appendFilePaths(from: path)
                } else if path.pathExtension == "swift" {
                    result.append(path)
                }
            }
        }

        appendFilePaths(from: directory)

        return result
    }
}

struct TestSuiteGenerator {
    typealias Subsctructure = [String: SourceKitRepresentable]

    var path: String

    func testSuite() -> TestSuiteData? {
        guard let file = File(path: path),
            let structure = try? Structure(file: file)
        else {
            return nil
        }

        if let className = Self.probeStructureForClassInfo(
            structure: structure.dictionary,
            stringView: file.stringView
        ) {
            var testNames = Set<String>()
            Self.probeStructureForTestNames(structure: structure.dictionary, accumulator: &testNames)

            return TestSuiteData(className: className, tests: testNames)
        } else {
            return nil
        }
    }

    private static func probeStructureForClassInfo(structure: Subsctructure, stringView: StringView) -> String? {
        if structure[SwiftDocKey.kind.rawValue] as? String == SwiftDeclarationKind.class.rawValue,
           let name = structure[SwiftDocKey.name.rawValue] as? String,
           name.hasSuffix("Test") || name.hasSuffix("Tests")
        {
            return name
        } else if let subsctructures = structure[SwiftDocKey.substructure.rawValue] as? [Subsctructure] {
            for subsctructure in subsctructures {
                if let result = probeStructureForClassInfo(structure: subsctructure, stringView: stringView) {
                    return result
                }
            }
            return nil
        } else {
            return nil
        }
    }

    private static func probeStructureForTestNames(structure: Subsctructure, accumulator: inout Set<String>) {
        if structure[SwiftDocKey.kind.rawValue] as? String == SwiftDeclarationKind.functionMethodInstance.rawValue,
           let name = structure[SwiftDocKey.name.rawValue] as? String,
           name.hasPrefix("test"),
           let accessibility = structure["key.accessibility"] as? String,
           accessibility == "source.lang.swift.accessibility.internal" || accessibility == "source.lang.swift.accessibility.public"
        {
            accumulator.insert(name)
            return
        } else if let subsctructures = structure[SwiftDocKey.substructure.rawValue] as? [Subsctructure] {
            for subsctructure in subsctructures {
                probeStructureForTestNames(structure: subsctructure, accumulator: &accumulator)
            }
        } else {
            return
        }
    }
}
