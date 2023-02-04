import http.client as httplib

import pytest

from infra.walle.server.tests.lib.util import TestCase, monkeypatch_function
from walle.clients import abc

OAUTH_TOKEN_MOCK = "AQAD-much-serious-token"
ABC_HOST = "abc-back.y-t.ru"


@pytest.fixture(autouse=True)
def mock_abc_config(mp):
    mp.config("abc.host", ABC_HOST)
    mp.config("abc.access_token", OAUTH_TOKEN_MOCK)


@pytest.fixture
def test(request):
    return TestCase.create(request)


@pytest.mark.usefixtures("unauthenticated")
@pytest.mark.usefixtures("monkeypatches_for_noop_scenario")
def test_unauthenticated(test):
    plot = test.maintenance_plots.mock({"id": "plot-id"})
    result = test.api_client.delete("/v1/maintenance-plots/{}".format(plot.id))

    assert result.status_code == httplib.UNAUTHORIZED
    test.maintenance_plots.assert_equal()


@pytest.mark.usefixtures("monkeypatch_locks")
@pytest.mark.usefixtures("monkeypatches_for_noop_scenario")
def test_authenticated_but_not_admin(test, mp):
    monkeypatch_function(
        mp,
        func=abc.get_service_members,
        module=abc,
        return_value=[
            {
                "person": {"login": "some-dummy"},
            }
        ],
    )
    plot = test.maintenance_plots.mock({"id": "plot-id"})
    result = test.api_client.delete("/v1/maintenance-plots/{}".format(plot.id))

    assert result.status_code == httplib.FORBIDDEN
    test.maintenance_plots.assert_equal()


@pytest.mark.usefixtures("authorized_admin")
@pytest.mark.usefixtures("monkeypatch_locks")
@pytest.mark.usefixtures("monkeypatches_for_noop_scenario")
def test_authenticated_admin_allowed(test):
    plot = test.maintenance_plots.mock({"id": "plot-id"}, add=False)

    result = test.api_client.delete("/v1/maintenance-plots/{}".format(plot.id))

    assert result.status_code == httplib.NO_CONTENT
    test.maintenance_plots.assert_equal()


@pytest.mark.usefixtures("monkeypatch_locks")
@pytest.mark.usefixtures("monkeypatches_for_noop_scenario")
def test_user_has_role_in_service_allowed(test, mp):
    monkeypatch_function(
        mp,
        func=abc.get_service_members,
        module=abc,
        return_value=[
            {
                "person": {"login": TestCase.api_user},
            }
        ],
    )
    plot = test.maintenance_plots.mock({"id": "plot-id"}, add=False)

    result = test.api_client.delete("/v1/maintenance-plots/{}".format(plot.id))

    assert result.status_code == httplib.NO_CONTENT
    test.maintenance_plots.assert_equal()


@pytest.mark.usefixtures("authorized_admin")
@pytest.mark.usefixtures("monkeypatch_locks")
@pytest.mark.usefixtures("monkeypatches_for_noop_scenario")
def test_plot_is_used_by_a_project(test):
    plot = test.maintenance_plots.mock({"id": "plot-id"})
    test.mock_project(
        {"id": "plot-user-project-mock", "name": "Plot User Project Mock", "maintenance_plot_id": plot.id}
    )

    result = test.api_client.delete("/v1/maintenance-plots/{}".format(plot.id))
    assert result.status_code == httplib.CONFLICT

    test.maintenance_plots.assert_equal()
    test.projects.assert_equal()
