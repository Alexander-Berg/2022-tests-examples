# coding=utf-8

import datetime

import pytest

import btestlib.reporter as reporter
from balance import balance_db as db
from balance import balance_steps as steps
from btestlib.constants import Products, Paysyses, Processings
from btestlib.utils import CheckMode, check_mode, aDict
from simpleapi.common import logger
from simpleapi.common.payment_methods import PaystepCard
from simpleapi.data import features, stories
from simpleapi.data import uids_pool as uids
from simpleapi.data.cards_pool import ALPHA_PAYSTEP_VISA
from simpleapi.steps import balance_steps as not_true_balance_steps
from simpleapi.steps import paystep_steps as paystep
from temp.igogor.balance_objects import Contexts, Regions, Currencies, PersonTypes

log = logger.get_logger()

BASE_CHECK_RENDER_URL = 'https://greed-ts.paysys.yandex.net:8019'


class Data(object):
    DIRECT_FISH = Products.DIRECT_FISH

    QTY = 49999.9866
    BASE_DT = datetime.datetime.now()

    # firm_id = 1
    DIRECT_PH_CONTEXT = aDict({'person_type': 'ph',
                               'paysys': Paysyses.CC_PH_RUB,
                               'descr': 'DIRECT_PH'})

    DIRECT_MONEY_RUB_CONTEXT = Contexts.DIRECT_MONEY_RUB_CONTEXT.new(region=Regions.RU, currency=Currencies.RUB,
                                                                     person_type=PersonTypes.PH,
                                                                     paysys=Paysyses.CC_PH_RUB)

    test_data_professional = [(PaystepCard(Processings.ALPHA), card, DIRECT_PH_CONTEXT) for card in [
        ALPHA_PAYSTEP_VISA,  # CreditCardValid3DSPaymentTest
    ]]

    test_data_professional_rub = [(PaystepCard(Processings.ALPHA), card, DIRECT_MONEY_RUB_CONTEXT) for card in [
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
        export = steps.CommonSteps.export('CASH_REGISTER', 'Payment', payment_id)


        #
        # sn = export['input']['fn']['sn']
        # id = export['input']['id']
        # fp = export['input']['fp']
        # # second_export = steps.CommonSteps.export('CASH_REGISTER', 'Payment', payment_id)
        # # print second_export['output']
        # html_url = BASE_CHECK_RENDER_URL + '/fiscal_storages/{}/documents/{}/{}'.format(sn, id, fp)
        # mobile_url = BASE_CHECK_RENDER_URL + '/mobile/fiscal_storages/{}/documents/{}/{}'.format(sn, id, fp)
        # pdf_url = BASE_CHECK_RENDER_URL + '/pdf/fiscal_storages/{}/documents/{}/{}'.format(sn, id, fp)
        #
        # print html_url
        # print mobile_url

    @pytest.mark.parametrize('test_data',
                             Data.test_data_professional_rub,
                             ids=ids_paymethod_context_card)
    def test_get_payment_money(self, test_data):
        uid = uids.get_random_of_type(uids.Types.random_from_all)

        paymethod, card, context = test_data
        person_type = context.person_type.code
        product = context.product
        region = context.region
        currency_code = context.currency.iso_code

        client_id = steps.ClientSteps.create_multicurrency(currency_convert_type='COPY')
        steps.ClientSteps.link(client_id, uid.login)
        person_id = steps.PersonSteps.create(client_id, person_type)

        campaigns_list = [
            {'service_id': product.service.id, 'product_id': product.id, 'qty': Data.QTY, 'begin_dt': Data.BASE_DT}]
        invoice_id, external_invoice_id, total_invoice_sum, _ = steps.InvoiceSteps.create_force_invoice(
            client_id=client_id,
            person_id=person_id,
            campaigns_list=campaigns_list,
            paysys_id=context.paysys.id,
            invoice_dt=Data.BASE_DT)

        with check_mode(CheckMode.FAILED):
            paystep.pay_by(paymethod, product.service, user=uid, card=card, region_id=region.id,
                           invoice_id=invoice_id, data_for_checks={'invoice_id': invoice_id,
                                                                   'external_id': external_invoice_id,
                                                                   'total_sum': total_invoice_sum,
                                                                   'currency_iso_code': currency_code})

        payment_id = db.get_payments_by_invoice_id(invoice_id)[0]['id']
        export = steps.CommonSteps.export('CASH_REGISTER', 'Payment', payment_id)
        #
        # sn = export['input']['fn']['sn']
        # id = export['input']['id']
        # fp = export['input']['fp']
        # # second_export = steps.CommonSteps.export('CASH_REGISTER', 'Payment', payment_id)
        # # print second_export['output']
        # html_url = BASE_CHECK_RENDER_URL + '/fiscal_storages/{}/documents/{}/{}'.format(sn, id, fp)
        # mobile_url = BASE_CHECK_RENDER_URL + '/mobile/fiscal_storages/{}/documents/{}/{}'.format(sn, id, fp)
        # pdf_url = BASE_CHECK_RENDER_URL + '/pdf/fiscal_storages/{}/documents/{}/{}'.format(sn, id, fp)
        #
        # print html_url
        # print mobile_url


if __name__ == '__main__':
    pytest.main()
