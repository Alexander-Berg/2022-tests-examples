import pytest
from flask import Flask
import requests
from maps.b2bgeo.test_lib.mock_pipedrive_gate import mock_pipedrive_gate, VALUE_TRIGGERING_INTERNAL_ERROR
from ya_courier_backend.util.pipedrive import create_pipedrive_card, create_pipedrive_card_tr
from ya_courier_backend.util.company_info import CompanyInfo
from maps.b2bgeo.ya_courier.backend.test_lib.conftest import skip_if_remote


@pytest.fixture(scope='module')
def _flask_mock_app():
    app = Flask(__name__)
    with mock_pipedrive_gate() as mock_pipedrive_url, \
            app.app_context():
        app.config['PIPEDRIVE_GATE_URL'] = mock_pipedrive_url
        yield mock_pipedrive_url, app


def _validate_pipedrive_card_response(response):
    j = response.json()
    assert isinstance(j, dict)
    assert j.keys() == {'organization_id', 'deal_id', 'person_id', 'note_id'}


@skip_if_remote
def test_pipedrive_request_local(_flask_mock_app):
    mock_pipedrive_url, app = _flask_mock_app
    company_info = CompanyInfo(
        name="Тайм-аут",
        manager_name="Акакий Назарыч Зирнбирнштейн",
        manager_phone="+79999999999",
        manager_email="testcourier@yandex.ru",
        manager_position="бас",
        vehicle_park_size="3 штуки",
        utm_source="maps",
        company_id=4567,
        yandex_id="123",
        facebook_id="789",
        problem_description="Описани какой-то проблемы",
        comments="Я знаком с Яндекс.Маршрутизацией",
        dadata={
            "value": "Название компании из dadata",
            "data": {"inn": "1234567890"},
        }
    )

    with app.test_request_context('/create-company', base_url="http://courier.yandex.ru"):
        response = create_pipedrive_card(company_info)
        assert response.headers
        assert response.headers.get('y-test-tag') == '[Routing] / Test-drive'
        _validate_pipedrive_card_response(response)

        company_info.apikey = '123'
        response = create_pipedrive_card(company_info)
        _validate_pipedrive_card_response(response)

        company_info.manager_name = VALUE_TRIGGERING_INTERNAL_ERROR
        with pytest.raises(requests.exceptions.RequestException):
            create_pipedrive_card(company_info)


@skip_if_remote
def test_pipedrive_request_local_tr(_flask_mock_app):
    mock_pipedrive_url, app = _flask_mock_app
    company_info = CompanyInfo(
        name="Тайм-аут",
        manager_name="Акакий Назарыч Зирнбирнштейн",
        manager_phone="+79999999999",
        manager_email="testcourier@yandex.ru",
        manager_position="бас",
        vehicle_park_size="3 штуки",
        yandex_id="123",
        google_id="456",
    )

    with app.test_request_context('/create-company', base_url="http://courier.yandex.com.tr"):
        response = create_pipedrive_card_tr(company_info)
        assert response.headers
        assert response.headers.get('y-test-tag') == '[Routing] / Test-drive-TURKEY'
        _validate_pipedrive_card_response(response)
