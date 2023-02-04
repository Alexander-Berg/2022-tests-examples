# -*- coding: utf-8 -*-
from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

from future import standard_library

standard_library.install_aliases()

import http.client as http
from hamcrest import (
    assert_that,
    equal_to,
    contains_string,
)

from balance import mapper

from yb_snout_api.tests_unit.base import TestCaseApiAppBase
from yb_snout_api.tests_unit.fixtures.client import create_client
from yb_snout_api.resources.assessor.export.enums import ExportQueueName, ExportClassname


class TestCaseExportObject(TestCaseApiAppBase):
    BASE_API = u'/assessor/export/export-object'
    EXPORT_TYPE = ExportQueueName.OVERDRAFT.name
    EXPORT_CLASSNAME = ExportClassname.Client.name

    def test_export_client(self, client):
        from yb_snout_api.resources import enums

        session = self.test_session

        response = self.test_client.secure_post(
            self.BASE_API,
            data={
                'class_name': self.EXPORT_CLASSNAME,
                'object_id': client.id,
                'queue_name': self.EXPORT_TYPE,
            },
        )
        assert_that(response.status_code, equal_to(http.OK), 'response code must be OK')

        export = session.query(mapper.Export).getone(
            classname=self.EXPORT_CLASSNAME,
            object_id=client.id,
            type=self.EXPORT_TYPE,
        )
        assert_that(export.state, equal_to(enums.ExportState.EXPORTED.value))
        assert_that(export.error, equal_to(None))
        assert_that(export.output, contains_string('Decline in overdraft for service_id=7 due reason '
                                                   '"Client doesn\'t have resident payers"'))
