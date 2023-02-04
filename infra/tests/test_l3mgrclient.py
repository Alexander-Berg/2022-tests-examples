# coding: utf-8
import pytest
from six.moves import http_client as httplib

import awtest
from awacs.lib import l3mgrclient


@pytest.fixture
def client():
    yield l3mgrclient.L3MgrClient(
        url='https://l3-api.tt.yandex-team.ru:8080/',
        token='AQAD-XXX',
    )


@pytest.mark.vcr
def test_l3mgr_client_get_service(client):
    """
    :type client: l3mgrclient.L3MgrClient
    """
    expected = {
        'abc': 'SR',
        'url': '/api/v1/service/735',
        'fqdn': 'l3mgr-awacs-test.yandex-team.ru',
        'id': 735,
        'state': 'ACTIVE',
        'vs': [
            {'status': [
                {'timestamp': '2018-04-13T08:24:48.039Z', 'state': 'ACTIVE',
                 'lb': {'full': False, 'name': 'myt-lb24', 'url': '/api/v1/balancer/242',
                        'fqdn': 'myt-lb24.yndx.net', 'state': 'ACTIVE', 'location': ['IVA', 'MYT'],
                        'test_env': False, 'id': 242}, 'description': 'Updated at 2018-04-13 08:24:44.837974+00:00'},
                {'timestamp': '2018-04-13T08:24:38.119Z', 'state': 'ACTIVE',
                 'lb': {'full': False, 'name': 'sas1-lb26', 'url': '/api/v1/balancer/67',
                        'fqdn': 'sas1-lb26.yndx.net', 'state': 'ACTIVE', 'location': ['SAS'], 'test_env': False,
                        'id': 67}, 'description': 'Updated at 2018-04-13 08:24:34.419381+00:00'}], 'lb': [],
                'ext_id': '3b3038081cbc9b4b54aa4fdee4dd8a75d5932981e2c63b8a976a6a1457bce9a5',
                'url': '/api/v1/service/735/vs/4063', 'total_count': 2,
                'editable': False, 'active_count': 0, 'id': 4063,
                'ip': '2a02:6b8:0:3400:0:2da:0:2', 'protocol': 'TCP',
                'group': ['sas1-1752.search.yandex.net'],
                'config': {'QUORUM': '1', 'CHECK_URL': '/ping',
                           'CHECK_RETRY_TIMEOUT': None, 'CHECK_TYPE': 'HTTP_GET',
                           'STATUS_CODE': '200', 'HYSTERESIS': '0',
                           'CONNECT_PORT': None, 'DYNAMICACCESS': False, 'HOST': '',
                           'WEIGHT_DC_VLA': 100, 'ANNOUNCE': False,
                           'SCHEDULER': 'wlc', 'WEIGHT_DC_MSK': 100,
                           'WEIGHT_DC_MAN': 100, 'CHECK_RETRY': None,
                           'DC_FILTER': False, 'METHOD': 'TUN', 'DIGEST': '',
                           'CHECK_TIMEOUT': None}, 'port': 80}, {'status': [
                {'timestamp': '2018-04-13T08:24:38.119Z', 'state': 'ACTIVE',
                 'lb': {'full': False, 'name': 'sas1-lb26', 'url': '/api/v1/balancer/67',
                        'fqdn': 'sas1-lb26.yndx.net', 'state': 'ACTIVE', 'location': ['SAS'], 'test_env': False,
                        'id': 67}, 'description': 'Updated at 2018-04-13 08:24:34.419381+00:00'},
                {'timestamp': '2018-04-13T08:24:48.039Z', 'state': 'ACTIVE',
                 'lb': {'full': False, 'name': 'myt-lb24', 'url': '/api/v1/balancer/242',
                        'fqdn': 'myt-lb24.yndx.net', 'state': 'ACTIVE', 'location': ['IVA', 'MYT'],
                        'test_env': False, 'id': 242}, 'description': 'Updated at 2018-04-13 08:24:44.837974+00:00'}],
                'lb': [],
                'ext_id': '3f59f0180b6dcd73902dfd3b54da44ba1deeedbfdfba16f9af0eccaab2caa522',
                'url': '/api/v1/service/735/vs/4062',
                'total_count': 2,
                'editable': False,
                'active_count': 0,
                'id': 4062,
                'ip': '2a02:6b8:0:3400:0:2da:0:2',
                'protocol': 'TCP',
                'group': [
                    'sas1-1752.search.yandex.net'],
                'config': {
                    'QUORUM': '1',
                    'CHECK_URL': '/ping',
                    'CHECK_RETRY_TIMEOUT': None,
                    'CHECK_TYPE': 'HTTP_GET',
                    'STATUS_CODE': '200',
                    'HYSTERESIS': '0',
                    'CONNECT_PORT': None,
                    'DYNAMICACCESS': False,
                    'HOST': '',
                    'WEIGHT_DC_VLA': 100,
                    'ANNOUNCE': False,
                    'SCHEDULER': 'wrr',
                    'WEIGHT_DC_MSK': 100,
                    'WEIGHT_DC_MAN': 100,
                    'CHECK_RETRY': None,
                    'DC_FILTER': False,
                    'METHOD': 'TUN',
                    'DIGEST': '',
                    'CHECK_TIMEOUT': None},
                'port': 443}
        ],
        'action': [],
        'config': {
            'comment': 'romanovich: Autoconfig on RS updating',
            'service': {
                'abc': 'SR',
                'url': '/api/v1/service/735',
                'fqdn': 'l3mgr-awacs-test.yandex-team.ru',
                'archive': False,
                'state': 'ACTIVE',
                'id': 735
            },
            'vs_id': [4063, 4062],
            'url': '/api/v1/service/735/config/2461',
            'timestamp': '2018-02-02T06:32:06.846Z',
            'description': 'Updated state "DEPLOYING" -> "ACTIVE"',
            'state': 'ACTIVE',
            'id': 2461,
            'history': [1776, 1777, 1779, 1780, 1785, 1786, 1819, 1844, 1907, 1912,
                        1915, 1916, 1917, 1918, 1919, 1920, 1923, 1922, 1924, 1925,
                        1926, 1928, 1929, 1930, 1931, 1933, 1934, 1936, 1937, 1939,
                        1941, 1944, 1948, 1947, 1949, 1950, 1951, 1952, 1956, 1976,
                        1978, 1979, 1980]
        },
        'archive': False
    }
    actual = client.get_service(735)
    assert actual == expected


