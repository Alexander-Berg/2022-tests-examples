from infra.ya_salt.proto import ya_salt_pb2
from infra.ya_salt.proto import config_pb2

from infra.ya_salt.hostmanager import shim


def test_init_orly():
    cfg_pb = config_pb2.Config()
    cfg_pb.no_orly = True
    spec = ya_salt_pb2.HostmanSpec()
    # Test no orly in config
    assert shim.init_orly(spec, cfg_pb) is None
    # Test properly constructed
    cfg_pb.no_orly = False
    cfg_pb.orly_url = 'http://test-orly.yandex.net'
    spec.hostname = 'test.init-orly.net'
    spec.env_type = 'prestable'
    spec.walle_project = 'rtc-in-test'
    spec.location = 'arcadia'
    orly = shim.init_orly(spec, cfg_pb)
    assert orly is not None
    assert orly.hostname == spec.hostname
    assert orly.orly.url == cfg_pb.orly_url + '/rest/'
    assert orly.labels == {'ctype': 'prestable', 'geo': 'arcadia', 'prj': 'rtc-in-test'}
