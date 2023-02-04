import pytest
import json

from utils import unpack_hash, build_response_filename, build_input_lama_file
from library.python import resource


@pytest.fixture(scope="module")
def data_response(request):
    response_file_path = build_response_filename(request.node.name)
    content = resource.resfs_read(response_file_path).decode('utf-8')
    file_data = json.loads(content)
    yield file_data


@pytest.fixture(autouse=True)
def api_mock(tmp_path, requests_mock, data_response, request):
    caller_function_name = request.node.name
    data = data_response[caller_function_name]

    for key in data:
        method, url, query = unpack_hash(key)
        response_list = list(map(lambda response: {'json': response['json'], 'status_code': response['status_code']}, data[key]))
        requests_mock.register_uri(method, url + query, response_list)

    input_filename = build_input_lama_file(request.node.name)
    content = resource.resfs_read(f'maps/analytics/tools/lama/tests/integration/fixtures/{input_filename}').decode('utf-8')
    with open(f'{tmp_path}/{input_filename}', 'w') as f:
        f.write(content)

    return tmp_path
