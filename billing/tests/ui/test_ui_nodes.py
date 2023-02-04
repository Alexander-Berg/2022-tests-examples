from mdh.core.models import GoverningRole, STATUS_ALIASES, STATUS_PUBLISHED


def test_nodes(
    drf_client,
    init_user,
    init_node,
    init_resource,
    init_domain,
    init_reference,
    django_assert_num_queries,
):

    user = init_user(roles=[GoverningRole])

    node_1 = init_node('node1', user=user, publish=True)

    dom1 = init_domain('dom1', user=user)
    ref1 = init_reference('ref1', user=user)
    init_resource(user=user, domain=dom1, reference=ref1, node=node_1, publish=True)

    init_node('node2', user=user)

    client = drf_client(user=user)

    # Список полный.
    with django_assert_num_queries(3):
        response = client.get('/uiapi/nodes/')

    assert response.status_code == 200

    data = response.json()
    assert data['count'] == 2

    node_data = data['results'][0]
    assert node_data['alias'] == node_1.alias
    assert node_data['creator'] == user.id
    assert node_data['status'] == {'alias': 'published', 'id': 6, 'title': 'Published'}
    assert node_data['url']

    # Список с фильтрами
    response = client.get('/uiapi/nodes/?status=published').json()
    assert response['count'] == 1

    response = client.get(f'/uiapi/nodes/?domain={dom1.alias}').json()
    assert response['count'] == 1

    response = client.get(f'/uiapi/nodes/?reference={ref1.id}').json()
    assert response['count'] == 1

    # Конкретная запись.
    with django_assert_num_queries(1):
        response = client.get(f'/uiapi/nodes/{node_1.id}/')

    data = response.json()
    assert data['creator'] == {'id': user.id, 'username': 'tester'}
    assert data != node_data

    # Адресация по псевдониму.
    response = client.get(f'/uiapi/nodes/{node_1.alias}/')
    assert response.status_code == 200

    # Создание записи.

    node_basic_data = {
        'alias': 'new_node',
        'titles': {},
        'hints': {},
    }

    response = client.get(f'/uiapi/nodes/{node_1.id}/')
    data = response.json()
    assert data['user_actions'] == {'edit': True}

    response = client.post('/uiapi/nodes/', data=node_basic_data)
    data = response.json()
    assert response.status_code == 201

    node_id = data['id']
    assert node_id

    # Редактирование созданного узла.
    response = client.patch(f'/uiapi/nodes/{node_id}/', data={
        **node_basic_data,
        'titles': {
            'ru': 'test',
        },
        'alias': 'ignored',
        'status': STATUS_ALIASES[STATUS_PUBLISHED],
    })
    data = response.json()
    assert response.status_code == 200
    assert data['status']['id'] == STATUS_PUBLISHED
    assert data['alias'] != 'ignored'
    assert data['titles'] == {'ru': 'test'}
