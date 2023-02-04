# coding=utf-8

import datetime

import pytest

import btestlib.reporter as reporter
from btestlib.constants import Products, Paysyses, Regions, Processings, Services
from btestlib.utils import CheckMode, check_mode, aDict
from simpleapi.common.payment_methods import PaystepCard, \
    PaystepPayPal, PaystepWebMoney, PaystepLinkedCard, LinkedCard, TrustWebPage, Via
from simpleapi.data import features, stories
from simpleapi.data import uids_pool as uids
from simpleapi.common.utils import DataObject
from simpleapi.data.cards_pool import RBS, ING, Saferpay, Bilderlings, Prior, get_card, ALPHA_PAYSTEP_VISA
from simpleapi.steps import balance_steps as not_true_balance_steps
from simpleapi.steps import paystep_steps as paystep
from simpleapi.steps import simple_steps_bo as simple_bo
from simpleapi.steps import trust_steps as trust

__author__ = 'torvald'


# TODO: torvald: DataObject
class Data(object):
    DIRECT_FISH = Products.DIRECT_FISH


    QTY = 100.15
    BASE_DT = datetime.datetime.now()

    # firm_id = 1
    DIRECT_PH_CONTEXT = aDict({'person_type': 'ph',
                               'paysys': Paysyses.CC_PH_RUB,
                               'descr': 'DIRECT_PH'})

    DIRECT_UR_CONTEXT = aDict({'person_type': 'ur',
                               'paysys': Paysyses.CC_UR_RUB,
                               'descr': 'DIRECT_UR'})

    MARKET_PH_CONTEXT = aDict({'person_type': 'ph',
                               'paysys': Paysyses.CC_PH_RUB,
                               'descr': 'MARKET_PH',
                               'product': Products.MARKET})

    MARKET_UR_CONTEXT = aDict({'person_type': 'ur',
                               'paysys': Paysyses.CC_UR_RUB,
                               'descr': 'MARKET_UR',
                               'product': Products.MARKET})

    DIRECT_PH_WEBMONEY_CONTEXT = aDict({'person_type': 'ph',
                                        'paysys': Paysyses.WEBMONEY_WMR,
                                        'currency_code': 'WMR',
                                        'descr': 'DIRECT_PH_WEBMONEY'})

    # firm_id = 2
    DIRECT_PU_CONTEXT = aDict({'person_type': 'pu',
                               'paysys': Paysyses.CC_PU_UAH,
                               'descr': 'DIRECT_PU'})

    DIRECT_PU_WEBMONEY_CONTEXT = aDict({'person_type': 'pu',
                                        'paysys': Paysyses.WEBMONEY_WMU,
                                        'currency_code': 'WMU',
                                        'descr': 'DIRECT_PU_WEBMONEY'})

    # firm_id = 8
    DIRECT_TRU_CONTEXT = aDict({'person_type': 'tru',
                                'paysys': Paysyses.CC_TRU_TRY,
                                'region': Regions.TR,
                                'descr': 'DIRECT_TRU'})

    DIRECT_TRP_CONTEXT = aDict({'person_type': 'trp',
                                'paysys': Paysyses.CC_TRP_TRY,
                                'region': Regions.TR,
                                'descr': 'DIRECT_TRP'})

    # firm_id = 7
    DIRECT_SW_YTPH_USD_CONTEXT = aDict({'person_type': 'sw_ytph',
                                        'paysys': Paysyses.CC_SW_YTPH_USD,
                                        'descr': 'DIRECT_SW_YTPH_USD'})

    DIRECT_BY_YTPH_RUB_CONTEXT = aDict({'person_type': 'by_ytph',
                                        'paysys': Paysyses.CC_BY_YTPH_RUB,
                                        'descr': 'DIRECT_BY_YTPH_RUB'})

    DIRECT_SW_YT_CHF_CONTEXT = aDict({'person_type': 'sw_yt',
                                      'paysys': Paysyses.CC_SW_YT_CHF,
                                      'descr': 'DIRECT_SW_YT_CHF'})

    DIRECT_SW_PH_CHF_CONTEXT = aDict({'person_type': 'sw_ph',
                                      'paysys': Paysyses.CC_SW_PH_CHF,
                                      'descr': 'DIRECT_SW_PH_CHF'})

    DIRECT_SW_UR_CHF_CONTEXT = aDict({'person_type': 'sw_ur',
                                      'paysys': Paysyses.CC_SW_UR_CHF,
                                      'descr': 'DIRECT_SW_UR_CHF'})

    # firm_id = 4
    DIRECT_USP_PAYPAL_CONTEXT = aDict({'person_type': 'usp',
                                       'paysys': Paysyses.PAYPAL_USP_USD,
                                       'descr': 'DIRECT_USP_PAYPAL'})

    DIRECT_USU_PAYPAL_CONTEXT = aDict({'person_type': 'usu',
                                       'paysys': Paysyses.PAYPAL_USU_USD,
                                       'descr': 'DIRECT_USU_PAYPAL'})

    # firm_id = 16
    TOLOKA_SW_YTPH_PAYPAL_CONTEXT = aDict({'product': Products.TOLOKA,
                                           'person_type': 'sw_ytph',
                                           'paysys': Paysyses.PAYPAL_SW_YTPH_USD,
                                           'descr': 'TOLOKA_SW_YTPH_PAYPAL'})

    # firm_id = 16
    TOLOKA_SW_YTPH_CONTEXT = aDict({'product': Products.TOLOKA,
                                    'person_type': 'sw_ytph',
                                    'paysys': Paysyses.CC_SW_YTPH_USD,
                                    'descr': 'TOLOKA_SW_YTPH'})

    TOLOKA_SW_YT_CONTEXT = aDict({'product': Products.TOLOKA,
                                  'person_type': 'sw_yt',
                                  'paysys': Paysyses.CC_SW_YT_USD,
                                  'descr': 'TOLOKA_SW_YTPH'})

    DIRECT_BEL_CONTEXT_PH = aDict({'person_type': 'byp',
                                   'paysys': Paysyses.CC_BY_PH_BYN,
                                   'descr': 'DIRECT_BEL'})

    DIRECT_BEL_CONTEXT_UR = aDict({'person_type': 'byu',
                                   'paysys': Paysyses.CC_BY_UR_BYN,
                                   'descr': 'DIRECT_BEL'})

    test_data_professional_trust = [(paymethod, card, context, True) for paymethod, card, context in [
        (PaystepCard(Processings.TRUST), get_card(), DIRECT_PH_CONTEXT),
        # (PaystepCard(Processings.TRUST), RBS.Success.With3DS.card_mastecard, DIRECT_UR_CONTEXT),
        (PaystepLinkedCard(Processings.TRUST), get_card(), DIRECT_UR_CONTEXT),
        (PaystepCard(Processings.TRUST), get_card(), MARKET_PH_CONTEXT),
        (PaystepLinkedCard(Processings.TRUST), get_card(), MARKET_UR_CONTEXT),
        # TODO: fellow расширить список карт
    ]]
    # скоро дожно умереть и на смену этому прийти траст как процессинг
    test_data_professional_alpha = [(PaystepCard(Processings.ALPHA), card, DIRECT_UR_CONTEXT, True) for card in [
        ALPHA_PAYSTEP_VISA,
        RBS.Success.With3DS.card_mastecard,
        RBS.Success.Without3DS.card_mastercard,
        # эта карта стала не 3дс-ной в какой-то момент
        # RBS.Success.With3DS.card_maestro
    ]]

    test_data_professional_invalid_ph = [(PaystepCard(Processings.ALPHA), card, DIRECT_PH_CONTEXT, True) for card in [
        RBS.Failed.CommError.card,
        RBS.Failed.BlockedByLimit.card,
        RBS.Failed.CardLimitations.card,
        # RBS.Failed.NoFunds.card,  # TODO sunshineguy RBS отдает невалидный код ошибки для карты
        RBS.Failed.VeRes.card_visa,
    ]]

    test_data_professional_invalid_ur = [(PaystepCard(Processings.ALPHA), card, DIRECT_UR_CONTEXT, True) for card in [
        RBS.Failed.MsgFormat.card,
        RBS.Failed.NetworkRefused.card,
        RBS.Failed.LimitExceed.card,
        RBS.Failed.PaRes.card_mastercard,
    ]]

    test_data_professional_ua = [(PaystepCard(Processings.PRIVAT), card, DIRECT_PU_CONTEXT, True) for card in [
        # UKRAINE PAYSYSES WAS HIDDEN
        # Private.Valid.card_uah  # CreditCardUkrPhPaymentTest
    ]]

    test_data_professional_tr = [(PaystepCard(Processings.ING), card, context, False) for card, context in [
        (ING.Valid.card, DIRECT_TRU_CONTEXT),
        (ING.Valid.card, DIRECT_TRP_CONTEXT),
    ]]

    test_data_professional_sw = [(PaystepCard(Processings.SAFERPAY), card, context, True) for card, context in [
        (Saferpay.Valid.card_usd, DIRECT_SW_YTPH_USD_CONTEXT),
        (Saferpay.Valid.card_rub, DIRECT_BY_YTPH_RUB_CONTEXT),
        (Saferpay.Valid.card_chf, DIRECT_SW_YT_CHF_CONTEXT),
        (Saferpay.Valid.card_chf, DIRECT_SW_PH_CHF_CONTEXT),
        (Saferpay.Valid.card_chf, DIRECT_SW_UR_CHF_CONTEXT),
    ]]
    test_data_professional_sw_bilderlings = [(PaystepCard(Processings.BILDERLINGS), card, context, True) for
                                             card, context in [
                                                 (Bilderlings.Valid.card_visa1, TOLOKA_SW_YT_CONTEXT),
                                                 (Bilderlings.Valid.card_mastercard2, TOLOKA_SW_YTPH_CONTEXT)
                                             ]]

    # TODO: torvald
    # test_data_professional_sw_walletone = [(PaystepCard(Processings.SAFERPAY), card, context) for card, context in [
    # CreditCardNonresPhSwitzRubWalletOneTest.java
    # CreditCardNonresPhSwitzUsdWalletOneTest.java
    # CreditCardSwitzUrEurWalletOneTest.java
    # ]]

    test_data_professional_paypal = [(PaystepPayPal(), card, context, False) for card, context in [
        (None, DIRECT_USP_PAYPAL_CONTEXT),
        (None, DIRECT_USU_PAYPAL_CONTEXT),
        (None, TOLOKA_SW_YTPH_PAYPAL_CONTEXT)
    ]]

    test_data_professional_webmoney = [(PaystepWebMoney(), card, context, False) for card, context in [
        (None, DIRECT_PH_WEBMONEY_CONTEXT),
        # UKRAINE PAYSYSES WAS HIDDEN
        # (None, DIRECT_PU_WEBMONEY_CONTEXT),  # WmuPaymentTest.java
    ]]

    test_data_professional_bel_prior = [(PaystepCard(Processings.PRIOR), card, context, True) for card, context in [
        (Prior.Valid.card, DIRECT_BEL_CONTEXT_PH),
        (Prior.Valid.card, DIRECT_BEL_CONTEXT_UR),
    ]]

    # TODO: torvald
    # test_data_professional_yamoney = [(PaystepYaMoney(), card, context) for card, context in [
    # yaMoneyPaymentGrnTest.java
    # yaMoneyPaymentTest.java
    # yaMoneyPaymentUnmoderateTest.java
    # ]]

    # TODO: torvald
    # test_data_professional_qiwi = [(PaystepQiwi(), card, context) for card, context in [
    # SmsPaymentTest.java
    # qiwiPaymentTest.java
    # ]]

    test_data_trust_as_processing_via_api = [
        DataObject(service=Services.DIRECT,
                   paymethod=LinkedCard(card=get_card()),
                   user_type=uids.Types.random_from_all).new(
            balance_context=DIRECT_PH_CONTEXT),
        DataObject(service=Services.DIRECT,
                   paymethod=TrustWebPage(Via.card(card=get_card()), in_browser=True),
                   user_type=uids.Types.random_from_all).new(
            balance_context=DIRECT_PH_CONTEXT),
        DataObject(service=Services.DIRECT,
                   paymethod=TrustWebPage(Via.card(card=get_card()), in_browser=True),
                   user_type=uids.Types.anonymous).new(
            balance_context=DIRECT_UR_CONTEXT),
        DataObject(service=Services.DIRECT,
                   paymethod=TrustWebPage(Via.linked_card(card=get_card()), in_browser=True),
                   user_type=uids.Types.random_from_all).new(
            balance_context=DIRECT_UR_CONTEXT),
    ]


