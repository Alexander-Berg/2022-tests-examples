import json
from http import HTTPStatus

import pytest
from copy import deepcopy

from maps.b2bgeo.ya_courier.backend.test_lib.conftest import skip_if_remote
from maps.b2bgeo.ya_courier.backend.test_lib.util_offline import Environment, local_get, local_post, local_patch, local_delete
from ya_courier_backend.models import db
from ya_courier_backend.models.zone import (
    ColorZoneException,
    PublicPrefixForCompanyZoneException,
    ForbiddenCharactersCompanyZoneException,
    InsideTtkMkadCompanyZoneException,
    GeoJsonTypeNotPolygonZoneException,
    PREFIX_PUBLIC_ZONE,
    FORBIDDEN_COMPANY_ZONE_NUMBERS,
    FORBIDDEN_COMPANY_ZONE_CHARACTERS,
    PublicZone,
    CompanyZone,
    Zone,
)

ORIGINAL_ZONE = \
    {
        "number": "test_company_zone",
        "color_fill": "#010101",
        "color_edge": "#020202",
        "polygon": {
            "coordinates": [[[0, 0], [0, 1], [1, 1], [1, 0], [0, 0]]],
            "type": "Polygon"
        },
    }


def _get_request_params(env: Environment, path: str, path_postfix: str):
    return {
        "client": env.client,
        "headers": env.user_auth_headers,
        "path": path if not path_postfix else f"{path}{path_postfix}",
    }


def _get_params_company_zone(env: Environment, path_postfix: str = ""):
    path = f"/api/v1/reference-book/companies/{env.default_company.id}/zones"
    return _get_request_params(env=env, path=path, path_postfix=path_postfix)


def _get_params_public_zone(env: Environment, path_postfix: str = ""):
    path = "/api/v1/reference-book/public/zones"
    return _get_request_params(env=env, path=path, path_postfix=path_postfix)


def _generate_new_zone(zone, idx):
    new_zone = deepcopy(zone)
    new_zone["number"] = f"{new_zone['number']}_{idx}"
    return new_zone


@skip_if_remote
def test_public_inside_ttk_mkad(env: Environment):  # remove_in_case_removing_inside_ttk_mkad
    """
    Just to check that we have created public_inside_ttk and public_inside_mkad
    For more info go to: 20210816232123_a89c4c53cfd1_modify_zone_table_for_public_n_company_zones.py
    REMOVE this test if you add new public zone or remove inside_ttk & inside_mkad
    """
    with env.flask_app.app_context():
        zones = db.session.query(Zone).all()
        public_zones = db.session.query(PublicZone).all()
        assert len(zones) / 2 == len(public_zones)


@skip_if_remote
def test_company_zone_create(env: Environment):
    public_zones = local_get(**_get_params_public_zone(env))
    local_post(**_get_params_company_zone(env), data=[ORIGINAL_ZONE])  # post_batch_init

    company_after_zones = local_get(**_get_params_company_zone(env))
    public_after_zones = local_get(**_get_params_public_zone(env))
    assert len(company_after_zones) == 1
    assert len(public_zones) == len(public_after_zones)
    company_zone_id = company_after_zones[0].pop("id")
    assert company_after_zones[0] == ORIGINAL_ZONE

    resp_get_single = local_get(**_get_params_company_zone(env, path_postfix=f"/{company_zone_id}"))
    assert resp_get_single.pop("id") == company_zone_id
    assert resp_get_single == ORIGINAL_ZONE


@pytest.mark.parametrize(
    argnames="forbidden_name",
    argvalues=FORBIDDEN_COMPANY_ZONE_NUMBERS,
)
@skip_if_remote
def test_company_zone_inside_ttk_or_mkad(env: Environment, forbidden_name):
    company_zone_inside_ttk = deepcopy(ORIGINAL_ZONE)
    company_zone_inside_ttk["number"] = forbidden_name
    resp_post_batch = local_post(
        **_get_params_company_zone(env),
        data=[company_zone_inside_ttk],
        expected_status=HTTPStatus.UNPROCESSABLE_ENTITY,
    )
    assert InsideTtkMkadCompanyZoneException.description in resp_post_batch['message']


