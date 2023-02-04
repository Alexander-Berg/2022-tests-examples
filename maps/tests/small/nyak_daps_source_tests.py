# coding=utf-8

import unittest

from maps.carparks.tools.dap_snippets.lib.nyak_daps_source import \
    NyakDapsSource

import test_utils
from test_utils import ewkb_hex_point


def input_nyak_row(**kwargs):
    return test_utils.patched_row(
        template={
            'key': 'geocoder_id_1',
            'geocoder_id': 1,
            'ft_id': 11,
            'name': 'Name',
            'anchor': ewkb_hex_point(0, 0),
            'tags': '["parking", "toll"]',
            'lang': 'ru',
            'target': ewkb_hex_point(0, 0),
            'is_major': False
        },
        patch_dict=kwargs)


def run_reducer(reduce_key, input_rows):
    daps_source = NyakDapsSource(ytc=None, sources=None)

    return list(daps_source.input_rows_to_snippets(
        reduce_key=reduce_key,
        rows=iter(input_rows)))


def _get_id_description_rating(input_rows):
    result = run_reducer(reduce_key={'key': '1'},
                            input_rows=input_rows)
    return test_utils.get_id_description_rating(result)


class SimpleTests(unittest.TestCase):
    def test_simple_conversion(self):
        result = run_reducer(
            reduce_key={'key': 'geocoder_id_1'},
            input_rows=[
                input_nyak_row(
                    name='Name 1',
                    ft_id=11,
                    anchor=ewkb_hex_point(0.0001, 0),
                    tags='["parking", "toll"]'),
                input_nyak_row(
                    name='Name 2',
                    ft_id=12,
                    anchor=ewkb_hex_point(0, 0.0002),
                    tags='["drop_off"]'),
            ])

        assert result == [{
            'address': None,
            'source_type': 'nyak',
            'kind': None,
            'target': None,
            'key': 'geocoder_id_1',
            'is_organization': False,
            'error': None,
            'value': u'''<DrivingArrivalPoints xmlns:gml="http://www.opengis.net/gml" xmlns="http://maps.yandex.ru/snippets/driving_arrival_points/1.x">
  <DrivingArrivalPoint>
    <id>1_0_</id>
    <Anchor>
      <gml:pos>0.0001 0.0</gml:pos>
    </Anchor>
    <WalkingTime>
      <value>8</value>
      <text>1 мин</text>
    </WalkingTime>
    <description>Name 1</description>
    <Tags>
      <tag>parking</tag>
      <tag>toll</tag>
    </Tags>
    <rating>1</rating>
  </DrivingArrivalPoint>
  <DrivingArrivalPoint>
    <id>1_1d</id>
    <Anchor>
      <gml:pos>0.0 0.0002</gml:pos>
    </Anchor>
    <WalkingTime>
      <value>15</value>
      <text>1 мин</text>
    </WalkingTime>
    <description>Name 2</description>
    <Tags>
      <tag>drop_off</tag>
    </Tags>
    <rating>1</rating>
  </DrivingArrivalPoint>
</DrivingArrivalPoints>
'''
        }]


class ParkingMarkingTests(unittest.TestCase):
    def test_single_major_parking_is_marked(self):
        assert (
            _get_id_description_rating([
                input_nyak_row(ft_id=1, name='1-1', tags='["parking"]', is_major=True)]) ==
            [('1_0_', '1-1', '1')])

    def test_single_regular_parking_is_marked(self):
        assert (
            _get_id_description_rating([
                input_nyak_row(ft_id=1, name='1-1', tags='["parking"]', is_major=False)]) ==
            [('1_0_', '1-1', '1')])

    def test_one_major_parking_is_marked(self):
        assert (
            _get_id_description_rating([
                input_nyak_row(ft_id=1, anchor=ewkb_hex_point(0, 0), name='1-1',
                               tags='["parking"]', is_major=False),
                input_nyak_row(ft_id=2, anchor=ewkb_hex_point(1, 1), name='1-2',
                               tags='["parking"]', is_major=False),
                input_nyak_row(ft_id=3, anchor=ewkb_hex_point(2, 2), name='1-3',
                               tags='["parking"]', is_major=True)]) ==
            [('1_2_', '1-3', '1'), ('1_0', '1-1', '0'), ('1_1', '1-2', '0')])


