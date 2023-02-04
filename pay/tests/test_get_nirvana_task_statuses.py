import datetime

import dateutil.tz
import pytest
from nirvana_api import execution_state

from payplatform.nirvana.get_nirvana_task_statuses import get_nirvana_task_statuses

TZ_OFFSET = dateutil.tz.tzutc()

START_OF_MONTH = datetime.datetime(year=2019, month=8, day=1, tzinfo=TZ_OFFSET)
START_OF_MONTH_STRING = '08.19'

START_DATE_OFFSET = datetime.timedelta(hours=9, minutes=30)
START_DATE = START_OF_MONTH + START_DATE_OFFSET

FINISH_DATE_OFFSET = datetime.timedelta(hours=17, minutes=45)
FINISH_DATE = START_OF_MONTH + FINISH_DATE_OFFSET

AFTER_FINISH_DATE_OFFSET = datetime.timedelta(hours=23, minutes=15)
AFTER_FINISH_DATE = START_OF_MONTH + AFTER_FINISH_DATE_OFFSET


class TestBlock:
    def __init__(self, started_date=None, completed_date=None, status=None, result=None, parameters=None):
        self.started_date = started_date
        self.completed_date = completed_date
        self.status = status
        self.result = result

        self.is_cached = False

        self.parameters = parameters if parameters else {}

    def get_parameter(self, name, default=None):
        return self.parameters.get(name, default)


@pytest.mark.parametrize("start_deadline", [
    datetime.timedelta(hours=1),
    datetime.timedelta(hours=-1),
    datetime.timedelta(),
])
@pytest.mark.parametrize("finish_deadline", [
    datetime.timedelta(hours=3),
    datetime.timedelta(hours=-3),
    datetime.timedelta(),
])
@pytest.mark.parametrize("task_offset", [
    datetime.timedelta(days=1),
    datetime.timedelta(),
])
def test_task_info_deadline_alert_is_correct_when_task_not_started(start_deadline, finish_deadline, task_offset):
    current_date = START_DATE
    current_date_offset = START_DATE_OFFSET

    if start_deadline:
        start_deadline += current_date_offset
    if finish_deadline:
        finish_deadline += current_date_offset

    task_info = get_nirvana_task_statuses.TaskInfo('test_task', 'test_task')
    task_block = TestBlock()
    task_info.blocks = [task_block]

    task_info.start_deadline = start_deadline
    task_info.finish_deadline = finish_deadline
    task_info.offset = task_offset

    expected_alert_state = 'INFO'

    if (start_deadline and start_deadline + task_offset < current_date_offset) or \
            (finish_deadline and finish_deadline + task_offset < current_date_offset):
        expected_alert_state = 'CRIT'

    actual_alert_state, _ = task_info.alert_state(now=current_date, start_of_month=START_OF_MONTH)

    assert actual_alert_state == expected_alert_state


@pytest.mark.parametrize("start_deadline", [
    datetime.timedelta(hours=1),
    datetime.timedelta(hours=-1),
    datetime.timedelta(),
])
@pytest.mark.parametrize("finish_deadline", [
    datetime.timedelta(hours=3),
    datetime.timedelta(hours=-3),
    datetime.timedelta(),
])
@pytest.mark.parametrize("task_offset", [
    datetime.timedelta(days=1),
    datetime.timedelta(),
])
@pytest.mark.parametrize("task_status", [
    'completed',
    'running'
])
def test_task_info_deadline_alert_is_correct_when_task_started(start_deadline, finish_deadline, task_offset, task_status):
    current_date = FINISH_DATE
    current_date_offset = FINISH_DATE_OFFSET

    if start_deadline:
        start_deadline += START_DATE_OFFSET
    if finish_deadline:
        finish_deadline += current_date_offset

    if task_status == 'completed':
        status, result = 'completed', 'success'
    else:
        status, result = 'running', 'undefined'

    task_info = get_nirvana_task_statuses.TaskInfo('test_task', 'test_task')
    task_block = TestBlock(started_date=START_DATE, status=status, result=result)
    task_info.blocks = [task_block]

    task_info.start_deadline = start_deadline
    task_info.finish_deadline = finish_deadline
    task_info.offset = task_offset

    if (start_deadline and start_deadline + task_offset < START_DATE_OFFSET) or \
            (finish_deadline and finish_deadline + task_offset < current_date_offset):
        if task_status == 'completed':
            expected_alert_state = 'OK'
        else:
            expected_alert_state = 'CRIT'
    else:
        if task_status == 'completed':
            expected_alert_state = 'OK'
        else:
            expected_alert_state = 'INFO'

    actual_alert_state, _ = task_info.alert_state(now=current_date, start_of_month=START_OF_MONTH)

    assert actual_alert_state == expected_alert_state