@skip_if_remote
def test_public_zone_add_new_one(env: Environment):
    public_zones = local_get(**_get_params_public_zone(env))
    company_zones = local_get(**_get_params_company_zone(env))

    new_test_public_zone = PublicZone(
        number="public_new",
        color_fill="red",
        polygon=json.dumps(ORIGINAL_ZONE["polygon"])
    )
    with env.flask_app.app_context():
        db.session.add(new_test_public_zone)
        db.session.commit()

    public_after_zones = local_get(**_get_params_public_zone(env))
    company_after_zones = local_get(**_get_params_company_zone(env))

    assert len(public_zones) + 1 == len(public_after_zones)
    assert len(company_zones) == len(company_after_zones)


@pytest.mark.parametrize(
    argnames="miss_property",
    argvalues=["number", "polygon"],
)
@skip_if_remote
def test_company_zone_create_miss_required_number(env: Environment, miss_property: str):
    zone_no_number = deepcopy(ORIGINAL_ZONE)
    zone_no_number.pop(miss_property)

    resp_post_batch = local_post(
        **_get_params_company_zone(env),
        data=[zone_no_number],
        expected_status=HTTPStatus.UNPROCESSABLE_ENTITY,
    )
    assert "is a required property" in resp_post_batch['message']


@skip_if_remote
def test_company_zone_create_with_bad_prefix_number(env: Environment):
    zone_bad_prefix = deepcopy(ORIGINAL_ZONE)
    zone_bad_prefix["number"] = f"{PREFIX_PUBLIC_ZONE}{zone_bad_prefix['number']}"

    resp_post_batch = local_post(
        **_get_params_company_zone(env),
        data=[zone_bad_prefix],
        expected_status=HTTPStatus.UNPROCESSABLE_ENTITY,
    )
    assert PublicPrefixForCompanyZoneException.description in resp_post_batch['message']


@pytest.mark.parametrize(
    "character",
    list(FORBIDDEN_COMPANY_ZONE_CHARACTERS))
@skip_if_remote
def test_company_zone_create_with_illegal_characters_number(env: Environment, character):
    zone_bad_prefix = deepcopy(ORIGINAL_ZONE)
    zone_bad_prefix["number"] = f"Sepa{character} rated"

    resp_post_batch = local_post(
        **_get_params_company_zone(env),
        data=[zone_bad_prefix],
        expected_status=HTTPStatus.UNPROCESSABLE_ENTITY,
    )
    assert ForbiddenCharactersCompanyZoneException.description in resp_post_batch['message']


@skip_if_remote
def test_company_zone_create_with_bad_geojson_type(env: Environment):
    zone_bad_geojson_type = deepcopy(ORIGINAL_ZONE)
    zone_bad_geojson_type["polygon"] = {
        "coordinates": [[[0, 0], [0, 1], [1, 1], [1, 0], [0, 0]]],
        "type": "LineString"
    }
    resp_post_batch = local_post(
        **_get_params_company_zone(env),
        data=[zone_bad_geojson_type],
        expected_status=HTTPStatus.UNPROCESSABLE_ENTITY,
    )
    assert GeoJsonTypeNotPolygonZoneException.description in resp_post_batch['message']


@skip_if_remote
def test_company_zone_create_with_bad_polygon(env: Environment):
    zone_self_intersecting = deepcopy(ORIGINAL_ZONE)
    ring = zone_self_intersecting["polygon"]["coordinates"][0]
    ring[1], ring[2] = ring[2], ring[1]
    resp_post_batch = local_post(
        **_get_params_company_zone(env),
        data=[zone_self_intersecting],
        expected_status=HTTPStatus.UNPROCESSABLE_ENTITY,
    )
    assert "Geometry validation failed: Self-intersection at coordinate: 0.5 0.5" in resp_post_batch['message']


