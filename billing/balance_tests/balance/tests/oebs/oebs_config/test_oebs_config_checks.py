# -*- coding: utf-8 -*-
import pytest

import balance.balance_api as api
import balance.balance_steps as steps
import btestlib.reporter as reporter
from balance.features import Features

log = reporter.logger()

pytestmark = [reporter.feature(Features.OEBS)]

@pytest.mark.smoke
def test_oebs_config_checks():
    result = api.test_balance().CheckOebsConfig()
    log.info('CheckOebsConfig result666: %s', result)
