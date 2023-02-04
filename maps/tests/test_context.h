#pragma once

#include <maps/factory/libs/tasks/context.h>

namespace maps::factory::processing::tests {

class TestContext : public tasks::ReadOnlyContext {
public:
    TestContext(const std::string& connStr)
        : tasks::ReadOnlyContext({connStr, pgpool3::PoolConstants(1, 1, 1, 1)})
    {
    }

    std::vector<tasks::Task> tasks() const override { return tasks_; }

    tasks::Transaction& transaction() override
    {
        if (!txn_) { txn_ = pool().masterWriteableTransaction(); }
        return txn_->get();
    }

    tasks::Ids add(tasks::TasksRef tasks) override
    {
        tasks::Ids ids;
        for (auto& task: tasks) {
            tasks_.push_back(task);
            ids.push_back(tasks_.size());
        }
        return ids;
    }

    void commit()
    {
        if (txn_) { (*txn_)->commit(); }
        txn_.reset();
    }

private:
    std::optional<tasks::TransactionHandle> txn_;
    std::vector<tasks::Task> tasks_;
};

} // namespace maps::factory::processing::tests
