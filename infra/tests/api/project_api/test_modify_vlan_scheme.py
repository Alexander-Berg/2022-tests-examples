"""Tests VLAN scheme modification API."""

import http.client

import pytest

from infra.walle.server.tests.lib.util import TestCase
from walle.constants import VLAN_SCHEME_SEARCH, SHARED_VLANS, VLAN_SCHEME_STATIC, MTN_VLAN_SCHEMES


@pytest.fixture
def test(request, monkeypatch_production_env):
    test = TestCase.create(request)
    test.mock_projects()
    return test


@pytest.mark.parametrize("method", ("PUT", "DELETE"))
def test_unauthenticated(test, unauthenticated, method):
    if method == "PUT":
        data = {
            "scheme": VLAN_SCHEME_SEARCH,
            "native_vlan": 1000,
        }
    else:
        data = {}
    project = test.mock_project({"id": "some-id"})
    result = test.api_client.open(f"/v1/projects/{project.id}/vlan_scheme", method=method, data=data)
    assert result.status_code == http.client.UNAUTHORIZED
    test.projects.assert_equal()


@pytest.mark.parametrize("method", ("PUT", "DELETE"))
def test_unauthorized(test, unauthorized_project, method):
    project = test.mock_project({"id": "some-id"})
    result = test.api_client.open(
        "/v1/projects/" + project.id + "/vlan_scheme",
        method=method,
        data={"scheme": VLAN_SCHEME_SEARCH, "native_vlan": 1} if method == "PUT" else None,
    )
    assert result.status_code == http.client.FORBIDDEN
    test.projects.assert_equal()


@pytest.mark.usefixtures("monkeypatch_locks")
def test_modify_does_not_allow_use_of_vlans_not_owned_by_project(test):
    project = test.mock_project(
        {
            "id": "some-id",
            "owned_vlans": [2],
            "vlan_scheme": None,
            "native_vlan": None,
            "extra_vlans": None,
        }
    )

    result = test.api_client.put(
        "/v1/projects/" + project.id + "/vlan_scheme", data={"scheme": VLAN_SCHEME_SEARCH, "native_vlan": 1}
    )

    assert result.status_code == http.client.FORBIDDEN
    test.projects.assert_equal()


@pytest.mark.usefixtures("monkeypatch_locks")
@pytest.mark.parametrize("vlan_scheme", MTN_VLAN_SCHEMES)
def test_modify_does_not_allow_to_set_mtn_vlan_scheme(test, vlan_scheme):
    # mtn vlans scheme can not be used without hbf project id.
    # Setting hbf project id sets mtn vlan scheme automatically.

    project = test.mock_project(
        {
            "id": "some-id",
            "owned_vlans": [333],
            "vlan_scheme": None,
            "native_vlan": None,
            "extra_vlans": None,
        }
    )

    result = test.api_client.put(
        "/v1/projects/" + project.id + "/vlan_scheme", data={"scheme": vlan_scheme, "native_vlan": 333}
    )

    assert result.status_code == http.client.CONFLICT
    test.projects.assert_equal()


@pytest.mark.usefixtures("monkeypatch_locks")
def test_modify_saves_vlan_scheme_name_and_native_vlan(test):
    project = test.mock_project(
        {
            "id": "some-id",
            "owned_vlans": [1],
            "vlan_scheme": None,
            "native_vlan": None,
            "extra_vlans": None,
        }
    )

    result = test.api_client.put(
        "/v1/projects/" + project.id + "/vlan_scheme", data={"scheme": VLAN_SCHEME_SEARCH, "native_vlan": 1}
    )
    assert result.status_code == http.client.OK

    project.vlan_scheme = VLAN_SCHEME_SEARCH
    project.native_vlan = 1
    test.projects.assert_equal()


@pytest.mark.usefixtures("monkeypatch_locks")
def test_extra_vlans_cleaned_up_before_setting(test):
    project = test.mock_project(
        {
            "id": "some-id",
            "owned_vlans": [1, 2, 3],
            "vlan_scheme": VLAN_SCHEME_STATIC,
            "native_vlan": 3,
            "extra_vlans": None,
        }
    )

    result = test.api_client.put(
        "/v1/projects/" + project.id + "/vlan_scheme",
        data={"scheme": VLAN_SCHEME_SEARCH, "native_vlan": 1, "extra_vlans": [1, 2]},
    )
    assert result.status_code == http.client.OK

    project.vlan_scheme = VLAN_SCHEME_SEARCH
    project.native_vlan = 1
    project.extra_vlans = [2]
    test.projects.assert_equal()


@pytest.mark.usefixtures("monkeypatch_locks")
def test_shared_vlans_does_not_require_authorization(test):
    project = test.mock_project(
        {
            "id": "some-id",
            "owned_vlans": [1, 2, 3],
            "vlan_scheme": VLAN_SCHEME_STATIC,
            "native_vlan": 2,
            "extra_vlans": [1],
        }
    )

    result = test.api_client.put(
        "/v1/projects/" + project.id + "/vlan_scheme",
        data={"scheme": VLAN_SCHEME_SEARCH, "native_vlan": 1, "extra_vlans": [3] + SHARED_VLANS},
    )
    assert result.status_code == http.client.OK

    project.vlan_scheme = VLAN_SCHEME_SEARCH
    project.native_vlan = 1
    project.extra_vlans = sorted([3] + SHARED_VLANS)
    test.projects.assert_equal()


@pytest.mark.usefixtures("monkeypatch_locks")
@pytest.mark.parametrize("vlan_scheme", MTN_VLAN_SCHEMES)
def test_modify_removes_hbf_project_id(test, vlan_scheme):
    project = test.mock_project(
        {
            "id": "some-id",
            "owned_vlans": [1, 2, 3],
            "vlan_scheme": vlan_scheme,
            "native_vlan": 333,
            "extra_vlans": [688, 788],
            "hbf_project_id": 640,
        }
    )

    result = test.api_client.put(
        "/v1/projects/" + project.id + "/vlan_scheme",
        data={"scheme": VLAN_SCHEME_SEARCH, "native_vlan": 1, "extra_vlans": [3]},
    )
    assert result.status_code == http.client.OK

    project.vlan_scheme = VLAN_SCHEME_SEARCH
    project.native_vlan = 1
    project.extra_vlans = [3]
    del project.hbf_project_id

    test.projects.assert_equal()


@pytest.mark.usefixtures("monkeypatch_locks")
def test_delete_vlan_scheme_removes_native_vlan_and_extra_vlans(test):
    project = test.mock_project(
        {
            "id": "some-id",
            "owned_vlans": [1, 2, 3],
            "vlan_scheme": VLAN_SCHEME_SEARCH,
            "native_vlan": 1,
            "extra_vlans": [3],
        }
    )

    result = test.api_client.delete("/v1/projects/" + project.id + "/vlan_scheme")
    assert result.status_code == http.client.NO_CONTENT

    del project.vlan_scheme
    del project.native_vlan
    del project.extra_vlans
    test.projects.assert_equal()
