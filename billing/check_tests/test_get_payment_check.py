# coding: utf-8

import datetime

import pytest

from balance import balance_db as db
from btestlib.constants import Products, Paysyses, Regions, Processings
from btestlib.utils import CheckMode, check_mode, aDict
from simpleapi.common import logger
from simpleapi.common.payment_methods import PaystepCard
from simpleapi.data import uids_pool as uids
from simpleapi.data.cards_pool import ALPHA_PAYSTEP_VISA
from simpleapi.steps import balance_steps as not_true_balance_steps
from simpleapi.steps import paystep_steps as paystep
import balance.balance_steps as steps
import balance.balance_web as web

log = logger.get_logger()


class Data(object):
    DIRECT_FISH = Products.DIRECT_FISH

    QTY = 100.456
    BASE_DT = datetime.datetime.now()

    # firm_id = 1
    DIRECT_PH_CONTEXT = aDict({'person_type': 'ph',
                               'paysys': Paysyses.CC_PH_RUB,
                               'descr': 'DIRECT_PH'})

    DIRECT_UR_CONTEXT = aDict({'person_type': 'ur',
                               'paysys': Paysyses.CC_UR_RUB,
                               'descr': 'DIRECT_UR'})

    DIRECT_UR_CONTEXT_INDIVIDUAL = aDict({'person_type': 'ur',
                                          'paysys': Paysyses.CC_UR_RUB,
                                          'descr': 'DIRECT_UR_IND',
                                          'inn': 500100732259})

    test_data_professional = [(PaystepCard(Processings.ALPHA), ALPHA_PAYSTEP_VISA, context) for context in [
        DIRECT_PH_CONTEXT,
        DIRECT_UR_CONTEXT,
        DIRECT_UR_CONTEXT_INDIVIDUAL
    ]
                              ]


class TestCheck(object):

    def ids_paymethod_context_card(val):
        paymethod, card, context = val
        card_descr = card['descr'] if card else 'None'
        ids = "paymethod={} processing={} context={} card={}".format(paymethod.title, paymethod.processing,
                                                                     context.descr, card_descr)
        return ids

    @pytest.mark.parametrize('test_data',
                             Data.test_data_professional,
                             ids=ids_paymethod_context_card)
    def test_get_payment_check(self, test_data):
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
        query = "select receipt_fn, receipt_fd, receipt_fpd from t_fiscal_receipt where payment_id=:payment_id"
        get_payment_data = db.balance().execute(query, {'payment_id': payment_id})[0]

        fn = get_payment_data['receipt_fn']
        fd = get_payment_data['receipt_fd']
        fpd = get_payment_data['receipt_fpd']

        url = web.CheckInterface.CheckPage.url(fd, fn, fpd, 'html')
        log.debug(url)
