import pytest
from mdh.core.exceptions import LogicError
from mdh.core.models import Lock
from mdh.core.toolbox.tasks import get_registered_task, task


def test_get_registered():

    with pytest.raises(LogicError):
        get_registered_task('unknown')

    task = get_registered_task('send_stats')
    assert task.name == 'send_stats'


@task()
def my_test_task(a, b):
    return 'a'


def test_task():
    Lock.register(['my_test_task'])
    assert my_test_task(1, 2) is None
