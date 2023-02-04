import http.client

import pytest

from walle.expert.types import get_limit_name, CheckType, get_walle_check_type


def test_unauthenticated(walle_test, unauthenticated):
    result = walle_test.api_client.open(
        "/v1/settings/global_timed_limits_overrides/foo",
        method="PUT",
        data={"disable_automation": True},
    )
    assert result.status_code == http.client.UNAUTHORIZED


def test_unauthorized(walle_test):
    result = walle_test.api_client.open(
        "/v1/settings/global_timed_limits_overrides/foo",
        method="PUT",
        data={"disable_automation": True},
    )
    assert result.status_code == http.client.FORBIDDEN


@pytest.mark.usefixtures("authorized_admin")
def test_global_limits_override_crud(walle_test):
    def _check(expected_state):
        res = walle_test.api_client.open("/v1/settings/global_timed_limits_overrides", method="GET")
        assert res.status_code == http.client.OK
        assert res.json == expected_state

    _check({})
    failure_name = get_limit_name(get_walle_check_type(CheckType.REBOOTS))

    overrided_data = {"limit": 42, "period": "5h"}
    walle_test.api_client.open(
        f"/v1/settings/global_timed_limits_overrides/{failure_name}", method="PUT", data=overrided_data
    )
    _check({failure_name: overrided_data})

    overrided_data["limit"] = 50
    walle_test.api_client.open(
        f"/v1/settings/global_timed_limits_overrides/{failure_name}", method="PUT", data=overrided_data
    )
    _check({failure_name: overrided_data})

    walle_test.api_client.open(f"/v1/settings/global_timed_limits_overrides/{failure_name}", method="DELETE")
    _check({})
