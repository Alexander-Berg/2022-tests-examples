# -*- coding: utf-8 -*-
from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

from future import standard_library

standard_library.install_aliases()

import pytest
import http.client as http
import hamcrest as hm
import sqlalchemy as sa

from balance import mapper

from yb_snout_api.tests_unit.base import TestCaseApiAppBase


class TestCaseEnums(TestCaseApiAppBase):
    BASE_API = '/v1/common/enums'

    def test_get_contract_types(self):
        parent = (
            self.test_session
            .query(mapper.EnumsTree)
            .filter_by(parent_id=None, code='contracts')
            .first()
        )
        max_id = self.test_session.query(sa.func.max(mapper.EnumsTree.id)).scalar()
        child = mapper.EnumsTree(
            id=max_id + 5,
            code=100500,
            parent_id=parent.id,
            value='New snout contract type',
        )
        self.test_session.add(child)
        self.test_session.flush()

        res = self.test_client.get(self.BASE_API, {'enum_code': 'CONTRACTS'})
        hm.assert_that(res.status_code, hm.equal_to(http.OK))

        data = res.get_json()['data']

        hm.assert_that(
            data,
            hm.all_of(
                hm.has_length(len(parent.children)),
                hm.has_item(
                    hm.has_entries({
                        'id': str(child.code),
                        'label': child.value,
                    }),
                ),
            ),
        )

    @pytest.mark.parametrize(
        'enum_code, ans',
        [
            pytest.param('CONTRACT_CLASS', {'id': 'PARTNERS', 'label': 'РСЯ'}, id='CONTRACT_CLASS'),
            pytest.param('PARTNER_C_TYPE', {'id': '9', 'label': 'Оферта'}, id='PARTNER_C_TYPE'),
            pytest.param('CONTRACT_DOC_SET', {'id': '4', 'label': 'с авг. 2009 (ППС)'}, id='CONTRACT_DOC_SET'),
            pytest.param('BILL_INTERVALS', {'id': '1', 'label': 'Акт раз в месяц'}, id='BILL_INTERVALS'),
            pytest.param('DISTRIBUTION_C_TYPE', {'id': '1', 'label': 'Разделение доходов'}, id='DISTRIBUTION_C_TYPE'),
            pytest.param('PLATFORM_TYPE', {'id': '3', 'label': 'Мобильный + Десктопный'}, id='PLATFORM_TYPE'),
        ],
    )
    def test_get_enums(self, enum_code, ans):
        """Проверяем по одной константе для каждого типа ресурсов
        """
        res = self.test_client.get(self.BASE_API, {'enum_code': enum_code})
        hm.assert_that(res.status_code, hm.equal_to(http.OK))

        data = res.get_json()['data']
        hm.assert_that(data, hm.has_item(hm.has_entries(ans)))
