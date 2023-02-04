# -*- coding: utf-8 -*-
from __future__ import absolute_import
from __future__ import division

from future import standard_library
from future.builtins import str as text

standard_library.install_aliases()

import http.client as http
import pytest
import hamcrest as hm

from balance import mapper

from brest.core.tests import security
from yb_snout_api.tests_unit.base import TestCaseApiAppBase
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.permissions import create_admin_role


@pytest.mark.smoke
class TestCaseEdoTypes(TestCaseApiAppBase):
    BASE_API = u'/v1/edo/types'

    def test_admin_access(self, admin_role):
        security.set_roles(admin_role)
        response = self.test_client.get(self.BASE_API)
        hm.assert_that(response.status_code, hm.equal_to(http.OK))

        hm.assert_that(
            response.get_json().get('data', {}),
            hm.contains_inanyorder(
                hm.has_entries({
                    'id': None,
                    'text': 'ID_not_selected',
                }),
                *[
                    hm.has_entries({
                        'id': text(edo_type.id),
                        'text': u'ID_EDO_%s' % edo_type.id,
                    })
                    for edo_type in self.test_session.query(mapper.EdoType)
                ]  # noqa: C815
            ),
        )

    def test_client_access(self):
        response = self.test_client.get(self.BASE_API)
        hm.assert_that(response.status_code, hm.equal_to(http.OK))

        hm.assert_that(
            response.get_json().get('data', {}),
            hm.contains_inanyorder(
                hm.has_entries({
                    'id': None,
                    'text': 'ID_not_selected',
                }),
                *[
                    hm.has_entries({
                        'id': text(edo_type.id),
                        'text': u'ID_EDO_%s' % edo_type.id,
                    })
                    for edo_type in self.test_session.query(mapper.EdoType)
                ]  # noqa: C815
            ),
        )
