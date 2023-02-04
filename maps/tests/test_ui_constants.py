import http.client

from maps.garden.server.lib.views.ui_constants import UI_CONSTANTS


def test_module_log_types(garden_client):
    response = garden_client.get("ui_constants/")
    assert response.status_code == http.client.OK
    assert response.get_json() == UI_CONSTANTS
