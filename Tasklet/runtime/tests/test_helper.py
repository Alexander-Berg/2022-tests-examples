from tasklet.api import tasklet_pb2
from tasklet.tests.proto import tasks_pb2

from tasklet import runtime
from tasklet.runtime import tests


def test_get_requirements():
    job = tasklet_pb2.JobStatement()
    job.name = "SumTask"
    inputs = tasks_pb2.Input()
    inputs.a = 1
    job.input.Pack(inputs)

    res = runtime.get_init_description(
        "tasklet.runtime.tests.common:SumDefaultRequirementsImpl", job.SerializeToString()
    )
    res_description = tasklet_pb2.JobStatement()
    res_description.ParseFromString(res)

    assert res_description.requirements.tmpfs == 0

    res = runtime.get_init_description(
        "tasklet.runtime.tests.common:SumNonDefaultRequirementsImpl", job.SerializeToString()
    )
    res_description.ParseFromString(res)

    assert res_description.requirements.tmpfs == 1


def test_execute():
    job_instance = tasklet_pb2.JobInstance()
    job_instance.statement.name = "SumTask"
    inputs = tasks_pb2.Input()
    inputs.a = 1
    inputs.b = 2
    job_instance.statement.input.Pack(inputs)

    impl_path = "tasklet.runtime.tests.common:SumImpl"

    res = runtime.execute_helper(impl_path, job_instance.SerializeToString())
    result = tasklet_pb2.JobResult()
    result.ParseFromString(res)

    outputs = tasks_pb2.Output()
    result.output.Unpack(outputs)

    assert outputs.c == 3


def sum_task_executor(inputs):
    job_instance = tasklet_pb2.JobInstance()
    job_instance.statement.name = "SumTask"
    job_instance.statement.input.Pack(inputs)

    impl_path = "tasklet.runtime.tests.common:SumWithDefaultParamsImpl"

    statement_str = runtime.get_init_description(impl_path, job_instance.statement.SerializeToString())
    statement = tasklet_pb2.JobStatement()
    statement.ParseFromString(statement_str)
    job_instance.statement.input.CopyFrom(statement.input)

    res = runtime.execute_helper(impl_path, job_instance.SerializeToString())
    result = tasklet_pb2.JobResult()
    result.ParseFromString(res)

    outputs = tasks_pb2.Output()
    result.output.Unpack(outputs)

    return outputs


def test_default_input():
    inputs = tasks_pb2.Input()
    inputs.a = 1

    outputs = sum_task_executor(inputs)

    assert outputs.c == inputs.a + tests.common.SumWithDefaultParamsImpl.default_b


def test_default_input_overrided():
    inputs = tasks_pb2.Input()
    inputs.a = 1
    inputs.b = 10

    outputs = sum_task_executor(inputs)

    assert outputs.c == inputs.a + inputs.b
