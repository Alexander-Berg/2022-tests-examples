# -*- coding: utf-8 -*-
from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

from future import standard_library

standard_library.install_aliases()

import pytest
import mock
import http.client as http
import hamcrest as hm

from yb_snout_api.utils import clean_dict
from yb_snout_api.tests_unit.base import TestCaseApiAppBase


class Region(object):
    def __init__(self, id, name, en_name):
        self.id = id
        self.name = name
        self.ename = en_name


class Geolookup(mock.MagicMock):
    def regions_by_type(self, type_):
        return [
            Region(1, 'Россия', 'Russia'),
            Region(2, 'Исландия', 'Iceland'),
            Region(3, 'Япония', 'Japan'),
        ]


class TestCaseRegions(TestCaseApiAppBase):
    BASE_API = '/v1/geo/regions'

    @pytest.mark.parametrize(
        'lang',
        ['ru', 'en', None],
    )
    @mock.patch('yb_snout_api.utils.plugins.get_geolookup', return_value=Geolookup())
    def test_get(self, _mock_geolookup, lang):
        from yb_snout_api.resources.v1.geo.enums import RegionType

        response = self.test_client.get(
            self.BASE_API,
            params=clean_dict({'lang': lang, 'region_type': RegionType.COUNTRIES.name}),
            is_admin=False,
        )
        hm.assert_that(response.status_code, hm.equal_to(http.OK))

        data = response.get_json()['data']
        region_match = {}
        if lang in [None, 'ru']:
            region_match['ru'] = hm.contains(
                {'id': 2, 'name': 'Исландия'},
                {'id': 1, 'name': 'Россия'},
                {'id': 3, 'name': 'Япония'},
            )
        if lang in [None, 'en']:
            region_match['en'] = hm.contains(
                {'id': 2, 'name': 'Iceland'},
                {'id': 3, 'name': 'Japan'},
                {'id': 1, 'name': 'Russia'},
            )
        hm.assert_that(
            data,
            hm.has_entries(region_match),
        )
