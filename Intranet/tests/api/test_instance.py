from mock import patch

from django.urls import reverse

from intranet.compositor.src.api.views.instance import InstanceDetailApiView


def test_get_detail_instance_success_status(client, test_vcr):
    url = reverse(
        "api_v1:instance-detail",
        kwargs={'pk': '8d10644c-b4dd-471e-b6d0-e21ff21edad5'},
    )
    expected_data = {
        "org_id": 42,
        "uid": "4018295016",
        "service_slug": "wiki",
    }
    with test_vcr.use_cassette('nirvana_get_detail_instance_success_status.yaml'):
        with patch.object(
                InstanceDetailApiView, '_get_json_from_mds',
        ) as mocked_mds:
            mocked_mds.return_value = expected_data
            response = client.get(url)

    assert response.status_code == 200
    response_json = response.json()

    assert response_json['output'] == expected_data
    assert response_json['result'] == 'success'
    assert response_json['status'] == 'completed'


def test_get_detail_instance_fail_status(client, test_vcr):
    url = reverse(
        "api_v1:instance-detail",
        kwargs={'pk': '5366a2b8-199e-490e-96b5-27ea58cac069'},
    )
    with test_vcr.use_cassette('nirvana_get_detail_instance_fail_status.yaml'):
        response = client.get(url)

    assert response.status_code == 200
    response_json = response.json()

    assert 'output' not in response_json

    assert response_json['result'] == 'failure'
    assert response_json['status'] == 'completed'


def test_get_detail_instance_wrong_id_fail(client, test_vcr):
    url = reverse(
        "api_v1:instance-detail",
        kwargs={'pk': '8d10644c-b4dd-471e-b6d0-e21ff21edad512'},
    )
    with test_vcr.use_cassette('nirvana_get_detail_instance_wrong_id_fail.yaml'):
        response = client.get(url)

    assert response.status_code == 404


def test_restart_success_finished_instance(client, test_vcr):
    url = reverse(
        'api_v1:instance-restart',
        kwargs={'pk': 'fa08a83e-7921-473c-b180-5bc6a1f96df3'},
    )
    with test_vcr.use_cassette('nirvana_get_success_finished_workflow_instance_state.yaml'):
        response = client.post(url)
    assert response.status_code == 422


def test_restart_failed_instance(client, test_vcr):
    url = reverse(
        'api_v1:instance-restart',
        kwargs={'pk': '613ca2c2-35eb-40eb-b537-7bcc4e09b840'},
    )

    with test_vcr.use_cassette('nirvana_get_failed_workflow_instance_and_restart_it.yaml'):
        response = client.post(url)

    assert response.status_code == 200

    response_json = response.json()
    assert response_json['workflow_instance_id'] == 'b12568fe-473a-423e-b1ad-f6e0e0da597c'
