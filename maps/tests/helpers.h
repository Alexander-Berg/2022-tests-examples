#pragma once

#include <maps/automotive/libs/idm_roles_manager/i_roles_storage.h>
#include <maps/automotive/libs/idm_roles_manager/i_roles_manager.h>
#include <maps/automotive/libs/interfaces/factory.h>

#include <maps/infra/yacare/include/request.h>

#include <library/cpp/testing/gmock_in_unittest/gmock.h>

namespace maps::automotive::tests {

class MockedDatabase:
    public maps::interfaces::FactoryBaseObject,
    public maps::interfaces::Implements<idm::IRolesStorage>
{
public:
    using maps::interfaces::FactoryBaseObject::FactoryBaseObject;

    MOCK_METHOD(void, addIdmUserRole, (const std::string& login, const std::string& role), (override));
    MOCK_METHOD(void, deleteIdmUserRole, (const std::string& login, const std::string& role), (override));
    MOCK_METHOD(idm::UsersWithRoles, getAllIdmRoles, (), (override));
};

void formRequest(
    yacare::RequestBuilder& builder,
    const std::optional<std::string>& params = std::nullopt);

void formRequest(
    yacare::RequestBuilder& builder,
    const std::string& login,
    const std::string& groupSlug,
    const std::string& roleId);

void checkResponse(
    yacare::Response& response,
    int code,
    const std::string& kind,
    const std::optional<std::string>& msg = std::nullopt);

} // namespace maps::automotive::tests
