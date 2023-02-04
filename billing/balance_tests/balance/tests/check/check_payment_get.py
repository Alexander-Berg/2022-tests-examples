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
from balance.integrations import Integrations

log = logger.get_logger()

BASE_CHECK_RENDER_URL = 'https://greed-ts.paysys.yandex.net:8019'


class Data(object):
    DIRECT_FISH = Products.DIRECT_FISH

    QTY = 100.456
    BASE_DT = datetime.datetime.now()

    # firm_id = 1
    DIRECT_PH_CONTEXT = aDict({'person_type': 'ph',
                               'paysys': Paysyses.CC_PH_RUB,
                               'descr': 'DIRECT_PH'})

    test_data_professional = [(PaystepCard(Processings.ALPHA), card, DIRECT_PH_CONTEXT) for card in [
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
    @pytest.mark.smoke
    @pytest.mark.integration(Integrations.CHECK)
    @pytest.mark.parametrize('test_data',
                             Data.test_data_professional,
                             ids=ids_paymethod_context_card)
    def test_get_payment(self, test_data):
        uid = uids.get_random_of_type(uids.Types.random_from_all)

        paymethod, card, context = test_data
        person_type = context.person_type
        product = context.get('product', Data.DIRECT_FISH)
        region = context.get('region', Regions.RU)
        currency_code = context.get('currency_code', context.paysys.currency.iso_code)

        client_id, person_id, invoice_id, external_invoice_id, total_invoice_sum = \
            not_true_balance_steps.prepare_data_for_paystep(context=context, user=uid, person_type=person_type,
                                                            product=product, qty=Data.QTY, dt=Data.BASE_DT)

        with check_mode(CheckMode.FAILED):
            paystep.pay_by(paymethod, product.service, user=uid, card=card, region_id=region.id,
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
