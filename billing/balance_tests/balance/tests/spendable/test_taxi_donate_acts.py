# -*- coding: utf-8 -*-

__author__ = 'atkaya'

import pytest
from dateutil.relativedelta import relativedelta
from hamcrest import empty

import btestlib.reporter as reporter
from balance import balance_steps as steps
from balance.features import Features, AuditFeatures
from btestlib.data import defaults as default
from btestlib.data.partner_contexts import *
from btestlib.matchers import contains_dicts_equal_to


pytestmark = [
    reporter.feature(Features.TAXI, Features.DONATE, Features.SPENDABLE, Features.ACT),
    pytest.mark.tickets('BALANCE-22816, BALANCE-28558')
]

FIRST_MONTH = utils.Date.first_day_of_month() - relativedelta(months=2)
SECOND_MONTH = FIRST_MONTH + relativedelta(months=1)

PASSPORT_ID = default.PASSPORT_UID

AMOUNTS = [{'type': Pages.COUPON, 'payment_sum': Decimal('100.1'), 'refund_sum': Decimal('95.9')},
           {'type': Pages.SUBSIDY, 'payment_sum': Decimal('42.77'), 'refund_sum': Decimal('24.47')},
           {'type': Pages.BRANDING_SUBSIDY, 'payment_sum': Decimal('50.3'), 'refund_sum': Decimal('1.2')},
           {'type': Pages.GUARANTEE_FEE, 'payment_sum': Decimal('125.46'), 'refund_sum': Decimal('23.4')},
           {'type': Pages.TRIP_BONUS, 'payment_sum': Decimal('33.33'), 'refund_sum': Decimal('0.2')},
           {'type': Pages.PERSONAL_BONUS, 'payment_sum': Decimal('87.67'), 'refund_sum': Decimal('9.03')},
           {'type': Pages.DISCOUNT_TAXI, 'payment_sum': Decimal('45.5'), 'refund_sum': Decimal('3.8')},
           {'type': Pages.SUPPORT_COUPON, 'payment_sum': Decimal('1.2'), 'refund_sum': Decimal('0.6')},
           {'type': Pages.BOOKING_SUBSIDY, 'payment_sum': Decimal('204.54'), 'refund_sum': Decimal('3.5')},
           {'type': Pages.DRYCLEAN, 'payment_sum': Decimal('17.89'), 'refund_sum': Decimal('2.5')},
           {'type': Pages.CASHRUNNER, 'payment_sum': Decimal('294.28'), 'refund_sum': Decimal('5.9')},
           {'type': Pages.CARGO_SUBSIDY, 'payment_sum': Decimal('42.27'), 'refund_sum': Decimal('22.13')},
           {'type': Pages.CARGO_COUPON, 'payment_sum': Decimal('56.37'), 'refund_sum': Decimal('32.03')},
           {'type': Pages.DELIVERY_SUBSIDY, 'payment_sum': Decimal('22.27'), 'refund_sum': Decimal('10.13')},
           {'type': Pages.DELIVERY_COUPON, 'payment_sum': Decimal('46.37'), 'refund_sum': Decimal('17.03')},
           {'type': Pages.PARTNERS_LEARNING_CENTER, 'payment_sum': Decimal('124.33'), 'refund_sum': Decimal('69.96')},
           {'type': Pages.PARTNERS_MOTIVATION_PROGRAM, 'payment_sum': Decimal('70.76'), 'refund_sum': Decimal('62.14')},
           ]


SCOUT_AMOUNTS = [{'type': Pages.SCOUTS, 'payment_sum': Decimal('100.1'), 'refund_sum': Decimal('95.9')},
                 {'type': Pages.SCOUTS_SZ, 'payment_sum': Decimal('42.77'), 'refund_sum': Decimal('24.47')},
                 {'type': Pages.SCOUT_CARGO_SUBSIDY, 'payment_sum': Decimal('50.3'), 'refund_sum': Decimal('1.2')},
                 {'type': Pages.SCOUT_CARGO_SZ_SUBSIDY, 'payment_sum': Decimal('125.46'), 'refund_sum': Decimal('23.4')}]

# при конвертации для купонов и субсидий заполняются поля в t_partner_act_data ref_partner_reward_wo_nds и reference_currency
# такой случай проверяем отдельно
TAXI_UBER_BV_BY_BYN_CONTEXT_TO_CHECK_CONVERSION = TAXI_UBER_BV_BY_BYN_CONTEXT_SPENDABLE.new(
    currency=Currencies.USD
)

TAXI_UBER_BV_BYN_BY_BYN_CONTEXT_TO_CHECK_CONVERSION = TAXI_UBER_BV_BYN_BY_BYN_CONTEXT_SPENDABLE.new(
    currency=Currencies.USD
)


