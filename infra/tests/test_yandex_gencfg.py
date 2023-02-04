import mock
import pytest
from requests import RequestException, Response

from sepelib.yandex.alemate import ContainerResourceLimits
from sepelib.yandex.gencfg import GencfgInstance, GencfgGroupCard, GencfgClient


def test_gencfg_instance(gencfg_instance_dict):
    cpu_limit = gencfg_instance_dict['limits']['cpu_limit']
    cpu_guarantee = gencfg_instance_dict['limits']['cpu_guarantee']
    memory_guarantee = gencfg_instance_dict['limits']['memory_guarantee']
    memory_limit = gencfg_instance_dict['limits']['memory_limit']
    gencfg_instance_dict['porto_limits'] = gencfg_instance_dict.pop('limits')
    gencfg_instance_dict['porto_limits']['cpu_cores_limit'] = cpu_limit
    gencfg_instance_dict['porto_limits']['cpu_cores_guarantee'] = cpu_guarantee
    g = GencfgInstance.from_gencfg_response(gencfg_instance_dict)
    for i in gencfg_instance_dict:
        assert getattr(g, i) == gencfg_instance_dict[i]
    assert g.limits.cpu_limit == cpu_limit
    assert g.limits.cpu_guarantee == cpu_guarantee
    assert g.limits.memory_guarantee == memory_guarantee
    assert g.limits.memory_limit == memory_limit


def test_genfg_instance_from_dict(gencfg_instance_dict):
    cpu_limit = gencfg_instance_dict['limits']['cpu_limit']
    cpu_guarantee = gencfg_instance_dict['limits']['cpu_guarantee']
    memory_guarantee = gencfg_instance_dict['limits']['memory_guarantee']
    memory_limit = gencfg_instance_dict['limits']['memory_limit']
    g = GencfgInstance.from_dict(gencfg_instance_dict)
    assert g.limits.cpu_limit == cpu_limit
    assert g.limits.cpu_guarantee == cpu_guarantee
    assert g.limits.memory_guarantee == memory_guarantee
    assert g.limits.memory_limit == memory_limit


def test_genfg_instance_to_dict(gencfg_instance_dict, res_container_dict):
    g = GencfgInstance.from_dict(gencfg_instance_dict)
    d = g.to_dict()
    for i in gencfg_instance_dict:
        assert d[i] == getattr(g, i)
    for limit in res_container_dict:
        assert d['limits'][limit] == getattr(g.limits, limit)


def test_gencfg_group_card(gencfg_groupcard_response):
    g = GencfgGroupCard.from_gencfg_response(gencfg_groupcard_response)
    assert g.group == gencfg_groupcard_response['name']
    assert g.owners == gencfg_groupcard_response['owners']
    assert g.tag_location.geo == 'sas'
    assert g.tag_location.itype == 'itype'
    assert g.tag_location.ctype == 'ctype'
    assert g.tag_location.prj == ['prj1', 'prj2']
    assert g.tag_location.metaprj == 'metaprj'


def test_gencfg_group_card_raise(gencfg_groupcard_response):
    gencfg_groupcard_response['tags']['prj'] = 0
    with pytest.raises(ValueError):
        GencfgGroupCard.from_gencfg_response(gencfg_groupcard_response)


def test_gencfg_client_wrapped_session():
    r = Response()
    r.status_code = 429
    func = mock.Mock(return_value=r)
    with pytest.raises(RequestException):
        GencfgClient._wrapped_session_get(func)
    r.status_code = 503
    with pytest.raises(RequestException):
        GencfgClient._wrapped_session_get(func)
    r.status_code = 200
    GencfgClient._wrapped_session_get(func)


def test_gencfg_client_from_config():
    cl = GencfgClient.from_config({})
    assert cl._base_url == cl._DEFAULT_BASE_URL
    assert cl._req_timeout == cl._DEFAULT_REQ_TIMEOUT
    assert cl._attempts == cl._DEFAULT_ATTEMPTS


def test_gencfg_client_from_invalid_config():
    with pytest.raises(ValueError):
        GencfgClient.from_config({'req_timeout': 0})
    with pytest.raises(ValueError):
        GencfgClient.from_config({'attempts': 0})


def test_gencfg_client_call_remote():
    r = Response()
    r.status_code = 200
    func = mock.Mock(return_value=r)
    GencfgClient()._call_remote(func)


