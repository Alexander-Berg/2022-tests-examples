from flask import Flask
import pytest
from sepelib.yandex.its import ItsClient
from sepelib.yandex.oauth import OAuth
from sepelib.yandex.passport import PassportClient
from sepelib.yandex.staff import StaffClient


@pytest.fixture
def res_container_dict():
    params = {
        'memory_guarantee': 256, 'memory_limit': 256, 'cpu_guarantee': 4, 'cpu_limit': 4, 'cpu_policy': 'normal',
        'io_limit': 8, 'io_policy': 'some_io_policy', 'net_guarantee': 4, 'net_limit': 6, 'net_priority': 1,
        'net_tos': 'default', 'disk_guarantee': 4, 'disk_limit': 4, 'dirty_limit': 1,
        'recharge_on_pgfault': False,
    }
    return params


@pytest.fixture
def iss_hook_time_limits_dict():
    params = {
        'min_restart_period': 1, 'max_execution_time': 2, 'restart_period_backoff': 3,
        'max_restart_period': 4, 'restart_period_scale': 5
    }
    return params


@pytest.fixture
def gencfg_instance_dict(res_container_dict):
    d = {
        'hostname': 'host',
        'port': 80,
        'limits': res_container_dict
    }
    return d


@pytest.fixture
def gencfg_groupcard_response():
    return {
        'name': 'sas_test_app',
        'tags': {'prj': ['prj1', 'prj2'], 'itype': 'itype', 'ctype': 'ctype', 'metaprj': 'metaprj'},
        'owners': ['owner1']
    }


@pytest.fixture
def its_dict():
    return {'token': 'test_token'}


@pytest.fixture
def its_client_mock(its_dict):
    return ItsClient.from_config(its_dict)


@pytest.fixture
def oauth_client_mock():
    return OAuth.from_config({
        'url': 'url',
        'client_id': 'client_id',
        'client_secret': 'client_secret',
        'scopes': ['test:scope']
    })


@pytest.fixture
def passport_client_mock():
    d = {
        'blackbox_url': 'url',
        'blackbox_auth_url': 'url',
        'req_timeout': 5,
    }
    return PassportClient.from_config(d)


@pytest.fixture
def staff_client_mock():
    return StaffClient.from_config({})


@pytest.fixture
def flask_test_app():
    return Flask(__name__)
