# coding: utf-8

from decimal import Decimal as D
import attr

import pytest
from balance import balance_steps as steps
from btestlib import utils
from btestlib.data.partner_contexts import HEALTH_PAYMENTS_PH_CONTEXT
from btestlib.constants import PaymentType, PaysysType
from cashmachines.data.constants import CMNds
from btestlib.matchers import contains_dicts_with_entries, equal_to
from simpleapi.common.payment_methods import VirtualPromocode


AMOUNT = D('666')

PAYMENT_DT = utils.Date.first_day_of_month()


# def create_person(context, client_id):
#     return steps.PersonSteps.create(client_id, context.person_type.code,
#                                     full=True,
#                                     inn_type=person_defaults.InnType.RANDOM,
#                                     name_type=person_defaults.NameType.RANDOM,
#                                     params={'is-partner': '0'},
#                                     )
#
#
# def create_contract(context, client_id, person_id, start_dt):
#     partner_integration_params = steps.CommonIntegrationSteps.DEFAULT_PARTNER_INTEGRATION_PARAMS_FOR_CREATE_CONTRACT
#     additional_params = dict(start_dt=start_dt, **context.special_contract_params)
#     return steps.ContractSteps.create_partner_contract(
#         context,
#         client_id=client_id, person_id=person_id,
#         partner_integration_params=partner_integration_params,
#         additional_params=additional_params)


@pytest.fixture(autouse=True)
def mock_trust(mock_simple_api):
    pass


@attr.s
class ProductParam(object):
    with_nds = attr.ib(type=bool)
    promo = attr.ib(type=bool)


@pytest.mark.parametrize('product_param', [ProductParam(with_nds=True, promo=False),
                                           ProductParam(with_nds=False, promo=False),
                                           ProductParam(with_nds=True, promo=True)],
                         ids=lambda x: str(x))
def test_payment(product_param):
    context = HEALTH_PAYMENTS_PH_CONTEXT
    client_id, person_id, contract_id = steps.CommonPartnerSteps.get_active_tech_ids(context.service)

    product_mapping_config = steps.CommonPartnerSteps.get_product_mapping_config(context.service)
    if product_param.promo:
        paymethod = VirtualPromocode()
        main_product_id = 511806
    else:
        paymethod = None
        mapping_label = 'default_product_mapping'
        product_label = 'default' if product_param.with_nds else 'nds_0'
        main_product_id = product_mapping_config[mapping_label][context.payment_currency.iso_code][product_label]

    fiscal_nds = CMNds.NDS_20 if product_param.with_nds else CMNds.NDS_NONE
    service_product_id = steps.SimpleApi.create_service_product(
        context.service,
        partner_id=client_id,
        fiscal_nds=fiscal_nds)

    invoice_eid = steps.InvoiceSteps.get_invoice_eid(contract_id, client_id, context.currency.char_code,
                                                     1 if product_param.with_nds else 0)

    service_order_id_list, trust_payment_id, purchase_token, payment_id = \
        steps.SimpleApi.create_multiple_trust_payments(context.service,
                                                       [service_product_id],
                                                       prices_list=[AMOUNT],
                                                       currency=context.currency,
                                                       paymethod=paymethod,
                                                       fiscal_nds_list=[fiscal_nds]
                                                       )

    expected_data = [steps.SimpleApi.create_expected_tpt_row(context, client_id, contract_id,
                                                             person_id, trust_payment_id, payment_id,
                                                             amount=AMOUNT,
                                                             payment_type=context.tpt_payment_type if not product_param.promo else PaymentType.NEW_PROMOCODE,
                                                             paysys_type_cc=context.tpt_paysys_type_cc if not product_param.promo else PaysysType.YANDEX,
                                                             internal=1,
                                                             invoice_eid=invoice_eid,
                                                             product_id=main_product_id,
                                                             ),
                     ]

    steps.CommonPartnerSteps.export_payment(payment_id)
    payment_data = steps.CommonPartnerSteps.get_thirdparty_transaction_by_payment_id(payment_id)
    key = lambda d: d['amount']

    payment_data.sort(key=key)
    expected_data.sort(key=key)

    utils.check_that(payment_data, contains_dicts_with_entries(expected_data), u'Сравниваем платеж с шаблоном')
