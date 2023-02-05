#include <maps/libs/http/include/test_utils.h>
#include <maps/libs/http/include/response_status_codes.h>
#include <maps/libs/common/include/file_utils.h>
#include <maps/libs/common/include/exception.h>

#include <library/cpp/testing/common/env.h>
#include <util/generic/mapfindptr.h>
#include <util/generic/scope.h>

#include <map>
#include <sstream>

namespace maps::http {

namespace {

Connection makeMockConnection(MockResponse&& mockResponse)
{
    mockResponse.headers.erase("Connection");

    if (Status(mockResponse.status) == Status::NoContent) {
        ASSERT(mockResponse.body.empty());
    } else {
        std::string size = std::to_string(mockResponse.body.size());
        mockResponse.headers.emplace("Content-Length", std::move(size));
    }

    std::string response;
    response += "HTTP/1.1 " + std::to_string(mockResponse.status) + " ";
    response += Status{mockResponse.status}.description();
    response += "\nConnection: close\n";
    for (auto&& [key, value] : mockResponse.headers) {
        response += key + ": " + value + "\n";
    }
    response += "\n";
    response += mockResponse.body;
    return std::make_unique<std::stringbuf>(response);
}

class MockHolder
{
public:
    static MockHolder& instance()
    {
        static MockHolder instance;
        return instance;
    }

    bool hasMocks() const
    {
        return hasMocks_.test();
    }

    bool isInsideMock() const
    {
        return isInsideMock_;
    }

    Connection matchRequest(const MockRequest& request) const
    {
        ASSERT(FromYaTest());
        ASSERT(!isInsideMock_);

        auto url = request.url.roundToPath();
        std::lock_guard lock(mutex_);
        if (auto mockFunc = MapFindPtr(mocks_, url)) {
            isInsideMock_ = true;
            Y_DEFER {
                isInsideMock_ = false;
            };
            return makeMockConnection((*mockFunc)(request));
        }

        throw LogicError() << "Failed to find mock for the given request with the url " << url;
    }

    MockHandle addMock(const URL& url, MockFunction mock)
    {
        ASSERT(FromYaTest());

        std::string roundedUrl = url.roundToPath();
        std::lock_guard lock(mutex_);
        auto [it, inserted] = mocks_.emplace(roundedUrl, std::move(mock));
        ASSERT(inserted);
        hasMocks_.test_and_set();
        return MockHandle(std::move(roundedUrl));
    }

    void removeMock(const std::string& url)
    {
        std::lock_guard lock(mutex_);
        mocks_.erase(url);
        if (mocks_.empty()) {
            hasMocks_.clear();
        }
    }

private:
    MockHolder() = default;
    MockHolder(const MockHolder&) = delete;
    MockHolder(MockHolder&&) = delete;
    MockHolder& operator=(const MockHolder&) = delete;
    MockHolder& operator=(MockHolder&&) = delete;

    mutable std::mutex mutex_;
    std::atomic_flag hasMocks_;
    std::map<std::string, MockFunction> mocks_;
    static thread_local bool isInsideMock_;
};

thread_local bool MockHolder::isInsideMock_ = false;

} //namespace

const std::string& MockRequest::header(const std::string& key) const
{
    static const std::string EMPTY;
    auto i = headers.find(key);
    return i != headers.end() ? i->second : EMPTY;
}

MockResponse::MockResponse(std::string body_)
    : body(std::move(body_))
{
}

MockResponse MockResponse::fromFile(const std::string& filepath)
{
    return MockResponse(common::readFileToString(filepath));
}

MockResponse MockResponse::fromArcadia(const std::string& filepath)
{
    return fromFile(ArcadiaSourceRoot() + "/" + filepath);
}

MockResponse MockResponse::withStatus(int status_)
{
    MockResponse result;
    result.status = status_;
    return result;
}

MockHandle::MockHandle(std::string url)
    : url(std::move(url))
{
}

MockHandle::~MockHandle()
{
    if (!url.empty()) {
        MockHolder::instance().removeMock(url);
    }
}

bool hasMocks()
{
    return MockHolder::instance().hasMocks();
}

bool isInsideMock()
{
    return MockHolder::instance().isInsideMock();
}

Connection matchRequest(const MockRequest& request)
{
    return MockHolder::instance().matchRequest(request);
}

MockHandle addMock(const URL& url, MockFunction mock)
{
    return MockHolder::instance().addMock(url, std::move(mock));
}

} //namespace maps::http
