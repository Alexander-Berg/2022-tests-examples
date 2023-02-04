# -*- coding: utf-8 -*-
__author__ = 'sfreest'

import datetime
from decimal import Decimal as D

import datetime as dt
import json
import pytest
import uuid
import xmlrpclib

import btestlib.utils as utils
from balance import balance_steps as steps
from btestlib.matchers import contains_dicts_with_entries, equal_to
from btestlib import reporter
from btestlib.constants import PaymentType, YTSourceName, YTDefaultPath, TransactionType, TaxiOrderType, Services
from btestlib.data.partner_contexts import PVZ_RU_CONTEXT_SPENDABLE

SOURCE_NAME = YTSourceName.MARKET_SUBVENTIONS_SOURCE_NAME


def test_olol():
    # for payment_id in (
    #         6616629163,
    #         6616650187,
    #         6616650240,
    #         6616650354,
    #         6616650495,
    #         6616650610,
    #         6616653108,
    #         6616653434,
    #         6616653587,
    #         6616654071,
    #         6616655808,
    #         6616674675,
    #         6616674703,
    #
    # ):
    #     steps.CommonPartnerSteps.export_payment(payment_id)

    # такси ЛСД
    steps.ExportSteps.export_oebs(client_id=1355234904)
    steps.ExportSteps.export_oebs(person_id=19309702)
    steps.ExportSteps.export_oebs(contract_id=15556336)

    #AGENT_REWARD
    steps.ExportSteps.export_oebs(invoice_id=147183040)
    #DEPOSITION
    steps.ExportSteps.export_oebs(invoice_id=147183038)
    #YANDEX_SERVICE
    steps.ExportSteps.export_oebs(invoice_id=147183041)

    # такси ЛСЗ

    steps.ExportSteps.export_oebs(contract_id=15798024)
    steps.ExportSteps.export_oebs(person_id=19309702)
    steps.ExportSteps.export_oebs(invoice_id=147572797)

    # АЗС
    steps.ExportSteps.export_oebs(client_id=1355478807)
    steps.ExportSteps.export_oebs(person_id=19659323)
    steps.ExportSteps.export_oebs(contract_id=15835402)
    steps.ExportSteps.export_oebs(invoice_id=147630990)

    tt_ids = [
        193749030199,
        193749030209,
        193749097299,
        193749208979,
        193749208989,
        193749472219,
        193749209299,
        193749209309,
        193749518699,
        193749210639,
        193749210649,
        193749518749,
        193749241439,
        193749241449,
        193749472249,
        193749241979,
        193749241989,
        193749472279,
        193749518779,
        193749254819,
        193749254829,
        193749259989,
        193749259999,
        193749472309,
        193749263199,
        193749263209,
        193749472339,
        193749271209,
        193749271219,
        193749472369,
        193749308819,
        193749308829,
        193749518809,
        193749463849,
        193749463859,
        193749518849,
        193749464219,
        193749518879,
        193749464209,
    ]

    for tt_id in tt_ids:
        str_tt_id = '66' + str(tt_id)
        try:
            steps.ExportSteps.create_export_record(str_tt_id, classname='ThirdPartyTransaction', type='OEBS')
        except:
            pass
        steps.ExportSteps.export_oebs(transaction_id=str_tt_id)

    steps.ExportSteps.export_oebs(product_id=513305)
    steps.ExportSteps.export_oebs(act_id=155652255)


    # steps.CommonPartnerSteps.generate_partner_acts_fair_and_export(1355478807, 15835402, datetime.datetime(2022, 1, 31))




    # steps.ExportSteps.export_oebs(collateral_id='9548148')
    # steps.ExportSteps.export_oebs(manager_id=20453)
    # steps.ExportSteps.export_oebs(invoice_id=114260439)


    # yt_client = steps.YTSteps.create_yt_client()
    # prev_date = dt.datetime(2021, 1, 25)
    #
    # with reporter.step(u"Закрываем предыдущий ресурс (prev_date - 1 день) и процессим текущий prev_date"):
    #     steps.CommonPartnerSteps.create_partner_completions_resource(SOURCE_NAME, prev_date - dt.timedelta(days=1),
    #                                                                  {'finished': dt.datetime.now()})
    #     steps.CommonPartnerSteps.create_partner_completions_resource(SOURCE_NAME, prev_date)
    #     steps.CommonPartnerSteps.process_partners_completions(SOURCE_NAME, prev_date)
    #     prev_pcr = steps.CommonPartnerSteps.get_partner_completions_resource(SOURCE_NAME, prev_date)
    #     print(prev_pcr)


# from simpleapi.steps import trust_steps as trust
# from btestlib.data.simpleapi_defaults import DEFAULT_USER
# def test_bind_card_for_new_user():
#     user = DEFAULT_USER
#     card = {
#         'cardholder': 'TEST TEST',
#         'cvn': '126',
#         'expiration_month': '05',
#         'expiration_year': '2020',
#         'descr': 'emulator_card',
#         'type': 'MasterCard',
#         'card_number': '5469380041179762'
#     }
#     linked_cards, trust_payment_id = trust.process_binding(user=user, cards=card)
#     print linked_cards


