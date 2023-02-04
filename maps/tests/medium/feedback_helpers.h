#pragma once

#include <yandex/maps/wiki/social/feedback/agent.h>

namespace maps::wiki::social::tests {

const TUid USER_ID = 1;
const TUid USER2_ID = 2;

const geolib3::Point2 ZERO_POSITION(0., 0.);
const feedback::Type SOME_TYPE = feedback::Type::Other;
const std::string SOME_SRC = "some-source";

inline auto DESCR()
{
    return feedback::Description("some-description");
}

feedback::Task getTask(feedback::GatewayRO& gatewayRo, social::TId taskId);

feedback::TaskForUpdate getTaskForUpdate(feedback::Agent& agent, social::TId taskId);

TId createRevealedTask(feedback::Agent& agent);

TId createTaskNeedInfoAvailable(feedback::Agent& agent);

} // namespace maps::wiki::social::feedback::tests
