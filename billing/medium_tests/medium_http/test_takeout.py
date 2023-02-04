# -*- coding: utf-8 -*-

import json
import time

import pytest
import hamcrest

from balance.constants import (
    ExportState,
)
from medium.medium_takeout import MediumTakeoutLogic

from tests import object_builder as ob

pytestmark = [
    pytest.mark.takeout
]


class TestTakeout(object):
    def setup(self):
        self.logic = MediumTakeoutLogic()
        self.default_unix_time = int(time.time())
        self.job_id = None
        self.default_job_id = 'test_job_id_{time}'.format(time=self.default_unix_time)
        self.response_no_data = (200, json.dumps({
            'status': 'no_data'
        }))
        self.response_ok = (200, json.dumps({
            'status': 'ok',
            'job_id': self.job_id if self.job_id else self.default_job_id
        }))

    def prepare_params(self, uid, job_id=None, unix_time=None):
        if job_id:
            self.job_id = job_id
        return {
            'uid': uid,
            'job_id': self.job_id if self.job_id else self.default_job_id,
            'unixtime': unix_time if unix_time else self.default_unix_time
        }

    def test_invalid_passport_id(self):
        invalid_passport_id = 0
        result = self.logic.takeout(
            self.prepare_params(uid=invalid_passport_id)
        )
        assert result == self.response_no_data

    def test_passport_id_without_client(self, session):
        passport = ob.PassportBuilder.construct(session=session)
        result = self.logic.takeout(self.prepare_params(uid=passport.passport_id))
        assert result == self.response_no_data

    def test_client_without_invoices(self, session):
        client = ob.ClientBuilder.construct(session=session)
        passport = ob.PassportBuilder.construct(session=session, client=client)
        result = self.logic.takeout(
            self.prepare_params(uid=passport.passport_id),
        )
        assert result == self.response_no_data

    def test_client_is_agency(self, session):
        client = ob.ClientBuilder.construct(session=session, is_agency=1)
        person = ob.PersonBuilder.construct(
            session=session,
            client=client,
            type='sw_ur',
        )
        ob.InvoiceBuilder.construct(session=session, client=client, person=person)
        passport = ob.PassportBuilder.construct(session=session, client=client)
        result = self.logic.takeout(
            self.prepare_params(uid=passport.passport_id)
        )
        assert result == self.response_no_data

    def test_client_with_invoices_russian_resident(self, session):
        client = ob.ClientBuilder.construct(session=session)
        person = ob.PersonBuilder.construct(
            session=session,
            client=client,
            type='ur',
        )
        ob.InvoiceBuilder.construct(session=session, client=client, person=person)
        passport = ob.PassportBuilder.construct(session=session, client=client)
        result = self.logic.takeout(
            self.prepare_params(uid=passport.passport_id),
        )
        assert result == self.response_no_data

    def test_client_with_invoices_not_russian_resident(self, session):
        client = ob.ClientBuilder.construct(session=session)
        person = ob.PersonBuilder.construct(
            session=session,
            client=client,
            type='sw_ur',
        )
        ob.InvoiceBuilder.construct(session=session, client=client, person=person)
        passport = ob.PassportBuilder.construct(session=session, client=client)
        result = self.logic.takeout(
            self.prepare_params(uid=passport.passport_id),
        )
        assert result == self.response_ok
        hamcrest.assert_that(
            passport.exports['TAKEOUT'],
            hamcrest.has_properties(
                state=ExportState.enqueued,
                input={'job_id': self.default_job_id}
            )
        )
