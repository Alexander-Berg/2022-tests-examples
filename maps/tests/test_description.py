# -*- coding: utf-8 -*-
from itertools import chain

from maps.garden.modules.export_cams.lib.extract import (
    generate_description, TURKEY, RUSSIA, TAGS_PRIORITY, TYPE_TO_TAGS)


def test_tags_priority_recall():
    assert set(TAGS_PRIORITY) == set(chain(*TYPE_TO_TAGS.values()))


def test_police_tag_only():
    assert (generate_description(["police"], TURKEY)
            == "Polis Kontrol Noktası")
    assert (generate_description(["police"], RUSSIA)
            == "Пост ДПС")


def test_single_tag():
    assert (generate_description(["cross_road_control", "police"], TURKEY)
            == "Kamera (Kaynak: www.trafik.gov.tr)")
    assert (generate_description(["no_stopping_control", "police"], TURKEY)
            == "Kamera (Kaynak: www.trafik.gov.tr)")
    assert (generate_description(["road_marking_control", "police"], TURKEY)
            == "Kamera (Kaynak: www.trafik.gov.tr)")

    assert (generate_description(["cross_road_control", "police"], RUSSIA)
            == "Камера контроля проезда перекрёстка")
    assert (generate_description(["no_stopping_control", "police"], RUSSIA)
            == "Камера контроля остановки")
    assert (generate_description(["road_marking_control", "police"], RUSSIA)
            == "Камера контроля разметки")


def test_two_tags_with_police():
    assert (generate_description(["speed_control", "police"], TURKEY)
            == "Hız Kamerası (Kaynak: www.trafik.gov.tr)")
    assert (generate_description(["speed_control", "police"], RUSSIA)
            == "Камера контроля скорости")
    assert (generate_description(["lane_control", "police"], TURKEY)
            == "EDS (Kaynak: www.trafik.gov.tr)")
    assert (generate_description(["lane_control", "police"], RUSSIA)
            == "Камера контроля полосы")


def test_several_tags_with_police():
    assert (generate_description(["speed_control", "police",
                                  "lane_control", "police"],
                                 TURKEY)
            == "Hız Kamerası, EDS (Kaynak: www.trafik.gov.tr)")
    assert (generate_description(["speed_control", "police",
                                  "lane_control", "police"],
                                 RUSSIA)
            == "Камера контроля скорости, полосы")


def test_several_tags_priority():
    assert (generate_description(["lane_control", "police",
                                  "speed_control", "police"],
                                 TURKEY)
            == "Hız Kamerası, EDS (Kaynak: www.trafik.gov.tr)")
    assert (generate_description(["police", "lane_control",
                                  "speed_control", "police"],
                                 RUSSIA)
            == "Камера контроля скорости, полосы")
    assert (generate_description(["road_marking_control", "cross_road_control",
                                  "no_stopping_control", "police"],
                                 RUSSIA)
            == "Камера контроля разметки, проезда перекрёстка, остановки")
    assert (generate_description(["no_stopping_control", "cross_road_control",
                                  "road_marking_control", "police"],
                                 RUSSIA)
            == "Камера контроля разметки, проезда перекрёстка, остановки")
    assert (generate_description(["cross_road_control", "no_stopping_control",
                                  "road_marking_control", "police"],
                                 RUSSIA)
            == "Камера контроля разметки, проезда перекрёстка, остановки")


def test_several_tags_with_incomplete_translation():
    assert (generate_description(["no_stopping_control", "road_marking_control",
                                  "police"],
                                 TURKEY)
            == "Kamera (Kaynak: www.trafik.gov.tr)")
    assert (generate_description(["speed_control", "police",
                                  "road_marking_control"],
                                 TURKEY)
            == "Hız Kamerası (Kaynak: www.trafik.gov.tr)")


def test_all_tags():
    assert (generate_description(["speed_control", "lane_control",
                                  "road_marking_control", "cross_road_control",
                                  "no_stopping_control", "police", ],
                                 TURKEY)
            == "Hız Kamerası, EDS (Kaynak: www.trafik.gov.tr)")
    assert (generate_description(["speed_control", "lane_control",
                                  "road_marking_control", "cross_road_control",
                                  "no_stopping_control", "police", ],
                                 RUSSIA)
            == "Камера контроля скорости, полосы, разметки, проезда перекрёстка, остановки")
