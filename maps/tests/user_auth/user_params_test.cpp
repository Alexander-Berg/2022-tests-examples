#include <maps/infra/yacare/include/metrics.h>
#include <maps/infra/yacare/include/params/tvm.h>
#include <maps/infra/yacare/include/test_utils.h>
#include <maps/infra/yacare/include/tvm.h>

#include <maps/libs/json/include/builder.h>
#include <maps/libs/http/include/http.h>
#include <maps/libs/http/include/test_utils.h>

#include <library/cpp/testing/unittest/registar.h>
#include <library/cpp/testing/gmock_in_unittest/gmock.h>

#include <format>
#include <sstream>

namespace yacare::tests {

using maps::http::URL;
using maps::http::GET;
using maps::http::MockRequest;
using maps::http::MockResponse;

constexpr auto RECIPE_TVM_CONFIG_FILE = "maps/infra/yacare/tests/user_auth/tvmtool.recipe.conf";
// Configures yacare auth with mock Blackbox and TvmTool recipe
class AuthBlackboxFixture
{
    UserAuthBlackboxFixture fix_;
public:
    explicit AuthBlackboxFixture(
        const std::vector<tvm::AuthMethod>& methods = tvm::DEFAULT_AUTH_METHODS,
        std::function<MockResponse(const MockRequest& request)> mockResponder = {})
        : fix_(RECIPE_TVM_CONFIG_FILE, methods, std::move(mockResponder)) {}

