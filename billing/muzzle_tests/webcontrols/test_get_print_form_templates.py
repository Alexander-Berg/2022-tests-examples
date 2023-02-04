import datetime

import pytest
import hamcrest

from balance import muzzle_util as ut
from balance import core
from balance.webcontrols.helpers import get_print_form_templates

from tests import object_builder as ob

TODAY = ut.trunc_date(datetime.datetime.now())
TOMORROW = TODAY + datetime.timedelta(1)


def test_template(session):
    if session.config.get('NEW_RULES_ON', False):
        attr_set = 'col0:firm:1'
        res = get_print_form_templates(
            session,
            attr_set=attr_set,
            c_type='general',
            is_col0='1'
        )
        assert '/sales/processing/Billing-agreements/OOO-Jandeks/Konnekt/Rastorzhenie-konnekt/' in res
