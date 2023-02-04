# coding: utf-8
import json

from ads.quality.phf.phf_direct_loader.tests.test_helpers import (
    BaseTestCase,
)

from ads.quality.phf.phf_direct_loader.lib.modules.regions.controllers import (
    RegionManager,
    DISTRICT_REGION,
    CITY_REGION
)
from ads.quality.phf.phf_direct_loader.tests.test_helpers import TEST_REGIONS


class TestRegionManager(BaseTestCase):
    def test_response_with_region_expand(self):
        query_regions = [r['direct_id'] for r in TEST_REGIONS
                         if r['region_type'] == DISTRICT_REGION and r['country'] == u'Россия']
        assert query_regions
        region_manager = RegionManager()
        regions = region_manager.get_regions(query_regions, region_manager.REGION_EXPAND)

        assert set(query_regions) == set(r.direct_id for r in regions)

    def test_response_with_region_city(self):
        query_regions = [r['direct_id'] for r in TEST_REGIONS
                         if r['region_type'] == DISTRICT_REGION and r['country'] == u'Россия']
        assert query_regions

        expected_regions = set([r['direct_id'] for r in TEST_REGIONS
                                if r['region_type'] == CITY_REGION and r['country'] == u'Россия'])
        region_manager = RegionManager()
        regions = region_manager.get_regions(query_regions, region_manager.CITY_EXPAND)

        assert expected_regions == set(r.direct_id for r in regions)

    def test_country_info_countries_correct(self):
        expected_countries = set(r['country'] for r in TEST_REGIONS)
        country_info_result = json.loads(self.client.get("regions/countries").data)
        assert set(cf['name'] for cf in country_info_result) == expected_countries

    def test_country_info_regions_correct(self):
        expected_regions = {}
        for r in TEST_REGIONS:
            if r['country'] not in expected_regions:
                expected_regions[r['country']] = []
            if r['region_type'] == DISTRICT_REGION:
                expected_regions[r['country']].append(r['direct_id'])

        country_info_result = json.loads(self.client.get("regions/countries").data)
        response_regions = {cf['name']: [r['direct_id'] for r in cf['regions']] for cf in country_info_result}

        assert response_regions == expected_regions

    def test_expand_types(self):
        country_expand_types = {}

        for r in TEST_REGIONS:
            if r['country'] not in country_expand_types:
                country_expand_types[r['country']] = set()
            if r['region_type'] == CITY_REGION:
                country_expand_types[r['country']].add(RegionManager.CITY_EXPAND)
            if r['is_capital']:
                country_expand_types[r['country']].add(RegionManager.CAPITAL_EXPAND)

        region_manager = RegionManager()
        expected_expand_types = {country: [
            {'name': region_manager.REGION_EXPAND, 'available': True},
            {'name': region_manager.CITY_EXPAND, 'available': region_manager.CITY_EXPAND in expand_set},
            {'name': region_manager.CAPITAL_EXPAND, 'available': region_manager.CAPITAL_EXPAND in expand_set}
        ] for country, expand_set in country_expand_types.iteritems()}

        country_info_result = json.loads(self.client.get("regions/countries").data)
        response_expand_types = {cf['name']: [et for et in cf['expand_types']] for cf in country_info_result}

        assert response_expand_types == expected_expand_types
