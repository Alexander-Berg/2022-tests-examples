from datetime import timedelta
from threading import Timer
from typing import List

import pytest
from django.utils import timezone
from pydantic import ValidationError

from dwh.core.models.work import Work
from dwh.core.tasks import do_work


@pytest.fixture
def check_messages():

    def check_messages_(work: Work, messages: List[str], *, min_total: int):

        log = work.context['log']
        oneline = '|'.join(log_entry['message'] for log_entry in log)
        found = []
        for message in messages:
            if message in oneline:
                found.append(message)

        if len(messages) != len(found):
            full_log = '\n'.join(oneline.split('|'))
            raise AssertionError(
                f'Some messages are missing: {set(messages).difference(found)}.\n'
                f'Full log:\n{full_log}'
            )

        if min_total:
            # проверяем минимум, потому что при тестах после
            # аркадийной сборки в root логер может писать машинерия запуска тестов
            if len(log) < min_total:
                full_log = '\n'.join(oneline.split('|'))
                raise AssertionError(
                    f'Produced messages count {len(log)} is less than expected {min_total}.\n'
                    f'Full log:\n{full_log}'
                )

    return check_messages_


def test_basic(init_work, check_work_stop, check_messages):

    # ошибка в данных - не заполнено поле по схеме
    with pytest.raises(ValidationError) as e:
        init_work({})

    assert 'params\n  field required' in f'{e.value}'

    # неизвестное имя задания
    with pytest.raises(ValueError) as e:
        init_work({'meta': {'task_name': 'my_task'}, 'params': {'a': 1}})
    assert 'unknown' in f'{e.value}'

    # неподдерживаемый параметр задания
    with pytest.raises(ValueError) as e:
        init_work({'meta': {'task_name': 'echo'}, 'params': {'buggy': 1, 'a': 5}})
    assert 'unknown parameter buggy' in f'{e.value}'

    # исключение внутри кода задания
    data_input = {
        'meta': {'task_name': 'echo', 'workers': 1, 'timeout': 0, 'retries': 1, 'hint': 'somehint'},
        'params': {'a': 1, 'fail': True}}
    data_context = {'log': [], 'progress': 0}
    work = init_work({'meta': {'task_name': 'echo', 'hint': 'somehint'}, 'params': {'a': 1, 'fail': True}})

    assert work.id
    assert work.input == data_input
    assert work.context == data_context
    assert work.status == work.Status.NEW
    assert work.dt_add
    assert work.dt_upd
    assert not work.host
    assert not work.dt_start
    assert not work.dt_finish

    do_work(None)
    work.refresh_from_db()

    assert work.status == work.Status.ERROR
    assert work.dt_start
    assert work.dt_finish
    assert work.host
    assert work.input == data_input
    assert work.context['progress'] == 10

    check_messages(work, [
        'wow',
        'failure triggered',
    ], min_total=15)

    # неожиданное исключение
    work = Work.objects.create(name='bogus', input={'meta': {'task_name': 'bogus'}, 'params': {}})
    do_work(None)
    work.refresh_from_db()
    assert work.status == work.Status.ERROR
    check_messages(work, [
        "KeyError: 'bogus'",
    ], min_total=1)
    log_entry = work.context['log'][0]
    assert log_entry['trace']
    assert log_entry['pid']

    # теперь всё удачно - один рабочий
    work = init_work({
        'meta': {'task_name': 'echo'},
        'params': {
            'a': 2,
            'enum': 'on',
            'long': True,
            'date_inter': '2022-01-04',
            'day': '2022-02-08',
            'list-parameter1': [1, 2, '3'],
            'list-parameter2': '[4, 5, "6"]',
        }
    })
    do_work(None)
    work.refresh_from_db()
    check_messages(work, [
        'stderr capture test',
        'stdout capture test',
        'root logger capture test',
        'module logger capture test',
        'date_inter:2022-01-01',
        'day:8',
        "list_parameter1:(1, 2, '3')",
        "list_parameter2:(4, 5, '6')",
    ], min_total=19)
    assert work.context['progress'] == 100
    assert work.status == work.Status.FINISHED

    # теперь всё удачно - несколько рабочих
    work = init_work({'meta': {'task_name': 'echo', 'workers': 2}, 'params': {'a': 2}})
    do_work(None)
    work.refresh_from_db()
    check_messages(work, [
        'stderr capture test',
        'stdout capture test',
        'root logger capture test',
        'module logger capture test',
    ], min_total=9)
    assert work.context['progress'] == 100
    assert work.status == work.Status.FINISHED


def test_stop(init_work, init_user, check_work_stop):
    # сценарий остановки
    work = init_work({'meta': {'task_name': 'echo'}, 'params': {'a': 3}})
    work_id = work.id
    user = init_user()

    assert not work.stop()  # не выполняется, нечего останавливать
    assert not work.dt_stop
    assert not work.schedule_stop()  # не запущена

    work = Work.acquire()
    assert work.launches == 1
    meta = work.input['meta']
    assert meta['retries'] == 1
    assert meta['timeout'] == 0
    assert work.id == work_id
    assert work.dt_deadline is None

    assert work.schedule_stop(user)
    work.refresh_from_db()
    assert work.dt_stop
    assert work.status == work.Status.STOP_SCHEDULED
    assert work.stopper == user

    check_work_stop(work)


def test_success_with_retry(init_work, check_messages):
    """
    Проверка случая, когда работа luigi завершается со статусом SUCCESS_WITH_RETRY.
    """

    work = init_work({
        'meta': {'task_name': 'unfulfilled_test', 'scheduler_retry_count': 5},
        'params': {'inits_to_fulfill': 3}
    })

    do_work(None)
    work.refresh_from_db()

    check_messages(work, [
        'Target exists for subtask is False',
        'Target exists for subtask is True',
        'RuntimeError: Unfulfilled dependency at run time',
    ], min_total=5)

    assert work.context['progress'] == 100
    assert work.status == work.Status.FINISHED


