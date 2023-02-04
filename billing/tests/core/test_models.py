from functools import partial

import pytest
from sitemessage.models import Message

from refs.core.exceptions import TaskInProgress
from refs.core.models import Log
from refs.core.tasks import check_sync_failures


@pytest.mark.django_db
def test_log_basics():
    log = Log.task_create('test')

    with pytest.raises(TaskInProgress):
        Log.task_create('test')

    assert log.get_latest_record() is None

    log.save_result(['1', '2'], fetcher_result='done', success=True)

    assert 'test' in f'{log}'
    assert log.pk
    assert log.task_info == '1\n2'
    assert log.fetcher_result == 'done'

    log = Log.task_create('test')
    log.save_result([], fetcher_result='super', success=False)

    log = Log.task_create('test')
    latest = log.get_latest_record()

    assert latest.fetcher_result == 'done'


@pytest.mark.django_db
def test_log_cleanup(time_forward):
    log = Log.task_create('test')
    log.task_running = True
    log.save()

    cleaned_up = Log.cleanup()
    assert cleaned_up == []

    with time_forward(35*60):
        cleaned_up = Log.cleanup()
        assert len(cleaned_up) == 1

    log.refresh_from_db()
    assert log.task_running is None
    assert log.status == log.STATUS_FAIL


@pytest.mark.django_db
def test_get_sync_failures():

    def add_log_entry(*, failed=False):
        task = Log.task_create('cbrf')
        if failed:
            task.status = Log.STATUS_FAIL
        task.task_mark_complete()
        task.save()
        return task

    add_log_entry()

    get_failures = partial(Log.get_sync_failures, fail_threshold=2)
    assert not get_failures()

    add_log_entry(failed=True)
    assert not get_failures()

    add_log_entry(failed=True)
    assert get_failures() == ['cbrf']
    assert len(Message.objects.all()) == 0

    check_sync_failures(1)
    messages = Message.objects.all()
    assert len(messages) == 1
    assert 'Проблемы фоновой синхронизации' in messages[0].context['subject']
    assert 'Ошибки замечены в: cbrf' in messages[0].context['stext_']
