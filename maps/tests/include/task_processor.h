#pragma once

#include "task.h"
#include "task_checker.h"

#include <maps/analyzer/libs/rtp/include/task_pool.h>
#include <maps/libs/deprecated/boost_time/utils.h>

class TaskStoreProcessor
{
public:
    TaskStoreProcessor(size_t deleteType, TaskChecker* checker)
        :deleteType_(deleteType), checker_(checker) { }

    std::unique_ptr<ProcessedInfo> createNewProcessedInfo(
            const Task& firstTask) const
    {
        std::unique_ptr<ProcessedInfo> result(new ProcessedInfo);
        result->push_back(firstTask);
        return result;
    }

    void process(const Task& task, ProcessedInfo* processedInfo) const
    {
        ASSERT(task.time() <= maps::nowUtc());
        processedInfo->push_back(task);
        if (task.type() == deleteType_) {
            checker_->check(*processedInfo);
        }
    }
private:
    size_t deleteType_;
    TaskChecker* checker_;
};
