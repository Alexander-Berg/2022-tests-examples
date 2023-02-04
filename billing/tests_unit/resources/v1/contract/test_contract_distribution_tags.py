# -*- coding: utf-8 -*-
from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

from future import standard_library

standard_library.install_aliases()

import http.client as http
import pytest
import hamcrest as hm

from tests import object_builder as ob

from yb_snout_api.tests_unit.base import TestCaseApiAppBase
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.client import create_client


@pytest.mark.smoke
class TestContractDistributionTags(TestCaseApiAppBase):
    BASE_API = '/v1/contract/distribution-tags'

    def test_get_several(self, client):
        name = 'AC/DC'
        tag1 = ob.DistributionTagBuilder.construct(self.test_session, tag_id=-666, client_id=client.id, name=name)
        tag2 = ob.DistributionTagBuilder.construct(self.test_session, tag_id=-333, client_id=client.id, name=name)

        res = self.test_client.get(self.BASE_API, {'tag_name': name})
        hm.assert_that(res.status_code, hm.equal_to(http.OK))

        data = res.get_json()['data']
        hm.assert_that(
            data,
            hm.contains(
                hm.has_entries({'id': tag1.id, 'name': name, 'first': True}),
                hm.has_entries({'id': tag2.id, 'name': name, 'first': False}),
            ),
        )

    @pytest.mark.parametrize(
        'field_name',
        ['tag_id', 'tag_name'],
    )
    def test_get(self, client, field_name):
        name = 'my_neighbor_is_totoro'
        tag = ob.DistributionTagBuilder.construct(self.test_session, tag_id=ob.get_big_number(), client_id=client.id, name=name)

        params = {
            'tag_id': tag.id,
            'tag_name': '_neighbor_is_toto',
        }
        params[field_name] = params[field_name]

        res = self.test_client.get(self.BASE_API, params)
        hm.assert_that(res.status_code, hm.equal_to(http.OK))

        data = res.get_json()['data']
        hm.assert_that(
            data,
            hm.contains(
                hm.has_entries({
                    'id': tag.id,
                    'name': name,
                    'first': True,
                }),
            ),
        )

    @pytest.mark.parametrize(
        'pn, ps, offset, limit',
        [
            (1, 2, 0, 2),
            (3, 3, 6, 7),
        ],
    )
    @pytest.mark.parametrize(
        'sort_key, sort_order',
        [
            ('tag_id', 'asc'),
            ('tag_name', 'desc')
        ],
    )
    def test_pagination(self, client, pn, ps, limit, offset, sort_key, sort_order):
        name = 'Kaze_no_tani_no_Naushika'
        tags = [
            ob.DistributionTagBuilder.construct(self.test_session, tag_id=ob.get_big_number(), client_id=client.id, name=name + str(i))
            for i in range(7)
        ]
        sort_fun = {'tag_id': lambda x: x.id, 'tag_name': lambda x: x.name}[sort_key]

        res = self.test_client.get(
            self.BASE_API,
            {
                'tag_name': name,
                'pagination_pn': pn,
                'pagination_ps': ps,
                'sort_order': sort_order.upper(),
                'sort_key': sort_key.upper(),
            },
        )
        hm.assert_that(res.status_code, hm.equal_to(http.OK))

        data = res.get_json()['data']
        hm.assert_that(
            data,
            hm.contains(*[
                hm.has_entries({'id': tag.id, 'name': tag.name})
                for tag in sorted(tags, key=sort_fun, reverse=sort_order == 'desc')[offset:limit]
            ])
        )


@pytest.mark.smoke
class TestContractDistributionTagsNames(TestCaseApiAppBase):
    BASE_API = '/v1/contract/distribution-tags/names'

    name_1 = 'Miyazaki: Hauru no ugoku shiro'
    name_2 = 'Miyazaki: Kaze_no_tani_no_Naushika'
    name_3 = 'Miyazaki: Sen to Chihiro no kamikakushi'

    @pytest.mark.parametrize(
        'sort_order, pn, ps, ans',
        [
            pytest.param('asc', 1, 3, [name_1, name_2, name_3]),
            pytest.param('asc', 1, 2, [name_1, name_2]),
            pytest.param('desc', 1, 2, [name_3, name_2]),
            pytest.param('desc', 1, 4, [name_3, name_2, name_1]),
            pytest.param('desc', 2, 2, [name_1]),
        ],
    )
    def test_base(self, client, sort_order, pn, ps, ans):
        [
            ob.DistributionTagBuilder.construct(
                self.test_session,
                tag_id=ob.get_big_number(),
                client_id=client.id,
                name=name,
            )
            for _i in range(2)
            for name in [self.name_1, self.name_2, self.name_3]
        ]
        res = self.test_client.get(
            self.BASE_API,
            {
                'tag_name': 'Miyazaki:',
                'pagination_pn': pn,
                'pagination_ps': ps,
                'sort_order': sort_order.upper(),
            },
        )
        hm.assert_that(res.status_code, hm.equal_to(http.OK))

        data = res.get_json()['data']
        hm.assert_that(
            data,
            hm.contains(*[
                hm.has_entries({'name': name})
                for name in ans
            ])
        )
