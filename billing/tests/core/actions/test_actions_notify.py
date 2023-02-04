from mdh.core.actions import NotifyMasterPublished, NotifyCandidateUpdated, NotifyPublishError, NotifyLbExported
from mdh.core.changes import Clone
from mdh.core.models import STATUS_NOMINATED, STATUS_ON_REVIEW, Queue, Record


def test_notify_published(
    init_resource_fk,
    init_user,
    response_mock,
    django_assert_num_queries,
    init_batch,
    spawn_batch_change
):

    resource = init_resource_fk()
    user = resource.creator

    record_fk = resource.record_add(creator=user, issue='TESTMDH-00', attrs={'integer1': 30, 'fk1': None})
    Record.publish(record_fk)

    record_0 = resource.record_add(attrs={'integer1': 1}, issue='TESTMDH-0', creator=user)
    record_0.set_master()
    record_0.save()

    record_1 = resource.record_add(
        attrs={'integer1': 2, 'fk1': record_fk.master_uid},
        issue='TESTMDH-1', creator=user, master_uid=record_0.master_uid,
    )
    Record.publish(record_1)

    record_2 = resource.record_add(
        attrs={'integer1': 3, 'fk1': record_fk.master_uid},
        issue='TESTMDH-2', creator=user, master_uid=record_0.master_uid,
        status=Record.STATUS_ON_REVIEW,
    )

    record_3 = resource.record_add(
        attrs={'integer1': 3, 'fk1': record_1.master_uid},
        issue='TESTMDH-3', creator=user,
        status=Record.STATUS_ON_REVIEW,
    )

    responses = [
        # обновили саму задачу
        'PATCH https://st-api.test.yandex-team.ru/v2/issues/TESTMDH-1 -> 200:{}',
        # перевели статус задачи
        'POST https://st-api.test.yandex-team.ru/v2/issues/TESTMDH-1/transitions/closed/_execute -> 200:[]',
        # обновили родственника на рассмотрении
        'PATCH https://st-api.test.yandex-team.ru/v2/issues/TESTMDH-2 -> 200:{}',
        # обновили задачу, ссылающуюся на данную
        'PATCH https://st-api.test.yandex-team.ru/v2/issues/TESTMDH-3 -> 200:{}',
    ]

    bypass = False

    with response_mock(responses, bypass=bypass):

        action = NotifyMasterPublished(obj=record_1)

        with django_assert_num_queries(8) as _:
            result = action.run()

        assert result

        siblings = result.data['siblings']
        assert len(siblings) == 1
        updated_sibling = siblings[0]
        assert updated_sibling.id == record_2.id

        dependents = result.data['dependents']
        assert len(dependents) == 1
        updated_dependent = dependents[0]
        assert updated_dependent.id == record_3.id

    # Проверяем обработку параметров действия.
    with response_mock(responses[2:]):
        action = NotifyMasterPublished(obj=record_1, params={'update_issue': False})
        assert action.run()

    # Проверяем оповещение для пакетных изменений.
    record_4 = resource.record_add(
        attrs={'integer1': 4, 'fk1': None},
        issue='TESTMDH-4', creator=user,
        master_uid=record_1.master_uid,
    )

    change_1 = spawn_batch_change(
        type=Clone,
        resource=resource,
        values={'attrs': {'string1': 'myvalue-batched'}},
        record=record_1,
    )
    assert change_1.record.id == record_1.id
    batch_1 = init_batch(creator=user, changes=[change_1], queue='some', issue='DUMDUM-1', status=STATUS_ON_REVIEW)

    Record.publish(record_4)
    action = Queue.objects.last().action_object

    responses = [
        # обновили задачу записи.
        'PATCH https://st-api.test.yandex-team.ru/v2/issues/TESTMDH-4 -> 200:{}',
        # перевели задачу записи
        'POST https://st-api.test.yandex-team.ru/v2/issues/TESTMDH-4/transitions/closed/_execute -> 200:[]',
        # обновили задачу пакета
        'PATCH https://st-api.test.yandex-team.ru/v2/issues/DUMDUM-1 -> 200:{}',
        # обновили родственника на рассмотрении
        'PATCH https://st-api.test.yandex-team.ru/v2/issues/TESTMDH-2 -> 200:{}',
        # обновили задачу, ссылающуюся на данную
        'PATCH https://st-api.test.yandex-team.ru/v2/issues/TESTMDH-3 -> 200:{}',
    ]

    with response_mock(responses):
        result = action.run()

    change_1.refresh_from_db()
    assert change_1.record.id == record_4.id
    assert result.data['batches'][0].id == batch_1.id
    assert result.data['siblings'][0].id == record_2.id
    assert result.data['dependents'][0].id == record_3.id


def test_notify_updated(init_record, response_mock):

    record_0 = init_record(record_kwargs={'status': STATUS_NOMINATED})

    # Задача на создание раздела в Логброкере.
    assert Queue.objects.count() == 1

    record_0.mark_on_review()
    record_0.save()
    # Ничего не поменялось при первом переходе статуса.
    assert Queue.objects.count() == 1

    record_0.save()
    # Повторные редактирования в статусе вызывают актуализацию записи в Трекере.
    assert Queue.objects.count() == 2

    bypass = False

    with response_mock('PATCH https://st-api.test.yandex-team.ru/v2/issues/TESTMDH-1 -> 200:{}', bypass=bypass):
        action = NotifyCandidateUpdated(obj=record_0)
        result = action.run()
        assert result
        assert result.success


def test_notify_publish_error(init_record, response_mock):

    record_0 = init_record()

    bypass = False

    with response_mock('POST https://st-api.test.yandex-team.ru/v2/issues/TESTMDH-1/transitions/new/_execute -> 200:{}', bypass=bypass):
        action = NotifyPublishError(obj=record_0)
        result = action.run()
        assert result
        assert result.success


def test_notify_lb_exported(init_record):

    record_0 = init_record()
    assert record_0.dt_lb is None

    action = NotifyLbExported(obj=None, params={
        'dt': '2021-09-30T09:57',
        'records': [record_0.id]
    })
    result = action.run()
    assert result.success
    record_0.refresh_from_db()
    assert record_0.dt_lb
