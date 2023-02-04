from .conftest import AnalyticsBackend, auth_headers


def test_cors(app: AnalyticsBackend):
    path = '/companies/6/plan-fact/routes?start_date=2021-01-01&end_date=2021-02-01'
    response = app.request(path, method='OPTIONS', headers={**auth_headers('1-admin'), 'origin': 'https://yandex.ru'})
    assert response.status_code == 200
    assert response.headers.get('Access-Control-Allow-Origin') == 'https://yandex.ru'

    response = app.request(path, headers={**auth_headers('1-admin'), 'origin': 'https://yandex.ru'})
    assert response.headers.get('Access-Control-Allow-Origin') == 'https://yandex.ru'
