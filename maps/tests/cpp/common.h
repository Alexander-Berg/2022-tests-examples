#pragma once

#include <maps/garden/libs_server/yt_task_handler/include/task_runner.h>
#include <maps/garden/libs_server/yt_task_handler/include/types.h>

#include <mapreduce/yt/interface/fwd.h>

namespace maps::garden::yt_task_handler::tests {

Task runTask(const TString& taskKey);
Task runTask(const TaskSpec& taskSpec);

NYT::IClientPtr getClient();
TString getWorkingDir();
TString getYtProxy();

} // namespace maps::garden::yt_task_handler::tests
