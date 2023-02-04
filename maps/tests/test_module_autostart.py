import http.client

from maps.garden.sdk.module_traits.module_traits import ModuleTraits, ModuleType


def test_autostart_manipulation(garden_client, module_helper):
    traits = ModuleTraits(
        name="ymapsdf",
        type=ModuleType.MAP,
    )
    module_helper.add_module_to_system_contour(traits)

    response = garden_client.get("modules/ymapsdf/")
    assert response.get_json()["autostart"]["enabled"]

    response = garden_client.post("modules/ymapsdf/disable-autostart/?contour=unittest")
    assert response.status_code == http.client.OK
    response = garden_client.get("modules/ymapsdf/")
    assert not response.get_json()["autostart"]["enabled"]

    response = garden_client.post("modules/ymapsdf/disable-autostart/?contour=unittest")
    assert response.status_code == http.client.OK
    assert not response.get_json()["enabled"]
    response = garden_client.get("modules/ymapsdf/")
    assert not response.get_json()["autostart"]["enabled"]

    response = garden_client.post("modules/ymapsdf/enable-autostart/?contour=unittest")
    assert response.status_code == http.client.OK
    assert response.get_json()["enabled"]
    response = garden_client.get("modules/ymapsdf/")
    assert response.get_json()["autostart"]["enabled"]