@pytest.mark.audit(reporter.feature(AuditFeatures.RV_C10_1_Taxi))
@pytest.mark.parametrize('context_general, context_spendable, nds', [
    pytest.mark.smoke(
        (TAXI_RU_CONTEXT, TAXI_RU_CONTEXT_SPENDABLE, NdsNew.YANDEX_RESIDENT)),
    (TAXI_RU_CONTEXT, TAXI_RU_CONTEXT_SPENDABLE, NdsNew.ZERO),
    pytest.mark.smoke(
        (TAXI_BV_GEO_USD_CONTEXT, TAXI_BV_GEO_USD_CONTEXT_SPENDABLE, NdsNew.ZERO)),
    (TAXI_BV_LAT_EUR_CONTEXT, TAXI_BV_LAT_EUR_CONTEXT_SPENDABLE, NdsNew.ZERO),
    (TAXI_UBER_BV_BY_BYN_CONTEXT, TAXI_UBER_BV_BY_BYN_CONTEXT_SPENDABLE, NdsNew.ZERO),
    (TAXI_UBER_BV_BYN_BY_BYN_CONTEXT, TAXI_UBER_BV_BYN_BY_BYN_CONTEXT_SPENDABLE, NdsNew.ZERO),
    (TAXI_UBER_BV_AZN_USD_CONTEXT, TAXI_UBER_BV_AZN_USD_CONTEXT_SPENDABLE, NdsNew.ZERO),
    (TAXI_UBER_BV_BYN_AZN_USD_CONTEXT, TAXI_UBER_BV_BYN_AZN_USD_CONTEXT_SPENDABLE, NdsNew.ZERO),
    (TAXI_UBER_BV_BY_BYN_CONTEXT, TAXI_UBER_BV_BY_BYN_CONTEXT_TO_CHECK_CONVERSION, NdsNew.ZERO),
    (TAXI_UBER_BV_BYN_BY_BYN_CONTEXT, TAXI_UBER_BV_BYN_BY_BYN_CONTEXT_TO_CHECK_CONVERSION, NdsNew.ZERO),
    (TAXI_ISRAEL_CONTEXT, TAXI_ISRAEL_CONTEXT_SPENDABLE, NdsNew.ISRAEL),
    (TAXI_ISRAEL_CONTEXT, TAXI_ISRAEL_CONTEXT_SPENDABLE, NdsNew.NOT_RESIDENT),
    (TAXI_YANGO_ISRAEL_CONTEXT, TAXI_YANGO_ISRAEL_CONTEXT_SPENDABLE, NdsNew.ISRAEL),
    (TAXI_YANGO_ISRAEL_CONTEXT, TAXI_YANGO_ISRAEL_CONTEXT_SPENDABLE, NdsNew.NOT_RESIDENT),
    (TAXI_YANDEX_GO_SRL_CONTEXT, TAXI_YANDEX_GO_SRL_CONTEXT_SPENDABLE, NdsNew.ROMANIA),
    (TAXI_MLU_EUROPE_ROMANIA_EUR_CONTEXT, TAXI_MLU_EUROPE_ROMANIA_EUR_CONTEXT_SPENDABLE, NdsNew.NOT_RESIDENT),
    (TAXI_MLU_EUROPE_ROMANIA_USD_CONTEXT, TAXI_MLU_EUROPE_ROMANIA_USD_CONTEXT_SPENDABLE, NdsNew.NOT_RESIDENT),
    (TAXI_MLU_EUROPE_ROMANIA_RON_CONTEXT, TAXI_MLU_EUROPE_ROMANIA_RON_CONTEXT_SPENDABLE, NdsNew.NOT_RESIDENT),
    (TAXI_GHANA_USD_CONTEXT, TAXI_GHANA_USD_CONTEXT_SPENDABLE, NdsNew.NOT_RESIDENT),
    (TAXI_BOLIVIA_USD_CONTEXT, TAXI_BOLIVIA_USD_CONTEXT_SPENDABLE, NdsNew.NOT_RESIDENT),
    (TAXI_YA_TAXI_CORP_KZ_KZT_CONTEXT, TAXI_YA_TAXI_CORP_KZ_KZT_CONTEXT_SPENDABLE, NdsNew.KAZAKHSTAN),
    (TAXI_ZA_USD_CONTEXT, TAXI_ZA_USD_CONTEXT_SPENDABLE, NdsNew.NOT_RESIDENT),
    (TAXI_BV_NOR_NOK_CONTEXT, TAXI_BV_NOR_NOK_CONTEXT_SPENDABLE, NdsNew.ZERO),
    (TAXI_BV_COD_EUR_CONTEXT, TAXI_BV_COD_EUR_CONTEXT_SPENDABLE, NdsNew.ZERO),
    (TAXI_MLU_EUROPE_CAMEROON_EUR_CONTEXT, TAXI_MLU_EUROPE_CAMEROON_EUR_CONTEXT_SPENDABLE, NdsNew.ZERO),
    (TAXI_MLU_EUROPE_CONGO_EUR_CONTEXT, TAXI_MLU_EUROPE_CONGO_EUR_CONTEXT_SPENDABLE, NdsNew.ZERO),
    (TAXI_MLU_EUROPE_ALGERIA_EUR_CONTEXT, TAXI_MLU_EUROPE_ALGERIA_EUR_CONTEXT_SPENDABLE, NdsNew.ZERO),
    (TAXI_BOLIVIA_USD_CONTEXT, TAXI_BOLIVIA_USD_CONTEXT_SPENDABLE, NdsNew.ZERO),
    (TAXI_MLU_EUROPE_SWE_SEK_CONTEXT, TAXI_MLU_EUROPE_SWE_SEK_CONTEXT_SPENDABLE_DONATE, NdsNew.ZERO),
    (TAXI_ARM_AZ_USD_CONTEXT, TAXI_ARM_AZ_USD_CONTEXT_SPENDABLE, NdsNew.ZERO),
    (TAXI_ARM_GEO_USD_CONTEXT, TAXI_ARM_GEO_USD_CONTEXT_SPENDABLE, NdsNew.ZERO),
    (TAXI_ARM_KGZ_USD_CONTEXT, TAXI_ARM_KGZ_USD_CONTEXT_SPENDABLE, NdsNew.ZERO),
    (TAXI_ARM_GHA_USD_CONTEXT, TAXI_ARM_GHA_USD_CONTEXT_SPENDABLE, NdsNew.ZERO),
    (TAXI_ARM_ZAM_USD_CONTEXT, TAXI_ARM_ZAM_USD_CONTEXT_SPENDABLE, NdsNew.ZERO),
    (TAXI_ARM_UZB_USD_CONTEXT, TAXI_ARM_UZB_USD_CONTEXT_SPENDABLE, NdsNew.ZERO),
    (TAXI_ARM_CMR_EUR_CONTEXT, TAXI_ARM_CMR_EUR_CONTEXT_SPENDABLE, NdsNew.ZERO),
    (TAXI_ARM_SEN_EUR_CONTEXT, TAXI_ARM_SEN_EUR_CONTEXT_SPENDABLE, NdsNew.ZERO),
    (TAXI_ARM_CIV_EUR_CONTEXT, TAXI_ARM_CIV_EUR_CONTEXT_SPENDABLE, NdsNew.ZERO),
    (TAXI_ARM_ANG_EUR_CONTEXT, TAXI_ARM_ANG_EUR_CONTEXT_SPENDABLE, NdsNew.ZERO),
    (TAXI_ARM_MD_EUR_CONTEXT, TAXI_ARM_MD_EUR_CONTEXT_SPENDABLE, NdsNew.ZERO),
    (TAXI_ARM_RS_EUR_CONTEXT, TAXI_ARM_RS_EUR_CONTEXT_SPENDABLE, NdsNew.ZERO),
    (TAXI_ARM_LT_EUR_CONTEXT, TAXI_ARM_LT_EUR_CONTEXT_SPENDABLE, NdsNew.ZERO),
    (TAXI_ARM_FIN_EUR_CONTEXT, TAXI_ARM_FIN_EUR_CONTEXT_SPENDABLE, NdsNew.ZERO),
    # (TAXI_ARM_BY_BYN_CONTEXT, TAXI_ARM_BY_BYN_CONTEXT_SPENDABLE, NdsNew.ZERO),
    # (TAXI_ARM_NOR_NOK_CONTEXT, TAXI_ARM_NOR_NOK_CONTEXT_SPENDABLE, NdsNew.ZERO),
    (TAXI_ARM_ARM_AMD_CONTEXT_RESIDENT, TAXI_ARM_ARM_AMD_CONTEXT_RESIDENT_SPENDABLE, NdsNew.ARMENIA)
], ids=lambda g, s, nds: s.name + '_NDS id = {}'.format(nds.nds_id))
def test_taxi_donate_acts(context_general, context_spendable, nds):
    client_id, person_id, contract_id = create_client_and_contract(context_general, context_spendable, nds)

    create_completions(context_spendable, client_id, person_id, contract_id, FIRST_MONTH)
    steps.CommonPartnerSteps.generate_partner_acts_fair(contract_id, FIRST_MONTH)

    create_completions(context_spendable, client_id, person_id, contract_id, FIRST_MONTH, coef=3)
    create_completions(context_spendable, client_id, person_id, contract_id, SECOND_MONTH, coef=4)
    steps.CommonPartnerSteps.generate_partner_acts_fair(contract_id, SECOND_MONTH)

    act_data = steps.CommonPartnerSteps.get_partner_act_data_by_contract_id(contract_id)
    expected_act_data = create_expected_acts(context_spendable, client_id, contract_id, FIRST_MONTH, nds) + \
                        create_expected_acts(context_spendable, client_id, contract_id, SECOND_MONTH, nds, coef=7)

    utils.check_that(act_data, contains_dicts_equal_to(expected_act_data), u'Сравниваем данные из акта с шаблоном')


