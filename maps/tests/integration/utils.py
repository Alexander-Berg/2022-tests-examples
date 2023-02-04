import os.path


MOCK_RESPONSES_FILENAME = 'responses.json'


def generate_hash(request_data):
    hash = f'{request_data.method} {request_data.url} {request_data.query}'
    return hash


def unpack_hash(hash):
    return hash.split(' ')


def build_response_filename(function_name):
    return os.path.join('maps/analytics/tools/lama/tests/integration/fixtures', f'{function_name.split(".")[0]}_{MOCK_RESPONSES_FILENAME}')


def build_input_lama_file(function_name):
    return f'{function_name.split(".")[0]}_lama.yaml'
