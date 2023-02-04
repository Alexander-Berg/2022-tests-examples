#pragma once

#include <maps/wikimap/mapspro/libs/social/include/yandex/maps/wiki/social/feedback/task.h>
#include <maps/libs/json/include/value.h>

namespace maps::wiki::social::feedback::tests {

class FbTaskFactory {
public:
    FbTaskFactory();

    FbTaskFactory& type(Type type);
    FbTaskFactory& source(std::string source);
    FbTaskFactory& id(TId id);
    FbTaskFactory& position(const geolib3::Point2& postion);
    FbTaskFactory& attrs(const json::Value& jsonAttrs);

    Task create() const;

private:
    Task task_;
};

} // namespace maps::wiki::social::feedback::tests
