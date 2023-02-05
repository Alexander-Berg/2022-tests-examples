#pragma once

namespace maps::photos {


class PhotoStorageMockHandle {
public:
    PhotoStorageMockHandle(const PhotoStorageMockHandle&) = delete;
    PhotoStorageMockHandle& operator=(const PhotoStorageMockHandle&) = delete;
    PhotoStorageMockHandle();
    ~PhotoStorageMockHandle();
};

} // namespace maps::photos
