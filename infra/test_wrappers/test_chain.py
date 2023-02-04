# coding: utf-8
import mock
import pytest

from infra.awacs.proto import modules_pb2
from awacs.wrappers.errors import ValidationError
from awacs.wrappers.main import Errorlog, Http, Accesslog, Regexp
from awacs.wrappers.base import Holder


def test_walk_chain():
    pb = modules_pb2.Holder()

    m = pb.modules.add()
    m.errorlog.SetInParent()

    m = pb.modules.add()
    m.http.SetInParent()

    m = pb.modules.add()
    m.accesslog.SetInParent()

    holder = Holder(pb)
    assert holder.chain
    assert not holder.module

    chain = holder.chain
    assert len(chain.modules) == 3
    assert isinstance(chain.modules[0].module, Errorlog)
    assert isinstance(chain.modules[1].module, Http)
    assert isinstance(chain.modules[2].module, Accesslog)

    modules = list(chain.walk_chain())
    assert len(modules) == 3
    assert isinstance(modules[0], Errorlog)
    assert isinstance(modules[1], Http)
    assert isinstance(modules[2], Accesslog)


def test_swat_3844():
    pb = modules_pb2.Holder()

    m = pb.modules.add()
    m.errorlog.SetInParent()

    m = pb.modules.add()
    m.regexp.SetInParent()

    m = pb.modules.add()
    m.accesslog.SetInParent()

    holder = Holder(pb)
    assert holder.chain
    assert not holder.module

    chain = holder.chain
    assert len(chain.modules) == 3
    assert isinstance(chain.modules[0].module, Errorlog)
    assert isinstance(chain.modules[1].module, Regexp)
    assert isinstance(chain.modules[2].module, Accesslog)

    with pytest.raises(ValidationError) as e:
        with mock.patch.object(Errorlog, 'validate'), \
             mock.patch.object(Regexp, 'validate'), \
             mock.patch.object(Accesslog, 'validate'):
            chain.validate()
    assert e.match(r'modules\[1\]: non-chainable module Regexp can not be chained')

    pb.modules.pop()
    holder.update_pb(pb)
    chain = holder.chain
    assert len(chain.modules) == 2
    with mock.patch.object(Errorlog, 'validate'), \
         mock.patch.object(Regexp, 'validate'):
        chain.validate()
