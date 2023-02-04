# TODO: Add this tests to integration (check this tests actually work)
import inspect
import os
import shutil
import tempfile
import json
import six
import pytest

from datacloud import key_manager
from datacloud.key_manager.memory import MemoryKeyStorage
from datacloud.key_manager.files import FilesKeyStorage
from datacloud.key_manager.generic import KeyStorage, UnknownKeyName
if six.PY2:
    from collections import Mapping
else:
    from collections.abc import Mapping


def pytest_generate_tests(metafunc):
    if 'storage' in metafunc.fixturenames:
        all_storages = [
            c
            for _name, c in inspect.getmembers(key_manager, inspect.isclass)
            if issubclass(c, KeyStorage) and c is not KeyStorage
        ]
        metafunc.parametrize("storage", all_storages, indirect=True)


@pytest.fixture(scope="function")
def storage(keys, request):
    if request.param is MemoryKeyStorage:
        yield MemoryKeyStorage(keys=keys)
    elif request.param is FilesKeyStorage:
        files_root = tempfile.mkdtemp(prefix='tmp_key_manager_test')
        files_suffix = '.testing'
        for key_name, values in six.iteritems(keys):
            with open(os.path.join(files_root, key_name + files_suffix), 'w') as file:
                json.dump(values, file, indent=4)
        yield FilesKeyStorage(keys_path=files_root, files_suffix=files_suffix)
        shutil.rmtree(files_root)
    else:
        raise ValueError("no test generation for %s" % request.param)


@pytest.fixture(scope="function")
def keys():
    return {
        'ns_a': {
            'foo': {
                'bar': 'baz',
            },
            'a': 'b',
        },
        'ns_b': {},
    }


def test_is_like_dict(storage):
    assert sorted(storage.keys()) == ['ns_a', 'ns_b']
    assert storage['ns_a']['a'] == 'b'
    assert sorted(six.iteritems(storage['ns_a']['foo'])) == [('bar', 'baz')]
    default = object()
    assert storage['ns_b'].get('a', default) is default
    assert storage['ns_b'].get('a') is None
    assert storage.get('a', default) is default
    assert storage.get('a') is None
    with pytest.raises(KeyError):
        storage['a']


def test_raises_specific_key_error(storage):
    with pytest.raises(UnknownKeyName):
        storage['a']
    try:
        storage['a']
    except UnknownKeyName as e:
        assert e.key_name == 'a'


def test_all_childs_is_key_storage_or_non_dict(storage):
    queue = [storage]
    while queue:
        storage = queue.pop()
        for val in storage.values():
            if isinstance(val, KeyStorage):
                queue.append(val)
            else:
                assert not isinstance(val, Mapping)


def test_is_read_only(storage):
    with pytest.raises(TypeError):
        storage['ns_c'] = 1
    with pytest.raises(TypeError):
        storage['ns_a']['foo'] = 1
    with pytest.raises(TypeError):
        storage['ns_a']['foo_1'] = 1
    with pytest.raises(TypeError):
        storage['ns_a']['foo']['bar'] = 1
    with pytest.raises(TypeError):
        storage['ns_a']['foo']['bar_1'] = 1


def test_find_key_returns_key(storage):
    default = object()
    assert storage.find_key('ns_a', 'foo', 'bar') == 'baz'
    assert storage.find_key('ns_a', 'foo', 'bar2', default=default) is default
    with pytest.raises(KeyError):
        assert storage.find_key('ns_a', 'foo', 'bar2')
    assert storage.find_key('ns_a', 'foo')['bar'] == 'baz'
