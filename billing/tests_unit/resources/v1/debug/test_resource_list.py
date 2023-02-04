# -*- coding: utf-8 -*-
from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

from future import standard_library

standard_library.install_aliases()

import http.client as http
import hamcrest as hm

from balance.constants import PermissionCode

from yb_snout_api.resources import enums
from yb_snout_api.tests_unit.base import TestCaseApiAppBase


class TestResourceList(TestCaseApiAppBase):
    BASE_API = '/v1/debug/resource/list'

    def test_resource_list(self):
        response = self.test_client.get(self.BASE_API)

        hm.assert_that(response.status_code, hm.equal_to(http.OK), 'response code must be OK')
        hm.assert_that(
            response.get_json().get('data', []),
            hm.has_item(hm.has_entries({
                'url': self.BASE_API,
                'module': 'yb_snout_api.resources.v1.debug.routes.resource_list',
                'classname': 'ResourceList',
                'methods': ['GET'],
                'allow_client_ui_access': True,
                'admins_only': True,
                'ui_permissions': hm.contains_inanyorder(PermissionCode.ADMIN_ACCESS, PermissionCode.VIEW_DEV_DATA),
            })),
        )

    def test_client_resource_list(self):
        from yb_snout_api.resources.v1.debug import enums as enums_debug

        response = self.test_client.get('{}?interface_type={}'.format(
            self.BASE_API,
            enums_debug.InterfaceType.CLIENT.name,
        ))

        hm.assert_that(response.status_code, hm.equal_to(http.OK), 'response code must be OK')
        hm.assert_that(
            response.get_json().get('data', []),
            hm.has_item(hm.has_entries({'url': self.BASE_API})),
        )
        hm.assert_that(
            response.get_json().get('data', []),
            hm.only_contains(hm.has_entries({'allow_client_ui_access': True})),
        )


class TestResourceListXls(TestCaseApiAppBase):
    BASE_API = '/v1/debug/resource/list/xls'
    DEFAULT_FILENAME = 'spam.xls'

    def test_resource_list_xls(self):
        url = u'{}?filename={}'.format(
            self.BASE_API,
            self.DEFAULT_FILENAME,
        )

        response = self.test_client.get(url)
        hm.assert_that(response.status_code, hm.equal_to(http.OK), 'response code must be OK')

        headers = response.headers
        content_disposition = 'filename=%s' % self.DEFAULT_FILENAME
        hm.assert_that(response.content_type, hm.equal_to(enums.Mimetype.XLS.value))
        hm.assert_that(
            headers,
            hm.has_items(
                hm.contains('Content-Type', enums.Mimetype.XLS.value),
                hm.contains('Content-Disposition', hm.contains_string(content_disposition.encode('utf-8'))),
            ),
        )
