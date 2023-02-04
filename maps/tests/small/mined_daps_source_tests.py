# coding=utf-8

import pytest

from maps.carparks.libs.geocoding.geocoding import Kind
from maps.carparks.tools.dap_snippets.lib.mined_daps_source import MinedDapsSource
from maps.carparks.tools.dap_snippets.lib.mined_daps_source import _COUNTRY_TO_CURRENCY
from maps.carparks.tools.dap_snippets.lib.daps_source import _TEXT_PRICE_BY_CURRENCY
import test_utils


_CLUSTER_JSON_TEMPLATE = '[{{"size": {size}, "center": {{"lat": {lat}, "lon": {lon}}}, "polygon": "{polygon}"}}]'
_DEFAULT_GEOMETRY_STRING = u'POLYGON ((0 0, 1 0, 0 1, 0 0))'
_DEFAULT_GEOMETRY_SNIPPET_PART = u'''
    <Geometry>
      <gml:Polygon>
        <gml:exterior>
          <gml:LinearRing>
            <gml:posList>0.0 0.0 1.0 0.0 0.0 1.0 0.0 0.0</gml:posList>
          </gml:LinearRing>
        </gml:exterior>
      </gml:Polygon>
    </Geometry>'''


# in all tests we use just these two keys:
_TOPONYM_KEY = {'is_organization': False, 'id': 42}
_ORGANIZATION_KEY = {'is_organization': True, 'id': 137}


def _make_carparks_info(org_id='', price='', isocode='null', type='null', id='null'):
    TEMPLATE = '{{"org_id": "{}", "price": "{}", "isocode": {}, "type": {}, "id": {}}}'
    return TEMPLATE.format(org_id, price, isocode, type, id)


def _make_price_xml(isocode='RU', value=0):
    currency = _COUNTRY_TO_CURRENCY[isocode]
    text = _TEXT_PRICE_BY_CURRENCY[currency].format(value)
    return u'''
    <Price>
      <value>{value}</value>
      <text>{text}</text>
      <currency>{currency}</currency>
    </Price>'''.format(value=value, text=text, currency=currency)


def input_row(patch_dict=None, reduce_key=_ORGANIZATION_KEY):
    _default_input_row = reduce_key.copy()
    _default_input_row.update({
        'timestamp': None,
        'address': None,
        'behind_barrier': False,
        'carpark_info': _make_carparks_info(),
        'cluster': 1,
        'geometry': _DEFAULT_GEOMETRY_STRING,
        'kind': None,
        'lat': 0,
        'lon': 0,
        'size': 1,
        'street_name': None,
        'target_lat': 0,
        'target_lon': 0,
        'type': None,
        'uuid': None,
        'wait_fraction': None
    })
    return test_utils.patched_row(
        template=_default_input_row,
        patch_dict=patch_dict)


def dap_value(dap_id,
              xml_geometry='',
              tags=('parking',),
              price=u'',
              anchor_pos=u'0.0 0.0',
              description=None):
    formatted_tags = '\n' + '\n'.join([
        '      <tag>{}</tag>'.format(tag) for tag in tags])
    description_string = u''
    if description is not None:
        description_string = u'\n    <description>{}</description>'.format(description)

    # TODO : we should use some function from xml_converter.py instead of this code
    return u'''<DrivingArrivalPoints xmlns:gml="http://www.opengis.net/gml" xmlns="http://maps.yandex.ru/snippets/driving_arrival_points/1.x">
  <DrivingArrivalPoint>
    <id>{id}</id>
    <Anchor>
      <gml:pos>{anchor}</gml:pos>
    </Anchor>{geometry}
    <WalkingTime>
      <value>0</value>
      <text>0 мин</text>
    </WalkingTime>{price}{description_string}
    <Tags>{tags}
    </Tags>
    <rating>1</rating>
  </DrivingArrivalPoint>
</DrivingArrivalPoints>
'''.format(id=dap_id,
           geometry=xml_geometry,
           tags=formatted_tags,
           price=price,
           anchor=anchor_pos,
           description_string=description_string)


