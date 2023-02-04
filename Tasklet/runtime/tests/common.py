from tasklet.api import tasklet_pb2
from tasklet.runtime.python import base
from tasklet.tests.proto import types_pb2, tasks_pb2


class DummyClient(base.TaskletBase):
    def __init__(self, channel):
        pass


class DummyPyAdapter(base.TaskletBase):
    def __init__(self, wrapped):
        assert isinstance(wrapped, tasklet_pb2.ContextEntry)
        self.wrapped = wrapped

    def get_value(self):
        b = types_pb2.PyEntry()
        self.wrapped.any.Unpack(b)
        return b.value


class SumImpl(base.TaskletBase):
    input = tasks_pb2.Input()
    output = tasks_pb2.Output()

    def run(self):
        self.output.c = self.input.a + self.input.b


class SumWithDefaultParamsImpl(base.TaskletBase):
    input = tasks_pb2.Input()
    output = tasks_pb2.Output()
    default_b = 2

    def run(self):
        self.output.c = self.input.a + self.input.b

    @classmethod
    def setup_default_input(cls, input):
        input.b = cls.default_b


class SumNonDefaultRequirementsImpl(base.TaskletBase):
    input = tasks_pb2.Input()

    @classmethod
    def setup_requirements(cls, requirements, input):
        requirements.tmpfs = 1


class SumDefaultRequirementsImpl(base.TaskletBase):
    input = tasks_pb2.Input()

    @classmethod
    def setup_requirements(cls, requirements, input):
        requirements.tmpfs = 0


class SumFailedRequirementsImpl(base.TaskletBase):
    input = tasks_pb2.Input()

    @classmethod
    def setup_requirements(cls, requirements, input):
        raise Exception('some')
