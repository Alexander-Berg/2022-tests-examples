#pragma once

#include "task.h"

struct TaskChecker
{
    virtual void check(const ProcessedInfo& info) const = 0;
    virtual ~TaskChecker() { }
};