def ids_paymethod_context_card(val):
    paymethod, card, context, _ = val
    card_descr = card['descr'] if card else 'None'
    ids = "paymethod={} processing={} context={} card={}".format(paymethod.title, paymethod.processing,
                                                                 context.descr, card_descr)
    return ids


@reporter.feature(features.General.Paystep)
class TestPaystep(object):
    @reporter.story(stories.General.Payment)
    @pytest.mark.parametrize('test_data',
                             Data.test_data_professional_sw_bilderlings +
                             Data.test_data_professional_alpha +  # TODO: Перенастроили базу, как накатим - проверить, что все стало нормально и таймауты не падают
                             Data.test_data_professional_trust +  # TODO: sunshineguy Проверить исправления от Коли
                             Data.test_data_professional_invalid_ph +  # TODO: sunshineguy Невалидный код ошибки от RBS
                             Data.test_data_professional_invalid_ur +
                             Data.test_data_professional_ua +  # Muted
                             Data.test_data_professional_tr +
                             Data.test_data_professional_sw +  # TODO: Перенастроили базу, как накатим - проверить, что все стало нормально и таймауты не падают
                             # Data.test_data_professional_sw_walletone + # NOT READY
                             Data.test_data_professional_paypal +
                             Data.test_data_professional_webmoney +
                             # Data.test_data_professional_yamoney + # NOT READY
                             # Data.test_data_professional_qiwi + # NOT READY
                             Data.test_data_professional_bel_prior,
                             # TODO: Перенастроили базу, как накатим - проверить, что все стало нормально и таймауты не падают
                             ids=ids_paymethod_context_card)
    def test_base_payment_cycle(self, test_data):
        uid = uids.get_random_of_type(uids.Types.random_from_all)

        paymethod, card, context, should_postauthorize = test_data
        paymethod.init(service=Services.DIRECT, user=uid, card=card)
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

            # Для успешных платежей проверяем поставторизацию
            if should_postauthorize and card and card.get('is_valid', True):
                not_true_balance_steps.wait_until_postauthorize(invoice_id)

    @reporter.story(stories.General.TrustAsProcessing)
    @pytest.mark.parametrize('test_data', Data.test_data_trust_as_processing_via_api, ids=DataObject.ids)
    def test_payment_via_trust_as_processing(self, test_data):
        service, paymethod, context = test_data.service, \
                                      test_data.paymethod, \
                                      test_data.balance_context
        user = uids.get_random_of_type(test_data.user_type)

        paymethod.init(service, user)
        client_id, person_id, invoice_id, external_invoice_id, total_invoice_sum = \
            not_true_balance_steps.prepare_data_for_paystep(context=context, user=user,
                                                            person_type=context.person_type,
                                                            product=context.get('product', Data.DIRECT_FISH),
                                                            qty=Data.QTY, dt=Data.BASE_DT)

        transaction_id = simple_bo.create_payment_for_invoice(service, invoice_id, user,
                                                              paymethod_id=paymethod.id)['transaction_id']
        payment_url = simple_bo.start_trust_api_payment(service, transaction_id).get('payment_url')
        trust.pay_by(paymethod, service, user=user, payment_url=payment_url)
        simple_bo.wait_until_trust_api_payment_done(service, transaction_id)
        not_true_balance_steps.wait_until_postauthorize(invoice_id)


if __name__ == '__main__':
    pytest.main()
