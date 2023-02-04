# -*- coding: utf-8 -*-

import json
import zipfile

import httpretty
import mock
import pytest

from csv import DictReader
from StringIO import StringIO

from balance.actions.nirvana.operations import execute_sql_select
from tests import object_builder as ob


DUMMY_QUERY = \
'''
select
  'test string' "ascii",
  'тестовая строка' "юникод",
  date'2020-05-01' "dt",
  100 value_num,
  100.0 value_float,
  null none
from dual
'''


def get_n_dummy_queries(n):
    return '\nunion all\n'.join([DUMMY_QUERY] * n)


def nirvana_block(session, **kwargs):
    return ob.NirvanaBlockBuilder(operation='execute_sql_select', request=
    {
        'data': {
            'inputs': {
                'input': {
                    'items': [
                        {
                            'downloadURL': 'http://yandex.ru'
                        }
                    ]
                }
            }
        }
    }).build(session).obj


def patched_process(nirvana_block, tmpdir):
    def upload(output, data):
        with open(str(tmpdir.join(output)), 'wb') as f:
            f.write(data)

        return

    with mock.patch.object(nirvana_block, 'upload', upload):
        execute_sql_select.process(nirvana_block)

    return


@pytest.mark.parametrize('body, error', [
    ([], 'malformed query tasks'),

    ([
         {'filename': 'тест1.csv', 'dbname': 'somedb',},
         {'filename': 'тест1.csv', 'dbname': 'somedb', 'query': 'select 1 from dual'},
     ], 'malformed query tasks'),

    ([
         {'filename': 'тест1.csv', 'dbname': 'somedb', 'query': 'select 1 from dual', 'extra': 'payload'},
         {'filename': 'тест1.csv', 'dbname': 'somedb', 'query': 'select 1 from dual'},
     ], 'malformed query tasks'),

    ([
         {'filename': 'тест1.csv', 'dbname': 'somedb', 'query': 'select 1 from dual',
          'raw_output': True, 'extra': 'payload'},
         {'filename': 'тест1.csv', 'dbname': 'somedb', 'query': 'select 1 from dual'},
     ], 'malformed query tasks'),

    ([
         {'filename': 'тест1.csv', 'dbname': 'somedb', 'query': 'select 1 from dual'},
     ], 'allowed backends'),

    ([
         {'filename': 'тест1.csv', 'dbname': 'balance_ro', 'query': 'select 1 from dual'},
         {'filename': 'тест2.csv', 'dbname': 'somedb', 'query': 'select 1 from dual'},
     ], 'allowed backends'),

    ([
         {'filename': 'тест1.csv', 'dbname': 'balance_ro', 'query': ' \n  \n \t'},
     ], 'empty query'),

    ([
         {'filename': 'тест1.csv', 'dbname': 'balance_ro', 'query': ' \n  \n \t'},
         {'filename': 'тест2.csv', 'dbname': 'balance_ro', 'query': 'select 1 from dual'},
     ], 'empty query'),

    ([
         {'filename': '  \n  \t\n', 'dbname': 'balance_ro', 'query': 'select 1 from dual'},
     ], 'empty filename'),

    ([
         {'filename': 'тест1.csv', 'dbname': 'balance_ro', 'query': 'select sysdate from dual'},
         {'filename': '  \n  \t\n', 'dbname': 'balance_ro', 'query': 'select 1 from dual'},
     ], 'empty filename'),

    ([
         {'filename': 'тест1.csv', 'dbname': 'balance_ro', 'query': 'select 1 from dual'},
         {'filename': 'тест1.csv', 'dbname': 'balance_ro', 'query': 'select sysdate from dual'},
     ], 'duplicate filename'),

    ([
         {'filename': 'тест1.csv', 'dbname': 'balance_ro', 'query': 'select 1 from dual;'},
     ], 'DatabaseError'),

    ([
         {'filename': 'тест1.csv', 'dbname': 'balance_ro', 'query': 'select 1 from dual'},
         {'filename': 'тест2.csv', 'dbname': 'balance_ro', 'query': 'select sysdate from dual;'},
     ], 'DatabaseError')
])
@pytest.mark.usefixtures('httpretty_enabled_fixture')
def test_execute_sql_select_process_invalid(tmpdir, session, body, error):
    dumped_response_file = tmpdir.join('output')
    nb = nirvana_block(session)

    body = json.loads(json.dumps(body))
    for input_ in nb.inputs.values():
        for item in input_['items']:
            httpretty.register_uri(httpretty.GET, item['downloadURL'], json.dumps(body))

    with pytest.raises(Exception) as exc:
        patched_process(nb, dumped_response_file)

    assert error in str(exc)

    return


