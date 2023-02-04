#include <maps/infra/apiteka/agent/tests/backend_stub.h>
#include <maps/infra/apiteka/proto/apiteka.pb.h>

#include <maps/libs/http/include/url.h>

#include <fmt/format.h>

namespace proto = yandex::maps::proto::apiteka;

namespace maps::apiteka::tests {

BackendStub::BackendStub(BackendStub::ApiKeys apiKeys)
{
    proto::ProviderInventory inventory;
    auto keysProto = inventory.add_keys_by_plan()->mutable_keys();
    for (auto [apiKey, secret] : apiKeys) {
        auto item = keysProto->Add();
        item->set_key(TString{apiKey});
        auto signatureRestriction = item->add_restrictions()->mutable_signature();
        signatureRestriction->set_signing_secret(TString{secret});
    }
    auto payload = inventory.SerializeAsStringOrThrow();

    inventoryHttpResponse_ = fmt::format(
        "HTTP/1.1 200 OK\r\nContent-Length: {}\r\n\r\n{}",
        payload.size(), payload
    );
}

http::Connection BackendStub::getCachedConnection(const http::URL&)
{
    return nullptr;
}

http::Connection BackendStub::connect(const http::URL&)
{
    return std::make_unique<SeparateIOStringBuf>(inventoryHttpResponse_);
}

void BackendStub::reuse(http::Connection conn, const http::URL& url)
{
    auto buff = dynamic_cast<SeparateIOStringBuf*>(conn.get());
    if (buff) {
        requestHistory_[url.toString()].push_back(buff->outputRawContents());
    }
}

} // namespace maps::apiteka::tests
