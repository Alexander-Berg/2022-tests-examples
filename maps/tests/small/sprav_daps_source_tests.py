# coding=utf-8

import unittest

from maps.carparks.tools.dap_snippets.lib.sprav_daps_source import \
    SpravDapsSource

import test_utils


def input_sprav_row(daps=(), is_exported=True):
    sprav_daps = [
        dict(
            {
                'anchor': {
                    'coordinates': dap.get('coordinates', [0, 1]),
                    'type': 'Point'
                },
                'description': [
                    {
                        'locale': 'ru',
                        'value': dap.get('description', '')
                    }
                ],
                'first_hour_fee': dap.get('first_hour_fee'),
                'geometry': dap.get('geometry'),
                'is_auto': dap.get('is_auto', False),
                'tags': dap.get('tags')
            },
            **({'rating': dap['rating']} if 'rating' in dap else {}))
        for dap in daps
    ]

    return {
        'permalink': 1,
        'is_exported': is_exported,
        'address': {
            'pos': {
                'coordinates': [0, 1],
                'type': 'Point'
            }
        },
        'driving_arrival_points': sprav_daps
    }


def run_mapper(input_row):
    daps_source = SpravDapsSource(ytc=None, sources=None)

    return list(daps_source.input_row_to_snippets(input_row))


def get_id_description_rating(input_row):
    result = run_mapper(input_row)
    return test_utils.get_id_description_rating(result)


class SimpleTests(unittest.TestCase):
    def test_simple_conversion(self):
        result = run_mapper(input_row={
            'permalink': 1,
            'is_exported': True,
            'address': {
                'pos': {
                    'coordinates': [0, 1],
                    'type': 'Point'
                }
            },
            'driving_arrival_points': [
                {
                    'anchor': {
                        'coordinates': [0.001, 1],
                        'type': 'Point'
                    },
                    'description': [
                        {
                            'locale': 'ru',
                            'value': 'Точка 1'
                        }
                    ],
                    'first_hour_fee': {
                        'unit': 10501,
                        'value': '200'
                    },
                    'is_auto': False,
                    'tags': [
                        'on_street'
                    ],
                    'walking_time': {
                        'unit': 3501523589,
                        'value': '100'
                    }
                },
                {
                    'anchor': {
                        'coordinates': [0.002, 1],
                        'type': 'Point'
                    },
                    'description': [
                        {
                            'locale': 'ru',
                            'value': 'Точка 2'
                        }
                    ],
                    'first_hour_fee': {
                        'unit': 3483777374,
                        'value': '10'
                    },
                    'is_auto': False,
                    'tags': [
                        'drop_off'
                    ]
                }
            ]
        })

        assert result == [{
            'source_type': 'sprav',
            'kind': None,
            'target': None,
            'key': '1',
            'is_organization': True,
            'error': None,
            'address': None,
            'value': u'''<DrivingArrivalPoints xmlns:gml="http://www.opengis.net/gml" xmlns="http://maps.yandex.ru/snippets/driving_arrival_points/1.x">
  <DrivingArrivalPoint>
    <id>1_0_</id>
    <Anchor>
      <gml:pos>0.001 1.0</gml:pos>
    </Anchor>
    <WalkingTime>
      <value>100</value>
      <text>1 мин</text>
    </WalkingTime>
    <Price>
      <value>200</value>
      <text>200 ₽</text>
      <currency>RUB</currency>
    </Price>
    <description>Точка 1</description>
    <Tags>
      <tag>on_street</tag>
      <tag>parking</tag>
      <tag>toll</tag>
    </Tags>
    <rating>0</rating>
  </DrivingArrivalPoint>
  <DrivingArrivalPoint>
    <id>1_1d</id>
    <Anchor>
      <gml:pos>0.002 1.0</gml:pos>
    </Anchor>
    <WalkingTime>
      <value>160</value>
      <text>3 мин</text>
    </WalkingTime>
    <Price>
      <value>10</value>
      <text>10 ₺</text>
      <currency>TRY</currency>
    </Price>
    <description>Точка 2</description>
    <Tags>
      <tag>drop_off</tag>
      <tag>toll</tag>
    </Tags>
    <rating>0</rating>
  </DrivingArrivalPoint>
</DrivingArrivalPoints>
'''
        }]


