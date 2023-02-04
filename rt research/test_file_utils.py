# coding=utf-8
import re
import io
import os
import platform
import pytest

import irt.utils
import irt.utils.file_utils


def test_file_utils():
    assert re.compile(r'^r\d+$').match(irt.utils.__version__) is not None

    assert irt.utils.read_env_or_file('IMPOSSIBLE_ENV_NAME', 'IMPOSSIBLE_PATH_ENV_NAME', path='impossible_path', default_value='no_value') == 'no_value'
    assert irt.utils.read_env_or_file('ENV_NAME', 'IMPOSSIBLE_PATH_ENV_NAME', path='impossible_path', default_value='no_value') == 'env_name'
    assert irt.utils.read_env_or_file('IMPOSSIBLE_ENV_NAME', 'IMPOSSIBLE_PATH_ENV_NAME', path='yql.token', default_value='no_value') == 'yql_token'
    assert irt.utils.read_env_or_file('IMPOSSIBLE_ENV_NAME', 'TEST_DATA_PATH_ENV_NAME', path='impossible_path', default_value='no_value') == 'yql_token'

    objects = [
        {'key1': 'value1'},
        {'key2': 'value2'},
        {'key1': 'value3', 'key2': 'value4'},
        {'key1': None},
        {},
        {'key1': u'Кирилица'}
    ]

    irt.utils.to_yml('test.yml', objects)

    read_value = irt.utils.from_yml('test.yml')
    assert read_value == objects

    with io.open('test.yml', 'r', encoding='utf-8') as f:
        assert f.read() == u'''- key1: value1
- key2: value2
- key1: value3
  key2: value4
- key1: null
- {}
- key1: Кирилица
'''

    irt.utils.to_json('test.json', objects)
    read_value = irt.utils.from_json('test.json')
    assert read_value == objects

    with io.open('test.json', 'r', encoding='utf-8') as f:
        assert f.read() == u'[{"key1": "value1"}, {"key2": "value2"}, {"key1": "value3", "key2": "value4"}, {"key1": null}, {}, {"key1": "Кирилица"}]'

    irt.utils.to_csv('test.csv', ['key1', 'key2'], objects)
    read_value = irt.utils.from_csv('test.csv')
    assert read_value == [
        {'key1': 'value1', 'key2': ''},
        {'key1': '', 'key2': 'value2'},
        {'key1': 'value3', 'key2': 'value4'},
        {'key1': '', 'key2': ''},
        {'key1': '', 'key2': ''},
        {'key1': u'Кирилица', 'key2': ''}
    ]

    with io.open('test.csv', 'r', encoding='utf-8') as f:
        assert f.read() == u'''key1,key2
value1,
,value2
value3,value4
,
,
Кирилица,
'''

    irt.utils.to_tsv('test.tsv', ['key1', 'key2'], objects)
    read_value = irt.utils.from_tsv('test.tsv')
    assert read_value == [
        {'key1': 'value1', 'key2': ''},
        {'key1': '', 'key2': 'value2'},
        {'key1': 'value3', 'key2': 'value4'},
        {'key1': '', 'key2': ''},
        {'key1': '', 'key2': ''},
        {'key1': u'Кирилица', 'key2': ''}
    ]

    with io.open('test.tsv', 'r', encoding='utf-8') as f:
        assert f.read() == u'''key1\tkey2
value1\t
\tvalue2
value3\tvalue4
\t
\t
Кирилица\t
'''

    objects = [
        {'key1': 'value1'},
        {'key2': 'value2'},
        {'key1': 'value3', 'key2': 'value4'},
        {'key1': None},
        {}
    ]

    irt.utils.to_yml('test.yml', objects)

    read_value = irt.utils.from_yml('test.yml')
    assert read_value == objects

    with io.open('test.yml', 'r', encoding='utf-8') as f:
        assert f.read() == '''- key1: value1
- key2: value2
- key1: value3
  key2: value4
- key1: null
- {}
'''

    irt.utils.to_json('test.json', objects)
    read_value = irt.utils.from_json('test.json')
    assert read_value == objects

    with io.open('test.json', 'r', encoding='utf-8') as f:
        assert f.read() == '[{"key1": "value1"}, {"key2": "value2"}, {"key1": "value3", "key2": "value4"}, {"key1": null}, {}]'

    irt.utils.to_csv('test.csv', ['key1', 'key2'], objects)
    read_value = irt.utils.from_csv('test.csv')
    assert read_value == [
        {'key1': 'value1', 'key2': ''},
        {'key1': '', 'key2': 'value2'},
        {'key1': 'value3', 'key2': 'value4'},
        {'key1': '', 'key2': ''},
        {'key1': '', 'key2': ''},
    ]

    with io.open('test.csv', 'r', encoding='utf-8') as f:
        assert f.read() == '''key1,key2
value1,
,value2
value3,value4
,
,
'''

    irt.utils.to_tsv('test.tsv', ['key1', 'key2'], objects)
    read_value = irt.utils.from_tsv('test.tsv')
    assert read_value == [
        {'key1': 'value1', 'key2': ''},
        {'key1': '', 'key2': 'value2'},
        {'key1': 'value3', 'key2': 'value4'},
        {'key1': '', 'key2': ''},
        {'key1': '', 'key2': ''},
    ]

    with io.open('test.tsv', 'r', encoding='utf-8') as f:
        assert f.read() == '''key1\tkey2
value1\t
\tvalue2
value3\tvalue4
\t
\t
'''

    with irt.utils.open_resource_or_file('test.inc') as f:
        assert f.read() == '''PEERDIR(
    rt-research/common/pylib/utils/ut
)

DATA(
    sbr://1009319433
)

IF(OS_LINUX)
    DEPENDS(
        yt/packages/latest
    )
ENDIF()

ENV(ENV_NAME=env_name)

ENV(TEST_DATA_PATH_ENV_NAME=yql.token)
'''

    with irt.utils.open_resource_or_file('test.inc', binary=True) as f:
        assert f.read() == b'''PEERDIR(
    rt-research/common/pylib/utils/ut
)

DATA(
    sbr://1009319433
)

IF(OS_LINUX)
    DEPENDS(
        yt/packages/latest
    )
ENDIF()

ENV(ENV_NAME=env_name)

ENV(TEST_DATA_PATH_ENV_NAME=yql.token)
'''


