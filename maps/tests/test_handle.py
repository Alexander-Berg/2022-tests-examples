from .conftest import API_KEY


def test_get_settings(on_demand_app):
    response = on_demand_app.request('/on-demand/companies/$/settings?apikey=' + API_KEY)
    assert response.status_code == 200

    json = response.json()
    assert json == {'quality': 'low'}
