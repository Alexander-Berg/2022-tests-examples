#include <boost/test/unit_test.hpp>
#include <library/cpp/testing/unittest/env.h>

#include "maps/mobile/server/init/lib/access_list.h"

BOOST_AUTO_TEST_CASE(test_whitelist)
{
    const auto acl = AccessList{SRC_("empty"), SRC_("whitelist")};

    BOOST_CHECK(!acl.isBlocked("testlib", "whitelisted.entry"));
    BOOST_CHECK(acl.isBlocked("testlib",  "not.whitelisted.entry"));
    BOOST_CHECK(acl.isBlocked("testlib",  "blacklisted.entry"));
    BOOST_CHECK(!acl.isBlocked("testlib", "common.entry"));
}

BOOST_AUTO_TEST_CASE(test_blacklist)
{
    const auto acl = AccessList{SRC_("blacklist"), SRC_("empty")};

    BOOST_CHECK(!acl.isBlocked("testlib", "whitelisted.entry"));
    BOOST_CHECK(!acl.isBlocked("testlib", "not.whitelisted.entry"));
    BOOST_CHECK(acl.isBlocked("testlib",  "blacklisted.entry"));
    BOOST_CHECK(acl.isBlocked("testlib",  "common.entry"));
}

BOOST_AUTO_TEST_CASE(test_none)
{
    const auto acl = AccessList{SRC_("empty"), SRC_("empty")};

    BOOST_CHECK(!acl.isBlocked("testlib", "whitelisted.entry"));
    BOOST_CHECK(!acl.isBlocked("testlib", "not.whitelisted.entry"));
    BOOST_CHECK(!acl.isBlocked("testlib", "blacklisted.entry"));
    BOOST_CHECK(!acl.isBlocked("testlib", "common.entry"));
}

BOOST_AUTO_TEST_CASE(test_both)
{
    const auto acl = AccessList{SRC_("blacklist"), SRC_("whitelist")};

    BOOST_CHECK(!acl.isBlocked("testlib", "whitelisted.entry"));
    BOOST_CHECK(acl.isBlocked("testlib",  "not.whitelisted.entry"));
    BOOST_CHECK(acl.isBlocked("testlib",  "blacklisted.entry"));
    BOOST_CHECK(acl.isBlocked("testlib",  "common.entry"));
}
