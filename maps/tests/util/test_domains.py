import pytest
from flask import Flask

from maps.b2bgeo.ya_courier.backend.test_lib.conftest import skip_if_remote
from ya_courier_backend.util.domains import get_enabled_services
from ya_courier_backend.resources.logistic_company import ServiceType


@pytest.fixture(scope='module')
def _flask_mock_app():
    app = Flask(__name__)
    with app.app_context():
        yield app


@skip_if_remote
def test_default_services(_flask_mock_app):
    with _flask_mock_app.test_request_context('/', base_url="http://courier.yandex.ru"):
        services = get_enabled_services()
    assert services == set([ServiceType.mvrp, ServiceType.courier])

    with _flask_mock_app.test_request_context('/', base_url="http://courier.yandex.com.tr"):
        services = get_enabled_services()
    assert services == set([ServiceType.mvrp])
