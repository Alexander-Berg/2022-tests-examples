#pragma once

#include <maps/wikimap/mapspro/services/editor/src/common.h>
#include <maps/wikimap/mapspro/services/editor/src/observers/observer.h>

#include <yandex/maps/wiki/social/task.h>

#include <maps/libs/json/include/value.h>


namespace maps::wiki::tests {

struct RevertResult {
    RevertResult(const json::Value& result);

    TCommitId  revertCommitId;
    TCommitIds revertedCommitIds;
    TCommitIds revertedDirectlyCommitIds;
};

RevertResult
revertCommit(TUid uid, TCommitId commitId, ObserverCollection& observers);

} // namespace maps::wiki::tests
