# -*- coding: utf-8 -*-
from __future__ import absolute_import
from __future__ import division

from future import standard_library

standard_library.install_aliases()

import datetime
import pytest
import allure
import tests.object_builder as ob

from brest.core.tests import utils as test_utils


class ExportPaymentParam(object):
    classname = 'Payment'
    queue_type = 'THIRDPARTY_TRANS'
    fail_queue_type = 'FAKE'
    export_dt = datetime.datetime.now()


@pytest.fixture(name='export_payment_id')
@allure.step('create export for payment')
def create_export_for_payment():
    from yb_snout_api.resources import enums

    session = test_utils.get_test_session()

    payment_id = ob.get_big_number()
    export = ob.ExportBuilder(
        classname=ExportPaymentParam.classname,
        object_id=payment_id,
        type=ExportPaymentParam.queue_type,
        state=enums.ExportState.EXPORTED.value,
        export_dt=ExportPaymentParam.export_dt,
    ).build(session)

    return export.object_id
