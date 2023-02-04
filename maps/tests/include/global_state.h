#pragma once

#include <vector>

struct TestGlobalState
{
    TestGlobalState() = default;
    TestGlobalState(const TestGlobalState& other) :
        ids(other.ids),
        requests(other.requests)
    { }

    void requestAdded(size_t id, size_t request)
    {
        ids.push_back(id);
        requests.push_back(request);
    }

    void lock()
    {
        mutex.lock();
    }

    void unlock()
    {
        mutex.unlock();
    }

    std::vector<size_t> ids;
    std::vector<size_t> requests;
    std::mutex mutex;
};
