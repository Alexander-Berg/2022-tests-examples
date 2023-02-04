from __future__ import unicode_literals

import json
import socket

import mock

import yatest.common

from infra.ya_salt.lib import gencfg


def _get_response_bytes():
    with open(yatest.common.source_path('infra/ya_salt/lib/tests/gencfg_response.json')) as f:
        return f.read()


def test_transform_response():
    d = json.loads(_get_response_bytes())
    # Bad case
    cfg, err = gencfg._transform_response('tags/stable-122-r211', {})
    assert cfg is None
    assert err
    # Good case
    cfg, err = gencfg._transform_response('tags/stable-122-r211', d)
    assert err is None
    assert cfg == {'gencfg': ['ALL_KERNEL_TEST_MAIN', 'ALL_RTC', 'ALL_RUNTIME', 'ALL_SEARCH',
                              'SAS_CALLISTO_DEPLOY', 'SAS_IMGS_SAAS_QUICK_BASE', 'SAS_JUGGLER_CLIENT_STABLE',
                              'SAS_KERNEL_UPDATE_1', 'SAS_PSI_DYNAMIC', 'SAS_PSI_DYNAMIC_AGENTS',
                              'SAS_PSI_DYNAMIC_ROTOR', 'SAS_PSI_YT_MASTER', 'SAS_RTC_SLA_TENTACLES_PROD',
                              'SAS_RUNTIME', 'SAS_SEARCH', 'SAS_SKYNET_EXPERIMENT', 'SAS_WEB_BASE',
                              'SAS_WEB_CALLISTO_CAM_BASE', 'SAS_WEB_CALLISTO_CAM_INT', 'SAS_WEB_DEPLOY',
                              'SAS_WEB_GEMINI_BASE', 'SAS_WEB_INT', 'SAS_WEB_INTL2', 'SAS_WEB_TIER0_JUPITER_BASE',
                              'SAS_WEB_TIER0_JUPITER_BASE_HAMSTER', 'SAS_WEB_TIER0_JUPITER_INT_HAMSTER',
                              'SAS_YASM_YASMAGENT_PRESTABLE', 'SAS_YASM_YASMAGENT_STABLE', 'SAS_YT_PROD2_PORTOVM',
                              ],
                   'gcfg_tag': 'tags/stable-122-r211'}


def test_load_from_api():
    tag = 'tags/stable-122-r211'
    m = mock.Mock()
    m.side_effect = Exception('Something bad happened')
    cfg, err = gencfg._load_from_api(tag, 'test-gencfg.search.yandex.net', get_func=m)
    assert err
    url = 'https://api.gencfg.yandex-team.ru/{}/hosts/test-gencfg.search.yandex.net/instances_tags'.format(tag)
    m.assert_called_once_with(url)
    # Test status code check
    m = mock.Mock()

    class FakeResponse(object):
        def __init__(self, code, content=''):
            self.status_code = code
            self.content = content

    m.return_value = FakeResponse(504)
    cfg, err = gencfg._load_from_api(tag, 'test-gencfg.search.yandex.net', get_func=m)
    assert err
    m.assert_called_once_with(url)
    # Test invalid content
    m.reset_mock()
    m.return_value = FakeResponse(200, 'some garbage')
    cfg, err = gencfg._load_from_api(tag, 'test-gencfg.search.yandex.net', get_func=m)
    assert err
    # Test ok
    m.reset_mock()
    m.return_value = FakeResponse(200, _get_response_bytes())
    cfg, err = gencfg._load_from_api(tag, 'test-gencfg.search.yandex.net', get_func=m)
    assert not err
    assert cfg


def test_gencfg():
    load_func = mock.Mock(return_value=(None, 'failed to load'))
    assert gencfg.gencfg('stable-112/r-123', load_func) == gencfg.EMPTY_CONFIG
    load_func.assert_called_once_with('stable-112/r-123', socket.gethostname())

    # Check partialy filled cache file: RUNTIMECLOUD-9529
    load_func = mock.Mock(return_value=(None, 'failed to load'))
    cache_func = mock.Mock(return_value=('''{"gencfg": []}''', None))
    assert gencfg.gencfg('stable-112/r-123', load_func, cache_func) == gencfg.EMPTY_CONFIG
    load_func.assert_called_once_with('stable-112/r-123', socket.gethostname())
    cache_func.assert_called_once_with(gencfg.GENCFG_DATA_FILE)

    # Check using cache only, if tags in arg and cache matches
    cache = {"gencfg": ["ALL_QLOUD",
                        "ALL_QLOUD_HOSTS",
                        "ALL_RTC",
                        "ALL_WALLE_TAG_QLOUD",
                        "SAS_JUGGLER_CLIENT_STABLE",
                        "a_ctype_prod",
                        "a_dc_sas",
                        "a_geo_sas",
                        "a_itype_jugglerclient"],
             "gcfg_tag": "tags/stable-112-123"}
    load_func = mock.Mock(return_value=(None, 'failed to load'))
    cache_func = mock.Mock(return_value=(cache, None))
    assert gencfg.gencfg('tags/stable-112-123', load_func, cache_func) == cache
    load_func.assert_not_called()
    cache_func.assert_called_once_with(gencfg.GENCFG_DATA_FILE)
