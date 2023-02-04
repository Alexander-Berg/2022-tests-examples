#include <maps/infra/apiteka/agent/tests/backend_stub.h>
#include <maps/infra/apiteka/agent/tests/fixture.h>
#include <maps/infra/apiteka/agent/tests/samples.h>

#include <maps/infra/apiteka/agent/lib/include/servant.h>
#include <maps/infra/yacare/include/request.h>
#include <maps/libs/http/include/request_methods.h>
#include <maps/libs/json/include/builder.h>

#include <library/cpp/tvmauth/client/mocked_updater.h>
#include <library/cpp/tvmauth/unittest.h>
#include <library/cpp/testing/gtest_protobuf/matcher.h>

#include <contrib/restricted/googletest/googletest/include/gtest/gtest.h>
#include <contrib/restricted/googletest/googlemock/include/gmock/gmock.h>

using namespace testing;

namespace maps::apiteka::tests {

class ApitekaServantFixture : public AgentBaseFixture
{
public:
    static void buildAcceptRequest(
        yacare::RequestBuilder& builder, const std::string& pathAndQuery)
    {
        std::ostringstream stream;
        json::Builder bodyBuilder{stream};
        bodyBuilder << [&pathAndQuery](json::ObjectBuilder obj) {
            obj["path_query"] = pathAndQuery;
            obj["ip"] = "1.1.1.1";
            obj["referer"] = "";
        };

        auto body{stream.str()};
        builder.putenv("REQUEST_METHOD", http::POST.value());
        builder.putenv("CONTENT_LENGTH", std::to_string(body.length()));
        builder.readBody([&body](char* data, std::size_t size) -> bool {
            EXPECT_EQ(size, body.length());
            std::memcpy(data, body.data(), size);
            return true;
        });
    }

    static auto setupBackendStub(BackendStub::ApiKeys apikeys)
    {
        return std::make_shared<BackendStub>(std::move(apikeys));
    }

    static auto setupTvmClient()
    {
        auto mockedSettings = NTvmAuth::TMockedUpdater::TSettings{
            .SelfTvmId = 42,
            .Backends = {
                {.Alias = "apiteka", .Id = 153, .Value = "PlushServiceTicket"}
            }
        };
        return std::make_shared<NTvmAuth::TTvmClient>(
            MakeIntrusiveConst<NTvmAuth::TMockedUpdater>(mockedSettings)
        );
    }
};

TEST_F(ApitekaServantFixture, ExtractKeyFromUrl)
{
    EXPECT_EQ(
        detail::extractQueryParameter("/foo/bar?key1=value1&key2=value2", "key1"),
        "value1");
    EXPECT_EQ(
        detail::extractQueryParameter("/foo/bar?key1=value1&key2=value2", "key2"),
        "value2");
}

TEST_F(ApitekaServantFixture, InventoryRequestUrlAndTvm)
{
    auto backendStub = setupBackendStub({{samples::SOME_APIKEY, samples::SOME_SECRET}});
    Servant servant{backendStub, setupTvmClient()};

    EXPECT_THAT(
        backendStub->receivedRequests(),
        UnorderedElementsAre(
            Pair(
                "http://apiteka.ohmy.backend/v1/provider/inventory",
                ElementsAre(HasSubstr(fmt::format("{}: PlushServiceTicket", auth::SERVICE_TICKET_HEADER)))
            )
        )
    );
}

TEST_F(ApitekaServantFixture, CorrectRequestValidation)
{
    Servant servant{setupBackendStub({{samples::SOME_APIKEY, samples::SOME_SECRET}})};
    auto unsignedUrl{attachApiKey("/foo/bar", samples::SOME_APIKEY)};
    {
        yacare::RequestBuilder builder;
        buildAcceptRequest(builder, signUrl(samples::SOME_SECRET, unsignedUrl));

        auto response = servant.access(builder.request());
        EXPECT_EQ(
            (json::Builder() << response).str(),
            R"({"X-Ya-Aptk-Provider-Features":""})"
        );
    }
    {
        yacare::RequestBuilder builder;
        buildAcceptRequest(builder, signUrl(samples::ANOTHER_SECRET, unsignedUrl));

        EXPECT_THROW(servant.access(builder.request()), yacare::errors::Forbidden);
    }
}

TEST_F(ApitekaServantFixture, MissingApikey)
{
    Servant servant{setupBackendStub({{samples::SOME_APIKEY, samples::SOME_SECRET}})};
    yacare::RequestBuilder builder;
    buildAcceptRequest(builder, signUrl(samples::SOME_SECRET, "/foo/bar"));
    EXPECT_THROW(servant.access(builder.request()), yacare::errors::BadRequest);
}

TEST_F(ApitekaServantFixture, DuplicateApikey)
{
    Servant servant{setupBackendStub({{samples::SOME_APIKEY, samples::SOME_SECRET}})};
    yacare::RequestBuilder builder;
    buildAcceptRequest(
        builder,
        signUrl(
            samples::SOME_SECRET,
            attachApiKey(
                attachApiKey("/foo/bar", samples::SOME_APIKEY),
                samples::ANOTHER_APIKEY)));

    EXPECT_THROW(servant.access(builder.request()), yacare::errors::BadRequest);
}

} // namespace maps::apiteka::tests
