# coding: utf-8
__author__ = 'blubimov'

import pytest

from balance import balance_steps as steps
from btestlib.constants import ClientCategories, PersonTypes, Services, Products, Paysyses
from post_restore_common import get_client_linked_with_login_or_create

'''  Создаем объекты баланса для логинов Молли  '''


@pytest.mark.parametrize('login', [
    'mollyprod',
    'balance-molly-tester',
])
def test_restore_balance_objects(login):
    person_type = PersonTypes.PH.code
    paysys_id = Paysyses.BANK_PH_RUB.id
    service_id = Services.DIRECT.id
    product_id = Products.DIRECT_FISH.id

    client_id = get_client_linked_with_login_or_create(login, ClientCategories.CLIENT)
    person_id = steps.PersonSteps.create(client_id, person_type)

    campaigns_list = [{'client_id': client_id, 'service_id': service_id, 'product_id': product_id, 'qty': 100}]

    invoice_id, _, _, orders_list = steps.InvoiceSteps.create_force_invoice(client_id=client_id,
                                                                            person_id=person_id,
                                                                            campaigns_list=campaigns_list,
                                                                            paysys_id=paysys_id)

    steps.InvoiceSteps.turn_on(invoice_id)
    for order in orders_list:
        steps.CampaignsSteps.do_campaigns(service_id, order['ServiceOrderID'], {'Bucks': order['Qty']})
    steps.ActsSteps.generate(client_id, force=0)
