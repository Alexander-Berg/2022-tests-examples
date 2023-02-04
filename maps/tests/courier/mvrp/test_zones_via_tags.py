import json
import os

import pytest
from copy import deepcopy

from maps.b2bgeo.ya_courier.backend.test_lib.conftest import skip_if_remote
from maps.b2bgeo.ya_courier.backend.test_lib.util import source_path
from maps.b2bgeo.ya_courier.backend.test_lib.util_offline import Environment
from ya_courier_backend.logic.zones_via_tags import ZoneTagsTaskFilter, CHUNK_SIZE
from ya_courier_backend.models import db, Zone


NON_EXISTENT_NUMBER = "non-existent-number"
BIG_SQUARE_NUMBER = "big-square"
ANOTHER_BIG_SQUARE_NUMBER = "another-big-square"
LEFT_SMALL_SQUARE_NUMBER = "left-small-square"
RIGHT_SMALL_SQUARE_NUMBER = "right-small-square"
BIGGER_SQUARE_NUMBER = "bigger-square"
BIGGER_BIG_RING_NUMBER = "bigger-big-ring"
BIGGER_WITH_2_SMALL_HOLES_NUMBER = "bigger-with-2-small-holes"
NON_CONVEX_POLYGON_NUMBER = "non-convex-polygon"
INSIDE_TTK_NUMBER = "inside_ttk"
INSIDE_MKAD_NUMBER = "inside_mkad"


def _add_new_zone(number, geojson, company_id=None):
    zone = Zone(number=number, polygon=geojson, company_id=company_id)
    db.session.add(zone)
    return zone


@pytest.fixture()
def _app_context_with_zones(env: Environment):
    with env.flask_app.app_context():
        big_square = "[[37, 55], [38, 55], [38, 56], [37, 56], [37, 55]]"
        bigger_square = "[[36.5, 54.5], [38.5, 54.5], [38.5, 56.5], [36.5, 56.5], [36.5, 54.5]]"
        left_small_square = "[[37, 55.3], [37.45, 55.3], [37.45, 55.7], [37, 55.7], [37, 55.3]]"
        right_small_square = "[[37.55, 55.3], [38, 55.3], [38, 55.7], [37.55, 55.7], [37.55, 55.3]]"
        non_convex_polygon = (
            "[[37.486783, 55.831445], [37.578623, 55.855873], [37.451011, 55.686424], "
            "[37.728787, 55.645074], [37.463663, 55.648806], [37.451011, 55.686424], [37.486783, 55.831445]]"
        )

        _add_new_zone(BIG_SQUARE_NUMBER, f'{{"coordinates": [{big_square}], "type": "Polygon"}}')
        _add_new_zone(ANOTHER_BIG_SQUARE_NUMBER, f'{{"coordinates": [{big_square}], "type": "Polygon"}}', env.default_company.id)
        _add_new_zone(BIGGER_SQUARE_NUMBER, f'{{"coordinates": [{bigger_square}], "type": "Polygon"}}')
        _add_new_zone(LEFT_SMALL_SQUARE_NUMBER, f'{{"coordinates": [{left_small_square}], "type": "Polygon"}}')
        _add_new_zone(RIGHT_SMALL_SQUARE_NUMBER, f'{{"coordinates": [{right_small_square}], "type": "Polygon"}}')
        _add_new_zone(BIGGER_BIG_RING_NUMBER, f'{{"coordinates": [{bigger_square}, {big_square}], "type": "Polygon"}}')
        _add_new_zone(
            BIGGER_WITH_2_SMALL_HOLES_NUMBER,
            f'{{"coordinates": [{bigger_square}, {left_small_square}, {right_small_square}], "type": "Polygon"}}'
        )
        _add_new_zone(NON_CONVEX_POLYGON_NUMBER, f'{{"coordinates": [{non_convex_polygon}], "type": "Polygon"}}')
        db.session.commit()
        yield env


def _get_task():
    data_root = source_path("maps/b2bgeo/ya_courier/backend")
    with open(os.path.join(data_root, "bin/tests/data/example-mvrp.json"), "r") as f:
        return json.load(f)


def _add_tags(task, array, field, tags, idxs=None):
    for idx in idxs or [0]:
        task[array][idx][field] = deepcopy(tags)
    return task


def _add_zones(task, company_id):
    task = deepcopy(task)
    ZoneTagsTaskFilter().preprocess_mvrp_task(task, company_id=company_id)
    return task


@skip_if_remote
def test_zones_are_ignored_if_no_tags_are_present_in_vehicles(_app_context_with_zones):
    initial_task = _get_task()

    task = _add_zones(initial_task, _app_context_with_zones.default_company.id)

    assert task == initial_task


