from __future__ import unicode_literals

import pytest
from yp_proto.yp.client.hq.proto import types_pb2

from instancectl.lib import specutil
from instancectl.cmd import log


def test_expand_container_spec_variables():
    log.setup_logging_to_stderr()
    s = types_pb2.Container()
    s.name = 'test-container'
    s.command.append('{GOOD_NAME}')
    e = {'GOOD_NAME': 'good_value'}
    specutil.expand_container_spec_variables(s, e)
    assert s.command[0] == 'good_value'
    s.command.append('{BAD_NAME}')
    with pytest.raises(KeyError):
        specutil.expand_container_spec_variables(s, e)
