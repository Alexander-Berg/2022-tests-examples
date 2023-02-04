# coding: utf-8
import pytest
import six

from awacs.lib import yasm_client


@pytest.fixture
def juggler_namespace():
    return 'test_awacs.yasm_alerts_testing'


def build_alert_json(prefix, name, juggler_namespace, juggler_check_tags, signal, warn_threshold, crit_threshold,
                     itype, ctype, prj, geo):
    alert_name = "{}/{}".format(prefix, name)
    tags = {
        'itype': [itype],
        'ctype': [ctype],
        'prj': [prj],
        'geo': [geo]
    }
    juggler_check = {
        'service': alert_name,
        'host': 'yasm_host',
        'namespace': juggler_namespace,
        'tags': juggler_check_tags
    }
    return {
        'name': alert_name,
        'signal': signal,
        'tags': tags,
        'warn': warn_threshold,
        'crit': crit_threshold,
        'juggler_check': juggler_check,
        'mgroups': ['ASEARCH']
    }


@pytest.fixture
def name_prefix():
    return 'test_awacs.'


@pytest.mark.vcr
def test_yasm_replace_alerts(name_prefix, juggler_namespace):
    client = yasm_client.YasmClient(name_prefix)
    wrong_alert = build_alert_json(
        prefix='1' + juggler_namespace,
        name='test1',
        juggler_namespace=juggler_namespace,
        juggler_check_tags=[],
        signal='perc(portoinst-cpu_usage_cores_tmmv, portoinst-cpu_limit_cores_tmmv)',
        warn_threshold=[50, 60],
        crit_threshold=[60, None],
        itype='balancer',
        ctype='prod',
        prj='qyp-testing.in.yandex-team.ru',
        geo='sas',
    )

    with pytest.raises(ValueError) as e:
        client.replace_alerts("1" + juggler_namespace, alerts=[wrong_alert])
    assert six.text_type(e.value) == ('Wrong alerts prefix: "1test_awacs.yasm_alerts_testing",'
                                      ' should starts with "test_awacs."')

    with pytest.raises(ValueError) as e:
        client.replace_alerts(juggler_namespace, alerts=[wrong_alert])
    assert six.text_type(e.value) == 'All alerts names should starts with passed prefix: "test_awacs.yasm_alerts_testing"'

    with pytest.raises(ValueError) as e:
        client.replace_alerts(juggler_namespace, alerts=[wrong_alert] * (client.REPLACE_ALERTS_MAX_COUNT + 1))
    assert six.text_type(e.value) == 'replace_alerts does not support more then {} alerts for replace'.format(
        client.REPLACE_ALERTS_MAX_COUNT)

    alert = build_alert_json(
        prefix=juggler_namespace,
        name='test1',
        juggler_namespace=juggler_namespace,
        juggler_check_tags=[],
        signal='perc(portoinst-cpu_usage_cores_tmmv, portoinst-cpu_limit_cores_tmmv)',
        warn_threshold=[50, 60],
        crit_threshold=[60, None],
        itype='balancer',
        ctype='prod',
        prj='qyp-testing.in.yandex-team.ru',
        geo='sas',
    )

    result = client.replace_alerts(juggler_namespace + '/', alerts=[alert])
    assert result.created == 1
    assert result.deleted == 0

    result = client.replace_alerts(juggler_namespace + '/', alerts=[alert])
    assert result.created == 0
    assert result.deleted == 0

    result = client.replace_alerts(juggler_namespace + '/', alerts=[])
    assert result.created == 0
    assert result.deleted == 1
