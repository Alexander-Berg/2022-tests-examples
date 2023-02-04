# -*- coding: utf-8 -*-
from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

from future import standard_library

standard_library.install_aliases()

import pytest
import hamcrest as hm
import http.client as http

from tests import object_builder as ob

from yb_snout_api.tests_unit.base import TestCaseApiAppBase


@pytest.mark.smoke
class TestCaseClientActivityTypes(TestCaseApiAppBase):
    BASE_API = u'/v1/client/activity-types'

    def test_returned_list(self):
        session = self.test_session
        parent = ob.ActivityBuilder.construct(session, name='parent', hidden=1)
        child = ob.ActivityBuilder.construct(session, name='child', parent=parent)
        session.flush()

        response = self.test_client.get(self.BASE_API)
        hm.assert_that(response.status_code, hm.equal_to(http.OK))

        data = response.get_json().get('data')
        hm.assert_that(
            data,
            hm.has_items(
                hm.has_entries({'id': parent.id, 'name': 'parent', 'hidden': True, 'parent_id': None}),
                hm.has_entries({'id': child.id, 'name': 'parent :: child', 'hidden': False, 'parent_id': parent.id}),
            ),
        )