@pytest.mark.vcr
def test_l3mgr_client_get_config(client):
    """
    :type client: l3mgrclient.L3MgrClient
    """
    expected = {'comment': 'romanovich: Autoconfig on RS updating',
                'description': 'Updated state "DEPLOYING" -> "ACTIVE"',
                'service': {'abc': 'SR', 'url': '/api/v1/service/735', 'fqdn': 'l3mgr-awacs-test.yandex-team.ru',
                            'archive': False, 'state': 'ACTIVE', 'id': 735},
                'url': '/api/v1/service/735/config/2461', 'timestamp': '2018-02-02T06:32:06.846Z',
                'state': 'ACTIVE', 'action': [], 'vs_id': [4063, 4062], 'id': 2461,
                'history': [1776, 1777, 1779, 1780, 1785, 1786, 1819, 1844, 1907, 1912, 1915, 1916, 1917, 1918, 1919,
                            1920, 1923, 1922, 1924, 1925, 1926, 1928, 1929, 1930, 1931, 1933, 1934, 1936, 1937, 1939,
                            1941, 1944, 1948, 1947, 1949, 1950, 1951, 1952, 1956, 1976, 1978, 1979, 1980]}
    actual = client.get_config('735', '2461')
    assert actual == expected


@pytest.mark.vcr
def test_l3mgr_client_create_service_400(client):
    """
    :type client: l3mgrclient.L3MgrClient
    """
    with pytest.raises(l3mgrclient.L3MgrException) as e:
        client.create_service(fqdn='test2.test.ru', abc_code='new_nanny')
    exc = e.value
    assert exc.resp.status_code == 400
    assert exc.resp.json() == {'message': '* fqdn\n  * Service with this FQDN already exists.',
                               'errors': {'fqdn': ['Service with this FQDN already exists.']}, 'result': 'ERROR'}


@pytest.mark.vcr
def test_l3mgr_client_create_service_500(client):
    """
    :type client: l3mgrclient.L3MgrClient
    """
    with pytest.raises(l3mgrclient.L3MgrException) as e:
        client.create_service(fqdn='test4.test.ru', abc_code='new_nanny')
    exc = e.value
    assert exc.resp.status_code == 500
    assert exc.resp.json() == {'message': 'Try again later', 'result': 'Internal error'}


