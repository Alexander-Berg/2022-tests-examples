"""Tests deploy config API."""

import pytest
import http.client

from infra.walle.server.tests.lib.util import TestCase
from walle import constants as walle_constants
from walle.clients import deploy, eine

_EINE_CONFIGS = ["eine-config1", "eine-config2"]
_LUI_CONFIGS = ["lui-config1", "lui-config2"]


@pytest.fixture
def test(request):
    return TestCase.create(request)


def test_get_empty_provisioners(test):
    result = test.api_client.get("/v1/deploy-configs", query_string={"provisioner": ""})
    assert result.status_code == http.client.OK
    assert result.json == {}


def test_get(mp, test):
    mp.method(eine.EineClient.get_profiles, return_value=_EINE_CONFIGS, obj=eine.EineClient)
    mp.function(deploy.get_deploy_configs, return_value=_LUI_CONFIGS)

    result = test.api_client.get("/v1/deploy-configs")
    assert result.status_code == http.client.OK
    assert result.json == {
        walle_constants.PROVISIONER_LUI: _LUI_CONFIGS,
        walle_constants.PROVISIONER_EINE: _EINE_CONFIGS,
    }

    result = test.api_client.get("/v1/deploy-configs", query_string={"provisioner": walle_constants.PROVISIONER_EINE})
    assert result.status_code == http.client.OK
    assert result.json == {walle_constants.PROVISIONER_EINE: _EINE_CONFIGS}
