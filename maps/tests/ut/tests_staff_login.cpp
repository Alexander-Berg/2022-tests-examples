#include <maps/wikimap/mapspro/libs/acl/include/aclgateway.h>

#include <yandex/maps/wiki/unittest/localdb.h>
#include <boost/test/unit_test.hpp>

namespace maps::wiki {

using namespace acl;

namespace {
pgpool3::Pool& pool()
{
    static unittest::MapsproDbFixture db;
    return db.pool();
}
} // namespace

BOOST_AUTO_TEST_CASE(acl_uid_staff_login)
{
    auto work = pool().masterWriteableTransaction();
    ACLGateway acl(*work);

    BOOST_CHECK_NO_THROW(acl.createUser(190111345, "yndx-user", "My name is YANDEX_USER", 77777));
    const auto user = acl.user(190111345);
    BOOST_CHECK(user.staffLogin().empty());
    BOOST_CHECK_NO_THROW(user.setStaffLogin("staff-user"));
    BOOST_CHECK_EQUAL(user.staffLogin(), "staff-user");
    BOOST_CHECK_NO_THROW(user.setStaffLogin("staff-user1"));
    BOOST_CHECK_EQUAL(user.staffLogin(), "staff-user1");
    acl.clearUidToStaff();
    BOOST_CHECK(user.staffLogin().empty());
}

} // namespace maps::wiki
