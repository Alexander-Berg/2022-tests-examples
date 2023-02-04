#pragma once

#include <maps/infra/apiteka/agent/tests/separate_io_stringbuf.h>
#include <maps/libs/http/include/connection.h>

#include <unordered_map>

namespace maps::apiteka::tests {

class BackendStub final : public http::ConnectionPool {
public:
    using ApiKeys = std::initializer_list<std::pair<std::string_view, std::string_view>>;

    explicit BackendStub(ApiKeys);

    http::Connection connect(const http::URL&) override;
    http::Connection getCachedConnection(const http::URL&) override;
    void reuse(http::Connection, const http::URL&) override;

    // map<url, requests>
    using RequestHistory = std::unordered_map<std::string, std::vector<std::string>>;
    const RequestHistory& receivedRequests() const noexcept
    {
        return requestHistory_;
    }

private:
    RequestHistory requestHistory_;
    std::string inventoryHttpResponse_;
};

} // namespace maps::apiteka::tests
