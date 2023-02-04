from datetime import datetime

from requests import HTTPError, Timeout

from mdh.core.actions import Action, ActionResult, ActionParams
from mdh.core.exceptions import ScheduledException
from mdh.core.models import Queue


class MyParams(ActionParams):

    a: str = ''


class DummyAction(Action):
    """Фиктивное действие. Не производит полезной работы."""

    id = -1

    params = MyParams

    def run(self) -> ActionResult:
        return ActionResult(success=True)


def test_basic(init_user, monkeypatch):

    user = init_user()

    # Нет кандиадатов для обработки.
    candidate = Queue.get_scheduled()
    assert not candidate

    # Добавлем запись.
    items = DummyAction.run_in_background(subjects=[user], params=MyParams(a='b', v='g'))

    assert len(items) == 1
    item: Queue = items[0]
    assert 'DummyAction' in str(item)
    assert item.is_scheduled
    assert item.id
    assert item.action_object.id == DummyAction.id
    assert item.action_object.params.dict() == {'a': 'b'}  # v: g - отфильтрованы
    assert item.ensure_unique

    # Попытка добавить запись повторно.
    items = DummyAction.run_in_background(subjects=[user], params=MyParams(a='b', v='g'))
    assert items[0].id is None

    with Queue.scheduled() as item:
        result = item.process()
        assert result

    item.refresh_from_db()
    assert item.is_complete

    # Пробуем задания не связанные с конкретным объектом.
    items = DummyAction.run_in_background(subjects=[None, None], params=MyParams(f='xx'))
    assert len(items) == 2  # Проверим, что нет конфликта уникальности.

    for item in items:
        assert item.oid is None
        assert item.otype is None
        assert item.id
        assert not item.ensure_unique

        with Queue.scheduled() as scheduled:
            assert scheduled == item
            result = scheduled.process()
            assert result.success

    # Добавлем запись снова. Тестируем исключение.
    def fail_run():
        """Функция не принимает нужный аргумент, поэтому будет исключение."""

    monkeypatch.setattr(DummyAction, 'run', fail_run)

    items = DummyAction.run_in_background(subjects=[user], params=MyParams(a='c', v='d'))
    item = items[0]
    assert item.id
    assert item.retries == 0
    assert item.notes == ''

    with Queue.scheduled() as item:
        item.process()

    item.refresh_from_db()
    assert item.is_scheduled
    assert item.retries == 1
    assert item.notes.endswith('fail_run() takes 0 positional arguments but 1 was given')

    # Тестируем специализированное исключение.
    def fail_run(*args, **kwargs):
        raise ScheduledException('msg', reset_status=False, immediate_stop=True)

    monkeypatch.setattr(DummyAction, 'run', fail_run)

    DummyAction.run_in_background(subjects=[user], params=MyParams(f='g'))
    with Queue.scheduled() as item:
        item.process()

    item.refresh_from_db()
    assert not item.is_scheduled
    assert not item.is_delayed
    assert item.retries == 1
    assert item.notes == ''


def test_group(init_user):

    class Response:

        def __init__(self, status_code: int):
            self.status_code = status_code

    dt_now = datetime.now()

    task_1, task_2, task_3, task_4 = Queue.add([
        init_user(),
        init_user('another'),
        init_user('other'),
        init_user('more'),
    ], action=DummyAction, group=True)

    assert task_1.is_scheduled
    assert task_2.is_delayed
    assert task_3.is_delayed
    assert task_4.is_delayed

    with Queue.scheduled(loud=True) as item:
        item.process()

    task_1.refresh_from_db()
    task_2.refresh_from_db()
    task_3.refresh_from_db()

    assert task_1.is_complete
    assert task_2.is_scheduled
    assert task_3.is_delayed

    with Queue.scheduled() as item:
        assert item.id == task_2.id
        raise Timeout('to postpone')

    task_2.refresh_from_db()
    task_2.after_dt = dt_now
    task_2.save()

    assert task_2.notes == 'ScheduledException: to postpone'
    assert task_2.is_scheduled

    with Queue.scheduled() as item:
        assert item.id == task_2.id
        item.retries = item.retries_max
        item.save()
        raise HTTPError('failure', response=Response(400))

    task_2.refresh_from_db()

    task_3.refresh_from_db()
    task_3.after_dt = dt_now
    task_3.save()

    task_4.refresh_from_db()
    assert task_2.is_error
    assert task_3.is_scheduled
    assert task_4.is_delayed

    Queue.stop_on_group_error = True

    with Queue.scheduled() as item:
        assert item.id == task_3.id
        item.retries = item.retries_max
        item.save()
        raise HTTPError('failure', response=Response(400))

    task_3.refresh_from_db()
    task_4.refresh_from_db()

    assert task_3.is_error
    assert task_4.is_error