def organization_result(
        id='137_0_',
        geometry='',
        tags=('parking',),
        price=u'',
        anchor=u'0.0 0.0',
        description=None):
    return {
        'address': None,
        'key': '137',
        'target': None,
        'source_type': 'mined',
        'kind': None,
        'is_organization': True,
        'error': None,
        'value': dap_value(dap_id=id,
                           xml_geometry=geometry,
                           tags=tags,
                           price=price,
                           anchor_pos=anchor,
                           description=description)
    }


def toponym_result(id, address=None, kind='house',
                   geometry='',
                   tags=('parking',), price=u'',
                   anchor=u'0.0 0.0',
                   description=None,
                   order_idx=0, is_major=True):
    snippet_id = '{}_{}'.format(id, order_idx)
    if is_major:
        snippet_id += '_'
    return {
        'key': 'geocoder_id_{}'.format(id),
        'target': None,
        'source_type': 'mined',
        'kind': kind,
        'address': address,
        'is_organization': False,
        'error': None,
        'value': dap_value(dap_id=snippet_id, xml_geometry=geometry,
                           tags=tags, price=price, anchor_pos=anchor, description=description)
    }


def run_reducer(reduce_key, input_rows):
    daps_source = MinedDapsSource(ytc=None,
                                  sources=None,
                                  load_mined_geometry=True)

    return list(daps_source.input_rows_to_snippets(reduce_key=reduce_key,
                                                   row_iterator=iter(input_rows)))


def test_simple_organization():
    result = run_reducer(
        reduce_key=_ORGANIZATION_KEY,
        input_rows=[input_row(patch_dict={
            'carpark_info': _make_carparks_info(price='2.5', isocode='"LV"'),
            'lat': 0,
            'lon': 1,
            'street_name': 'Street name',
            'target_lat': 0,
            'target_lon': 1,
            'type': 'road',
            'wait_fraction': 0,
        })])

    assert result == [organization_result(tags={'parking', 'toll', 'on_street'},
                                          price=_make_price_xml(isocode='LV', value=2.5),
                                          anchor='1.0 0.0', description='Street name')]


def test_simple_toponym():
    result = run_reducer(
        reduce_key=_TOPONYM_KEY,
        input_rows=[input_row(patch_dict={
            'address': 'A',
            'carpark_info': _make_carparks_info(price='0', type='1'),
            'kind': 'house',
            'lat': 0,
            'lon': 1,
            'street_name': 'Street name',
            'target_lat': 0,
            'target_lon': 1,
            'type': 'road',
        }, reduce_key=_TOPONYM_KEY)])

    assert result == [toponym_result(id=42, address='A', tags={'parking', 'free', 'on_street'},
                                     price=_make_price_xml(),
                                     anchor='1.0 0.0', description='Street name')]


