#include "save_object.h"

#include <maps/wikimap/mapspro/services/editor/src/tests/helpers/controller_helpers.h>

#include <yandex/maps/wiki/common/string_utils.h>


namespace maps::wiki::tests {

SaveResult::SaveResult(const json::Value& result)
{
    ASSERT(result.hasField("geoObjects"));
    ASSERT(!result["geoObjects"].empty());

    const auto& geoObject = result["geoObjects"][0];

    revisionId = geoObject["revisionId"].as<std::string>();
    commitId = std::stoull(common::split(revisionId, ":")[1]);
    objectId = std::stoull(geoObject["id"].as<std::string>());
    geometry = geoObject["geometry"];

    if (geoObject.hasField("slaves")) {
        for (const auto& slaveName: geoObject["slaves"].fields()) {
            slaves.emplace(slaveName, SaveResult(geoObject["slaves"][slaveName]));
        }
    }
}


SaveResult
saveObject(
    TUid uid,
    std::string json,
    ObserverCollection& observerCollection)
{
    return json::Value::fromString(
        performSaveObjectRequestJsonStr(
            json,
            observerCollection,
            uid
        )
    );
}

} // namespace maps::wiki::tests
