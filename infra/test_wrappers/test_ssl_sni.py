# coding: utf-8
import mock
import pytest

import awtest
from infra.awacs.proto import modules_pb2
from awacs.wrappers.base import ValidationCtx
from awacs.wrappers.errors import ValidationError
from awacs.wrappers.main import SslSni, SslSniContext, SecondaryCert


def test_ssl_sni():
    pb = modules_pb2.SslSniModule()

    c_entry_pb = pb.contexts.add()
    c_entry_pb.key = 'default'
    c_pb = c_entry_pb.value

    c_pb.cert = 'cert'
    c_pb.priv = 'priv'
    k_pb = c_pb.ticket_keys.add()
    k_pb.keyfile = './first'

    ssl_sni = SslSni(pb)
    assert set(ssl_sni.contexts.keys()) == {'default'}

    pb.max_send_fragment = 100000
    with mock.patch.object(ssl_sni, 'require_nested'):
        with pytest.raises(ValidationError) as e:
            ssl_sni.validate()
    e.match('max_send_fragment: must be less or equal to 16384')

    pb.max_send_fragment = 100
    with mock.patch.object(ssl_sni, 'require_nested'):
        with pytest.raises(ValidationError) as e:
            ssl_sni.validate()
    e.match('max_send_fragment: must be greater or equal to 512')

    pb.max_send_fragment = 1024

    with mock.patch.object(ssl_sni, 'require_nested'):
        ssl_sni.validate()
    ssl_sni_config = ssl_sni.to_config()
    assert set(ssl_sni_config.table.keys()) == {'max_send_fragment', 'contexts', 'events', 'force_ssl'}
    assert set(ssl_sni_config.table['contexts'].table.keys()) == {'default'}

    c_entry_pb = pb.contexts.add()
    c_entry_pb.key = 'x'
    c_pb = c_entry_pb.value
    c_pb.servername.servername_regexp = '.*'
    c_pb.cert = 'cert'
    c_pb.priv = 'priv'

    k_pb = c_pb.ticket_keys.add()
    k_pb.keyfile = './second'

    pb.validate_cert_date = True
    pb.ja3_enabled = True

    ssl_sni.update_pb(pb)

    assert len(ssl_sni.contexts) == 2
    assert len(ssl_sni.contexts['x'].ticket_keys) == 1
    assert len(ssl_sni.contexts['default'].ticket_keys) == 1

    with mock.patch.object(ssl_sni, 'require_nested'):
        ssl_sni.validate()

    ssl_sni_config = ssl_sni.to_config()
    assert set(ssl_sni_config.table.keys()) == {'max_send_fragment', 'contexts', 'events', 'force_ssl',
                                                'validate_cert_date', 'ja3_enabled'}
    contexts = ssl_sni_config.table['contexts']
    assert 'default' in contexts.table
    assert 'x' in contexts.table

    assert contexts.table['x'].table['priority'] == 1
    assert contexts.table['default'].table['priority'] == 2


def test_ssl_sni_context():
    pb = modules_pb2.SslSniContext()

    ssl_sni_context = SslSniContext(pb)

    with pytest.raises(ValidationError) as e:
        ssl_sni_context.validate()
    e.match('cert: is required')

    call_pb = pb.f_cert
    call_pb.type = modules_pb2.Call.GET_PUBLIC_CERT_PATH
    params_pb = call_pb.get_public_cert_path_params
    params_pb.name = 'cert'

    pb.priv = 'priv'
    ssl_sni_context.update_pb(pb)

    with pytest.raises(ValidationError) as e:
        ssl_sni_context.validate()
    e.match('servername: is required')

    pb.servername.SetInParent()
    ssl_sni_context.update_pb(pb)

    with pytest.raises(ValidationError) as e:
        ssl_sni_context.validate()
    e.match('servername -> servername_regexp: is required')

    pb.servername.servername_regexp = '.*'
    ssl_sni_context.update_pb(pb)

    pb.ticket_keys.add()
    ssl_sni_context.update_pb(pb)

    with pytest.raises(ValidationError) as e:
        ssl_sni_context.validate()
    e.match(r'ticket_keys\[0\] -> keyfile: is required')

    pb.ticket_keys[0].keyfile = './test'
    ssl_sni_context.update_pb(pb)

    ssl_sni_context.validate()

    config = ssl_sni_context.to_config(priority=10)
    assert set(config.table.keys()) == {'cert', 'priv', 'priority',
                                        'timeout', 'ticket_keys_list', 'servername', 'ciphers'}
    ticket_keys_config = config.table['ticket_keys_list']
    assert config.table['priority'] == 10

    assert ticket_keys_config.array[0].table == {'keyfile': './test', 'priority': 1}

    servername_config = config.table['servername']
    assert servername_config.table == {
        'case_insensitive': False,
        'servername_regexp': '.*',
        'surround': False
    }

    pb.client.verify_peer = False
    pb.client.verify_once = True
    pb.client.fail_if_no_peer_cert = True
    pb.client.verify_depth = 1
    ssl_sni_context.update_pb(pb)

    with pytest.raises(ValidationError) as e:
        ssl_sni_context.validate()
    e.match('client: enabling "fail_if_no_peer_cert" and/or "verify_once" options with disabled '
            '"verify_peer" option is prohibited')

    pb.client.verify_peer = True
    ssl_sni_context.update_pb(pb)

    with pytest.raises(ValidationError) as e:
        ssl_sni_context.validate()
    e.match('client: using "client" opt without "ca" opt is prohibited')

    pb.ca = 'ca'
    ssl_sni_context.update_pb(pb)
    ssl_sni_context.validate()

    ctx = ValidationCtx(config_type=ValidationCtx.CONFIG_TYPE_UPSTREAM)
    pb = modules_pb2.SslSniContext()
    pb.c_cert.id = 'test'
    pb.servername.servername_regexp = '.*'
    ssl_sni_context = SslSniContext(pb)
    ssl_sni_context.validate(ctx)

    pb = modules_pb2.SslSniContext()
    pb.cert = 'test'
    pb.servername.servername_regexp = '.*'
    ssl_sni_context = SslSniContext(pb)
    with pytest.raises(ValidationError) as e:
        ssl_sni_context.validate(ctx)
    e.match('priv: is required')

    pb = modules_pb2.SslSniContext()
    pb.c_cert.id = '!test'
    pb.priv = 'priv'
    pb.servername.servername_regexp = '.*'
    ssl_sni_context = SslSniContext(pb)
    with pytest.raises(ValidationError) as e:
        ssl_sni_context.validate(ctx)
    e.match('priv: using "priv" opt while !c-value is used in "cert" opt is prohibited')


