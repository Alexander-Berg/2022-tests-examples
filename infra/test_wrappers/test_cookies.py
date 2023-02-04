# coding: utf-8
import pytest

from infra.awacs.proto import modules_pb2
from awacs.wrappers.main import CookiesModule, OrderedDict
from awacs.wrappers.errors import ValidationError


def test_cookies():
    pb = modules_pb2.CookiesModule()

    cookies = CookiesModule(pb)

    with pytest.raises(ValidationError) as e:
        cookies.validate(chained_modules=True)
    e.match('at least one of the "create", "create_weak", "delete" must be specified')

    pb.delete = '^('
    cookies.update_pb(pb)

    with pytest.raises(ValidationError) as e:
        cookies.validate(chained_modules=True)
    e.match('is not a valid regexp')

    pb.delete = '.*value.*'
    entry_pb = pb.create.add(key='\\')
    entry_pb.value = '1'
    cookies.update_pb(pb)

    with pytest.raises(ValidationError) as e:
        cookies.validate(chained_modules=True)
    e.match(r'create\[0\].*key: must match')

    entry_pb.key = 'test1'
    entry_pb.value = ''
    cookies.update_pb(pb)

    with pytest.raises(ValidationError) as e:
        cookies.validate(chained_modules=True)
    e.match(r'create\[0\].*value: is required')

    entry_pb.value = '1'
    cookies.update_pb(pb)

    pb.create.add(key='test2', value='2')
    pb.create.add(key='test3', value='3')
    pb.create.add(key='test4', value='4')

    cookies.update_pb(pb)
    cookies.validate(chained_modules=True)

    cookies.update_pb(pb)
    config = cookies.to_config()
    assert isinstance(config.table, OrderedDict)
    assert isinstance(config.table['create'].table, OrderedDict)
    assert list(config.table['create'].table.keys()) == ['test1', 'test2', 'test3', 'test4']
