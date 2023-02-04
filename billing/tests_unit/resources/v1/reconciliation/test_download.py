# -*- coding: utf-8 -*-

from __future__ import absolute_import
from __future__ import division

import urlparse

import datetime

from future import standard_library

standard_library.install_aliases()

import pytest
import hamcrest as hm
import http.client as http
import httpretty
import json

from balance.application import getApplication

from balance import constants as cst

from brest.core.tests import security
from yb_snout_api.tests_unit.base import TestCaseApiAppBase
from yb_snout_api.resources import enums
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.client import create_client
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.reconciliation import create_reconciliation_request
from yb_snout_api.tests_unit.fixtures.permissions import create_role


def make_yadoc_api_response(id_, status_code=http.OK, file_status_code=http.OK):
    url = getApplication().get_component_cfg('reconciliation_report')['Url']
    file_url = 'https://filepath.com/1234'
    httpretty.register_uri(
        httpretty.GET,
        urlparse.urljoin(url, 'report/{}/pdf'.format(id_)),
        status=status_code,
        body=json.dumps(file_url),
    )
    httpretty.register_uri(
        httpretty.GET,
        file_url,
        status=file_status_code,
        body=b'abc',
    )


@pytest.fixture(name='view_inv_role')
def create_view_inv_role():
    return create_role(
        (
            cst.PermissionCode.VIEW_INVOICES,
            {cst.ConstraintTypes.firm_id: None, cst.ConstraintTypes.client_batch_id: None},
        ),
    )


@pytest.mark.smoke
@pytest.mark.usefixtures('httpretty_enabled_fixture')
class TestDownloadRequest(TestCaseApiAppBase):
    BASE_API = '/v1/reconciliation/download'

    @pytest.mark.parametrize(
        'w_role',
        [True, False],
    )
    def test_download(self, client, view_inv_role, w_role):
        if w_role:
            security.set_roles([view_inv_role])
        else:
            security.set_roles([])
            security.set_passport_client(client)

        reconciliation_request = create_reconciliation_request(client=client)
        make_yadoc_api_response(reconciliation_request.external_id)

        res = self.test_client.get(
            self.BASE_API,
            {'reconciliation_request_id': reconciliation_request.id},
            is_admin=False,
        )
        hm.assert_that(res.status_code, hm.equal_to(http.OK))

        hm.assert_that(
            res,
            hm.has_properties({
                'headers': hm.has_items(
                    hm.contains('Content-Type', enums.Mimetype.PDF.value),
                    hm.contains('Content-Disposition', 'attachment; filename="report_%s.pdf"'
                                % reconciliation_request.dt.strftime('%Y-%m-%dT%H:%M:%S')),
                ),
                'response': hm.contains(b'abc')
            }),
        )

    @pytest.mark.parametrize(
        'f_name',
        ['status_code', 'file_status_code'],
    )
    def test_invalid_resp(self, reconciliation_request, f_name):
        params = {f_name: http.BAD_GATEWAY}
        make_yadoc_api_response(reconciliation_request.external_id, **params)

        res = self.test_client.get(
            self.BASE_API,
            {'reconciliation_request_id': reconciliation_request.id},
            is_admin=False,
        )
        hm.assert_that(res.status_code, hm.equal_to(http.INTERNAL_SERVER_ERROR))
        hm.assert_that(
            res.get_json(),
            hm.has_entries({'error': 'RECONCILIATION_REPORT_CONNECTION_ERROR'}),
        )

    def test_nobody(self, client):
        security.set_roles([])
        reconciliation_request = create_reconciliation_request(client=client)
        res = self.test_client.get(
            self.BASE_API,
            {'reconciliation_request_id': reconciliation_request.id},
            is_admin=False,
        )
        hm.assert_that(res.status_code, hm.equal_to(http.FORBIDDEN))