def test_gencfg_client_request_group_instances():
    r = Response()
    r.status_code = 200
    with mock.patch('sepelib.yandex.gencfg.GencfgClient._call_remote', return_value=r):
        GencfgClient()._request_group_instances('test_group')


def test_gencfg_client_list_group_instances_json():
    r = Response()
    r.status_code = 200
    with mock.patch('sepelib.yandex.gencfg.GencfgClient._call_remote', return_value=r):
        GencfgClient().list_group_instances_json('test_group')


def test_gencfg_client_list_group_instances(gencfg_instance_dict):
    gencfg_instance_dict['porto_limits'] = gencfg_instance_dict.pop('limits')
    gencfg_instance_dict['porto_limits']['cpu_cores_limit'] = gencfg_instance_dict['porto_limits'].pop('cpu_limit')
    gencfg_instance_dict['porto_limits']['cpu_cores_guarantee'] = gencfg_instance_dict['porto_limits'].pop('cpu_guarantee')
    r = mock.Mock
    r.json = mock.Mock(return_value={'instances': [gencfg_instance_dict]})
    with mock.patch('sepelib.yandex.gencfg.GencfgClient._request_group_instances', return_value=r):
        instances = GencfgClient().list_group_instances('test_group')
        assert instances[0].hostname == gencfg_instance_dict['hostname']
        assert instances[0].port == gencfg_instance_dict['port']


def test_gencfg_client_list_intlookup_instances(gencfg_instance_dict):
    r = mock.Mock()
    d = gencfg_instance_dict.pop('limits')
    limits = ContainerResourceLimits.from_dict(d)
    gencfg_instance_dict['limits'] = limits
    r.json = mock.Mock(return_value={'instances': [gencfg_instance_dict]})
    with mock.patch('sepelib.yandex.gencfg.GencfgClient._call_remote', return_value=r):
        instances = GencfgClient().list_intlookup_instances('test_group')
        assert instances[0].hostname == gencfg_instance_dict['hostname']
        assert instances[0].port == gencfg_instance_dict['port']
        for l in d:
            assert d[l] == getattr(instances[0].limits, l)


def test_gencfg_client_describe_gencfg_version():
    r = mock.Mock()
    r.json = mock.Mock(return_value={'description': 'test_desc'})
    with mock.patch('sepelib.yandex.gencfg.GencfgClient._call_remote', return_value=r):
        assert GencfgClient().describe_gencfg_version('test_group') == 'test_desc'


def test_gencfg_client_get_hosts_info():
    r = mock.Mock()
    r.json = mock.Mock(return_value={'hosts_data': {'test_host_data': 'test'}})
    with mock.patch('sepelib.yandex.gencfg.GencfgClient._call_remote', return_value=r):
        assert GencfgClient().get_hosts_info(['test_fqdn']) == {'test_host_data': 'test'}


def test_gencfg_client_list_hosts():
    d = {
        "host_names" : [
            "100-43-90-6.yandex.com", "100-43-90-7.yandex.com", "zworker-s1.yandex.net", "zworker-test.yandex.net"
        ]
    }
    r = mock.Mock()
    r.json = mock.Mock(return_value=d)
    with mock.patch('sepelib.yandex.gencfg.GencfgClient._call_remote', return_value=r):
        assert GencfgClient().list_hosts() == d['host_names']


def test_gencfg_client_get_group_card():
    tags = {"itag": [], "prj": ["sgcluster-nanny"], "ctype": "prod", "itype": "nanny", "metaprj": "internal"}
    d = {
        "owners" : ["nekto0n"], "name" : "MSK_SG_NANNY", "tags": tags,
        "debug_info" : {
            "backend_host" : "man1-7286.search.yandex.net", "backend_port" : 7300,
            "branch" : "trunk", "backend_type" : "trunk"
        },
    }
    r = mock.Mock()
    r.json = mock.Mock(return_value=d)
    with mock.patch('sepelib.yandex.gencfg.GencfgClient._call_remote', return_value=r):
        card = GencfgClient().get_group_card('g')
        d['tags'] = tags
        c = GencfgGroupCard.from_gencfg_response(d)
        assert c.group == card.group
        assert c.owners == card.owners
        assert c.extra_tags == card.extra_tags
        assert c.tag_location.geo == card.tag_location.geo
        assert c.tag_location.prj == card.tag_location.prj
        assert c.tag_location.metaprj == card.tag_location.metaprj
        assert c.tag_location.itype == card.tag_location.itype
        assert c.tag_location.ctype == card.tag_location.ctype