class TestTags():
    def _run(self, row_patch):
        return run_reducer(reduce_key=_ORGANIZATION_KEY,
                           input_rows=[input_row(row_patch)])

    @pytest.mark.parametrize(
        ('row_patch', 'expected_tags'), [
            ({'type': 'road'}, {'parking', 'on_street'}),
            ({'type': 'yard'}, {'parking', 'yard'}),
            ({'type': 'mixed'}, {'parking', 'mixed'}),
            ({'type': 'area'}, {'parking', 'area'}),
            ({'type': 'bld'}, {'parking', 'building'}),
        ]
    )
    def test_row_type_produce_some_tags(self, row_patch, expected_tags):
        assert self._run(row_patch) == \
            [organization_result(tags=expected_tags)]

    @pytest.mark.parametrize(
        ('info_type', 'expected_id', 'expected_tags'), [
            ('1', '137_0_', {'parking', 'free'}),
            ('2', '137_0_', {'parking', 'toll'}),
            ('3', '137_0', {'parking', 'restricted'}),
            ('4', '137_0d', {'drop_off', 'prohibited'}),
            ('5', '137_0_', {'parking'}),
            ('6', '137_0_', {'parking', 'park_and_ride'}),
            ('7', '137_0_', {'parking', 'free'}),
            ('8', '137_0_', {'parking', 'toll'}),
            ('9', '137_0', {'parking', 'restricted'}),
            ('10', '137_0_', {'parking'}),
        ]
    )
    def test_carparks_info_type_produce_some_tags(self, info_type, expected_id, expected_tags):
        assert self._run(row_patch={'carpark_info': _make_carparks_info(type=info_type)}) == \
            [organization_result(id=expected_id, tags=expected_tags)]

    def test_behind_barrier_adds_behind_barrier_tag(self):
        assert (self._run(row_patch={'behind_barrier': True}) ==
                [organization_result(tags={'parking', 'behind_barrier'})])

    def test_non_zero_price_adds_toll_tag(self):
        assert (self._run(row_patch={'carpark_info': _make_carparks_info(price='100')}) ==
                [organization_result(tags={'parking', 'toll'}, price=_make_price_xml(value=100))])

    def test_zero_price_does_not_add_toll_tag(self):
        assert (self._run(row_patch={'carpark_info': _make_carparks_info(price='0')}) ==
                [organization_result(tags={'parking'}, price=_make_price_xml(value=0))])


class TestToponymAddress():
    def _run(self, row_patch):
        patched_row = input_row(row_patch, reduce_key=_TOPONYM_KEY)
        return run_reducer(reduce_key=_TOPONYM_KEY,
                           input_rows=[patched_row])

    def test_row_with_address_produces_result_with_address(self):
        results = self._run(row_patch={'address': 'A', 'kind': Kind.HOUSE})
        assert results == [toponym_result(id=42, address='A')]


class TestGeocoderKindsFilter():
    def _run(self, row_patch):
        patched_row = input_row(row_patch, reduce_key=_TOPONYM_KEY)
        return run_reducer(reduce_key=_TOPONYM_KEY,
                           input_rows=[patched_row])

    def test_allowed_kinds(self):
        allowed_kinds = [
            Kind.AIRPORT,
            Kind.VEGETATION,
            Kind.HOUSE,
            Kind.RAILWAY,
        ]
        for kind in allowed_kinds:
            results = self._run(row_patch={'kind': kind})
            assert results == [toponym_result(id=42, kind=kind)]

    def test_disallowed_kinds(self):
        disallowed_kinds = [
            Kind.COUNTRY,
            Kind.PROVINCE,
            Kind.AREA,
            Kind.LOCALITY,
            Kind.DISTRICT,
            Kind.HYDRO,
            Kind.OTHER,
            Kind.ROUTE,
            Kind.STREET,
            Kind.ENTRANCE,
            Kind.METRO,
        ]
        for kind in disallowed_kinds:
            results = self._run(row_patch={'kind': kind})
            assert results == []


def get_id_description_rating(input_rows):
    result = run_reducer(reduce_key=_ORGANIZATION_KEY,
                         input_rows=input_rows)
    return test_utils.get_id_description_rating(result)


class TestPopularIdAndRating():
    @pytest.mark.parametrize(
        ('size_a', 'size_b', 'expected'), [
            (70, 30, [('137_0_', 'a', '0.7'), ('137_1', 'b', '0.3')]),
            (30, 70, [('137_0_', 'b', '0.7'), ('137_1', 'a', '0.3')]),
        ]
    )
    def test_many_daps_with_a_popular_one(self, size_a, size_b, expected):
        assert get_id_description_rating([
            input_row({'street_name': 'a', 'size': size_a}),
            input_row({'street_name': 'b', 'size': size_b})]) == expected

    def test_one_dap(self):
        assert get_id_description_rating([input_row(
            {'street_name': 'a', 'size': 100})]) == [('137_0_', 'a', '1')]

    @pytest.mark.parametrize('restricted_type', ['3', '9'])
    def test_restricted_dap(self, restricted_type):
        assert get_id_description_rating([input_row({
            'street_name': 'a', 'size': 100,
            'carpark_info': _make_carparks_info(type=restricted_type)
            })]) == [('137_0', 'a', '1')]


