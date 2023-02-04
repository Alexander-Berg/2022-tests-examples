import uuid

from tasklet.api import sched_pb2
from tasklet.tests.proto import tasks_tasklet as tasks


class SumTaskImpl(tasks.SumTaskBase):
    def run(self):
        self.output.c = self.input.a + self.input.b


class AwaitTaskImpl(tasks.AwaitTaskBase):
    def run(self):
        r1 = self.wait("left", tasks.SumTask(self.ctx.ctx_msg, tasks.SumTask.Input(a=self.input.a, b=self.input.b)))
        r2 = self.wait("right", tasks.SumTask(self.ctx.ctx_msg, tasks.SumTask.Input(a=self.input.a, b=self.input.b)))
        r3 = self.wait("result", tasks.ProductTask(self.ctx.ctx_msg, tasks.ProductTask.Input(a=r1.c, b=r2.c)))
        self.output.CopyFrom(r3)


class CheckIdTaskImpl(tasks.CheckIdTaskBase):
    def run(self):
        assert self.tasklet_id is not None
        id_unpacked = uuid.UUID(self.tasklet_id, version=4)
        assert self.tasklet_id == str(id_unpacked)

        if not self.input.is_child:
            holder = tasks.CheckIdTask(self.ctx.ctx_msg, tasks.CheckIdTask.Input(is_child=True))
            holder.name = "CheckIdTaskPy"
            child_id = self.wait("test_token", holder).result

            job_id = sched_pb2.TaskletId()
            job_id.id = child_id
            status = next(self.ctx.sched.status(job_id))
            assert status is not None
            assert status.ready and status.result.success

            status.result.output.Unpack(job_id)
            assert job_id.id == child_id

        self.output.result = self.tasklet_id
