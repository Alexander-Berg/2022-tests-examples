#include "matchers.h"
#include "printers.h"

#include <maps/libs/stringutils/include/join.h>
#include <maps/libs/stringutils/include/to_string.h>

#include <boost/lexical_cast.hpp>

namespace maps::wiki::acl::ut {

using namespace testing;

std::ostream&
operator<<(
    std::ostream& os,
    const PolicyDescription& policyDescription)
{
    return os <<
        "{" <<
            "agent: " << policyDescription.agentId << ", "
            "role: " << policyDescription.roleId << ", "
            "aoi: " << policyDescription.aoiId <<
        "}";
}

Matcher<PoliciesVec>
policiesAre(const PolicyDescriptions& policies)
{
    class Matcher : public MatcherInterface<PoliciesVec> {
        public:
            Matcher(const PolicyDescriptions& policies): policies_(policies) {}

            bool MatchAndExplain(PoliciesVec policies, MatchResultListener* listener) const override
            {
                if (policies.size() != policies_.size()) {
                    *listener <<
                        "expected number of policies is " << policies_.size() << " "
                        "got " << policies.size();
                    return false;
                }

                for (const auto& policy : policies) {
                    if (!policies_.count({policy.agentId(), policy.roleId(), policy.aoiId()})) {
                        *listener
                            << "unexpected policy " << policy;
                        return false;
                    }
                }

                return true;
            }

            void DescribeTo(::std::ostream* os) const override
            {
                *os << "policies are "
                    << stringutils::join(stringutils::toStrings(policies_), ", ") << ".";
            }

            void DescribeNegationTo(::std::ostream* os) const override
            {
                *os << "policies are not "
                    << stringutils::join(stringutils::toStrings(policies_), ", ") << ".";
            }

        private:
            PolicyDescriptions policies_;
        };

    return MakeMatcher(new Matcher(policies));
}

Matcher<GroupsVec>
haveNames(const std::set<std::string>& names)
{
    class Matcher : public MatcherInterface<GroupsVec> {
        public:
            Matcher(const std::set<std::string>& names): names_(names) {}

            bool MatchAndExplain(GroupsVec groups, MatchResultListener* listener) const override
            {
                if (groups.size() != names_.size()) {
                    *listener <<
                        "expected number of groups is " << names_.size() << " "
                        "got " << groups.size();
                    return false;
                }

                for (const auto& group : groups) {
                    if (!names_.count(group.name())) {
                        *listener << "unexpected group '" << group.name() << "'";
                        return false;
                    }
                }

                return true;
            }

            void DescribeTo(::std::ostream* os) const override
            {
                *os << "groups names are '" << stringutils::join(names_, "', '") << "'.";
            }

            void DescribeNegationTo(::std::ostream* os) const override
            {
                *os << "groups names are not '" << stringutils::join(names_, "', '") << "'.";
            }

        private:
            std::set<std::string> names_;
    };

    return MakeMatcher(new Matcher(names));
}

} // namespace maps::wiki::acl::ut
