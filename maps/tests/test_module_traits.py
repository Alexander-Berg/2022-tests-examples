import http.client

from maps.garden.sdk.module_traits import module_traits


def test_empty_traits(garden_client):
    response = garden_client.get("modules/unknown_module/traits/")
    assert response.status_code == http.client.NOT_FOUND


def test_non_empty_traits(garden_client, module_helper):
    traits = module_traits.ModuleTraits(
        name="denormalization",
        type=module_traits.ModuleType.MAP,
        sources=["ymapsdf"]
    )
    module_helper.add_module_to_system_contour(traits)

    response = garden_client.get("modules/denormalization/traits/")
    assert response.status_code == http.client.OK
    assert module_traits.ModuleTraits.parse_obj(response.get_json()) == traits
