//
//  FileListRequestMock.swift
//  18/07/2019
//

@testable import YandexDisk

final class FileListRequestMock: YOFileListRequest {
    private let testReceivedData: NSMutableData

    init(data: Data) {
        self.testReceivedData = NSMutableData(data: data)
        super.init(url: URL(string: "https://ya.ru"))
    }

    override var receivedData: NSMutableData! {
        return testReceivedData
    }

    func testParseReceivedData() {
        perform(#selector(YOFileListRequestYDTest.parseReceivedData))
    }
}
