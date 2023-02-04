#include <maps/wikimap/mapspro/services/acl/lib/access_checks.h>
#include <maps/wikimap/mapspro/services/acl/lib/exception.h>
#include <maps/wikimap/mapspro/services/acl/lib/save.h>
#include <maps/wikimap/mapspro/libs/acl/include/aclgateway.h>
#include <maps/wikimap/mapspro/libs/acl/include/exception.h>

#include <yandex/maps/wiki/common/json_helpers.h>
#include <maps/libs/json/include/value.h>

#include <yandex/maps/wiki/unittest/arcadia.h>
#include <maps/wikimap/mapspro/libs/acl/tests/gtest_helpers/matchers.h>
#include <maps/wikimap/mapspro/libs/acl/tests/gtest_helpers/printers.h>

#include <library/cpp/testing/gmock_in_unittest/gmock.h>
#include <library/cpp/testing/unittest/env.h>
#include <library/cpp/testing/unittest/registar.h>

#include <boost/lexical_cast.hpp>

namespace maps::wiki::aclsrv::ut {

using namespace testing;
using namespace maps::wiki::acl::ut;

namespace {

using AoisVec = std::vector<acl::Aoi>;
using RolesVec = std::vector<acl::Role>;

class DataCreator {
    const acl::ID NON_EXISTING_USER = 0;

public:
    DataCreator() = default;

    DataCreator(const acl::User& user)
    {
        userUpdateData_.insert_or_assign("uid", json::Value(common::idToJson(user.uid())));
        userUpdateData_.insert_or_assign("id", json::Value(common::idToJson(user.id())));
    }

    operator UserUpdateData() const
    {
        // Copy object, so the internal state is not destroyed
        json::repr::ObjectRepr result(userUpdateData_.begin(), userUpdateData_.end());
        result.emplace("policies", json::Value(policies_));
        return UserUpdateData(json::Value(result));
    }

    UserUpdateData operator()() const
    {
        return operator UserUpdateData();
    }

    DataCreator& uid(acl::UID value)
    {
        userUpdateData_.insert_or_assign("uid", json::Value(common::idToJson(value)));
        return *this;
    }

    DataCreator& id(acl::UID value)
    {
        userUpdateData_.insert_or_assign("id", json::Value(common::idToJson(value)));
        return *this;
    }

    DataCreator& login(const std::string& value)
    {
        userUpdateData_.insert_or_assign("login", json::Value(value));
        return *this;
    }

    DataCreator& status(acl::User::Status value)
    {
        userUpdateData_.insert_or_assign("status", json::Value(boost::lexical_cast<std::string>(value)));
        return *this;
    }

    DataCreator& groupId(acl::ID value)
    {
        policies_.emplace_back(
            toJsonValue("group", toJsonValue("id", common::idToJson(value)))
        );
        return *this;
    }

    DataCreator& groupName(const std::string& value)
    {
        policies_.emplace_back(
            toJsonValue("group", toJsonValue("name", value))
        );
        return *this;
    }

    DataCreator& policy(acl::ID role)
    {
        policies_.emplace_back(
            toJsonValue("role", toJsonValue("id", common::idToJson(role)))
        );
        return *this;
    }

    DataCreator& policy(acl::ID role, acl::ID aoi)
    {
        policies_.emplace_back(
            json::repr::ObjectRepr{
                {"role", toJsonValue("id", common::idToJson(role))},
                {"aoi", toJsonValue("id", common::idToJson(aoi))}
            }
        );
        return *this;
    }

private:
    template<typename ValueType>
    json::Value toJsonValue(const std::string& name, const ValueType& value) {
        return json::Value(
            json::repr::ObjectRepr{
                {name, json::Value(value)}
            }
        );
    }

    json::repr::ObjectRepr userUpdateData_ = {
        {"id", json::Value(common::idToJson(NON_EXISTING_USER))},
        {"login", json::Value("login")}
    };

