# coding: utf-8

__author__ = 'blubimov'

from hamcrest import equal_to

from balance import balance_db as db
from balance import balance_steps as steps
from balance import balance_web
from balance.features import Features
from btestlib import constants as c
from btestlib import reporter
from btestlib import utils

TEST_EMAIL = "test-balance-notify@yandex-team.ru"


@reporter.feature(Features.UI, Features.INVOICE, Features.INVOICE_PRINT_FORM)
def test_pf_send():
    client_id = steps.ClientSteps.create()
    person_id = steps.PersonSteps.create(client_id, c.PersonTypes.PH.code)

    campaigns_list = [
        {'client_id': client_id, 'service_id': c.Services.DIRECT.id, 'product_id': c.Products.DIRECT_FISH.id, 'qty': 1},
    ]

    invoice_id, _, _, orders_list = steps.InvoiceSteps.create_force_invoice(client_id=client_id,
                                                                            person_id=person_id,
                                                                            campaigns_list=campaigns_list,
                                                                            paysys_id=c.Paysyses.BANK_PH_RUB.id)

    with balance_web.Driver() as driver:
        page = balance_web.ClientInterface.SuccessPage.open(driver, invoice_id)
        page.send_print_form(email=TEST_EMAIL)

    query = """select * from T_MESSAGE
                where OBJECT_ID = :invoice_id
                and opcode = 1
                and RECEPIENT_ADDRESS = :email"""
    lines = db.balance().execute(query, {'invoice_id': invoice_id, 'email': TEST_EMAIL})

    utils.check_that(len(lines), equal_to(1), step=u'Проверяем, что в t_message появилась одна строка')
