#pragma once

#include <maps/analyzer/libs/rtp/include/user_global_state.h>

#include "task_processor.h"
#include "task_scheduler.h"
#include "task.h"

#include <set>

struct UserTypes
{
    typedef size_t RequestObject;
    typedef size_t TaskId;
    typedef ::Task Task;
    typedef std::multiset<RequestObject> RequestsQueue;
    typedef ::ProcessedInfo ProcessedInfo;
    typedef IncrementTaskScheduler Scheduler;
    typedef TaskStoreProcessor Processor;
    typedef maps::analyzer::rtp::EmptyUserGlobalState GlobalState;
};
