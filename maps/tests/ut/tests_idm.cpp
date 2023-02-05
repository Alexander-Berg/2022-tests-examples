#include <maps/wikimap/mapspro/libs/acl/include/aclgateway.h>

#include <yandex/maps/wiki/unittest/localdb.h>

#include <boost/test/unit_test.hpp>

namespace maps::wiki {

using namespace acl;
using namespace std::chrono_literals;
namespace {
pgpool3::Pool& pool()
{
    static unittest::MapsproDbFixture db;
    return db.pool();
}
} // namespace

BOOST_AUTO_TEST_CASE(acl_user_idm)
{
    const std::string LOGIN = "negodnik";
    const std::string NMAPS_SERVICE = "nmaps";
    const std::string YANDEX_ROLE = "yandex";
    auto work = pool().masterWriteableTransaction();
    ACLGateway acl(*work);
    // Test is clear
    BOOST_CHECK(acl.allUsersIDMRoles(NMAPS_SERVICE).empty());

    // Test addition
    acl.addIDMRole(LOGIN, {NMAPS_SERVICE, YANDEX_ROLE});
    BOOST_CHECK_EQUAL(acl.allUsersIDMRoles(NMAPS_SERVICE).size(), 1);
    BOOST_CHECK_EQUAL(acl.userIDMRoles(LOGIN, NMAPS_SERVICE).size(), 1);

    // Test new user
    acl.addIDMRole("godnik", {NMAPS_SERVICE, YANDEX_ROLE});
    BOOST_CHECK_EQUAL(acl.allUsersIDMRoles(NMAPS_SERVICE).size(), 2);

    // Test duplicate
    acl.addIDMRole(LOGIN, {NMAPS_SERVICE, YANDEX_ROLE});
    BOOST_CHECK_EQUAL(acl.userIDMRoles(LOGIN, NMAPS_SERVICE).size(), 1);

    // Test new role
    acl.addIDMRole(LOGIN, {NMAPS_SERVICE, "outsourcer"});
    BOOST_CHECK_EQUAL(acl.userIDMRoles(LOGIN, NMAPS_SERVICE).size(), 2);

    // Test new service
    acl.addIDMRole(LOGIN, {"acl", "admin"});
    BOOST_CHECK_EQUAL(acl.userIDMRoles(LOGIN, NMAPS_SERVICE).size(), 2);
    BOOST_CHECK_EQUAL(acl.userIDMRoles(LOGIN, "acl").size(), 1);

    // Test remove
    acl.removeIDMRole(LOGIN, {NMAPS_SERVICE, YANDEX_ROLE});
    BOOST_CHECK_EQUAL(acl.userIDMRoles(LOGIN, NMAPS_SERVICE).size(), 1);

    // Test remove missing
    BOOST_CHECK_NO_THROW(acl.removeIDMRole(LOGIN, {NMAPS_SERVICE, YANDEX_ROLE}));
}

} // namespace maps::wiki
