from datetime import datetime

import pytest
from freezegun import freeze_time

from mdh.core.changes import Create, Clone, Evolve
from mdh.core.exceptions import BatchError
from mdh.core.models import Record, Queue


def test_startrek_issue(
    init_user, init_resource, django_assert_num_queries, assert_startrek_create_update,
    init_batch, spawn_batch_change,
):

    user = init_user()
    res = init_resource(user=user)
    record_1 = res.record_add(creator=user, attrs={'integer1': 111})

    batch_1 = init_batch(creator=user, changes=[
        spawn_batch_change(type=Create, resource=res),
        spawn_batch_change(type=Clone, resource=res, record=record_1),
    ])

    assert not batch_1.issue
    assert f'{batch_1}'

    with django_assert_num_queries(7) as _:

        title = batch_1.startrek_title
        assert str(batch_1.id) in title

        descr = batch_1.startrek_description
        assert '===Create: 1' in descr
        assert '===Clone: 1' in descr

    assert_startrek_create_update(obj=batch_1, issue_key='TESTMDH-8', bypass=False)

    batch_1.refresh_from_db()
    assert batch_1.issue


def test_publish(init_user, init_resource, django_assert_num_queries, init_batch, spawn_batch_change):

    user = init_user()
    res = init_resource(user=user, publish=True)

    record_1 = res.record_add(creator=user, attrs={'integer1': 111})
    record_2 = res.record_add(creator=user, attrs={'integer1': 222})
    Record.publish(record_2)

    change_create = spawn_batch_change(
        type=Create, resource=res,
        type_params={'count': 3},
        values={'attrs': {'integer1': '{{counter}}', 'string1': 'myvalue-bnum{{batch_num}}'}}
    )
    change_clone = spawn_batch_change(
        type=Clone,
        resource=res,
        type_params={'count': 2},
        values={'attrs': {'string1': 'myvalue-cnttype{{counter_type}}-dt{{date}}'}},
        record=record_1,
    )
    change_evolve = spawn_batch_change(
        type=Evolve,
        resource=res,
        values={'attrs': {'string1': 'myvalue-cnt{{counter}}'}},
        record=record_2,
    )

    batch_issue = 'other-11'

    init_batch(creator=user)  # для проверки счётчика пакетов
    batch_1 = init_batch(creator=user, changes=[
        change_create,
        change_clone,
        change_evolve,
    ], queue='some', issue=batch_issue)

    with freeze_time(datetime(2021, 4, 27)):
        with django_assert_num_queries(31) as _:
            batch_1.publish(batch_1)

    records = list(Record.objects.all())
    assert len(records) == 8

    records_create = [records[2], records[3], records[4]]
    for idx, record in enumerate(records_create, 1):
        assert record.attrs == {'string1': f'myvalue-bnum{batch_1.id}', 'integer1': idx}
        assert record.status == record_1.status
        assert record.issue == batch_issue

    records_clone = [records[5], records[6]]
    for record in records_clone:
        assert record.attrs == {'string1': 'myvalue-cnttype2-dt2021-04-27', 'integer1': 111}
        assert record.status == record_1.status
        assert record.issue == batch_issue

    record_evolve = records[7]
    assert record_evolve.attrs == {'string1': 'myvalue-cnt0', 'integer1': 222}
    assert record_evolve.is_published
    record_2.refresh_from_db()
    assert record_2.is_archived
    assert record_2.master_id == record_evolve.id
    assert record_evolve.issue == batch_issue

    queue = list(Queue.objects.all())
    assert len(queue) == 4
    assert queue[2].params == {'update_issue': False, 'update_changes': []}  # Сообщение о публикации записи
    assert queue[3].params == {}  # Сообщение о публикации пакета


def test_publish_error(init_user, init_resource, django_assert_num_queries, init_batch, spawn_batch_change):

    user = init_user()
    res = init_resource(user=user, publish=True)

    record_1 = res.record_add(creator=user, attrs={'integer1': 111})
    record_2 = res.record_add(creator=user, attrs={'integer1': 222}, master_id=record_1.id)

    change_clone = spawn_batch_change(
        type=Clone,
        resource=res,
        values={'attrs': {'string1': 'toobogus'}},
        record=record_1,
    )
    change_create = spawn_batch_change(
        type=Create, resource=res,
        type_params={'count': 3},
        values={'attrs': {'integer1': 'bogus', 'string1': 'buggy'}}
    )
    change_create_stale_master = spawn_batch_change(
        type=Clone, resource=res,
        type_params={'count': 1},
        values={'attrs': {'integer1': 333}},
        record=record_2,
    )

    batch_1 = init_batch(creator=user, changes=[
        change_clone,
        change_create,
        change_create_stale_master,
    ], editor=user)

    with django_assert_num_queries(12) as _:
        with pytest.raises(BatchError) as e:
            batch_1.publish(batch_1)

    error = f'{e.value}'
    assert f'ID {change_clone.id} **Clone**' in error
    assert '  - string1:: string does not match regex "myvalue*"' in error
    assert f'ID {change_create.id} **Create**' in error
    assert '  - integer1:: value is not a valid integer' in error

    batch_1.refresh_from_db()
    assert batch_1.is_on_review  # вернулась не пересмотр
    assert batch_1.changes.first().is_on_review  # статус взят из пакета

    queue = list(Queue.objects.all())
    assert len(queue) == 2
    assert f'ID {change_clone.id} **Clone**' in queue[1].action_object.params.err

    with django_assert_num_queries(10) as _:
        description = batch_1.startrek_description

    assert '===Clone: 2' in description
    assert '| %%toobogus%% | - %%string does not match regex "myvalue*"%% ||' in description
    assert '| %%bogus%% | - %%value is not a valid integer%% ||' in description
    assert 'in the batch have been changed' in description