@pytest.mark.vcr
def test_l3mgr_client_create_service_200(client):
    """
    :type client: l3mgrclient.L3MgrClient
    """
    actual = client.create_service(
        fqdn=u'awacs-testing.test.ru', abc_code=u'rclb',
        data={
            u'meta-OWNER': u'awacs',
            u'meta-LINK': u'https://nanny.yandex-team.ru/ui/#/awacs/namespaces/list/a/l3-balancers/list/b/show/'
        })
    expected = {u'object': {u'id': 14040}, u'result': u'OK'}
    assert actual == expected


@pytest.mark.vcr
def test_l3mgr_client_get_ip(client):
    """
    :type client: l3mgrclient.L3MgrClient
    """
    actual = client.get_new_ip(abc_code='SR', fqdn=None)
    expected = {'object': '2a02:6b8:0:3400:0:2da:0:3', 'result': 'OK'}
    assert actual == expected


@pytest.mark.vcr
def test_l3mgr_client_create_virtual_server(client):
    """
    :type client: l3mgrclient.L3MgrClient
    """
    actual = client.create_virtual_server(svc_id=735, ip='2a02:6b8:0:3400:0:2da:0:3', port=10210, protocol='TCP')
    expected = {
        'object': {'ext_id': 'eb876b85a15ffa03e34c89e999a712cd288599867e923a8e962327a4ea5603dd', 'service': 735,
                   'id': 14711}, 'result': 'OK'}
    assert actual == expected


@pytest.mark.vcr
def test_l3mgr_client_add_virtual_server(client):
    """
    :type client: l3mgrclient.L3MgrClient
    """
    cfg = client.get_service(svc_id=735)
    vs_ids = [vs['id'] for vs in cfg['vs']]
    vs_ids.append(14711)  # new vs
    actual = client.create_config_with_vs(svc_id=735, vs_ids=vs_ids, comment='Add VS', use_etag=False)
    expected = {'object': {'id': 7132}, 'result': 'OK'}
    assert actual == expected

    actual = client.get_config(svc_id=735, cfg_id=7132)
    expected = {
        'comment': 'romanovich: Add VS', 'description': 'Created new config',
        'service': {
            'abc': 'SR',
            'url': '/api/v1/service/735',
            'fqdn': 'l3mgr-awacs-test.yandex-team.ru',
            'archive': False,
            'state': 'ACTIVE',
            'id': 735
        },
        'url': '/api/v1/service/735/config/7132',
        'timestamp': '2018-04-16T13:35:06.371Z',
        'state': 'NEW',
        'action': [],
        'vs_id': [4063, 4062, 14711],
        'id': 7132,
        'history': []
    }
    assert actual == expected


@pytest.mark.vcr
def test_l3mgr_client_save_config_if_match(client):
    """
    :type client: l3mgrclient.L3MgrClient
    """
    cfg = client.get_service(svc_id=12360)
    vs_ids = [vs['id'] for vs in cfg['vs']]
    vs_ids.append(2917293)  # new vs
    with awtest.raises(l3mgrclient.L3MgrException) as e:
        client.create_config_with_vs(svc_id=12360, vs_ids=vs_ids, comment='Add VS', use_etag=True, latest_cfg_id=1)
    assert e.value.resp.status_code == httplib.PRECONDITION_FAILED

    actual = client.create_config_with_vs(svc_id=12360, vs_ids=vs_ids,
                                          comment='Add VS', use_etag=True, latest_cfg_id=235653)
    assert actual == {"result": "OK", "object": {"id": 235654}}


@pytest.mark.vcr
def test_l3mgr_client_process(client):
    """
    :type client: l3mgrclient.L3MgrClient
    """
    actual = client.process_config(svc_id=735, cfg_id=7132, use_etag=True, latest_cfg_id=1)
    expected = {'object': {'id': 7132}, 'result': 'OK'}
    assert actual == expected


@pytest.mark.vcr
def test_l3mgr_client_process_if_match(client):
    """
    :type client: l3mgrclient.L3MgrClient
    """
    with awtest.raises(l3mgrclient.L3MgrException) as e:
        client.process_config(svc_id=12360, cfg_id=235640, use_etag=True, latest_cfg_id=1)
    assert e.value.resp.status_code == httplib.PRECONDITION_FAILED

    actual = client.process_config(svc_id=12360, cfg_id=235640, use_etag=True, latest_cfg_id=235640)
    assert actual == {'object': {'id': 235640}, 'result': 'OK'}


