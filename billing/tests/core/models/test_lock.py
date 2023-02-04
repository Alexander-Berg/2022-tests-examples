from time import sleep
from datetime import datetime, timedelta

import pytest

from dwh.core.models import Lock
from dwh.core.toolbox.tasks import get_registered_task
from dwh.core.tasks import cleanup_self


@pytest.fixture
def run_bg_task_wrapped():
    """
    Запускает фоновый процесс по его имени с учетом блокировок.
    """

    def _run_bg_task_wrapped(name: str):
        # Вызываем обёртку над фоновой задачей.
        return get_registered_task(name).func_wrapper()

    return _run_bg_task_wrapped


@pytest.fixture
def lock_and_stale():
    """
    Имитирует залипшую блокировку.
    """

    def _lock_and_stale(name: str) -> Lock:
        lock = Lock.acquire(name)
        lock._update_timer.cancel()
        lock.save()

        return lock

    return _lock_and_stale


def test_basic(init_user):
    """
    Проверяем базовую работу блокировок.
    """

    locks = Lock.register(['a', 'b'])
    assert len(locks) == 2

    lock = locks[0]
    assert str(lock) == 'a'
    assert lock.released
    assert not lock.dt_acquired
    assert not lock.dt_released

    locked = Lock.acquire('a')
    updated = locked.dt_upd

    lock.refresh_from_db()
    assert locked.dt_upd == updated

    sleep(2)  # для проверки обновления по таймеру.

    assert locked.name == 'a'
    assert locked.dt_acquired
    assert not locked.dt_released
    assert not locked.released

    lock.refresh_from_db()
    assert locked.dt_upd > updated  # было обновление по таймеру

    locked.release(result='some')
    lock.refresh_from_db()

    assert locked.result == 'some'
    assert locked.dt_acquired
    assert locked.dt_released
    assert locked.released


def test_cleanup(time_freeze, lock_and_stale):
    """
    Проверяем очистку протухших локов.
    """

    time_now = datetime.now()

    with time_freeze(time_now):
        locks = Lock.register(['a', 'b'])

        # имитируем залипшую блокировку
        for lock in locks:
            lock_and_stale(lock.name)

        assert len(locks) == 2

    with time_freeze(time_now + timedelta(seconds=123)):
        Lock.cleanup_stale(123, ['a'])

    assert Lock.objects.filter(released=False).count() == 1


def test_cleanup_task(run_bg_task, time_freeze, lock_and_stale):
    """
    Проверяем работу регулярной задачи по очистке протухших локов.
    """
    time_now = datetime.now()

    # имитируем залипшую блокировку
    with time_freeze(time_now):
        lock = Lock.register(['some_task'])[0]
        lock = lock_and_stale(lock.name)

    # не попали в таймаут
    with time_freeze(time_now):
        run_bg_task('cleanup')
        lock.refresh_from_db()
        assert not lock.released

    # попали в таймаут, блокировка должна быть снята
    with time_freeze(time_now + timedelta(seconds=100500)):
        run_bg_task('cleanup')
        lock.refresh_from_db()
        assert lock.released


def test_cleanup_self(run_bg_task_wrapped, time_freeze, lock_and_stale):
    """
    Проверяем случай, когда произошел дедлок самой таски `cleanup`.
    """

    time_now = datetime.now()

    with time_freeze(time_now):
        cleanup_lock = Lock.register(['cleanup'])[0]
        cleanup_lock = lock_and_stale(cleanup_lock.name)

        other_locks = Lock.register(['some_task1', 'some_task2'])
        for lock in other_locks:
            lock_and_stale(lock.name)

    # попадаем в таймаут, но блокировка стоит на самой задаче
    with time_freeze(time_now + timedelta(seconds=100500)):
        run_bg_task_wrapped('cleanup')

    cleanup_lock.refresh_from_db()
    assert not cleanup_lock.released
    assert Lock.objects.filter(released=False).count() == 3

    # запускаем задачу, которая не зависит от блокировок
    with time_freeze(time_now + timedelta(seconds=100500)):
        cleanup_self(None)

    cleanup_lock.refresh_from_db()
    assert cleanup_lock.released
    assert Lock.objects.filter(released=False).count() == 2

    # убеждаемся, что остальные блокировки подчистились
    with time_freeze(time_now + timedelta(seconds=100500)):
        run_bg_task_wrapped('cleanup')

    assert Lock.objects.filter(released=False).count() == 0
