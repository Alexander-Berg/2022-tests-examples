from mdh.core.changes import Create, Evolve, Clone
from mdh.core.models import (
    BatchContributorRole, STATUS_ALIASES, STATUS_PUBLISHED, Batch, STATUS_DRAFT, STATUS_NOMINATED
)


def test_batches(
    drf_client,
    init_user,
    init_batch,
    init_resource,
    spawn_batch_change,
    django_assert_num_queries,
    run_task,
):
    url_prefix = '/uiapi/batches/'

    user = init_user(roles=[BatchContributorRole])
    user2 = init_user(username='another', roles=[BatchContributorRole])

    res = init_resource(user=user)

    batch_1 = init_batch(creator=user, publish=True, changes=[
        spawn_batch_change(type=Create, resource=res),
        spawn_batch_change(type=Evolve, resource=res),
    ])
    batch_2 = init_batch(creator=user2, hint='somehint')

    client = drf_client(user=user)

    # Список полный.
    with django_assert_num_queries(3):
        response = client.get(url_prefix)

    assert response.status_code == 200
    data = response.json()
    assert data['count'] == 2

    batch_data = data['results'][0]
    assert batch_data['creator'] == user.id
    assert batch_data['issue'] == ''
    assert batch_data['queue'] == ''
    assert batch_data['status'] == {'alias': 'published', 'id': 6, 'title': 'Published'}

    # Список с фильтрами
    response = client.get(f'{url_prefix}?status=published').json()
    assert response['count'] == 1
    assert response['results'][0]['id'] == batch_1.id

    response = client.get(f'{url_prefix}?creator_id={user2.id}').json()
    assert response['count'] == 1
    assert response['results'][0]['id'] == batch_2.id

    # Поиск
    response = client.get(f'{url_prefix}?search=somehint').json()
    assert response['count'] == 1
    assert response['results'][0]['id'] == batch_2.id

    # Конкретный пакет.
    with django_assert_num_queries(1):
        response = client.get(f'{url_prefix}{batch_1.id}/')

    data = response.json()
    assert data != batch_data
    assert data['creator'] == {'id': user.id, 'username': 'tester'}
    assert data['user_actions'] == {'edit': True}

    # Создание пакета.
    response = client.post(url_prefix, data={
        'description': 'somede',
    })
    data = response.json()
    assert response.status_code == 201
    assert data['status']['id'] == STATUS_DRAFT

    batch_new_id = data['id']
    assert batch_new_id

    def do_edit(*, batch_id: int, status: int, params: dict = None):
        response = client.patch(f'{url_prefix}{batch_id}/', data={
            'status': STATUS_ALIASES[status],
            **(params or {})
        })
        return response.json(), response.status_code

    # Редактирование созданного пакета.
    # Попытка смены статуса на опубликованном пакете.
    data, status = do_edit(batch_id=batch_1.id, status=STATUS_NOMINATED)
    assert status == 403
    assert 'does not allow editing' in data['error']['msg']['detail']

    # Попытка смены статуса на пакете без изменений.
    data, status = do_edit(batch_id=batch_new_id, status=STATUS_NOMINATED)
    assert status == 403
    assert 'at least one change' in data['error']['msg']['detail']

    data, status = do_edit(
        batch_id=batch_new_id,
        params={
            'description': 'newdesc',
        },
        status=STATUS_PUBLISHED  # запрос на смену статуса будет проигнорирован
    )
    assert status == 200
    assert data['status']['id'] == STATUS_DRAFT
    assert data['description'] == 'newdesc'

    # Проверка невозможности редактирования записей в некоторых статусах.
    # Публикуем запись.
    batch = Batch.objects.get(id=batch_new_id)
    assert not batch.is_published
    Batch.publish(batch)
    batch.refresh_from_db()
    assert batch.is_published
    assert batch.startrek_queue == 'TESTMDH'
    run_task('process_queue')  # проверяем работу оповещения о публикации

    response = client.patch(f'{url_prefix}{batch_new_id}/', data={})
    assert response.status_code == 403
    data = response.json()
    assert data['error']['type'] == 'PermissionDenied'


