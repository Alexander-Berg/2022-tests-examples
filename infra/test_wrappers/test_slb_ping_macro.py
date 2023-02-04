# coding: utf-8
import pytest

from infra.awacs.proto import modules_pb2
from awacs.wrappers.main import SlbPingMacro
from awacs.wrappers.errors import ValidationError


def test_slb_ping_macro():
    pb = modules_pb2.SlbPingMacro()
    m = SlbPingMacro(pb)

    with pytest.raises(ValidationError) as e:
        m.validate()
    e.match(u'at least one of the "active_check_reply", "errordoc", "generated_proxy_backends", '
            u'"use_shared_backends" must be specified')

    pb.errordoc = True
    m.update_pb(pb)
    m.validate()

    pb.switch_off_status_code = 600
    m.update_pb(pb)
    with pytest.raises(ValidationError) as e:
        m.validate()
    e.match(u'switch_off_status_code: unknown status code or family:')

    pb.switch_off_status_code = 503
    m.update_pb(pb)

    pb.use_shared_backends = True
    m.update_pb(pb)
    with pytest.raises(ValidationError) as e:
        m.validate()
    e.match(u'at most one of the "active_check_reply", "errordoc", "generated_proxy_backends", '
            u'"use_shared_backends" must be specified')
