#include <yandex/maps/wiki/social/gateway.h>

#include "helpers.h"

#include <library/cpp/testing/gmock_in_unittest/gmock.h>
#include <library/cpp/testing/unittest/registar.h>

#include <cstdint>
#include <string>

namespace maps::wiki::social::tests {

using namespace testing;


Y_UNIT_TEST_SUITE_F(skills, DbFixture) {
    Y_UNIT_TEST(shouldGetSkills) {
        {   // Should get skills for different users
            pqxx::work txn(conn);
            txn.exec(
                "INSERT INTO social.skills VALUES"
                "(1, 'addr', 'accept', 1),"
                "(2, 'bld', 'edit', 2)"
            );

            EXPECT_THAT(
                Gateway(txn).getSkills({1}),
                SkillsByUid({{1, {{"addr", {{ResolveResolution::Accept, 1}}}}}})
            );

            EXPECT_THAT(
                Gateway(txn).getSkills({2}),
                SkillsByUid({{2, {{"bld", {{ResolveResolution::Edit, 2}}}}}})
            );

            EXPECT_THAT(
                Gateway(txn).getSkills({2, 1}),
                SkillsByUid({
                    {1, {{"addr", {{ResolveResolution::Accept, 1}}}}},
                    {2, {{"bld", {{ResolveResolution::Edit, 2}}}}}
                })
            );
        }

        {   // Should get different skills
            pqxx::work txn(conn);
            txn.exec(
                "INSERT INTO social.skills VALUES"
                "(1, 'addr', 'accept', 1),"
                "(1, 'bld', 'accept', 2),"
                "(1, 'bld', 'edit', 3),"
                "(1, 'bld', 'revert', 4),"
                "(1, 'rd', 'revert', 5)"
            );

            EXPECT_THAT(
                Gateway(txn).getSkills({1}),
                SkillsByUid({{1, {
                        {"addr", {{ResolveResolution::Accept, 1}}},
                        {"bld", {
                            {ResolveResolution::Accept, 2},
                            {ResolveResolution::Edit, 3},
                            {ResolveResolution::Revert, 4}}},
                        {"rd", {{ResolveResolution::Revert, 5}}}
                    }
                }})
            );
        }
    }
}

} // namespace maps::wiki::social::tests
