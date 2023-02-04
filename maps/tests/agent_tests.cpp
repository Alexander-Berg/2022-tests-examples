#include <maps/infra/apiteka/agent/tests/backend_stub.h>
#include <maps/infra/apiteka/agent/tests/fixture.h>
#include <maps/infra/apiteka/agent/tests/mocks.h>
#include <maps/infra/apiteka/agent/tests/samples.h>

#include <maps/infra/apiteka/agent/lib/include/agent.h>
#include <maps/infra/apiteka/agent/lib/include/inventory.h>
#include <maps/infra/apiteka/proto/apiteka.pb.h>
#include <maps/infra/yacare/include/error.h>

#include <maps/libs/auth/include/tvm.h>
#include <maps/libs/common/include/base64.h>
#include <maps/libs/common/include/hmac.h>
#include <maps/libs/http/include/connection.h>

#include <library/cpp/testing/gtest/gtest.h>
#include <contrib/restricted/googletest/googlemock/include/gmock/gmock.h>

#include <future>

using namespace testing;
using namespace yacare::errors;
namespace proto = yandex::maps::proto::apiteka;

namespace maps::apiteka::tests {

namespace samples {
const proto::ProviderInventory EMPTY_INVENTORY{};
const std::string SOME_APPID{"someappid"};
const std::string ANOTHER_APPID{"anotherappid"};

const std::string SOME_IP_ADDRESS{"192.168.1.0"};
const std::string ANOTHER_IP_ADDRESS{"127.0.0.1"};

const std::string SOME_HTTP_REFERER{"http://yandex.ru"};
const std::string ANOTHER_HTTP_REFERER{"http://google.com"};
} // namespace samples

class AgentFixture : public AgentBaseFixture {
public:
    static auto respondWith(const proto::ProviderInventory& inventory)
    {
        std::string response;
        prepareInventoryResponse(inventory, response);
        return InvokeWithoutArgs([response = std::move(response)] {
            return std::make_unique<SeparateIOStringBuf>(response);
        });
    }

protected:
    static void prepareInventoryResponse(
        const proto::ProviderInventory& inventory, std::string& out)
    {
        std::ostringstream response;
        response << "HTTP/1.1 200 OK\r\n\r\n";
        ASSERT_TRUE(inventory.SerializeToOstream(&response));

        out = response.str();
    }

