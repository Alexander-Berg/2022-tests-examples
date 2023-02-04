from django.urls import reverse


def test_get_detail_workflow_success(client, test_vcr):
    url = reverse(
        "api_v1:workflows-detail",
        kwargs={'workflow_slug': 'advertising-campaign'},
    )
    with test_vcr.use_cassette('nirvana_get_detail_workflow_success.yaml'):
        response = client.get(url)

    response_json = response.json()
    assert response.status_code == 200
    assert response_json == {
        'blocks': [
            {'id': '10342a70-1521-485e-9280-fdb62aaeb423',
             'name': 'Создаем организацию',
             'operation_id': 'd9a00760-b376-4589-9eee-6276b49fa33e',
             'type': 'operation'},
            {'id': 'fd367179-eb94-45b6-b8bf-39143e05b7e4',
             'name': 'Включаем трекер',
             'operation_id': 'a7668be6-35f4-4d15-b1ac-fa22291559ba',
             'type': 'operation'}
        ],
        'description': 'Создаем организацию для пользователя и включаем трекер',
        'id': 'dee5ccf3-bebf-40f1-970e-6740bcbb00b7',
        'name': 'Воркфлоу включения трекера',
    }


def test_get_detail_workflow_fail(client, test_vcr):
    url = reverse(
        'api_v1:workflows-detail',
        kwargs={'workflow_slug': 'non-existent-slug'},
    )
    response = client.get(url)
    assert response.status_code == 404
    response_json = response.json()
    assert response_json['error_code'] == 'workflow_not_found'


def test_list_workflow_success(client, test_vcr):
    url = reverse('api_v1:workflows-list')
    with test_vcr.use_cassette('nirvana_list_workflow_success.yaml'):
        response = client.get(url)
    assert response.status_code == 200
    response_json = response.json()
    assert response_json == {
        'next': None,
        'previous': None,
        'results': [
            {'id': 'e4fb960c-6eb3-412f-80de-deb70c02c277',
             'name': 'test2'},
            {'id': 'e872ea5c-de0e-4480-bd04-c82a181e3f7b',
             'name': 'DIR-7910 Тестовый воркфлоу'},
        ]
    }


def test_start_workflow(client, test_vcr):
    url = reverse(
        'api_v1:workflows-start',
        kwargs={'workflow_slug': 'advertising-campaign'},
    )

    with test_vcr.use_cassette('nirvana_start_workflow_success.yaml'):
        response = client.post(url, {
            'org_id': 1,
            'website': 'https://test.com',
        })

    assert response.status_code == 200
    response_json = response.json()
    assert response_json['workflow_instance_id'] == '3e078956-db91-4d86-9a65-e97055e7d366'


def test_start_non_existent_workflow(client):
    url = reverse(
        'api_v1:workflows-start',
        kwargs={'workflow_slug': 'non-existent-workflow'},
    )

    response = client.post(url, {
        'org_id': 1
    })

    assert response.status_code == 404
    response_json = response.json()
    assert response_json['error_code'] == 'workflow_not_found'


def test_start_workflow_with_invalid_data(client):
    url = reverse(
        'api_v1:workflows-start',
        kwargs={'workflow_slug': 'site-testing'},
    )

    response = client.post(url, {
        'org_name': 'org-1'
    })

    assert response.status_code == 400
