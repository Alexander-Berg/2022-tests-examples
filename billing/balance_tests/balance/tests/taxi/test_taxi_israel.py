# coding: utf-8

__author__ = 'a-vasin'

import pytest
from dateutil.relativedelta import relativedelta
from hamcrest import equal_to, contains_string

from balance import balance_steps as steps
from btestlib.data.partner_contexts import *
import btestlib.reporter as reporter
from balance.features import Features

CONTRACT_START_DT = utils.Date.first_day_of_month(datetime.now() - relativedelta(months=1))

PCT_BEFORE = Decimal('12.32')
PCT_AFTER = Decimal('42.66')


@pytest.mark.parametrize("is_offer", [True, False])
@pytest.mark.parametrize("context", [TAXI_ISRAEL_CONTEXT, TAXI_YANGO_ISRAEL_CONTEXT], ids=lambda c: c.name)
def test_general_israel_tax_pct(is_offer, context):

    client_id, person_id, contract_id, _ = \
        steps.ContractSteps.create_partner_contract(context, is_offer=is_offer, additional_params={
            'israel_tax_pct': PCT_BEFORE
        })
    check_pct(contract_id, collateral_num=0, expected_pct=PCT_BEFORE)

    steps.ContractSteps.create_collateral_real(contract_id, Collateral.ISRAEL_TAX_PCT_CHANGE_GENERAL, {
        'israel_tax_pct': PCT_AFTER
    })
    check_pct(contract_id, collateral_num=1, expected_pct=PCT_AFTER)


@pytest.mark.parametrize("context_general, context_spendable", [
    (TAXI_ISRAEL_CONTEXT, TAXI_ISRAEL_CONTEXT_SPENDABLE),
    (TAXI_CORP_ISRAEL_CONTEXT, TAXI_CORP_ISRAEL_CONTEXT_SPENDABLE),
    (TAXI_YANGO_ISRAEL_CONTEXT, TAXI_YANGO_ISRAEL_CONTEXT_SPENDABLE),
    (CORP_TAXI_YANGO_ISRAEL_CONTEXT_GENERAL_DECOUP, TAXI_CORP_YANGO_ISRAEL_CONTEXT_SPENDABLE),
], ids=lambda _, c: c.name)
@pytest.mark.parametrize("is_offer", [True, False])
def test_spendable_israel_tax_pct(context_general, context_spendable, is_offer):
    client_id, _, general_contract_id, _ = steps.ContractSteps.create_partner_contract(context_general)

    _, _, contract_id, _ = \
        steps.ContractSteps.create_partner_contract(context_spendable, client_id=client_id, is_offer=is_offer,
                                                    additional_params={
                                                        'israel_tax_pct': PCT_BEFORE,
                                                        'link_contract_id': general_contract_id
                                                    })
    check_pct(contract_id, collateral_num=0, expected_pct=PCT_BEFORE)

    steps.ContractSteps.create_collateral_real(contract_id, Collateral.ISRAEL_TAX_PCT_CHANGE_SPENDABLE, {
        'israel_tax_pct': PCT_AFTER,
    })
    check_pct(contract_id, collateral_num=1, expected_pct=PCT_AFTER)


@reporter.feature(Features.TO_UNIT)
@pytest.mark.parametrize("context", [TAXI_ISRAEL_CONTEXT, TAXI_YANGO_ISRAEL_CONTEXT], ids=lambda c: c.name)
def test_missing_pct(context):

    with pytest.raises(Exception) as error:
        steps.ContractSteps.create_partner_contract(context, remove_params=['israel_tax_pct'])

    utils.check_that(error.value.faultString, contains_string(u'Необходимо задать процент удержания налога'))


@reporter.feature(Features.TO_UNIT)
@pytest.mark.parametrize("pct", [Decimal('-1'), Decimal('100.01')], ids=['NEGATIVE', 'TOO_BIG'])
@pytest.mark.parametrize("context", [TAXI_ISRAEL_CONTEXT, TAXI_YANGO_ISRAEL_CONTEXT], ids=lambda c: c.name)
def test_wrong_pct(pct, context):

    with pytest.raises(Exception) as error:
        steps.ContractSteps.create_partner_contract(context, additional_params={
            'israel_tax_pct': pct
        })

    utils.check_that(error.value.faultString, contains_string(u'Процент должен иметь значение из отрезка [0, 100]'))


@reporter.feature(Features.TO_UNIT)
def test_wrong_contract():
    context = TAXI_RU_CONTEXT

    client_id, person_id, contract_id, _ = steps.ContractSteps.create_partner_contract(context)

    with pytest.raises(Exception) as error:
        steps.ContractSteps.create_collateral_real(contract_id, Collateral.ISRAEL_TAX_PCT_CHANGE_GENERAL, {
            'israel_tax_pct': PCT_AFTER
        })

    utils.check_that(
        error.value.faultString,
        contains_string(u'Данное допсоглашение доступно только для фирм YANDEX.GO ISRAEL Ltd, Yango.Taxi Ltd. и '
                        u'Yango Market Israel Ltd.при наличии платежных сервисов'))


# def test_general_export_oebs():
#     context = TAXI_ISRAEL_CONTEXT
#
#     client_id, person_id, contract_id, _ = \
#         steps.ContractSteps.create_partner_contract(context, additional_params={
#             'israel_tax_pct': PCT_BEFORE,
#             'start_dt': datetime.now()
#         })
#
#     steps.ContractSteps.create_collateral_real(contract_id, Collateral.ISRAEL_TAX_PCT_CHANGE_GENERAL, {
#         'israel_tax_pct': PCT_AFTER
#     })
#
#     steps.ExportSteps.export_oebs(client_id=client_id, person_id=person_id, contract_id=contract_id)
#
#
# @pytest.mark.parametrize("context_general, context_spendable", [
#     (TAXI_ISRAEL_CONTEXT, TAXI_ISRAEL_CONTEXT_SPENDABLE),
#     (TAXI_CORP_ISRAEL_CONTEXT, TAXI_CORP_ISRAEL_CONTEXT_SPENDABLE)
# ], ids=lambda _, c: c.name)
# def test_spendable_export_oebs(context_general, context_spendable):
#     client_id, general_person_id, general_contract_id, _ = steps.ContractSteps.create_partner_contract(context_general)
#
#     _, person_id, contract_id, _ = \
#         steps.ContractSteps.create_partner_contract(context_spendable, client_id=client_id, is_offer=True,
#                                                     additional_params={
#                                                         'israel_tax_pct': PCT_BEFORE,
#                                                         'link_contract_id': general_contract_id
#                                                     })
#
#     steps.ContractSteps.create_collateral_real(contract_id, Collateral.ISRAEL_TAX_PCT_CHANGE_SPENDABLE, {
#         'israel_tax_pct': PCT_AFTER,
#     })
#
#     steps.ExportSteps.export_oebs(client_id=client_id, person_id=general_person_id, contract_id=general_contract_id)
#     steps.ExportSteps.export_oebs(person_id=person_id, contract_id=contract_id)


def check_pct(contract_id, collateral_num, expected_pct):
    collateral_id = steps.ContractSteps.get_collateral_id(contract_id, collateral_num)
    pct = steps.ContractSteps.get_attribute_collateral(collateral_id, ContractAttributeType.NUM, 'ISRAEL_TAX_PCT', True)
    return utils.check_that(pct, equal_to(str(expected_pct)))
