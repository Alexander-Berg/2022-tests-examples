# coding=utf-8

import datetime

import pytest

import btestlib.reporter as reporter
from balance import balance_db as db
from balance import balance_steps as steps
from btestlib.constants import Products, Paysyses, Regions, Processings
from btestlib.utils import CheckMode, check_mode, aDict
from simpleapi.common import logger
from simpleapi.common.payment_methods import PaystepCard
from simpleapi.data import features, stories
from simpleapi.data import uids_pool as uids
from simpleapi.data.cards_pool import ALPHA_PAYSTEP_VISA
from simpleapi.steps import balance_steps as not_true_balance_steps
from simpleapi.steps import paystep_steps as paystep
from btestlib.constants import Services, Firms, PersonTypes, Currencies
from temp.igogor.balance_objects import Contexts
from balance.integrations import Integrations

log = logger.get_logger()

BASE_CHECK_RENDER_URL = 'https://greed-ts.paysys.yandex.net:8019'

NOW = datetime.datetime.now()


class Data(object):
    DIRECT_FISH = Products.DIRECT_FISH

    QTY = 100.456
    BASE_DT = datetime.datetime.now()

    # firm_id = 1
    # DIRECT_PH_CONTEXT = aDict({'person_type': 'ph',
    #                            'paysys': Paysyses.CC_PH_RUB,
    #                            'descr': 'DIRECT_PH',
    #                            ''})

    DIRECT_PH_CONTEXT = Contexts.DIRECT_FISH_RUB_CONTEXT.new(person_type=PersonTypes.PH,
                                                             firm=Firms.YANDEX_1,
                                                             currency=Currencies.RUB,
                                                             region=Regions.RU)
    test_data_professional = [
        (PaystepCard(Processings.ALPHA), card, DIRECT_PH_CONTEXT.new(service=Services.DIRECT)) for card in [
            ALPHA_PAYSTEP_VISA,  # CreditCardValid3DSPaymentTest
        ]]


def ids_paymethod_context_card(val):
    paymethod, card, context = val
    card_descr = card['descr'] if card else 'None'
    ids = "paymethod={} processing={} context={} card={}".format(paymethod.title, paymethod.processing,
                                                                 context.descr, card_descr)
    return ids


@reporter.feature(features.General.Paystep)
class TestPaystep(object):
    @reporter.story(stories.General.Payment)
    @pytest.mark.integration(Integrations.CHECK)
    @pytest.mark.parametrize('test_data',
                             Data.test_data_professional,
                             ids=ids_paymethod_context_card)
    def test_get_payment(self, test_data):
        uid = uids.get_random_of_type(uids.Types.random_from_all)

        paymethod, card, context = test_data
        person_type = context.person_type
        currency_code = context.currency.iso_code
        client_id = steps.ClientSteps.create_multicurrency()
        steps.ClientSteps.link(client_id, str(db.get_passport_by_passport_id(str(uid.id_))[0]['login']))
        person_id = steps.PersonSteps.create(client_id, 'ph')
        OVERDRAFT_LIMIT = 300

        steps.OverdraftSteps.set_force_overdraft(client_id, 7, OVERDRAFT_LIMIT, 1)

        service_order_id = steps.OrderSteps.next_id(context.service.id)
        steps.OrderSteps.create(client_id, service_order_id, context.product.id, context.service.id)
        service_order_id2 = steps.OrderSteps.next_id(context.service.id)
        order_id = steps.OrderSteps.create(client_id, service_order_id2, 503162, context.service.id)

        orders_list = [
            {'ServiceID': context.service.id, 'ServiceOrderID': service_order_id, 'Qty': 10, 'BeginDT': NOW},
            {'ServiceID': context.service.id, 'ServiceOrderID': service_order_id2, 'Qty': 10, 'BeginDT': NOW}
        ]

        request_id = steps.RequestSteps.create(client_id=client_id, orders_list=orders_list,
                                               additional_params=dict(InvoiceDesireDT=NOW))

        context.paysys = Paysyses.CC_PH_RUB
        steps.CampaignsSteps.do_campaigns(context.service.id, service_order_id2, {'Money': 10},
                                          0, datetime.datetime.now())
        invoice_id, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id,
                                                     paysys_id=context.paysys.id,
                                                     credit=0, contract_id=None, overdraft=1, endbuyer_id=None)
        # steps.InvoiceSteps.turn_on(invoice_id)

        steps.OverdraftSteps.expire_overdraft_invoice(invoice_id, delta=5)
        db.balance().execute('''update t_consume set completion_qty= :c_qty, completion_sum =:c_sum WHERE
                             parent_order_id = :order_id''', {'order_id': order_id, 'c_qty': 10, 'c_sum': 10})
        steps.OverdraftSteps.reset_overdraft_invoices(client_id)
        invoice = db.get_invoice_by_id(invoice_id)[0]
        external_invoice_id, total_invoice_sum = invoice['external_id'], invoice['total_sum']

        with check_mode(CheckMode.FAILED):
            paystep.pay_by(paymethod, context.service, user=uid, card=card, region_id=context.region.id,
                           invoice_id=invoice_id, data_for_checks={'invoice_id': invoice_id,
                                                                   'external_id': external_invoice_id,
                                                                   'total_sum': total_invoice_sum,
                                                                   'currency_iso_code': currency_code})

        payment_id = db.get_payments_by_invoice_id(invoice_id)[0]['id']
        steps.CommonSteps.export('CASH_REGISTER', 'Payment', payment_id)

        with reporter.step(u'Получаем краткие данные чека из таблицы BO.T_FISCAL_RECEIPT'):
            query = "SELECT RECEIPT_FN, RECEIPT_FD, RECEIPT_FPD FROM T_FISCAL_RECEIPT WHERE PAYMENT_ID=:item"
            check_data = db.balance().execute(query, {'item': payment_id})[0]

        fn = check_data['receipt_fn']
        fd = check_data['receipt_fd']
        fpd = check_data['receipt_fpd']
        # second_export = steps.CommonSteps.export('CASH_REGISTER', 'Payment', payment_id)
        # print second_export['output']
        html_url = BASE_CHECK_RENDER_URL + '/?n={}&fn={}&fpd={}'.format(fd, fn, fpd)
        mobile_url = BASE_CHECK_RENDER_URL + '/mobile/?n={}&fn={}&fpd={}'.format(fd, fn, fpd)
        pdf_url = BASE_CHECK_RENDER_URL + '/pdf/?n={}&fn={}&fpd={}'.format(fd, fn, fpd)

        print html_url
        print mobile_url
        print pdf_url


if __name__ == '__main__':
    pytest.main()
