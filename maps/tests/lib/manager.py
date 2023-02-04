from maps.automotive.libs.large_tests.lib.http import http_request, http_request_json

MANAGER = 'http://127.0.0.1:9003'
HOST = 'auto-remote-tasks-manager.maps.yandex.net'
HEADERS = {'Host': HOST}


def set_url(url):
    global MANAGER
    MANAGER = url


def get_url():
    return MANAGER


def get_host():
    return HOST


def get_available_task(head_unit):
    return http_request(
        'GET', MANAGER + '/tasks/1.x/get_available_task',
        params={
            'head_id': head_unit.head_id,
            'device_id': head_unit.device_id,
            'uuid': head_unit.uuid,
        },
        headers={
            'Accept': 'application/x-protobuf',
            'Host': HOST,
        })


def upload_result(task_result):
    params = {
        'task_id': task_result.task_id,
        'head_id': task_result.head_unit.head_id,
        'uuid': task_result.head_unit.uuid,
    }
    if task_result.head_unit.device_id is not None:
        params['device_id'] = task_result.head_unit.device_id

    result_kind = 'result' if not task_result.error else 'error'
    return http_request(
        'POST', MANAGER + '/tasks/1.x/upload_' + result_kind,
        params=params,
        headers={
            'Accept': 'application/x-protobuf',
            'Host': HOST,
        },
        data=task_result.result)


def upload_file(anonymous_file):
    return http_request(
        'POST', MANAGER + '/reports/1.x/upload_file',
        params={
            'file_id': anonymous_file.file_id,
            'uuid': anonymous_file.head_unit.uuid,
            'head_id': anonymous_file.head_unit.head_id,
            'device_id': anonymous_file.head_unit.device_id
        },
        headers=HEADERS,
        data=anonymous_file.contents)


def get_experiment_config(head_id, experiment_name):
    return http_request_json(
        'GET', MANAGER + '/config',
        params={
            'headid': head_id,
            'experiment_name': experiment_name,
        },
        headers=HEADERS)


def put_is_alive(head_id, head_timestamp):
    return http_request_json(
        'PUT', MANAGER + '/is_alive',
        params={
            'headid': head_id,
            'head_timestamp': head_timestamp,
        },
        headers=HEADERS)
