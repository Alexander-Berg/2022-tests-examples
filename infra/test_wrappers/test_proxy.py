# coding: utf-8
from infra.awacs.proto import modules_pb2
from awacs.wrappers.main import Proxy
from awtest.wrappers import get_validation_exception


def test_proxy():
    pb = modules_pb2.ProxyModule()

    proxy = Proxy(pb)

    e = get_validation_exception(proxy.validate)
    e.match('host.*is required')

    pb.host = 'test'
    proxy.update_pb(pb)

    e = get_validation_exception(proxy.validate)
    e.match('port.*is required')

    pb.port = -100
    proxy.update_pb(pb)

    e = get_validation_exception(proxy.validate)
    e.match('port.*is not a valid port')

    pb.port = 1024
    pb.backend_timeout = '10s'
    pb.connect_timeout = '1s'
    proxy.validate()

    pb.cached_ip = 'localhost'
    proxy.update_pb(pb)

    e = get_validation_exception(proxy.validate)
    e.match('cached_ip.*is not a valid IP address')

    pb.cached_ip = '127.0.0.1'
    pb.connect_timeout = '100hours'
    proxy.update_pb(pb)

    e = get_validation_exception(proxy.validate)
    e.match('connect_timeout.*is not a valid timedelta string')

    pb.connect_timeout = '100ms'
    pb.backend_timeout = '100hours'
    proxy.update_pb(pb)

    e = get_validation_exception(proxy.validate)
    e.match('backend_timeout.*is not a valid timedelta string')

    pb.backend_timeout = '100ms'
    pb.keepalive_count = -10
    proxy.update_pb(pb)

    e = get_validation_exception(proxy.validate)
    e.match('keepalive_count.*must be non-negative')


def test_proxy_https_settings():
    pb = modules_pb2.ProxyModule(host='test', port=80, backend_timeout='10s', connect_timeout='1s')

    proxy = Proxy(pb)
    proxy.validate()

    pb.https_settings.SetInParent()
    proxy.update_pb(pb)

    e = get_validation_exception(proxy.validate)
    e.match('https_settings -> ca_file: is required')

    pb.https_settings.f_ca_file.type = pb.https_settings.f_ca_file.GET_INT_VAR
    pb.https_settings.f_ca_file.get_int_var_params.var = 'test'
    proxy.update_pb(pb)

    e = get_validation_exception(proxy.validate)
    e.match('https_settings -> verify_depth: is required')

    pb.https_settings.verify_depth = 3
    proxy.update_pb(pb)
    e = get_validation_exception(proxy.validate)

    e.match('https_settings -> ca_file: only the following functions allowed here: "get_str_var", "get_ca_cert_path"')

    pb.https_settings.f_ca_file.type = pb.https_settings.f_ca_file.GET_CA_CERT_PATH
    pb.https_settings.f_ca_file.get_ca_cert_path_params.name = './privet.txt'
    proxy.update_pb(pb)

    proxy.validate()

    pb.https_settings.ClearField('f_ca_file')
    pb.https_settings.ca_file = '/privet.txt'
    proxy.update_pb(pb)

    proxy.validate()

    pb.https_settings.verify_depth = -1
    proxy.update_pb(pb)

    e = get_validation_exception(proxy.validate)
    e.match('https_settings -> verify_depth: must be positive')

    pb.https_settings.verify_depth = 3
    proxy.update_pb(pb)
    proxy.validate()
