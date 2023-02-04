#include "revert_commit.h"

#include <maps/wikimap/mapspro/services/editor/src/tests/helpers/controller_helpers.h>

#include <boost/optional.hpp>

#include <string_view>


namespace maps::wiki::tests {

RevertResult::RevertResult(const json::Value& result)
{
    std::string_view commitField =
        result.hasField("revertingCommit")
        ? "revertingCommit"
        : "commit";

    revertCommitId = std::stoull(result[commitField]["id"].as<std::string>());

    for (const auto& revertedCommitId: result[commitField]["revertedCommitIds"]) {
        revertedCommitIds.emplace(std::stoull(revertedCommitId.as<std::string>()));
    }

    for (const auto& revertedDirectlyCommitId: result[commitField]["revertedDirectlyCommitIds"]) {
        revertedDirectlyCommitIds.emplace(std::stoull(revertedDirectlyCommitId.as<std::string>()));
    }
}


RevertResult
revertCommit(
    TUid uid,
    TCommitId commitId,
    ObserverCollection& observers)
{
    static const auto NO_FEEDBACK_TASK_ID = boost::none;
    static const auto NO_REVERT_REASON    = boost::none;

    return convertToJson(
        performRequest<CommitsRevert>(
            observers,
            UserContext{uid, {}},
            commitId,
            NO_REVERT_REASON,
            NO_FEEDBACK_TASK_ID
        )
    );
}

} // namespace maps::wiki::tests
