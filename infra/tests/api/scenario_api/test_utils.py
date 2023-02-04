import pytest
import http.client

from infra.walle.server.tests.lib.util import MonkeyPatch, monkeypatch_method, TestCase
from walle.application import app
from walle.util.api import scenario_id_handler, api_response


@pytest.fixture(autouse=True, scope="module")
def test(request):
    monkeypatch = MonkeyPatch()
    request.addfinalizer(monkeypatch.undo)

    # Required by api_handler decorator
    monkeypatch.setattr(app, "flask", None)
    monkeypatch.setattr(app, "_Application__services", None)
    monkeypatch.setattr(app, "_Application__logging_initialized", None)
    monkeypatch.setattr(TestCase, "_app_initialized", False)
    app.init_flask()

    with app.init_blueprint("api", "/v1") as mock_api_blueprint:
        app.api_blueprint = mock_api_blueprint
        create_handler()

    monkeypatch_method(monkeypatch, app.setup_api_blueprint)
    monkeypatch_method(monkeypatch, app.setup_cms_api_blueprint)
    monkeypatch_method(monkeypatch, app.setup_metrics_blueprint)


def create_handler():
    @scenario_id_handler("/test/<scenario_id>", "GET")
    def api_func(scenario_id):
        return api_response({"scenario_id": int(scenario_id)})


@pytest.mark.parametrize("value", [0, 1, 10, 9999, 9999999])
def test_scenario_id_handler_allowed_values(walle_test, value):
    create_handler()
    result = walle_test.api_client.get("/v1/test/{}".format(value))
    assert result.status_code == http.client.OK
    assert result.json == {"scenario_id": value}


@pytest.mark.parametrize("value", [None, -1, "a"])
def test_scenario_id_handler_forbidden_values(walle_test, value):
    create_handler()
    result = walle_test.api_client.get("/v1/test/{}".format(value))
    assert result.status_code == http.client.BAD_REQUEST
