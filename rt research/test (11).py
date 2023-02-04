import pytest
import time

from django.core.management import execute_from_command_line
import django
import django.db.utils
django.setup()  # noqa

from irt.multik.operations_queue.queue import WorkerQueue, Worker
from irt.multik.operations_queue.models import Operation, Queue


class Sum(Worker):
    def run(self, *args):
        self.add_message("Summing {}".format('+'.join(str(s) for s in args)))
        time.sleep(2)
        return sum(args)

    def get_last_error(self):
        return 'Ok'


class SumQueue(WorkerQueue):
    queue_name = 'SumQueue'
    worker_class = Sum

    def validate(self):
        pass


def test_bad_queues():
    class NoQueueName(WorkerQueue):
        worker_class = Sum

        def validate(self):
            pass

    class NoWorkerClass(WorkerQueue):
        queue_name = 'test'

        def validate(self):
            pass

    class NoValidate(WorkerQueue):
        queue_name = 'no_validate'
        worker_class = Sum

    with pytest.raises(AttributeError):
        NoQueueName(schedule_at='1s')
    with pytest.raises(AttributeError):
        NoWorkerClass(schedule_at='1s')
    with pytest.raises(TypeError):
        NoValidate()


def test_bad_workers():
    class NoLastErrorWorker(Worker):
        def run(*args, **kwargs):
            pass

    class NoRunWorker(Worker):
        def get_last_error():
            return None

    with pytest.raises(TypeError):
        NoRunWorker(None)
    with pytest.raises(TypeError):
        NoLastErrorWorker(None)


@pytest.fixture()
def django_db():
    execute_from_command_line(['./manage', 'migrate', 'operations_queue'])
    yield
    execute_from_command_line(['./manage', 'migrate', 'operations_queue', 'zero'])


def wait_operation(op):
    op.refresh_from_db()
    timeout = 10
    while op.operation_status not in Operation.FINAL_STATUSES and timeout:
        time.sleep(1)
        timeout -= 1
        op.refresh_from_db()


@pytest.mark.parametrize("call_method", [
    lambda queue, *args: queue.execute(),
    lambda queue, *args: queue.async_execute(),
    lambda queue, op, *args: queue._call(op.queue_id, *args)
], ids=["sync", "async", "direct"])
def test_queue(django_db, call_method):
    """Test that queue executeion works correctly"""
    queue = SumQueue()

    assert len(list(Operation.objects.all())) == 0
    assert len(list(Queue.objects.all())) == 0

    op = queue.push(None, 1, 2)
    assert op.operation_status == Operation.STATUS_SCHEDULED
    call_method(queue, op, 1, 2)
    wait_operation(op)
    op.refresh_from_db()
    assert op.errors is None
    assert op.operation_status == Operation.STATUS_FINISHED
    assert op.result == 3

    messages = op.messages.all()
    assert len(messages) == 1
    assert messages[0].module_name == '__tests__.test'
    assert messages[0].message == 'Summing 1+2'


class Fail(Worker):
    def run(self, *args):
        time.sleep(2)
        raise ValueError

    def get_last_error(self):
        return 'Ok'


class FailQueue(WorkerQueue):
    queue_name = 'FailQueue'
    worker_class = Fail

    def validate(self):
        pass


def test_queue_fails(django_db):
    queue = FailQueue(max_retries=0)
    op = queue.push(None)
    queue.execute()
    op.refresh_from_db()
    assert op.operation_status == Operation.STATUS_ERROR
    assert op.errors is not None
