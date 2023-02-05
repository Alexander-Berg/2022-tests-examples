#include <maps/libs/fuzzing/include/handle_tester.h>
#include <maps/infra/yacare/include/yacare.h>
#include <maps/infra/yacare/include/test_utils.h>
#include <maps/libs/common/include/exception.h>

#include <maps/libs/log8/include/log8.h>

#include <algorithm>

namespace maps::fuzzing {

namespace {

http::MockResponse callApi(const std::string& path)
{
    http::MockRequest req(http::GET, "http://localhost" + path);
    return yacare::performTestRequest(req);
}

void initRandomHandle(const uint8_t* data, size_t size, Handle* h)
{
    FuzzedDataProvider fdp(data, size);
    // we don't want randomize port, so we start handle with '/'
    h->setPath("/" + fuzzed<std::string>(fdp, 1000));
}

} // namespace


Handle::Handle(const uint8_t* data, size_t size)
    : fdp_(std::make_unique<FuzzedDataProvider>(data, size))
{
    if (fuzzed<bool>(*fdp_))
        trash_ = fuzzed<std::string>(*fdp_);
}

Handle::~Handle()
{}


HandleTester::HandleTester(const uint8_t* data, size_t size)
{
    FuzzedDataProvider fdp(data, size);
    randomNum_ = fdp.ConsumeIntegral<size_t>();
    if (fdp.remaining_bytes() > 0) {
        bufferForFdp_.resize(fdp.remaining_bytes());
        if (auto numBytes = fdp.ConsumeData(bufferForFdp_.data(), size)) {
            bufferForFdp_.resize(numBytes);
        }
    }
    // add random handle
    initRandomHandle(bufferForFdp_.data(), bufferForFdp_.size(), &addHandle());
}

HandleTester::~HandleTester()
{}

Handle& HandleTester::addHandle()
{
    handles_.emplace_back(bufferForFdp_.data(), bufferForFdp_.size());
    return handles_.back();
}

Handle& HandleTester::addHandle(const std::string& name)
{
    return addHandle().setPath(name);
}

void HandleTester::checkAllHandlesPresent(const std::vector<std::string>& exclude)
{
    std::set<std::string> allHandles;
    for (auto& h : handles_)
        allHandles.insert(h.path());
    for (auto& h : exclude)
        allHandles.insert(h);

    std::vector<std::string> absentHandles;
    for (const auto* h: yacare::handlers()) {
        if (allHandles.count(h->path()) == 0)
            absentHandles.push_back(h->path());
    }

    if (absentHandles.size()) {
        throw Exception() << "Some handles are absent: " << stringutils::join(absentHandles, ", ");
    }
}

void HandleTester::chooseAndTestHandle()
{
    size_t num = randomNum_ % handles_.size();
    INFO() << "run handle " << num + 1 << "/" << handles_.size();
    auto& handle = handles_[num];
    testYacareHandle(handle);
}

void testYacareHandle(const Handle& handle)
{
    auto pathWithArgs = handle.pathWithArgs();
    INFO() << "path with args: " << pathWithArgs;
    testYacareHandle(pathWithArgs);
}

void testYacareHandle(const std::string& path)
{
    auto response = callApi(path);
    if (response.status >= 500)
        __builtin_trap();
}

} // maps::fuzzing
