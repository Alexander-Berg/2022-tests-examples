from maps.wikimap.feedback.pushes.addresses.make_pushes.lib import choose_buildings_without_address as choose
from maps.wikimap.feedback.pushes.helpers import helpers
from nile.api.v1 import Record


def test_get_candidates_for_push_all_with_address():
    candidates = choose.get_candidates_for_push([
        Record(ds_lat=55., ds_lon=37., has_addr=True, distance=300.),
        Record(ds_lat=55., ds_lon=37., has_addr=True, distance=100.),
        Record(ds_lat=55., ds_lon=37., has_addr=True, distance=200.),
    ])
    assert candidates == []


def test_get_candidates_for_push_all_candidatesess():
    input_records = [
        Record(ds_lat=54., ds_lon=27., has_addr=False, distance=202.),
        Record(ds_lat=54., ds_lon=27., has_addr=False, distance=102.),
        Record(ds_lat=54., ds_lon=27., has_addr=False, distance=302.),
    ]
    helpers.compare_records_lists(
        choose.get_candidates_for_push(input_records),
        input_records
    )


def test_get_candidates_for_push():
    candidates = choose.get_candidates_for_push([
        Record(ds_lat=54., ds_lon=27., has_addr=False, distance=302.),
        Record(ds_lat=55., ds_lon=37., has_addr=True, distance=300.),
        Record(ds_lat=54., ds_lon=27., has_addr=False, distance=102.),
        Record(ds_lat=55., ds_lon=37., has_addr=True, distance=100.),
        Record(ds_lat=55., ds_lon=37., has_addr=True, distance=200.),
        Record(ds_lat=54., ds_lon=27., has_addr=False, distance=202.),
    ])
    helpers.compare_records_lists(
        candidates,
        [
            Record(ds_lat=54., ds_lon=27., has_addr=False, distance=302.),
            Record(ds_lat=54., ds_lon=27., has_addr=False, distance=102.),
            Record(ds_lat=54., ds_lon=27., has_addr=False, distance=202.),
        ]
    )


def test_choose_best_push():
    assert choose.choose_best_push([
        Record(has_addr=False, distance=80., bld_area=100.),
        Record(has_addr=False, distance=100., bld_area=100.),
        Record(has_addr=False, distance=110., bld_area=100.),
    ]) == Record(has_addr=False, distance=80., bld_area=100.)

    assert choose.choose_best_push([
        Record(has_addr=False, distance=8., bld_area=10.),
        Record(has_addr=False, distance=10., bld_area=200.),
        Record(has_addr=False, distance=11., bld_area=100.),
    ]) == Record(has_addr=False, distance=10., bld_area=200.)

    assert choose.choose_best_push([
        Record(has_addr=False, distance=8., bld_area=10.),
        Record(has_addr=False, distance=10., bld_area=200.),
        Record(has_addr=False, distance=11., bld_area=100.),
    ]) == Record(has_addr=False, distance=10., bld_area=200.)


def test_choose_best_building_without_address_reducer():
    groups = [
        (
            Record(uuid="1", ds_lat=54.0, ds_lon=27.0),
            [
                # the most relevant is record the nearest to the dwellplace
                Record(has_addr=False, distance=8., bld_area=10., uuid="1", ds_lat=54.0, ds_lon=27.0),
                Record(has_addr=False, distance=10., bld_area=10., uuid="1", ds_lat=54.0, ds_lon=27.0),
                Record(has_addr=False, distance=21., bld_area=10., uuid="1", ds_lat=54.0, ds_lon=27.0),
            ]

        ),
        (
            Record(uuid="2", ds_lat=54.0, ds_lon=27.0),
            [
                Record(has_addr=False, distance=8., bld_area=10., uuid="2", ds_lat=54.0, ds_lon=27.0),
                # the most relevant is record with big area
                Record(has_addr=False, distance=10., bld_area=200., uuid="2", ds_lat=54.0, ds_lon=27.0),
                Record(has_addr=False, distance=21., bld_area=100., uuid="2", ds_lat=54.0, ds_lon=27.0),
            ]

        ),
        (
            Record(uuid="3", ds_lat=54.0, ds_lon=27.0),
            [
                Record(has_addr=True, distance=16., bld_area=100., uuid="3", ds_lat=54.0, ds_lon=27.0),
                Record(has_addr=True, distance=14., bld_area=50., uuid="3", ds_lat=54.0, ds_lon=27.0),
                Record(has_addr=False, distance=10., bld_area=200., uuid="3", ds_lat=54.0, ds_lon=27.0),
                Record(has_addr=False, distance=8., bld_area=10., uuid="3", ds_lat=54.0, ds_lon=27.0),
                Record(has_addr=True, distance=12., bld_area=10., uuid="3", ds_lat=54.0, ds_lon=27.0),
                # the most relevant is record that is not very close to the dwellpalce, because
                # there is building with address nearby
                Record(has_addr=False, distance=21., bld_area=100., uuid="3", ds_lat=54.0, ds_lon=27.0),
            ]
        ),
        (
            Record(uuid="4", ds_lat=54.0, ds_lon=27.0),
            [
                Record(has_addr=True, distance=11., bld_area=11., uuid="4", ds_lat=54.0, ds_lon=27.0),
                Record(has_addr=True, distance=13., bld_area=13., uuid="4", ds_lat=54.0, ds_lon=27.0),
                Record(has_addr=True, distance=12., bld_area=12., uuid="4", ds_lat=54.0, ds_lon=27.0),
            ]

        ),
        (
            Record(uuid="5", ds_lat=54.0, ds_lon=27.0),
            [
                Record(has_addr=False, distance=13., bld_area=10., uuid="5", ds_lat=54.0, ds_lon=27.0),
            ]

        ),
    ]
    helpers.compare_records_lists(
        list(choose.choose_best_building_without_address_reducer(groups)),
        [
            Record(has_addr=False, distance=8., bld_area=10., uuid="1", ds_lat=54.0, ds_lon=27.0),
            Record(has_addr=False, distance=10., bld_area=200., uuid="2", ds_lat=54.0, ds_lon=27.0),
            Record(has_addr=False, distance=21., bld_area=100., uuid="3", ds_lat=54.0, ds_lon=27.0),
            Record(has_addr=False, distance=13., bld_area=10., uuid="5", ds_lat=54.0, ds_lon=27.0),
        ]
    )
