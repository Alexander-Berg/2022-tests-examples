# conding: utf-8
import mock
import pytest

import awtest
from awacs.wrappers.base import Holder, ANY_MODULE
from awacs.wrappers.errors import ValidationError
from awacs.wrappers.main import ExtendedHttpMacro, ExtendedHttpMacroSslSniContext, Report
from infra.awacs.proto import modules_pb2


AM = [ANY_MODULE]


@pytest.mark.parametrize('field,value', (
    ('http2_alpn_freq', 0.5),
    ('http2_allow_without_ssl', True),
    ('http2_allow_sending_trailers', True),
))
def test_extended_http_macro_http2_validation(field, value):
    pb = modules_pb2.ExtendedHttpMacro()
    pb.port = 8080
    pb.maxlen = 100
    pb.maxreq = 100
    pb.nested.errordocument.status = 200

    getattr(pb, field).value = value
    w = ExtendedHttpMacro(pb)
    with awtest.raises(ValidationError, text=u'{}: is not allowed without enable_http2'.format(field)):
        w.validate(preceding_modules=AM)

    pb.enable_http2 = True
    if field == 'http2_alpn_freq':
        with awtest.raises(ValidationError, text=u'{}: is not allowed without enable_ssl'.format(field)):
            w.validate(preceding_modules=AM)
        pb.enable_ssl = True
        pb.ssl_sni_contexts.add(key='xxx').value.servername_regexp = '.*'

    w.validate(preceding_modules=AM)


def test_extended_http_macro():
    pb = modules_pb2.Holder()

    m_pb = pb.modules.add()
    m_pb.accesslog.log = 'test'

    m_pb = pb.modules.add()
    m_pb.extended_http_macro.port = 8080
    m_pb.extended_http_macro.maxlen = 100
    m_pb.extended_http_macro.maxreq = 100
    m_pb.extended_http_macro.report_uuid = 'http'
    m_pb.extended_http_macro.report_refers = 'http2'

    holder = Holder(pb)
    m_pb.extended_http_macro.ssl_sni_max_send_fragment = 100000
    with pytest.raises(ValidationError) as e:
        holder.validate()
    e.match('max_send_fragment: must be less or equal to 16384')

    m_pb.extended_http_macro.ssl_sni_max_send_fragment = 100
    with pytest.raises(ValidationError) as e:
        holder.validate()
    e.match('max_send_fragment: must be greater or equal to 512')

    m_pb.extended_http_macro.ssl_sni_max_send_fragment = 1024

    holder = Holder(pb)
    with mock.patch.object(holder.chain.modules[1].module, 'validate'):
        holder.validate()

    name, c = holder.chain.to_config()

    assert name == 'accesslog'
    assert set(c.table.keys()) == {'log', 'errorlog'}
    assert not c.array

    assert set(c.table['errorlog'].table.keys()) == {'log_level', 'log', 'http'}
    assert not c.table['errorlog'].array

    http_table = c.table['errorlog'].table['http'].table
    allowed_http_keys = {
        'accesslog',
        'events',
        'keepalive',
        'maxlen',
        'maxreq',
        'no_keepalive_file'
    }
    assert set(http_table.keys()) == allowed_http_keys

    accesslog_table = http_table['accesslog'].table
    assert set(accesslog_table.keys()) == {'log', 'report'}

    report_table = accesslog_table['report'].table
    allowed_report_keys = {
        'disable_robotness',
        'disable_sslness',
        'events',
        'just_storage',
        'ranges',
        'refers',
        'uuid'
    }
    assert set(report_table.keys()) == allowed_report_keys
    assert 'http2' in report_table['refers'].split(',')
    assert 'service_total' in report_table['refers'].split(',')

    holder_pb = modules_pb2.Holder()
    holder_pb.modules.add(extended_http_macro=m_pb.extended_http_macro)
    holder = Holder(holder_pb)
    assert holder.chain
    assert len(holder.chain.modules) == 1


