# -*- coding: utf-8 -*-
from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

from future import standard_library
from future.utils import iteritems

standard_library.install_aliases()

import pytest
import hamcrest as hm
import http.client as http

from tests import object_builder as ob

from yb_snout_api.tests_unit.base import TestCaseApiAppBase


@pytest.mark.smoke
class TestCaseContractPdfEmailAddresses(TestCaseApiAppBase):
    BASE_API = u'/v1/contract/pdf-email-addresses'

    def _generate_pdf_emails(self, firm_id, email=None, hidden=0, count=3):
        if email is None:
            email = ob.generate_character_string(20)
        return [
            ob.ContractPDFEmailBuilder.construct(
                self.test_session,
                firm_id=firm_id,
                email=email,
                hidden=hidden,
            )
            for _ in range(count)
        ]

    def test_getting_pdf_email_addresses(self):
        firms = [ob.FirmBuilder.construct(self.test_session) for _ in range(2)]

        visible_pdf_emails = {firm.id: self._generate_pdf_emails(firm.id, hidden=0) for firm in firms}
        for firm in firms:
            self._generate_pdf_emails(firm.id, hidden=1)

        res = self.test_client.get(
            self.BASE_API,
        )
        hm.assert_that(res.status_code, hm.equal_to(http.OK))
        data = res.get_json().get('data', [])
        firms_matchers = []
        for firm_id, pdf_emails in iteritems(visible_pdf_emails):
            emails_matcher = (e.email for e in pdf_emails)
            firms_matchers.append(
                hm.has_entries(firm_id=firm_id, addresses=hm.contains_inanyorder(*emails_matcher)),
            )
        hm.assert_that(data, hm.has_entries(firms=hm.has_items(*firms_matchers)))
