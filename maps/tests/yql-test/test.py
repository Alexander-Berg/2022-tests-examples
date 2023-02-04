#!/usr/bin/env python
# -*- coding: utf-8 -*-

import os
import json
import re
from pathlib import Path

import yatest.common as yc
from yql.client.parameter_value_builder import YqlParameterValueBuilder as ValueBuilder
from yql.api.v1.client import YqlClient
from yt.yson import YsonUint64


TEST_FOLDER_PATH = '../tests/fixtures'
TEST_FOLDER_PATH2 = '../tests/config.json'

is_ci = os.getenv('CI') == "1"
is_yql_new = os.getenv('IS_YQL_NEW') == "true"


def test_yql(yql_api, yt):
    yql_query_path = yc.get_param('yql-query-path')
    is_save = yc.get_param('save') == 'True'

    print(f"Yql port: {yql_api.port}")
    print(f"Yt port: {yt.yt_proxy_port}")
    print(f'Query path: {yql_query_path}')

    print("is_ci", is_ci)
    print("is_yql_new", is_yql_new)

    yql_query = open(yql_query_path).read()
    test_config_path = Path(yql_query_path).joinpath(TEST_FOLDER_PATH2).resolve()

    if is_ci and not is_yql_new:
        if not test_config_path.is_file():
            return
    if is_yql_new and not test_config_path.is_file():
        raise Exception('There is not tests for this query.sql')

    config_str = open(test_config_path).read()
    test_config = json.loads(config_str)

    # load_samples_to_local_yt(yt.yt_wrapper, config, yql_query_path)
    load_samples_to_local_yt2(yt.yt_wrapper, test_config, test_config_path)
    run_yql(yql_api, yql_query, test_config)
    assert_result(yt.yt_wrapper, test_config, yql_query_path, is_save)


# def load_samples_to_local_yt(local_yt, config, path):
#     # TODO: добавить user attributes
#     for key, value in config['inputs'].items():
#         if isinstance(value, str):
#             input_sample_path = Path(os.path.join(path, f'{TEST_FOLDER_PATH}/{key}.json')).resolve()
#             input_sample_schemfa_path = Path(os.path.join(path, f'{TEST_FOLDER_PATH}/{key}_schema.json')).resolve()

#             upload_table(local_yt, input_sample_path, input_sample_schemfa_path, value)
#         elif isinstance(value, dict):
#             date_start = datetime.datetime.fromisoformat(value['date-start'])
#             date_end = datetime.datetime.fromisoformat(value['date-end'])
#             yt_path = value['path']

#             for i in range(0, date_end.toordinal() - date_start.toordinal() + 1):
#                 current_date = date_start + datetime.timedelta(days=i)
#                 full_yt_path = os.path.join(yt_path, current_date.strftime('%Y-%m-%d'))

#                 input_sample_path = Path(os.path.join(path, f'{TEST_FOLDER_PATH}/{key}_{current_date.strftime("%Y-%m-%d")}.json')).resolve()
#                 input_sample_schemfa_path = Path(os.path.join(path, f'{TEST_FOLDER_PATH}/{key}_{current_date.strftime("%Y-%m-%d")}_schema.json')).resolve()

#                 upload_table(local_yt, input_sample_path, input_sample_schemfa_path, full_yt_path)
#         else:
#             raise Exception(f'Input value {value} has unhandled type')


def load_samples_to_local_yt2(local_yt, test_config, test_config_path):
    for key, value in test_config['inputs'].items():
        input_sample_path = Path(test_config_path).joinpath('..', 'fixtures', f'{value}.json').resolve()
        input_sample_schema_path = Path(test_config_path).joinpath('..', 'fixtures', f'{value}_schema.json').resolve()
        input_sample_user_attributes_path = Path(test_config_path).joinpath('..', 'fixtures', f'{value}_user_attributes.json').resolve()

        upload_table(local_yt, input_sample_path, input_sample_schema_path, input_sample_user_attributes_path, key)


def upload_table(yt_wrapper, data_path, schema_path, user_attributes_path, table_path):
    input_sample = open(data_path).read()

    input_schema_str = open(schema_path).read()
    input_schema = json.loads(input_schema_str)

    input_user_attributes_text = open(user_attributes_path).read()
    input_user_attributes = json.loads(input_user_attributes_text)

    input_data = list(map(lambda row: json.loads(row), filter(lambda row: row, input_sample.split('\n'))))

    if input_schema:
        yt_wrapper.create('table', table_path, recursive=True, ignore_existing=True, attributes={"schema": input_schema})
    else:
        yt_wrapper.create('table', table_path, recursive=True, ignore_existing=True)

    for attr, value in input_user_attributes.items():
        if not attr.startswith('_yql_'):
            continue

        if attr == '_yql_row_spec':
            _yql_row_spec = value
            if 'NativeYtTypeFlags' in _yql_row_spec:
                _yql_row_spec['NativeYtTypeFlags'] = YsonUint64(_yql_row_spec['NativeYtTypeFlags'])
            yt_wrapper.set_attribute(attribute=attr, path=table_path, value=_yql_row_spec)
        else:
            yt_wrapper.set_attribute(attribute=attr, path=table_path, value=value)

    yt_wrapper.write_table(table_path, input_data)

    assert yt_wrapper.exists(table_path)


def run_yql(yql_api, yql_query: str, test_config):
    client = YqlClient(server='localhost', port=yql_api.port, db='plato')

    parameters = []

    for key, value in test_config['parameters'].items():
        parameters.append((ValueBuilder.make_string(key), ValueBuilder.make_string(value)))

    all_parameters = {'$param_dict': ValueBuilder.make_dict(parameters)}
    for key, value in test_config['parameters'].items():
        all_parameters['$' + key] = ValueBuilder.make_string(value)

    request_parameters = ValueBuilder.build_json_map(all_parameters)

    # delete cluster, because here another one (plato)
    hahn_define = re.compile(re.escape('use hahn;'), re.IGNORECASE)
    yql_query = hahn_define.sub('', yql_query)

    # add some code
    pragma_options = 'PRAGMA yt.InferSchema = "1000";\n'
    yql_query = pragma_options + yql_query

    # remove some code
    yql_query = re.sub(r'PRAGMA\s+yt\.PoolTrees\s*=.+;', '', yql_query)  # PRAGMA yt.PoolTrees = "physical";
    yql_query = re.sub(r'PRAGMA\s+yt\.TentativePoolTrees\s*=.+;', '', yql_query)  # PRAGMA yt.TentativePoolTrees = "cloud";

    request = client.query(f'{yql_query}', syntax_version=1)
    request.run(parameters=request_parameters)

    results = request.get_results()
    assert results.is_success, [str(error) for error in results.errors]


def assert_result(yt_wrapper, test_config: dict, path: str, save: bool) -> None:
    for key, value in test_config['outputs'].items():
        current = [*yt_wrapper.read_table(key).rows]
        output_path = Path(path).joinpath(TEST_FOLDER_PATH, f'{value}.json').resolve()
        if save:
            fd = open(output_path, 'w', encoding='utf8')
            fd.write(json.dumps(current, indent=4, ensure_ascii=False))
        else:
            expected = json.loads(open(output_path).read())
            assert current == expected