class TestLargestAndFartherstCluster():
    @pytest.mark.parametrize(
        ('size_a', 'size_b', 'expected'), [
            (70, 30, [('137_0_', 'b', '0.7'), ('137_1', 'a', '0.3')]),
            (70, 15, [('137_0_', 'a', '0.82'), ('137_1', 'b', '0.18')]),
            (1000, 30, [('137_0_', 'a', '0.97'), ('137_1', 'b', '0.03')]),
        ]
    )
    def test_second_one_is_too_small(self, size_a, size_b, expected):
        # Depends on SECOND_CLUSTER_MIN_SIZE and MIN_CLUSTER_RELATIVE_SIZE
        assert get_id_description_rating([
            input_row({'street_name': 'a', 'size': size_a, 'lat': 0.001, 'lon': 0}),
            input_row({'street_name': 'b', 'size': size_b, 'lat': 0, 'lon': 0})
            ]) == expected

    @pytest.mark.parametrize(
        ('lat_a', 'lat_b', 'expected'), [
            # Depends on MIN_DISTANCE_TO_THE_LARGEST_CLUSTER
            (0.0005, 0, [('137_0_', 'b', '0.7'), ('137_1', 'a', '0.3')]),
            (0.0004, 0, [('137_0_', 'a', '0.7'), ('137_1', 'b', '0.3')]),
            # Depends on DISTANCE_RATIO_SIMILARITY_THRESHOLD
            (0.0016, 0.001, [('137_0_', 'b', '0.7'), ('137_1', 'a', '0.3')]),
            (0.0014, 0.001, [('137_0_', 'a', '0.7'), ('137_1', 'b', '0.3')]),
        ]
    )
    def test_distance_to_first_and_second_cluster(self, lat_a, lat_b, expected):
        assert get_id_description_rating([
            input_row({'street_name': 'a', 'size': 70, 'lat': lat_a, 'lon': 0}),
            input_row({'street_name': 'b', 'size': 30, 'lat': lat_b, 'lon': 0})
            ]) == expected

    @pytest.mark.parametrize(
        ('size_a', 'size_b', 'barrier_a', 'barrier_b', 'expected'), [
            (200, 30, True, True, [('137_0_', 'b', '0.87'), ('137_1', 'a', '0.13')]),
            (70, 30, False, True, [('137_0_', 'b', '0.7'), ('137_1', 'a', '0.3')]),
            (200, 30, False, True, [('137_0_', 'a', '0.87'), ('137_1', 'b', '0.13')]),
        ]
    )
    def test_changing_to_cluster_behind_barrier_is_allowed_only_when_it_is_big_enough(
            self, size_a, size_b, barrier_a, barrier_b, expected):
        # Depends on BARRIER_CLUSTER_MINIMUM_RATIO and MIN_CLUSTER_RELATIVE_SIZE
        assert get_id_description_rating([
            input_row({'street_name': 'a', 'size': size_a, 'lat': 0.001, 'lon': 0, 'behind_barrier': barrier_a}),
            input_row({'street_name': 'b', 'size': size_b, 'lat': 0, 'lon': 0, 'behind_barrier': barrier_b})
            ]) == expected

    def test_changing_main_cluster_with_area_type_is_not_allowed(self):
        # Depends on AREA_CARPARKS_TYPE
        assert (
            get_id_description_rating([
                input_row({'street_name': 'a', 'size': 70, 'lat': 0.001, 'lon': 0, 'type': 'area'}),
                input_row({'street_name': 'b', 'size': 30, 'lat': 0, 'lon': 0, 'type': 'area'})]) ==
            [('137_0_', 'b', '0.7'), ('137_1', 'a', '0.3')])
        assert (
            get_id_description_rating([
                input_row({'street_name': 'a', 'size': 70, 'lat': 0.001, 'lon': 0, 'type': 'area'}),
                input_row({'street_name': 'b', 'size': 30, 'lat': 0, 'lon': 0, 'type': 'street'})]) ==
            [('137_0_', 'a', '0.7'), ('137_1', 'b', '0.3')])

    def test_changing_yard_parking_to_street_is_not_allowed_if_yard_is_close_enough(self):
        # Depends on MAX_DISTANCE_TO_THE_YARD_CLUSTER
        assert (
            get_id_description_rating([
                input_row({'street_name': 'a', 'size': 70, 'lat': 0.0007, 'lon': 0, 'type': 'yard'}),
                input_row({'street_name': 'b', 'size': 30, 'lat': 0, 'lon': 0, 'type': 'street'})]) ==
            [('137_0_', 'b', '0.7'), ('137_1', 'a', '0.3')])
        assert (
            get_id_description_rating([
                input_row({'street_name': 'a', 'size': 70, 'lat': 0.0004, 'lon': 0, 'type': 'yard'}),
                input_row({'street_name': 'b', 'size': 30, 'lat': 0, 'lon': 0, 'type': 'street'})]) ==
            [('137_0_', 'a', '0.7'), ('137_1', 'b', '0.3')])

    def test_excluding_small_and_farthest_cluster_allows_second_cluster_to_win(self):
        # Depends on MIN_CLUSTER_RELATIVE_SIZE and CLUSTER_MIN_SIZE
        assert (
            get_id_description_rating([
                input_row({'street_name': 'a', 'size': 50, 'lat': 0.001, 'lon': 0}),
                input_row({'street_name': 'b', 'size': 40, 'lat': 0, 'lon': 0}),
                input_row({'street_name': 'c', 'size': 40, 'lat': 0.001, 'lon': 0.001})]) ==
            [('137_0_', 'a', '0.38'), ('137_1', 'b', '0.31'), ('137_2', 'c', '0.31')])
        assert (
            get_id_description_rating([
                input_row({'street_name': 'a', 'size': 200, 'lat': 0.001, 'lon': 0}),
                input_row({'street_name': 'b', 'size': 40, 'lat': 0, 'lon': 0}),
                input_row({'street_name': 'c', 'size': 25, 'lat': 0.001, 'lon': 0.001})]) ==
            [('137_0_', 'b', '0.75'), ('137_1', 'a', '0.15'), ('137_2', 'c', '0.09')])


