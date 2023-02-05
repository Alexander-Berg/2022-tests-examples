#include <maps/wikimap/mapspro/libs/acl/impl/factory.h>
#include <maps/wikimap/mapspro/libs/acl/impl/cluster.h>

#include <maps/wikimap/mapspro/libs/acl/include/aclgateway.h>
#include <maps/wikimap/mapspro/libs/acl/include/check_context.h>
#include <maps/wikimap/mapspro/libs/acl/include/deleted_users_cache.h>
#include <maps/wikimap/mapspro/libs/acl/include/exception.h>
#include <maps/wikimap/mapspro/libs/acl/include/restricted_users.h>
#include <maps/wikimap/mapspro/libs/acl/include/subject_path.h>

#include <maps/libs/chrono/include/time_point.h>
#include <maps/libs/concurrent/include/scoped_guard.h>

#include <yandex/maps/wiki/unittest/localdb.h>
#include <yandex/maps/wiki/unittest/query_helpers.h>

#include <boost/format.hpp>
#include <boost/none.hpp>
#include <boost/test/unit_test.hpp>
#include <ctime>
#include <fstream>
#include <iostream>

namespace maps::wiki {

using namespace acl;
using namespace std::chrono_literals;

unittest::MapsproDbFixture& db()
{
    static unittest::MapsproDbFixture db;
    return db;
}

struct DatabaseManager
{
    DatabaseManager()
        : connection(db().connectionString())
        , queryHelpers(connection)
    {}

