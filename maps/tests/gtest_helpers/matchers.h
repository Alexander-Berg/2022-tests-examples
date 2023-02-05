#pragma once

#include <maps/wikimap/mapspro/libs/acl/include/common.h>
#include <maps/wikimap/mapspro/libs/acl/include/group.h>
#include <maps/wikimap/mapspro/libs/acl/include/policy.h>

#include <maps/libs/introspection/include/comparison.h>
#include <library/cpp/testing/gmock_in_unittest/gmock.h>

#include <set>
#include <string>
#include <vector>

namespace maps::wiki::acl::ut {

using GroupsVec = std::vector<acl::Group>;
using PoliciesVec = std::vector<acl::Policy>;

struct PolicyDescription {
    acl::ID agentId;
    acl::ID roleId;
    acl::ID aoiId;

    auto introspect() const {
        return std::tie(agentId, roleId, aoiId);
    }
};

using maps::introspection::operator<;

using PolicyDescriptions = std::set<PolicyDescription>;

testing::Matcher<PoliciesVec> policiesAre(const PolicyDescriptions& policies);
testing::Matcher<GroupsVec> haveNames(const std::set<std::string>& names);

} // namespace maps::wiki::acl::ut