def find_report_module(holders):
    report = None
    for holder in holders:
        if holder.module_name == 'report':
            report = holder.module
            break
    return report


def find_http_module(holders):
    report = None
    for holder in holders:
        if holder.module_name == 'http':
            report = holder.module
            break
    return report


def test_extended_http_macro_expand():
    pb = modules_pb2.ExtendedHttpMacro()
    pb.disable_error_log = True
    m = ExtendedHttpMacro(pb)

    with pytest.raises(ValidationError) as e:
        m.expand()
    assert e.match('must be preceded by ipdispatch module if port is not specified')

    pb.port = 80
    m.update_pb(pb)

    holders = list(map(Holder, m.expand()))
    report = find_report_module(holders)
    assert report
    assert report.pb.uuid == ExtendedHttpMacro.DEFAULT_REPORT_UUID
    assert not report.pb.refers
    assert report.pb.ranges == Report.DEFAULT_RANGES_ALIAS

    pb.report_uuid = 'internal'
    m.update_pb(pb)

    holders = list(map(Holder, m.expand()))
    report = find_report_module(holders)
    assert report
    assert report.pb.uuid == 'internal'
    assert report.pb.refers == ExtendedHttpMacro.DEFAULT_REPORT_REFERS
    assert report.pb.ranges == Report.DEFAULT_RANGES_ALIAS

    pb.report_refers = 'http'

    holders = list(map(Holder, m.expand()))
    report = find_report_module(holders)
    assert report
    assert report.pb.uuid == 'internal'
    assert report.pb.refers == 'http,{}'.format(ExtendedHttpMacro.DEFAULT_REPORT_REFERS)
    assert report.pb.ranges == Report.DEFAULT_RANGES_ALIAS
    assert not report.pb.outgoing_codes

    pb.keepalive.value = True
    pb.keepalive_timeout = '100s'
    pb.keepalive_requests.value = 100
    pb.keepalive_drop_probability.value = 0.5
    m.update_pb(pb)

    holders = list(map(Holder, m.expand()))
    http = find_http_module(holders)
    assert http.pb.keepalive.value is True
    assert http.pb.keepalive_requests.value == 100
    assert http.pb.keepalive_drop_probability.value == 0.5

    pb.report_outgoing_codes.extend(['403', '404'])
    m.update_pb(pb)

    holders = list(map(Holder, m.expand()))
    report = find_report_module(holders)
    assert report.pb.outgoing_codes == ['403', '404']


def test_extended_http_macro_ssl_sni_context_validate():
    pb = modules_pb2.ExtendedHttpMacroSslSniContext()
    m = ExtendedHttpMacroSslSniContext(pb)
    with awtest.raises(ValidationError, text=u'servername_regexp: is required'):
        m.validate()

    pb.servername_regexp = u'default'
    pb.c_secondary_cert.id = u'!test'
    pb.secondary_cert_postfix = u'test'
    m.update_pb(pb)
    with awtest.raises(ValidationError, text=u'secondary_cert: in ExtendedHttpMacro cannot be used '
                                             u'together with secondary_cert_postfix'):
        m.validate()

    pb = modules_pb2.ExtendedHttpMacroSslSniContext()
    m = ExtendedHttpMacroSslSniContext(pb)
    pb.servername_regexp = u'default'
    pb.cert = u'test'
    with awtest.raises(ValidationError, text=u'cert: in ExtendedHttpMacro only supports !c-values'):
        m.validate()

    pb = modules_pb2.ExtendedHttpMacroSslSniContext()
    m = ExtendedHttpMacroSslSniContext(pb)
    pb.servername_regexp = u'default'
    pb.ssl_protocols.extend([u'wrong'])
    with awtest.raises(ValidationError, text=u'ssl_protocols: unsupported protocol "wrong"'):
        m.validate()

    del pb.ssl_protocols[:]
    pb.ssl_protocols.extend([u'sslv3', u'tlsv1.3'])
    m.validate()
