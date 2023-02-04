#pragma once

#include <yandex/maps/wiki/social/gateway.h>

namespace maps::wiki::tests {

social::TId
acquireTask(
    const social::Gateway& socialGw,
    social::TUid uid,
    const social::EventFilter& eventFilter,
    const social::ModerationTimeIntervals& moderationTimeIntervals);

} // namespace maps::wiki::tests
