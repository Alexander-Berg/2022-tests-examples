from mdh.core.models import SupportRole, Nested


def test_nested(drf_client, init_reference, init_user, django_assert_num_queries):

    user = init_user()
    ref1 = init_reference(alias='ref1', user=user)
    ref2 = init_reference(alias='ref2', user=user)
    ref3 = init_reference(alias='ref3', user=user)
    ref4 = init_reference(alias='ref4', user=user)
    ref5 = init_reference(alias='ref5', user=user)

    client = drf_client(user=user)
    Nested.objects.bulk_create([
        Nested(parent=ref1, child=ref2, child_attr='a'),
        Nested(parent=ref3, child=ref4, child_attr='c'),
    ])

    # Список полный.
    with django_assert_num_queries(3) as _:
        response = client.get('/uiapi/nested/')

    assert response.status_code == 200
    data = response.json()
    assert data['count'] == 2

    nested = data['results'][0]
    assert nested['parent'] == ref1.id
    assert nested['url']
    # Список с фильтрами
    response = client.get(
        f'/uiapi/nested/?parent={ref3.id}'
    ).json()
    assert response['count'] == 1
    assert response['results'][0]['child'] == ref4.id

    # Конкретная запись
    with django_assert_num_queries(1):
        response = client.get(f"/uiapi/nested/{nested['id']}/")

    data = response.json()
    assert 'url' in data

    # Создание связи. Крепим второй справочник к первой области.
    nested_basic_data = {
        'parent': ref1.id,
        'child': ref5.id,
        'child_attr': 'b',
    }

    def create():
        return client.post('/uiapi/nested/', data=nested_basic_data)

    response = create()
    data = response.json()
    assert response.status_code == 403
    assert data['error']['msg']['detail'] == 'You do not have enough permissions'

    user.role_add(roles=[SupportRole], author=user)

    response = create()
    data = response.json()
    assert response.status_code == 201
    assert data['child_attr'] == 'b'

    new_nested_id = data['id']
    assert new_nested_id

    # Редактирование созданной связи.
    response = client.patch(f'/uiapi/nested/{new_nested_id}/', data={
        **nested_basic_data,  # не будет изменено
        'child_attr': 'd',
    })
    data = response.json()
    assert response.status_code == 200
    assert data['child_attr'] == 'd'

    # Проба удаления связи.
    response = client.delete(f'/uiapi/nested/{new_nested_id}/')
    assert response.status_code == 204