    pqxx::connection connection;
    unittest::QueryHelpers queryHelpers;
};

BOOST_FIXTURE_TEST_SUITE(acl_tests, DatabaseManager)

namespace {

void
printPermission(
    const Permissions& allPermissions,
    const Permission& p,
    std::ostream& os,
    size_t indent)
{
    std::string indentStr;
    indentStr.assign(indent, '-');
    os << indentStr << p.name() << '|';
    for (const auto& c : allPermissions.children(p)) {
        printPermission(allPermissions, c, os, indent + 2);
    }
}

std::string
printPermissions(ACLGateway& acl)
{
    auto allPermissions = acl.allPermissions();
    std::ostringstream os;
    for (const auto& r : allPermissions.roots()) {
        printPermission(allPermissions, r, os, 0);
    }
    return os.str();
}

size_t
scheduledObjectsSize(const ScheduledObjects& objects)
{
    return objects.policies.size() + objects.groups.size();
}

} // namespace

BOOST_AUTO_TEST_CASE(acl_user_unbanned_at)
{
    const UID userId = 1;
    const UID authorId = 1;
    const std::string reason;

    pqxx::work work(connection);
    ACLGateway acl(work);

    BOOST_CHECK(!acl.createUser(userId, "Login", "My name is", authorId).unbannedAt());

    std::string expires = "2010-10-10 10:10:10+00:00";
    acl.banUser(userId, authorId, expires, reason);
    BOOST_REQUIRE(acl.user(userId).unbannedAt());
    BOOST_CHECK_EQUAL(chrono::formatSqlDateTime(*acl.user(userId).unbannedAt()), expires);

    expires = "2110-10-10 10:10:10+00:00";
    acl.banUser(userId, authorId, expires, reason);
    BOOST_REQUIRE(acl.user(userId).unbannedAt());
    BOOST_CHECK_EQUAL(chrono::formatSqlDateTime(*acl.user(userId).unbannedAt()), expires);

    acl.unbanUser(userId, authorId);
    auto unbannedAt = acl.user(userId).unbannedAt();
    const auto now = std::chrono::system_clock::now();
    BOOST_REQUIRE(unbannedAt);
    BOOST_CHECK(
        *unbannedAt < now + 1s &&
        *unbannedAt > now - 1s
    );
}

BOOST_AUTO_TEST_CASE(acl_recently_unbanned_users)
{
    queryHelpers.applyQuery(
        "INSERT INTO acl.user (uid, login, created_by, modified_by) VALUES "
        "(0, 'superuser', 0, 0),"
        "(1, 'long ago banned, ban expired long ago', 0, 0),"
        "(2, 'long ago banned, ban expired recently', 0, 0),"
        "(3, 'long ago banned, ban has not experied yet', 0, 0),"
        "(4, 'recently banned, ban experied recently', 0, 0),"
        "(5, 'recently banned, ban has not experied yet', 0, 0),"
        "(6, 'long ago unbanned', 0, 0),"
        "(7, 'recently unbanned', 0, 0),"
        "(8, 'not banned at all', 0, 0);"

        "INSERT INTO acl.ban_record (br_id, br_uid, br_action, br_created, br_created_by, br_expires) VALUES "
        "(1, 0, 'ban', NOW() - '1 year'::interval, 0, NOW() + '1 year'::interval),"
        "(2, 0, 'ban', NOW() - '1 year'::interval, 0, NOW() - '1 hour'::interval),"
        "(3, 0, 'ban', NOW() - '1 year'::interval, 0, NOW() + '1 hour'::interval),"
        "(4, 0, 'ban', NOW() - '2 hour'::interval, 0, NOW() - '1 hour'::interval),"
        "(5, 0, 'ban', NOW() - '2 hour'::interval, 0, NOW() + '1 hour'::interval),"
        "(6, 0, 'unban', NOW() - '1 year'::interval, 0, NULL),"
        "(7, 0, 'unban', NOW() - '1 hour'::interval, 0, NULL);"

        "UPDATE acl.user SET current_ban_id = uid WHERE uid >= 1 AND uid <= 7;"
    );

    concurrent::ScopedGuard guard(
        [this]() {
            queryHelpers.applyQuery(
                "TRUNCATE acl.user CASCADE;"
                "TRUNCATE acl.ban_record CASCADE;"
            );
        }
    );

    pqxx::work txn(connection);
    auto uids = ACLGateway(txn).recentlyUnbannedUsers(24h);
    std::sort(uids.begin(), uids.end());
    BOOST_CHECK(uids == std::vector<UID>({2, 4, 7}));
}

BOOST_AUTO_TEST_CASE(acl_recently_unbanned_users_ban_after_unban)
{
    queryHelpers.applyQuery(
        "INSERT INTO acl.user (uid, login, created_by, modified_by) VALUES "
        "(0, 'superuser', 0, 0),"
        "(1, 'ban unban ban', 0, 0),"
        "(2, 'ban unban', 0, 0);"
    );

    concurrent::ScopedGuard guard(
        [this]() {
            queryHelpers.applyQuery(
                "TRUNCATE acl.user CASCADE;"
            );
        }
    );

    pqxx::work txn(connection);
    ACLGateway acl(txn);

    acl.banUser(1, 0, "2111-11-11 11:11", "");
    acl.banUser(2, 0, "2111-11-11 11:11", "");
    acl.unbanUser(1, 0);
    acl.unbanUser(2, 0);
    acl.banUser(1, 0, "2111-11-11 11:11", "");

    BOOST_CHECK(
        ACLGateway(txn).recentlyUnbannedUsers(24h) ==
        std::vector<UID>({2})
    );
}

BOOST_AUTO_TEST_CASE(acl_create_permissions_tree)
{
    {
        pqxx::work work(connection);
        ACLGateway acl(work);
        Permission projectPermission = acl.createRootPermission("mpro");
        Permission editorServicePermission =
            projectPermission.createChildPermission("editor");
        BOOST_CHECK_NO_THROW(editorServicePermission.createChildPermission("rd_jc"));
        BOOST_CHECK_NO_THROW(editorServicePermission.createChildPermission("aoi"));
        Permission rdElPermission = editorServicePermission.createChildPermission("rd_el");
        BOOST_CHECK_NO_THROW(rdElPermission.createChildPermission("attrs"));
        BOOST_CHECK_NO_THROW(rdElPermission.createChildPermission("geom"));
        BOOST_CHECK_NO_THROW(projectPermission.createChildPermission("tasks"));
        work.commit();
    }
    {
        pqxx::work work(connection);
        ACLGateway acl(work);
        auto allPermissions = acl.allPermissions();

        BOOST_CHECK_EQUAL(allPermissions.roots().size(), 1);
        BOOST_CHECK_NO_THROW(acl.rootPermission("mpro"));
        BOOST_CHECK_THROW(acl.rootPermission("nonexistent"), PermissionNotExists);
        BOOST_CHECK_EQUAL(printPermissions(acl),
            "mpro|--editor|----rd_jc|----aoi|----rd_el|------attrs|------geom|--tasks|");
        BOOST_CHECK_THROW(acl.permission(SubjectPath("")), BadParam);
        BOOST_CHECK_THROW(acl.permission(SubjectPath("mpro")("editor")("error")),
            PermissionNotExists);

        const std::string CAT_AOI = "aoi";
        const auto pathAoi = SubjectPath("mpro")("editor")(CAT_AOI);
        BOOST_CHECK_NO_THROW(acl.permission(pathAoi));

        auto permission = acl.permission(pathAoi);
        BOOST_CHECK_EQUAL(permission.name(), CAT_AOI);
        BOOST_CHECK_EQUAL(acl.permission(permission.id()).name(), CAT_AOI);

        auto path = allPermissions.path(permission);
        BOOST_CHECK_EQUAL(path.size(), 3);
        BOOST_CHECK_EQUAL(path.front(), "mpro");
        BOOST_CHECK_EQUAL(path.back(), CAT_AOI);

        BOOST_CHECK_EQUAL(allPermissions.permission(pathAoi.pathParts()).name(), CAT_AOI);

        auto mpro = acl.rootPermission("mpro");
        mpro.updateLeafsIds(acl);
        BOOST_CHECK_EQUAL(mpro.leafIds().size(), 5);
        BOOST_CHECK_EQUAL(mpro.childPermission("tasks").leafIds().size(), 1);
        BOOST_CHECK_EQUAL(mpro.childPermission("editor").leafIds().size(), 4);
    }
}

BOOST_AUTO_TEST_CASE(acl_create_users)
{
    pqxx::work work(connection);
    ACLGateway acl(work);
    BOOST_CHECK_NO_THROW(acl.createUser(190345, "SUPER.TROOPER", "My name is SUPER.TROOPER", 77777));
    BOOST_CHECK_NO_THROW(acl.createUser(190341, "PARATROOPER", "My name is PARATROOPER", 77777));
    BOOST_CHECK_NO_THROW(acl.createUser(190337, "ÜBERTROOPER", "My name is ÜBERTROOPER", 77777));
    User user1 = acl.user(190345);
    User user2 = acl.user(190341);
    BOOST_CHECK(user1.id() != user2.id());
    BOOST_CHECK_EQUAL(user1.login(), "SUPER.TROOPER");
    BOOST_CHECK_EQUAL(user1.displayName(), "My name is SUPER.TROOPER");
    BOOST_CHECK_EQUAL(user1.status(), User::Status::Active);
    std::vector<UID> uids;
    uids.push_back(user1.uid());
    uids.push_back(user2.uid());
    BOOST_CHECK_EQUAL(acl.users(uids).size(), 2);

    std::vector<ID> ids;
    ids.push_back(user1.id());
    ids.push_back(user2.id());
    BOOST_CHECK_EQUAL(acl.usersByIds(ids).size(), 2);
    work.commit();
}

BOOST_AUTO_TEST_CASE(acl_drop_user)
{
    pqxx::work work(connection);
    ACLGateway acl(work);
    BOOST_CHECK_NO_THROW(acl.createUser(19034435, "TESTSUSER", "My name is SUPER.TROOPER", 77777));
    BOOST_CHECK_NO_THROW(acl.user(19034435));
    BOOST_CHECK_NO_THROW(acl.drop(acl.user(19034435)));
    BOOST_CHECK_THROW(acl.user(19034435), UserNotExists);
    work.commit();
}

BOOST_AUTO_TEST_CASE(acl_create_duplicate_users)
{
    pqxx::work work(connection);
    ACLGateway acl(work);
    BOOST_CHECK_THROW(acl.createUser(190345, "PARATROOPER", "My name is PARATROOPER", 77777), DuplicateUser);
}

BOOST_AUTO_TEST_CASE(acl_set_user_status)
{
    pqxx::work work(connection);
    ACLGateway acl(work);
    BOOST_CHECK_NO_THROW(acl.user("PARATROOPER").setDeleted(User::DeleteReason::User, 1111));
    BOOST_CHECK_EQUAL(acl.user("PARATROOPER").status(), User::Status::Deleted);
    BOOST_CHECK_EQUAL(*acl.user("PARATROOPER").deleteReason(), User::DeleteReason::User);
    BOOST_CHECK_NO_THROW(acl.user("PARATROOPER").setActive(1111));
    BOOST_CHECK_EQUAL(acl.user("PARATROOPER").status(), User::Status::Active);
}

BOOST_AUTO_TEST_CASE(acl_deleted_users_cache)
{
    pqxx::work work(connection);

    const UID AUTHOR_UID = 1111;

    DeletedUsersCache deletedUsersCache;
    BOOST_CHECK(deletedUsersCache.isAllowed(AUTHOR_UID));

    deletedUsersCache.update(work);
    BOOST_CHECK(deletedUsersCache.isAllowed(AUTHOR_UID));

    ACLGateway acl(work);
    auto user = acl.user("PARATROOPER");
    auto uid = user.uid();
    BOOST_CHECK(deletedUsersCache.isAllowed(uid));

    BOOST_CHECK_NO_THROW(user.setDeleted(User::DeleteReason::User, AUTHOR_UID));
    BOOST_CHECK_NO_THROW(deletedUsersCache.update(work));
    BOOST_CHECK(!deletedUsersCache.isAllowed(uid));
    BOOST_CHECK_THROW(deletedUsersCache.checkUser(uid), AccessDenied);

    BOOST_CHECK_NO_THROW(user.setActive(AUTHOR_UID));
    BOOST_CHECK_NO_THROW(deletedUsersCache.update(work));
    BOOST_CHECK(deletedUsersCache.isAllowed(uid));
    BOOST_CHECK_NO_THROW(deletedUsersCache.checkUser(uid));
}

BOOST_AUTO_TEST_CASE(acl_bans_bad)
{
    {
        pqxx::work work(connection);
        ACLGateway acl(work);
        auto uid = acl.user("ÜBERTROOPER").uid();
        BOOST_CHECK_THROW(acl.banUser(1111, uid, "", ""), UserNotExists);
    }
    {
        pqxx::work work(connection);
        ACLGateway acl(work);
        auto uid = acl.user("ÜBERTROOPER").uid();
        BOOST_CHECK_THROW(acl.banUser(uid, 1111, "", ""), UserNotExists);
    }
}

BOOST_AUTO_TEST_CASE(acl_bans)
{
    UID uid = 0;
    UID authorUid = 0;
    UID otherUid = 0;

    {
        pqxx::work work(connection);
        ACLGateway acl(work);
        uid = acl.user("ÜBERTROOPER").uid();
        authorUid = acl.user("SUPER.TROOPER").uid();
        otherUid = acl.user("PARATROOPER").uid();
    }

    auto checkBan = [&](
            UID uid, UID author,
            const std::string& expires, const std::string& reason)
    {
        pqxx::work work(connection);
        ACLGateway acl(work);
        auto banRecord = acl.banUser(uid, author, expires, reason);
        BOOST_CHECK_EQUAL(banRecord.uid(), uid);
        BOOST_CHECK_EQUAL(banRecord.action(), BanRecord::Action::Ban);
        BOOST_CHECK_EQUAL(banRecord.createdBy(), author);
        BOOST_CHECK_EQUAL(banRecord.expires(), expires);
        BOOST_CHECK_EQUAL(banRecord.reason(), reason);

        work.commit();
        return banRecord;
    };

    auto compareCurrentBan = [this](UID uid, const BanRecord& banRecord)
    {
        pqxx::work work(connection);
        ACLGateway acl(work);
        auto user = acl.user(uid);

        BOOST_CHECK_EQUAL(user.status(), User::Status::Banned);
        BOOST_REQUIRE(user.currentBan());
        const auto& currentBan = *user.currentBan();
        BOOST_CHECK_EQUAL(currentBan.uid(), banRecord.uid());
        BOOST_CHECK_EQUAL(currentBan.action(), banRecord.action());
        BOOST_CHECK_EQUAL(currentBan.createdBy(), banRecord.createdBy());
        BOOST_CHECK_EQUAL(currentBan.expires(), banRecord.expires());
        BOOST_CHECK_EQUAL(currentBan.reason(), banRecord.reason());
    };

    auto checkNotBanned = [this](UID uid)
    {
        pqxx::work work(connection);
        ACLGateway acl(work);
        auto user = acl.user(uid);

        BOOST_CHECK_EQUAL(user.status(), User::Status::Active);
        BOOST_CHECK(!user.currentBan());
    };

    checkNotBanned(uid);

    auto banRecord = checkBan(uid, authorUid, "", "");
    compareCurrentBan(uid, banRecord);

    // Expiry date is intentionnaly in the past
    checkBan(uid, otherUid, "2015-02-15 15:57:25.978911+00", "");
    checkNotBanned(uid);

    banRecord = checkBan(uid, authorUid, "2040-02-29 09:13:14+00", "blah");
    compareCurrentBan(uid, banRecord);

    {
        pqxx::work work(connection);
        ACLGateway acl(work);
        auto user = acl.user(uid);

        auto unbanRecord = acl.unbanUser(uid, otherUid);
        BOOST_CHECK_EQUAL(unbanRecord.uid(), uid);
        BOOST_CHECK_EQUAL(unbanRecord.action(), BanRecord::Action::Unban);
        BOOST_CHECK_EQUAL(unbanRecord.createdBy(), otherUid);
        work.commit();
    }
    checkNotBanned(uid);

    banRecord = checkBan(uid, authorUid, "", "meh");
    compareCurrentBan(uid, banRecord);

    {
        pqxx::work work(connection);
        ACLGateway acl(work);
        BOOST_CHECK_EQUAL(acl.banRecords(uid, 0, 0).value().size(), 5);
    }
}

BOOST_AUTO_TEST_CASE(acl_ban_history)
{
    pqxx::work work(connection);
    ACLGateway acl(work);
    auto uid = acl.user("ÜBERTROOPER").uid();

    auto banRecords = acl.banRecords(uid, 0, 0);
    {
        BOOST_REQUIRE(!banRecords.value().empty());
        BOOST_CHECK_EQUAL(
            banRecords.pager().totalCount(),
            banRecords.value().size());
        for (auto it = std::next(std::begin(banRecords.value()));
                it != std::end(banRecords.value());
                ++it)
        {
            BOOST_CHECK_GT(std::prev(it)->id(), it->id());
        }
    }
    {
        BOOST_REQUIRE_GE(banRecords.value().size(), 4);
        auto banPage = acl.banRecords(uid, 2, 2);
        BOOST_CHECK_EQUAL(
            banPage.pager().totalCount(),
            banRecords.pager().totalCount());
        BOOST_CHECK_EQUAL(banPage.pager().page(), 2);
        BOOST_CHECK_EQUAL(banPage.pager().perPage(), 2);

        BOOST_REQUIRE_EQUAL(banPage.value().size(), 2);
        BOOST_CHECK_EQUAL(
            banPage.value()[0].id(),
            banRecords.value()[2].id());
        BOOST_CHECK_EQUAL(
            banPage.value()[1].id(),
            banRecords.value()[3].id());
    }
}

BOOST_AUTO_TEST_CASE(acl_user_trust_level)
{
    pqxx::work work(connection);
    ACLGateway acl(work);
    auto bannedUser = acl.user("ÜBERTROOPER");
    auto noviceUser = acl.user("PARATROOPER");

    BOOST_CHECK_EQUAL(bannedUser.trustLevel(), User::TrustLevel::AfterBan);
    BOOST_CHECK_EQUAL(noviceUser.trustLevel(), User::TrustLevel::Novice);
}

BOOST_AUTO_TEST_CASE(acl_users_order)
{
    UID uid1 = 0;
    UID uid2 = 0;
    {
        pqxx::work work(connection);
        ACLGateway acl(work);
        User u1 = acl.user("SUPER.TROOPER");
        uid1 = u1.uid();
        u1.setDeleted(User::DeleteReason::User, 1111);
        work.commit();
    }
    {
        pqxx::work work(connection);
        ACLGateway acl(work);
        User u2 = acl.user("PARATROOPER");
        uid2 = u2.uid();
        u2.setDeleted(User::DeleteReason::User, 1111);
        work.commit();
    }

    std::vector<UID> userIds{uid1, uid2, uid1};

    pqxx::work work(connection);
    ACLGateway acl(work);
    auto users = acl.users(userIds);
    BOOST_REQUIRE_EQUAL(users.size(), 2);
    BOOST_CHECK_EQUAL(users[0].uid(), uid2);
    BOOST_CHECK_EQUAL(users[1].uid(), uid1);
    for (auto& user : users) {
        user.setActive(1111);
    }
    work.commit();
}

BOOST_AUTO_TEST_CASE(acl_find_user)
{
    pqxx::work work(connection);
    ACLGateway acl(work);
    BOOST_CHECK_EQUAL(acl.user(190345).login(), "SUPER.TROOPER");
    BOOST_CHECK_EQUAL(acl.user("SUPER.TROOPER").uid(), 190345);
    BOOST_CHECK_EQUAL(acl.user("super.trooper").uid(), 190345);
    BOOST_CHECK_EQUAL(acl.user("super-trooper").uid(), 190345);
    BOOST_CHECK_THROW(acl.user("SUPER___OPER"), UserNotExists);
}

BOOST_AUTO_TEST_CASE(acl_create_role)
{
    {
        pqxx::work work(connection);
        ACLGateway acl(work);
        BOOST_CHECK_NO_THROW(acl.createRole("admin", "admin description",
            Role::Privacy::Private));
        BOOST_CHECK(acl.role("admin").privacy() == Role::Privacy::Private);
        BOOST_CHECK_NO_THROW(acl.createRole("poweruser", "power user description",
            Role::Privacy::Public));
        BOOST_CHECK(acl.role("poweruser").privacy() == Role::Privacy::Public);
        work.commit();
    }
    {
        pqxx::work work(connection);
        ACLGateway acl(work);
        BOOST_CHECK_THROW(acl.createRole("poweruser", "power user new description",
            Role::Privacy::Public), DuplicateRole);
    }
    {
        pqxx::work work(connection);
        ACLGateway acl(work);
        BOOST_CHECK_NO_THROW(acl.role("poweruser"));
        BOOST_CHECK_EQUAL(acl.role("poweruser").name(), "poweruser");
        BOOST_CHECK_EQUAL(acl.role("poweruser").description(), "power user description");
        BOOST_CHECK_EQUAL(acl.role(acl.role("poweruser").id()).name(), "poweruser");
        BOOST_CHECK_THROW(acl.role("guest"), RoleNotExists);
    }
}

BOOST_AUTO_TEST_CASE(acl_create_group)
{
    {
        pqxx::work work(connection);
        ACLGateway acl(work);
        auto group1 = acl.createGroup("yandex", "staff");
        auto group2 = acl.createGroup("outsource", "");
        std::vector<ID> ids;
        ids.push_back(group1.id());
        ids.push_back(group2.id());
        BOOST_CHECK_EQUAL(acl.groups(ids).size(), 2);
        work.commit();
    }
    {
        pqxx::work work(connection);
        ACLGateway acl(work);
        BOOST_CHECK_THROW(acl.createGroup("outsource", ""), DuplicateGroup);
    }
    {
        pqxx::work work(connection);
        ACLGateway acl(work);
        BOOST_CHECK_NO_THROW(acl.group("outsource"));
        BOOST_CHECK_EQUAL(acl.group("outsource").name(), "outsource");
        BOOST_CHECK_EQUAL(acl.group(acl.group("outsource").id()).name(), "outsource");
        BOOST_CHECK_THROW(acl.group("aliens"), GroupNotExists);
    }
}

BOOST_AUTO_TEST_CASE(acl_group_user)
{
    {
        pqxx::work work(connection);
        ACLGateway acl(work);
        Group grp = acl.group("yandex");
        User u1 = acl.user("PARATROOPER");
        User u2 = acl.user("SUPER.TROOPER");
        grp.add(u1);
        grp.add(u2);
        work.commit();
    }
    {
        pqxx::work work(connection);
        ACLGateway acl(work);
        BOOST_CHECK_THROW(acl.group("yandex").add(acl.user("PARATROOPER")), DuplicateRelation);
    }
    {
        pqxx::work work(connection);
        ACLGateway acl(work);
        std::vector<User> users = acl.group("yandex").users();
        BOOST_CHECK_EQUAL(users.size(), 2);
        acl.group("yandex").remove(users.back());
        work.commit();
    }
    {
        pqxx::work work(connection);
        ACLGateway acl(work);
        BOOST_CHECK_EQUAL(acl.group("yandex").users().size(), 1);
        BOOST_CHECK_EQUAL(acl.group("yandex").users()[0].groups().size(), 1);
    }
}

BOOST_AUTO_TEST_CASE(acl_role_permission)
{
    {
        pqxx::work work(connection);
        ACLGateway acl(work);
        acl.role("admin").add(acl.permission(SubjectPath("mpro")("editor")));

        std::vector<Permission> permissions;
        permissions.push_back(acl.permission(SubjectPath("mpro")("editor")("rd_el")));
        permissions.push_back(acl.permission(SubjectPath("mpro")("editor")("aoi")));
        acl.role("poweruser").setPermissions(permissions); // first, remove + add 2 permissions
        acl.role("poweruser").setPermissions(permissions); // ok, remove + add 2 permissions
        work.commit();
    }
    {
        pqxx::work work(connection);
        ACLGateway acl(work);
        BOOST_CHECK_EQUAL(acl.role("admin").permissions().size(), 1);
        BOOST_CHECK_EQUAL(acl.role("poweruser").permissions().size(), 2);
    }
    {
        pqxx::work work(connection);
        ACLGateway acl(work);
        auto roles = acl.permittingRoles({SubjectPath("mpro/editor/rd_el")});
        BOOST_CHECK_EQUAL(roles.size(), 2);
        roles = acl.permittingRoles({SubjectPath("mpro/editor")});
        BOOST_CHECK_EQUAL(roles.size(), 1);
        roles = acl.permittingRoles({
            SubjectPath("mpro/editor"),
            SubjectPath("mpro/editor/rd_el")});
        BOOST_CHECK_EQUAL(roles.size(), 1);
    }
}

BOOST_AUTO_TEST_CASE(acl_role_can_assign)
{
    {
        pqxx::work work(connection);
        ACLGateway acl(work);
        BOOST_CHECK_EQUAL(acl.role("admin").canAssignRoles().size(), 0);
        acl.role("admin").addCanAssignRole(acl.role("poweruser"));
        BOOST_CHECK_EQUAL(acl.role("admin").canAssignRoles().size(), 1);
        acl.role("admin").removeCanAssignRole(acl.role("poweruser"));
        BOOST_CHECK_EQUAL(acl.role("admin").canAssignRoles().size(), 0);

        BOOST_CHECK_EQUAL(acl.role("admin").canAssignGroups().size(), 0);
        acl.role("admin").addCanAssignGroup(acl.group("yandex"));
        BOOST_CHECK_EQUAL(acl.role("admin").canAssignGroups().size(), 1);
        acl.role("admin").removeCanAssignGroup(acl.group("yandex"));
        BOOST_CHECK_EQUAL(acl.role("admin").canAssignGroups().size(), 0);
        work.commit();
    }
}

BOOST_AUTO_TEST_CASE(acl_create_policy)
{
    {
        pqxx::work work(connection);
        ACLGateway acl(work);
        acl.createPolicy(acl.group("yandex"), acl.role("admin"),
            Factory::aoi(12, "", "", Deleted::No));
        acl.createPolicy(acl.user("SUPER.TROOPER"), acl.role("admin"),
            Factory::aoi(12, "", "", Deleted::No));
        auto policy = acl.createPolicy(acl.user("PARATROOPER"),
            acl.role("poweruser"),
            Factory::aoi(12, "", "", Deleted::No));
        BOOST_CHECK_EQUAL(policy.role().name(), "poweruser");
        work.commit();
    }
    {
        pqxx::work work(connection);
        ACLGateway acl(work);
        BOOST_CHECK_EQUAL(acl.user("PARATROOPER").policies().size(), 1);
        BOOST_CHECK_EQUAL(acl.group("yandex").policies().size(), 1);
        BOOST_CHECK_EQUAL(acl.user("PARATROOPER").groupsPolicies().size(), 1);
        BOOST_CHECK_EQUAL(acl.user("PARATROOPER").allPolicies().size(), 2);
        BOOST_CHECK_EQUAL(acl.role("admin").policies().size(), 2);
    }
}

BOOST_AUTO_TEST_CASE(acl_get_all)
{
    pqxx::work work(connection);
    ACLGateway acl(work);
    std::vector<User> users = acl.users();
    BOOST_CHECK(users.size());
    std::vector<Group> groups = acl.groups();
    BOOST_CHECK(groups.size());
    std::vector<Role> roles = acl.roles();
    BOOST_CHECK(roles.size());
}

BOOST_AUTO_TEST_CASE(acl_drop)
{
    pqxx::work work(connection);
    ACLGateway acl(work);

    auto group = acl.createGroup("tempGroup", "delete me");
    auto role = acl.createRole("tempRole", "", Role::Privacy::Public);
    auto policy = acl.createPolicy(group, role, acl.aoi(0));

    {
        auto policies = group.policies();
        BOOST_REQUIRE_EQUAL(policies.size(), 1);
        BOOST_CHECK_NO_THROW(acl.drop(std::move(policies.front())));
    }
    BOOST_CHECK(group.policies().empty());

    BOOST_CHECK_NO_THROW(acl.drop(std::move(group)));
    BOOST_CHECK_THROW(acl.group("tempGroup"), GroupNotExists);

    BOOST_CHECK_NO_THROW(acl.drop(std::move(role)));
    BOOST_CHECK_THROW(acl.role("tempRole"), RoleNotExists);
}

BOOST_AUTO_TEST_CASE(acl_find_users)
{
    pqxx::work work(connection);
    ACLGateway acl(work);
    BOOST_CHECK_EQUAL(acl.users("roop").size(), 3);
    BOOST_CHECK_EQUAL(acl.users("PARATROOPER").size(), 1);

    acl.createUser(190346, "PARA.TROOPER", "My name is PARA.TROOPER", 77777);

    BOOST_CHECK_EQUAL(acl.users("para.").size(), 1);
    BOOST_CHECK_EQUAL(acl.users("para-").size(), 1);
    BOOST_CHECK_EQUAL(acl.users("para.")[0].uid(), acl.users("para-")[0].uid());

    acl.createUser(190347, "PARA-TROOPER", "My name is PARA.TROOPER", 77777);
    BOOST_CHECK_EQUAL(acl.users("para.").size(), 2);
    BOOST_CHECK_EQUAL(acl.users("para-").size(), 2);
}

BOOST_AUTO_TEST_CASE(acl_filter_users)
{
    pqxx::work work(connection);
    ACLGateway acl(work);
    ID groupId = acl.group("yandex").id();
    ID roleId = acl.role("poweruser").id();
    ID aoiId = 12;
    {
        auto pagedResult = acl.users(
            groupId, roleId, aoiId, User::Status::Active, 1, 1);
        BOOST_CHECK_EQUAL(pagedResult.pager().totalCount(), 1);
        BOOST_CHECK_EQUAL(pagedResult.pager().page(), 1);
        BOOST_CHECK_EQUAL(pagedResult.pager().perPage(), 1);
        BOOST_CHECK_EQUAL(pagedResult.value().size(), 1);
    }
    {
        auto users = acl.users(0, 0, 0, User::Status::Banned, 0, 0);
        BOOST_REQUIRE_EQUAL(users.value().size(), 1);
        BOOST_CHECK_EQUAL(
            users.value()[0].uid(),
            acl.user("ÜBERTROOPER").uid());
    }
    BOOST_CHECK(acl.users(0, 0, 0, User::Status::Deleted, 0, 0).value().empty());
}

BOOST_AUTO_TEST_CASE(acl_filter_user_ids)
{
    pqxx::work work(connection);
    ACLGateway acl(work);
    ID groupId = acl.group("yandex").id();
    ID roleId = acl.role("poweruser").id();
    ID aoiId = 12;

    auto userIds = acl.userIds(groupId, roleId, aoiId);
    BOOST_CHECK_EQUAL(userIds.size(), 1);
    BOOST_CHECK_EQUAL(*userIds.begin(), acl.user("PARATROOPER").id());
}

BOOST_AUTO_TEST_CASE(acl_filter_all_user_ids)
{
    pqxx::work work(connection);
    ACLGateway acl(work);

    auto userIds = acl.userIds(0, 0, 0);
    BOOST_CHECK_EQUAL(userIds.size(), 3);
}

BOOST_AUTO_TEST_CASE(acl_filter_users_by_roles)
{
    pqxx::work work(connection);
    ACLGateway acl(work);

    std::set<ID> roleIds = {
        acl.role("admin").id(),
        acl.role("poweruser").id()
    };
    auto userIds = acl.userIdsByRoles(roleIds);
    BOOST_CHECK_EQUAL(userIds.size(), 2);
    BOOST_CHECK_EQUAL(*userIds.begin(), acl.user("SUPER.TROOPER").id());
    BOOST_CHECK_EQUAL(*userIds.rbegin(), acl.user("PARATROOPER").id());
}

BOOST_AUTO_TEST_CASE(acl_filter_users_deleted)
{
    pqxx::work work(connection);
    ACLGateway acl(work);
    auto bannedUser = acl.user("ÜBERTROOPER");

    bannedUser.setDeleted(User::DeleteReason::User, 1111);

    BOOST_CHECK(acl.users(0, 0, 0, User::Status::Banned, 0, 0).value().empty());
    {
        auto users = acl.users(0, 0, 0, User::Status::Deleted, 0, 0);
        BOOST_REQUIRE_EQUAL(users.value().size(), 1);
        BOOST_CHECK_EQUAL(
            users.value()[0].uid(),
            acl.user("ÜBERTROOPER").uid());
    }
}

BOOST_AUTO_TEST_CASE(acl_filter_groups)
{
    pqxx::work work(connection);
    ACLGateway acl(work);
    auto pagedResult = acl.groups(1, 12, 1, 1);
    BOOST_CHECK_EQUAL(pagedResult.pager().totalCount(), 1);
    BOOST_CHECK_EQUAL(pagedResult.pager().page(), 1);
    BOOST_CHECK_EQUAL(pagedResult.pager().perPage(), 1);
    BOOST_REQUIRE_EQUAL(pagedResult.value().size(), 1);
    BOOST_CHECK_EQUAL(pagedResult.value()[0].name(), "yandex");
}

BOOST_AUTO_TEST_CASE(acl_filter_roles)
{
    pqxx::work work(connection);
    ACLGateway acl(work);
    ID groupId = acl.group("yandex").id();

    auto pagedResult = acl.roles(groupId, 1, 1);
    BOOST_CHECK_EQUAL(pagedResult.pager().totalCount(), 1);
    BOOST_CHECK_EQUAL(pagedResult.pager().page(), 1);
    BOOST_CHECK_EQUAL(pagedResult.pager().perPage(), 1);
    BOOST_REQUIRE_EQUAL(pagedResult.value().size(), 1);
    BOOST_CHECK_EQUAL(pagedResult.value()[0].id(), acl.role("admin").id());
}

BOOST_AUTO_TEST_CASE(acl_filter_roles_starts_with)
{
    pqxx::work work(connection);
    ACLGateway acl(work);

    auto createRoles = [&](const std::vector<std::string>& names) {
        for (const auto& name : names) {
            acl.createRole(name, "", Role::Privacy::Public);
        }
    };

    std::vector<std::string> relevantNames{
        "prёfix_%", "prёfix_%.postfix", "Prёfix_%.postfix"};
    createRoles(relevantNames);
    std::vector<std::string> irrelevantNames{
        "prёfix_",
        "prёfix0",
        "prёfix_1",
        "prёfix_.prёfix_%.postfix",
        "prёfix0.prёfix_%.postfix",
        "prёfix_1.prёfix_%.postfix",
        "2prёfix_%",
        "2prёfix_%.prёfix_%.postfix"};
    createRoles(irrelevantNames);

    const std::string prefix = "prёfix_%";
    const auto roles = acl.roles(InclusionType::StartsWith, prefix);

    BOOST_CHECK_EQUAL(roles.size(), relevantNames.size());

    for (const auto& role: roles) {
        auto nameCount = std::count(
            relevantNames.begin(), relevantNames.end(), role.name());
        BOOST_CHECK_EQUAL(nameCount, 1);
    }
}

BOOST_AUTO_TEST_CASE(acl_filter_roles_contains)
{
    pqxx::work work(connection);
    ACLGateway acl(work);

    auto createRoles = [&](const std::vector<std::string>& names) {
        for (const auto& name : names) {
            acl.createRole(name, "", Role::Privacy::Public);
        }
    };

    std::vector<std::string> relevantNames{
        "%cöntains_%", "%cöntains_%.postfix", "prefix.%Cöntains_%.postfix"};
    createRoles(relevantNames);
    std::vector<std::string> irrelevantNames{
        "cöntains",
        "cöntains_",
        "%cöntains%",
        "%cöntains1%",
        "cöntains.cöntains%.postfix",
        "cöntains1",
        "cöntains1.cöntains%.postfix",
        "2cöntains%",
        "2cöntains%.cöntains%.postfix"};
    createRoles(irrelevantNames);

    const std::string prefix = "%cöntains_%";
    const auto roles = acl.roles(InclusionType::Contains, prefix);

    BOOST_CHECK_EQUAL(roles.size(), relevantNames.size());

    for (const auto& role: roles) {
        auto nameCount = std::count(
            relevantNames.begin(), relevantNames.end(), role.name());
        BOOST_CHECK_EQUAL(nameCount, 1);
    }
}

BOOST_AUTO_TEST_CASE(acl_filter_roles_limit)
{
    pqxx::work work(connection);
    ACLGateway acl(work);

    auto createRoles = [&](const std::vector<std::string>& names) {
        for (const auto& name : names) {
            acl.createRole(name, "", Role::Privacy::Public);
        }
    };
    std::vector<std::string> roleNames{"name.a", "name.z", "name", "name.b"};
    createRoles(roleNames);

    const std::string namePart = "name";
    {
        const size_t limit = 0;
        const auto roles = acl.roles(InclusionType::Contains, namePart, limit);
        BOOST_CHECK_EQUAL(roles.size(), roleNames.size());
    }

    {
        const size_t limit = 2;
        const auto roles = acl.roles(InclusionType::Contains, namePart, limit);
        BOOST_CHECK_EQUAL(roles.size(), limit);
    }

    {
        size_t limit = roleNames.size() + 1;
        const auto roles = acl.roles(InclusionType::Contains, namePart, limit);
        BOOST_CHECK_EQUAL(roles.size(), roleNames.size());
    }

}

BOOST_AUTO_TEST_CASE(acl_filter_roles_empty_name_part)
{
    pqxx::work work(connection);
    ACLGateway acl(work);

    const std::string namePart = "";
    const size_t limit = 0;
    BOOST_CHECK_THROW(
        acl.roles(InclusionType::Contains, namePart, limit),
        ::maps::RuntimeError);
}

BOOST_AUTO_TEST_CASE(acl_client_test)
{
    // Permissions tree created by test acl_create_permissions_tree.
    // Role 'poweruser' permissions assigned by test acl_role_permission.
    {
        pqxx::work work(connection);
        ACLGateway acl(work);
        auto policy = acl.createPolicy(acl.user("PARATROOPER"),
            acl.role("poweruser"),
            Factory::aoi(0, "", "", Deleted::No));
        BOOST_CHECK_EQUAL(SubjectPath("mpro/editor").str(),
            "mpro/editor");
        BOOST_CHECK_EQUAL(SubjectPath("mpro/editor").pathParts().size(),
            2);
        std::vector<std::string> objGeoms;
        CheckContext context1(190341, objGeoms, work, {User::Status::Active});
        BOOST_CHECK_NO_THROW(
            SubjectPath("mpro")("editor")("aoi").check(context1));
        BOOST_CHECK_NO_THROW(
            SubjectPath("mpro")("editor")("aoi").check(context1));
        BOOST_CHECK_NO_THROW(
            SubjectPath("mpro")("editor")("aoi").checkPartialAccess(context1));
        BOOST_CHECK(
            SubjectPath("mpro")("editor")("aoi").isAllowed(context1));
        BOOST_CHECK_NO_THROW(
            SubjectPath("mpro")("editor")("aoi")
            ("attributes").check(context1));
        BOOST_CHECK_NO_THROW(
            SubjectPath("mpro")("editor")("aoi")
            ("attributes").checkPartialAccess(context1));
        BOOST_CHECK_NO_THROW(
            SubjectPath("mpro")("editor/aoi/attributes").check(context1));
        BOOST_CHECK_THROW(
            SubjectPath("mpro")("tasks").check(context1),
            AccessDenied);
        BOOST_CHECK_THROW(
            SubjectPath("mpro")("tasks").checkPartialAccess(context1),
            AccessDenied);
        // As configured current user doesn't have access to mpro/tasks but for mpro/editor/...
        BOOST_CHECK_THROW( // This check to demonstrate that we don't have full access to mpro
            SubjectPath("mpro").check(context1),
            AccessDenied);
        BOOST_CHECK_NO_THROW( // But this check ensures that still we got partial access
            SubjectPath("mpro").checkPartialAccess(context1));
        BOOST_CHECK_THROW(
            // Case when path longer then resource but
            // not actually same path of permissions tree
            SubjectPath("mp").checkPartialAccess(context1),
            AccessDenied);
        BOOST_CHECK_THROW(CheckContext context2(190341235, objGeoms, work, {User::Status::Active}),
            UserNotExists);

    }
}

BOOST_AUTO_TEST_CASE(acl_filter_test)
{
    {
        pqxx::work work(connection);
        ACLGateway acl(work);
        User uGraphed = acl.createUser(2, "userGRAPHED", "My name is", 77777);
        Role rGraphed = acl.createRole("roleGRAPHED", "", Role::Privacy::Public);
        Role rTasker = acl.createRole("roleTASKER", "", Role::Privacy::Public);
        Aoi aoi1 = Factory::aoi(1, "", "", Deleted::No);
        Aoi aoi2 = Factory::aoi(2, "", "", Deleted::No);
        acl.createPolicy(uGraphed, rGraphed, aoi1);
        acl.createPolicy(uGraphed, rTasker, aoi2);
        BOOST_CHECK_EQUAL(
            acl.users(0, rGraphed.id(), aoi1.id(), std::nullopt, 0, 0
                ).pager().totalCount(), 1);
        BOOST_CHECK_EQUAL(
            acl.users(0, rGraphed.id(), 0, std::nullopt, 0, 0
                ).pager().totalCount(), 1);
        BOOST_CHECK_EQUAL(
            acl.users(0, 0, aoi1.id(), std::nullopt, 0, 0
                ).pager().totalCount(), 1);
        BOOST_CHECK_EQUAL(
            acl.users(0, rTasker.id(), aoi2.id(), std::nullopt, 0, 0
                ).pager().totalCount(), 1);
        BOOST_CHECK_EQUAL(
            acl.users(0, rGraphed.id(), aoi2.id(), std::nullopt, 0, 0
                ).pager().totalCount(), 0);
        BOOST_CHECK_EQUAL(
            acl.users(0, rTasker.id(), aoi1.id(), std::nullopt, 0, 0
                ).pager().totalCount(), 0);
        work.commit();
    }
}

BOOST_AUTO_TEST_CASE(acl_first_applicable_role_test)
{
    {
        pqxx::work work(connection);
        ACLGateway acl(work);

        User u1 = acl.createUser(101, "user1", "My name is 1", 77777);
        User u2 = acl.createUser(102, "user2", "My name is 2", 77777);
        User u3 = acl.createUser(103, "user3", "My name is 3", 77777);
        Aoi aoi1 = Factory::aoi(201, "", "", Deleted::No);
        Aoi aoi2 = Factory::aoi(202, "", "", Deleted::No);

        Group g1 = acl.createGroup("group1", "");
        g1.add(u1);
        g1.add(u2);

        Role r1 = acl.createRole("role1", "", Role::Privacy::Public);
        acl.createPolicy(u1, r1, aoi2);

        Role r2 = acl.createRole("role2", "", Role::Privacy::Public);
        acl.createPolicy(g1, r2, aoi1);

        Role r3 = acl.createRole("role3", "", Role::Privacy::Public);
        acl.createPolicy(u2, r3, aoi1);
        acl.createPolicy(u3, r3, aoi1);

        BOOST_CHECK_EQUAL(
            acl.firstApplicableRole(u1, {"role2", "role1"}, "test"),
            "role2");
        BOOST_CHECK_EQUAL(
            acl.firstApplicableRole(u3, {"role1", "role2"}, "test"),
            "test");

        auto rolesMap =
            acl.firstApplicableRoles({u1, u2}, {"role1", "role2"}, "");
        BOOST_CHECK_EQUAL(rolesMap.size(), 2);
        BOOST_REQUIRE(rolesMap.count(u1.id()));
        BOOST_CHECK_EQUAL(rolesMap[u1.id()], "role1");
        BOOST_REQUIRE(rolesMap.count(u2.id()));
        BOOST_CHECK_EQUAL(rolesMap[u2.id()], "role2");
    }
}

BOOST_AUTO_TEST_CASE(acl_restricted_users)
{
    const UID TEST_UID = 1;
    const UID OTHER_TEST_UID = 2;
    const std::string REASON = "comments.60";

    pqxx::work txn(connection);

    BOOST_REQUIRE(!isUserRestricted(txn, TEST_UID));
    BOOST_REQUIRE(!isUserRestricted(txn, OTHER_TEST_UID));

    restrictUser(txn, TEST_UID, REASON);

    BOOST_REQUIRE(isUserRestricted(txn, TEST_UID));
    BOOST_REQUIRE(!isUserRestricted(txn, OTHER_TEST_UID));

    unrestrictUser(txn, TEST_UID);

    BOOST_REQUIRE(!isUserRestricted(txn, TEST_UID));
    BOOST_REQUIRE(!isUserRestricted(txn, OTHER_TEST_UID));
}

BOOST_AUTO_TEST_CASE(acl_clusters)
{
    pqxx::work work(connection);
    ClusterManager cm(work);
    ACLGateway acl(work);
    auto mpro = acl.rootPermission("mpro");
    mpro.updateLeafsIds(acl);
    //Check no empty cluster created
    BOOST_CHECK_EQUAL(cm.requestCluster({},{}), 0);
    BOOST_REQUIRE_NO_THROW(acl.createRole("role1", {}, Role::Privacy::Public));
    BOOST_REQUIRE_NO_THROW(acl.createGroup("group1", {}));
    //Check no cluster created if no permissions
    auto role1 = acl.role("role1");
    BOOST_CHECK_EQUAL(cm.requestCluster({}, {role1.id()}), 0);
    auto group1 = acl.group("group1");
    BOOST_CHECK_EQUAL(cm.requestCluster({group1.id()}, {}), 0);
    BOOST_CHECK_EQUAL(cm.requestCluster({group1.id()}, {role1.id()}), 0);
    //Check cluster created and returned with same call (permissions added)
    role1.setPermissions({acl.permission(SubjectPath("mpro/editor"))});
    ID clusterId1 = 0;
    BOOST_REQUIRE_NO_THROW(clusterId1 = cm.requestCluster({}, {role1.id()}));
    BOOST_CHECK(clusterId1);
    BOOST_CHECK_EQUAL(clusterId1, cm.requestCluster({}, {role1.id()}));
    //Check unique cluster created and returned with other call
    BOOST_REQUIRE_NO_THROW(acl.createRole("role2", {}, Role::Privacy::Public));
    auto role2 = acl.role("role2");
    role2.setPermissions({acl.permission(SubjectPath("mpro/tasks"))});
    BOOST_CHECK(clusterId1 != cm.requestCluster({}, {role2.id()}));
    ID clusterId2 = cm.requestCluster({}, {role2.id()});
    BOOST_CHECK(clusterId2);
    //Add group role cluster and group role and update
    ID clusterId3 = 0;
    BOOST_REQUIRE_NO_THROW(clusterId3 = cm.requestCluster({group1.id()}, {role1.id()}));
    BOOST_CHECK(clusterId3);
    BOOST_CHECK(clusterId3 != clusterId1);
    BOOST_CHECK(clusterId3 != clusterId2);
    acl.createPolicy(group1, role2, acl.aoi(0));
    cm.enqueueClusters({clusterId3});
    BOOST_CHECK_EQUAL(cm.processClustersUpdateQueue(), 1);
    BOOST_CHECK_EQUAL(clusterId3, cm.requestCluster({group1.id()}, {role1.id()}));
    BOOST_CHECK(clusterId3 != cm.requestCluster({group1.id()}, {role1.id(), role2.id()}));
}

BOOST_AUTO_TEST_CASE(acl_sync_schedules)
{
    pqxx::work work(connection);
    ACLGateway acl(work);

    User u1 = acl.createUser(101, "user1", "Name 1", 1000);
    Group g1 = acl.createGroup("group1", "");
    Group g2 = acl.createGroup("group2", "");
    Group g3 = acl.createGroup("group3", "");
    Role r1 = acl.createRole("role1", "", Role::Privacy::Public);
    Role r2 = acl.createRole("role2", "", Role::Privacy::Public);
    Role r3 = acl.createRole("role3", "", Role::Privacy::Public);
    Role r4 = acl.createRole("role4", "", Role::Privacy::Public);
    Aoi aoi1 = Factory::aoi(201, "", "", Deleted::No);
    Aoi aoi2 = Factory::aoi(202, "", "", Deleted::No);
    Aoi aoi3 = Factory::aoi(203, "", "", Deleted::No);
    Aoi aoi4 = Factory::aoi(204, "", "", Deleted::No);

    const auto now = std::chrono::system_clock::now() + SCHEDULE_TIMEZONE_OFFSET;
    const auto startDateActive = chrono::formatIntegralDateTime(now, "%d.%m.%Y");
    const auto endDateActive = chrono::formatIntegralDateTime(
        now + std::chrono::days(14), "%d.%m.%Y");
    const auto startDateInactive = chrono::formatIntegralDateTime(
        now - std::chrono::days(5), "%d.%m.%Y");
    const auto endDateInactive = chrono::formatIntegralDateTime(
        now - std::chrono::days(1), "%d.%m.%Y");
    const auto startTime = chrono::formatIntegralDateTime(now, "%H:00");
    const auto endTime = chrono::formatIntegralDateTime(now, "%H:59");
    const auto weekdayNow = std::stoi(chrono::formatIntegralDateTime(now, "%u"));

    const auto so1 = acl.createScheduledPolicy(
        u1.id(), r1.id(), aoi1.id(),
        startDateActive, endDateActive,
        std::nullopt, std::nullopt,
        std::nullopt, "[[4,2],[6,2]]");
    const auto so2 = acl.createScheduledGroup(
        u1.id(), g1.id(),
        startDateActive, std::nullopt,
        startTime, endTime,
        std::nullopt, "[[5,2]]");
    const auto so3 = acl.createScheduledPolicy(
        g2.id(), r2.id(), aoi2.id(),
        startDateActive, endDateActive,
        std::nullopt, std::nullopt,
        1 << (weekdayNow - 1), std::nullopt);

    const auto so4 = acl.createScheduledPolicy(
        u1.id(), r3.id(), aoi3.id(),
        startDateInactive, endDateInactive,
        std::nullopt, std::nullopt,
        std::nullopt, "[[1,1]]");
    const auto so5 = acl.createScheduledGroup(
        u1.id(), g3.id(),
        startDateInactive, endDateInactive,
        std::nullopt, std::nullopt,
        std::nullopt, "[[1,1]]");
    const auto so6 = acl.createScheduledPolicy(
        g2.id(), r4.id(), aoi4.id(),
        startDateInactive, endDateInactive,
        std::nullopt, std::nullopt,
        1 << (weekdayNow - 1), std::nullopt);

    g3.add(u1);
    acl.createPolicy(u1, r3, aoi3);
    acl.createPolicy(g2, r4, aoi4);

    BOOST_CHECK_EQUAL(
        scheduledObjectsSize(acl.allScheduledObjects()), 6);
    BOOST_CHECK_EQUAL(
        scheduledObjectsSize(acl.scheduledObjectsForAgent(u1.id())), 4);
    BOOST_CHECK_EQUAL(
        scheduledObjectsSize(acl.scheduledObjectsForAgent(g1.id())), 0);
    BOOST_CHECK_EQUAL(
        scheduledObjectsSize(acl.scheduledObjectsForAgent(g2.id())), 2);
    BOOST_CHECK_EQUAL(
        scheduledObjectsSize(acl.scheduledObjectsForAgent(g3.id())), 0);

    acl.synchronizeSchedules();

    BOOST_CHECK_EQUAL(u1.policies().size(), 1);
    BOOST_CHECK_EQUAL(u1.policies()[0].role().name(), "role1");
    BOOST_CHECK_EQUAL(u1.groups().size(), 1);
    BOOST_CHECK_EQUAL(u1.groups()[0].name(), "group1");
    BOOST_CHECK_EQUAL(u1.groupsPolicies().size(), 0);

    BOOST_CHECK_EQUAL(g1.users().size(), 1);
    BOOST_CHECK_EQUAL(g2.users().size(), 0);
    BOOST_CHECK_EQUAL(g3.users().size(), 0);

    BOOST_CHECK_EQUAL(g1.policies().size(), 0);
    BOOST_CHECK_EQUAL(g2.policies().size(), 1);
    BOOST_CHECK_EQUAL(g2.policies()[0].role().name(), "role2");
    BOOST_CHECK_EQUAL(g3.policies().size(), 0);

    acl.dropSchedulesObjects(so4.schedule().id());
    acl.dropSchedulesObjects(so5.schedule().id());
    acl.dropSchedulesObjects(so6.schedule().id());

    BOOST_CHECK_EQUAL(scheduledObjectsSize(acl.allScheduledObjects()), 3);
}

BOOST_AUTO_TEST_SUITE_END()

} // namespace maps::wiki
