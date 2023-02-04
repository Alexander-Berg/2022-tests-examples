# -*- coding: utf-8 -*-

from __future__ import absolute_import
from __future__ import division

import datetime
import json

import pytest
from future import standard_library

standard_library.install_aliases()

import pytest
import mock


@pytest.fixture(scope='module', autouse=True)
def mock_tvm_ticket():
    from balance.api.reconciliation_report import ReconciliationReportApi
    with mock.patch.object(ReconciliationReportApi, '_get_tvm_ticket', return_value='666'):
        yield