@pytest.mark.vcr
def test_l3mgr_client_edit_service_rs(client):
    """
    :type client: l3mgrclient.L3MgrClient
    """
    actual = client.create_config_with_rs(svc_id=735,
                                          groups=['@hbf:SAS_AWACS_BALANCER/stable-104-r98'],
                                          use_etag=True,
                                          latest_cfg_id=1)
    expected = {'object': {'id': 7134}, 'result': 'OK'}
    assert actual == expected


@pytest.mark.vcr
def test_l3mgr_client_edit_service_rs_if_match(client):
    """
    :type client: l3mgrclient.L3MgrClient
    """
    with awtest.raises(l3mgrclient.L3MgrException) as e:
        client.create_config_with_rs(svc_id=12360, groups=[], use_etag=True, latest_cfg_id=1)
    assert e.value.resp.status_code == httplib.PRECONDITION_FAILED

    actual = client.create_config_with_rs(svc_id=12360, groups=[], use_etag=True, latest_cfg_id=235640)
    assert actual == {'object': {'id': 235652}, 'result': 'OK'}


@pytest.mark.vcr
def test_l3mgr_client_edit_service_rs_2(client):
    """
    :type client: l3mgrclient.L3MgrClient
    """
    with pytest.raises(l3mgrclient.L3MgrException) as e:
        client.create_config_with_rs(
            svc_id=1959,
            groups=[
                'it3tx7wfxvmcs.sas-test.yp-c.yandex.net',
                '7g4mxq9k0m9vq.sas-test.yp-c.yandex.net',
                'vy2xp8ay6j2a6.sas-test.yp-c.yandex.net',
            ],
            use_etag=True,
            latest_cfg_id=1)
    assert e.match('Failed to resolve it3tx7wfxvmcs.sas-test.yp-c.yandex.net')
    assert e.value.resp.status_code == 400


@pytest.mark.vcr
def test_l3mgr_client_edit_service_rs_3(client):
    """
    :type client: l3mgrclient.L3MgrClient
    """
    groups = [
        'it3tx7wfxvmcs.sas-test.yp-c.yandex.net=2a02:6b8:c00:2e1:100:0:d35e:0',
        '7g4mxq9k0m9vq.sas-test.yp-c.yandex.net=2a02:6b8:c0c:a82:100:0:6692:0',
        'vy2xp8ay6j2a6.sas-test.yp-c.yandex.net=2a02:6b8:c0c:b02:100:0:2744:0',
    ]
    actual = client.create_config_with_rs(svc_id=735, groups=groups, use_etag=True, latest_cfg_id=1)
    expected = {'object': {'id': 8138}, 'result': 'OK'}
    assert actual == expected

    actual = client.get_config(735, 8138)
    expected = {
        'comment': 'romanovich: Autoconfig on RS updating',
        'description': 'Created new config',
        'vs_id': [16126, 16127],
        'url': '/api/v1/service/735/config/8138',
        'timestamp': '2018-05-03T08:52:09.538Z',
        'state': 'NEW',
        'service': {
            'abc': 'SR',
            'url': '/api/v1/service/735',
            'fqdn': 'l3mgr-awacs-test.yandex-team.ru',
            'archive': False,
            'state': 'ACTIVE',
            'id': 735
        },
        'action': [],
        'id': 8138,
        'history': []
    }
    assert actual == expected

    for vs_id in actual['vs_id']:
        actual = client.get_vs(735, vs_id)
        assert actual['group'] == groups


@pytest.mark.vcr
def test_l3mgr_set_fw_grants(client):
    """
    :type client: l3mgrclient.L3MgrClient
    """
    actual = client.set_grants(svc_id=496, subjects=['svc_SR'])
    expected = {'object': {'id': 496}, 'result': 'OK'}
    assert actual == expected


@pytest.mark.vcr
def test_l3mgr_get_fw_grants(client):
    """
    :type client: l3mgrclient.L3MgrClient
    """
    actual = client.list_grants(svc_id=496)
    expected = {"limit": 1, "page": 1, "objects": ["svc_rclb_administration"], "total": 1}
    assert actual == expected
