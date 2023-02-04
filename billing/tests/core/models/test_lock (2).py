from time import sleep

from mdh.core.models import Lock


def test_basic(init_user):

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
