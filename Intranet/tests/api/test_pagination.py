from django.urls import reverse


def test_get_list_workflow_pagination(client, test_vcr):
    url = reverse("api_v1:workflows-list")

    with test_vcr.use_cassette('nirvana_list_workflow_success.yaml'):
        response = client.get(f'{url}?per_page=1')

    assert response.status_code == 200
    response_json = response.json()
    assert len(response_json['results']) == 1
    assert response_json['results'][0] == {
        "id": "e4fb960c-6eb3-412f-80de-deb70c02c277",
        "name": "test2",
    }

    with test_vcr.use_cassette('nirvana_list_workflow_success.yaml'):
        response = client.get(response_json['next'])

    assert response.status_code == 200
    response_json = response.json()
    assert len(response_json['results']) == 1
    assert response_json['results'][0] == {
        "id": "e872ea5c-de0e-4480-bd04-c82a181e3f7b",
        "name": "DIR-7910 Тестовый воркфлоу",
    }
    assert response_json['next'] is None