@pytest.mark.parametrize(
    argnames="bad_color_fill, msg",
    argvalues=[
        (True, "Json schema validation failed"),
        ("#foobar", ColorZoneException.description),
    ],
)
@skip_if_remote
def test_company_zone_create_with_bad_color_fill(env: Environment, bad_color_fill, msg):
    zone_bad_color_fill = deepcopy(ORIGINAL_ZONE)
    zone_bad_color_fill["color_fill"] = bad_color_fill
    resp_post_batch = local_post(
        **_get_params_company_zone(env),
        data=[zone_bad_color_fill],
        expected_status=HTTPStatus.UNPROCESSABLE_ENTITY,
    )
    assert msg in resp_post_batch['message']


@skip_if_remote
def test_company_zone_update_with_post(env: Environment):
    local_post(     # post_batch_init
        **_get_params_company_zone(env),
        data=[ORIGINAL_ZONE],
    )

    zone_update_by_post = deepcopy(ORIGINAL_ZONE)
    zone_update_by_post["color_fill"] = "#FFFFFF"

    zone_new_add_by_post = deepcopy(ORIGINAL_ZONE)
    zone_new_add_by_post["number"] = "yet_another_zone"

    local_post(     # post_batch_with_update
        **_get_params_company_zone(env),
        data=[zone_update_by_post, zone_new_add_by_post],
        expected_status=HTTPStatus.UNPROCESSABLE_ENTITY
    )


@skip_if_remote
def test_company_zone_update_with_post_bad_prefix(env: Environment):
    local_post(     # post_batch_init
        **_get_params_company_zone(env),
        data=[ORIGINAL_ZONE],
    )

    zone_update_by_post = deepcopy(ORIGINAL_ZONE)
    zone_update_by_post["color_fill"] = "#FFFFFF"
    zone_new_add_by_post = deepcopy(ORIGINAL_ZONE)
    zone_new_add_by_post["number"] = f"{PREFIX_PUBLIC_ZONE}yet_another_zone"
    local_post(     # post_batch_with_update
        **_get_params_company_zone(env),
        data=[zone_update_by_post, zone_new_add_by_post],
        expected_status=HTTPStatus.UNPROCESSABLE_ENTITY,
    )

    resp_get_batch = local_get(**_get_params_company_zone(env))
    assert len(resp_get_batch) == 1
    assert resp_get_batch[0]["color_fill"] == ORIGINAL_ZONE["color_fill"]


@skip_if_remote
def test_company_zone_patch_and_delete(env: Environment):
    local_post(   # post_batch_init
        **_get_params_company_zone(env),
        data=[ORIGINAL_ZONE],
    )
    resp_get_batch = local_get(**_get_params_company_zone(env))
    assert len(resp_get_batch) == 1
    company_zone = resp_get_batch[0]

    zone_update_by_patch = deepcopy(ORIGINAL_ZONE)
    zone_update_by_patch["number"] = "test_patched_zone"

    local_patch(
        **_get_params_company_zone(env, path_postfix=f"/{company_zone['id']}"),
        data=zone_update_by_patch,
    )
    resp_get_single = local_get(**_get_params_company_zone(env, path_postfix=f"/{company_zone['id']}"))
    assert resp_get_single["number"] == zone_update_by_patch["number"]
    assert resp_get_single["id"] == company_zone["id"]

    local_delete(**_get_params_company_zone(env, path_postfix=f"/{company_zone['id']}"))
    resp_get_batch = local_get(**_get_params_company_zone(env))
    assert len(resp_get_batch) == 0


@skip_if_remote
def test_public_zone_patch_and_delete_by_endpoints_of_company_zone(env: Environment):
    public_zones = local_get(**_get_params_public_zone(env))

    zone_for_patch = deepcopy(public_zones[0])
    zone_for_patch["number"] = "try_to_patch_public_zone"

    local_patch(
        **_get_params_company_zone(env, path_postfix=f"/{zone_for_patch['id']}"),
        data=zone_for_patch,
        expected_status=HTTPStatus.NOT_FOUND,
    )

    local_delete(
        **_get_params_company_zone(env, path_postfix=f"/{public_zones[0]['id']}"),
        expected_status=HTTPStatus.NOT_FOUND,
    )


