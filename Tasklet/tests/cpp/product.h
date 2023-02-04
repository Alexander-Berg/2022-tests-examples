#pragma once

#include <tasklet/tests/proto/tasks.pb.h>
#include <tasklet/tests/proto/tasks.tasklet.h>

#include <tasklet/api/tasklet.pb.h>

namespace ProductTaskImpl {

class TImpl : public NTasklet::ProductTaskBase {
public:
    void Run(const Tasklet::Test::Input& input, Tasklet::Test::Output& output) override {
        output.set_c(input.a() * input.b());
    }
};

}
