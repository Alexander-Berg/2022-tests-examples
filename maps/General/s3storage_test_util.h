#pragma once

namespace maps::streetview::ugc {

class MockS3StorageHandle {
public:
    MockS3StorageHandle(const MockS3StorageHandle&) = delete;
    MockS3StorageHandle& operator=(const MockS3StorageHandle&) = delete;
    MockS3StorageHandle();
    ~MockS3StorageHandle();
};

} // namespace maps::streetview::ugc
