#!/usr/bin/swift

import Foundation

let diskImage = "/Applications/Xcode.app/Contents/Developer/Platforms/AppleTVOS.platform/DeviceSupport/10.0 (14T328)/DeveloperDiskImage.dmg"
let diskSignature = "/Applications/Xcode.app/Contents/Developer/Platforms/AppleTVOS.platform/DeviceSupport/10.0 (14T328)/DeveloperDiskImage.dmg.signature"

enum Command: String {
    case install = "-i"
    case run = "-r"
    case runsNumber = "-n"
    case restart = "restart"
}

let arguments = CommandLine.arguments
dump(arguments)

var bundleId: String? = nil
var bundlePath: String? = nil
var shouldRestart: Bool = false
var numberOfRuns = 1

if arguments.count > 1 {
    var index = 1
    while index < arguments.count {
        let arg = arguments[index]
        if let command = Command(rawValue: arg) {
            switch(command) {
            case .install:
                bundlePath = arguments[index + 1]
                index = index + 1
            case .restart:
                shouldRestart = true
            case .run:
                bundleId = arguments[index + 1]
                index = index + 1
            case .runsNumber:
                numberOfRuns = Int(arguments[index + 1])!
                index = index + 1
            }
        }
        index = index + 1
    }
}

class Device {

    var uuid: String = ""
    var name: String = ""
    var iosVersion: String = ""

    init() {

    }

    func restart() {
        ep("idevicediagnostics", "restart", "-u", uuid)
    }

    func installApp(bundlePath: String) {
        ep("ideviceinstaller", "-u", uuid, "-i", bundlePath)
    }

    @discardableResult
    func runApp(bundleId: String) -> String {
       return ep("idevicedebug", "-e", "DYLD_PRINT_STATISTICS=1", "-e", "OS_ACTIVITY_MODE=disable", "-u", uuid, "run", bundleId).0
    }

    var coldLogs: [String] = []
    var warmLogs: [String] = []
}

func processLogs(logs: [String]) {

    var timesNames: [String] = []
    var times: [String:[Double]] = [:]

    for log in logs {

        let lines = log.components(separatedBy: .newlines) as [NSString]

        for line in lines {
            let range = NSRange(location: 0, length: line.length)

            let regexp = try! NSRegularExpression(pattern: "^(.* time):\\s*(\\d+.\\d+) millisec.*", options: NSRegularExpression.Options(rawValue: 0))
            let matches = regexp.matches(in: line as String, options: NSRegularExpression.MatchingOptions(rawValue: 0), range: range)

            for match: NSTextCheckingResult in matches {

                if match.numberOfRanges == 3 {
                    let name = line.substring(with: match.rangeAt(1))
                    let value = Double(line.substring(with: match.rangeAt(2)))!
                    if times[name] == nil {
                        times[name] = [value]
                    } else {
                        times[name]!.append(value)
                    }
                    if !timesNames.contains(name) {
                        timesNames.append(name)
                    }
                }
            }
        }
    }

    for name in timesNames {
        let values = times[name]!
        //let output = "\(name): \(values)"
        //print(output)
        times[name] = [values.reduce(0, +) / Double(values.count)]
    }

    let totalTimesNames = timesNames.enumerated().filter { (index, timeName) in timeName.contains("Total") }.map { $0.1 }
    let totalTime = totalTimesNames.map { times[$0]![0] }.reduce(0, +)
    var lastTotalTime: Double = -1

    print("Total launch time \(String(format: "%.2f", totalTime)) milliseconds")
    for timeName in timesNames {

        let value = times[timeName]![0]
        let valueString = String(format: "%.2f", value)
        var output = "\(timeName): \(valueString)"

        if totalTimesNames.contains(timeName) {
            lastTotalTime = value
            output += " (100.0%)"
            let valuePercent = String(format: "%.1f", value / totalTime * 100.0)
            output += ", \(valuePercent)% of total"
        } else {
            let valuePercent = String(format: "%.1f", value / lastTotalTime * 100.0)
            output += " (\(valuePercent)%)"
            let totalPercent = String(format: "%.1f", value / totalTime * 100.0)
            output += ", \(totalPercent)% of total"
            output = "\t" + output
        }
        
        print(output)
    }
}

@discardableResult
func shell(_ launchPath: String, _ arguments: [String] = [], printCommand: Bool = false, printOutput: Bool = false) -> (String , Int32) {

    if (printCommand) {
        print("# " + ([launchPath] + arguments).joined(separator: " "))
    }

    let task = Process()
    task.launchPath = launchPath
    task.arguments = arguments

    let outputPipe = Pipe()
    let inputPipe = Pipe()
    let errorPipe = Pipe()
    task.standardOutput = outputPipe
    task.standardError = errorPipe
    task.standardInput = inputPipe
    task.launch()

    var output = ""
    outputPipe.fileHandleForReading.readabilityHandler = { fileHandle in
        if let newPart = String(data: fileHandle.availableData, encoding: String.Encoding.utf8) {
            if printOutput {
                print(newPart, terminator: "")
            }
            output += newPart
        }
    }

    errorPipe.fileHandleForReading.readabilityHandler = { fileHandle in
        if let errorPart = String(data: fileHandle.availableData, encoding: String.Encoding.utf8) {
            if printOutput {
                print("Error part: " + errorPart, terminator: "")
            }
        }

    }

    task.waitUntilExit()
    outputPipe.fileHandleForReading.readDataToEndOfFile()

    let status = task.terminationStatus
    output = output.trimmingCharacters(in: CharacterSet.whitespacesAndNewlines)

    if printOutput {
        print()
    }

    return (output, status)
}

@discardableResult
func e(_ command: String, _ arguments: String...) -> (String , Int32) {
    let whichPathForCommand = shell("/bin/bash", [ "-l", "-c", "which \(command)"]).0
    return shell(whichPathForCommand, arguments)
}

@discardableResult
func ep(_ command: String, _ arguments: String...) -> (String , Int32) {
    let whichPathForCommand = shell("/bin/bash", [ "-l", "-c", "which \(command)"]).0
    return shell(whichPathForCommand, arguments, printCommand: true, printOutput: true)
}

var devices = [Device]()

let uuids = ep("idevice_id", "-l").0.components(separatedBy: CharacterSet.whitespacesAndNewlines)
for uuid in uuids {
    var device = Device()
    device.uuid = uuid
    device.name = e("idevicename", "-u", uuid).0
    device.iosVersion = e("ideviceinfo", "-u", uuid, "-k", "ProductVersion").0
    devices.append(device)
}

dump(devices)

if shouldRestart {

    for device in devices {
        device.restart()
    }

} else {

    if let bunldeId = bundleId {
        for i in 0..<numberOfRuns {
            for device in devices {
                if let bundlePath = bundlePath {
                    device.installApp(bundlePath: bundlePath)
                    sleep(3)
                    device.coldLogs.append(device.runApp(bundleId: bunldeId))
                    sleep(3)
                }
                device.warmLogs.append(device.runApp(bundleId: bunldeId))
                sleep(3)
            }

            for device in devices {
                let deviceString = "\(device.name) \(device.uuid) iOS \(device.iosVersion)"
                print("----------  \(deviceString)  -------------\n")
                if device.coldLogs.count > 0 {
                    print("---- COLD LAUNCHES ---- ")
                    processLogs(logs: device.coldLogs)
                }
                print("---- WARM LAUNCHES ---- ")
                processLogs(logs: device.warmLogs)
                print()
            }
        }
    }
}