    std::vector<json::Value> policies_;
};

} // namespace

Y_UNIT_TEST_SUITE_F(save, unittest::ArcadiaDbFixture)
{
    Y_UNIT_TEST(shouldSaveUser)
    {
        const acl::UID AUTHOR = 0xface;
        const acl::UID USER = 0xbee;
        pqxx::connection conn(connectionString());
        {
            pqxx::work txn(conn);
            acl::ACLGateway aclGw(txn);
            auto author = aclGw.createUser(AUTHOR, "author-login", "author-display-name", AUTHOR);
            author.setStaffLogin("author-login");
            aclGw.addIDMRole("author-login", {"acl", "admin"});
            auto mproPermission = aclGw.createRootPermission("mpro");
            auto mproRole = aclGw.createRole("mpro", "mpro");
            mproRole.add(mproPermission);
            aclGw.createPolicy(author, mproRole, aclGw.aoi(0));
            txn.commit();
        }
        {   // Should not update user with an unknown uid
            pqxx::work txn(conn);
            acl::ACLGateway aclGw(txn);

            // If `id` is set then user with corresponding `uid` is updated
            EXPECT_THROW(
                saveUser(
                    aclGw,
                    DataCreator().id(10).uid(USER),
                    AUTHOR,
                    FromTest::True
                ),
                maps::wiki::acl::UserNotExists
            );
        }

        {   // Should create a new user
            pqxx::work txn(conn);
            acl::ACLGateway aclGw(txn);

            // If `id` is not set, a new user is created
            const auto id = saveUser(
                aclGw,
                DataCreator().uid(USER),
                AUTHOR,
                FromTest::True
            );

            const auto user = aclGw.user(USER);
            EXPECT_THAT(user.id(), id);
            EXPECT_THAT(user.uid(), USER);
            EXPECT_THAT(user.createdBy(), AUTHOR);
            EXPECT_THAT(user.modifiedBy(), AUTHOR);
            EXPECT_THAT(user.allPolicies(), IsEmpty());
            EXPECT_THAT(user.groups(), IsEmpty());
            EXPECT_THAT(user.status(), acl::User::Status::Active);
            EXPECT_FALSE(user.currentBan());
            EXPECT_THAT(user.trustLevel(), acl::User::TrustLevel::Novice);
            EXPECT_FALSE(user.unbannedAt());
        }

        {   // Update status
            pqxx::work txn(conn);
            acl::ACLGateway aclGw(txn);

            const auto user = aclGw.createUser(USER, "login", "display name", AUTHOR);

            EXPECT_THROW(
                saveUser(
                    aclGw,
                    DataCreator(user).status(acl::User::Status::Banned),
                    AUTHOR,
                    FromTest::True
                ),
                yacare::errors::BadRequest
            );

            EXPECT_THAT(aclGw.user(user.uid()).status(), acl::User::Status::Active);
            EXPECT_THAT(aclGw.user(user.uid()).trustLevel(), acl::User::TrustLevel::Novice);

            saveUser(
                aclGw,
                DataCreator(user).status(acl::User::Status::Deleted),
                AUTHOR,
                FromTest::True
            );
            EXPECT_THAT(aclGw.user(user.uid()).status(), acl::User::Status::Deleted);

            saveUser(
                aclGw,
                DataCreator(user).status(acl::User::Status::Active),
                AUTHOR,
                FromTest::True
            );
            EXPECT_THAT(aclGw.user(user.uid()).status(), acl::User::Status::Active);
        }

        {   // Should not add to non existent groups
            pqxx::work txn(conn);
            acl::ACLGateway aclGw(txn);

            const auto user = aclGw.createUser(USER, "login", "display name", AUTHOR);

            EXPECT_THROW(
                saveUser(
                    aclGw,
                    DataCreator(user).groupId(1),
                    AUTHOR,
                    FromTest::True
                ),
                acl::GroupNotExists
            );

            EXPECT_THROW(
                saveUser(
                    aclGw,
                    DataCreator(user).groupName("group"),
                    AUTHOR,
                    FromTest::True
                ),
                acl::GroupNotExists
            );

            EXPECT_THAT(aclGw.groups(), IsEmpty());
        }

        {   // Should add/remove to/from groups
            pqxx::work txn(conn);
            acl::ACLGateway aclGw(txn);

            const auto user = aclGw.createUser(USER, "login", "display name", AUTHOR);
            const auto groups = GroupsVec{
                aclGw.createGroup("group 0", "description"),
                aclGw.createGroup("group 1", "description"),
                aclGw.createGroup("group 2", "description"),
                aclGw.createGroup("group 3", "description")
            };

            groups[0].add(user);
            groups[2].add(user);

            saveUser(
                aclGw,
                DataCreator(user)
                    .groupId(groups[0].id())
                    .groupId(groups[1].id())
                    .groupName(groups[3].name()),
                AUTHOR,
                FromTest::True
            );

            EXPECT_THAT(
                aclGw.user(user.uid()).groups(),
                haveNames({groups[0].name(), groups[1].name(), groups[3].name()})
            );
        }

        {   // Should add/remove policy
            pqxx::work txn(conn);
            acl::ACLGateway aclGw(txn);

            const auto user = aclGw.createUser(USER, "login", "display name", AUTHOR);
            const auto roles = RolesVec{
                aclGw.createRole("role 0", "description 0"),
                aclGw.createRole("role 1", "description 1"),
                aclGw.createRole("role 2", "description 2")
            };
            const auto aois = AoisVec{
                aclGw.aoi(10),
                aclGw.aoi(11),
                aclGw.aoi(12)
            };
            aclGw.createPolicy(user, roles[0], aois[0]);
            aclGw.createPolicy(user, roles[2], aois[2]);

            saveUser(
                aclGw,
                DataCreator(user)
                    .policy(roles[0].id(), aois[0].id())
                    .policy(roles[1].id(), aois[1].id()),
                AUTHOR,
                FromTest::True
            );

            EXPECT_THAT(
                aclGw.user(user.uid()).policies(),
                policiesAre({
                    {user.id(), roles[0].id(), aois[0].id()},
                    {user.id(), roles[1].id(), aois[1].id()}
                })
            );
        }

        {   // Should add policy with default AOI
            pqxx::work txn(conn);
            acl::ACLGateway aclGw(txn);

            const auto user = aclGw.createUser(USER, "login", "display name", AUTHOR);
            const auto role = aclGw.createRole("role 0", "");

            saveUser(
                aclGw,
                DataCreator(user).policy(role.id()),
                AUTHOR,
                FromTest::True
            );

            EXPECT_THAT(
                aclGw.user(user.uid()).policies(),
                policiesAre({{user.id(), role.id(), 0}})
            );
        }

        // Currently failed scenarious
        // {   // Should create banned user
        //     pqxx::work txn(conn);
        //     acl::ACLGateway aclGw(txn);

        //     // Create
        //     saveUser(
        //         aclGw,
        //         DataCreator().uid(USER).status(acl::User::Status::Banned),
        //         AUTHOR
        //     );

        //     auto user = aclGw.user(USER);
        //     EXPECT_THAT(user.status(), acl::User::Status::Banned); // An active user created!
        //     EXPECT_THAT(user.trustLevel(), acl::User::TrustLevel::AfterBan);
        // }
    }
}

} // namespace maps::wiki::aclsrv::ut
