import json
import pytest

import six

from google.protobuf import json_format

from tasklet import runtime
from tasklet.api import tasklet_pb2
from tasklet.runtime import utils
from tasklet.runtime.tests import common

from tasklet.tests.proto import types_pb2 as test_types_pb2


# TODO: tests for other cons* from runtime.__init__.py
class TestBaseContextInjection(object):

    def add_dummy_entry(self, ctx, name, value):
        entry = tasklet_pb2.ContextEntry()
        entry.name = name

        id = test_types_pb2.Entry()
        id.value = value
        entry.any.Pack(id)

        ctx.entries.add().CopyFrom(entry)

    def test_service_ref(self):

        init_ctx = test_types_pb2.Context1()
        init_ctx.a.value = 'a_init'
        init_ctx.b.value = 'b_init'
        init_ctx.ref.address = 'ref_address_init'

        new_ctx = tasklet_pb2.Context()
        self.add_dummy_entry(new_ctx, 'a', 'a_new')
        self.add_dummy_entry(new_ctx, 'c', 'c_new')

        entry = tasklet_pb2.ContextEntry()
        entry.name = 'ref'

        ref = tasklet_pb2.ServiceRef()
        ref.address = 'localhost:50051'
        ref.client = 'tasklet.runtime.tests.common:DummyClient'

        entry.any.Pack(ref)
        new_ctx.entries.add().CopyFrom(entry)

        orig_ctx = tasklet_pb2.Context()
        self.add_dummy_entry(orig_ctx, 'a', 'a_orig')
        self.add_dummy_entry(orig_ctx, 'b', 'b_orig')

        res = runtime.cons(init_ctx, new_ctx, orig_ctx)

        assert json.loads(json_format.MessageToJson(res.a.any))['value'] == 'a_new'
        assert type(res.ref) == common.DummyClient
        assert not hasattr(res, 'b')
        assert not hasattr(res, 'c')
        assert res.ctx_msg == orig_ctx

    def test_py_adapter(self):

        init_ctx = test_types_pb2.Context2()
        init_ctx.a.value = 'a_init'
        init_ctx.b.value = 'b_init'

        new_ctx = tasklet_pb2.Context()
        self.add_dummy_entry(new_ctx, 'a', 'a_new')

        entry = tasklet_pb2.ContextEntry()
        entry.name = 'b'

        b = test_types_pb2.PyEntry()
        b.value = 'b_new'

        entry.any.Pack(b)
        new_ctx.entries.add().CopyFrom(entry)

        res = runtime.cons(init_ctx, new_ctx, tasklet_pb2.Context())

        assert json.loads(json_format.MessageToJson(res.a.any))['value'] == 'a_new'
        assert type(res.b) == common.DummyPyAdapter
        assert res.b.get_value() == 'b_new'


@pytest.mark.parametrize(
    "import_path,sym",
    [
        ("__builtin__:int" if six.PY2 else "builtins:int", int),
        ("tasklet.runtime.tests.common:DummyClient", common.DummyClient),
    ]
)
def test_mangle(import_path, sym):
    assert utils.import_symbol(import_path) is sym
