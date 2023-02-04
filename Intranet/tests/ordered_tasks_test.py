import pytest

import random

from staff.celery_app import app
from staff.gap.controllers.gap_tasks import GapTasks
from staff.lib.testing import get_random_datetime

task_called = False
expected_value = None


@app.task
def sample_task(value):
    assert value == expected_value
    global task_called
    task_called = True


@pytest.mark.parametrize(
    'value_generator',
    [
        lambda: get_random_datetime(),
        lambda: get_random_datetime().date(),
        lambda: get_random_datetime().time(),
        lambda: str(get_random_datetime()),
        lambda: str(get_random_datetime().date()),
        lambda: str(get_random_datetime().time()),
        lambda: get_random_datetime().isoformat(),
        lambda: get_random_datetime().date().isoformat(),
        lambda: get_random_datetime().time().isoformat(),
        lambda: get_random_datetime().date().isoformat() + get_random_datetime().date().isoformat(),
    ],
)
@pytest.mark.django_db()
def test_schedule_ordered_task_args_passing(value_generator):
    global task_called
    global expected_value

    identity = random.randint(1, 10000)
    task_called = False
    expected_value = value_generator()

    GapTasks.schedule_ordered_task(
        identity,
        callable_or_task=sample_task,
        kwargs={
            'value': expected_value,
        },
    )

    GapTasks.execute_next_task(identity)

    assert task_called
