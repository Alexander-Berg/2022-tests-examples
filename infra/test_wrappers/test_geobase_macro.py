# conding: utf-8
import mock
from awacs.wrappers.base import Holder

from infra.awacs.proto import modules_pb2
from awacs.wrappers.main import GeobaseMacro


def test_geobase_macro_defaults():
    pb = modules_pb2.Holder()
    pb.geobase_macro.SetInParent()

    h = Holder(pb)
    with mock.patch.object(GeobaseMacro, 'require_nested'):
        h.validate()

    h.expand_macroses()
    config = h.to_config()

    geobase_config = config.table.get('geobase')
    assert geobase_config
    assert geobase_config.table['file_switch'] == GeobaseMacro.DEFAULT_FILE_SWITCH
    geo_config = geobase_config.table.get('geo')
    assert geo_config
    report_config = geo_config.table.get('report')
    assert report_config
    assert report_config.table['uuid'] == GeobaseMacro.DEFAULT_REPORT_UUID
    stats_eater_config = report_config.table.get('stats_eater')
    assert stats_eater_config
    balancer2_config = stats_eater_config.table.get('balancer2')
    assert balancer2_config
    assert balancer2_config.table['attempts'] == GeobaseMacro.DEFAULT_ATTEMPTS_NUMBER
    rr_config = balancer2_config.table.get('rr')
    assert rr_config
    instance_configs, proxy_options = rr_config.array.args[0].array, rr_config.array.args[1].table

    assert len(instance_configs) == 1
    instance_config = instance_configs[0]
    assert instance_config.array == ['laas.yandex.ru', 80, 1.0]

    assert proxy_options['connect_timeout'] == GeobaseMacro.DEFAULT_CONNECT_TIMEOUT
    assert proxy_options['backend_timeout'] == GeobaseMacro.DEFAULT_BACKEND_TIMEOUT
    assert proxy_options['keepalive_count'] == GeobaseMacro.DEFAULT_KEEPALIVE_COUNT


def test_geobase_macro():
    pb = modules_pb2.Holder()
    pb.geobase_macro.report_uuid = REPORT_UUID = 'test'
    pb.geobase_macro.file_switch = FILE_SWITCH = './test.txt'
    pb.geobase_macro.attempts = ATTEMPTS = 300
    pb.geobase_macro.generated_proxy_backends.proxy_options.connect_timeout = CONNECT_TIMEOUT = '1000s'
    pb.geobase_macro.generated_proxy_backends.instances.add(host='test.yandex-team.ru', port=8080, weight=10)

    h = Holder(pb)
    with mock.patch.object(GeobaseMacro, 'require_nested'):
        h.validate()

    h.expand_macroses()
    config = h.to_config()

    geobase_config = config.table.get('geobase')
    assert geobase_config
    assert geobase_config.table['file_switch'] == FILE_SWITCH
    geo_config = geobase_config.table.get('geo')
    assert geo_config
    report_config = geo_config.table.get('report')
    assert report_config
    assert report_config.table['uuid'] == REPORT_UUID
    stats_eater_config = report_config.table.get('stats_eater')
    assert stats_eater_config
    balancer2_config = stats_eater_config.table.get('balancer2')
    assert balancer2_config
    assert balancer2_config.table['attempts'] == ATTEMPTS
    rr_config = balancer2_config.table.get('rr')
    assert rr_config
    instance_configs, proxy_options = rr_config.array.args[0].array, rr_config.array.args[1].table

    assert len(instance_configs) == 1
    instance_config = instance_configs[0]
    assert instance_config.array == ['test.yandex-team.ru', 8080, 10.0]

    assert proxy_options['connect_timeout'] == CONNECT_TIMEOUT
    assert proxy_options['backend_timeout'] == GeobaseMacro.DEFAULT_BACKEND_TIMEOUT
    assert proxy_options['keepalive_count'] == GeobaseMacro.DEFAULT_KEEPALIVE_COUNT