def organization_result(id=u'1_0_', geometry=u'', price=u'', tags=None):
    if tags is None:
        tags = u'''
    <Tags>
      <tag>parking</tag>
    </Tags>'''

    return {
        'source_type': 'sprav',
        'kind': None,
        'target': None,
        'key': '1',
        'is_organization': True,
        'error': None,
        'address': None,
        'value': u'''<DrivingArrivalPoints xmlns:gml="http://www.opengis.net/gml" xmlns="http://maps.yandex.ru/snippets/driving_arrival_points/1.x">
  <DrivingArrivalPoint>
    <id>{id}</id>
    <Anchor>
      <gml:pos>0.0 1.0</gml:pos>
    </Anchor>{geometry}
    <WalkingTime>
      <value>0</value>
      <text>0 мин</text>
    </WalkingTime>{price}{tags}
    <rating>0</rating>
  </DrivingArrivalPoint>
</DrivingArrivalPoints>
'''.format(id=id, geometry=geometry, price=price, tags=tags)
    }


class GeometryTests(unittest.TestCase):
    def test_single_point_is_not_converted(self):
        result = run_mapper(input_sprav_row([{
            'geometry': {
                'points': [
                    {
                        'coordinates': [0, 0],
                        'type': 'Point'
                    }
                ]
            }
        }]))

        assert result == [organization_result()]

    def test_polyline(self):
        result = run_mapper(input_sprav_row([{
            'geometry': {
                'points': [
                    {
                        'coordinates': [0, 0],
                        'type': 'Point'
                    },
                    {
                        'coordinates': [1, 1],
                        'type': 'Point'
                    }
                ]
            }
        }]))

        assert result == [organization_result(geometry=u'''
    <Geometry>
      <gml:LineString>
        <gml:posList>0.0 0.0 1.0 1.0</gml:posList>
      </gml:LineString>
    </Geometry>''')]


class ExportTests(unittest.TestCase):
    def test_no_snippet_is_produced_for_not_exported_organization(self):
        source = SpravDapsSource(ytc=None, sources=None)
        mapper = source.mapper
        assert (
            list(mapper(input_sprav_row(daps=[{'description': '1-1'}],
                                        is_exported=False))) == [])


class TagsTests(unittest.TestCase):
    def test_parking_is_added_for_tags_without_parking_and_drop_off(self):
        result = run_mapper(input_sprav_row([{
            'tags': [
                'on_street'
            ]
        }]))

        assert result == [organization_result(tags=u'''
    <Tags>
      <tag>on_street</tag>
      <tag>parking</tag>
    </Tags>''')]

    def test_parking_is_added_if_no_tags(self):
        result = run_mapper(input_sprav_row([{'tags': None}]))

        assert result == [organization_result(tags=u'''
    <Tags>
      <tag>parking</tag>
    </Tags>''')]

    def test_parking_is_not_added_if_tags_have_drop_off(self):
        result = run_mapper(input_sprav_row([{
            'tags': [
                'drop_off'
            ]
        }]))

        assert result == [organization_result(id=u'1_0d', tags=u'''
    <Tags>
      <tag>drop_off</tag>
    </Tags>''')]

    def test_toll_is_added_if_price_is_not_zero(self):
        result = run_mapper(input_sprav_row([{
            'first_hour_fee': {
                'unit': 10501,
                'value': '100'
            },
        }]))

        assert result == [organization_result(
            price=u'''
    <Price>
      <value>100</value>
      <text>100 ₽</text>
      <currency>RUB</currency>
    </Price>''',
            tags=u'''
    <Tags>
      <tag>parking</tag>
      <tag>toll</tag>
    </Tags>''')]

    def test_toll_is_not_added_if_price_is_zero(self):
        result = run_mapper(input_sprav_row([{
            'first_hour_fee': {
                'unit': 10501,
                'value': '0'
            },
        }]))

        assert result == [organization_result(
            price=u'''
    <Price>
      <value>0</value>
      <text>0 ₽</text>
      <currency>RUB</currency>
    </Price>''',
            tags=u'''
    <Tags>
      <tag>parking</tag>
    </Tags>''')]


