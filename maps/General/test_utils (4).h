#pragma once

#include <maps/infra/yacare/include/request.h>
#include <maps/infra/yacare/include/tvm.h>

#include <maps/libs/auth/include/blackbox.h>
#include <maps/libs/auth/include/test_utils.h>
#include <maps/libs/http/include/test_utils.h>

namespace yacare {

maps::http::MockResponse performTestRequest(const maps::http::MockRequest& request);

namespace tests {

// UserAuth fixtures use tvmtool recipe to configure builtin user auth
// with either Blackbox instance or just TTvmClient
class UserAuthBlackboxFixture
{
public:
    explicit UserAuthBlackboxFixture(std::string_view tvmtoolRecipeConfigPath,
        const std::vector<tvm::AuthMethod>& methods = tvm::DEFAULT_AUTH_METHODS,
        std::function<maps::http::MockResponse(const maps::http::MockRequest& request)> mockResponder = {});
    ~UserAuthBlackboxFixture();
    static const maps::auth::BlackboxApi& blackbox();
private:
    struct State;
    std::unique_ptr<State> originalState_;
    std::optional<maps::http::MockHandle> mockHandle_;
};

class UserAuthTvmOnlyFixture
{
public:
    explicit UserAuthTvmOnlyFixture(std::string_view tvmtoolRecipeConfigPath);
    ~UserAuthTvmOnlyFixture();
private:
    struct State;
    std::unique_ptr<State> originalState_;
};

/**
 * Fixture allows passing uid directly through "X-Ya-Tests-User-Id" header
 * Disables blackbox auth
 **/
constexpr auto USER_ID_HEADER = "X-Ya-Tests-User-Id";
class UserIdHeaderFixture
{
public:
    UserIdHeaderFixture();
    ~UserIdHeaderFixture();
private:
    struct State;
    std::unique_ptr<State> originalState_;
};

/**
 *  Fixture replaces blackbox auth with mock func returning userInfo
 *  Disables blackbox auth
 **/
class UserInfoFixture
{
public:
    explicit UserInfoFixture(maps::auth::UserInfo userInfo);
    ~UserInfoFixture();
private:
    maps::auth::UserInfo userInfo_;
    struct State;
    std::unique_ptr<State> originalState_;
};

}  // namespace tests

} //namespace yacare