def test_taxi_donate_quarter_act():
    context_general = TAXI_RU_CONTEXT
    context_spendable = TAXI_RU_CONTEXT_SPENDABLE

    start_dt = utils.Date.first_day_of_month() - relativedelta(years=1, month=1)
    end_dt = start_dt + relativedelta(months=2)

    client_id, person_id, contract_id = create_client_and_contract(context_general, context_spendable,
                                                                   context_spendable.nds, start_dt,
                                                                   SpendablePaymentType.QUARTERLY)

    create_completions(context_spendable, client_id, person_id, contract_id, start_dt)
    steps.CommonPartnerSteps.generate_partner_acts_fair(contract_id, start_dt)

    act_data = steps.CommonPartnerSteps.get_partner_act_data_by_contract_id(contract_id)
    utils.check_that(act_data, empty(), u'Проверяем, что актов нет')

    create_completions(context_spendable, client_id, person_id, contract_id, start_dt + relativedelta(months=2), coef=3)
    steps.CommonPartnerSteps.generate_partner_acts_fair(contract_id, start_dt + relativedelta(months=2))

    act_data = steps.CommonPartnerSteps.get_partner_act_data_by_contract_id(contract_id)
    expected_act_data = create_expected_quarter_acts(context_spendable, client_id, contract_id, start_dt, end_dt,
                                                     context_spendable.nds, coef=4)
    utils.check_that(act_data, contains_dicts_equal_to(expected_act_data), u'Сравниваем данные из акта с шаблоном')


