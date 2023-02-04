from tasklet.tests.proto import tasks_tasklet as t


class SumTaskBrokenImpl(t.SumTaskBase):
    def run(self):
        self.output.c = self.input.a + self.input.b
        raise Exception("some error")


class AwaitTaskBrokenImpl(t.AwaitTaskBase):
    def run(self):
        r1 = self.wait(t.SumTask(self.ctx.ctx_msg, t.SumTask.Input(a=self.input.a, b=self.input.a)))
        r2 = self.wait("right", t.SumTask(self.ctx.ctx_msg, t.SumTask.Input(a=self.input.b, b=self.input.b)))
        r3 = self.wait("result", t.ProductTask(self.ctx.ctx_msg, t.ProductTask.Input(a=r1.c, b=r2.c)))
        self.output.CopyFrom(r3)