class DropOffMarkingTests(unittest.TestCase):
    def test_single_major_drop_off_is_marked(self):
        assert (
            _get_id_description_rating([
                input_nyak_row(ft_id=1, name='1-1', tags='["drop_off"]', is_major=True)]) ==
            [('1_0d', '1-1', '1')])

    def test_single_regular_drop_off_is_marked(self):
        assert (
            _get_id_description_rating([
                input_nyak_row(ft_id=1, name='1-1', tags='["drop_off"]', is_major=False)]) ==
            [('1_0d', '1-1', '1')])

    def test_one_major_drop_off_is_marked(self):
        assert (
            _get_id_description_rating([
                input_nyak_row(ft_id=1, anchor=ewkb_hex_point(0, 0), name='1-1',
                               tags='["drop_off"]', is_major=False),
                input_nyak_row(ft_id=2, anchor=ewkb_hex_point(1, 1), name='1-2',
                               tags='["drop_off"]', is_major=False),
                input_nyak_row(ft_id=3, anchor=ewkb_hex_point(2, 2), name='1-3',
                               tags='["drop_off"]', is_major=True)]) ==
            [('1_2d', '1-3', '1'), ('1_0', '1-1', '0'), ('1_1', '1-2', '0')])


class ParkingAndDropOffMixMarkingTests(unittest.TestCase):
    def test_no_labels(self):
        assert (
            _get_id_description_rating([
                input_nyak_row(ft_id=1, anchor=ewkb_hex_point(0, 0), name='1-1',
                               tags='["parking"]', is_major=False),
                input_nyak_row(ft_id=2, anchor=ewkb_hex_point(1, 1), name='1-2',
                               tags='["drop_off"]', is_major=False)]) ==
            [('1_0_', '1-1', '1'), ('1_1d', '1-2', '1')])

    def test_major_parking_and_drop_off_without_label(self):
        assert (
            _get_id_description_rating([
                input_nyak_row(ft_id=1, anchor=ewkb_hex_point(0, 0), name='1-1',
                               tags='["parking"]', is_major=True),
                input_nyak_row(ft_id=2, anchor=ewkb_hex_point(1, 1), name='1-2',
                               tags='["parking"]', is_major=False),
                input_nyak_row(ft_id=3, anchor=ewkb_hex_point(2, 2), name='1-3',
                               tags='["drop_off"]', is_major=False)]) ==
            [('1_0_', '1-1', '1'), ('1_2d', '1-3', '1'), ('1_1', '1-2', '0')])

    def test_major_drop_off_and_parking_without_label(self):
        assert (
            _get_id_description_rating([
                input_nyak_row(ft_id=1, anchor=ewkb_hex_point(0, 0), name='1-1',
                               tags='["parking"]', is_major=False),
                input_nyak_row(ft_id=2, anchor=ewkb_hex_point(1, 1), name='1-2',
                               tags='["drop_off"]', is_major=False),
                input_nyak_row(ft_id=3, anchor=ewkb_hex_point(2, 2), name='1-3',
                               tags='["drop_off"]', is_major=True)]) ==
            [('1_0_', '1-1', '1'), ('1_2d', '1-3', '1'), ('1_1', '1-2', '0')])

    def test_parking_and_drop_off_have_major_label(self):
        assert (
            _get_id_description_rating([
                input_nyak_row(ft_id=1, anchor=ewkb_hex_point(0, 0), name='1-1',
                               tags='["parking"]', is_major=True),
                input_nyak_row(ft_id=2, anchor=ewkb_hex_point(1, 1), name='1-2',
                               tags='["parking"]', is_major=False),
                input_nyak_row(ft_id=3, anchor=ewkb_hex_point(2, 2), name='1-3',
                               tags='["drop_off"]', is_major=False),
                input_nyak_row(ft_id=4, anchor=ewkb_hex_point(3, 3), name='1-4',
                               tags='["drop_off"]', is_major=True)]) ==
            [('1_0_', '1-1', '1'), ('1_3d', '1-4', '1'),
             ('1_1', '1-2', '0'), ('1_2', '1-3', '0')])


class ManyLanguagesTests(unittest.TestCase):
    # We consider daps with the same anchor are the same with
    # different tranclations. So, we keep only first of them.
    def test_no_labels(self):
        assert (
            _get_id_description_rating([
                input_nyak_row(ft_id=1, anchor=ewkb_hex_point(0, 0), name='eng',
                               tags='["parking"]', is_major=False),
                input_nyak_row(ft_id=1, anchor=ewkb_hex_point(0, 0), name='rus',
                               tags='["parking"]', is_major=False)]) ==
            [('1_0_', 'eng', '1')])