@pytest.mark.parametrize('body', [
    ([
         {'filename': 'тест1.csv', 'dbname': 'balance_ro', 'query': get_n_dummy_queries(1)},
    ]),

    ([
         {'filename': 'тест1.csv', 'dbname': 'balance_ro', 'query': get_n_dummy_queries(1)},
         {'filename': 'тест2.csv', 'dbname': 'balance_ro', 'query': get_n_dummy_queries(2), 'raw_output': True},
         {'filename': 'тест3.csv', 'dbname': 'balance_ro', 'query': get_n_dummy_queries(3), 'raw_output': True},
    ]),

    ([
         {'filename': 'тест1.csv', 'dbname': 'balance_ro', 'query': get_n_dummy_queries(1)},
         {'filename': 'test_raw', 'dbname': 'balance_ro', 'query': get_n_dummy_queries(2), 'raw_output': True},
         {'filename': 'no_output', 'dbname': 'balance_ro', 'query': 'declare a number; '
                                                                    'begin select 1 into a from dual; end;'}
    ]),

    ([
         {'filename': 'тест1.csv', 'dbname': 'balance_ro', 'query': get_n_dummy_queries(1)},
         {'filename': 'тест_2.csv', 'dbname': 'meta_ro', 'query': get_n_dummy_queries(2)},
         {'filename': 'тест 3.csv', 'dbname': 'balance_ro', 'query': get_n_dummy_queries(3)},
    ])
])
@pytest.mark.usefixtures('httpretty_enabled_fixture')
def test_execute_sql_select_process_valid(tmpdir, app, session, body):
    nb = nirvana_block(session)

    body = json.loads(json.dumps(body))
    for input_ in nb.inputs.values():
        for item in input_['items']:
            httpretty.register_uri(httpretty.GET, item['downloadURL'], json.dumps(body))

    patched_process(nb, tmpdir)

    body = [row for row in body if row['filename'] != 'no_output']

    with zipfile.ZipFile(str(tmpdir.join('output')), 'r', zipfile.ZIP_DEFLATED) as zf:
        filenames = zf.namelist()
        assert len(filenames) == len(body)
        filenames = set(filename for filename in filenames)
        assert filenames == set(row['filename'] for row in body)
        for row in body:
            with zf.open(row['filename'], 'r') as f:
                csv = f.read()
                res = session.execute(row['query'])
                expected_csv = execute_sql_select.data_to_text_file(res.keys(), res, sep=',', escape=True)
                assert csv == expected_csv

    raw_body = filter(lambda x: x.get('raw_output'), body)
    if not raw_body:
        return

    with open(str(tmpdir.join('raw_output'))) as f:
        output = json.load(f)
        for output_row in output:
            output_row['result'] = [
                {
                    key.encode('utf-8'): val.encode('utf-8') for key, val in row.iteritems()
                } for row in output_row['result']
            ]
        assert len(output) == len(raw_body)
        assert set(row['filename'] for row in output) == set(row['filename'] for row in raw_body)
        for output_row, body_row in zip(output, raw_body):
            res = session.execute(body_row['query'])
            res_csv = execute_sql_select.data_to_text_file(res.keys(), res, sep=',', escape=True)
            expected_json = list(DictReader(StringIO(res_csv)))
            assert output_row['result'] == expected_json

    return
