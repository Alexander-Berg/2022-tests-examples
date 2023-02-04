import json
import os
import werkzeug.exceptions

import pytest
from copy import deepcopy

from maps.b2bgeo.ya_courier.backend.test_lib.conftest import skip_if_remote
from maps.b2bgeo.ya_courier.backend.test_lib.util import source_path
from maps.b2bgeo.ya_courier.backend.test_lib.util_offline import Environment
from ya_courier_backend.logic.zones import ZoneTaskFilter
from ya_courier_backend.models import db, Zone
from ya_courier_backend.models.zone_relation import ZoneRelation, ZoneRelationType


NON_EXISTENT_NUMBER = "non-existent-number"
BIG_SQUARE_NUMBER = "big-square"
BIGGER_SQUARE_NUMBER = "private-bigger-square"
PUBLIC_BIGGER_SQUARE_NUMBER = "public_bigger-square"
TAGS_PUBLIC_BIGGER_SQUARE_NUMBER = "bigger-square"


def _add_new_zone(number, geojson, company_id=None):
    zone = Zone(number=number, polygon=geojson, company_id=company_id)
    db.session.add(zone)
    return zone


def _add_new_zone_relation(zone_1_number, zone_2_number, company_id):
    zones_query = db.session.query(Zone.id) \
        .filter(Zone.number.in_([zone_1_number, zone_2_number])) \
        .filter(Zone.company_id == company_id)
    zone_ids = [x[0] for x in zones_query.all()]

    zone_reltation = ZoneRelation(company_id=company_id,
                                  zone_ids=zone_ids,
                                  type=ZoneRelationType.incompatible)
    db.session.add(zone_reltation)
    return zone_reltation


@pytest.fixture()
def _app_context_with_zones(env: Environment):
    with env.flask_app.app_context():
        big_square = "[[37, 55], [38, 55], [38, 56], [37, 56], [37, 55]]"
        bigger_square = "[[36.5, 54.5], [38.5, 54.5], [38.5, 56.5], [36.5, 56.5], [36.5, 54.5]]"

        _add_new_zone(BIG_SQUARE_NUMBER, f'{{"coordinates": [{big_square}], "type": "Polygon"}}', env.default_company.id)
        _add_new_zone(BIGGER_SQUARE_NUMBER, f'{{"coordinates": [{bigger_square}], "type": "Polygon"}}', env.default_company.id)
        _add_new_zone(PUBLIC_BIGGER_SQUARE_NUMBER, f'{{"coordinates": [{bigger_square}], "type": "Polygon"}}')
        _add_new_zone(TAGS_PUBLIC_BIGGER_SQUARE_NUMBER, f'{{"coordinates": [{bigger_square}], "type": "Polygon"}}')
        db.session.commit()
        yield env


def _get_task():
    data_root = source_path("maps/b2bgeo/ya_courier/backend")
    with open(os.path.join(data_root, "bin/tests/data/example-mvrp.json"), "r") as f:
        return json.load(f)


def _add_zones(task, array, field, zones, idx=None):
    value = task[array]
    if idx is not None:
        value = value[idx]
    value[field] = deepcopy(zones)
    return task


def _add_zones_geojson(task, company_id):
    task = deepcopy(task)
    ZoneTaskFilter().preprocess_mvrp_task(task, company_id=company_id)
    return task


@skip_if_remote
def test_geojson_added_only_mentioned_zones(_app_context_with_zones):
    initial_task = _get_task()
    initial_task = _add_zones(initial_task, "vehicles", "allowed_zones", [BIG_SQUARE_NUMBER], 4)
    initial_task = _add_zones(initial_task, "options", "incompatible_zones", [[BIG_SQUARE_NUMBER]])

    task = _add_zones_geojson(initial_task, _app_context_with_zones.default_company.id)

    assert len(task["zones"]) == 1


@skip_if_remote
def test_zones_are_not_added_if_ignore_zones_is_true(_app_context_with_zones):
    initial_task = _get_task()
    initial_task = _add_zones(initial_task, "vehicles", "allowed_zones", [BIG_SQUARE_NUMBER], 4)
    initial_task = _add_zones(initial_task, "options", "incompatible_zones", [[BIG_SQUARE_NUMBER]])
    initial_task["options"]["ignore_zones"] = True

    task = _add_zones_geojson(initial_task, _app_context_with_zones.default_company.id)

    assert "zones" not in task


@skip_if_remote
def test_non_existent_zone_422(_app_context_with_zones):
    initial_task = _get_task()
    initial_task = _add_zones(initial_task, "vehicles", "allowed_zones", [BIG_SQUARE_NUMBER, PUBLIC_BIGGER_SQUARE_NUMBER, NON_EXISTENT_NUMBER], 0)
    initial_task = _add_zones(initial_task, "vehicles", "forbidden_zones", [NON_EXISTENT_NUMBER], -1)

    with pytest.raises(werkzeug.exceptions.UnprocessableEntity, match=f"Some of specified zones are not found in reference-book: \\['{NON_EXISTENT_NUMBER}'\\]"):
        _add_zones_geojson(initial_task, _app_context_with_zones.default_company.id)


@skip_if_remote
def test_invalid_tasks_are_ignored(_app_context_with_zones):
    # Right now it doesn't have all the needed checks, because it would be complicated to manually check all the types
    # We should validate the whole json schema in future in add/mvrp call

    valid_options = {"incompatible_zones": [[BIG_SQUARE_NUMBER, PUBLIC_BIGGER_SQUARE_NUMBER]]}
    valid_vehicles = [{"allowed_zones": [PUBLIC_BIGGER_SQUARE_NUMBER]}]

    initial_tasks = [
        {"test-test": "test"},
        {"options": valid_options, "vehicles": {}},
        {"options": [], "vehicles": valid_vehicles},
        {"vehicles": valid_vehicles},
        {"options": valid_options},
        {"options": {}, "vehicles": [{"allowed_zones": [PUBLIC_BIGGER_SQUARE_NUMBER]}, []]},
        # {"options": valid_options, "vehicles": {"allowed_zones": {"abc": "123"}}},
        # {"options": {"incompatible_zones": {"abc": "123"}}, "vehicles": valid_vehicles},
    ]

    tasks = [_add_zones_geojson(t, _app_context_with_zones.default_company.id) for t in initial_tasks]

    assert tasks == initial_tasks


@skip_if_remote
def test_do_not_query_tags_public_zones(_app_context_with_zones):
    initial_task = _get_task()
    initial_task = _add_zones(
        initial_task, "vehicles", "allowed_zones", [PUBLIC_BIGGER_SQUARE_NUMBER, TAGS_PUBLIC_BIGGER_SQUARE_NUMBER], 4)

    with pytest.raises(werkzeug.exceptions.UnprocessableEntity,
                       match=f"Some of specified zones are not found in reference-book: \\['{TAGS_PUBLIC_BIGGER_SQUARE_NUMBER}'\\]"):
        _add_zones_geojson(initial_task, _app_context_with_zones.default_company.id)


@skip_if_remote
def test_zones_with_relation_are_added_to_task(_app_context_with_zones):
    with _app_context_with_zones.flask_app.app_context():
        _add_new_zone_relation(BIG_SQUARE_NUMBER, BIGGER_SQUARE_NUMBER, _app_context_with_zones.default_company.id)
        db.session.commit()
    initial_task = _get_task()
    task = _add_zones_geojson(initial_task, _app_context_with_zones.default_company.id)

    assert len(task["zones"]) == 2