    static std::string_view blackboxUrl()
    {
        return yacare::tests::UserAuthBlackboxFixture::blackbox().environment().url;
    }
};

class AuthTvmFixture : public UserAuthTvmOnlyFixture
{
public:
    AuthTvmFixture() : UserAuthTvmOnlyFixture(RECIPE_TVM_CONFIG_FILE)
    {
    }
};

// Unittest ticket (env = prod, uid = 153, scopes = "plush:scope,maps:personalization")
constexpr auto VALID_USER_TICKET =
    "3:user:CAsQ__________9_GjMKAwiZARCZARoUbWFwczpwZXJzb25hbGl"
    "6YXRpb24aC3BsdXNoOnNjb3BlINKF2MwEKAA:GNgEZYzV_oXsDnIIeKTai"
    "xN2uwNYAIKkpr2kPVh5axWkis2DdLUUh9dl1TAcBHN9sJHxJeOREfCraRFv"
    "GY3JtPVimTmw5XZtJXmYwIkTxyaxySNMGw5b4AGqyb1ZSREyx-"
    "Pan6Up5CFOAQYcIkfsA5RUuf6vCbDK5oANxhgyrW0";

Y_UNIT_TEST_SUITE(user_auth_params) {

Y_UNIT_TEST(test_no_auth_headers)
{
    {  // User parameter
        MockRequest request(GET, URL("http://localhost/user"));
        EXPECT_EQ(yacare::performTestRequest(request).status, 401);
    }
    {  // UserInfo parameter
        MockRequest request(GET, URL("http://localhost/user_info"));
        EXPECT_EQ(yacare::performTestRequest(request).status, 401);
    }
    {  // optional maybeUser param
        MockRequest request(GET, URL("http://localhost/maybeUser"));
        auto response = yacare::performTestRequest(request);
        EXPECT_EQ(response.status, 200);
        EXPECT_EQ(response.body, "no userId");
    }
}

Y_UNIT_TEST(test_blackbox_error_500)
{
    AuthBlackboxFixture mockAuth(
        tvm::DEFAULT_AUTH_METHODS,
        [](const MockRequest&) {
            return MockResponse::withStatus(503);
        });
    MockRequest request(GET, URL("http://localhost/user"));
    request.headers = {
        { "Authorization", "OAuth b1070fac00ce46388a2f2645dfff09c6" }
    };

    auto response = yacare::performTestRequest(request);
    // Expect 500 from servant on blackbox err
    EXPECT_EQ(response.status, 500);
}

Y_UNIT_TEST(test_user_with_oauth)
{
    MockRequest request(GET, URL("http://localhost/user"));
    request.headers = {
        { "Authorization", "OAuth b1070fac00ce46388a2f2645dfff09c6" }
    };

    // 500 if builtin user auth not configured
    EXPECT_EQ(yacare::performTestRequest(request).status, 500);

    {   // OAuth not supported in configuration with TVM only (no blackbox->only UserTicket method)
        AuthTvmFixture mockAuth;
        EXPECT_EQ(yacare::performTestRequest(request).status, 401);
    }

    {
        // Remove needed resolve method(tvm::AuthMethod::OAuth)
        AuthBlackboxFixture mockAuth(
            {tvm::AuthMethod::UserTicket, tvm::AuthMethod::SessionId},
            [](const MockRequest&) {
                return MockResponse::fromArcadia("maps/libs/auth/tests/responses/oauth.good");
            });
        EXPECT_EQ(yacare::performTestRequest(request).status, 401);
    }
    {
        // Leave only needed method
        AuthBlackboxFixture mockAuth(
            {tvm::AuthMethod::OAuth},
            [](const MockRequest&) {
                return MockResponse::fromArcadia("maps/libs/auth/tests/responses/oauth.good");
            });
        auto response = yacare::performTestRequest(request);
        EXPECT_EQ(response.status, 200);
        EXPECT_EQ(response.body, "uid=111111111");
    }
}

Y_UNIT_TEST(test_user_with_sessionid)
{
    MockRequest request(GET, URL("http://localhost/user"));
    request.headers = {
        { "Cookie", "someCookie=value; Session_id=3:valid:sessioncookie; otherCookie=value" },
        { "Host", "yandex.ru" }
    };

    // 500 if builtin user auth not configured
    EXPECT_EQ(yacare::performTestRequest(request).status, 500);

    {   // Session cookie not supported in configuration with TVM only (no blackbox->only UserTicket method)
        AuthTvmFixture mockAuth;
        EXPECT_EQ(yacare::performTestRequest(request).status, 401);
    }

    {
        // Remove needed resolve method(tvm::AuthMethod::SessionId)
        AuthBlackboxFixture mockAuth(
            {tvm::AuthMethod::UserTicket, tvm::AuthMethod::OAuth},
            [](const MockRequest&) {
                return MockResponse::fromArcadia("maps/libs/auth/tests/responses/sessionid.good");
            });
        EXPECT_EQ(yacare::performTestRequest(request).status, 401);
    }
    {
        // Leave only needed method
        AuthBlackboxFixture mockAuth(
            {tvm::AuthMethod::SessionId},
            [](const MockRequest&) {
                return MockResponse::fromArcadia("maps/libs/auth/tests/responses/sessionid.good");
            });
        auto response = yacare::performTestRequest(request);
        EXPECT_EQ(response.status, 200);
        EXPECT_EQ(response.body, "uid=4001517835");
    }
}

Y_UNIT_TEST(test_user_info_with_ticket)
{
    MockRequest request(GET, URL("http://localhost/user_info"));
    request.headers = {{"X-Ya-User-Ticket", VALID_USER_TICKET}};

    // 500 if builtin user auth not configured
    EXPECT_EQ(yacare::performTestRequest(request).status, 500);

    {   // UserInfo fails in configuration with TVM only
        AuthTvmFixture mockAuth;
        EXPECT_EQ(yacare::performTestRequest(request).status, 500);
    }

    {
        // Remove needed resolve method(tvm::AuthMethod::UserTicket)
        AuthBlackboxFixture mockAuth(
            {tvm::AuthMethod::SessionId, tvm::AuthMethod::OAuth},
            [](const MockRequest&) {
                return MockResponse::fromArcadia("maps/libs/auth/tests/responses/user_ticket.good");
            });
        EXPECT_EQ(yacare::performTestRequest(request).status, 401);
    }
    {
        // Leave only needed method
        AuthBlackboxFixture mockAuth(
            {tvm::AuthMethod::UserTicket},
            [](const MockRequest&) {
                return MockResponse::fromArcadia("maps/libs/auth/tests/responses/user_ticket.good");
            });
        auto response = yacare::performTestRequest(request);
        EXPECT_EQ(response.status, 200);
        EXPECT_EQ(response.body, "userinfo: {uid:3000062912, login:test}");
    }
}

Y_UNIT_TEST(test_maybeuser_with_ticket)
{
    auto goodTicketTest = [] {
        MockRequest request(GET, URL("http://localhost/maybeUser"));
        request.headers = {{"X-Ya-User-Ticket", VALID_USER_TICKET}};
        auto response = yacare::performTestRequest(request);
        EXPECT_EQ(response.status, 200);
        EXPECT_EQ(response.body, "uid=153");
    };
    auto badTicketTest = [] {
        MockRequest request(GET, URL("http://localhost/maybeUser"));
        request.headers = {{"X-Ya-User-Ticket", "3:InvalidTicket"}};
        auto response = yacare::performTestRequest(request);
        EXPECT_EQ(response.status, 200);
        EXPECT_EQ(response.body, "no userId");
    };

    {   // User tickets accepted in configuration with TVM only
        AuthTvmFixture mockAuth;
        goodTicketTest();
        badTicketTest();
    }
    {   // User tickets accepted in configuration with Blackbox
        AuthBlackboxFixture mockAuth;
        goodTicketTest();
        badTicketTest();
    }
}

namespace {
struct ScopesPolicyFixture
{
    ScopesPolicyFixture(yacare::tvm::ScopesPolicy policy)
        : oldPolicy(yacare::tvm::userScopesPolicy())
    {
        yacare::tvm::setUserScopesPolicy(std::move(policy));
    }
    ~ScopesPolicyFixture()
    {
        yacare::tvm::setUserScopesPolicy(std::move(oldPolicy));
    }
    yacare::tvm::ScopesPolicy oldPolicy;
};
}  // anonymous namespace

Y_UNIT_TEST(user_scopes_with_oauth)
{
    AuthBlackboxFixture mockAuth(
        tvm::DEFAULT_AUTH_METHODS,
        [](const MockRequest&) {
            return MockResponse::fromArcadia("maps/libs/auth/tests/responses/oauth.good");
        });

    MockRequest request(GET, URL("http://localhost/user"));
    request.headers = {
        { "Authorization", "OAuth b1070fac00ce46388a2f2645dfff09c6" }
    };

    {   // reject 'cause scope is missing
        ScopesPolicyFixture fixture(yacare::tvm::ScopesPolicy({"nosuch:scope"}));
        auto response = yacare::performTestRequest(request);
        EXPECT_EQ(response.status, 403);
    }
    {   // allowed by mobile:all rule
        ScopesPolicyFixture fixture(
            yacare::tvm::ScopesPolicy({"nosuch:scope"}) ||
            yacare::tvm::ScopesPolicy({"mobile:all"})
        );
        auto response = yacare::performTestRequest(request);
        EXPECT_EQ(response.status, 200);
        EXPECT_EQ(response.body, "uid=111111111");
    }
    {   // dry-run
        auto policy = yacare::tvm::ScopesPolicy({"nosuch:scope"})
                || yacare::tvm::ScopesPolicy({"fake:scope", "another:one"})
                || yacare::tvm::ScopesPolicy::dryRunMode();
        EXPECT_TRUE(policy.dryRun);

        ScopesPolicyFixture fixture(policy);
        auto response = yacare::performTestRequest(request);
        EXPECT_EQ(response.status, 200);
    }
    {   // rule with no scopes is ignored
        yacare::tvm::ScopesPolicy noScopes({});
        EXPECT_TRUE(noScopes.rules.empty());

        ScopesPolicyFixture fixture(noScopes);
        auto response = yacare::performTestRequest(request);
        EXPECT_EQ(response.status, 200);
    }
}

Y_UNIT_TEST(maybeuser_scopes_with_ticket)
{
    AuthTvmFixture fixture;  // Auth configured with tvm only

    MockRequest request(GET, URL("http://localhost/maybeUser"));
    request.headers = {{"X-Ya-User-Ticket", VALID_USER_TICKET}};

    {   // Missing one of required scopes
        ScopesPolicyFixture fixture(
            yacare::tvm::ScopesPolicy({"nosuch:scope", "maps:personalization"}));
        auto response = yacare::performTestRequest(request);
        EXPECT_EQ(response.status, 200);
        EXPECT_EQ(response.body, "no userId");  // no user, 'cause of failed scopes validation
    }

    {   // Correct scope
        ScopesPolicyFixture fixture(yacare::tvm::ScopesPolicy({"maps:personalization"}));
        auto response = yacare::performTestRequest(request);
        EXPECT_EQ(response.status, 200);
        EXPECT_EQ(response.body, "uid=153");
    }
}

Y_UNIT_TEST(auth_metrics)
{
    // Nullify
    yacare::impl::serviceMetrics()["yacare-auth-success"]->yasmMetrics();
    yacare::impl::serviceMetrics()["yacare-auth-unauthorized"]->yasmMetrics();
    yacare::impl::serviceMetrics()["yacare-auth-forbidden"]->yasmMetrics();

    {   // +1 success with mandatory userId
        AuthBlackboxFixture mockAuth(
            {tvm::AuthMethod::OAuth},
            [](const MockRequest&) {
                return MockResponse::fromArcadia("maps/libs/auth/tests/responses/oauth.good");
            });
        MockRequest request(GET, URL("http://localhost/user"));
        request.headers = {
            { "Authorization", "OAuth b1070fac00ce46388a2f2645dfff09c6" }
        };
        EXPECT_EQ(yacare::performTestRequest(request).status, 200);
    }
    {  // +1 unauthorized with mandatory userId (Disabled account)
        AuthBlackboxFixture mockAuth(
            {tvm::AuthMethod::OAuth},
            [](const MockRequest&) {
                return MockResponse::fromArcadia("maps/libs/auth/tests/responses/oauth.disabled");
            });
        MockRequest request(GET, URL("http://localhost/user"));
        request.headers = {
            { "Authorization", "OAuth b1070fac00ce46388a2f2645dfff09c6" }
        };
        EXPECT_EQ(yacare::performTestRequest(request).status, 401);
    }
    {   // +1 forbidden (Invalid scopes)
        AuthBlackboxFixture mockAuth(
            {tvm::AuthMethod::OAuth},
            [](const MockRequest&) {
                return MockResponse::fromArcadia("maps/libs/auth/tests/responses/oauth.good");
            });
        MockRequest request(GET, URL("http://localhost/user"));
        request.headers = {
            { "Authorization", "OAuth b1070fac00ce46388a2f2645dfff09c6" }
        };
        ScopesPolicyFixture fixture(yacare::tvm::ScopesPolicy({"nosuch:scope"}));
        EXPECT_EQ(yacare::performTestRequest(request).status, 403);
    }
    {  // +1 success with optional userId
        AuthBlackboxFixture mockAuth(
            {tvm::AuthMethod::OAuth},
            [](const MockRequest&) {
                return MockResponse::fromArcadia("maps/libs/auth/tests/responses/oauth.good");
            });
        MockRequest request(GET, URL("http://localhost/maybeUser"));
        request.headers = {
            { "Authorization", "OAuth b1070fac00ce46388a2f2645dfff09c6" }
        };
        EXPECT_EQ(yacare::performTestRequest(request).status, 200);
    }
    {  // +1 unauthorized with optional userId (Disabled account)
        AuthBlackboxFixture mockAuth(
            {tvm::AuthMethod::OAuth},
            [](const MockRequest&) {
                return MockResponse::fromArcadia("maps/libs/auth/tests/responses/oauth.disabled");
            });
        MockRequest request(GET, URL("http://localhost/maybeUser"));
        request.headers = {
            { "Authorization", "OAuth b1070fac00ce46388a2f2645dfff09c6" }
        };
        EXPECT_EQ(yacare::performTestRequest(request).status, 200);
    }

    std::stringstream ssMetricsReport;
    maps::yasm_metrics::YasmMetrics().addMetrics(
        yacare::impl::serviceMetrics()["yacare-auth-success"]->yasmMetrics()
    ).addMetrics(
        yacare::impl::serviceMetrics()["yacare-auth-unauthorized"]->yasmMetrics()
    ).addMetrics(
        yacare::impl::serviceMetrics()["yacare-auth-forbidden"]->yasmMetrics()
    ).dump(ssMetricsReport);
    EXPECT_EQ(
        ssMetricsReport.str(),
        R"([["yacare-auth-success_ammv",2],)"
        R"(["yacare-auth-unauthorized_ammv",2],)"
        R"(["yacare-auth-forbidden_ammv",1]])"
    );
}

} // Y_UNIT_TEST_SUITE(user_auth_params)

} // namespace yacare::tests