@pytest.mark.parametrize("start_deadline", [
    datetime.timedelta(hours=1),
    datetime.timedelta(hours=-1),
    datetime.timedelta(),
])
@pytest.mark.parametrize("finish_deadline", [
    datetime.timedelta(hours=3),
    datetime.timedelta(hours=-3),
    datetime.timedelta(),
])
@pytest.mark.parametrize("task_offset", [
    datetime.timedelta(days=1),
    datetime.timedelta(),
])
def test_task_info_deadline_alert_is_correct_when_task_finished(start_deadline, finish_deadline, task_offset):
    current_date = AFTER_FINISH_DATE

    if start_deadline:
        start_deadline += START_DATE_OFFSET
    if finish_deadline:
        finish_deadline += FINISH_DATE_OFFSET

    task_info = get_nirvana_task_statuses.TaskInfo('test_task', 'test_task')
    task_block = TestBlock(started_date=START_DATE, completed_date=FINISH_DATE)
    task_info.blocks = [task_block]

    task_info.start_deadline = start_deadline
    task_info.finish_deadline = finish_deadline
    task_info.offset = task_offset

    expected_alert_state = 'INFO'

    if (start_deadline and start_deadline + task_offset < START_DATE_OFFSET) or \
            (finish_deadline and finish_deadline + task_offset < FINISH_DATE_OFFSET):
        expected_alert_state = 'CRIT'

    actual_alert_state, _ = task_info.alert_state(now=current_date, start_of_month=START_OF_MONTH)

    assert actual_alert_state == expected_alert_state


def test_task_info_duration_limit_alert_is_correct_when_task_not_started():
    task_info = get_nirvana_task_statuses.TaskInfo('test_task', 'test_task')
    task_block = TestBlock()
    task_info.blocks = [task_block]

    task_info.duration_limit = datetime.timedelta(hours=0.5)

    expected_alert_state = 'INFO'

    actual_alert_state, _ = task_info.alert_state(now=START_DATE, start_of_month=START_OF_MONTH)

    assert actual_alert_state == expected_alert_state


@pytest.mark.parametrize("duration_limit, expected_alert_state", [
    (datetime.timedelta(hours=0.5), 'CRIT'),
    (datetime.timedelta(hours=1.5), 'INFO'),
])
def test_task_info_duration_limit_alert_is_correct_when_task_started(duration_limit, expected_alert_state):
    task_info = get_nirvana_task_statuses.TaskInfo('test_task', 'test_task')
    task_block = TestBlock(started_date=START_DATE)
    task_info.blocks = [task_block]

    task_info.duration_limit = duration_limit

    task_info.start_date = START_DATE

    actual_alert_state, _ = task_info.alert_state(now=START_DATE + datetime.timedelta(hours=1),
                                                  start_of_month=START_OF_MONTH)

    assert actual_alert_state == expected_alert_state


@pytest.mark.parametrize("duration_limit, expected_alert_state, expected_message", [
    (datetime.timedelta(hours=0.5), 'OK', 'Duration limit exceeded'),
    (datetime.timedelta(hours=1.5), 'OK', ''),
])
def test_task_info_duration_limit_alert_is_correct_when_task_finished(duration_limit, expected_alert_state, expected_message):
    task_info = get_nirvana_task_statuses.TaskInfo('test_task', 'test_task')
    task_block = TestBlock(
        started_date=START_DATE,
        completed_date=START_DATE + datetime.timedelta(hours=1),
        status=execution_state.ExecutionStatus.completed,
        result=execution_state.ExecutionResult.success
    )
    task_info.blocks = [task_block]

    task_info.duration_limit = duration_limit

    actual_alert_state, message = task_info.alert_state(now=AFTER_FINISH_DATE, start_of_month=START_OF_MONTH)

    assert actual_alert_state == expected_alert_state
    assert message == expected_message


@pytest.mark.parametrize("task_result, expected_alert_state", [
    (execution_state.ExecutionResult.failure, 'CRIT'),
    (execution_state.ExecutionResult.cancel, 'CRIT'),
    (execution_state.ExecutionResult.success, 'OK'),
    (execution_state.ExecutionResult.undefined, 'INFO'),
])
def test_task_info_alert_is_correct_for_task_result(task_result, expected_alert_state):
    task_info = get_nirvana_task_statuses.TaskInfo('test_task', 'test_task')

    status = None

    if task_result == execution_state.ExecutionResult.success:
        status = execution_state.ExecutionStatus.completed

    task_block = TestBlock(result=task_result, status=status)
    task_info.blocks = [task_block]

    actual_alert_state, _ = task_info.alert_state(now=START_DATE, start_of_month=START_OF_MONTH)
    assert actual_alert_state == expected_alert_state


@pytest.mark.parametrize("first_start_date, second_start_date, expected_start_date", [
    (START_DATE, FINISH_DATE, START_DATE),
    (FINISH_DATE, START_DATE, START_DATE)
])
def test_task_info_start_date_is_earliest_of_blocks(first_start_date, second_start_date, expected_start_date):
    task_info = get_nirvana_task_statuses.TaskInfo('test_task', 'test_task')
    task_block_first = TestBlock(started_date=first_start_date)
    task_block_second = TestBlock(started_date=second_start_date)
    task_info.blocks = [task_block_first, task_block_second]

    assert expected_start_date == task_info.start_date


def test_month_is_extracted_correcly():
    task_info = get_nirvana_task_statuses.TaskInfo('test_task', 'test_task')
    task_block = TestBlock(parameters={'month': START_OF_MONTH_STRING})
    task_info.blocks = [task_block]

    assert START_OF_MONTH == get_nirvana_task_statuses.get_month_start([task_info], month_start_tz_offset=0)
