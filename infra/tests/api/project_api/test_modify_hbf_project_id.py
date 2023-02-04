"""Tests project modify API."""

import pytest
import http.client

from infra.walle.server.tests.lib.util import TestCase, drop_none
from walle.clients import racktables
from walle.constants import (
    VLAN_SCHEME_MTN,
    MTN_NATIVE_VLAN,
    MTN_EXTRA_VLANS,
    VLAN_SCHEME_STATIC,
    VLAN_SCHEME_MTN_HOSTID,
    MTN_VLAN_SCHEMES,
    MTN_IP_METHOD_MAC,
    MTN_IP_METHOD_HOSTNAME,
    VLAN_SCHEME_MTN_WITHOUT_FASTBONE,
)

_HBF_PROJECT_ID_STR = "984e3"
_HBF_PROJECT_ID_INT = 0x984E3


@pytest.fixture
def test(request, monkeypatch_production_env):
    test = TestCase.create(request)
    test.mock_projects()
    return test


@pytest.fixture
def monkeypatch_racktables(mp):
    mp.function(racktables.get_hbf_projects, return_value={_HBF_PROJECT_ID_INT: "HBF_PROJECT_MACRO"})


@pytest.mark.parametrize("method", ["POST", "PATCH"])
def test_unauthenticated(test, unauthenticated, method):
    project = test.mock_project({"id": "some-id"})

    result = test.api_client.open(
        "/v1/projects/{}/hbf_project_id".format(project.id), method=method, data={"hbf_project_id": _HBF_PROJECT_ID_STR}
    )
    assert result.status_code == http.client.UNAUTHORIZED

    test.projects.assert_equal()


@pytest.mark.parametrize("method", ["POST", "PATCH"])
def test_unauthorized(test, unauthorized_project, method):
    project = test.mock_project({"id": "some-id"})

    result = test.api_client.open(
        "/v1/projects/{}/hbf_project_id".format(project.id), method=method, data={"hbf_project_id": _HBF_PROJECT_ID_STR}
    )
    assert result.status_code == http.client.FORBIDDEN

    test.projects.assert_equal()


@pytest.mark.parametrize("method", ["POST", "PATCH"])
@pytest.mark.parametrize("reason", [None, "reason mock"])
@pytest.mark.parametrize("old_hbf_project_id", [None, 0x100])
@pytest.mark.parametrize("old_vlan_scheme", [None, VLAN_SCHEME_STATIC])
@pytest.mark.parametrize(
    ["ip_method", "vlan_scheme", "use_fastbone"],
    [
        (None, VLAN_SCHEME_MTN, True),
        (MTN_IP_METHOD_MAC, VLAN_SCHEME_MTN, True),
        (MTN_IP_METHOD_HOSTNAME, VLAN_SCHEME_MTN_HOSTID, True),
        (MTN_IP_METHOD_MAC, VLAN_SCHEME_MTN_WITHOUT_FASTBONE, False),
    ],
)
@pytest.mark.usefixtures("monkeypatch_locks", "monkeypatch_racktables")
def test_set_hbf_project_id(
    test, method, reason, old_hbf_project_id, old_vlan_scheme, ip_method, vlan_scheme, use_fastbone
):
    project = test.mock_project(
        drop_none(
            {
                "id": "some-id",
                "name": "Some name",
                "hbf_project_id": old_hbf_project_id,
                "vlan_scheme": old_vlan_scheme,
                "owned_vlans": [1, 2, 3],
                "native_vlan": 1 if old_vlan_scheme else None,
                "extra_vlans": [2, 3] if old_vlan_scheme else None,
            }
        )
    )
    result = test.api_client.open(
        "/v1/projects/{}/hbf_project_id".format(project.id),
        method=method,
        data=drop_none(
            {
                "hbf_project_id": _HBF_PROJECT_ID_STR,
                "ip_method": ip_method,
                "use_fastbone": use_fastbone,
                "reason": reason,
            }
        ),
    )
    assert result.status_code == http.client.NO_CONTENT

    # we need to:
    # 1. set hbf/mtn project id
    # 2. set vlan scheme to mtn, native vlans to 333, extra vlans to 688,788
    # 3. keep owned vlans unchanged so that project could go back from mtn to previous vlan scheme
    project.hbf_project_id = _HBF_PROJECT_ID_INT
    project.vlan_scheme = vlan_scheme
    project.native_vlan = MTN_NATIVE_VLAN
    project.extra_vlans = MTN_EXTRA_VLANS

    test.projects.assert_equal()


@pytest.mark.parametrize("reason", [None, "reason mock"])
@pytest.mark.parametrize("vlan_scheme", [None] + MTN_VLAN_SCHEMES)
@pytest.mark.usefixtures("monkeypatch_locks")
def test_unset_hbf_project_id(test, reason, vlan_scheme):
    project = test.mock_project(
        {
            "id": "some-id",
            "name": "Some name",
            "owned_vlans": [1, 2, 3],
            "hbf_project_id": _HBF_PROJECT_ID_INT if vlan_scheme else None,
            "vlan_scheme": vlan_scheme,
            "native_vlan": MTN_NATIVE_VLAN if vlan_scheme else None,
            "extra_vlans": MTN_EXTRA_VLANS if vlan_scheme else None,
        }
    )

    result = test.api_client.open(
        "/v1/projects/{}/hbf_project_id".format(project.id), method="DELETE", data=drop_none({"reason": reason})
    )
    assert result.status_code == http.client.NO_CONTENT

    # wall-e needs to:
    # 1. unset hbf project id, if set
    # 2. drop vlan scheme, native vlan, extra vlans - if vlan scheme is mtn
    # 3. keep owned vlans and all other settings.
    del project.hbf_project_id
    del project.vlan_scheme
    del project.native_vlan
    del project.extra_vlans

    test.projects.assert_equal()


@pytest.mark.parametrize("reason", [None, "reason mock"])
@pytest.mark.usefixtures("monkeypatch_locks")
def test_unset_hbf_project_id_when_project_not_in_mtn(test, reason):
    project = test.mock_project(
        {
            "id": "some-id",
            "name": "Some name",
            "hbf_project_id": None,
            "vlan_scheme": VLAN_SCHEME_STATIC,
            "native_vlan": 1,
            "extra_vlans": [2, 3],
        }
    )

    result = test.api_client.open(
        "/v1/projects/{}/hbf_project_id".format(project.id), method="DELETE", data=drop_none({"reason": reason})
    )
    assert result.status_code == http.client.NO_CONTENT

    # wall-e needs to keep current vlan scheme and make no other changes because they are not expected nor needed.
    test.projects.assert_equal()
