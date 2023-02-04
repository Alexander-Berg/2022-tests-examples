#pragma once

#include <maps/b2bgeo/vm_scheduler/libs/common/include/slot.h>
#include <maps/b2bgeo/vm_scheduler/server/libs/state/include/state.h>

#include <vector>


namespace maps::b2bgeo::vm_scheduler::testing {

std::vector<SlotCapacity> getPossibleSlots();

void checkStateConstrains(const State& state, const StateChange& stateChange);

} // namespace maps::b2bgeo::vm_scheduler::testing
