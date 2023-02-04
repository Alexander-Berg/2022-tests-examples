#pragma once

#include "tests_common.h"

#include <maps/wikimap/mapspro/services/editor/src/approve_status.h>
#include <maps/wikimap/mapspro/services/editor/src/common.h>
#include <yandex/maps/wiki/revision/common.h>

#include <library/cpp/testing/unittest/registar.h>

#include <string>
#include <utility>


#define WIKI_ASSERT_COMMIT_STATE(commitId, expState, expApproveStatus)        \
    do {                                                                      \
        const auto [gotState, gotStatus] = getCommitStateAndStatus(commitId); \
        UNIT_ASSERT_VALUES_EQUAL(commit_state::expState, gotState);           \
        UNIT_ASSERT_VALUES_EQUAL(                                             \
            approve_status::ApproveStatus::expApproveStatus, gotStatus);      \
    } while (false)


namespace maps::wiki::tests {

std::pair<std::string, approve_status::ApproveStatus>
getCommitStateAndStatus(
    TCommitId commitId,
    TUid uid = TESTS_USER,
    TBranchId branchId = revision::TRUNK_BRANCH_ID);

} // namespace maps::wiki::tests
