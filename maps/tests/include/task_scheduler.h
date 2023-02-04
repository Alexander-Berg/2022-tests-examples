#pragma once

#include "global_state.h"
#include "task.h"

#include <boost/date_time/posix_time/posix_time.hpp>
#include <optional>
#include <set>

// Helper task for schedule tasks to process signals.
class IncrementTaskScheduler
{
public:
    IncrementTaskScheduler(const boost::posix_time::ptime& start,
            size_t toDeleteType, size_t timeDelay)
        :start_(start), toDeleteType_(toDeleteType), timeDelay_(timeDelay),
         emptyState_(0), testState_(0) { }

    void setGlobalState(maps::analyzer::rtp::EmptyUserGlobalState* state)
    {
        ASSERT((!emptyState_ || emptyState_ == state)
                && "second time set state");
        emptyState_ = state;
    }

    void unsetEmptyState()
        { emptyState_ = 0; }

    const maps::analyzer::rtp::EmptyUserGlobalState* emptyState() const
        { return emptyState_; }

    void setGlobalState(TestGlobalState* state)
    {
        ASSERT((!testState_ || testState_ == state)
                && "second time set state");
        testState_ = state;
    }

    const TestGlobalState* testState() const
        { return testState_; }

    Task create(size_t id,
            std::multiset<size_t>* unprocessedRequests) const
    {
        ASSERT(!unprocessedRequests->empty());
        size_t first = *unprocessedRequests->begin();
        Task task(
            0,
            id,
            start_
        );
        task.addNumber(first);
        unprocessedRequests->erase(unprocessedRequests->begin());
        return task;
    }

    void update(
            Task* inQueueTask,
            const ProcessedInfo* /*processedInfo*/,
            std::multiset<size_t>* unprocessedRequests) const
    {
        ASSERT(!unprocessedRequests->empty());
        if (inQueueTask->type() != 0) {
            *inQueueTask = create(inQueueTask->id(), unprocessedRequests);
        }
    }

    std::optional<Task> next(const Task& completedTask,
            const ProcessedInfo& /*processedInfo*/,
            std::multiset<size_t>* unprocessedRequests) const
    {
        if (unprocessedRequests->empty()) {
            if (completedTask.type() != toDeleteType_) {
                boost::posix_time::ptime time =
                    start_ + boost::posix_time::seconds(
                        timeDelay_ * (1 + completedTask.type()));
                //We wait at least one second to give a chance to add all
                //request before we start to process next stage
                time = std::max(time,
                        maps::nowUtc() + boost::posix_time::seconds(1));
                return Task(completedTask.type() + 1,
                    completedTask.id(), time);
            } else {
                return std::nullopt;
            }
        } else {
            return create(completedTask.id(), unprocessedRequests);
        }
    }

private:
    boost::posix_time::ptime start_;
    size_t toDeleteType_;
    size_t timeDelay_;
    maps::analyzer::rtp::EmptyUserGlobalState* emptyState_;
    TestGlobalState* testState_;
};
