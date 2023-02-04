from mdh.core.models import GoverningRole, Resource


def test_resources(drf_client, init_user, init_node, init_resource, django_assert_num_queries):

    user = init_user(roles=[GoverningRole])

    node = init_node('mynode', user=user)

    resource1 = init_resource(user=user, node=node, alias_postfix='1')
    reference_link = resource1.reference_link

    init_resource(user=user, reference=reference_link.reference, schema=resource1.schema, alias_postfix='2')
    init_resource(
        user=user,
        node=resource1.node, reference=reference_link.reference, schema=resource1.schema,
        alias_postfix='3'
    )

    client = drf_client(user=user)

    # Список полный.
    with django_assert_num_queries(3):
        response = client.get('/uiapi/resources/')

    assert response.status_code == 200

    data = response.json()
    assert data['count'] == 3

    resource = data['results'][0]
    assert resource['creator'] == user.id
    assert resource['status'] == {'alias': 'draft', 'id': 1, 'title': 'Draft'}
    assert resource['type'] == {'alias': 'src', 'id': 1, 'title': 'Source'}
    assert resource['node'] == resource1.node.id
    assert resource['url']

    # Список с фильтрами
    response = client.get(
        f'/uiapi/resources/?node={resource1.node.alias}'
    ).json()
    assert response['count'] == 2

    # Конкретная запись.
    with django_assert_num_queries(1):
        response = client.get(f'/uiapi/resources/{resource1.id}/')

        data = response.json()
        # В детальной выдаче больше данных, чем в списковой.
        assert data['reference']['id']
        assert 'alias' in data['node']
        assert data['creator'] == {'id': user.id, 'username': 'tester'}
        assert data['reference']['alias']
        assert data['domain']['alias']

    # Создание записи.

    resource_basic_data = {
        'titles': {},
        'hints': {},
        'type': Resource.TYPE_DESTINATION,
        'node': node.id,
        'reference_link': reference_link.id,
    }

    response = client.get(f'/uiapi/resources/{resource1.id}/')
    data = response.json()
    assert data['user_actions'] == {'edit': True}

    response = client.post('/uiapi/resources/', data=resource_basic_data)
    data = response.json()
    assert response.status_code == 201

    resource_id = data['id']
    assert resource_id
    assert data['status'] == {'alias': 'published', 'id': 6, 'title': 'Published'}

    # Редактирование созданного ресурса.
    response = client.patch(f'/uiapi/resources/{resource_id}/', data={
        **resource_basic_data,
        'titles': {
            'ru': 'test',
        },
        'type': Resource.TYPE_SOURCE,  # игнорируется
    })
    data = response.json()
    assert response.status_code == 200
    assert data['type']['id'] == Resource.TYPE_DESTINATION
    assert data['titles'] == {'ru': 'test'}
