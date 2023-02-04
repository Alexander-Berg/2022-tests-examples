# -*- coding: utf-8 -*-
import json

import datetime
import hamcrest
import pytest

from balance import mapper
from balance.queue_processor import QueueProcessor
from tests import object_builder as ob
from tests.balance_tests.oebs_api.conftest import (mock_post, assert_call_check, assert_call_start)

on_dt = datetime.datetime.now().replace(microsecond=0)


class TestDBLogging(object):

    @pytest.fixture(autouse=True)
    def config_db_logging(self, session):
        session.config.__dict__['LOG_OEBS_API_EXPORT'] = True

    @pytest.fixture(autouse=True)
    def config_use_oebs_api(self, session):
        session.config.__dict__['CLASSNAMES_EXPORTED_WITH_OEBS_API'] = {'Person': 1}

    def test_start_ok(self, session, person, firm, export_obj, use_oebs_api, service_ticket_mock):
        person.type = 'ph'
        person.firms = [firm]
        session.flush()
        answer = {
            "result": "SUCCESS",
            "request_id": ob.get_big_number(),
        }
        with mock_post(answer) as mock_obj:
            QueueProcessor('OEBS_API').process_one(export_obj)
            session.flush()

        log_obj = (
            session.query(mapper.OEBSApiExportLog)
                .filter_by(classname='Person', object_id=export_obj.object_id)
                .one()
        )
        hamcrest.assert_that(
            log_obj,
            hamcrest.has_properties(
                dt=hamcrest.is_not(None),
                method='billingImport',
                data=hamcrest.is_not(hamcrest.empty()),
                status_code=200,
                response=json.dumps(answer)
            )
        )
        assert_call_start(mock_obj)

    def test_check_fail(self, session, person, firm, export_obj, use_oebs_api, service_ticket_mock):
        person.firms = [firm]
        session.flush()
        answer = [{"result": "ERROR",
                   "errors": ["Ошибка получения статуса"]}]
        request_id = ob.get_big_number()
        input_ = {'request_id': request_id}
        export_obj.input = input_
        session.flush()
        with mock_post(answer, 418) as mock_obj:
            QueueProcessor('OEBS_API').process_one(export_obj)
            session.flush()

        log_obj = (
            session.query(mapper.OEBSApiExportLog)
                .filter_by(classname='Person', object_id=export_obj.object_id)
                .one()
        )
        hamcrest.assert_that(
            log_obj,
            hamcrest.has_properties(
                dt=hamcrest.is_not(None),
                method='getStatusBilling',
                data=json.dumps([input_]),
                status_code=418,
                response=json.dumps(answer)
            )
        )
        assert_call_check(mock_obj, request_id)
