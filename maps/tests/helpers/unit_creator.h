#pragma once

#include <maps/wikimap/mapspro/libs/assessment/include/unit.h>

namespace maps::wiki::assessment::tests {

class UnitCreator {
public:
    UnitCreator(TId id = 0):
        unit_{
            .id = id,
            .entity = {.id = "entity id", .domain = Entity::Domain::Edits},
            .action = {.name = "name", .by = 1, .at = std::chrono::system_clock::now()},
        }
    {}

    Unit operator *() const { return unit_; }

    UnitCreator& entityId(std::string value) { unit_.entity.id = std::move(value); return *this; }
    UnitCreator& entityDomain(Entity::Domain value) { unit_.entity.domain = value; return *this; }
    UnitCreator& actionName(std::string value) { unit_.action.name = std::move(value); return *this; }
    UnitCreator& actionBy(TUid value) { unit_.action.by = value; return *this; }
    UnitCreator& actionAt(chrono::TimePoint value) { unit_.action.at = value; return *this; }

private:
    Unit unit_;
};

} // namespace maps::wiki::assessment::tests