def test_stop_multiproc(init_work, check_messages):

    init_work({
        'meta': {'task_name': 'echo', 'workers': 2},
        'params': {'a': 4, 'subcount': 4, 'subloops': 8}
    })

    work = Work.acquire()
    Timer(1, work.stop).start()
    work.process()

    work.refresh_from_db()

    check_messages(work, [
        'Disabling known workers',
        'Dummy resources freed',
        'with exit code -9',
    ], min_total=37)

    assert work.status == work.Status.STOPPED
    assert not work.dt_finish


def test_timed_out(init_work, monkeypatch):

    work = init_work({
        'meta': {'task_name': 'echo', 'timeout': 10, 'retries': 2},
        'params': {'a': 33}
    })
    meta = work.input['meta']
    assert meta['retries'] == 2
    assert meta['timeout'] == 10
    assert work.launches == 0

    monkeypatch.setattr('dwh.core.models.work.Work._schedule_update_work', lambda **kwargs: None)

    def check_run():
        work = Work.acquire()
        assert work.dt_deadline
        assert not work.dt_stop

        work.dt_deadline = timezone.now() - timedelta(hours=3)
        work.save()

        work._in_progress = True
        Work.heartbeat(work=work)
        work.refresh_from_db()

        # проверим простановку статуса.
        assert work.is_timedout
        assert work.dt_stop

        return work

    work = check_run()
    assert work.launches == 1

    # пришло задание, актуализирующее статус
    Work.handle_timedout()
    work.refresh_from_db()

    assert work.status == Work.Status.NEW
    assert not work.dt_stop
    assert not work.dt_start

    # пришло время выполнения задания
    deadline_old = work.dt_deadline
    work = check_run()

    assert work.launches == 2
    assert work.dt_deadline > deadline_old

    # вновь пришло задание, актуализирующее статус
    Work.handle_timedout()
    work.refresh_from_db()

    assert work.status == Work.Status.ERROR
    assert work.dt_stop
    assert work.dt_deadline
    assert work.dt_start
    assert len(work.context['log']) >= 2
    # две записи о двух таймаутах минимум. плюс доп. информация из очереди


def test_conflict_supersede(init_work, monkeypatch, time_freeze):

    def get_work(*, conflict: bool = True):

        conflict_data = {}
        if conflict:
            conflict_data['conflict'] = {
                'type': 'supersede',
                'params': {'delay': 10}
            }

        work = init_work({
            'meta': {'task_name': 'echo', **conflict_data},
            'params': {'a': 444}
        })
        work.refresh_from_db()
        return work

    # в бд уже присутствует исполняющаяся работа
    work_started = get_work(conflict=False)
    work_started.status = Work.Status.STARTED
    work_started.save()
    assert not work_started.dt_stop

    # регистрируем вытесняющую
    work_superseding = get_work()
    conflict = work_superseding.input['meta']['conflict']
    assert conflict == {'type': 'supersede', 'params': {'delay': 10}}
    assert work_superseding.dt_postpone is None

    monkeypatch.setattr('dwh.core.models.work.Work._schedule_update_work', lambda **kwargs: None)
    monkeypatch.setattr('dwh.core.models.work.Work._schedule_update_context', lambda **kwargs: None)

    # фоновое пробует подхватить вытесняющую
    assert not Work.acquire()  # отсрочена из-за конфликта
    work_superseding.refresh_from_db()
    assert work_superseding.status == Work.Status.NEW
    work_started.refresh_from_db()
    assert work_started.dt_stop
    work_started.status = Work.Status.ERROR
    work_started.save()

    # проверка связывания при вытеснении
    assert work_superseding.prev.id == work_started.id
    assert work_started.next_id == work_superseding.id

    # фоновое пробует подхватить вытесняющую по прошествии времени
    postponed = work_superseding.dt_postpone
    with time_freeze(postponed + timedelta(seconds=120)):
        work = Work.acquire()
        assert work.id == work_superseding.id
        assert work.is_started


def test_get_build_params():

    assert Work.get_build_params({
        'workers': 3,
        'task_name': 'somename',
        'logging_conf_file': 'do-no-apply',

    }) == {
        'workers': 3,
        'logging_conf_file': ''
    }


def test_flatten_params():

    flatten = Work.flatten_params

    assert flatten({}) == ''

    assert flatten({
        'tables': ['mv_curr|ency_rate'],
        'transfer`_to_clusters': ['hah*n', 'arnold'],
        'current_month': True,
        'sub': {
            'two': ['\\w', 'o?'],
            'one': 1,
        },
        'x_too_log': (
            '1234567890_1234567890_1234567890_1234567890_1234567890_1234567890_1234567890_1234567890_1234567890_'
        ),
    }) == (
        'current_month=True;sub=[one=1;two=w,o];tables=mv_currency_rate;transfer_to_clusters=arnold,hahn;'
        'x_too_log=1234567890_1234567890_1234567890_1234567890_1234567890_1234567890_1234567890_1234567890_123456'
    )


def test_wc(init_work):

    work = init_work({
        'meta': {
            'task_name': 'wc',
            'workers': 2,
            'worker_max_reschedules': 3,
            'scheduler_retry_count': 4,
            'scheduler_disable_window': 50000,
        },
        'params': {'myparam': 'some'}
    })
    do_work(None)
    work.refresh_from_db()
    assert work.status == work.Status.FINISHED
    assert work.input['meta']['workers'] == 2
