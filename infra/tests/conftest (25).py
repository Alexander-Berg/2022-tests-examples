import pytest
from infra.rtc_sla_tentacles.backend.lib.api.api_class import Api


@pytest.fixture
def api(config_interface):
    return Api(config_interface)


@pytest.fixture
def client(config_interface, api):
    app = api.make_api_app(log_file=False, log_stdout=True, it_is_uwsgi=False)
    assert api.processes == 1
    return app.test_client()
