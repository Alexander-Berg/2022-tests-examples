import Foundation
import Combine
import AutoRuCallsCore

final class CallAudioDeviceSourceMock: AudioDeviceSource {
    struct Device: AudioDevice {
        var name: String {
            "\(deviceType)"
        }

        var deviceType: AudioDeviceType
    }

    let currentDevicePublisher = PassthroughSubject<Device, Never>()
    let availableDevicesPublisher = PassthroughSubject<Set<Device>, Never>()

    var currentDevice: Device

    func setCurrentDevice(_ device: Device) {
        currentDevice = device
        currentDevicePublisher.send(device)
    }

    var availableDevices: Set<Device> {
        didSet {
            guard availableDevices != oldValue else { return }

            availableDevicesPublisher.send(availableDevices)
        }
    }

    nonisolated init() {
        currentDevice = Device(deviceType: .receiver)
        availableDevices = [Device(deviceType: .receiver), Device(deviceType: .speaker)]
    }
}
