#pragma once

#include <maps/wikimap/mapspro/services/editor/src/tests/helpers/objects_creator/objects_creator.h>

#include <maps/wikimap/mapspro/services/editor/src/common.h>
#include <maps/wikimap/mapspro/services/editor/src/observers/observer.h>

#include <maps/libs/json/include/value.h>

#include <string>


namespace maps::wiki::tests {

struct SaveResult;

struct SaveResult {
    SaveResult(const json::Value& result);

    std::string revisionId;
    TCommitId commitId;
    TOid objectId;
    std::unordered_map<std::string, SaveResult> slaves;
    Geometry geometry;
};

SaveResult
saveObject(TUid uid, std::string json, ObserverCollection& observerCollection);

} // namespace maps::wiki::tests