@pytest.mark.audit(reporter.feature(AuditFeatures.RV_C10_1_Taxi))
@pytest.mark.parametrize('context_general, context_spendable, context_scouts, nds', [
    (TAXI_RU_CONTEXT, TAXI_RU_CONTEXT_SPENDABLE, SCOUTS_RU_CONTEXT, NdsNew.YANDEX_RESIDENT),
    (TAXI_RU_CONTEXT, TAXI_RU_CONTEXT_SPENDABLE, SCOUTS_RU_CONTEXT, NdsNew.ZERO),
    (TAXI_MLU_EUROPE_SWE_SEK_CONTEXT, TAXI_MLU_EUROPE_SWE_SEK_CONTEXT_SPENDABLE_DONATE,
     TAXI_MLU_EUROPE_SWE_SEK_CONTEXT_SPENDABLE_SCOUTS, NdsNew.ZERO),
    (TAXI_MLU_EUROPE_CAMEROON_EUR_CONTEXT, TAXI_MLU_EUROPE_CAMEROON_EUR_CONTEXT_SPENDABLE,
     TAXI_MLU_EUROPE_CAMEROON_EUR_CONTEXT_SPENDABLE_SCOUTS, NdsNew.ZERO),
    (TAXI_MLU_EUROPE_CONGO_EUR_CONTEXT, TAXI_MLU_EUROPE_CONGO_EUR_CONTEXT_SPENDABLE,
     TAXI_MLU_EUROPE_CONGO_EUR_CONTEXT_SPENDABLE_SCOUTS, NdsNew.ZERO),
    (TAXI_MLU_EUROPE_ALGERIA_EUR_CONTEXT, TAXI_MLU_EUROPE_ALGERIA_EUR_CONTEXT_SPENDABLE,
     TAXI_MLU_EUROPE_ALGERIA_EUR_CONTEXT_SPENDABLE_SCOUTS, NdsNew.ZERO),
    (TAXI_MLU_EUROPE_SENEGAL_EUR_CONTEXT, TAXI_MLU_EUROPE_SENEGAL_EUR_CONTEXT_SPENDABLE,
     TAXI_MLU_EUROPE_SENEGAL_EUR_CONTEXT_SPENDABLE_SCOUTS, NdsNew.ZERO),
    (TAXI_GHANA_USD_CONTEXT, TAXI_GHANA_USD_CONTEXT_SPENDABLE,
     TAXI_GHANA_USD_CONTEXT_SPENDABLE_SCOUTS, NdsNew.ZERO),
    (TAXI_BOLIVIA_USD_CONTEXT, TAXI_BOLIVIA_USD_CONTEXT_SPENDABLE,
     TAXI_BOLIVIA_USD_CONTEXT_SPENDABLE_SCOUTS, NdsNew.ZERO),
    (TAXI_BV_CIV_EUR_CONTEXT, TAXI_BV_CIV_EUR_CONTEXT_SPENDABLE,
     TAXI_BV_CIV_EUR_CONTEXT_SPENDABLE_SCOUTS, NdsNew.ZERO),
    (TAXI_YA_TAXI_CORP_KZ_KZT_CONTEXT, TAXI_YA_TAXI_CORP_KZ_KZT_CONTEXT_SPENDABLE,
     TAXI_YA_TAXI_CORP_KZ_KZT_CONTEXT_SPENDABLE_SCOUTS, NdsNew.KAZAKHSTAN),
    (TAXI_YA_TAXI_CORP_KZ_KZT_CONTEXT, TAXI_YA_TAXI_CORP_KZ_KZT_CONTEXT_SPENDABLE,
     TAXI_YA_TAXI_CORP_KZ_KZT_CONTEXT_SPENDABLE_SCOUTS, NdsNew.ZERO),
    (TAXI_UBER_BV_BYN_AZN_USD_CONTEXT, TAXI_UBER_BV_BYN_AZN_USD_CONTEXT_SPENDABLE,
     TAXI_UBER_BV_BYN_AZN_USD_CONTEXT_SPENDABLE_SCOUTS, NdsNew.ZERO),
    (TAXI_BV_GEO_USD_CONTEXT, TAXI_BV_GEO_USD_CONTEXT_SPENDABLE,
     TAXI_BV_GEO_USD_CONTEXT_SPENDABLE_SCOUTS, NdsNew.ZERO),
    (TAXI_BV_UZB_USD_CONTEXT, TAXI_BV_UZB_USD_CONTEXT_SPENDABLE,
     TAXI_BV_UZB_USD_CONTEXT_SPENDABLE_SCOUTS, NdsNew.ZERO),
    # (TAXI_MLU_EUROPE_ANGOLA_EUR_CONTEXT, TAXI_MLU_EUROPE_ANGOLA_EUR_CONTEXT_SPENDABLE,
    #  TAXI_MLU_EUROPE_ANGOLA_EUR_CONTEXT_SPENDABLE_SCOUTS, NdsNew.ZERO),
    # (TAXI_ZAM_USD_CONTEXT, TAXI_ZAM_USD_CONTEXT_SPENDABLE,
    #  TAXI_ZAM_USD_CONTEXT_SPENDABLE_SCOUTS, NdsNew.ZERO),
    (TAXI_RU_DELIVERY_CONTEXT, TAXI_RU_DELIVERY_CONTEXT_SPENDABLE,
     TAXI_RU_DELIVERY_CONTEXT_SPENDABLE_SCOUTS, NdsNew.YANDEX_RESIDENT),
    (TAXI_RU_DELIVERY_CONTEXT, TAXI_RU_DELIVERY_CONTEXT_SPENDABLE,
     TAXI_RU_DELIVERY_CONTEXT_SPENDABLE_SCOUTS, NdsNew.ZERO),
    (LOGISTICS_CLIENTS_RU_CONTEXT_GENERAL, LOGISTICS_PARTNERS_RU_CONTEXT_SPENDABLE,
     LOGISTICS_PARTNERS_RU_CONTEXT_SPENDABLE_SCOUTS, NdsNew.YANDEX_RESIDENT),
    (LOGISTICS_CLIENTS_RU_CONTEXT_GENERAL, LOGISTICS_PARTNERS_RU_CONTEXT_SPENDABLE,
     LOGISTICS_PARTNERS_RU_CONTEXT_SPENDABLE_SCOUTS, NdsNew.ZERO),
    (TAXI_DELIVERY_KZ_EUR_CONTEXT, TAXI_DELIVERY_KZ_EUR_CONTEXT_SPENDABLE,
     TAXI_DELIVERY_KZ_EUR_CONTEXT_SPENDABLE_SCOUTS, NdsNew.NOT_RESIDENT),
    (TAXI_DELIVERY_BY_EUR_CONTEXT, TAXI_DELIVERY_BY_EUR_CONTEXT_SPENDABLE,
     TAXI_DELIVERY_BY_EUR_CONTEXT_SPENDABLE_SCOUTS, NdsNew.NOT_RESIDENT),
    (TAXI_DELIVERY_ISR_EUR_CONTEXT, TAXI_DELIVERY_ISR_EUR_CONTEXT_SPENDABLE,
     TAXI_DELIVERY_ISR_EUR_CONTEXT_SPENDABLE_SCOUTS, NdsNew.NOT_RESIDENT),
    (TAXI_DELIVERY_UZB_EUR_CONTEXT, TAXI_DELIVERY_UZB_EUR_CONTEXT_SPENDABLE,
     TAXI_DELIVERY_UZB_EUR_CONTEXT_SPENDABLE_SCOUTS, NdsNew.NOT_RESIDENT),
    (TAXI_DELIVERY_CMR_EUR_CONTEXT, TAXI_DELIVERY_CMR_EUR_CONTEXT_SPENDABLE,
     TAXI_DELIVERY_CMR_EUR_CONTEXT_SPENDABLE_SCOUTS, NdsNew.NOT_RESIDENT),
    (TAXI_DELIVERY_ARM_EUR_CONTEXT, TAXI_DELIVERY_ARM_EUR_CONTEXT_SPENDABLE,
     TAXI_DELIVERY_ARM_EUR_CONTEXT_SPENDABLE_SCOUTS, NdsNew.NOT_RESIDENT),
    (TAXI_DELIVERY_KGZ_EUR_CONTEXT, TAXI_DELIVERY_KGZ_EUR_CONTEXT_SPENDABLE,
     TAXI_DELIVERY_KGZ_EUR_CONTEXT_SPENDABLE_SCOUTS, NdsNew.NOT_RESIDENT),
    (TAXI_DELIVERY_GHA_EUR_CONTEXT, TAXI_DELIVERY_GHA_EUR_CONTEXT_SPENDABLE,
     TAXI_DELIVERY_GHA_EUR_CONTEXT_SPENDABLE_SCOUTS, NdsNew.NOT_RESIDENT),
    (TAXI_DELIVERY_SEN_EUR_CONTEXT, TAXI_DELIVERY_SEN_EUR_CONTEXT_SPENDABLE,
     TAXI_DELIVERY_SEN_EUR_CONTEXT_SPENDABLE_SCOUTS, NdsNew.NOT_RESIDENT),
    (TAXI_DELIVERY_MXC_EUR_CONTEXT, TAXI_DELIVERY_MXC_EUR_CONTEXT_SPENDABLE,
     TAXI_DELIVERY_MXC_EUR_CONTEXT_SPENDABLE_SCOUTS, NdsNew.NOT_RESIDENT),
    (TAXI_DELIVERY_TR_EUR_CONTEXT, TAXI_DELIVERY_TR_EUR_CONTEXT_SPENDABLE,
     TAXI_DELIVERY_TR_EUR_CONTEXT_SPENDABLE_SCOUTS, NdsNew.NOT_RESIDENT),
    (TAXI_DELIVERY_PER_EUR_CONTEXT, TAXI_DELIVERY_PER_EUR_CONTEXT_SPENDABLE,
     TAXI_DELIVERY_PER_EUR_CONTEXT_SPENDABLE_SCOUTS, NdsNew.NOT_RESIDENT),
    (TAXI_DELIVERY_ZA_EUR_CONTEXT, TAXI_DELIVERY_ZA_EUR_CONTEXT_SPENDABLE,
     TAXI_DELIVERY_ZA_EUR_CONTEXT_SPENDABLE_SCOUTS, NdsNew.NOT_RESIDENT),
    # (TAXI_DELIVERY_UAE_EUR_CONTEXT, TAXI_DELIVERY_UAE_EUR_CONTEXT_SPENDABLE,
    #  TAXI_DELIVERY_UAE_EUR_CONTEXT_SPENDABLE_SCOUTS, NdsNew.NOT_RESIDENT),
    (TAXI_DELIVERY_ANG_EUR_CONTEXT, TAXI_DELIVERY_ANG_EUR_CONTEXT_SPENDABLE,
     TAXI_DELIVERY_ANG_EUR_CONTEXT_SPENDABLE_SCOUTS, NdsNew.NOT_RESIDENT),
    (TAXI_DELIVERY_ZAM_EUR_CONTEXT, TAXI_DELIVERY_ZAM_EUR_CONTEXT_SPENDABLE,
     TAXI_DELIVERY_ZAM_EUR_CONTEXT_SPENDABLE_SCOUTS, NdsNew.NOT_RESIDENT),
    (TAXI_DELIVERY_CIV_EUR_CONTEXT, TAXI_DELIVERY_CIV_EUR_CONTEXT_SPENDABLE,
     TAXI_DELIVERY_CIV_EUR_CONTEXT_SPENDABLE_SCOUTS, NdsNew.NOT_RESIDENT),
    (TAXI_DELIVERY_AZ_EUR_CONTEXT, TAXI_DELIVERY_AZ_EUR_CONTEXT_SPENDABLE,
     TAXI_DELIVERY_AZ_EUR_CONTEXT_SPENDABLE_SCOUTS, NdsNew.NOT_RESIDENT),
    (TAXI_DELIVERY_MD_EUR_CONTEXT, TAXI_DELIVERY_MD_EUR_CONTEXT_SPENDABLE,
     TAXI_DELIVERY_MD_EUR_CONTEXT_SPENDABLE_SCOUTS, NdsNew.NOT_RESIDENT),
    (TAXI_DELIVERY_RS_EUR_CONTEXT, TAXI_DELIVERY_RS_EUR_CONTEXT_SPENDABLE,
     TAXI_DELIVERY_RS_EUR_CONTEXT_SPENDABLE_SCOUTS, NdsNew.NOT_RESIDENT),
    (TAXI_DELIVERY_UZB_USD_CONTEXT, TAXI_DELIVERY_UZB_USD_CONTEXT_SPENDABLE,
     TAXI_DELIVERY_UZB_USD_CONTEXT_SPENDABLE_SCOUTS, NdsNew.NOT_RESIDENT),
    (TAXI_DELIVERY_ARM_USD_CONTEXT, TAXI_DELIVERY_ARM_USD_CONTEXT_SPENDABLE,
     TAXI_DELIVERY_ARM_USD_CONTEXT_SPENDABLE_SCOUTS, NdsNew.NOT_RESIDENT),
    (TAXI_DELIVERY_KGZ_USD_CONTEXT, TAXI_DELIVERY_KGZ_USD_CONTEXT_SPENDABLE,
     TAXI_DELIVERY_KGZ_USD_CONTEXT_SPENDABLE_SCOUTS, NdsNew.NOT_RESIDENT),
    (TAXI_DELIVERY_GHA_USD_CONTEXT, TAXI_DELIVERY_GHA_USD_CONTEXT_SPENDABLE,
     TAXI_DELIVERY_GHA_USD_CONTEXT_SPENDABLE_SCOUTS, NdsNew.NOT_RESIDENT),
    (TAXI_DELIVERY_ZAM_USD_CONTEXT, TAXI_DELIVERY_ZAM_USD_CONTEXT_SPENDABLE,
     TAXI_DELIVERY_ZAM_USD_CONTEXT_SPENDABLE_SCOUTS, NdsNew.NOT_RESIDENT),
    (TAXI_DELIVERY_AZ_USD_CONTEXT, TAXI_DELIVERY_AZ_USD_CONTEXT_SPENDABLE,
     TAXI_DELIVERY_AZ_USD_CONTEXT_SPENDABLE_SCOUTS, NdsNew.NOT_RESIDENT),
    (TAXI_BV_COD_EUR_CONTEXT, TAXI_BV_COD_EUR_CONTEXT_SPENDABLE,
     TAXI_BV_COD_EUR_CONTEXT_SPENDABLE_SCOUTS, NdsNew.NOT_RESIDENT),
    (TAXI_ARM_GEO_USD_CONTEXT, TAXI_ARM_GEO_USD_CONTEXT_SPENDABLE,
     TAXI_ARM_GEO_USD_CONTEXT_SPENDABLE_SCOUTS, NdsNew.NOT_RESIDENT),
    (TAXI_ARM_ARM_AMD_CONTEXT_RESIDENT, TAXI_ARM_ARM_AMD_CONTEXT_RESIDENT_SPENDABLE,
     TAXI_ARM_ARM_AMD_CONTEXT_RESIDENT_SPENDABLE_SCOUTS, NdsNew.ARMENIA),
    (TAXI_ARM_KGZ_USD_CONTEXT, TAXI_ARM_KGZ_USD_CONTEXT_SPENDABLE,
     TAXI_ARM_KGZ_USD_CONTEXT_SPENDABLE_SCOUTS, NdsNew.NOT_RESIDENT),
    (TAXI_ARM_GHA_USD_CONTEXT, TAXI_ARM_GHA_USD_CONTEXT_SPENDABLE,
     TAXI_ARM_GHA_USD_CONTEXT_SPENDABLE_SCOUTS, NdsNew.NOT_RESIDENT),
    (TAXI_ARM_ZAM_USD_CONTEXT, TAXI_ARM_ZAM_USD_CONTEXT_SPENDABLE,
     TAXI_ARM_ZAM_USD_CONTEXT_SPENDABLE_SCOUTS, NdsNew.NOT_RESIDENT),
    (TAXI_ARM_UZB_USD_CONTEXT, TAXI_ARM_UZB_USD_CONTEXT_SPENDABLE,
     TAXI_ARM_UZB_USD_CONTEXT_SPENDABLE_SCOUTS, NdsNew.NOT_RESIDENT),
    (TAXI_ARM_CMR_EUR_CONTEXT, TAXI_ARM_CMR_EUR_CONTEXT_SPENDABLE,
     TAXI_ARM_CMR_EUR_CONTEXT_SPENDABLE_SCOUTS, NdsNew.NOT_RESIDENT),
    (TAXI_ARM_SEN_EUR_CONTEXT, TAXI_ARM_SEN_EUR_CONTEXT_SPENDABLE,
     TAXI_ARM_SEN_EUR_CONTEXT_SPENDABLE_SCOUTS, NdsNew.NOT_RESIDENT),
    (TAXI_ARM_CIV_EUR_CONTEXT, TAXI_ARM_CIV_EUR_CONTEXT_SPENDABLE,
     TAXI_ARM_CIV_EUR_CONTEXT_SPENDABLE_SCOUTS, NdsNew.NOT_RESIDENT),
    (TAXI_ARM_ANG_EUR_CONTEXT, TAXI_ARM_ANG_EUR_CONTEXT_SPENDABLE,
     TAXI_ARM_ANG_EUR_CONTEXT_SPENDABLE_SCOUTS, NdsNew.NOT_RESIDENT),
    (TAXI_ARM_MD_EUR_CONTEXT, TAXI_ARM_MD_EUR_CONTEXT_SPENDABLE,
     TAXI_ARM_MD_EUR_CONTEXT_SPENDABLE_SCOUTS, NdsNew.NOT_RESIDENT),
    (TAXI_ARM_RS_EUR_CONTEXT, TAXI_ARM_RS_EUR_CONTEXT_SPENDABLE,
     TAXI_ARM_RS_EUR_CONTEXT_SPENDABLE_SCOUTS, NdsNew.NOT_RESIDENT),
    (TAXI_ARM_LT_EUR_CONTEXT, TAXI_ARM_LT_EUR_CONTEXT_SPENDABLE,
     TAXI_ARM_LT_EUR_CONTEXT_SPENDABLE_SCOUTS, NdsNew.NOT_RESIDENT),
    (TAXI_ARM_FIN_EUR_CONTEXT, TAXI_ARM_FIN_EUR_CONTEXT_SPENDABLE,
     TAXI_ARM_FIN_EUR_CONTEXT_SPENDABLE_SCOUTS, NdsNew.NOT_RESIDENT),
    (TAXI_ARM_AZ_USD_CONTEXT, TAXI_ARM_AZ_USD_CONTEXT_SPENDABLE,
     TAXI_ARM_AZ_USD_CONTEXT_SPENDABLE_SCOUTS, NdsNew.NOT_RESIDENT),
], ids=lambda g, s, scouts, nds: scouts.name + '_NDS id = {}'.format(nds.nds_id))
def test_taxi_donate_plus_scouts(context_general, context_spendable, context_scouts, nds):
    client_id, person_id, contract_id = create_client_and_contract(context_general, context_spendable, nds,
                                                                   add_scouts=True)

    create_completions(context_spendable, client_id, person_id, contract_id, FIRST_MONTH)
    create_completions(context_scouts, client_id, person_id, contract_id, FIRST_MONTH, items=SCOUT_AMOUNTS)

    steps.CommonPartnerSteps.generate_partner_acts_fair(contract_id, FIRST_MONTH)

    act_data = steps.CommonPartnerSteps.get_partner_act_data_by_contract_id(contract_id)
    expected_act_data = create_expected_acts(context_spendable, client_id, contract_id, FIRST_MONTH, nds) + \
                        create_expected_acts(context_scouts, client_id, contract_id, FIRST_MONTH, nds, items=SCOUT_AMOUNTS)
    utils.check_that(act_data, contains_dicts_equal_to(expected_act_data), u'Сравниваем данные из акта с шаблоном')


