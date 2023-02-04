from unittest import mock
import pytest

from intranet.search.core.models import PushInstance, PushRecord
from intranet.search.core.swarm.pushes import restart_push
from intranet.search.tests.helpers.models_helpers import (
    create_push,
    create_push_instance,
    create_revision,
    reload_obj,
    create_indexation,
)

pytestmark = pytest.mark.django_db(transaction=False)


def test_restart_single_push():
    push = create_push(meta={'id': 'someid'})
    revision = create_revision(search=push.search, index=push.index)
    instance = create_push_instance(push, indexation=None, revision=revision)

    with mock.patch('intranet.search.core.swarm.indexer.Indexer.next', autospec=True) as p:
        restart_push(push.id, only_failed=False)

    assert_push_changed(push)
    assert reload_obj(instance).status == PushInstance.STATUS_NEW

    assert p.call_count == 1
    indexer = p.call_args[0][0]
    p.assert_called_once_with(indexer, 'push', data=push.meta)
    assert_valid_indexer_for_single_push(indexer, instance)


def test_restart_indexation_push():
    push = create_push(meta={'id': 'someid'})
    keys = ['queue1', 'queue2']
    indexation = create_indexation(
        revision=create_revision(search=push.search, index=push.index),
        options={'keys': keys},
    )
    instance = create_push_instance(push, indexation=indexation, revision=None)

    with mock.patch('intranet.search.core.swarm.indexer.Indexer.next', autospec=True) as p:
        restart_push(push.id, only_failed=False)

    assert_push_changed(push)
    # В случае пуша про индексацию мы заводим новую push_instance, привязанную к новой индексации,
    # а старой ставим статус retry
    assert reload_obj(instance).status == PushInstance.STATUS_RETRY
    new_instance = push.instances.exclude(id=instance.id).get()
    assert_new_indexation_instance_valid(new_instance, instance)

    assert p.call_count == 1
    indexer = p.call_args[0][0]
    p.assert_called_once_with(indexer, 'setup')
    assert_valid_indexer_for_indexation_push(indexer, new_instance)


def test_restart_only_failed_instance():
    """ При флаге only_failed=True перезапускаем только упавшие инстансы
    """
    push = create_push(status=PushRecord.STATUS_FAIL,  meta={'id': 'someid'})
    done_instance = create_push_instance(push, revision=create_revision(), status=PushInstance.STATUS_DONE)
    fail_instance = create_push_instance(push, revision=create_revision(), status=PushInstance.STATUS_FAIL)
    retry_instance = create_push_instance(push, revision=create_revision(), status=PushInstance.STATUS_RETRY)

    with mock.patch('intranet.search.core.swarm.indexer.Indexer.next', autospec=True) as p:
        restart_push(push.id, only_failed=True)

    assert_push_changed(push)
    assert reload_obj(fail_instance).status == PushInstance.STATUS_NEW
    assert reload_obj(done_instance).status == done_instance.status
    assert reload_obj(retry_instance).status == retry_instance.status

    # пуш вызвался только один раз для инстанса в статусе 'fail'
    assert p.call_count == 1
    indexer = p.call_args[0][0]
    p.assert_called_once_with(indexer, 'push', data=push.meta)
    assert_valid_indexer_for_single_push(indexer, fail_instance)


def test_different_indexes_single_push():
    """ Бывают ситуации, что у пуша есть несколько инстанов для разных индексаторов.
    Например, пуш об изменении названия очереди попрождает одиночный пуш в индекс st.queues
    и переиндексацию всех задач этой очереди (то есть индексацию в st.default c keys=['<QUEUE>'].
    Проверяем, что такие ретраи обрабатываются правильно в случае одиночных пушей.
    """
    push = create_push(search='st', index='')
    queue_revision = create_revision(search='st', index='queues')
    queue_instance = create_push_instance(push, revision=queue_revision)

    with mock.patch('intranet.search.core.swarm.indexer.Indexer.next', autospec=True) as p:
        restart_push(push.id, only_failed=False)

    assert p.call_count == 1
    indexer = p.call_args[0][0]
    assert_valid_indexer_for_single_push(indexer, queue_instance)


def test_different_indexes_indexation_push():
    """ Бывают ситуации, что у пуша есть несколько инстанов для разных индексаторов.
    Например, пуш об изменении названия очереди попрождает одиночный пуш в индекс st.queues
    и переиндексацию всех задач этой очереди (то есть индексацию в st.default c keys=['<QUEUE>'].
    Проверяем, что такие ретраи обрабатываются правильно в случае пушей об индексации.
    """
    push = create_push(search='st', index='')
    queue_indexation = create_indexation(
        revision=create_revision(search='st', index='queues'),
        options={'keys': ['key1']},
    )
    queue_instance = create_push_instance(push, indexation=queue_indexation)

    with mock.patch('intranet.search.core.swarm.indexer.Indexer.next', autospec=True) as p:
        restart_push(push.id, only_failed=False)

    assert p.call_count == 1
    indexer = p.call_args[0][0]
    new_instance = push.instances.exclude(id=queue_instance.id).get()
    assert_valid_indexer_for_indexation_push(indexer, new_instance)


def assert_push_changed(push):
    updated_push = reload_obj(push)
    assert updated_push.status == PushInstance.STATUS_RETRY
    assert updated_push.retries == push.retries + 1


def assert_new_indexation_instance_valid(new_instance, old_instance):
    assert new_instance.status == PushInstance.STATUS_NEW
    assert new_instance.indexation.search == old_instance.indexation.search
    assert new_instance.indexation.index == old_instance.indexation.index
    assert new_instance.indexation.options['keys'] == old_instance.indexation.options['keys']


def assert_valid_indexer_for_single_push(indexer, push_instance):
    assert indexer.revision['id'] == push_instance.revision_id
    assert indexer.indexation_id is None
    assert indexer.push_id == push_instance.push_id
    assert indexer.options['index'] == push_instance.revision.index


def assert_valid_indexer_for_indexation_push(indexer, push_instance):
    assert indexer.revision['id'] == push_instance.indexation.revision_id
    assert indexer.indexation_id == push_instance.indexation_id
    assert indexer.push_id == push_instance.push_id
    assert indexer.options['index'] == push_instance.indexation.revision.index
    assert indexer.options['keys'] == push_instance.indexation.options['keys']