@skip_if_remote
def test_zones_are_matched_only_to_given_tags(_app_context_with_zones):
    initial_task = _get_task()
    initial_task = _add_tags(initial_task, "vehicles", "tags", [BIG_SQUARE_NUMBER], [4])

    task = _add_zones(initial_task, _app_context_with_zones.default_company.id)

    expected_location_tags = [[BIG_SQUARE_NUMBER]] * len(initial_task["locations"])
    location_tags = [location.get("required_tags") for location in task["locations"]]
    assert expected_location_tags == location_tags


@skip_if_remote
def test_zones_specified_manually_in_locations_are_ignored(_app_context_with_zones):
    initial_task = _get_task()
    initial_task = _add_tags(
        initial_task, "vehicles", "tags", [BIG_SQUARE_NUMBER, ANOTHER_BIG_SQUARE_NUMBER, BIGGER_SQUARE_NUMBER]
    )
    initial_task = _add_tags(initial_task, "locations", "required_tags", [BIG_SQUARE_NUMBER], [0, 1, 2])
    initial_task = _add_tags(
        initial_task, "locations", "optional_tags", [{"tag": ANOTHER_BIG_SQUARE_NUMBER, "value": 123}], [2]
    )

    task = _add_zones(initial_task, _app_context_with_zones.default_company.id)

    expected_location_tags = [[BIG_SQUARE_NUMBER, BIGGER_SQUARE_NUMBER]] * 3 + [[BIGGER_SQUARE_NUMBER]] * (
        len(initial_task["locations"]) - 3
    )
    location_tags = [location.get("required_tags") for location in task["locations"]]
    assert expected_location_tags == location_tags


@skip_if_remote
def test_non_existent_tag_does_not_affect_zones_placing(_app_context_with_zones):
    initial_task = _get_task()
    initial_task = _add_tags(initial_task, "vehicles", "tags", [BIG_SQUARE_NUMBER, BIGGER_SQUARE_NUMBER, NON_EXISTENT_NUMBER])
    initial_task = _add_tags(initial_task, "locations", "required_tags", [NON_EXISTENT_NUMBER], [-1])

    task = _add_zones(initial_task, _app_context_with_zones.default_company.id)

    expected_location_tags = [[BIG_SQUARE_NUMBER, BIGGER_SQUARE_NUMBER]] * (len(initial_task["locations"]) - 1) + [
        [NON_EXISTENT_NUMBER, BIG_SQUARE_NUMBER, BIGGER_SQUARE_NUMBER]
    ]
    location_tags = [location.get("required_tags") for location in task["locations"]]
    assert expected_location_tags == location_tags


@skip_if_remote
def test_zones_are_matched_with_more_than_chunk_size_locations(_app_context_with_zones):
    initial_task = _get_task()
    initial_task = _add_tags(initial_task, "vehicles", "tags", [BIG_SQUARE_NUMBER], [4])
    initial_task["locations"] = [
        deepcopy(initial_task["locations"][i % len(initial_task["locations"])]) for i in range(CHUNK_SIZE + 10)
    ]

    task = _add_zones(initial_task, _app_context_with_zones.default_company.id)

    expected_location_tags = [[BIG_SQUARE_NUMBER]] * len(initial_task["locations"])
    location_tags = [location.get("required_tags") for location in task["locations"]]
    assert expected_location_tags == location_tags


@skip_if_remote
def test_zone_with_inner_outline_is_matched_to_corresponding_locations(_app_context_with_zones):
    initial_task = _get_task()
    initial_task = _add_tags(initial_task, "vehicles", "tags", [BIGGER_BIG_RING_NUMBER])
    initial_task["locations"][-1]["point"] = {"lat": 56.2, "lon": 38.2}  # point on the ring
    initial_task["locations"][0]["point"] = {"lat": 56.6, "lon": 38.6}  # point outside of the ring

    task = _add_zones(initial_task, _app_context_with_zones.default_company.id)

    expected_location_tags = [None] * (len(initial_task["locations"]) - 1) + [[BIGGER_BIG_RING_NUMBER]]
    location_tags = [location.get("required_tags") for location in task["locations"]]
    assert expected_location_tags == location_tags


