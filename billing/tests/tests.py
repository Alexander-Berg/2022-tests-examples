from billing.contrib.py3solomon.py3solomon import (
    push,
    Sensor,
    API,
    Solomon
)

all_purpose_id = 1488


def test_solomon(requests_mock):
    host = 'http://solomon-sever/'
    path = 'get/data'

    requests_mock.get(host + API + path, request_headers={'Authorization': 'OAuth xxx'}, text='ok')
    s = Solomon(host, 'xxx')
    s.raw_get(path)

    request = requests_mock.last_request
    assert request.method == 'GET'
    assert request.url == host + API + path

    requests_mock.post(host + API + path, request_headers={'Authorization': 'OAuth xxx'}, text='ok')
    s.post(path, {'a': 1})

    request = requests_mock.last_request
    assert request.method == 'POST'
    assert request.url == host + API + path
    assert request.json() == {'a': 1}


def test_push(requests_mock):
    raw_fake_shard = {
        'createdAt': "dobyri vecher",
        'createdBy': "hitler",
        'state': 'nice',
        'clusterId': all_purpose_id,
        'serviceId': all_purpose_id,
    }

    raw_fake_cluster = {
        "name": "antarktida",
        'createdAt': "dobyri vecher",
        'createdBy': "hitler",
    }

    raw_fake_service = {
        "name": "cake",
        'createdAt': "dobyri vecher",
        'createdBy': "hitler",
    }

    host = 'http://solomon-sever/'
    sensor = Sensor("hohoho", valenok="poleno")
    sensor.value = 666

    project_url = host + API + f"projects/{all_purpose_id}"
    shard_url = project_url + f"/shards/{all_purpose_id}"
    cluster_url = project_url + f"/clusters/{all_purpose_id}"
    service_url = project_url + f"/services/{all_purpose_id}"

    headers = {'Authorization': 'OAuth xxx'}

    requests_mock.get(shard_url, request_headers=headers, json=raw_fake_shard)
    requests_mock.get(cluster_url, request_headers=headers, json=raw_fake_cluster)
    requests_mock.get(service_url, request_headers=headers, json=raw_fake_service)

    push_url = host + API + 'push/?service=cake&project=1488&cluster=antarktida'
    requests_mock.post(push_url, headers=headers, text='ok')

    push(host, all_purpose_id, all_purpose_id, sensor, token="xxx")