def test_batch_changes(
    drf_client,
    init_user,
    init_batch,
    init_resource,
    init_reference,
    spawn_batch_change,
    django_assert_num_queries,
):

    url_prefix = '/uiapi/batch_changes/'

    user = init_user(roles=[BatchContributorRole])

    resource = init_resource(user=user)
    reference_1 = resource.reference
    reference_id = reference_1.id

    reference_2 = init_reference('alternative', user=user, queue='tryme')

    change_11 = spawn_batch_change(type=Create, resource=resource)
    change_12 = spawn_batch_change(type=Clone, resource=resource)
    change_21 = spawn_batch_change(type=Create, resource=resource)

    batch_1 = init_batch(creator=user, publish=True, changes=[
        change_11,
        change_12,
    ])
    batch_2 = init_batch(creator=user, changes=[change_21], queue='TESTMDH')
    batch_3 = init_batch(creator=user, changes=[], queue='')
    assert batch_3.queue == ''

    client = drf_client(user=user)

    # Список полный.
    with django_assert_num_queries(3):
        response = client.get(url_prefix)

    assert response.status_code == 200
    data = response.json()
    assert data['count'] == 3

    change_data = data['results'][0]
    assert 'schema' not in change_data
    assert change_data['creator'] == user.id
    assert change_data['type'] == Create.id
    assert change_data['reference'] == reference_id
    assert change_data['batch'] == batch_1.id
    assert change_data['status'] == {'alias': 'published', 'id': 6, 'title': 'Published'}

    # Список с фильтрами
    response = client.get(f'{url_prefix}?status=published').json()
    assert response['count'] == 2
    assert response['results'][0]['id'] == batch_1.id

    response = client.get(f'{url_prefix}?batch_id={batch_2.id}').json()
    assert response['count'] == 1
    assert response['results'][0]['id'] == change_21.id

    response = client.get(f'{url_prefix}?type={Create.id}').json()
    assert response['count'] == 2
    assert response['results'][0]['id'] == change_11.id

    # Конкретное изменение.
    with django_assert_num_queries(1):
        response = client.get(f'{url_prefix}{change_11.id}/')

    data = response.json()
    assert data != change_data
    assert data['creator'] == {'id': user.id, 'username': 'tester'}
    assert data['reference'] == reference_id
    assert data['user_actions'] == {'edit': True}

    def do_create(*, reference_id: int, batch: int, queries: int):
        with django_assert_num_queries(queries) as _:
            response = client.post(url_prefix, data={
                'batch': batch,
                'type': Create.id,
                'reference': reference_id,
            })
        return response.json(), response.status_code

    # Регистрация дополнительного изменения.
    data, status = do_create(reference_id=reference_id, batch=batch_2.id, queries=5)
    assert status == 201
    assert data['status']['id'] == STATUS_DRAFT
    change_new_id = data['id']
    assert change_new_id

    # Попытка регистрации изменения из справочника с другой очередью.
    data, status = do_create(reference_id=reference_2.id, batch=batch_2.id, queries=3)

    # Первым блин комом - не подвязана схема.
    assert data['error']['msg'] == {'reference': ['No schema found for the reference']}
    reference_2.schema = reference_1.schema
    reference_2.save()

    # Далее тоже плохо - очередь ведь другая.
    data, status = do_create(reference_id=reference_2.id, batch=batch_2.id, queries=4)
    assert status == 400
    assert data['error']['msg'] == [
        'This batch accepts changes only for "TESTMDH" queue. Changes for "tryme" require a separate batch']

    # Проверяем, что проставилась очередь в ранее пустовавшем пакете.
    data, status = do_create(reference_id=reference_2.id, batch=batch_3.id, queries=6)
    assert status == 201
    batch_3.refresh_from_db()
    assert batch_3.queue == 'tryme'

    def do_edit(*, change_id: int, queries: int, params: dict = None):
        with django_assert_num_queries(queries) as _:
            response = client.patch(f'{url_prefix}{change_id}/', data={
                **(params or {})
            })
        return response.json(), response.status_code

    # Редактирование созданного пакета.
    # Попытка обновления данных опубликованного пакета.
    data, status = do_edit(change_id=change_11.id, params={'values': {'a': 'b'}}, queries=1)
    assert status == 400
    assert data['error']['msg'] == {'batch': ['The batch does not support edit']}

    # Попытка смены неизменяемых атрибутов.
    data, status = do_edit(change_id=change_new_id, params=dict(
        status=STATUS_ALIASES[STATUS_NOMINATED],
        batch=999,
        type=111,
        reference=222,
        values={'remote_id': '4444'}
    ), queries=2)

    assert status == 200
    assert data['status']['id'] == STATUS_DRAFT
    assert data['batch'] == batch_2.id
    assert data['type'] == Create.id
    assert data['reference'] == reference_id
    assert data['values'] == {'attrs': {}, 'remote_id': '4444', 'since': None, 'till': None}

    # Удаляем изменение.
    # Удачное удаление.
    response = client.delete(f'{url_prefix}{change_new_id}/')
    assert response.status_code == 204
    # А теперь неудачное - пакет опубликован.
    response = client.delete(f'{url_prefix}{change_11.id}/')
    assert response.status_code == 403
    assert 'does not allow delete of' in response.json()['error']['msg']['detail']
