#include "commit_helpers.h"

#include "controller_helpers.h"

namespace maps::wiki::tests {

std::pair<std::string, approve_status::ApproveStatus>
getCommitStateAndStatus(TCommitId commitId, TUid uid, TBranchId branchId)
{
    const auto getCommitResult =
        performRequest<GetCommit>(commitId, uid, branchId, TESTS_TOKEN);
    return {
        *getCommitResult->commitModelPtr->state(),
        *getCommitResult->commitModelPtr->approveStatus()
    };
}

} // namespace maps::wiki::tests