def test_get_system_config_file_for_nt():
    import ntpath
    prev_path = os.path
    os.path = ntpath
    prev_system = platform.system
    platform.system = lambda: 'Windows'

    with pytest.raises(RuntimeError):
        irt.utils.get_system_config_file('multik', 'config')

    os.environ['ALLUSERSPROFILE'] = 'C:\\Documents And Settings\\All Users'
    assert irt.utils.get_system_config_file('multik', 'config') == 'C:\\Documents And Settings\\All Users\\multik\\config'

    os.environ['PROGRAMDATA'] = 'C:\\ProgramData\\'
    assert irt.utils.get_system_config_file('multik', 'config') == 'C:\\ProgramData\\multik\\config'

    os.environ['APPDATA'] = 'C:\\Documents and Settings\\UserName\\Application Data'
    assert irt.utils.get_user_config_file('multik', 'config') == 'C:\\Documents and Settings\\UserName\\Application Data\\multik\\config'

    platform.system = lambda: 'NoSuchOS'
    with pytest.raises(RuntimeError):
        irt.utils.get_system_config_file('multik', 'config')
    with pytest.raises(RuntimeError):
        irt.utils.get_user_config_file('multik', 'config')

    platform.system = prev_system
    os.path = prev_path


def test_py23():
    assert getattr(irt.utils.file_utils, '_to_bytes_py3')(b'123') == b'123'
    assert getattr(irt.utils.file_utils, '_to_bytes_py3')('123') == b'123'