@skip_if_remote
def test_zone_with_two_inner_outlines_is_matched_to_corresponding_locations(_app_context_with_zones):
    initial_task = _get_task()
    initial_task = _add_tags(
        initial_task,
        "vehicles",
        "tags",
        [BIGGER_WITH_2_SMALL_HOLES_NUMBER, BIGGER_SQUARE_NUMBER, LEFT_SMALL_SQUARE_NUMBER, RIGHT_SMALL_SQUARE_NUMBER],
    )
    initial_task["locations"] = initial_task["locations"][:5]
    initial_task["locations"][0]["point"] = {"lat": 55.40, "lon": 37.2}  # point in the left small square
    initial_task["locations"][1]["point"] = {"lat": 55.40, "lon": 37.8}  # point in the right small square
    initial_task["locations"][2]["point"] = {"lat": 55.71, "lon": 37.2}  # point just outside of left square
    initial_task["locations"][3]["point"] = {"lat": 55.40, "lon": 37.5}  # point in between left and right square
    initial_task["locations"][4]["point"] = {"lat": 54.49, "lon": 37.8}  # point just outside of bigger square

    task = _add_zones(initial_task, _app_context_with_zones.default_company.id)

    expected_location_tags = [
        [BIGGER_SQUARE_NUMBER, LEFT_SMALL_SQUARE_NUMBER],
        [BIGGER_SQUARE_NUMBER, RIGHT_SMALL_SQUARE_NUMBER],
        [BIGGER_SQUARE_NUMBER, BIGGER_WITH_2_SMALL_HOLES_NUMBER],
        [BIGGER_SQUARE_NUMBER, BIGGER_WITH_2_SMALL_HOLES_NUMBER],
        None,
    ]
    location_tags = [location.get("required_tags") for location in task["locations"]]
    assert expected_location_tags == location_tags


@skip_if_remote
def test_non_convex_polygon_is_matched_to_corresponding_locations(_app_context_with_zones):
    initial_task = _get_task()
    initial_task = _add_tags(initial_task, "vehicles", "tags", [NON_CONVEX_POLYGON_NUMBER])
    initial_task["locations"] = initial_task["locations"][:8]

    task = _add_zones(initial_task, _app_context_with_zones.default_company.id)

    matched = [NON_CONVEX_POLYGON_NUMBER]
    expected_location_tags = [matched, matched, None, matched, matched, matched, None, matched]
    location_tags = [location.get("required_tags") for location in task["locations"]]
    assert expected_location_tags == location_tags


@skip_if_remote
def test_locations_matching_with_ttk_and_mkad_zones(_app_context_with_zones):
    initial_task = _get_task()
    initial_task = _add_tags(initial_task, "vehicles", "tags", [INSIDE_TTK_NUMBER], [5])
    initial_task = _add_tags(initial_task, "vehicles", "tags", [INSIDE_MKAD_NUMBER], [6])
    initial_task["locations"][0]["point"] = {"lat": 55.705788, "lon": 37.660347}  # point just inside of TTK
    initial_task["locations"][1]["point"] = {"lat": 55.704952, "lon": 37.662304}  # point just outside of TTK
    initial_task["locations"][2]["point"] = {"lat": 55.894926, "lon": 37.660519}  # point just inside of MKAD
    initial_task["locations"][3]["point"] = {"lat": 55.896202, "lon": 37.661748}  # point just outside of MKAD
    initial_task["locations"] = initial_task["locations"][:4]

    task = _add_zones(initial_task, _app_context_with_zones.default_company.id)

    expected_location_tags = [[INSIDE_TTK_NUMBER], [INSIDE_MKAD_NUMBER], [INSIDE_MKAD_NUMBER], None]
    location_tags = [location.get("required_tags") for location in task["locations"]]
    assert expected_location_tags == location_tags


@skip_if_remote
def test_invalid_tasks_are_ignored(_app_context_with_zones):
    # Right now it doesn't have all the needed checks, because it would be complicated to manually check all the types
    # We should validate the whole json schema in future in add/mvrp call
    initial_tasks = [
        {"test-test": "test"},
        {"locations": {}, "vehicles": {}},
        {"locations": [], "vehicles": []},
        {"vehicles": {"tags": [BIGGER_SQUARE_NUMBER]}},
        {"locations": [{"point": {"lat": 55.5, "wrong": 37.5}}], "vehicles": [{"tags": [BIGGER_SQUARE_NUMBER]}]},
        {"locations": [{"point": None}], "vehicles": [{"tags": [BIGGER_SQUARE_NUMBER]}]},
        {"locations": [{}], "vehicles": [{"tags": [BIGGER_SQUARE_NUMBER]}]},
        # {"locations": [{"point": {"lat": "wrong-type", "lon": 37.5}}], "vehicles": [{"tags": [BIGGER_SQUARE_NUMBER]}]},
        # {"locations": [{"required_tags": {}}], "vehicles": [{"tags": {}}]},
    ]

    tasks = [_add_zones(t, _app_context_with_zones.default_company.id) for t in initial_tasks]

    assert tasks == initial_tasks