@skip_if_remote
def test_company_zone_patch_bad_prefix(env: Environment):
    local_post(   # post_batch_init
        **_get_params_company_zone(env),
        data=[ORIGINAL_ZONE],
    )
    resp_get_batch = local_get(**_get_params_company_zone(env))
    assert len(resp_get_batch) == 1
    company_zone = resp_get_batch[0]

    zone_update_by_patch_bad_prefix = deepcopy(ORIGINAL_ZONE)
    zone_update_by_patch_bad_prefix["number"] = f"{PREFIX_PUBLIC_ZONE}test_patched_zone"

    local_patch(
        **_get_params_company_zone(env, path_postfix=f"/{company_zone['id']}"),
        data=zone_update_by_patch_bad_prefix,
        expected_status=HTTPStatus.UNPROCESSABLE_ENTITY,
    )
    resp_get_single = local_get(**_get_params_company_zone(env, path_postfix=f"/{company_zone['id']}"))
    assert resp_get_single["number"] == ORIGINAL_ZONE["number"]


@skip_if_remote
def test_company_zone_updated_at(env: Environment):
    local_post(
        **_get_params_company_zone(env),
        data=[ORIGINAL_ZONE],
    )
    with env.flask_app.app_context():
        zone_created = db.session.query(CompanyZone).one()

    zone_update_by_post = deepcopy(ORIGINAL_ZONE)
    zone_update_by_post["color_fill"] = "#121212"
    local_patch(
        **_get_params_company_zone(env, path_postfix=f"/{zone_created.id}"),
        data=zone_update_by_post,
    )
    with env.flask_app.app_context():
        zone_updated = db.session.query(CompanyZone).one()

    assert zone_updated.updated_at > zone_created.updated_at


@skip_if_remote
def test_company_zone_count_limit(env: Environment):
    for idx in range(3):
        zones = [_generate_new_zone(ORIGINAL_ZONE, idx) for idx in range(idx * 100, (idx + 1) * 100)]
        local_post(
            **_get_params_company_zone(env),
            data=zones
        )

    zones = [_generate_new_zone(ORIGINAL_ZONE, 300)]
    resp = local_post(
        **_get_params_company_zone(env),
        data=zones,
        expected_status=HTTPStatus.UNPROCESSABLE_ENTITY
    )
    assert "Exceeded limit on object count" in resp["message"], resp["message"]


@skip_if_remote
@pytest.mark.parametrize(
    'coordinates,error_substr',
    [([], 'minItems'), ([[1, 2, 3]], 'maxItems'), ([[]], 'minItems')])
def test_invalid_coordinates(env: Environment, coordinates, error_substr):
    zones = [
        {
            "polygon": {
                "coordinates": coordinates,
                "type": "Polygon"
            },
            "number": "test_company_zone"
        }
    ]
    resp = local_post(
        **_get_params_company_zone(env),
        data=zones,
        expected_status=HTTPStatus.UNPROCESSABLE_ENTITY
    )
    assert error_substr in resp["message"], resp["message"]


@skip_if_remote
@pytest.mark.parametrize(
    argnames="zone_patch_data",
    argvalues=[
        {"color_fill": "#010101"},
        {"color_edge": "#020202"},
        {"number": "new patched zone number"},
        {"polygon": {
            "coordinates": [[[0, 0], [0, 1], [1, 1], [1, 0], [0, 0]]],
            "type": "Polygon",
        }},
        {},
    ],
)
def test_company_zone_patch_different_fields(env: Environment, zone_patch_data):
    local_post(
        **_get_params_company_zone(env),
        data=[ORIGINAL_ZONE],
    )
    resp_get_batch = local_get(**_get_params_company_zone(env))
    assert len(resp_get_batch) == 1
    company_zone = resp_get_batch[0]

    local_patch(
        **_get_params_company_zone(env, path_postfix=f"/{company_zone['id']}"),
        data=zone_patch_data,
        expected_status=HTTPStatus.OK,
    )
    resp_get_single = local_get(**_get_params_company_zone(env, path_postfix=f"/{company_zone['id']}"))
    for key, value in zone_patch_data.items():
        assert resp_get_single[key] == value