class TestMarkMostPopular():
    def test_single_cluster_without_carpark_info(self):
        assert (
            get_id_description_rating([
                input_row({'street_name': 'a', 'size': 13})]) ==
            [('137_0_', 'a', '1')])

    def test_single_cluster_with_allowed_carpark_type(self):
        assert (
            get_id_description_rating([
                input_row({'street_name': 'a', 'size': 13,
                           'carpark_info': _make_carparks_info(type='1')})]) ==
            [('137_0_', 'a', '1')])

    def test_single_cluster_with_prohibited_carpark_type(self):
        assert (
            get_id_description_rating([
                input_row({'street_name': 'a', 'size': 13,
                           'carpark_info': _make_carparks_info(type='4')})]) ==
            [('137_0d', 'a', '1')])

    @pytest.mark.parametrize(
        ('size_a', 'size_b', 'type_a', 'type_b', 'expected', 'test_description'), [
            (40, 60, '1', '2', [('137_0_', 'b', '0.6'), ('137_1', 'a', '0.4')],
             'Two clusters with allowed carpark type'),
            (40, 60, '4', '4', [('137_0d', 'b', '0.6'), ('137_1', 'a', '0.4')],
             'Two clusters with prohibited carpark type'),
            (40, 60, '1', '4', [('137_0d', 'b', '0.6'), ('137_1_', 'a', '0.4')],
             'Big prohibited and small allowed clusters'),
            (40, 60, '4', '1', [('137_0_', 'b', '0.6'), ('137_1', 'a', '0.4')],
             'Big allowed and small prohibited clusters'),
            (50, 50, '4', '1', [('137_0d', 'a', '0.5'), ('137_1_', 'b', '0.5')],
             'Same rating allowed and prohibited clusters'),
        ]
    )
    def test_two_clusters_with_carpark_types(
            self, size_a, size_b, type_a, type_b, expected, test_description):
        assert get_id_description_rating(
            [
                input_row({'street_name': 'a', 'size': size_a,
                           'carpark_info': _make_carparks_info(type=type_a)}),
                input_row({'street_name': 'b', 'size': size_b,
                           'carpark_info': _make_carparks_info(type=type_b)}),
            ]) == expected, test_description

    def test_many_prohibited_and_small_allowed_clusters(self):
        assert (
            get_id_description_rating([
                input_row({'street_name': 'a', 'size': 50,
                           'carpark_info': _make_carparks_info(type='4')}),
                input_row({'street_name': 'b', 'size': 25,
                           'carpark_info': _make_carparks_info(type='4')}),
                input_row({'street_name': 'c', 'size': 15,
                           'carpark_info': _make_carparks_info(type='1')}),
                input_row({'street_name': 'd', 'size': 10,
                           'carpark_info': _make_carparks_info(type='4')})]) ==
            [('137_0d', 'a', '0.5'), ('137_1', 'b', '0.25'),
             ('137_2_', 'c', '0.15'), ('137_3', 'd', '0.1')])

    def test_many_allowed_and_prohibited_clusters_with_the_same_size(self):
        assert (
            get_id_description_rating([
                input_row({'street_name': 'a', 'size': 25,
                           'carpark_info': _make_carparks_info(type='1')}),
                input_row({'street_name': 'b', 'size': 25,
                           'carpark_info': _make_carparks_info(type='2')}),
                input_row({'street_name': 'c', 'size': 25,
                           'carpark_info': _make_carparks_info(type='4')}),
                input_row({'street_name': 'd', 'size': 25,
                           'carpark_info': _make_carparks_info(type='3')})]) ==
            [('137_0_', 'a', '0.25'), ('137_1', 'b', '0.25'),
             ('137_2d', 'c', '0.25'), ('137_3', 'd', '0.25')])


