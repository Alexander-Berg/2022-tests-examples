#include "mock_tick_context.h"

namespace NInfra::NPodAgent {

TTickContextPtr MockTickContext(TLogger& logger) {
    return new TTickContext{logger.SpawnFrame(), new TTreeTracer()};
}

} // namespace NInfra::NPodAgent