# ------------------------------------------------------------
# Utils
def create_client_and_contract(context_general, context_spendable, nds, start_dt=FIRST_MONTH,
                               payment_type=SpendablePaymentType.MONTHLY,
                               is_spendable_unsigned=False, add_scouts=False):

    additional_params = {'start_dt': start_dt}
    client_id, person_id, contract_id, _ = steps.ContractSteps.create_partner_contract(context_general,
                                                                               additional_params=additional_params)
    additional_params.update({'nds': nds.nds_id, 'payment_type': payment_type, 'link_contract_id': contract_id})
    if add_scouts:
        additional_params.update({'services': [Services.TAXI_DONATE.id, Services.SCOUTS.id]})

    _, spendable_person_id, spendable_contract_id, _ = steps.ContractSteps.create_partner_contract(context_spendable,
                                                                                                   client_id=client_id,
                                                                                                   unsigned=is_spendable_unsigned,
                                                                                                   additional_params=additional_params)

    return client_id, spendable_person_id, spendable_contract_id


def create_completions(context, client_id, person_id, contract_id, dt, coef=1, items=AMOUNTS):
    for item in items:
        steps.SimpleApi.create_fake_tpt_data(context, client_id, person_id, contract_id,
                                                               dt,
                                                               [{'amount': coef * item['payment_sum'],
                                                                 'transaction_type': TransactionType.PAYMENT,
                                                                 'payment_type': item['type'].payment_type},
                                                                {'amount': coef * item['refund_sum'],
                                                                 'transaction_type': TransactionType.REFUND,
                                                                 'payment_type': item['type'].payment_type}])