@pytest.mark.parametrize(
    ('input_', 'expected', 'test_description'), [
        ([], [], 'Empty input produce empty result'),
        ([input_row({'street_name': 'a', 'size': 100, 'lat': 0.0065, 'lon': 0})],
         [('137_0_', 'a', '1')], 'Close cluster is exported'),
        ([input_row({'street_name': 'a', 'size': 100, 'lat': 0.0070, 'lon': 0})],
         [], 'Far cluster is not exported'),
        ([input_row({'street_name': 'a', 'size': 100, 'lat': 0.0065, 'lon': 0}),
          input_row({'street_name': 'b', 'size': 100, 'lat': 0.0070, 'lon': 0})],
         [('137_0_', 'a', '0.5')], 'Only close cluster is exported')
    ]
)
def test_cluster_filtration(input_, expected, test_description):
    assert get_id_description_rating(input_) == expected, \
        'Test expectation: ' + test_description


@pytest.mark.parametrize(
    ('input_', 'size', 'test_description'), [
        ([input_row({'street_name': 'a', 'size': 100, 'lat': 0.0065, 'lon': 0})],
         1, 'Close cluster couse one yielded snippet'),
        ([input_row({'street_name': 'a', 'size': 100, 'lat': 0.0070, 'lon': 0})],
         0, 'Far cluster couse no snippets'),
        ([input_row({'street_name': 'a', 'size': 100, 'lat': 0.0065, 'lon': 0}),
          input_row({'street_name': 'b', 'size': 100, 'lat': 0.0070, 'lon': 0})],
         1, 'Close and far clusters couse one yielded snippet')
    ]
)
def test_reducer_yield_some_result(input_, size, test_description):
    daps_source = MinedDapsSource(ytc=None,
                                  sources=None,
                                  load_mined_geometry=True)
    snippets = list(daps_source.input_rows_to_snippets(
        reduce_key=_ORGANIZATION_KEY, row_iterator=iter(input_)))
    assert len(snippets) == size, test_description
