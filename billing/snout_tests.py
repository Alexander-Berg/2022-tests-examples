# coding: utf-8
import pytest

from balance import balance_steps as steps
from balance.snout_steps import CommonSnoutSteps as common_snout_steps, CartSteps as cart_steps
from balance import real_builders
from btestlib.constants import Permissions


def test_basic_case():
    client_id, invoice_id = real_builders.test_prepayment_unpaid_invoice()
    session = common_snout_steps.get_session(client_id, required_permissions=[Permissions.ADMIN_ACCESS_0])
    print common_snout_steps.get_invoice_data(session, invoice_id).content