class ManualPointsImportTests(unittest.TestCase):
    def test_only_manual_points_are_imported(self):
        assert (
            get_id_description_rating(
                input_sprav_row([{'description': '1-1', 'is_auto': True},
                                 {'description': '1-2', 'is_auto': False}])) ==
            [('1_0_', '1-2', '0')])

    def test_no_snippet_created_if_only_auto_points(self):
        assert (
            get_id_description_rating(
                input_sprav_row([{'description': '1-1', 'is_auto': True},
                                 {'description': '1-2', 'is_auto': True}])) == [])

    def test_no_snippet_created_if_only_one_auto_point(self):
        assert (
            get_id_description_rating(
                input_sprav_row([{'description': '1-1', 'is_auto': True}])) == [])


class ParkingAndDropOffMarkingTests(unittest.TestCase):
    def test_only_drop_off_daps(self):
        assert (
            get_id_description_rating(
                input_sprav_row([
                    {'description': '1-1', 'tags': ['drop_off'], 'rating': 0.7},
                    {'description': '1-2', 'tags': ['drop_off'], 'rating': 0.3},
                    ])) ==
            [('1_0d', '1-1', '0.7'), ('1_1', '1-2', '0.3')])

    def test_drop_off_dap_has_biggest_rating_and_we_set_both_marker_types(self):
        assert (
            get_id_description_rating(
                input_sprav_row([
                    {'description': '1-1', 'tags': ['drop_off'], 'rating': 0.7},
                    {'description': '1-2', 'tags': ['parking'], 'rating': 0.3},
                    ])) ==
            [('1_1_', '1-2', '0.3'), ('1_0d', '1-1', '0.7')])

    def test_drop_off_dap_with_biggest_rating_is_marked(self):
        assert (
            get_id_description_rating(
                input_sprav_row([
                    {'description': '1-1', 'tags': ['drop_off'], 'rating': 0.25},
                    {'description': '1-2', 'tags': ['drop_off'], 'rating': 0.45},
                    {'description': '1-3', 'tags': ['drop_off'], 'rating': 0.3},
                    ])) ==
            [('1_1d', '1-2', '0.45'), ('1_2', '1-3', '0.3'), ('1_0', '1-1', '0.25')])

    def test_parking_and_drop_off_daps_with_highest_rating_are_marked(self):
        assert (
            get_id_description_rating(
                input_sprav_row([
                    {'description': '1-1', 'tags': ['parking'], 'rating': 0.1},
                    {'description': '1-2', 'tags': ['drop_off'], 'rating': 0.45},
                    {'description': '1-3', 'tags': ['parking'], 'rating': 0.15},
                    {'description': '1-4', 'tags': ['drop_off'], 'rating': 0.3},
                    ])) ==
            [('1_2_', '1-3', '0.15'), ('1_0', '1-1', '0.1'),
             ('1_1d', '1-2', '0.45'), ('1_3', '1-4', '0.3')])

    def test_parking_and_drop_off_daps_with_same_rating(self):
        assert (
            get_id_description_rating(
                input_sprav_row([
                    {'description': '1-1', 'tags': ['parking'], 'rating': 1.00},
                    {'description': '1-2', 'tags': ['drop_off'], 'rating': 0.00},
                    {'description': '1-3', 'tags': ['parking'], 'rating': 0.00},
                    {'description': '1-4', 'tags': ['drop_off'], 'rating': 1.00},
                    ])) ==
            [('1_0_', '1-1', '1'), ('1_2', '1-3', '0'),
             ('1_3d', '1-4', '1'), ('1_1', '1-2', '0')])