def test_secondary_cert():
    pb = modules_pb2.SecondaryCert()
    secondary_cert = SecondaryCert(pb)
    ctx = ValidationCtx(config_type=ValidationCtx.CONFIG_TYPE_UPSTREAM)

    with pytest.raises(ValidationError) as e:
        secondary_cert.validate(ctx)
    e.match('cert: is required')

    call_pb = pb.f_cert
    call_pb.type = modules_pb2.Call.GET_PUBLIC_CERT_PATH
    params_pb = call_pb.get_public_cert_path_params
    params_pb.name = 'cert'
    secondary_cert.update_pb(pb)
    with pytest.raises(ValidationError) as e:
        secondary_cert.validate(ctx)
    e.match('priv: is required')

    pb.priv = 'priv'
    secondary_cert.update_pb(pb)
    secondary_cert.validate(ctx)

    config = secondary_cert.to_config()
    assert set(config.table.keys()) == {'cert', 'priv', }

    pb = modules_pb2.SecondaryCert()
    pb.c_cert.id = 'cert'
    secondary_cert = SecondaryCert(pb)
    secondary_cert.validate(ctx)

    pb.priv = 'priv'
    secondary_cert.update_pb(pb)
    with pytest.raises(ValidationError) as e:
        secondary_cert.validate(ctx)
    e.match('priv: using "priv" opt while !c-value is used in "cert" opt is prohibited')


def test_ssl_protocols():
    pb = modules_pb2.SslSniContext()
    call_pb = pb.f_cert
    call_pb.type = modules_pb2.Call.GET_PUBLIC_CERT_PATH
    params_pb = call_pb.get_public_cert_path_params
    params_pb.name = u'cert'
    pb.priv = u'priv'
    pb.servername.servername_regexp = u'.*'
    pb.ticket_keys.add(keyfile=u'./test')

    pb.ssl_protocols.extend([u'wrong'])
    ssl_sni_context = SslSniContext(pb)

    with awtest.raises(ValidationError, text=u'ssl_protocols: unsupported protocol "wrong"'):
        ssl_sni_context.validate()

    del pb.ssl_protocols[:]
    pb.ssl_protocols.extend([u'sslv2', u'tlsv1.3', u'tlsv1', u'sslv3'])
    ssl_sni_context.update_pb(pb)

    ssl_sni_context.validate()

    pb.disable_tlsv1_3.value = True
    ssl_sni_context.update_pb(pb)
    with awtest.raises(ValidationError,
                       text=u'ssl_protocols: protocol "tlsv1.3" is disabled by config option "disable_tlsv1_3"'):
        ssl_sni_context.validate()

    pb.disable_tlsv1_3.value = False
    pb.disable_sslv3 = True
    ssl_sni_context.update_pb(pb)
    with awtest.raises(ValidationError,
                       text=u'ssl_protocols: protocol "sslv3" is disabled by config option "disable_sslv3"'):
        ssl_sni_context.validate()

    pb.disable_sslv3 = False
    ssl_sni_context.update_pb(pb)
    ssl_sni_context.validate()