def create_expected_acts(context, client_id, contract_id, dt, nds, coef=Decimal('1'), items=AMOUNTS):
    expected_data = []

    currency_rate = steps.CurrencySteps.get_currency_rate(utils.Date.last_day_of_month(dt),
                                                          context.currency.char_code, context.region.currency.char_code,
                                                          context.region.rate_scr_id.id, iso_base=True)
    ref_currency = context.region.currency.char_code
    nds_coef = nds.koef_on_dt(dt)

    for item in items:
        total_amount = (item['payment_sum'] - item['refund_sum']) * coef
        reward = utils.dround(total_amount / nds_coef, 5)
        if item['type'].payment_type == PaymentType.COUPON or item['type'].payment_type == PaymentType.SUBSIDY:
            ref_reward = utils.dround(reward * currency_rate, 5)
        else:
            ref_reward = None
            ref_currency = None
        expected_data.append(steps.CommonData.create_expected_pad(context, client_id, contract_id, dt,
                                                                  partner_reward=reward, nds=nds,
                                                                  page_id=item['type'].id,
                                                                  description=item['type'].desc,
                                                                  reference_currency=ref_currency,
                                                                  ref_partner_reward_wo_nds=ref_reward))
    return expected_data


def create_expected_quarter_acts(context, client_id, contract_id, start_dt, end_dt, nds, coef=1):
    expected_acts = create_expected_acts(context, client_id, contract_id, start_dt, nds, coef=coef)
    for row in expected_acts:
        row['end_dt'] = utils.Date.last_day_of_month(end_dt)
    return expected_acts