    ConnectionPoolMock connectionPool_;
};

TEST_F(AgentFixture, AccessForbiddenWithUnknownApiKey)
{
    EXPECT_CALL(connectionPool_, connect)
        .WillOnce(respondWith(samples::EMPTY_INVENTORY));

    Agent agent{{}, {.connectionPool = &connectionPool_}};
    EXPECT_THROW_MESSAGE_HAS_SUBSTR(
        agent.access(samples::SOME_APIKEY, {}),
        yacare::errors::Forbidden, samples::SOME_APIKEY
    );
}

TEST_F(AgentFixture, ApiKeyWithNoRestrictions)
{
    proto::ProviderInventory inventory;
    inventory.add_keys_by_plan()->add_keys()->set_key(TString{samples::SOME_APIKEY});
    EXPECT_CALL(connectionPool_, connect).WillOnce(respondWith(inventory));

    Agent agent{{}, {.connectionPool = &connectionPool_}};
    EXPECT_EQ(
        agent.access(samples::SOME_APIKEY, {}),
        Inventory::Plan{}  // empty plan
    );
}

TEST_F(AgentFixture, ApiKeyAppIdValidity)
{
    proto::ProviderInventory inventory;
    auto& key{*inventory.add_keys_by_plan()->add_keys()};
    key.set_key(TString{samples::SOME_APIKEY});
    key.add_restrictions()->set_app_id(TString{samples::SOME_APPID});
    EXPECT_CALL(connectionPool_, connect)
        .WillOnce(respondWith(inventory));

    Agent agent{{}, {.connectionPool = &connectionPool_}};
    EXPECT_THROW(agent.access(samples::SOME_APIKEY, {}), Forbidden);
    EXPECT_THROW(
        agent.access(samples::SOME_APIKEY, {.appId = samples::ANOTHER_APPID}),
        Forbidden
    );
    // success
    EXPECT_EQ(
        agent.access(samples::SOME_APIKEY, {.appId = samples::SOME_APPID}),
        Inventory::Plan{}
    );
}

TEST_F(AgentFixture, ApiKeyIpAddressValidity)
{
    proto::ProviderInventory inventory;
    auto& key{*inventory.add_keys_by_plan()->add_keys()};
    key.set_key(TString{samples::SOME_APIKEY});
    key.add_restrictions()->set_ip_address(TString{samples::SOME_IP_ADDRESS});
    EXPECT_CALL(connectionPool_, connect)
        .WillOnce(respondWith(inventory));

    Agent agent{{}, {.connectionPool = &connectionPool_}};
    EXPECT_THROW(agent.access(samples::SOME_APIKEY, {}), Forbidden);
    EXPECT_THROW(
        agent.access(samples::SOME_APIKEY, {.ipAddress = samples::ANOTHER_IP_ADDRESS}),
        Forbidden
    );
    // success
    EXPECT_EQ(
        agent.access(samples::SOME_APIKEY, {.ipAddress = samples::SOME_IP_ADDRESS}),
        Inventory::Plan{}
    );
}

TEST_F(AgentFixture, ApiKeyHttpRefererValidity)
{
    proto::ProviderInventory inventory;
    auto& key{*inventory.add_keys_by_plan()->add_keys()};
    key.set_key(TString{samples::SOME_APIKEY});
    key.add_restrictions()->set_http_referer(
        TString{samples::SOME_HTTP_REFERER});
    EXPECT_CALL(connectionPool_, connect)
        .WillOnce(respondWith(inventory));

    Agent agent{{}, {.connectionPool = &connectionPool_}};
    EXPECT_THROW(agent.access(samples::SOME_APIKEY, {}), Forbidden);
    EXPECT_THROW(
        agent.access(samples::SOME_APIKEY, {.httpReferer = samples::ANOTHER_HTTP_REFERER}),
        Forbidden
    );

    EXPECT_EQ(
        agent.access(samples::SOME_APIKEY, {.httpReferer = samples::SOME_HTTP_REFERER}),
        Inventory::Plan{}
    );
}

TEST_F(AgentFixture, ApiKeySignatureValidity)
{
    proto::ProviderInventory inventory;
    auto& key{*inventory.add_keys_by_plan()->add_keys()};
    key.set_key(TString{samples::SOME_APIKEY});
    key.add_restrictions()->mutable_signature()->set_signing_secret(TString{samples::SOME_SECRET});
    EXPECT_CALL(connectionPool_, connect)
        .WillOnce(respondWith(inventory));

    Agent agent{{}, {.connectionPool = &connectionPool_}};
    EXPECT_THROW(
        agent.access(samples::SOME_APIKEY, {}),
        yacare::errors::Forbidden
    );

    const auto url{samples::SOME_URL_PATH + "?apikey=" + samples::SOME_APIKEY};
    EXPECT_THROW(
        agent.access(samples::SOME_APIKEY, {.url = url}),
        yacare::errors::Forbidden
    );

    EXPECT_EQ(
        agent.access(samples::SOME_APIKEY, {.url = signUrl(samples::SOME_SECRET, url)}),
        Inventory::Plan{}
    );
}

TEST_F(AgentFixture, MultipleRestrictionsPerKey)
{
    proto::ProviderInventory inventory;
    {  // set plan
        auto planEntry = inventory.add_keys_by_plan();

        auto plan = planEntry->mutable_plan();
        plan->set_id("plan-with-size");
        plan->set_features(R"({"maxSize":"1000,1000"})");

        auto keySpec = planEntry->add_keys();
        keySpec->set_key(TString{samples::SOME_APIKEY});
        keySpec->add_restrictions()->set_ip_address(TString{samples::SOME_IP_ADDRESS});
        keySpec->add_restrictions()->set_ip_address(TString{samples::ANOTHER_IP_ADDRESS});
        keySpec->add_restrictions()->set_app_id(TString{samples::SOME_APPID});
    }
    EXPECT_CALL(connectionPool_, connect)
        .WillOnce(respondWith(inventory));

    Agent agent{{}, {.connectionPool = &connectionPool_}};
    EXPECT_THROW(
        agent.access(
            samples::SOME_APIKEY,
            {.appId = samples::ANOTHER_APPID,.ipAddress = samples::SOME_IP_ADDRESS}
        ),
        Forbidden
    );

    EXPECT_EQ(
        agent.access(
            samples::SOME_APIKEY,
            {.appId = samples::SOME_APPID,
            .ipAddress = samples::SOME_IP_ADDRESS,
            .httpReferer = samples::SOME_HTTP_REFERER}
        ),
        (Inventory::Plan{.id = "plan-with-size", .features = R"({"maxSize":"1000,1000"})"})
    );
}

TEST_F(AgentFixture, ProviderInventoryUpdate)
{
    std::optional<Agent> lazyAgent;
    auto testAccessIsForbidden{InvokeWithoutArgs([&lazyAgent] {
        ASSERT_TRUE(lazyAgent);
        ASSERT_THROW(
            lazyAgent->access(samples::SOME_APIKEY, {}),
            yacare::errors::Forbidden);
    })};
    auto testHasAccess{InvokeWithoutArgs([&lazyAgent] {
        ASSERT_TRUE(lazyAgent);
        ASSERT_NO_THROW(lazyAgent->access(samples::SOME_APIKEY, {}));
    })};

    proto::ProviderInventory inventory;
    inventory.add_keys_by_plan()->add_keys()->set_key(TString{samples::SOME_APIKEY});

    auto promise{std::make_shared<std::promise<void>>()};
    auto future{promise->get_future()};
    auto notify{InvokeWithoutArgs(
        [promise = std::move(promise)]() mutable { promise->set_value(); })};

    EXPECT_CALL(connectionPool_, connect)
        .WillOnce(respondWith(samples::EMPTY_INVENTORY))
        .WillOnce(
            DoAll(testAccessIsForbidden, respondWith(inventory)))
        .WillOnce(DoAll(testHasAccess, respondWith(samples::EMPTY_INVENTORY)))
        .WillOnce(DoAll(
            testAccessIsForbidden, notify, respondWith(samples::EMPTY_INVENTORY)))
        .WillRepeatedly(respondWith(samples::EMPTY_INVENTORY));

    using namespace std::literals;
    lazyAgent.emplace(
        Agent::Configuration{.inventoryFetchInterval = 0s},
        Agent::Context{.connectionPool = &connectionPool_});
    ASSERT_EQ(future.wait_for(10s), std::future_status::ready);
}

TEST_F(AgentFixture, BackendRequestUrlAndTvm)
{
    BackendStub backendStub{{
        {samples::SOME_APIKEY, samples::SOME_SECRET},
        {samples::ANOTHER_APIKEY, samples::ANOTHER_SECRET}
    }};
    TvmTicketProvider ticketStub = [] { return "PlushServiceTicket"; };

    Agent agent{
        {.inventoryUrl = "http://oh.my/v1/provider/inventory"},
        {.connectionPool = &backendStub, .ticketProvider = ticketStub}
    };

    EXPECT_THAT(
        backendStub.receivedRequests(),
        UnorderedElementsAre(
            Pair(
                "http://oh.my/v1/provider/inventory",
                ElementsAre(HasSubstr(fmt::format("{}: {}", auth::SERVICE_TICKET_HEADER, ticketStub())))
            )
        )
    );
}

} // namespace maps::apiteka::tests
