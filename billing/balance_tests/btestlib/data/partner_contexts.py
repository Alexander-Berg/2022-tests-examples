# coding: utf-8

import json

import attr

from balance.balance_objects import Context
from btestlib.constants import *
from btestlib.data.defaults import *

from partner_schemes import SCHEMES

# набор используемых полей в контекстах, common задаем обязательно, в additional paysys и invoice_type обязательны для доходных,
# thirdparty заполняем, если у сервиса есть платежи, partner_act_data для расходных
# # common
# name = наименование контекста
# service = сервис Пример: Services.TAXI
# person_type = тип плательщика Пример: PersonTypes.UR
# firm = фирма Пример: Firms.TAXI_13
# currency = валюта договора Пример: Currencies.RUB
# nds = НДС Пример: NdsNew.DEFAULT
# contract_type = тип договора Пример: ContractSubtype.GENERAL
# special_contract_params = специфичные для сервиса параметры договора, если на ЛС, то обязтательно указать Пример: {'personal_account': 1, 'country': Regions.RU.id}
# contract_services = список сервисов для создания договора Пример: [Services.TAXI.id, Services.UBER.id]

# # additional
# paysys = способ оплаты Пример: Paysyses.BANK_UR_RUB_TAXI
# invoice_type = тип счета Пример: InvoiceType.PERSONAL_ACCOUNT
# region = регион, если применим к сервису/договору Пример: Regions.RU
# manager = менеджер, если специфично
# oebs_contract_type = id типа в оебс при выгрузке расходного договора с указанным сервисом Пример: 48

# # thirdparty
# payment_currency = валюта платежа Пример: Currencies.RUB
# tpt_payment_type = ожидаемый payment_type в tpt Пример: PaymentType.CARD
# tpt_paysys_type_cc = ожидаемый paysys_type_cc в tpt Пример: PaysysType.ALFA
# min_commission = минимальное АВ в платежах Пример: Decimal('0.01')
# precision = точность округления АВ Пример: 2
# currency_rate_src = источник для курсов Пример: CurrencyRateSource.CBRF

# # partner_act_data
# page_id = id продукта из t_page_data Пример: Pages.SCOUTS.id,
# pad_description = описание продукта из t_page_data Пример: Pages.SCOUTS.desc,
# pad_type_id = тип из t_page_data Пример: 6

# TAXI
# какие сервисы в какой таксишной фирме и стране используются можно посмотреть тут
# https://wiki.yandex-team.ru/taxi/balance/CountryFirmServiceMatrix/
TAXI_RU_CONTEXT = Context().new(
    # common
    name='TAXI_RU_CONTEXT',
    service=Services.TAXI,
    person_type=PersonTypes.UR,
    firm=Firms.TAXI_13,
    currency=Currencies.RUB,
    paysys=Paysyses.BANK_UR_RUB_TAXI,
    nds=NdsNew.DEFAULT,
    contract_type=ContractSubtype.GENERAL,
    invoice_type=InvoiceType.PERSONAL_ACCOUNT,
    special_contract_params={'personal_account': 1, 'country': Regions.RU.id,
                             'region': CountryRegion.RUS, 'nds_for_receipt': NdsNew.DEFAULT.nds_id},
    manager=None,
    region=Regions.RU,
    contract_services=[Services.TAXI.id, Services.UBER.id, Services.UBER_ROAMING.id,
                       Services.TAXI_VEZET.id, Services.TAXI_RUTAXI.id,
                       Services.TAXI_111.id, Services.TAXI_128.id, Services.TAXI_SVO.id, ],
    monetization_services=[Services.TAXI_111.id, Services.TAXI_128.id],
    # костыль для нескольких ЛС у заправок
    additional_paysys=Paysyses.CE_TAXI,
    zaxi_deposit_product=Products.ZAXI_DEPOSIT_RUB,
    # thirdparty
    payment_currency=Currencies.RUB,
    tpt_payment_type=PaymentType.CARD,
    tpt_paysys_type_cc=PaysysType.MONEY,  # PaysysType.ALFA
    min_commission=Decimal('0'),
    # АВ всегда 0, т.к. на commission_category не смотрим, а в договоре поля для процента для России нет
    precision=2,  # точность округления АВ
    currency_rate_src=CurrencyRateSource.CBR,
    migration_alias='taxi',
)

TAXI_RU_CONTEXT_CLONE = Context().new(
    # common
    name='TAXI_RU_CONTEXT_CLONE',
    service=Services.TAXI,
    person_type=PersonTypes.UR,
    firm=Firms.TAXI_13,
    currency=Currencies.RUB,
    paysys=Paysyses.BANK_UR_RUB_TAXI,
    nds=NdsNew.DEFAULT,
    contract_type=ContractSubtype.GENERAL,
    invoice_type=InvoiceType.PERSONAL_ACCOUNT,
    special_contract_params={'personal_account': 1, 'country': Regions.RU.id,
                             'region': CountryRegion.RUS, 'nds_for_receipt': NdsNew.DEFAULT.nds_id},
    manager=None,
    region=Regions.RU,
    contract_services=[Services.TAXI.id, Services.UBER.id, Services.UBER_ROAMING.id,
                       Services.TAXI_111.id, Services.TAXI_128.id, Services.TAXI_SVO.id, ],
    monetization_services=[Services.TAXI_111.id, Services.TAXI_128.id],
    # костыль для нескольких ЛС у заправок
    additional_paysys=Paysyses.CE_TAXI,
    # thirdparty
    payment_currency=Currencies.RUB,
    tpt_payment_type=PaymentType.CARD,
    tpt_paysys_type_cc=PaysysType.MONEY,  # PaysysType.ALFA
    min_commission=Decimal('0'),
    # АВ всегда 0, т.к. на commission_category не смотрим, а в договоре поля для процента для России нет
    precision=2,  # точность округления АВ
    currency_rate_src=CurrencyRateSource.CBR,
    migration_alias='taxi',
)

TAXI_RU_CONTEXT_SPENDABLE = TAXI_RU_CONTEXT.new(
    # common
    name='TAXI_RU_CONTEXT_SPENDABLE',
    service=Services.TAXI_DONATE,
    contract_type=ContractSubtype.SPENDABLE,
    special_contract_params={'country': Regions.RU.id, 'region': CountryRegion.RUS},
    contract_services=[Services.TAXI_DONATE.id],
    # thirdparty
    tpt_paysys_type_cc=PaysysType.TAXI,
    # partner_act_data
    pad_type_id=6,
)


TAXI_BV_GEO_USD_CONTEXT = TAXI_RU_CONTEXT.new(
    # common
    name='TAXI_BV_GEO_USD_CONTEXT',
    person_type=PersonTypes.EU_YT,
    firm=Firms.TAXI_BV_22,
    currency=Currencies.USD,
    paysys=Paysyses.BANK_UR_USD_TAXI_BV,
    nds=NdsNew.NOT_RESIDENT,
    special_contract_params={'personal_account': 1, 'country': Regions.GEO.id,
                             'partner_commission_pct2': Decimal('10.2')},
    region=Regions.GEO,
    contract_services=[Services.TAXI.id, Services.TAXI_111.id, Services.TAXI_128.id],
    monetization_services=[Services.TAXI_111.id, Services.TAXI_128.id],
    # thirdparty
    payment_currency=Currencies.USD,
    tpt_paysys_type_cc=PaysysType.MONEY,  # PaysysType.WALLET
    min_commission=Decimal('0.01'),  # минимальная комиссия АВ, если в договоре процент не 0 или вообще не задан
    currency_rate_src=CurrencyRateSource.ECB,
)

TAXI_ARM_GEO_USD_CONTEXT = TAXI_BV_GEO_USD_CONTEXT.new(
    # common
    name='TAXI_ARM_GEO_USD_CONTEXT',
    person_type=PersonTypes.AM_YT,
    firm=Firms.TAXI_CORP_ARM_122,
    paysys=Paysyses.BANK_AM_YT_USD_TAXI_CORP_ARM,
)

TAXI_ARM_GEO_USD_CONTEXT_SPENDABLE = TAXI_ARM_GEO_USD_CONTEXT.new(
    # common
    name='TAXI_ARM_GEO_USD_CONTEXT_SPENDABLE',
    service=Services.TAXI_DONATE,
    contract_type=ContractSubtype.SPENDABLE,
    special_contract_params={'country': Regions.GEO.id},
    contract_services=[Services.TAXI_DONATE.id],
    # thirdparty
    tpt_paysys_type_cc=PaysysType.TAXI,
    # partner_act_data
    pad_type_id=6,
)

TAXI_ARM_GEO_USD_CONTEXT_SPENDABLE_SCOUTS = TAXI_ARM_GEO_USD_CONTEXT_SPENDABLE.new(
    # common
    name='TAXI_ARM_GEO_USD_CONTEXT_SPENDABLE_SCOUTS',
    service=Services.SCOUTS,
    contract_services=[Services.SCOUTS.id],
    # thirdparty
    tpt_payment_type=PaymentType.SCOUT,
    oebs_contract_type=None,
    # partner_act_data
    page_id=Pages.SCOUTS.id,
    pad_description=Pages.SCOUTS.desc,
    # partner_act_data
    pad_type_id=4,
)

TAXI_ARM_ARM_AMD_CONTEXT = TAXI_BV_GEO_USD_CONTEXT.new(
    # common
    name='TAXI_ARM_ARM_AMD_CONTEXT',
    person_type=PersonTypes.AM_YT,
    firm=Firms.TAXI_CORP_ARM_122,
    currency=Currencies.AMD,
    paysys=Paysyses.BANK_AM_YT_AMD_TAXI_CORP_ARM,
    special_contract_params={'personal_account': 1, 'country': Regions.ARM.id,
                             'partner_commission_pct2': Decimal('10.2')},
    region=Regions.ARM,
    # thirdparty
    payment_currency=Currencies.AMD,
    currency_rate_src=CurrencyRateSource.CBA,
)

TAXI_ARM_ARM_AMD_CONTEXT_SPENDABLE = TAXI_ARM_ARM_AMD_CONTEXT.new(
    # common
    name='TAXI_ARM_ARM_AMD_CONTEXT_SPENDABLE',
    service=Services.TAXI_DONATE,
    contract_type=ContractSubtype.SPENDABLE,
    special_contract_params={'country': Regions.ARM.id},
    contract_services=[Services.TAXI_DONATE.id],
    # thirdparty
    tpt_paysys_type_cc=PaysysType.TAXI,
    # partner_act_data
    pad_type_id=6,
)

TAXI_ARM_ARM_AMD_CONTEXT_RESIDENT = TAXI_ARM_ARM_AMD_CONTEXT.new(
    # common
    name='TAXI_ARM_ARM_AMD_CONTEXT_RESIDENT',
    person_type=PersonTypes.AM_UR,
    paysys=Paysyses.BANK_AM_UR_AMD_TAXI_CORP_ARM,
    nds=NdsNew.ARMENIA,
)

TAXI_ARM_ARM_AMD_CONTEXT_RESIDENT_SPENDABLE = TAXI_ARM_ARM_AMD_CONTEXT_RESIDENT.new(
    # common
    name='TAXI_ARM_ARM_AMD_CONTEXT_RESIDENT_SPENDABLE',
    service=Services.TAXI_DONATE,
    contract_type=ContractSubtype.SPENDABLE,
    special_contract_params={'country': Regions.ARM.id},
    contract_services=[Services.TAXI_DONATE.id],
    # thirdparty
    tpt_paysys_type_cc=PaysysType.TAXI,
    # partner_act_data
    pad_type_id=6,
)

TAXI_ARM_ARM_AMD_CONTEXT_RESIDENT_SPENDABLE_SCOUTS = TAXI_ARM_ARM_AMD_CONTEXT_RESIDENT_SPENDABLE.new(
    # common
    name='TAXI_ARM_ARM_AMD_CONTEXT_RESIDENT_SPENDABLE_SCOUTS',
    service=Services.SCOUTS,
    contract_services=[Services.SCOUTS.id],
    # thirdparty
    tpt_payment_type=PaymentType.SCOUT,
    oebs_contract_type=None,
    # partner_act_data
    page_id=Pages.SCOUTS.id,
    pad_description=Pages.SCOUTS.desc,
    # partner_act_data
    pad_type_id=4,
)

# TAXI_ARM_BY_BYN_CONTEXT = TAXI_BV_GEO_USD_CONTEXT.new(
#     # todo: Для 122 фирмы и валюты BYN нет счета в t_bank_details, поэтому проверялось на фейковом счёте.
#     # todo: Перед запуском на проде нужно наинсертить реальный счёт.
#     # common
#     name='TAXI_ARM_BY_BYN_CONTEXT',
#     person_type=PersonTypes.AM_YT,
#     firm=Firms.TAXI_CORP_ARM_122,
#     currency=Currencies.BYN,
#     paysys=Paysyses.BANK_AM_YT_BYN_TAXI_CORP_ARM,
#     special_contract_params={'personal_account': 1, 'country': Regions.BY.id,
#                              'partner_commission_pct2': Decimal('10.2')},
#     region=Regions.BY,
#     contract_services=[Services.TAXI.id, Services.TAXI_111.id, Services.TAXI_128.id, Services.UBER.id],
#     monetization_services=[Services.TAXI_111.id, Services.TAXI_128.id],
#     # thirdparty
#     payment_currency=Currencies.BYN,
#     currency_rate_src=CurrencyRateSource.NBRB,
# )
#
# TAXI_ARM_BY_BYN_CONTEXT_SPENDABLE = TAXI_ARM_BY_BYN_CONTEXT.new(
#     # common
#     name='TAXI_ARM_BY_BYN_CONTEXT_SPENDABLE',
#     service=Services.TAXI_DONATE,
#     contract_type=ContractSubtype.SPENDABLE,
#     special_contract_params={'country': Regions.BY.id},
#     contract_services=[Services.TAXI_DONATE.id],
#     # thirdparty
#     tpt_paysys_type_cc=PaysysType.TAXI,
#     # partner_act_data
#     pad_type_id=6,
# )

TAXI_ARM_KGZ_USD_CONTEXT = TAXI_BV_GEO_USD_CONTEXT.new(
    # common
    name='TAXI_ARM_KGZ_USD_CONTEXT',
    person_type=PersonTypes.AM_YT,
    firm=Firms.TAXI_CORP_ARM_122,
    paysys=Paysyses.BANK_AM_YT_USD_TAXI_CORP_ARM,
    special_contract_params={'personal_account': 1, 'country': Regions.KGZ.id,
                             'partner_commission_pct2': Decimal('10.2')},
    region=Regions.KGZ,
)

TAXI_ARM_KGZ_USD_CONTEXT_SPENDABLE = TAXI_ARM_KGZ_USD_CONTEXT.new(
    # common
    name='TAXI_ARM_KGZ_USD_CONTEXT_SPENDABLE',
    service=Services.TAXI_DONATE,
    contract_type=ContractSubtype.SPENDABLE,
    special_contract_params={'country': Regions.KGZ.id},
    contract_services=[Services.TAXI_DONATE.id],
    # thirdparty
    tpt_paysys_type_cc=PaysysType.TAXI,
    # partner_act_data
    pad_type_id=6,
)

TAXI_ARM_KGZ_USD_CONTEXT_SPENDABLE_SCOUTS = TAXI_ARM_KGZ_USD_CONTEXT_SPENDABLE.new(
    # common
    name='TAXI_ARM_KGZ_USD_CONTEXT_SPENDABLE_SCOUTS',
    service=Services.SCOUTS,
    contract_services=[Services.SCOUTS.id],
    # thirdparty
    tpt_payment_type=PaymentType.SCOUT,
    oebs_contract_type=None,
    # partner_act_data
    page_id=Pages.SCOUTS.id,
    pad_description=Pages.SCOUTS.desc,
    # partner_act_data
    pad_type_id=4,
)

TAXI_ARM_GHA_USD_CONTEXT = TAXI_ARM_KGZ_USD_CONTEXT.new(
    # common
    name='TAXI_ARM_GHA_USD_CONTEXT',
    special_contract_params={'personal_account': 1, 'country': Regions.GHA.id,
                             'partner_commission_pct2': Decimal('10.2')},
    region=Regions.GHA,
)

TAXI_ARM_GHA_USD_CONTEXT_SPENDABLE = TAXI_ARM_GHA_USD_CONTEXT.new(
    # common
    name='TAXI_ARM_GHA_USD_CONTEXT_SPENDABLE',
    service=Services.TAXI_DONATE,
    contract_type=ContractSubtype.SPENDABLE,
    special_contract_params={'country': Regions.GHA.id},
    contract_services=[Services.TAXI_DONATE.id],
    # thirdparty
    tpt_paysys_type_cc=PaysysType.TAXI,
    # partner_act_data
    pad_type_id=6,
)

TAXI_ARM_GHA_USD_CONTEXT_SPENDABLE_SCOUTS = TAXI_ARM_GHA_USD_CONTEXT_SPENDABLE.new(
    # common
    name='TAXI_ARM_GHA_USD_CONTEXT_SPENDABLE_SCOUTS',
    service=Services.SCOUTS,
    contract_services=[Services.SCOUTS.id],
    # thirdparty
    tpt_payment_type=PaymentType.SCOUT,
    oebs_contract_type=None,
    # partner_act_data
    page_id=Pages.SCOUTS.id,
    pad_description=Pages.SCOUTS.desc,
    # partner_act_data
    pad_type_id=4,
)

TAXI_ARM_ZAM_USD_CONTEXT = TAXI_ARM_KGZ_USD_CONTEXT.new(
    # common
    name='TAXI_ARM_ZAM_USD_CONTEXT',
    special_contract_params={'personal_account': 1, 'country': Regions.ZAM.id,
                             'partner_commission_pct2': Decimal('10.2')},
    region=Regions.ZAM,
)

TAXI_ARM_ZAM_USD_CONTEXT_SPENDABLE = TAXI_ARM_ZAM_USD_CONTEXT.new(
    # common
    name='TAXI_ARM_ZAM_USD_CONTEXT_SPENDABLE',
    service=Services.TAXI_DONATE,
    contract_type=ContractSubtype.SPENDABLE,
    special_contract_params={'country': Regions.ZAM.id},
    contract_services=[Services.TAXI_DONATE.id],
    # thirdparty
    tpt_paysys_type_cc=PaysysType.TAXI,
    # partner_act_data
    pad_type_id=6,
)

TAXI_ARM_ZAM_USD_CONTEXT_SPENDABLE_SCOUTS = TAXI_ARM_ZAM_USD_CONTEXT_SPENDABLE.new(
    # common
    name='TAXI_ARM_ZAM_USD_CONTEXT_SPENDABLE_SCOUTS',
    service=Services.SCOUTS,
    contract_services=[Services.SCOUTS.id],
    # thirdparty
    tpt_payment_type=PaymentType.SCOUT,
    oebs_contract_type=None,
    # partner_act_data
    page_id=Pages.SCOUTS.id,
    pad_description=Pages.SCOUTS.desc,
    # partner_act_data
    pad_type_id=4,
)

TAXI_ARM_AZ_USD_CONTEXT = TAXI_ARM_KGZ_USD_CONTEXT.new(
    # common
    name='TAXI_ARM_AZ_USD_CONTEXT',
    special_contract_params={'personal_account': 1, 'country': Regions.AZ.id,
                             'partner_commission_pct2': Decimal('10.2')},
    contract_services=[Services.TAXI_111.id, Services.TAXI_128.id],
    region=Regions.AZ,
    is_resident=False,
)

TAXI_ARM_AZ_USD_CONTEXT_SPENDABLE = TAXI_ARM_AZ_USD_CONTEXT.new(
    # common
    name='TAXI_ARM_AZ_USD_CONTEXT_SPENDABLE',
    service=Services.TAXI_DONATE,
    contract_type=ContractSubtype.SPENDABLE,
    special_contract_params={'country': Regions.AZ.id},
    contract_services=[Services.TAXI_DONATE.id],
    # thirdparty
    tpt_paysys_type_cc=PaysysType.TAXI,
    # partner_act_data
    pad_type_id=6,
)

TAXI_ARM_AZ_USD_CONTEXT_SPENDABLE_SCOUTS = TAXI_ARM_AZ_USD_CONTEXT_SPENDABLE.new(
    # common
    name='TAXI_ARM_AZ_USD_CONTEXT_SPENDABLE_SCOUTS',
    service=Services.SCOUTS,
    contract_services=[Services.SCOUTS.id],
    # thirdparty
    tpt_payment_type=PaymentType.SCOUT,
    oebs_contract_type=None,
    # partner_act_data
    page_id=Pages.SCOUTS.id,
    pad_description=Pages.SCOUTS.desc,
    # partner_act_data
    pad_type_id=4,
)

TAXI_ARM_UZB_USD_CONTEXT = TAXI_ARM_KGZ_USD_CONTEXT.new(
    # common
    name='TAXI_ARM_UZB_USD_CONTEXT',
    special_contract_params={'personal_account': 1, 'country': Regions.UZB.id,
                             'partner_commission_pct2': Decimal('10.2')},
    region=Regions.UZB,
)

TAXI_ARM_UZB_USD_CONTEXT_SPENDABLE = TAXI_ARM_UZB_USD_CONTEXT.new(
    # common
    name='TAXI_ARM_UZB_USD_CONTEXT_SPENDABLE',
    service=Services.TAXI_DONATE,
    contract_type=ContractSubtype.SPENDABLE,
    special_contract_params={'country': Regions.UZB.id},
    contract_services=[Services.TAXI_DONATE.id],
    # thirdparty
    tpt_paysys_type_cc=PaysysType.TAXI,
    # partner_act_data
    pad_type_id=6,
)

TAXI_ARM_UZB_USD_CONTEXT_SPENDABLE_SCOUTS = TAXI_ARM_UZB_USD_CONTEXT_SPENDABLE.new(
    # common
    name='TAXI_ARM_UZB_USD_CONTEXT_SPENDABLE_SCOUTS',
    service=Services.SCOUTS,
    contract_services=[Services.SCOUTS.id],
    # thirdparty
    tpt_payment_type=PaymentType.SCOUT,
    oebs_contract_type=None,
    # partner_act_data
    page_id=Pages.SCOUTS.id,
    pad_description=Pages.SCOUTS.desc,
    # partner_act_data
    pad_type_id=4,
)

TAXI_ARM_CMR_EUR_CONTEXT = TAXI_BV_GEO_USD_CONTEXT.new(
    # common
    name='TAXI_ARM_CMR_EUR_CONTEXT',
    person_type=PersonTypes.AM_YT,
    firm=Firms.TAXI_CORP_ARM_122,
    currency=Currencies.EUR,
    paysys=Paysyses.BANK_AM_YT_EUR_TAXI_CORP_ARM,
    special_contract_params={'personal_account': 1, 'country': Regions.CMR.id,
                             'partner_commission_pct2': Decimal('10.2')},
    region=Regions.CMR,
    # thirdparty
    payment_currency=Currencies.EUR,
)

TAXI_ARM_CMR_EUR_CONTEXT_SPENDABLE = TAXI_ARM_CMR_EUR_CONTEXT.new(
    # common
    name='TAXI_ARM_CMR_EUR_CONTEXT_SPENDABLE',
    service=Services.TAXI_DONATE,
    contract_type=ContractSubtype.SPENDABLE,
    special_contract_params={'country': Regions.CMR.id},
    contract_services=[Services.TAXI_DONATE.id],
    # thirdparty
    tpt_paysys_type_cc=PaysysType.TAXI,
    # partner_act_data
    pad_type_id=6,
)

TAXI_ARM_CMR_EUR_CONTEXT_SPENDABLE_SCOUTS = TAXI_ARM_CMR_EUR_CONTEXT_SPENDABLE.new(
    # common
    name='TAXI_ARM_CMR_EUR_CONTEXT_SPENDABLE_SCOUTS',
    service=Services.SCOUTS,
    contract_services=[Services.SCOUTS.id],
    # thirdparty
    tpt_payment_type=PaymentType.SCOUT,
    oebs_contract_type=None,
    # partner_act_data
    page_id=Pages.SCOUTS.id,
    pad_description=Pages.SCOUTS.desc,
    # partner_act_data
    pad_type_id=4,
)

TAXI_ARM_SEN_EUR_CONTEXT = TAXI_ARM_CMR_EUR_CONTEXT.new(
    # common
    name='TAXI_ARM_SEN_EUR_CONTEXT',
    special_contract_params={'personal_account': 1, 'country': Regions.SEN.id,
                             'partner_commission_pct2': Decimal('10.2')},
    region=Regions.SEN,
)

TAXI_ARM_SEN_EUR_CONTEXT_SPENDABLE = TAXI_ARM_SEN_EUR_CONTEXT.new(
    # common
    name='TAXI_ARM_SEN_EUR_CONTEXT_SPENDABLE',
    service=Services.TAXI_DONATE,
    contract_type=ContractSubtype.SPENDABLE,
    special_contract_params={'country': Regions.SEN.id},
    contract_services=[Services.TAXI_DONATE.id],
    # thirdparty
    tpt_paysys_type_cc=PaysysType.TAXI,
    # partner_act_data
    pad_type_id=6,
)

TAXI_ARM_SEN_EUR_CONTEXT_SPENDABLE_SCOUTS = TAXI_ARM_SEN_EUR_CONTEXT_SPENDABLE.new(
    # common
    name='TAXI_ARM_SEN_EUR_CONTEXT_SPENDABLE_SCOUTS',
    service=Services.SCOUTS,
    contract_services=[Services.SCOUTS.id],
    # thirdparty
    tpt_payment_type=PaymentType.SCOUT,
    oebs_contract_type=None,
    # partner_act_data
    page_id=Pages.SCOUTS.id,
    pad_description=Pages.SCOUTS.desc,
    # partner_act_data
    pad_type_id=4,
)

TAXI_ARM_CIV_EUR_CONTEXT = TAXI_ARM_CMR_EUR_CONTEXT.new(
    # common
    name='TAXI_ARM_CIV_EUR_CONTEXT',
    special_contract_params={'personal_account': 1, 'country': Regions.CIV.id,
                             'partner_commission_pct2': Decimal('10.2')},
    region=Regions.CIV,
)

TAXI_ARM_CIV_EUR_CONTEXT_SPENDABLE = TAXI_ARM_CIV_EUR_CONTEXT.new(
    # common
    name='TAXI_ARM_CIV_EUR_CONTEXT_SPENDABLE',
    service=Services.TAXI_DONATE,
    contract_type=ContractSubtype.SPENDABLE,
    special_contract_params={'country': Regions.CIV.id},
    contract_services=[Services.TAXI_DONATE.id],
    # thirdparty
    tpt_paysys_type_cc=PaysysType.TAXI,
    # partner_act_data
    pad_type_id=6,
)

TAXI_ARM_CIV_EUR_CONTEXT_SPENDABLE_SCOUTS = TAXI_ARM_CIV_EUR_CONTEXT_SPENDABLE.new(
    # common
    name='TAXI_ARM_SEN_EUR_CONTEXT_SPENDABLE_SCOUTS',
    service=Services.SCOUTS,
    contract_services=[Services.SCOUTS.id],
    # thirdparty
    tpt_payment_type=PaymentType.SCOUT,
    oebs_contract_type=None,
    # partner_act_data
    page_id=Pages.SCOUTS.id,
    pad_description=Pages.SCOUTS.desc,
    # partner_act_data
    pad_type_id=4,
)

TAXI_ARM_ANG_EUR_CONTEXT = TAXI_ARM_CMR_EUR_CONTEXT.new(
    # common
    name='TAXI_ARM_ANG_EUR_CONTEXT',
    special_contract_params={'personal_account': 1, 'country': Regions.ANG.id,
                             'partner_commission_pct2': Decimal('10.2')},
    region=Regions.ANG,
)

TAXI_ARM_ANG_EUR_CONTEXT_SPENDABLE = TAXI_ARM_ANG_EUR_CONTEXT.new(
    # common
    name='TAXI_ARM_ANG_EUR_CONTEXT_SPENDABLE',
    service=Services.TAXI_DONATE,
    contract_type=ContractSubtype.SPENDABLE,
    special_contract_params={'country': Regions.ANG.id},
    contract_services=[Services.TAXI_DONATE.id],
    # thirdparty
    tpt_paysys_type_cc=PaysysType.TAXI,
    # partner_act_data
    pad_type_id=6,
)

TAXI_ARM_ANG_EUR_CONTEXT_SPENDABLE_SCOUTS = TAXI_ARM_ANG_EUR_CONTEXT_SPENDABLE.new(
    # common
    name='TAXI_ARM_SEN_EUR_CONTEXT_SPENDABLE_SCOUTS',
    service=Services.SCOUTS,
    contract_services=[Services.SCOUTS.id],
    # thirdparty
    tpt_payment_type=PaymentType.SCOUT,
    oebs_contract_type=None,
    # partner_act_data
    page_id=Pages.SCOUTS.id,
    pad_description=Pages.SCOUTS.desc,
    # partner_act_data
    pad_type_id=4,
)

TAXI_ARM_MD_EUR_CONTEXT = TAXI_ARM_CMR_EUR_CONTEXT.new(
    # common
    name='TAXI_ARM_MD_EUR_CONTEXT',
    special_contract_params={'personal_account': 1, 'country': Regions.MD.id,
                             'partner_commission_pct2': Decimal('10.2')},
    region=Regions.MD,
)

TAXI_ARM_MD_EUR_CONTEXT_SPENDABLE = TAXI_ARM_MD_EUR_CONTEXT.new(
    # common
    name='TAXI_ARM_MD_EUR_CONTEXT_SPENDABLE',
    service=Services.TAXI_DONATE,
    contract_type=ContractSubtype.SPENDABLE,
    special_contract_params={'country': Regions.MD.id},
    contract_services=[Services.TAXI_DONATE.id],
    # thirdparty
    tpt_paysys_type_cc=PaysysType.TAXI,
    # partner_act_data
    pad_type_id=6,
)

TAXI_ARM_MD_EUR_CONTEXT_SPENDABLE_SCOUTS = TAXI_ARM_MD_EUR_CONTEXT_SPENDABLE.new(
    # common
    name='TAXI_ARM_SEN_EUR_CONTEXT_SPENDABLE_SCOUTS',
    service=Services.SCOUTS,
    contract_services=[Services.SCOUTS.id],
    # thirdparty
    tpt_payment_type=PaymentType.SCOUT,
    oebs_contract_type=None,
    # partner_act_data
    page_id=Pages.SCOUTS.id,
    pad_description=Pages.SCOUTS.desc,
    # partner_act_data
    pad_type_id=4,
)

TAXI_ARM_RS_EUR_CONTEXT = TAXI_ARM_CMR_EUR_CONTEXT.new(
    # common
    name='TAXI_ARM_RS_EUR_CONTEXT',
    special_contract_params={'personal_account': 1, 'country': Regions.RS.id,
                             'partner_commission_pct2': Decimal('10.2')},
    region=Regions.RS,
)

TAXI_ARM_RS_EUR_CONTEXT_SPENDABLE = TAXI_ARM_RS_EUR_CONTEXT.new(
    # common
    name='TAXI_ARM_RS_EUR_CONTEXT_SPENDABLE',
    service=Services.TAXI_DONATE,
    contract_type=ContractSubtype.SPENDABLE,
    special_contract_params={'country': Regions.RS.id},
    contract_services=[Services.TAXI_DONATE.id],
    # thirdparty
    tpt_paysys_type_cc=PaysysType.TAXI,
    # partner_act_data
    pad_type_id=6,
)

TAXI_ARM_RS_EUR_CONTEXT_SPENDABLE_SCOUTS = TAXI_ARM_RS_EUR_CONTEXT_SPENDABLE.new(
    # common
    name='TAXI_ARM_SEN_EUR_CONTEXT_SPENDABLE_SCOUTS',
    service=Services.SCOUTS,
    contract_services=[Services.SCOUTS.id],
    # thirdparty
    tpt_payment_type=PaymentType.SCOUT,
    oebs_contract_type=None,
    # partner_act_data
    page_id=Pages.SCOUTS.id,
    pad_description=Pages.SCOUTS.desc,
    # partner_act_data
    pad_type_id=4,
)

TAXI_ARM_LT_EUR_CONTEXT = TAXI_ARM_CMR_EUR_CONTEXT.new(
    # common
    name='TAXI_ARM_LT_EUR_CONTEXT',
    special_contract_params={'personal_account': 1, 'country': Regions.LT.id,
                             'partner_commission_pct2': Decimal('10.2')},
    region=Regions.LT,
)

TAXI_ARM_LT_EUR_CONTEXT_SPENDABLE = TAXI_ARM_LT_EUR_CONTEXT.new(
    # common
    name='TAXI_ARM_LT_EUR_CONTEXT_SPENDABLE',
    service=Services.TAXI_DONATE,
    contract_type=ContractSubtype.SPENDABLE,
    special_contract_params={'country': Regions.LT.id},
    contract_services=[Services.TAXI_DONATE.id],
    # thirdparty
    tpt_paysys_type_cc=PaysysType.TAXI,
    # partner_act_data
    pad_type_id=6,
)

TAXI_ARM_LT_EUR_CONTEXT_SPENDABLE_SCOUTS = TAXI_ARM_LT_EUR_CONTEXT_SPENDABLE.new(
    # common
    name='TAXI_ARM_LT_EUR_CONTEXT_SPENDABLE_SCOUTS',
    service=Services.SCOUTS,
    contract_services=[Services.SCOUTS.id],
    # thirdparty
    tpt_payment_type=PaymentType.SCOUT,
    oebs_contract_type=None,
    # partner_act_data
    page_id=Pages.SCOUTS.id,
    pad_description=Pages.SCOUTS.desc,
    # partner_act_data
    pad_type_id=4,
)

TAXI_ARM_FIN_EUR_CONTEXT = TAXI_ARM_CMR_EUR_CONTEXT.new(
    # common
    name='TAXI_ARM_FIN_EUR_CONTEXT',
    special_contract_params={'personal_account': 1, 'country': Regions.FIN.id,
                             'partner_commission_pct2': Decimal('10.2')},
    region=Regions.FIN,
)

TAXI_ARM_FIN_EUR_CONTEXT_SPENDABLE = TAXI_ARM_FIN_EUR_CONTEXT.new(
    # common
    name='TAXI_ARM_FIN_EUR_CONTEXT_SPENDABLE',
    service=Services.TAXI_DONATE,
    contract_type=ContractSubtype.SPENDABLE,
    special_contract_params={'country': Regions.FIN.id},
    contract_services=[Services.TAXI_DONATE.id],
    # thirdparty
    tpt_paysys_type_cc=PaysysType.TAXI,
    # partner_act_data
    pad_type_id=6,
)

TAXI_ARM_FIN_EUR_CONTEXT_SPENDABLE_SCOUTS = TAXI_ARM_FIN_EUR_CONTEXT_SPENDABLE.new(
    # common
    name='TAXI_ARM_FIN_EUR_CONTEXT_SPENDABLE_SCOUTS',
    service=Services.SCOUTS,
    contract_services=[Services.SCOUTS.id],
    # thirdparty
    tpt_payment_type=PaymentType.SCOUT,
    oebs_contract_type=None,
    # partner_act_data
    page_id=Pages.SCOUTS.id,
    pad_description=Pages.SCOUTS.desc,
    # partner_act_data
    pad_type_id=4,
)

TAXI_BV_KZ_USD_CONTEXT = TAXI_BV_GEO_USD_CONTEXT.new(
    name='TAXI_BV_KZ_USD_CONTEXT',
    special_contract_params={'personal_account': 1, 'country': Regions.KZ.id,
                             'partner_commission_pct2': Decimal('10.2')},
    region=Regions.KZ,
)


TAXI_BV_ARM_USD_CONTEXT = TAXI_BV_GEO_USD_CONTEXT.new(
    name='TAXI_BV_ARM_USD_CONTEXT',
    special_contract_params={'personal_account': 1, 'country': Regions.ARM.id,
                             'partner_commission_pct2': Decimal('10.2')},
    region=Regions.ARM,
)


TAXI_BV_UZB_USD_CONTEXT = TAXI_BV_GEO_USD_CONTEXT.new(
    name='TAXI_BV_UZB_USD_CONTEXT',
    special_contract_params={'personal_account': 1, 'country': Regions.UZB.id,
                             'partner_commission_pct2': Decimal('10.2')},
    region=Regions.UZB,
)

TAXI_BV_UZB_USD_CONTEXT_SPENDABLE = TAXI_BV_UZB_USD_CONTEXT.new(
    # common
    name='TAXI_BV_UZB_USD_CONTEXT_SPENDABLE',
    service=Services.TAXI_DONATE,
    contract_type=ContractSubtype.SPENDABLE,
    special_contract_params={'country': Regions.UZB.id},
    contract_services=[Services.TAXI_DONATE.id],
    # thirdparty
    tpt_paysys_type_cc=PaysysType.TAXI,
    # partner_act_data
    pad_type_id=6,
)


TAXI_BV_UZB_USD_CONTEXT_SPENDABLE_SCOUTS = TAXI_BV_UZB_USD_CONTEXT_SPENDABLE.new(
    # common
    name='TAXI_BV_UZB_USD_CONTEXT_SPENDABLE_SCOUTS',
    service=Services.SCOUTS,
    contract_services=[Services.SCOUTS.id],
    # thirdparty
    tpt_payment_type=PaymentType.SCOUT,
    oebs_contract_type=None,
    # partner_act_data
    page_id=Pages.SCOUTS.id,
    pad_description=Pages.SCOUTS.desc,
    # partner_act_data
    pad_type_id=4,
)

TAXI_BV_RS_EUR_CONTEXT = TAXI_BV_GEO_USD_CONTEXT.new(
    name='TAXI_BV_RS_EUR_CONTEXT',
    currency=Currencies.EUR,
    paysys=Paysyses.BANK_UR_EUR_TAXI_BV,
    special_contract_params={'personal_account': 1, 'country': Regions.RS.id,
                             'partner_commission_pct2': Decimal('10.2')},
    region=Regions.RS,
)

TAXI_BV_EST_EUR_CONTEXT = TAXI_BV_GEO_USD_CONTEXT.new(
    name='TAXI_BV_EST_EUR_CONTEXT',
    currency=Currencies.EUR,
    paysys=Paysyses.BANK_UR_EUR_TAXI_BV,
    special_contract_params={'personal_account': 1, 'country': Regions.EST.id,
                             'partner_commission_pct2': Decimal('10.2')},
    region=Regions.EST,
)

TAXI_BV_LT_EUR_CONTEXT = TAXI_BV_GEO_USD_CONTEXT.new(
    name='TAXI_BV_LT_EUR_CONTEXT',
    currency=Currencies.EUR,
    paysys=Paysyses.BANK_UR_EUR_TAXI_BV,
    special_contract_params={'personal_account': 1, 'country': Regions.LT.id,
                             'partner_commission_pct2': Decimal('10.2')},
    region=Regions.LT,
)

TAXI_BV_FIN_EUR_CONTEXT = TAXI_BV_GEO_USD_CONTEXT.new(
    name='TAXI_BV_FIN_EUR_CONTEXT',
    currency=Currencies.EUR,
    paysys=Paysyses.BANK_UR_EUR_TAXI_BV,
    special_contract_params={'personal_account': 1, 'country': Regions.FIN.id,
                             'partner_commission_pct2': Decimal('10.2')},
    region=Regions.FIN,
)

TAXI_BV_KGZ_USD_CONTEXT = TAXI_BV_GEO_USD_CONTEXT.new(
    name='TAXI_BV_KGZ_USD_CONTEXT',
    special_contract_params={'personal_account': 1, 'country': Regions.KGZ.id,
                             'partner_commission_pct2': Decimal('10.2')},
    region=Regions.KGZ,
)

TAXI_BV_MD_EUR_CONTEXT = TAXI_BV_GEO_USD_CONTEXT.new(
    name='TAXI_BV_MD_EUR_CONTEXT',
    currency=Currencies.EUR,
    paysys=Paysyses.BANK_UR_EUR_TAXI_BV,
    special_contract_params={'personal_account': 1, 'country': Regions.MD.id,
                             'partner_commission_pct2': Decimal('10.2')},
    region=Regions.MD,
)

TAXI_BV_CIV_EUR_CONTEXT = TAXI_BV_GEO_USD_CONTEXT.new(
    name='TAXI_BV_CIV_EUR_CONTEXT',
    currency=Currencies.EUR,
    paysys=Paysyses.BANK_UR_EUR_TAXI_BV,
    special_contract_params={'personal_account': 1, 'country': Regions.CIV.id,
                             'partner_commission_pct2': Decimal('10.2')},
    region=Regions.CIV,
)

TAXI_BV_CIV_EUR_CONTEXT_SPENDABLE = TAXI_BV_CIV_EUR_CONTEXT.new(
    # common
    name='TAXI_BV_CIV_EUR_CONTEXT_SPENDABLE',
    service=Services.TAXI_DONATE,
    contract_type=ContractSubtype.SPENDABLE,
    special_contract_params={'country': Regions.CIV.id},
    contract_services=[Services.TAXI_DONATE.id],
    # thirdparty
    tpt_paysys_type_cc=PaysysType.TAXI,
    # partner_act_data
    pad_type_id=6,
)


TAXI_BV_CIV_EUR_CONTEXT_SPENDABLE_SCOUTS = TAXI_BV_CIV_EUR_CONTEXT_SPENDABLE.new(
    # common
    name='TAXI_BV_CIV_EUR_CONTEXT_SPENDABLE_SCOUTS',
    service=Services.SCOUTS,
    contract_services=[Services.SCOUTS.id],
    # thirdparty
    tpt_payment_type=PaymentType.SCOUT,
    oebs_contract_type=None,
    # partner_act_data
    page_id=Pages.SCOUTS.id,
    pad_description=Pages.SCOUTS.desc,
    # partner_act_data
    pad_type_id=4,
)

TAXI_BV_GEO_USD_CONTEXT_SPENDABLE = TAXI_BV_GEO_USD_CONTEXT.new(
    # common
    name='TAXI_BV_GEO_USD_CONTEXT_SPENDABLE',
    service=Services.TAXI_DONATE,
    contract_type=ContractSubtype.SPENDABLE,
    special_contract_params={'country': Regions.GEO.id},
    contract_services=[Services.TAXI_DONATE.id],
    # thirdparty
    tpt_paysys_type_cc=PaysysType.TAXI,
    # partner_act_data
    pad_type_id=6,
)

TAXI_BV_GEO_USD_CONTEXT_SPENDABLE_SCOUTS = TAXI_BV_GEO_USD_CONTEXT_SPENDABLE.new(
    # common
    name='TAXI_BV_GEO_USD_CONTEXT_SPENDABLE_SCOUTS',
    service=Services.SCOUTS,
    contract_services=[Services.SCOUTS.id],
    # thirdparty
    tpt_payment_type=PaymentType.SCOUT,
    oebs_contract_type=None,
    # partner_act_data
    page_id=Pages.SCOUTS.id,
    pad_description=Pages.SCOUTS.desc,
    # partner_act_data
    pad_type_id=4,
)

TAXI_BV_LAT_EUR_CONTEXT = TAXI_BV_GEO_USD_CONTEXT.new(
    # common
    name='TAXI_BV_LAT_EUR_CONTEXT',
    currency=Currencies.EUR,
    paysys=Paysyses.BANK_UR_EUR_TAXI_BV,
    special_contract_params={'personal_account': 1, 'country': Regions.LAT.id,
                             'partner_commission_pct2': Decimal('10.2')},
    region=Regions.LAT,
    # thirdparty
    payment_currency=Currencies.EUR,
)

TAXI_BV_LAT_EUR_CONTEXT_SPENDABLE = TAXI_BV_LAT_EUR_CONTEXT.new(
    # common
    name='TAXI_BV_LAT_EUR_CONTEXT_SPENDABLE',
    service=Services.TAXI_DONATE,
    contract_type=ContractSubtype.SPENDABLE,
    special_contract_params={'country': Regions.LAT.id},
    contract_services=[Services.TAXI_DONATE.id],
    # thirdparty
    tpt_paysys_type_cc=PaysysType.TAXI,
    # partner_act_data
    pad_type_id=6,
)

TAXI_KZ_CONTEXT = TAXI_RU_CONTEXT.new(
    # common
    name='TAXI_KZ_CONTEXT',
    person_type=PersonTypes.KZU,
    firm=Firms.TAXI_KAZ_24,
    currency=Currencies.KZT,
    paysys=Paysyses.BANK_KZ_UR_WO_NDS,
    nds=NdsNew.KAZAKHSTAN,
    special_contract_params={'personal_account': 1, 'country': Regions.KZ.id,
                             'partner_commission_pct2': Decimal('10.2')},
    region=Regions.KZ,
    contract_services=[Services.TAXI.id, Services.UBER.id, Services.UBER_ROAMING.id],
    # thirdparty
    min_commission=Decimal('15'),  # минимальная комиссия АВ
    tpt_paysys_type_cc=PaysysType.MONEY,  # PaysysType.ALFA
    currency_rate_src=CurrencyRateSource.NBKZ,
    payment_currency=Currencies.KZT,
)

TAXI_ARM_CONTEXT = TAXI_RU_CONTEXT.new(
    # common
    name='TAXI_ARM_CONTEXT',
    person_type=PersonTypes.AM_UR,
    firm=Firms.TAXI_AM_26,
    currency=Currencies.AMD,
    paysys=Paysyses.BANK_ARM_UR,
    nds=NdsNew.ARMENIA,
    special_contract_params={'personal_account': 1, 'country': Regions.ARM.id,
                             'partner_commission_pct2': Decimal('10.2')},
    region=Regions.ARM,
    contract_services=[Services.TAXI.id],
    monetization_services=[Services.TAXI_111.id, Services.TAXI_128.id],
    # thirdparty
    min_commission=Decimal('1'),  # минимальная комиссия АВ
    precision=0,  # точность округления АВ
    currency_rate_src=CurrencyRateSource.CBA,
    payment_currency=Currencies.AMD,
    tpt_paysys_type_cc=PaysysType.MONEY,  # PaysysType.PAYTURE
)


TAXI_UBER_BV_AZN_USD_CONTEXT = TAXI_RU_CONTEXT.new(
    # common
    name='TAXI_UBER_BV_AZN_USD_CONTEXT',
    service=Services.UBER,
    person_type=PersonTypes.EU_YT,
    firm=Firms.UBER_115,
    currency=Currencies.USD,
    paysys=Paysyses.BANK_UR_UBER_USD,
    nds=NdsNew.NOT_RESIDENT,
    special_contract_params={'personal_account': 1, 'country': Regions.AZ.id,
                             'partner_commission_pct2': Decimal('10.2')},
    region=Regions.AZ,
    contract_services=[Services.TAXI_111.id, Services.TAXI_128.id,
                       Services.UBER.id, Services.UBER_ROAMING.id],
    monetization_services=[Services.TAXI_111.id, Services.TAXI_128.id],
    # thirdparty
    min_commission=Decimal('0.01'),  # минимальная комиссия АВ, если в договоре процент не 0 или вообще не задан
    currency_rate_src=CurrencyRateSource.CBAR,
    payment_currency=Currencies.AZN,
    tpt_paysys_type_cc=PaysysType.MONEY,  # PaysysType.ECOMMPAY_EUROPE
)

TAXI_UBER_BV_AZN_USD_CONTEXT_SPENDABLE = TAXI_UBER_BV_AZN_USD_CONTEXT.new(
    # common
    name='TAXI_UBER_BV_AZN_USD_CONTEXT_SPENDABLE',
    service=Services.TAXI_DONATE,
    contract_type=ContractSubtype.SPENDABLE,
    special_contract_params={'country': Regions.AZ.id},
    contract_services=[Services.TAXI_DONATE.id],
    # thirdparty
    tpt_paysys_type_cc=PaysysType.TAXI,
    # partner_act_data
    pad_type_id=6,
)


TAXI_UBER_BV_BY_BYN_CONTEXT = TAXI_UBER_BV_AZN_USD_CONTEXT.new(
    # common
    name='TAXI_UBER_BV_BY_BYN_CONTEXT',
    currency=Currencies.BYN,
    service=Services.TAXI,
    paysys=Paysyses.BANK_UR_UBER_BYN,
    special_contract_params={'personal_account': 1, 'country': Regions.BY.id,
                             'partner_commission_pct2': Decimal('10.2')},
    region=Regions.BY,
    contract_services=[Services.TAXI.id, Services.TAXI_111.id, Services.TAXI_128.id,
                       Services.UBER.id, Services.UBER_ROAMING.id],
    monetization_services=[Services.TAXI_111.id, Services.TAXI_128.id],
    # thirdparty
    currency_rate_src=CurrencyRateSource.ECB,
    payment_currency=Currencies.BYN,
)


TAXI_UBER_BV_BY_BYN_CONTEXT_SPENDABLE = TAXI_UBER_BV_BY_BYN_CONTEXT.new(
    # common
    name='TAXI_UBER_BV_BY_BYN_CONTEXT_SPENDABLE',
    service=Services.TAXI_DONATE,
    contract_type=ContractSubtype.SPENDABLE,
    special_contract_params={'country': Regions.BY.id},
    contract_services=[Services.TAXI_DONATE.id],
    # thirdparty
    tpt_paysys_type_cc=PaysysType.TAXI,
    # partner_act_data
    pad_type_id=6,
)


# FIRM UBER_1088
TAXI_UBER_BV_BYN_AZN_USD_CONTEXT = TAXI_RU_CONTEXT.new(
    # common
    name='TAXI_UBER_BV_BYN_AZN_USD_CONTEXT',
    service=Services.UBER,
    person_type=PersonTypes.EU_YT,
    firm=Firms.UBER_1088,
    currency=Currencies.USD,
    paysys=Paysyses.BANK_UR_UBER_BYN_USD,
    nds=NdsNew.NOT_RESIDENT,
    special_contract_params={'personal_account': 1, 'country': Regions.AZ.id,
                             'partner_commission_pct2': Decimal('10.2')},
    region=Regions.AZ,
    contract_services=[Services.TAXI_111.id, Services.TAXI_128.id,
                       Services.UBER.id, Services.UBER_ROAMING.id],
    monetization_services=[Services.TAXI_111.id, Services.TAXI_128.id],
    # thirdparty
    min_commission=Decimal('0.01'),  # минимальная комиссия АВ, если в договоре процент не 0 или вообще не задан
    currency_rate_src=CurrencyRateSource.CBAR,
    payment_currency=Currencies.AZN,
    tpt_paysys_type_cc=PaysysType.MONEY,  # PaysysType.ECOMMPAY_EUROPE
)

TAXI_UBER_BV_BYN_AZN_USD_CONTEXT_SPENDABLE = TAXI_UBER_BV_BYN_AZN_USD_CONTEXT.new(
    # common
    name='TAXI_UBER_BV_BYN_AZN_USD_CONTEXT_SPENDABLE',
    service=Services.TAXI_DONATE,
    contract_type=ContractSubtype.SPENDABLE,
    special_contract_params={'country': Regions.AZ.id},
    contract_services=[Services.TAXI_DONATE.id],
    # thirdparty
    tpt_paysys_type_cc=PaysysType.TAXI,
    # partner_act_data
    pad_type_id=6,
)

TAXI_UBER_BV_BYN_AZN_USD_CONTEXT_SPENDABLE_SCOUTS = TAXI_UBER_BV_BYN_AZN_USD_CONTEXT_SPENDABLE.new(
    # common
    name='TAXI_UBER_BV_BYN_AZN_USD_CONTEXT_SPENDABLE_SCOUTS',
    service=Services.SCOUTS,
    contract_services=[Services.SCOUTS.id],
    payment_currency=Currencies.USD,
    # thirdparty
    tpt_payment_type=PaymentType.SCOUT,
    oebs_contract_type=None,
    # partner_act_data
    page_id=Pages.SCOUTS.id,
    pad_description=Pages.SCOUTS.desc,
    # partner_act_data
    pad_type_id=4,
)

TAXI_UBER_BV_BYN_BY_BYN_CONTEXT = TAXI_UBER_BV_BYN_AZN_USD_CONTEXT.new(
    # common
    name='TAXI_UBER_BV_BYN_BY_BYN_CONTEXT',
    currency=Currencies.BYN,
    service=Services.TAXI,
    paysys=Paysyses.BANK_UR_UBER_BYN_BYN,
    special_contract_params={'personal_account': 1, 'country': Regions.BY.id,
                             'partner_commission_pct2': Decimal('10.2')},
    region=Regions.BY,
    contract_services=[Services.TAXI.id, Services.TAXI_111.id, Services.TAXI_128.id,
                       Services.UBER.id, Services.UBER_ROAMING.id],
    monetization_services=[Services.TAXI_111.id, Services.TAXI_128.id],
    # thirdparty
    currency_rate_src=CurrencyRateSource.ECB,
    payment_currency=Currencies.BYN,
)


TAXI_UBER_BV_BYN_BY_BYN_CONTEXT_SPENDABLE = TAXI_UBER_BV_BYN_BY_BYN_CONTEXT.new(
    # common
    name='TAXI_UBER_BV_BYN_BY_BYN_CONTEXT_SPENDABLE',
    service=Services.TAXI_DONATE,
    contract_type=ContractSubtype.SPENDABLE,
    special_contract_params={'country': Regions.BY.id},
    contract_services=[Services.TAXI_DONATE.id],
    # thirdparty
    tpt_paysys_type_cc=PaysysType.TAXI,
    # partner_act_data
    pad_type_id=6,
)


TAXI_REQUEST_CONTEXT_SPENDABLE = TAXI_RU_CONTEXT_SPENDABLE.new(
    # common
    name='TAXI_REQUEST_CONTEXT_SPENDABLE',
    service=Services.TAXI_REQUEST,
    contract_services=[Services.TAXI_REQUEST.id],
    nds=NdsNew.ZERO,
)


TAXI_ISRAEL_CONTEXT = Context().new(
    # common
    name='TAXI_ISRAEL_CONTEXT',
    service=Services.TAXI,
    person_type=PersonTypes.IL_UR,
    firm=Firms.YANDEX_GO_ISRAEL_35,
    currency=Currencies.ILS,
    paysys=Paysyses.BANK_IL_UR_ILS,
    nds=NdsNew.ISRAEL,
    contract_type=ContractSubtype.GENERAL,
    invoice_type=InvoiceType.PERSONAL_ACCOUNT,
    special_contract_params={'personal_account': 1, 'country': Regions.ISR.id,
                             'partner_commission_pct2': Decimal('10.2'),
                             'israel_tax_pct': Decimal('7.1')
    },
    region=Regions.ISR,
    contract_services=[Services.TAXI.id, Services.TAXI_111.id, Services.TAXI_128.id],
    monetization_services=[Services.TAXI_111.id, Services.TAXI_128.id],
    # thirdparty
    payment_currency=Currencies.ILS,
    tpt_payment_type=PaymentType.CARD,
    tpt_paysys_type_cc=PaysysType.MONEY,
    min_commission=Decimal('0.01'),
    precision=2,  # точность округления АВ
    currency_rate_src=CurrencyRateSource.BOI,
    migration_alias='taxi',
)

TAXI_YANGO_ISRAEL_CONTEXT = TAXI_ISRAEL_CONTEXT.new(
    name='TAXI_YANGO_ISRAEL_CONTEXT',
    firm=Firms.YANGO_ISRAEL_1090,
    paysys=Paysyses.BANK_YANGO_IL_UR_ILS,
)

TAXI_CORP_ISRAEL_CONTEXT = TAXI_ISRAEL_CONTEXT.new(
    name='TAXI_CORP_ISRAEL_CONTEXT',
    service=Services.TAXI_CORP_CLIENTS,
    contract_services=[Services.TAXI_CORP.id, Services.TAXI_CORP_CLIENTS.id],
    special_contract_params={
        'personal_account': 1,
        'country': Regions.ISR.id,
        'partner_commission_pct2': Decimal('10.2')
    },
)

TAXI_GHANA_USD_CONTEXT = Context().new(
    # common
    name='TAXI_GHANA_USD_CONTEXT',
    service=Services.TAXI,
    person_type=PersonTypes.EU_YT,
    firm=Firms.MLU_AFRICA_126,
    currency=Currencies.USD,
    paysys=Paysyses.BANK_UR_USD_MLU_AFRICA,
    nds=NdsNew.NOT_RESIDENT,
    contract_type=ContractSubtype.GENERAL,
    invoice_type=InvoiceType.PERSONAL_ACCOUNT,
    special_contract_params={'personal_account': 1, 'country': Regions.GHA.id,
                             'partner_commission_pct2': Decimal('10.2')},
    region=Regions.GHA,
    contract_services=[Services.TAXI.id, Services.TAXI_111.id, Services.TAXI_128.id,
                       Services.UBER.id, Services.UBER_ROAMING.id],
    monetization_services=[Services.TAXI_111.id, Services.TAXI_128.id],
    # thirdparty
    payment_currency=Currencies.GHS,
    tpt_payment_type=PaymentType.CARD,
    tpt_paysys_type_cc=PaysysType.MONEY,
    min_commission=Decimal('0.01'),
    precision=2,  # точность округления АВ
    currency_rate_src=CurrencyRateSource.OANDA,
    migration_alias='taxi',
)


TAXI_GHANA_USD_CONTEXT_SPENDABLE = TAXI_GHANA_USD_CONTEXT.new(
    # common
    name='TAXI_GHANA_USD_CONTEXT_SPENDABLE',
    service=Services.TAXI_DONATE,
    contract_type=ContractSubtype.SPENDABLE,
    special_contract_params={'country': Regions.GHA.id, 'partner_commission_pct2': Decimal('10.2')},
    contract_services=[Services.TAXI_DONATE.id],
    # thirdparty
    payment_currency=Currencies.USD,
    tpt_paysys_type_cc=PaysysType.TAXI,
    # partner_act_data
    pad_type_id=6,
)

TAXI_GHANA_USD_CONTEXT_SPENDABLE_SCOUTS = TAXI_GHANA_USD_CONTEXT_SPENDABLE.new(
    # common
    name='TAXI_GHANA_USD_CONTEXT_SPENDABLE_SCOUTS',
    service=Services.SCOUTS,
    contract_services=[Services.SCOUTS.id],
    # thirdparty
    tpt_payment_type=PaymentType.SCOUT,
    oebs_contract_type=None,
    # partner_act_data
    page_id=Pages.SCOUTS.id,
    pad_description=Pages.SCOUTS.desc,
    # partner_act_data
    pad_type_id=4,
)

TAXI_BOLIVIA_USD_CONTEXT = Context().new(
    # common
    name='TAXI_BOLIVIA_USD_CONTEXT',
    service=Services.TAXI,
    person_type=PersonTypes.EU_YT,
    firm=Firms.MLU_AFRICA_126,
    currency=Currencies.USD,
    paysys=Paysyses.BANK_UR_USD_MLU_AFRICA,
    nds=NdsNew.NOT_RESIDENT,
    contract_type=ContractSubtype.GENERAL,
    invoice_type=InvoiceType.PERSONAL_ACCOUNT,
    special_contract_params={'personal_account': 1, 'country': Regions.BOL.id,
                             'partner_commission_pct2': Decimal('10.2')},
    region=Regions.BOL,
    contract_services=[Services.TAXI.id, Services.TAXI_111.id, Services.TAXI_128.id],
    monetization_services=[Services.TAXI_111.id, Services.TAXI_128.id],
    # thirdparty
    payment_currency=Currencies.BOB,
    tpt_payment_type=PaymentType.CARD,
    tpt_paysys_type_cc=PaysysType.MONEY,
    min_commission=Decimal('0.01'),
    precision=2,  # точность округления АВ
    currency_rate_src=CurrencyRateSource.OANDA,
    migration_alias='taxi',
)

TAXI_BOLIVIA_USD_CONTEXT_SPENDABLE = TAXI_BOLIVIA_USD_CONTEXT.new(
    # common
    name='TAXI_BOLIVIA_USD_CONTEXT_SPENDABLE',
    service=Services.TAXI_DONATE,
    contract_type=ContractSubtype.SPENDABLE,
    special_contract_params={'country': Regions.BOL.id, 'partner_commission_pct2': Decimal('10.2')},
    contract_services=[Services.TAXI_DONATE.id],
    # thirdparty
    payment_currency=Currencies.USD,
    tpt_paysys_type_cc=PaysysType.TAXI,
    # partner_act_data
    pad_type_id=6,
)

TAXI_BOLIVIA_USD_CONTEXT_SPENDABLE_SCOUTS = TAXI_BOLIVIA_USD_CONTEXT_SPENDABLE.new(
    # common
    name='TAXI_BOLIVIA_USD_CONTEXT_SPENDABLE_SCOUTS',
    service=Services.SCOUTS,
    contract_services=[Services.SCOUTS.id],
    # thirdparty
    tpt_payment_type=PaymentType.SCOUT,
    oebs_contract_type=None,
    # partner_act_data
    page_id=Pages.SCOUTS.id,
    pad_description=Pages.SCOUTS.desc,
    # partner_act_data
    pad_type_id=4,
)

TAXI_ZA_USD_CONTEXT = Context().new(
    # common
    name='TAXI_ZA_USD_CONTEXT',
    service=Services.TAXI,
    person_type=PersonTypes.EU_YT,
    firm=Firms.MLU_AFRICA_126,
    currency=Currencies.USD,
    paysys=Paysyses.BANK_UR_USD_MLU_AFRICA,
    nds=NdsNew.NOT_RESIDENT,
    contract_type=ContractSubtype.GENERAL,
    invoice_type=InvoiceType.PERSONAL_ACCOUNT,
    special_contract_params={'personal_account': 1, 'country': Regions.ZA.id,
                             'partner_commission_pct2': Decimal('10.2')},
    region=Regions.ZA,
    contract_services=[Services.TAXI.id, Services.TAXI_111.id, Services.TAXI_128.id, ],
    monetization_services=[Services.TAXI_111.id, Services.TAXI_128.id],
    # thirdparty
    payment_currency=Currencies.ZAR,
    tpt_payment_type=PaymentType.CARD,
    tpt_paysys_type_cc=PaysysType.MONEY,
    min_commission=Decimal('0.01'),
    precision=2,  # точность округления АВ
    currency_rate_src=CurrencyRateSource.SARB,
    migration_alias='taxi',
)

TAXI_ZA_USD_CONTEXT_SPENDABLE = TAXI_ZA_USD_CONTEXT.new(
    # common
    name='TAXI_ZA_USD_CONTEXT_SPENDABLE',
    service=Services.TAXI_DONATE,
    contract_type=ContractSubtype.SPENDABLE,
    special_contract_params={'country': Regions.ZA.id, 'partner_commission_pct2': Decimal('10.2')},
    contract_services=[Services.TAXI_DONATE.id],
    # thirdparty
    payment_currency=Currencies.USD,
    tpt_paysys_type_cc=PaysysType.TAXI,
    # partner_act_data
    pad_type_id=6,
)

TAXI_ZAM_USD_CONTEXT = TAXI_ZA_USD_CONTEXT.new(
    name='TAXI_ZAM_USD_CONTEXT',
    special_contract_params={'personal_account': 1, 'country': Regions.ZAM.id, },
    region=Regions.ZAM,
    contract_services=[Services.TAXI_111.id, Services.TAXI_128.id, ],
    payment_currency=Currencies.ZMW
)

TAXI_ZAM_USD_CONTEXT_SPENDABLE = TAXI_ZAM_USD_CONTEXT.new(
    # common
    name='TAXI_ZAM_USD_CONTEXT_SPENDABLE',
    service=Services.TAXI_DONATE,
    contract_type=ContractSubtype.SPENDABLE,
    special_contract_params={
        'country': Regions.ZAM.id,
        'partner_commission_pct2': Decimal('10.2')
    },
    contract_services=[Services.TAXI_DONATE.id],
    # thirdparty
    payment_currency=Currencies.USD,
    tpt_paysys_type_cc=PaysysType.TAXI,
    # partner_act_data
    pad_type_id=6,
)

TAXI_ZAM_USD_CONTEXT_SPENDABLE_SCOUTS = TAXI_ZAM_USD_CONTEXT_SPENDABLE.new(
    # common
    name='TAXI_ZAM_USD_CONTEXT_SPENDABLE_SCOUTS',
    service=Services.SCOUTS,
    contract_services=[Services.SCOUTS.id],
    # thirdparty
    tpt_payment_type=PaymentType.SCOUT,
    oebs_contract_type=None,
    # partner_act_data
    page_id=Pages.SCOUTS.id,
    pad_description=Pages.SCOUTS.desc,
    # partner_act_data
    pad_type_id=4,
)

TAXI_AZARBAYCAN_CONTEXT = Context().new(
    # common
    name='TAXI_AZARBAYCAN_CONTEXT',
    service=Services.TAXI,
    person_type=PersonTypes.AZ_UR,
    firm=Firms.UBER_AZ_116,
    currency=Currencies.AZN,
    paysys=Paysyses.BANK_AZ_AZN,
    nds=NdsNew.AZARBAYCAN,
    contract_type=ContractSubtype.GENERAL,
    invoice_type=InvoiceType.PERSONAL_ACCOUNT,
    special_contract_params={'personal_account': 1, 'country': Regions.AZ.id,
                             'partner_commission_pct2': Decimal('10.2')},
    region=Regions.AZ,
    contract_services=[Services.UBER.id, Services.TAXI.id],
    # thirdparty
    payment_currency=Currencies.AZN,
    tpt_payment_type=PaymentType.CARD,
    tpt_paysys_type_cc=PaysysType.MONEY,
    min_commission=Decimal('0.01'),
    precision=2,  # точность округления АВ
    currency_rate_src=CurrencyRateSource.CBAR,
    migration_alias='taxi',
)

TAXI_ISRAEL_CONTEXT_SPENDABLE = TAXI_ISRAEL_CONTEXT.new(
    # common
    name='TAXI_ISRAEL_CONTEXT_SPENDABLE',
    service=Services.TAXI_DONATE,
    contract_type=ContractSubtype.SPENDABLE,
    special_contract_params={
        'country': Regions.ISR.id,
        'israel_tax_pct': Decimal('3.3')
    },
    contract_services=[Services.TAXI_DONATE.id],
    # thirdparty
    tpt_paysys_type_cc=PaysysType.TAXI,
    # partner_act_data
    pad_type_id=6,
)

TAXI_YANGO_ISRAEL_CONTEXT_SPENDABLE = TAXI_ISRAEL_CONTEXT_SPENDABLE.new(
    name='TAXI_YANGO_ISRAEL_CONTEXT_SPENDABLE',
    firm=Firms.YANGO_ISRAEL_1090,
)

TAXI_CORP_ISRAEL_CONTEXT_SPENDABLE = TAXI_ISRAEL_CONTEXT.new(
    name='TAXI_CORP_ISRAEL_CONTEXT_SPENDABLE',
    service=Services.TAXI_CORP_PARTNERS,
    contract_services=[Services.TAXI_CORP.id, Services.TAXI_CORP_PARTNERS.id],
    special_contract_params={
        'country': Regions.ISR.id,
        'israel_tax_pct': Decimal('2.1')
    },
    contract_type=ContractSubtype.SPENDABLE
)

TAXI_CORP_YANGO_ISRAEL_CONTEXT_SPENDABLE = TAXI_CORP_ISRAEL_CONTEXT_SPENDABLE.new(
    name = 'TAXI_CORP_YANGO_ISRAEL_CONTEXT_SPENDABLE',
    firm=Firms.YANGO_ISRAEL_1090,
    pad_type_id=6,
)

TAXI_MLU_EUROPE_ROMANIA_EUR_CONTEXT = Context().new(
    # common
    name='TAXI_MLU_EUROPE_ROMANIA_EUR_CONTEXT',
    service=Services.TAXI,
    person_type=PersonTypes.EU_YT,
    firm=Firms.MLU_EUROPE_125,
    currency=Currencies.EUR,
    paysys=Paysyses.BANK_EUR_MLU_EUROPE_EU_YT,
    nds=NdsNew.NOT_RESIDENT,
    contract_type=ContractSubtype.GENERAL,
    invoice_type=InvoiceType.PERSONAL_ACCOUNT,
    special_contract_params={'personal_account': 1, 'country': Regions.RO.id,
                             'partner_commission_pct2': Decimal('10.2')},
    region=Regions.RO,
    contract_services=[Services.TAXI.id, Services.TAXI_111.id, Services.TAXI_128.id],
    monetization_services=[Services.TAXI_111.id, Services.TAXI_128.id],
    # thirdparty
    payment_currency=Currencies.RON,
    tpt_payment_type=PaymentType.CARD,
    tpt_paysys_type_cc=PaysysType.MONEY,
    min_commission=Decimal('0.01'),
    precision=2,  # точность округления АВ
    currency_rate_src=CurrencyRateSource.BNR,
    migration_alias='taxi',
)

TAXI_MLU_EUROPE_ROMANIA_RON_CONTEXT = TAXI_MLU_EUROPE_ROMANIA_EUR_CONTEXT.new(
    name='TAXI_MLU_EUROPE_ROMANIA_RON_CONTEXT',
    currency=Currencies.RON,
    paysys=Paysyses.BANK_RON_MLU_EUROPE_EU_YT,
)

TAXI_MLU_EUROPE_ROMANIA_EUR_CONTEXT_SPENDABLE = TAXI_MLU_EUROPE_ROMANIA_EUR_CONTEXT.new(
    # common
    name='TAXI_MLU_EUROPE_ROMANIA_EUR_CONTEXT_SPENDABLE',
    service=Services.TAXI_DONATE,
    contract_type=ContractSubtype.SPENDABLE,
    special_contract_params={'country': Regions.RO.id},
    contract_services=[Services.TAXI_DONATE.id],
    # thirdparty
    tpt_paysys_type_cc=PaysysType.TAXI,
    payment_currency=Currencies.EUR,
    # partner_act_data
    pad_type_id=6,
)

TAXI_MLU_EUROPE_ROMANIA_RON_CONTEXT_SPENDABLE = TAXI_MLU_EUROPE_ROMANIA_EUR_CONTEXT_SPENDABLE.new(
    name='TAXI_MLU_EUROPE_ROMANIA_RON_CONTEXT_SPENDABLE',
    currency=Currencies.RON,
    paysys=Paysyses.BANK_RON_MLU_EUROPE_EU_YT,
    payment_currency=Currencies.RON,
)

TAXI_YANDEX_GO_SRL_CONTEXT = TAXI_MLU_EUROPE_ROMANIA_RON_CONTEXT.new(
    name='TAXI_YANDEX_GO_SRL_CONTEXT',
    person_type=PersonTypes.RO_UR,
    firm=Firms.YANDEX_GO_SRL_127,
    paysys=Paysyses.BANK_RON_YANDEX_GO_SRL_RO_UR,
    nds=NdsNew.ROMANIA
)

TAXI_YANDEX_GO_SRL_CONTEXT_SPENDABLE = TAXI_MLU_EUROPE_ROMANIA_RON_CONTEXT_SPENDABLE.new(
    name='TAXI_YANDEX_GO_SRL_CONTEXT_SPENDABLE',
    person_type=PersonTypes.RO_UR,
    firm=Firms.YANDEX_GO_SRL_127,
    paysys=Paysyses.BANK_RON_YANDEX_GO_SRL_RO_UR,
    nds=NdsNew.ROMANIA,
)

TAXI_MLU_EUROPE_ROMANIA_USD_CONTEXT = TAXI_MLU_EUROPE_ROMANIA_EUR_CONTEXT.new(
    name='TAXI_MLU_EUROPE_ROMANIA_USD_CONTEXT',
    currency=Currencies.USD,
    paysys=Paysyses.BANK_USD_MLU_EUROPE_EU_YT,
)

TAXI_MLU_EUROPE_ROMANIA_USD_CONTEXT_SPENDABLE = TAXI_MLU_EUROPE_ROMANIA_EUR_CONTEXT_SPENDABLE.new(
    name='TAXI_MLU_EUROPE_ROMANIA_USD_CONTEXT_SPENDABLE',
    currency=Currencies.USD,
    paysys=Paysyses.BANK_USD_MLU_EUROPE_EU_YT,
    payment_currency=Currencies.USD,
)


TAXI_RU_DELIVERY_CONTEXT = Context().new(
    # common
    name='TAXI_RU_DELIVERY_CONTEXT',
    service=Services.TAXI_DELIVERY_PAYMENTS,
    person_type=PersonTypes.UR,
    firm=Firms.LOGISTICS_130,
    currency=Currencies.RUB,
    paysys=Paysyses.BANK_UR_RUB_LOGISTICS,
    additional_paysys=Paysyses.CE_TAXI_DELIVERY,
    zaxi_deposit_product=Products.ZAXI_DELIVERY_DEPOSIT_RUB,
    nds=NdsNew.DEFAULT,
    contract_type=ContractSubtype.GENERAL,
    invoice_type=InvoiceType.PERSONAL_ACCOUNT,
    special_contract_params={'personal_account': 1, 'country': Regions.RU.id,
                             'region': CountryRegion.RUS, 'nds_for_receipt': NdsNew.DEFAULT.nds_id},
    manager=None,
    region=Regions.RU,
    contract_services=[Services.TAXI_DELIVERY_PAYMENTS.id,
                       Services.TAXI_DELIVERY_CASH.id, Services.TAXI_DELIVERY_CARD.id],
    monetization_services=[Services.TAXI_DELIVERY_CASH.id, Services.TAXI_DELIVERY_CARD.id],
    partner_balance_service=Services.TAXI_DELIVERY_CASH,
    # thirdparty
    payment_currency=Currencies.RUB,
    tpt_payment_type=PaymentType.CARD,
    tpt_paysys_type_cc=PaysysType.MONEY,  # PaysysType.ALFA
    min_commission=Decimal('0'),
    # АВ всегда 0, т.к. на commission_category не смотрим, а в договоре поля для процента для России нет
    precision=2,  # точность округления АВ
    currency_rate_src=CurrencyRateSource.CBR,
)


TAXI_RU_DELIVERY_CONTEXT_SPENDABLE = TAXI_RU_DELIVERY_CONTEXT.new(
    # common
    name='TAXI_RU_DELIVERY_CONTEXT_SPENDABLE',
    service=Services.TAXI_DELIVERY_SPENDABLE,
    contract_type=ContractSubtype.SPENDABLE,
    special_contract_params={'country': Regions.RU.id, 'region': CountryRegion.RUS},
    contract_services=[Services.TAXI_DELIVERY_SPENDABLE.id],
    # thirdparty
    tpt_paysys_type_cc=PaysysType.TAXI,
    # partner_act_data
    pad_type_id=6,
)

TAXI_RU_DELIVERY_CONTEXT_SPENDABLE_SCOUTS = TAXI_RU_DELIVERY_CONTEXT_SPENDABLE.new(
    # common
    name='TAXI_RU_DELIVERY_CONTEXT_SPENDABLE_SCOUTS',
    service=Services.SCOUTS,
    contract_services=[Services.SCOUTS.id],
    # thirdparty
    tpt_payment_type=PaymentType.SCOUT,
    oebs_contract_type=None,
    # partner_act_data
    page_id=Pages.SCOUTS.id,
    pad_description=Pages.SCOUTS.desc,
    # partner_act_data
    pad_type_id=4,
)

TAXI_DELIVERY_ISRAEL_CONTEXT = Context().new(
    # common
    name='TAXI_DELIVERY_ISRAEL_CONTEXT',
    service=Services.TAXI_DELIVERY_PAYMENTS,
    person_type=PersonTypes.IL_UR,
    firm=Firms.YANDEX_GO_ISRAEL_35,
    currency=Currencies.ILS,
    paysys=Paysyses.BANK_IL_UR_ILS,
    nds=NdsNew.ISRAEL,
    contract_type=ContractSubtype.GENERAL,
    invoice_type=InvoiceType.PERSONAL_ACCOUNT,
    special_contract_params={'personal_account': 1, 'country': Regions.ISR.id,
                             'partner_commission_pct2': Decimal('10.2'),
                             'israel_tax_pct': Decimal('7.1')
    },
    region=Regions.ISR,
    contract_services=[Services.TAXI_DELIVERY_PAYMENTS.id,
                       Services.TAXI_DELIVERY_CASH.id, Services.TAXI_DELIVERY_CARD.id],
    monetization_services=[Services.TAXI_DELIVERY_CASH.id, Services.TAXI_DELIVERY_CARD.id],
    partner_balance_service=Services.TAXI_DELIVERY_CASH,
    # thirdparty
    payment_currency=Currencies.ILS,
    tpt_payment_type=PaymentType.CARD,
    tpt_paysys_type_cc=PaysysType.MONEY,
    min_commission=Decimal('0.01'),
    precision=2,  # точность округления АВ
    currency_rate_src=CurrencyRateSource.BOI,
    migration_alias='taxi',
)


TAXI_DELIVERY_ISRAEL_CONTEXT_SPENDABLE = TAXI_DELIVERY_ISRAEL_CONTEXT.new(
    # common
    name='TAXI_DELIVERY_ISRAEL_CONTEXT_SPENDABLE',
    service=Services.TAXI_DELIVERY_SPENDABLE,
    contract_type=ContractSubtype.SPENDABLE,
    special_contract_params={
        'country': Regions.ISR.id,
        'israel_tax_pct': Decimal('3.3')
    },
    contract_services=[Services.TAXI_DELIVERY_SPENDABLE.id],
    # thirdparty
    tpt_paysys_type_cc=PaysysType.TAXI,
    # partner_act_data
    pad_type_id=6,
)

TAXI_DELIVERY_KZ_CONTEXT = Context().new(
    # TAXI_DELIVERY_KZ_CONTEXT
    name='TAXI_DELIVERY_KZ_CONTEXT',
    service=Services.TAXI_DELIVERY_PAYMENTS,
    person_type=PersonTypes.KZU,
    firm=Firms.YANDEX_DELIVERY_KZ,
    currency=Currencies.KZT,
    paysys=Paysyses.BANK_KZU_DELIVERY_KZ_KZT,
    nds=NdsNew.KAZAKHSTAN,
    contract_type=ContractSubtype.GENERAL,
    invoice_type=InvoiceType.PERSONAL_ACCOUNT,
    special_contract_params={'personal_account': 1, 'country': Regions.KZ.id,
                             'partner_commission_pct2': Decimal('10.2'),},
    manager=None,
    region=Regions.KZ,
    contract_services=[Services.TAXI_DELIVERY_PAYMENTS.id,
                       Services.TAXI_DELIVERY_CASH.id, Services.TAXI_DELIVERY_CARD.id],
    monetization_services=[Services.TAXI_DELIVERY_CASH.id, Services.TAXI_DELIVERY_CARD.id],
    partner_balance_service=Services.TAXI_DELIVERY_CASH,
    # thirdparty
    payment_currency=Currencies.KZT,
    tpt_payment_type=PaymentType.CARD,
    tpt_paysys_type_cc=PaysysType.MONEY,  # PaysysType.ALFA
    min_commission=Decimal('0'),
    precision=2,  # точность округления АВ
    is_offer=1
)

TAXI_DELIVERY_KZ_CONTEXT_SPENDABLE = TAXI_DELIVERY_KZ_CONTEXT.new(
    # common
    name='TAXI_DELIVERY_KZ_CONTEXT_SPENDABLE',
    service=Services.TAXI_DELIVERY_SPENDABLE,
    contract_type=ContractSubtype.SPENDABLE,
    special_contract_params={'country': Regions.KZ.id},
    contract_services=[Services.TAXI_DELIVERY_SPENDABLE.id],
    # thirdparty
    tpt_paysys_type_cc=PaysysType.TAXI,
    # partner_act_data
    pad_type_id=6,
)

TAXI_DELIVERY_GEO_USD_CONTEXT = Context().new(
    name='TAXI_DELIVERY_GEO_USD_CONTEXT',
    service=Services.TAXI_DELIVERY_PAYMENTS,
    person_type=PersonTypes.EU_YT,
    firm=Firms.FOODTECH_DELIVERY_BV,
    currency=Currencies.USD,
    paysys=Paysyses.BANK_EU_YT_FOODTECH_DELIVERY_USD,
    nds=NdsNew.NOT_RESIDENT,
    contract_type=ContractSubtype.GENERAL,
    invoice_type=InvoiceType.PERSONAL_ACCOUNT,
    special_contract_params={'personal_account': 1, 'country': Regions.GEO.id,
                             'partner_commission_pct2': Decimal('10.2'), },
    manager=None,
    region=Regions.GEO,
    contract_services=[Services.TAXI_DELIVERY_PAYMENTS.id,
                       Services.TAXI_DELIVERY_CASH.id, Services.TAXI_DELIVERY_CARD.id],
    monetization_services=[Services.TAXI_DELIVERY_CASH.id, Services.TAXI_DELIVERY_CARD.id],
    partner_balance_service=Services.TAXI_DELIVERY_CASH,
    # thirdparty
    payment_currency=Currencies.USD,
    tpt_payment_type=PaymentType.CARD,
    tpt_paysys_type_cc=PaysysType.MONEY,  # PaysysType.ALFA
    min_commission=Decimal('0'),
    precision=2,  # точность округления АВ
)

TAXI_DELIVERY_GEO_USD_CONTEXT_SPENDABLE = TAXI_DELIVERY_GEO_USD_CONTEXT.new(
    # common
    name='TAXI_DELIVERY_GEO_USD_CONTEXT_SPENDABLE',
    service=Services.TAXI_DELIVERY_SPENDABLE,
    contract_type=ContractSubtype.SPENDABLE,
    special_contract_params={'country': Regions.GEO.id},
    contract_services=[Services.TAXI_DELIVERY_SPENDABLE.id],
    # thirdparty
    tpt_paysys_type_cc=PaysysType.TAXI,
    # partner_act_data
    pad_type_id=6,
)

TAXI_DELIVERY_EST_EUR_CONTEXT = Context().new(
    name='TAXI_DELIVERY_EST_EUR_CONTEXT',
    service=Services.TAXI_DELIVERY_PAYMENTS,
    person_type=PersonTypes.EU_YT,
    firm=Firms.FOODTECH_DELIVERY_BV,
    currency=Currencies.EUR,
    paysys=Paysyses.BANK_EU_YT_FOODTECH_DELIVERY_EUR,
    nds=NdsNew.NOT_RESIDENT,
    contract_type=ContractSubtype.GENERAL,
    invoice_type=InvoiceType.PERSONAL_ACCOUNT,
    special_contract_params={'personal_account': 1, 'country': Regions.EST.id,
                             'partner_commission_pct2': Decimal('10.2'), },
    manager=None,
    region=Regions.EST,
    contract_services=[Services.TAXI_DELIVERY_PAYMENTS.id,
                       Services.TAXI_DELIVERY_CASH.id, Services.TAXI_DELIVERY_CARD.id],
    monetization_services=[Services.TAXI_DELIVERY_CASH.id, Services.TAXI_DELIVERY_CARD.id],
    partner_balance_service=Services.TAXI_DELIVERY_CASH,
    # thirdparty
    payment_currency=Currencies.EUR,
    tpt_payment_type=PaymentType.CARD,
    tpt_paysys_type_cc=PaysysType.MONEY,  # PaysysType.ALFA
    min_commission=Decimal('0'),
    precision=2,  # точность округления АВ
)

TAXI_DELIVERY_EST_EUR_CONTEXT_SPENDABLE = TAXI_DELIVERY_EST_EUR_CONTEXT.new(
    # common
    name='TAXI_DELIVERY_EST_EUR_CONTEXT_SPENDABLE',
    service=Services.TAXI_DELIVERY_SPENDABLE,
    contract_type=ContractSubtype.SPENDABLE,
    special_contract_params={'country': Regions.EST.id},
    contract_services=[Services.TAXI_DELIVERY_SPENDABLE.id],
    # thirdparty
    tpt_paysys_type_cc=PaysysType.TAXI,
    # partner_act_data
    pad_type_id=6,
)

TAXI_DELIVERY_BY_BYN_CONTEXT = Context().new(
    name='TAXI_DELIVERY_BY_BYN_CONTEXT',
    service=Services.TAXI_DELIVERY_PAYMENTS,
    person_type=PersonTypes.EU_YT,
    firm=Firms.FOODTECH_DELIVERY_BV,
    currency=Currencies.BYN,
    paysys=Paysyses.BANK_EU_YT_FOODTECH_DELIVERY_BYN,
    nds=NdsNew.NOT_RESIDENT,
    contract_type=ContractSubtype.GENERAL,
    invoice_type=InvoiceType.PERSONAL_ACCOUNT,
    special_contract_params={'personal_account': 1, 'country': Regions.BY.id,
                             'partner_commission_pct2': Decimal('10.2'), },
    manager=None,
    region=Regions.BY,
    contract_services=[Services.TAXI_DELIVERY_PAYMENTS.id,
                       Services.TAXI_DELIVERY_CASH.id, Services.TAXI_DELIVERY_CARD.id],
    monetization_services=[Services.TAXI_DELIVERY_CASH.id, Services.TAXI_DELIVERY_CARD.id],
    partner_balance_service=Services.TAXI_DELIVERY_CASH,
    # thirdparty
    payment_currency=Currencies.BYN,
    tpt_payment_type=PaymentType.CARD,
    tpt_paysys_type_cc=PaysysType.MONEY,  # PaysysType.ALFA
    min_commission=Decimal('0'),
    precision=2,  # точность округления АВ
)

TAXI_DELIVERY_BY_BYN_CONTEXT_SPENDABLE = TAXI_DELIVERY_BY_BYN_CONTEXT.new(
    # common
    name='TAXI_DELIVERY_BY_BYN_CONTEXT_SPENDABLE',
    service=Services.TAXI_DELIVERY_SPENDABLE,
    contract_type=ContractSubtype.SPENDABLE,
    special_contract_params={'country': Regions.BY.id},
    contract_services=[Services.TAXI_DELIVERY_SPENDABLE.id],
    # thirdparty
    tpt_paysys_type_cc=PaysysType.TAXI,
    # partner_act_data
    pad_type_id=6,
)

TAXI_DELIVERY_KZ_EUR_CONTEXT = TAXI_DELIVERY_EST_EUR_CONTEXT.new(
    name='TAXI_DELIVERY_KZ_EUR_CONTEXT',
    special_contract_params={'personal_account': 1, 'country': Regions.KZ.id,
                             'partner_commission_pct2': Decimal('10.2'), },
    region=Regions.KZ,
)

TAXI_DELIVERY_KZ_EUR_CONTEXT_SPENDABLE = TAXI_DELIVERY_EST_EUR_CONTEXT_SPENDABLE.new(
    name='TAXI_DELIVERY_KZ_EUR_CONTEXT_SPENDABLE',
    special_contract_params={'country': Regions.KZ.id},
    region=Regions.KZ,
)

TAXI_DELIVERY_KZ_EUR_CONTEXT_SPENDABLE_SCOUTS = TAXI_DELIVERY_KZ_EUR_CONTEXT_SPENDABLE.new(
    # common
    name='TAXI_DELIVERY_KZ_EUR_CONTEXT_SPENDABLE_SCOUTS',
    service=Services.SCOUTS,
    contract_services=[Services.SCOUTS.id],
    # thirdparty
    tpt_payment_type=PaymentType.SCOUT,
    oebs_contract_type=None,
    # partner_act_data
    page_id=Pages.SCOUTS.id,
    pad_description=Pages.SCOUTS.desc,
    # partner_act_data
    pad_type_id=4,
)

TAXI_DELIVERY_BY_EUR_CONTEXT = TAXI_DELIVERY_EST_EUR_CONTEXT.new(
    name='TAXI_DELIVERY_BY_EUR_CONTEXT',
    special_contract_params={'personal_account': 1, 'country': Regions.BY.id,
                             'partner_commission_pct2': Decimal('10.2'), },
    region=Regions.BY,
)

TAXI_DELIVERY_BY_EUR_CONTEXT_SPENDABLE = TAXI_DELIVERY_EST_EUR_CONTEXT_SPENDABLE.new(
    name='TAXI_DELIVERY_BY_EUR_CONTEXT_SPENDABLE',
    special_contract_params={'country': Regions.BY.id},
    region=Regions.BY,
)

TAXI_DELIVERY_BY_EUR_CONTEXT_SPENDABLE_SCOUTS = TAXI_DELIVERY_KZ_EUR_CONTEXT_SPENDABLE_SCOUTS.new(
    name='TAXI_DELIVERY_BY_EUR_CONTEXT_SPENDABLE_SCOUTS',
    special_contract_params={'country': Regions.BY.id},
    region=Regions.BY,
)

TAXI_DELIVERY_ISR_EUR_CONTEXT = TAXI_DELIVERY_EST_EUR_CONTEXT.new(
    name='TAXI_DELIVERY_ISR_EUR_CONTEXT',
    special_contract_params={'personal_account': 1, 'country': Regions.ISR.id,
                             'partner_commission_pct2': Decimal('10.2'), },
    region=Regions.ISR,
)

TAXI_DELIVERY_ISR_EUR_CONTEXT_SPENDABLE = TAXI_DELIVERY_EST_EUR_CONTEXT_SPENDABLE.new(
    name='TAXI_DELIVERY_ISR_EUR_CONTEXT_SPENDABLE',
    special_contract_params={'country': Regions.ISR.id},
    region=Regions.ISR,
)

TAXI_DELIVERY_ISR_EUR_CONTEXT_SPENDABLE_SCOUTS = TAXI_DELIVERY_KZ_EUR_CONTEXT_SPENDABLE_SCOUTS.new(
    name='TAXI_DELIVERY_ISR_EUR_CONTEXT_SPENDABLE_SCOUTS',
    special_contract_params={'country': Regions.ISR.id},
    region=Regions.ISR,
)

TAXI_DELIVERY_UZB_EUR_CONTEXT = TAXI_DELIVERY_EST_EUR_CONTEXT.new(
    name='TAXI_DELIVERY_UZB_EUR_CONTEXT',
    special_contract_params={'personal_account': 1, 'country': Regions.UZB.id,
                             'partner_commission_pct2': Decimal('10.2'), },
    region=Regions.UZB,
)

TAXI_DELIVERY_UZB_EUR_CONTEXT_SPENDABLE = TAXI_DELIVERY_EST_EUR_CONTEXT_SPENDABLE.new(
    name='TAXI_DELIVERY_UZB_EUR_CONTEXT_SPENDABLE',
    special_contract_params={'country': Regions.UZB.id},
    region=Regions.UZB,
)

TAXI_DELIVERY_UZB_EUR_CONTEXT_SPENDABLE_SCOUTS = TAXI_DELIVERY_KZ_EUR_CONTEXT_SPENDABLE_SCOUTS.new(
    name='TAXI_DELIVERY_UZB_EUR_CONTEXT_SPENDABLE_SCOUTS',
    special_contract_params={'country': Regions.UZB.id},
    region=Regions.UZB,
)

TAXI_DELIVERY_UZB_USD_CONTEXT = TAXI_DELIVERY_GEO_USD_CONTEXT.new(
    name='TAXI_DELIVERY_UZB_USD_CONTEXT',
    special_contract_params={'personal_account': 1, 'country': Regions.UZB.id,
                             'partner_commission_pct2': Decimal('10.2'), },
    region=Regions.UZB,
)

TAXI_DELIVERY_UZB_USD_CONTEXT_SPENDABLE = TAXI_DELIVERY_GEO_USD_CONTEXT_SPENDABLE.new(
    name='TAXI_DELIVERY_UZB_USD_CONTEXT_SPENDABLE',
    special_contract_params={'country': Regions.UZB.id},
    region=Regions.UZB,
)

TAXI_DELIVERY_UZB_USD_CONTEXT_SPENDABLE_SCOUTS = TAXI_DELIVERY_UZB_USD_CONTEXT_SPENDABLE.new(
    # common
    name='TAXI_DELIVERY_UZB_USD_CONTEXT_SPENDABLE_SCOUTS',
    service=Services.SCOUTS,
    contract_services=[Services.SCOUTS.id],
    # thirdparty
    tpt_payment_type=PaymentType.SCOUT,
    oebs_contract_type=None,
    # partner_act_data
    page_id=Pages.SCOUTS.id,
    pad_description=Pages.SCOUTS.desc,
    # partner_act_data
    pad_type_id=4,
)

TAXI_DELIVERY_CMR_EUR_CONTEXT = TAXI_DELIVERY_EST_EUR_CONTEXT.new(
    name='TAXI_DELIVERY_CMR_EUR_CONTEXT',
    special_contract_params={'personal_account': 1, 'country': Regions.CMR.id,
                             'partner_commission_pct2': Decimal('10.2'), },
    region=Regions.CMR,
)

TAXI_DELIVERY_CMR_EUR_CONTEXT_SPENDABLE = TAXI_DELIVERY_EST_EUR_CONTEXT_SPENDABLE.new(
    name='TAXI_DELIVERY_CMR_EUR_CONTEXT_SPENDABLE',
    special_contract_params={'country': Regions.CMR.id},
    region=Regions.CMR,
)

TAXI_DELIVERY_CMR_EUR_CONTEXT_SPENDABLE_SCOUTS = TAXI_DELIVERY_KZ_EUR_CONTEXT_SPENDABLE_SCOUTS.new(
    name='TAXI_DELIVERY_CMR_EUR_CONTEXT_SPENDABLE_SCOUTS',
    special_contract_params={'country': Regions.CMR.id},
    region=Regions.CMR,
)

TAXI_DELIVERY_ARM_EUR_CONTEXT = TAXI_DELIVERY_EST_EUR_CONTEXT.new(
    name='TAXI_DELIVERY_ARM_EUR_CONTEXT',
    special_contract_params={'personal_account': 1, 'country': Regions.ARM.id,
                             'partner_commission_pct2': Decimal('10.2'), },
    region=Regions.ARM,
)

TAXI_DELIVERY_ARM_EUR_CONTEXT_SPENDABLE = TAXI_DELIVERY_EST_EUR_CONTEXT_SPENDABLE.new(
    name='TAXI_DELIVERY_ARM_EUR_CONTEXT_SPENDABLE',
    special_contract_params={'country': Regions.ARM.id},
    region=Regions.ARM,
)

TAXI_DELIVERY_ARM_EUR_CONTEXT_SPENDABLE_SCOUTS = TAXI_DELIVERY_KZ_EUR_CONTEXT_SPENDABLE_SCOUTS.new(
    name='TAXI_DELIVERY_ARM_EUR_CONTEXT_SPENDABLE_SCOUTS',
    special_contract_params={'country': Regions.ARM.id},
    region=Regions.ARM,
)

TAXI_DELIVERY_ARM_USD_CONTEXT = TAXI_DELIVERY_UZB_USD_CONTEXT.new(
    name='TAXI_DELIVERY_ARM_USD_CONTEXT',
    special_contract_params={'personal_account': 1, 'country': Regions.ARM.id,
                             'partner_commission_pct2': Decimal('10.2'), },
    region=Regions.ARM,
)

TAXI_DELIVERY_ARM_USD_CONTEXT_SPENDABLE = TAXI_DELIVERY_UZB_USD_CONTEXT_SPENDABLE.new(
    name='TAXI_DELIVERY_ARM_USD_CONTEXT_SPENDABLE',
    special_contract_params={'country': Regions.ARM.id},
    region=Regions.ARM,
)

TAXI_DELIVERY_ARM_USD_CONTEXT_SPENDABLE_SCOUTS = TAXI_DELIVERY_UZB_USD_CONTEXT_SPENDABLE_SCOUTS.new(
    name='TAXI_DELIVERY_ARM_USD_CONTEXT_SPENDABLE_SCOUTS',
    special_contract_params={'country': Regions.ARM.id},
    region=Regions.ARM,
)

TAXI_DELIVERY_KGZ_EUR_CONTEXT = TAXI_DELIVERY_EST_EUR_CONTEXT.new(
    name='TAXI_DELIVERY_KGZ_EUR_CONTEXT',
    special_contract_params={'personal_account': 1, 'country': Regions.KGZ.id,
                             'partner_commission_pct2': Decimal('10.2'), },
    region=Regions.KGZ,
)

TAXI_DELIVERY_KGZ_EUR_CONTEXT_SPENDABLE = TAXI_DELIVERY_EST_EUR_CONTEXT_SPENDABLE.new(
    name='TAXI_DELIVERY_KGZ_EUR_CONTEXT_SPENDABLE',
    special_contract_params={'country': Regions.KGZ.id},
    region=Regions.KGZ,
)

TAXI_DELIVERY_KGZ_EUR_CONTEXT_SPENDABLE_SCOUTS = TAXI_DELIVERY_KZ_EUR_CONTEXT_SPENDABLE_SCOUTS.new(
    name='TAXI_DELIVERY_KGZ_EUR_CONTEXT_SPENDABLE_SCOUTS',
    special_contract_params={'country': Regions.KGZ.id},
    region=Regions.KGZ,
)

TAXI_DELIVERY_KGZ_USD_CONTEXT = TAXI_DELIVERY_UZB_USD_CONTEXT.new(
    name='TAXI_DELIVERY_KGZ_USD_CONTEXT',
    special_contract_params={'personal_account': 1, 'country': Regions.KGZ.id,
                             'partner_commission_pct2': Decimal('10.2'), },
    region=Regions.KGZ,
)

TAXI_DELIVERY_KGZ_USD_CONTEXT_SPENDABLE = TAXI_DELIVERY_UZB_USD_CONTEXT_SPENDABLE.new(
    name='TAXI_DELIVERY_KGZ_USD_CONTEXT_SPENDABLE',
    special_contract_params={'country': Regions.KGZ.id},
    region=Regions.KGZ,
)

TAXI_DELIVERY_KGZ_USD_CONTEXT_SPENDABLE_SCOUTS = TAXI_DELIVERY_UZB_USD_CONTEXT_SPENDABLE_SCOUTS.new(
    name='TAXI_DELIVERY_KGZ_USD_CONTEXT_SPENDABLE_SCOUTS',
    special_contract_params={'country': Regions.KGZ.id},
    region=Regions.KGZ,
)

TAXI_DELIVERY_GHA_EUR_CONTEXT = TAXI_DELIVERY_EST_EUR_CONTEXT.new(
    name='TAXI_DELIVERY_GHA_EUR_CONTEXT',
    special_contract_params={'personal_account': 1, 'country': Regions.GHA.id,
                             'partner_commission_pct2': Decimal('10.2'), },
    region=Regions.GHA,
)

TAXI_DELIVERY_GHA_EUR_CONTEXT_SPENDABLE = TAXI_DELIVERY_EST_EUR_CONTEXT_SPENDABLE.new(
    name='TAXI_DELIVERY_GHA_EUR_CONTEXT_SPENDABLE',
    special_contract_params={'country': Regions.GHA.id},
    region=Regions.GHA,
)

TAXI_DELIVERY_GHA_EUR_CONTEXT_SPENDABLE_SCOUTS = TAXI_DELIVERY_KZ_EUR_CONTEXT_SPENDABLE_SCOUTS.new(
    name='TAXI_DELIVERY_GHA_EUR_CONTEXT_SPENDABLE_SCOUTS',
    special_contract_params={'country': Regions.GHA.id},
    region=Regions.GHA,
)

TAXI_DELIVERY_GHA_USD_CONTEXT = TAXI_DELIVERY_UZB_USD_CONTEXT.new(
    name='TAXI_DELIVERY_GHA_USD_CONTEXT',
    special_contract_params={'personal_account': 1, 'country': Regions.GHA.id,
                             'partner_commission_pct2': Decimal('10.2'), },
    region=Regions.GHA,
)

TAXI_DELIVERY_GHA_USD_CONTEXT_SPENDABLE = TAXI_DELIVERY_UZB_USD_CONTEXT_SPENDABLE.new(
    name='TAXI_DELIVERY_GHA_USD_CONTEXT_SPENDABLE',
    special_contract_params={'country': Regions.GHA.id},
    region=Regions.GHA,
)

TAXI_DELIVERY_GHA_USD_CONTEXT_SPENDABLE_SCOUTS = TAXI_DELIVERY_UZB_USD_CONTEXT_SPENDABLE_SCOUTS.new(
    name='TAXI_DELIVERY_GHA_USD_CONTEXT_SPENDABLE_SCOUTS',
    special_contract_params={'country': Regions.GHA.id},
    region=Regions.GHA,
)

TAXI_DELIVERY_SEN_EUR_CONTEXT = TAXI_DELIVERY_EST_EUR_CONTEXT.new(
    name='TAXI_DELIVERY_SEN_EUR_CONTEXT',
    special_contract_params={'personal_account': 1, 'country': Regions.SEN.id,
                             'partner_commission_pct2': Decimal('10.2'), },
    region=Regions.SEN,
)

TAXI_DELIVERY_SEN_EUR_CONTEXT_SPENDABLE = TAXI_DELIVERY_EST_EUR_CONTEXT_SPENDABLE.new(
    name='TAXI_DELIVERY_SEN_EUR_CONTEXT_SPENDABLE',
    special_contract_params={'country': Regions.SEN.id},
    region=Regions.SEN,
)

TAXI_DELIVERY_SEN_EUR_CONTEXT_SPENDABLE_SCOUTS = TAXI_DELIVERY_KZ_EUR_CONTEXT_SPENDABLE_SCOUTS.new(
    name='TAXI_DELIVERY_SEN_EUR_CONTEXT_SPENDABLE_SCOUTS',
    special_contract_params={'country': Regions.SEN.id},
    region=Regions.SEN,
)

TAXI_DELIVERY_MXC_EUR_CONTEXT = TAXI_DELIVERY_EST_EUR_CONTEXT.new(
    name='TAXI_DELIVERY_MXC_EUR_CONTEXT',
    special_contract_params={'personal_account': 1, 'country': Regions.MXC.id,
                             'partner_commission_pct2': Decimal('10.2'), },
    region=Regions.MXC,
)

TAXI_DELIVERY_MXC_EUR_CONTEXT_SPENDABLE = TAXI_DELIVERY_EST_EUR_CONTEXT_SPENDABLE.new(
    name='TAXI_DELIVERY_MXC_EUR_CONTEXT_SPENDABLE',
    special_contract_params={'country': Regions.MXC.id},
    region=Regions.MXC,
)

TAXI_DELIVERY_MXC_EUR_CONTEXT_SPENDABLE_SCOUTS = TAXI_DELIVERY_KZ_EUR_CONTEXT_SPENDABLE_SCOUTS.new(
    name='TAXI_DELIVERY_MXC_EUR_CONTEXT_SPENDABLE_SCOUTS',
    special_contract_params={'country': Regions.MXC.id},
    region=Regions.MXC,
)

TAXI_DELIVERY_TR_EUR_CONTEXT = TAXI_DELIVERY_EST_EUR_CONTEXT.new(
    name='TAXI_DELIVERY_TR_EUR_CONTEXT',
    special_contract_params={'personal_account': 1, 'country': Regions.TR.id,
                             'partner_commission_pct2': Decimal('10.2'), },
    region=Regions.TR,
)

TAXI_DELIVERY_TR_EUR_CONTEXT_SPENDABLE = TAXI_DELIVERY_EST_EUR_CONTEXT_SPENDABLE.new(
    name='TAXI_DELIVERY_TR_EUR_CONTEXT_SPENDABLE',
    special_contract_params={'country': Regions.TR.id},
    region=Regions.TR,
)

TAXI_DELIVERY_TR_EUR_CONTEXT_SPENDABLE_SCOUTS = TAXI_DELIVERY_KZ_EUR_CONTEXT_SPENDABLE_SCOUTS.new(
    name='TAXI_DELIVERY_TR_EUR_CONTEXT_SPENDABLE_SCOUTS',
    special_contract_params={'country': Regions.TR.id},
    region=Regions.TR,
)

TAXI_DELIVERY_PER_EUR_CONTEXT = TAXI_DELIVERY_EST_EUR_CONTEXT.new(
    name='TAXI_DELIVERY_PER_EUR_CONTEXT',
    special_contract_params={'personal_account': 1, 'country': Regions.PER.id,
                             'partner_commission_pct2': Decimal('10.2'), },
    region=Regions.PER,
)

TAXI_DELIVERY_PER_EUR_CONTEXT_SPENDABLE = TAXI_DELIVERY_EST_EUR_CONTEXT_SPENDABLE.new(
    name='TAXI_DELIVERY_PER_EUR_CONTEXT_SPENDABLE',
    special_contract_params={'country': Regions.PER.id},
    region=Regions.PER,
)

TAXI_DELIVERY_PER_EUR_CONTEXT_SPENDABLE_SCOUTS = TAXI_DELIVERY_KZ_EUR_CONTEXT_SPENDABLE_SCOUTS.new(
    name='TAXI_DELIVERY_PER_EUR_CONTEXT_SPENDABLE_SCOUTS',
    special_contract_params={'country': Regions.PER.id},
    region=Regions.PER,
)

TAXI_DELIVERY_ZA_EUR_CONTEXT = TAXI_DELIVERY_EST_EUR_CONTEXT.new(
    name='TAXI_DELIVERY_ZA_EUR_CONTEXT',
    special_contract_params={'personal_account': 1, 'country': Regions.ZA.id,
                             'partner_commission_pct2': Decimal('10.2'), },
    region=Regions.ZA,
)

TAXI_DELIVERY_ZA_EUR_CONTEXT_SPENDABLE = TAXI_DELIVERY_EST_EUR_CONTEXT_SPENDABLE.new(
    name='TAXI_DELIVERY_ZA_EUR_CONTEXT_SPENDABLE',
    special_contract_params={'country': Regions.ZA.id},
    region=Regions.ZA,
)

TAXI_DELIVERY_ZA_EUR_CONTEXT_SPENDABLE_SCOUTS = TAXI_DELIVERY_KZ_EUR_CONTEXT_SPENDABLE_SCOUTS.new(
    name='TAXI_DELIVERY_ZA_EUR_CONTEXT_SPENDABLE_SCOUTS',
    special_contract_params={'country': Regions.ZA.id},
    region=Regions.ZA,
)

TAXI_DELIVERY_UAE_EUR_CONTEXT = TAXI_DELIVERY_EST_EUR_CONTEXT.new(
    name='TAXI_DELIVERY_UAE_EUR_CONTEXT',
    special_contract_params={'personal_account': 1, 'country': Regions.UAE.id,
                             'partner_commission_pct2': Decimal('10.2'), },
    region=Regions.UAE,
)

TAXI_DELIVERY_UAE_EUR_CONTEXT_SPENDABLE = TAXI_DELIVERY_EST_EUR_CONTEXT_SPENDABLE.new(
    name='TAXI_DELIVERY_UAE_EUR_CONTEXT_SPENDABLE',
    special_contract_params={'country': Regions.UAE.id},
    region=Regions.UAE,
)

TAXI_DELIVERY_UAE_EUR_CONTEXT_SPENDABLE_SCOUTS = TAXI_DELIVERY_KZ_EUR_CONTEXT_SPENDABLE_SCOUTS.new(
    name='TAXI_DELIVERY_UAE_EUR_CONTEXT_SPENDABLE_SCOUTS',
    special_contract_params={'country': Regions.UAE.id},
    region=Regions.UAE,
)

TAXI_DELIVERY_ANG_EUR_CONTEXT = TAXI_DELIVERY_EST_EUR_CONTEXT.new(
    name='TAXI_DELIVERY_ANG_EUR_CONTEXT',
    special_contract_params={'personal_account': 1, 'country': Regions.ANG.id,
                             'partner_commission_pct2': Decimal('10.2'), },
    region=Regions.ANG,
)

TAXI_DELIVERY_ANG_EUR_CONTEXT_SPENDABLE = TAXI_DELIVERY_EST_EUR_CONTEXT_SPENDABLE.new(
    name='TAXI_DELIVERY_ANG_EUR_CONTEXT_SPENDABLE',
    special_contract_params={'country': Regions.ANG.id},
    region=Regions.ANG,
)

TAXI_DELIVERY_ANG_EUR_CONTEXT_SPENDABLE_SCOUTS = TAXI_DELIVERY_KZ_EUR_CONTEXT_SPENDABLE_SCOUTS.new(
    name='TAXI_DELIVERY_ANG_EUR_CONTEXT_SPENDABLE_SCOUTS',
    special_contract_params={'country': Regions.ANG.id},
    region=Regions.ANG,
)

TAXI_DELIVERY_ZAM_EUR_CONTEXT = TAXI_DELIVERY_EST_EUR_CONTEXT.new(
    name='TAXI_DELIVERY_ZAM_EUR_CONTEXT',
    special_contract_params={'personal_account': 1, 'country': Regions.ZAM.id,
                             'partner_commission_pct2': Decimal('10.2'), },
    region=Regions.ZAM,
)

TAXI_DELIVERY_ZAM_EUR_CONTEXT_SPENDABLE = TAXI_DELIVERY_EST_EUR_CONTEXT_SPENDABLE.new(
    name='TAXI_DELIVERY_ZAM_EUR_CONTEXT_SPENDABLE',
    special_contract_params={'country': Regions.ZAM.id},
    region=Regions.ZAM,
)

TAXI_DELIVERY_ZAM_EUR_CONTEXT_SPENDABLE_SCOUTS = TAXI_DELIVERY_KZ_EUR_CONTEXT_SPENDABLE_SCOUTS.new(
    name='TAXI_DELIVERY_ZAM_EUR_CONTEXT_SPENDABLE_SCOUTS',
    special_contract_params={'country': Regions.ZAM.id},
    region=Regions.ZAM,
)

TAXI_DELIVERY_ZAM_USD_CONTEXT = TAXI_DELIVERY_UZB_USD_CONTEXT.new(
    name='TAXI_DELIVERY_ZAM_USD_CONTEXT',
    special_contract_params={'personal_account': 1, 'country': Regions.ZAM.id,
                             'partner_commission_pct2': Decimal('10.2'), },
    region=Regions.ZAM,
)

TAXI_DELIVERY_ZAM_USD_CONTEXT_SPENDABLE = TAXI_DELIVERY_UZB_USD_CONTEXT_SPENDABLE.new(
    name='TAXI_DELIVERY_ZAM_USD_CONTEXT_SPENDABLE',
    special_contract_params={'country': Regions.ZAM.id},
    region=Regions.ZAM,
)

TAXI_DELIVERY_ZAM_USD_CONTEXT_SPENDABLE_SCOUTS = TAXI_DELIVERY_UZB_USD_CONTEXT_SPENDABLE_SCOUTS.new(
    name='TAXI_DELIVERY_ZAM_USD_CONTEXT_SPENDABLE_SCOUTS',
    special_contract_params={'country': Regions.ZAM.id},
    region=Regions.ZAM,
)

TAXI_DELIVERY_CIV_EUR_CONTEXT = TAXI_DELIVERY_EST_EUR_CONTEXT.new(
    name='TAXI_DELIVERY_CIV_EUR_CONTEXT',
    special_contract_params={'personal_account': 1, 'country': Regions.CIV.id,
                             'partner_commission_pct2': Decimal('10.2'), },
    region=Regions.CIV,
)

TAXI_DELIVERY_CIV_EUR_CONTEXT_SPENDABLE = TAXI_DELIVERY_EST_EUR_CONTEXT_SPENDABLE.new(
    name='TAXI_DELIVERY_CIV_EUR_CONTEXT_SPENDABLE',
    special_contract_params={'country': Regions.CIV.id},
    region=Regions.CIV,
)

TAXI_DELIVERY_CIV_EUR_CONTEXT_SPENDABLE_SCOUTS = TAXI_DELIVERY_KZ_EUR_CONTEXT_SPENDABLE_SCOUTS.new(
    name='TAXI_DELIVERY_CIV_EUR_CONTEXT_SPENDABLE_SCOUTS',
    special_contract_params={'country': Regions.CIV.id},
    region=Regions.CIV,
)

TAXI_DELIVERY_AZ_EUR_CONTEXT = TAXI_DELIVERY_EST_EUR_CONTEXT.new(
    name='TAXI_DELIVERY_AZ_EUR_CONTEXT',
    special_contract_params={'personal_account': 1, 'country': Regions.AZ.id,
                             'partner_commission_pct2': Decimal('10.2'), },
    region=Regions.AZ,
)

TAXI_DELIVERY_AZ_EUR_CONTEXT_SPENDABLE = TAXI_DELIVERY_EST_EUR_CONTEXT_SPENDABLE.new(
    name='TAXI_DELIVERY_AZ_EUR_CONTEXT_SPENDABLE',
    special_contract_params={'country': Regions.AZ.id},
    region=Regions.AZ,
)

TAXI_DELIVERY_AZ_EUR_CONTEXT_SPENDABLE_SCOUTS = TAXI_DELIVERY_KZ_EUR_CONTEXT_SPENDABLE_SCOUTS.new(
    name='TAXI_DELIVERY_AZ_EUR_CONTEXT_SPENDABLE_SCOUTS',
    special_contract_params={'country': Regions.AZ.id},
    region=Regions.AZ,
)

TAXI_DELIVERY_AZ_USD_CONTEXT = TAXI_DELIVERY_UZB_USD_CONTEXT.new(
    name='TAXI_DELIVERY_AZ_USD_CONTEXT',
    special_contract_params={'personal_account': 1, 'country': Regions.AZ.id,
                             'partner_commission_pct2': Decimal('10.2'), },
    region=Regions.AZ,
)

TAXI_DELIVERY_AZ_USD_CONTEXT_SPENDABLE = TAXI_DELIVERY_UZB_USD_CONTEXT_SPENDABLE.new(
    name='TAXI_DELIVERY_AZ_USD_CONTEXT_SPENDABLE',
    special_contract_params={'country': Regions.AZ.id},
    region=Regions.AZ,
)

TAXI_DELIVERY_AZ_USD_CONTEXT_SPENDABLE_SCOUTS = TAXI_DELIVERY_UZB_USD_CONTEXT_SPENDABLE_SCOUTS.new(
    name='TAXI_DELIVERY_AZ_USD_CONTEXT_SPENDABLE_SCOUTS',
    special_contract_params={'country': Regions.AZ.id},
    region=Regions.AZ,
)

TAXI_DELIVERY_MD_EUR_CONTEXT = TAXI_DELIVERY_EST_EUR_CONTEXT.new(
    name='TAXI_DELIVERY_MD_EUR_CONTEXT',
    special_contract_params={'personal_account': 1, 'country': Regions.MD.id,
                             'partner_commission_pct2': Decimal('10.2'), },
    region=Regions.MD,
)

TAXI_DELIVERY_MD_EUR_CONTEXT_SPENDABLE = TAXI_DELIVERY_EST_EUR_CONTEXT_SPENDABLE.new(
    name='TAXI_DELIVERY_MD_EUR_CONTEXT_SPENDABLE',
    special_contract_params={'country': Regions.MD.id},
    region=Regions.MD,
)

TAXI_DELIVERY_MD_EUR_CONTEXT_SPENDABLE_SCOUTS = TAXI_DELIVERY_KZ_EUR_CONTEXT_SPENDABLE_SCOUTS.new(
    name='TAXI_DELIVERY_MD_EUR_CONTEXT_SPENDABLE_SCOUTS',
    special_contract_params={'country': Regions.MD.id},
    region=Regions.MD,
)

TAXI_DELIVERY_RS_EUR_CONTEXT = TAXI_DELIVERY_EST_EUR_CONTEXT.new(
    name='TAXI_DELIVERY_RS_EUR_CONTEXT',
    special_contract_params={'personal_account': 1, 'country': Regions.RS.id,
                             'partner_commission_pct2': Decimal('10.2'), },
    region=Regions.RS,
)

TAXI_DELIVERY_RS_EUR_CONTEXT_SPENDABLE = TAXI_DELIVERY_EST_EUR_CONTEXT_SPENDABLE.new(
    name='TAXI_DELIVERY_RS_EUR_CONTEXT_SPENDABLE',
    special_contract_params={'country': Regions.RS.id},
    region=Regions.RS,
)

TAXI_DELIVERY_RS_EUR_CONTEXT_SPENDABLE_SCOUTS = TAXI_DELIVERY_KZ_EUR_CONTEXT_SPENDABLE_SCOUTS.new(
    name='TAXI_DELIVERY_RS_EUR_CONTEXT_SPENDABLE_SCOUTS',
    special_contract_params={'country': Regions.RS.id},
    region=Regions.RS,
)

TAXI_YA_TAXI_CORP_KZ_KZT_CONTEXT = Context().new(
    # common
    name='TAXI_YA_TAXI_CORP_KZ_KZT_CONTEXT',
    service=Services.TAXI,
    person_type=PersonTypes.KZU,
    firm=Firms.TAXI_CORP_KZT_31,
    currency=Currencies.KZT,
    paysys=Paysyses.BANK_UR_KZT_TAXI_CORP,
    nds=NdsNew.KAZAKHSTAN,
    contract_type=ContractSubtype.GENERAL,
    invoice_type=InvoiceType.PERSONAL_ACCOUNT,
    special_contract_params={'personal_account': 1, 'country': Regions.KZ.id,
                             'partner_commission_pct2': Decimal('10.2')},
    region=Regions.KZ,
    contract_services=[Services.TAXI.id, Services.UBER.id, Services.UBER_ROAMING.id,
                       Services.TAXI_111.id, Services.TAXI_128.id],
    monetization_services=[Services.TAXI_111.id, Services.TAXI_128.id],
    # thirdparty
    payment_currency=Currencies.KZT,
    tpt_payment_type=PaymentType.CARD,
    tpt_paysys_type_cc=PaysysType.MONEY,
    min_commission=Decimal('15'),
    precision=2,  # точность округления АВ
    currency_rate_src=CurrencyRateSource.NBKZ,
    # костыль для нескольких ЛС у заправок
    additional_paysys=Paysyses.CE_KZT_TAXI_CORP,
    zaxi_deposit_product=Products.ZAXI_DEPOSIT_KZT,
    migration_alias='taxi',
)

TAXI_YA_TAXI_CORP_KZ_KZT_CONTEXT_SPENDABLE = TAXI_YA_TAXI_CORP_KZ_KZT_CONTEXT.new(
    # common
    name='TAXI_YA_TAXI_CORP_KZ_KZT_CONTEXT_SPENDABLE',
    service=Services.TAXI_DONATE,
    contract_type=ContractSubtype.SPENDABLE,
    special_contract_params={'country': Regions.KZ.id},
    contract_services=[Services.TAXI_DONATE.id],
    # thirdparty
    tpt_paysys_type_cc=PaysysType.TAXI,
    payment_currency=Currencies.KZT,
    # partner_act_data
    pad_type_id=6,
)

TAXI_YA_TAXI_CORP_KZ_KZT_CONTEXT_SPENDABLE_SCOUTS = TAXI_YA_TAXI_CORP_KZ_KZT_CONTEXT_SPENDABLE.new(
    # common
    name='TAXI_YA_TAXI_CORP_KZ_KZT_CONTEXT_SPENDABLE_SCOUTS',
    service=Services.SCOUTS,
    contract_services=[Services.SCOUTS.id],
    # thirdparty
    tpt_payment_type=PaymentType.SCOUT,
    oebs_contract_type=None,
    # partner_act_data
    page_id=Pages.SCOUTS.id,
    pad_description=Pages.SCOUTS.desc,
    pad_type_id=4,
)

TAXI_BV_NOR_NOK_CONTEXT = TAXI_RU_CONTEXT.new(
    # common
    name='TAXI_BV_NOR_NOK_CONTEXT',
    person_type=PersonTypes.EU_YT,
    firm=Firms.TAXI_BV_22,
    currency=Currencies.NOK,
    paysys=Paysyses.BANK_UR_NOK_TAXI_BV,
    nds=NdsNew.NOT_RESIDENT,
    special_contract_params={'personal_account': 1, 'country': Regions.NOR.id,
                             'partner_commission_pct2': Decimal('0')},
    region=Regions.NOR,
    contract_services=[Services.TAXI.id, Services.TAXI_111.id, Services.TAXI_128.id],
    monetization_services=[Services.TAXI_111.id, Services.TAXI_128.id],
    # thirdparty
    payment_currency=Currencies.NOK,
    tpt_paysys_type_cc=PaysysType.MONEY,  # PaysysType.WALLET
    min_commission=Decimal('0.00'),  # минимальная комиссия АВ, если в договоре процент не 0 или вообще не задан
    currency_rate_src=CurrencyRateSource.ECB,
)

TAXI_BV_NOR_NOK_CONTEXT_SPENDABLE = TAXI_BV_NOR_NOK_CONTEXT.new(
    # common
    name='TAXI_BV_NOR_NOK_CONTEXT_SPENDABLE',
    service=Services.TAXI_DONATE,
    contract_type=ContractSubtype.SPENDABLE,
    special_contract_params={'country': Regions.NOR.id},
    contract_services=[Services.TAXI_DONATE.id],
    # thirdparty
    tpt_paysys_type_cc=PaysysType.TAXI,
    # partner_act_data
    pad_type_id=6,
)

# TAXI_ARM_NOR_NOK_CONTEXT = TAXI_BV_NOR_NOK_CONTEXT.new(
#     # todo: Для 122 фирмы и валюты NOK нет счета в t_bank_details, поэтому проверялось на фейковом счёте.
#     # todo: Перед запуском на проде нужно наинсертить реальный счёт.
#     # common
#     name='TAXI_ARM_NOR_NOK_CONTEXT',
#     person_type=PersonTypes.AM_YT,
#     firm=Firms.TAXI_CORP_ARM_122,
#     paysys=Paysyses.BANK_AM_YT_NOK_TAXI_CORP_ARM,
# )
#
# TAXI_ARM_NOR_NOK_CONTEXT_SPENDABLE = TAXI_ARM_NOR_NOK_CONTEXT.new(
#     # common
#     name='TAXI_ARM_NOR_NOK_CONTEXT_SPENDABLE',
#     service=Services.TAXI_DONATE,
#     contract_type=ContractSubtype.SPENDABLE,
#     special_contract_params={'country': Regions.NOR.id},
#     contract_services=[Services.TAXI_DONATE.id],
#     # thirdparty
#     tpt_paysys_type_cc=PaysysType.TAXI,
#     # partner_act_data
#     pad_type_id=6,
# )

TAXI_MLU_EUROPE_SWE_SEK_CONTEXT = TAXI_RU_CONTEXT.new(
    # common
    name='TAXI_MLU_EUROPE_SWE_SEK_CONTEXT',
    person_type=PersonTypes.EU_YT,
    firm=Firms.MLU_EUROPE_125,
    currency=Currencies.SEK,
    paysys=Paysyses.BANK_SEK_MLU_EUROPE_EU_YT,
    nds=NdsNew.NOT_RESIDENT,
    special_contract_params={'personal_account': 1, 'country': Regions.SWE.id,
                             'partner_commission_pct2': Decimal('0')},
    region=Regions.SWE,
    contract_services=[Services.TAXI.id, Services.TAXI_111.id, Services.TAXI_128.id],
    monetization_services=[Services.TAXI_111.id, Services.TAXI_128.id],
    # thirdparty
    payment_currency=Currencies.SEK,
    tpt_paysys_type_cc=PaysysType.MONEY,  # PaysysType.WALLET
    min_commission=Decimal('0.00'),  # минимальная комиссия АВ, если в договоре процент не 0 или вообще не задан
    currency_rate_src=CurrencyRateSource.ECB,
)

TAXI_MLU_EUROPE_SWE_SEK_CONTEXT_SPENDABLE_DONATE = TAXI_MLU_EUROPE_SWE_SEK_CONTEXT.new(
    # common
    name='TAXI_MLU_EUROPE_SWE_SEK_CONTEXT_SPENDABLE_DONATE',
    service=Services.TAXI_DONATE,
    contract_type=ContractSubtype.SPENDABLE,
    special_contract_params={'country': Regions.SWE.id},
    contract_services=[Services.TAXI_DONATE.id],
    # thirdparty
    tpt_paysys_type_cc=PaysysType.TAXI,
    # partner_act_data
    pad_type_id=6,
)

TAXI_MLU_EUROPE_SWE_SEK_CONTEXT_SPENDABLE_SCOUTS = TAXI_MLU_EUROPE_SWE_SEK_CONTEXT_SPENDABLE_DONATE.new(
    # common
    name='TAXI_MLU_EUROPE_SWE_SEK_CONTEXT_SPENDABLE_SCOUTS',
    service=Services.SCOUTS,
    contract_services=[Services.SCOUTS.id],
    # thirdparty
    tpt_payment_type=PaymentType.SCOUT,
    oebs_contract_type=None,
    # partner_act_data
    page_id=Pages.SCOUTS.id,
    pad_description=Pages.SCOUTS.desc,
    pad_type_id=4,
)

TAXI_MLU_EUROPE_CAMEROON_EUR_CONTEXT = Context().new(
    # common
    name='TAXI_MLU_EUROPE_CAMEROON_EUR_CONTEXT',
    service=Services.TAXI_111,
    person_type=PersonTypes.EU_YT,
    firm=Firms.MLU_EUROPE_125,
    currency=Currencies.EUR,
    paysys=Paysyses.BANK_EUR_MLU_EUROPE_EU_YT,
    nds=NdsNew.NOT_RESIDENT,
    contract_type=ContractSubtype.GENERAL,
    invoice_type=InvoiceType.PERSONAL_ACCOUNT,
    special_contract_params={'personal_account': 1, 'country': Regions.CMR.id},
    region=Regions.CMR,
    contract_services=[Services.TAXI_111.id, Services.TAXI_128.id],
    monetization_services=[Services.TAXI_111.id, Services.TAXI_128.id],
    # thirdparty
    payment_currency=Currencies.XAF,
    tpt_payment_type=PaymentType.CARD,
    tpt_paysys_type_cc=PaysysType.MONEY,
    min_commission=Decimal('0.01'),
    precision=2,  # точность округления АВ
    currency_rate_src=CurrencyRateSource.OANDA,
    migration_alias='taxi',
)

TAXI_MLU_EUROPE_CAMEROON_EUR_CONTEXT_SPENDABLE = TAXI_MLU_EUROPE_CAMEROON_EUR_CONTEXT.new(
    # common
    name='TAXI_MLU_EUROPE_CAMEROON_EUR_CONTEXT_SPENDABLE',
    service=Services.TAXI_DONATE,
    contract_type=ContractSubtype.SPENDABLE,
    special_contract_params={'country': Regions.CMR.id},
    contract_services=[Services.TAXI_DONATE.id],
    # thirdparty
    tpt_paysys_type_cc=PaysysType.TAXI,
    payment_currency=Currencies.EUR,
    # partner_act_data
    pad_type_id=6,
)

TAXI_MLU_EUROPE_CAMEROON_EUR_CONTEXT_SPENDABLE_SCOUTS = TAXI_MLU_EUROPE_CAMEROON_EUR_CONTEXT_SPENDABLE.new(
    # common
    name='TAXI_MLU_EUROPE_CAMEROON_EUR_CONTEXT_SPENDABLE_SCOUTS',
    service=Services.SCOUTS,
    contract_services=[Services.SCOUTS.id],
    # thirdparty
    tpt_payment_type=PaymentType.SCOUT,
    oebs_contract_type=None,
    # partner_act_data
    page_id=Pages.SCOUTS.id,
    pad_description=Pages.SCOUTS.desc,
    # partner_act_data
    pad_type_id=4,
)

TAXI_MLU_EUROPE_CONGO_EUR_CONTEXT = Context().new(
    # common
    name='TAXI_MLU_EUROPE_CONGO_EUR_CONTEXT',
    service=Services.TAXI_111,
    person_type=PersonTypes.EU_YT,
    firm=Firms.MLU_EUROPE_125,
    currency=Currencies.EUR,
    paysys=Paysyses.BANK_EUR_MLU_EUROPE_EU_YT,
    nds=NdsNew.NOT_RESIDENT,
    contract_type=ContractSubtype.GENERAL,
    invoice_type=InvoiceType.PERSONAL_ACCOUNT,
    special_contract_params={'personal_account': 1, 'country': Regions.COG.id,
                             'partner_commission_pct2': Decimal('10.2')},
    region=Regions.COG,
    contract_services=[Services.TAXI_111.id, Services.TAXI.id, Services.TAXI_128.id],
    monetization_services=[Services.TAXI_111.id, Services.TAXI_128.id],
    # thirdparty
    payment_currency=Currencies.CDF,
    tpt_payment_type=PaymentType.CARD,
    tpt_paysys_type_cc=PaysysType.MONEY,
    min_commission=Decimal('0.01'),
    precision=2,  # точность округления АВ
    currency_rate_src=CurrencyRateSource.OANDA,
    migration_alias='taxi',
)

TAXI_MLU_EUROPE_CONGO_EUR_CONTEXT_SPENDABLE = TAXI_MLU_EUROPE_CONGO_EUR_CONTEXT.new(
    # common
    name='TAXI_MLU_EUROPE_CONGO_EUR_CONTEXT_SPENDABLE',
    service=Services.TAXI_DONATE,
    contract_type=ContractSubtype.SPENDABLE,
    special_contract_params={'country': Regions.COG.id, 'partner_commission_pct2': Decimal('10.2')},
    contract_services=[Services.TAXI_DONATE.id],
    # thirdparty
    tpt_paysys_type_cc=PaysysType.TAXI,
    payment_currency=Currencies.EUR,
    # partner_act_data
    pad_type_id=6,
)

TAXI_MLU_EUROPE_CONGO_EUR_CONTEXT_SPENDABLE_SCOUTS = TAXI_MLU_EUROPE_CONGO_EUR_CONTEXT_SPENDABLE.new(
    # common
    name='TAXI_MLU_EUROPE_CONGO_EUR_CONTEXT_SPENDABLE_SCOUTS',
    service=Services.SCOUTS,
    contract_services=[Services.SCOUTS.id],
    # thirdparty
    tpt_payment_type=PaymentType.SCOUT,
    oebs_contract_type=None,
    # partner_act_data
    page_id=Pages.SCOUTS.id,
    pad_description=Pages.SCOUTS.desc,
    # partner_act_data
    pad_type_id=4,
)

TAXI_MLU_EUROPE_ALGERIA_EUR_CONTEXT = Context().new(
    # common
    name='TAXI_MLU_EUROPE_ALGERIA_EUR_CONTEXT',
    service=Services.TAXI_111,
    person_type=PersonTypes.EU_YT,
    firm=Firms.MLU_EUROPE_125,
    currency=Currencies.EUR,
    paysys=Paysyses.BANK_EUR_MLU_EUROPE_EU_YT,
    nds=NdsNew.NOT_RESIDENT,
    contract_type=ContractSubtype.GENERAL,
    invoice_type=InvoiceType.PERSONAL_ACCOUNT,
    special_contract_params={'personal_account': 1, 'country': Regions.DZA.id,
                             'partner_commission_pct2': Decimal('10.2')},
    region=Regions.DZA,
    contract_services=[Services.TAXI_111.id, Services.TAXI.id, Services.TAXI_128.id],
    monetization_services=[Services.TAXI_111.id, Services.TAXI_128.id],
    # thirdparty
    payment_currency=Currencies.DZD,
    tpt_payment_type=PaymentType.CARD,
    tpt_paysys_type_cc=PaysysType.MONEY,
    min_commission=Decimal('0.01'),
    precision=2,  # точность округления АВ
    currency_rate_src=CurrencyRateSource.OANDA,
    migration_alias='taxi',
)

TAXI_MLU_EUROPE_ALGERIA_EUR_CONTEXT_SPENDABLE = TAXI_MLU_EUROPE_ALGERIA_EUR_CONTEXT.new(
    # common
    name='TAXI_MLU_EUROPE_ALGERIA_EUR_CONTEXT_SPENDABLE',
    service=Services.TAXI_DONATE,
    contract_type=ContractSubtype.SPENDABLE,
    special_contract_params={'country': Regions.DZA.id, 'partner_commission_pct2': Decimal('10.2')},
    contract_services=[Services.TAXI_DONATE.id],
    # thirdparty
    tpt_paysys_type_cc=PaysysType.TAXI,
    payment_currency=Currencies.EUR,
    # partner_act_data
    pad_type_id=6,
)

TAXI_MLU_EUROPE_ALGERIA_EUR_CONTEXT_SPENDABLE_SCOUTS = TAXI_MLU_EUROPE_ALGERIA_EUR_CONTEXT_SPENDABLE.new(
    # common
    name='TAXI_MLU_EUROPE_ALGERIA_EUR_CONTEXT_SPENDABLE_SCOUTS',
    service=Services.SCOUTS,
    contract_services=[Services.SCOUTS.id],
    # thirdparty
    tpt_payment_type=PaymentType.SCOUT,
    oebs_contract_type=None,
    # partner_act_data
    page_id=Pages.SCOUTS.id,
    pad_description=Pages.SCOUTS.desc,
    # partner_act_data
    pad_type_id=4,
)

TAXI_MLU_EUROPE_ANGOLA_EUR_CONTEXT = TAXI_MLU_EUROPE_CAMEROON_EUR_CONTEXT.new(
    name=u'TAXI_MLU_EUROPE_ANGOLA_EUR_CONTEXT',
    region=Regions.ANG,
    special_contract_params={'personal_account': 1, 'country': Regions.ANG.id},
    payment_currency=Currencies.AOA,
)

TAXI_MLU_EUROPE_ANGOLA_EUR_CONTEXT_SPENDABLE = TAXI_MLU_EUROPE_ANGOLA_EUR_CONTEXT.new(
    # common
    name='TAXI_MLU_EUROPE_ANGOLA_EUR_CONTEXT_SPENDABLE',
    service=Services.TAXI_DONATE,
    contract_type=ContractSubtype.SPENDABLE,
    special_contract_params={'country': Regions.ANG.id},
    contract_services=[Services.TAXI_DONATE.id],
    # thirdparty
    tpt_paysys_type_cc=PaysysType.TAXI,
    # partner_act_data
    pad_type_id=6,
)

TAXI_MLU_EUROPE_ANGOLA_EUR_CONTEXT_SPENDABLE_SCOUTS = TAXI_MLU_EUROPE_ANGOLA_EUR_CONTEXT_SPENDABLE.new(
    # common
    name='TAXI_MLU_EUROPE_ANGOLA_EUR_CONTEXT_SPENDABLE_SCOUTS',
    service=Services.SCOUTS,
    contract_services=[Services.SCOUTS.id],
    # thirdparty
    tpt_payment_type=PaymentType.SCOUT,
    oebs_contract_type=None,
    # partner_act_data
    page_id=Pages.SCOUTS.id,
    pad_description=Pages.SCOUTS.desc,
    # partner_act_data
    pad_type_id=4,
)

TAXI_MLU_EUROPE_SENEGAL_EUR_CONTEXT = Context().new(
    # common
    name='TAXI_MLU_EUROPE_SENEGAL_EUR_CONTEXT',
    service=Services.TAXI_111,
    person_type=PersonTypes.EU_YT,
    firm=Firms.MLU_EUROPE_125,
    currency=Currencies.EUR,
    paysys=Paysyses.BANK_EUR_MLU_EUROPE_EU_YT,
    nds=NdsNew.NOT_RESIDENT,
    contract_type=ContractSubtype.GENERAL,
    invoice_type=InvoiceType.PERSONAL_ACCOUNT,
    special_contract_params={'personal_account': 1, 'country': Regions.SEN.id},
    region=Regions.SEN,
    contract_services=[Services.TAXI_111.id, Services.TAXI_128.id],
    monetization_services=[Services.TAXI_111.id, Services.TAXI_128.id],
    # thirdparty
    payment_currency=Currencies.XOF,
    tpt_payment_type=PaymentType.CARD,
    tpt_paysys_type_cc=PaysysType.MONEY,
    min_commission=Decimal('0.01'),
    precision=2,  # точность округления АВ
    currency_rate_src=CurrencyRateSource.BCEAO,
    migration_alias='taxi',
)

TAXI_MLU_EUROPE_SENEGAL_EUR_CONTEXT_SPENDABLE = TAXI_MLU_EUROPE_SENEGAL_EUR_CONTEXT.new(
    # common
    name='TAXI_MLU_EUROPE_SENEGAL_EUR_CONTEXT_SPENDABLE',
    service=Services.TAXI_DONATE,
    contract_type=ContractSubtype.SPENDABLE,
    special_contract_params={'country': Regions.SEN.id},
    contract_services=[Services.TAXI_DONATE.id],
    # thirdparty
    tpt_paysys_type_cc=PaysysType.TAXI,
    payment_currency=Currencies.EUR,
    # partner_act_data
    pad_type_id=6,
)

TAXI_MLU_EUROPE_SENEGAL_EUR_CONTEXT_SPENDABLE_SCOUTS = TAXI_MLU_EUROPE_SENEGAL_EUR_CONTEXT_SPENDABLE.new(
    # common
    name='TAXI_MLU_EUROPE_SENEGAL_EUR_CONTEXT_SPENDABLE_SCOUTS',
    service=Services.SCOUTS,
    contract_services=[Services.SCOUTS.id],
    # thirdparty
    tpt_payment_type=PaymentType.SCOUT,
    oebs_contract_type=None,
    # partner_act_data
    page_id=Pages.SCOUTS.id,
    pad_description=Pages.SCOUTS.desc,
    # partner_act_data
    pad_type_id=4,
)

ZAXI_RU_CONTEXT = Context().new(
    # common
    name='ZAXI_RU_CONTEXT',
    service=Services.ZAXI,
    person_type=PersonTypes.UR,
    firm=Firms.GAS_STATIONS_124,
    currency=Currencies.RUB,
    paysys=Paysyses.BANK_UR_RUB_GAS_STATIONS,
    nds=NdsNew.DEFAULT,
    invoice_type=InvoiceType.PERSONAL_ACCOUNT,
    contract_type=ContractSubtype.GENERAL,
    # thirdparty
    tpt_payment_type=PaymentType.DEPOSIT,
    tpt_paysys_type_cc=PaysysType.FUEL_HOLD,
    payment_currency=Currencies.RUB,
    b30_logbroker_logic=1
)

ZAXI_DELIVERY_RU_CONTEXT = ZAXI_RU_CONTEXT.new(
    name='ZAXI_DELIVERY_RU_CONTEXT',
    service=Services.ZAXI_DELIVERY,
    b30_logbroker_logic=0
)

ZAXI_KZ_COMMISSION_CONTEXT = Context().new(
    # common
    name='ZAXI_KZ_COMMISSION_CONTEXT',
    service=Services.ZAXI,
    person_type=PersonTypes.KZU,
    firm=Firms.ZAPRAVKI_KZ_1096,
    currency=Currencies.KZT,
    paysys=Paysyses.BANK_UR_KZT_ZAPRAVKI_KZ,
    nds=NdsNew.KAZAKHSTAN,
    invoice_type=InvoiceType.PERSONAL_ACCOUNT,
    contract_type=ContractSubtype.GENERAL,
    # thirdparty
    tpt_payment_type=PaymentType.DEPOSIT,
    tpt_paysys_type_cc=PaysysType.FUEL_HOLD,
    payment_currency=Currencies.KZT,
)

ZAXI_KZ_AGENT_CONTEXT = Context().new(
    # common
    name='ZAXI_KZ_AGENT_CONTEXT',
    service=Services.ZAXI_AGENT_COMMISSION,
    person_type=PersonTypes.KZU,
    firm=Firms.ZAPRAVKI_KZ_1096,
    currency=Currencies.KZT,
    paysys=Paysyses.BANK_UR_KZT_ZAPRAVKI_KZ,
    nds=NdsNew.KAZAKHSTAN,
    invoice_type=InvoiceType.PERSONAL_ACCOUNT,
    contract_type=ContractSubtype.GENERAL,
    special_contract_params={'country': Regions.KZ.id},
    # thirdparty
    tpt_paysys_type_cc=PaysysType.TAXI,
    payment_currency=Currencies.KZT,
)

ZAXI_RU_SPENDABLE_CONTEXT = ZAXI_RU_CONTEXT.new(
    # common
    name='ZAXI_RU_SPENDABLE_CONTEXT',
    service=Services.ZAXI_SPENDABLE,
    contract_type=ContractSubtype.SPENDABLE,
)

ZAXI_RU_SELFEMPLOYED_TIPS_CONTEXT = ZAXI_RU_CONTEXT.new(
    name='ZAXI_RU_SELFEMPLOYED_TIPS_CONTEXT',
    person_type=PersonTypes.PH,
    service=Services.ZAXI_SELFEMPLOYED_TIPS,
    special_contract_params={'personal_account': 1},
    paysys=Paysyses.BANK_PH_RUB_GAS_STATIONS,
)

SCOUTS_RU_CONTEXT = Context().new(
    # common
    name='SCOUTS_RU_CONTEXT',
    service=Services.SCOUTS,
    person_type=PersonTypes.UR,
    firm=Firms.TAXI_13,
    currency=Currencies.RUB,
    nds=NdsNew.DEFAULT,
    contract_type=ContractSubtype.SPENDABLE,
    special_contract_params={'country': Regions.RU.id},
    contract_services=[Services.SCOUTS.id],
    # thirdparty
    payment_currency=Currencies.RUB,
    tpt_payment_type=PaymentType.SCOUT,
    tpt_paysys_type_cc=PaysysType.TAXI,
    oebs_contract_type=None,
    # partner_act_data
    page_id=Pages.SCOUTS.id,
    pad_description=Pages.SCOUTS.desc,
    pad_type_id=4,
    region=Regions.RU,
)

SCOUTS_KZ_CONTEXT = SCOUTS_RU_CONTEXT.new(
    name='SCOUTS_KZ_CONTEXT',
    person_type=PersonTypes.KZU,
    firm=Firms.TAXI_CORP_KZT_31,
    currency=Currencies.KZT,
    nds=NdsNew.KAZAKHSTAN,
    special_contract_params={'country': Regions.KZ.id},
    # thirdparty
    payment_currency=Currencies.KZT,
    tpt_payment_type=PaymentType.SCOUT,
    tpt_paysys_type_cc=PaysysType.TAXI,
    oebs_contract_type=None,
    page_id=Pages.SCOUTS.id,
    pad_description=Pages.SCOUTS.desc,
    pad_type_id=4,

)

# BUSES
BUSES_RU_CONTEXT = Context().new(
    # common
    name='BUSES_RU_CONTEXT',
    service=Services.BUSES,
    person_type=PersonTypes.UR,
    firm=Firms.YANDEX_1,
    currency=Currencies.RUB,
    nds=NdsNew.DEFAULT,
    invoice_type=InvoiceType.PERSONAL_ACCOUNT,
    contract_type=ContractSubtype.GENERAL,
    paysys=Paysyses.BANK_UR_RUB,
    special_contract_params={'personal_account': 1},
    # thirdparty
    payment_currency=Currencies.RUB,
    tpt_payment_type=PaymentType.CARD,
    tpt_paysys_type_cc=PaysysType.MONEY,
    min_commission=Decimal('0.01'),
)

GAS_STATION_RU_CONTEXT = Context().new(
    # common
    name='GAS_STATION_RU_CONTEXT',
    service=Services.GAS_STATIONS,
    person_type=PersonTypes.UR,
    paysys=Paysyses.BANK_UR_RUB_GAS_STATIONS,
    contract_type=ContractSubtype.GENERAL,
    manager=None,
    firm=Firms.GAS_STATIONS_124,
    currency=Currencies.RUB,
    nds=NdsNew.DEFAULT,
    invoice_type=InvoiceType.PERSONAL_ACCOUNT,
    special_contract_params={'personal_account': 1},
    # thirdparty
    tpt_payment_type=PaymentType.CARD,
    tpt_paysys_type_cc=PaysysType.MONEY,
    payment_currency=Currencies.RUB,
)

# CORP_TAXI
CORP_TAXI_RU_CONTEXT_SPENDABLE = Context().new(
    name='CORP_TAXI_RU_CONTEXT',
    service=Services.TAXI_CORP,
    person_type=PersonTypes.UR,
    firm=Firms.TAXI_13,
    currency=Currencies.RUB,
    nds=NdsNew.DEFAULT,
    contract_type=ContractSubtype.SPENDABLE,
    invoice_type=InvoiceType.PERSONAL_ACCOUNT,
    special_contract_params={'country': Regions.RU.id, 'region': CountryRegion.RUS},
    # thirdparty
    tpt_payment_type=PaymentType.CASH,
    tpt_paysys_type_cc=PaysysType.YANDEX,
    payment_currency=Currencies.RUB,
    # partner_act_data
    page_id=Pages.CORP_TAXI.id,
    pad_description=Pages.CORP_TAXI.desc,
    pad_type_id=6,
    migration_alias='corp_taxi',
)

CORP_TAXI_RU_CONTEXT_SPENDABLE_MIGRATED = CORP_TAXI_RU_CONTEXT_SPENDABLE.new(
    name='CORP_TAXI_RU_CONTEXT_SPENDABLE_MIGRATED',
    contract_services=[Services.TAXI_CORP.id, Services.TAXI_CORP_PARTNERS.id],
)

CORP_TAXI_KZ_CONTEXT_SPENDABLE = CORP_TAXI_RU_CONTEXT_SPENDABLE.new(
    name='CORP_TAXI_KZ_CONTEXT_SPENDABLE',
    person_type=PersonTypes.KZU,
    firm=Firms.TAXI_CORP_KZT_31,
    currency=Currencies.KZT,
    nds=NdsNew.KAZAKHSTAN,
    special_contract_params={'country': Regions.KZ.id, 'partner_commission_pct': Decimal('3.4')},
    payment_currency=Currencies.KZT,
)

CORP_TAXI_KZ_CONTEXT_SPENDABLE_MIGRATED = CORP_TAXI_KZ_CONTEXT_SPENDABLE.new(
    name='CORP_TAXI_KZ_CONTEXT_SPENDABLE_MIGRATED',
    contract_services=[Services.TAXI_CORP.id, Services.TAXI_CORP_PARTNERS.id],
)

CORP_TAXI_KZ_CONTEXT_SPENDABLE_DECOUP = CORP_TAXI_KZ_CONTEXT_SPENDABLE.new(
    name='CORP_TAXI_KZ_CONTEXT_SPENDABLE_DECOUP',
    contract_services=[Services.TAXI_CORP_PARTNERS.id],
)

CORP_TAXI_ARM_CONTEXT_SPENDABLE = CORP_TAXI_RU_CONTEXT_SPENDABLE.new(
    name='CORP_TAXI_ARM_CONTEXT_SPENDABLE',
    person_type=PersonTypes.AM_UR,
    firm=Firms.TAXI_CORP_ARM_122,
    currency=Currencies.AMD,
    nds=NdsNew.ARMENIA,
    special_contract_params={'country': Regions.ARM.id, 'partner_commission_pct': Decimal('3.4')},
    payment_currency=Currencies.AMD,
)

CORP_TAXI_ARM_CONTEXT_SPENDABLE_MIGRATED = CORP_TAXI_ARM_CONTEXT_SPENDABLE.new(
    name='CORP_TAXI_ARM_CONTEXT_SPENDABLE_MIGRATED',
    contract_services=[Services.TAXI_CORP.id, Services.TAXI_CORP_PARTNERS.id],
)

CORP_TAXI_RU_CONTEXT_SPENDABLE_DECOUP = CORP_TAXI_RU_CONTEXT_SPENDABLE.new(
    name='CORP_TAXI_RU_CONTEXT_SPENDABLE_DECOUP',
    service=Services.TAXI_CORP_PARTNERS,
)

CORP_TAXI_ISRAEL_CONTEXT_SPENDABLE = CORP_TAXI_RU_CONTEXT_SPENDABLE.new(
    name='CORP_TAXI_ISRAEL_CONTEXT_SPENDABLE',
    person_type=PersonTypes.IL_UR,
    firm=Firms.YANDEX_GO_ISRAEL_35,
    currency=Currencies.ILS,
    nds=NdsNew.ISRAEL,
    special_contract_params={
        'country': Regions.ISR.id,
        'partner_commission_pct': Decimal('3.4'),
        'ctype': 'SPENDABLE',
        'israel_tax_pct': Decimal('7.1')
    },
    payment_currency=Currencies.ILS,
)

CORP_TAXI_ISRAEL_CONTEXT_SPENDABLE_MIGRATED = CORP_TAXI_RU_CONTEXT_SPENDABLE.new(
    name='CORP_TAXI_ISRAEL_CONTEXT_SPENDABLE_MIGRATED',
    person_type=PersonTypes.IL_UR,
    firm=Firms.YANDEX_GO_ISRAEL_35,
    currency=Currencies.ILS,
    nds=NdsNew.ISRAEL,
    special_contract_params={
        'country': Regions.ISR.id,
        'partner_commission_pct': Decimal('3.4'),
        'ctype': 'SPENDABLE',
        'israel_tax_pct': Decimal('7.1')
    },
    payment_currency=Currencies.ILS,
    contract_services=[Services.TAXI_CORP.id, Services.TAXI_CORP_PARTNERS.id],
)

CORP_TAXI_KGZ_CONTEXT_SPENDABLE = CORP_TAXI_RU_CONTEXT_SPENDABLE.new(
    name='CORP_TAXI_KGZ_CONTEXT_SPENDABLE',
    person_type=PersonTypes.KG_UR,
    firm=Firms.TAXI_CORP_KGZ_1100,
    currency=Currencies.KGS,
    nds=NdsNew.KG,
    special_contract_params={
        'country': Regions.KGZ.id,
#        'partner_commission_pct': Decimal('3.4'),
#         'ctype': 'SPENDABLE',
    },
    payment_currency=Currencies.KGS,
    contract_services=[Services.TAXI_CORP_PARTNERS.id],
)

CORP_TAXI_RU_CONTEXT_GENERAL = CORP_TAXI_RU_CONTEXT_SPENDABLE.new(
    name='CORP_TAXI_RU_CONTEXT_GENERAL',
    contract_type=ContractSubtype.GENERAL,
    paysys=Paysyses.BANK_UR_RUB_TAXI,
    invoice_type=InvoiceType.PERSONAL_ACCOUNT,
    special_contract_params={'personal_account': 1}
)

CORP_TAXI_RU_CONTEXT_GENERAL_DECOUP = CORP_TAXI_RU_CONTEXT_SPENDABLE.new(
    name='CORP_TAXI_RU_CONTEXT_GENERAL_DECOUP',
    service=Services.TAXI_CORP_CLIENTS,
    contract_type=ContractSubtype.GENERAL,
    paysys=Paysyses.BANK_UR_RUB_TAXI,
    invoice_type=InvoiceType.PERSONAL_ACCOUNT,
    special_contract_params={'personal_account': 1},
    contract_services=[Services.TAXI_CORP_CLIENTS.id],
)

CORP_TAXI_RU_CONTEXT_GENERAL_MIGRATED = CORP_TAXI_RU_CONTEXT_SPENDABLE.new(
    name='CORP_TAXI_RU_CONTEXT_GENERAL_MIGRATED',
    service=Services.TAXI_CORP_CLIENTS,
    contract_type=ContractSubtype.GENERAL,
    paysys=Paysyses.BANK_UR_RUB_TAXI,
    invoice_type=InvoiceType.PERSONAL_ACCOUNT,
    contract_services=[Services.TAXI_CORP_CLIENTS.id, Services.TAXI_CORP.id],
    special_contract_params={'personal_account': 1},
)

CORP_TAXI_KZ_CONTEXT_GENERAL = CORP_TAXI_KZ_CONTEXT_SPENDABLE.new(
    name='CORP_TAXI_KZ_CONTEXT_GENERAL',
    contract_type=ContractSubtype.GENERAL,
    paysys=Paysyses.BANK_UR_KZT_TAXI_CORP,
    invoice_type=InvoiceType.PERSONAL_ACCOUNT,
    special_contract_params={'personal_account': 1},
)

CORP_TAXI_KZ_CONTEXT_GENERAL_MIGRATED = CORP_TAXI_KZ_CONTEXT_GENERAL.new(
    name='CORP_TAXI_KZ_CONTEXT_GENERAL_MIGRATED',
    service=Services.TAXI_CORP_CLIENTS,
    contract_services=[Services.TAXI_CORP_CLIENTS.id, Services.TAXI_CORP.id],
)

CORP_TAXI_KZ_CONTEXT_GENERAL_DECOUP = CORP_TAXI_KZ_CONTEXT_GENERAL.new(
    name='CORP_TAXI_KZ_CONTEXT_GENERAL_DECOUP',
    contract_services=[Services.TAXI_CORP_CLIENTS.id],
)

CORP_TAXI_ISRAEL_CONTEXT_GENERAL = CORP_TAXI_ISRAEL_CONTEXT_SPENDABLE.new(
    name='CORP_TAXI_ISRAEL_CONTEXT_GENERAL',
    contract_type=ContractSubtype.GENERAL,
    paysys=Paysyses.BANK_IL_UR_ILS,
    invoice_type=InvoiceType.PERSONAL_ACCOUNT,
    special_contract_params={'personal_account': 1},
)

CORP_TAXI_ISRAEL_CONTEXT_GENERAL_MIGRATED = CORP_TAXI_ISRAEL_CONTEXT_SPENDABLE_MIGRATED.new(
    name='CORP_TAXI_ISRAEL_CONTEXT_GENERAL_MIGRATED',
    service=Services.TAXI_CORP_CLIENTS,
    contract_type=ContractSubtype.GENERAL,
    paysys=Paysyses.BANK_IL_UR_ILS,
    invoice_type=InvoiceType.PERSONAL_ACCOUNT,
    special_contract_params={'personal_account': 1},
    contract_services=[Services.TAXI_CORP_CLIENTS.id, Services.TAXI_CORP.id],
)

CORP_TAXI_ISRAEL_CONTEXT_GENERAL_DECOUP = CORP_TAXI_ISRAEL_CONTEXT_GENERAL_MIGRATED.new(
    name='CORP_TAXI_ISRAEL_CONTEXT_GENERAL_DECOUP',
    contract_services=[Services.TAXI_CORP_CLIENTS.id, ],
)

CORP_TAXI_YANGO_ISRAEL_CONTEXT_GENERAL_DECOUP = CORP_TAXI_ISRAEL_CONTEXT_GENERAL_DECOUP.new(
    name='CORP_TAXI_YANGO_ISRAEL_CONTEXT_GENERAL_DECOUP',
    firm=Firms.YANGO_ISRAEL_1090,
    paysys=Paysyses.BANK_YANGO_IL_UR_ILS,
)

CORP_TAXI_BY_CONTEXT_SPENDABLE = CORP_TAXI_RU_CONTEXT_SPENDABLE.new(
    name='CORP_TAXI_BY_CONTEXT_SPENDABLE',
    person_type=PersonTypes.BYU,
    firm=Firms.BELGO_CORP_128,
    currency=Currencies.BYN,
    nds=NdsNew.BELARUS,
    special_contract_params={'country': Regions.BY.id, 'partner_commission_pct': Decimal('3.4')},
    payment_currency=Currencies.BYN,
)

CORP_TAXI_BY_CONTEXT_SPENDABLE_DECOUP = CORP_TAXI_BY_CONTEXT_SPENDABLE.new(
    name='CORP_TAXI_BY_CONTEXT_SPENDABLE_DECOUP',
    service = Services.TAXI_CORP_PARTNERS,
    contract_services=[Services.TAXI_CORP_PARTNERS.id],
)

CORP_TAXI_BY_CONTEXT_SPENDABLE_MIGRATED = CORP_TAXI_BY_CONTEXT_SPENDABLE.new(
    name='CORP_TAXI_BY_CONTEXT_SPENDABLE_MIGRATED',
    contract_services=[Services.TAXI_CORP.id, Services.TAXI_CORP_PARTNERS.id],
)


CORP_TAXI_BY_CONTEXT_GENERAL = CORP_TAXI_BY_CONTEXT_SPENDABLE.new(
    name='CORP_TAXI_BY_CONTEXT_GENERAL',
    contract_type=ContractSubtype.GENERAL,
    paysys=Paysyses.BANK_UR_BYN_BELGO_CORP,
    invoice_type=InvoiceType.PERSONAL_ACCOUNT,
    special_contract_params={'personal_account': 1},
)

CORP_TAXI_BY_CONTEXT_GENERAL_DECOUP = CORP_TAXI_BY_CONTEXT_GENERAL.new(
    name='CORP_TAXI_BY_CONTEXT_GENERAL_DECOUP',
    service=Services.TAXI_CORP_CLIENTS,
    contract_services=[Services.TAXI_CORP_CLIENTS.id],
)

CORP_TAXI_BY_CONTEXT_GENERAL_MIGRATED = CORP_TAXI_BY_CONTEXT_GENERAL.new(
    name='CORP_TAXI_BY_CONTEXT_GENERAL_MIGRATED',
    service=Services.TAXI_CORP_CLIENTS,
    contract_services=[Services.TAXI_CORP_CLIENTS.id, Services.TAXI_CORP.id],
)

CORP_TAXI_KGZ_CONTEXT_GENERAL = CORP_TAXI_KGZ_CONTEXT_SPENDABLE.new(
    name='CORP_TAXI_KGZ_CONTEXT_GENERAL',
    service=Services.TAXI_CORP_CLIENTS,
    contract_type=ContractSubtype.GENERAL,
    paysys=Paysyses.BANK_UR_KGS_TAXI_CORP_KGZ,
    invoice_type=InvoiceType.PERSONAL_ACCOUNT,
    special_contract_params={'personal_account': 1},
    contract_services=[Services.TAXI_CORP_CLIENTS.id],
)


CORP_DISPATCHING_BY_CONTEXT = Context().new(
    name='CORP_DISPATCHING_BY_CONTEXT',
    service=Services.CORP_DISPATCHING,
    contract_services=[Services.CORP_DISPATCHING.id],
    person_type=PersonTypes.BYU,
    firm=Firms.BELGO_CORP_128,
    currency=Currencies.BYN,
    nds=NdsNew.BELARUS,
    contract_type=ContractSubtype.GENERAL,
    invoice_type=InvoiceType.PERSONAL_ACCOUNT,
    paysys=Paysyses.BANK_UR_BYN_BELGO_CORP,
    is_offer=True,
    special_contract_params={'personal_account': 1},
    migration_alias='taxi_corp_dispatching',
)


DELIVERY_DISPATCHING_BY_CONTEXT = Context().new(
    name='DELIVERY_DISPATCHING_BY_CONTEXT',
    service=Services.DELIVERY_DISPATCHING,
    contract_services=[Services.DELIVERY_DISPATCHING.id],
    person_type=PersonTypes.BYU,
    firm=Firms.YANDEX_DELIVERY_BY,
    currency=Currencies.BYN,
    nds=NdsNew.BELARUS,
    contract_type=ContractSubtype.GENERAL,
    invoice_type=InvoiceType.PERSONAL_ACCOUNT,
    paysys=Paysyses.BANK_BYU_DELIVERY_BY_BYN,
    is_offer=True,
    special_contract_params={'personal_account': 1},
)


CORP_TAXI_USN_RU_CONTEXT = CORP_TAXI_RU_CONTEXT_SPENDABLE.new(
    name='CORP_TAXI_USN_RU_CONTEXT',
    service=Services.TAXI_CORP_CLIENTS_USN_AGENT,
    balance_service=Services.TAXI_CORP_CLIENTS_USN_GENERAL,
    agent_service=Services.TAXI_CORP_CLIENTS_USN_AGENT,
    general_order_type='client_b2b_agent_reward',
    agent_order_type='client_b2b_agent_payment',
    contract_type=ContractSubtype.GENERAL,
    paysys=Paysyses.BANK_UR_RUB_TAXI,
    invoice_type=InvoiceType.PERSONAL_ACCOUNT,
    special_contract_params={'personal_account': 1},
    contract_services=[Services.TAXI_CORP_CLIENTS_USN_GENERAL.id, Services.TAXI_CORP_CLIENTS_USN_AGENT.id],
)

CORP_TAXI_USN_RU_SPENDABLE_CONTEXT = Context().new(
    name='CORP_TAXI_USN_RU_SPENDABLE_CONTEXT',
    service=Services.TAXI_CORP_CLIENTS_USN_SPENDABLE,
    person_type=PersonTypes.UR,
    firm=Firms.TAXI_13,
    currency=Currencies.RUB,
    nds=NdsNew.DEFAULT,
    contract_type=ContractSubtype.SPENDABLE,
    invoice_type=InvoiceType.PERSONAL_ACCOUNT,
    special_contract_params={'country': Regions.RU.id, 'region': CountryRegion.RUS},
)


# FOOD CORP

FOOD_CORP_CONTEXT = Context().new(
    name='FOOD_CORP_CONTEXT',
    service=Services.FOOD_CORP,
    contract_services=[Services.FOOD_CORP.id],
    person_type=PersonTypes.UR,
    firm=Firms.TAXI_13,
    currency=Currencies.RUB,
    nds=NdsNew.DEFAULT,
    contract_type=ContractSubtype.GENERAL,
    invoice_type=InvoiceType.PERSONAL_ACCOUNT,
    paysys=Paysyses.BANK_UR_RUB_TAXI,
    special_contract_params={
        'personal_account': 1
    },
    migration_alias='food_corp',
)

# LOGISTICS

LOGISTICS_PARTNERS_RU_CONTEXT_SPENDABLE = Context().new(
    name='LOGISTICS_PARTNERS_RU_CONTEXT_SPENDABLE',
    service=Services.LOGISTICS_PARTNERS,
    person_type=PersonTypes.UR,
    firm=Firms.LOGISTICS_130,
    region=Regions.RU,
    currency=Currencies.RUB,
    nds=NdsNew.DEFAULT,
    contract_type=ContractSubtype.SPENDABLE,
    is_offer=1,
    special_contract_params={'country': Regions.RU.id, 'region': CountryRegion.RUS},
    # thirdparty
    tpt_paysys_type_cc=PaysysType.YANDEX,
    payment_currency=Currencies.RUB,
    # partner_act_data
    pad_type_id=6,
)

LOGISTICS_PARTNERS_RU_CONTEXT_SPENDABLE_SCOUTS = LOGISTICS_PARTNERS_RU_CONTEXT_SPENDABLE.new(
    # common
    name='LOGISTICS_PARTNERS_RU_CONTEXT_SPENDABLE_SCOUTS',
    service=Services.SCOUTS,
    contract_services=[Services.SCOUTS.id],
    # thirdparty
    tpt_payment_type=PaymentType.SCOUT,
    oebs_contract_type=None,
    # partner_act_data
    page_id=Pages.SCOUTS.id,
    pad_description=Pages.SCOUTS.desc,
    # partner_act_data
    pad_type_id=4,
)

LOGISTICS_CLIENTS_RU_CONTEXT_GENERAL = Context().new(
    name='LOGISTICS_CLIENTS_RU_CONTEXT_GENERAL',
    service=Services.LOGISTICS_CLIENTS,
    person_type=PersonTypes.UR,
    firm=Firms.LOGISTICS_130,
    currency=Currencies.RUB,
    nds=NdsNew.DEFAULT,
    contract_type=ContractSubtype.GENERAL,
    is_offer=1,
    invoice_type=InvoiceType.PERSONAL_ACCOUNT,
    special_contract_params={'personal_account': 1},
    paysys=Paysyses.BANK_UR_RUB_LOGISTICS,
    contract_services=[Services.LOGISTICS_CLIENTS.id],
    region=Regions.RU,
)

LOGISTICS_PAYMENTS_RU_CONTEXT_GENERAL = LOGISTICS_CLIENTS_RU_CONTEXT_GENERAL.new(
    name='LOGISTICS_PAYMENTS_RU_CONTEXT_GENERAL',
    contract_services=[Services.LOGISTICS_CLIENTS.id, Services.LOGISTICS_PAYMENTS.id],
)


LOGISTICS_PARTNERS_ISRAEL_CONTEXT_SPENDABLE = Context().new(
    name='LOGISTICS_PARTNERS_ISRAEL_CONTEXT_SPENDABLE',
    service=Services.LOGISTICS_PARTNERS,
    person_type=PersonTypes.IL_UR,
    firm=Firms.YANDEX_GO_ISRAEL_35,
    currency=Currencies.ILS,
    nds=NdsNew.ISRAEL,
    contract_type=ContractSubtype.SPENDABLE,
    is_offer=1,
    special_contract_params={
        'country': Regions.ISR.id,
        'israel_tax_pct': Decimal('7.1')
    },
    # thirdparty
    tpt_paysys_type_cc=PaysysType.YANDEX,
    payment_currency=Currencies.ILS,
    # partner_act_data
    pad_type_id=6,
)


LOGISTICS_CLIENTS_ISRAEL_CONTEXT_GENERAL = Context().new(
    name='LOGISTICS_CLIENTS_ISRAEL_CONTEXT_GENERAL',
    service=Services.LOGISTICS_CLIENTS,
    person_type=PersonTypes.IL_UR,
    firm=Firms.YANDEX_GO_ISRAEL_35,
    currency=Currencies.ILS,
    nds=NdsNew.ISRAEL,
    contract_type=ContractSubtype.GENERAL,
    is_offer=1,
    invoice_type=InvoiceType.PERSONAL_ACCOUNT,
    special_contract_params={'personal_account': 1},
    paysys=Paysyses.BANK_IL_UR_ILS,
    contract_services=[Services.LOGISTICS_CLIENTS.id],
)

LOGISTICS_PARTNERS_YANGO_DELIVERY_BEOGRAD_SERBIA_CONTEXT_SPENDABLE = Context().new(
    name='LOGISTICS_PARTNERS_YANGO_DELIVERY_BEOGRAD_SERBIA_CONTEXT_SPENDABLE',
    service=Services.LOGISTICS_PARTNERS,
    person_type=PersonTypes.SRB_UR,
    firm=Firms.YANGO_DELIVERY_BEOGRAD_1898,
    currency=Currencies.RSD,
    nds=NdsNew.RS,
    contract_type=ContractSubtype.SPENDABLE,
    is_offer=1,
    special_contract_params={'country': Regions.RS.id},
    # thirdparty
    payment_currency=Currencies.RSD,
    # partner_act_data
    pad_type_id=6,
)

LOGISTICS_CLIENTS_YANGO_DELIVERY_BEOGRAD_SERBIA_CONTEXT_GENERAL = Context().new(
    name='LOGISTICS_CLIENTS_YANGO_DELIVERY_BEOGRAD_SERBIA_CONTEXT_GENERAL',
    service=Services.LOGISTICS_CLIENTS,
    person_type=PersonTypes.SRB_UR,
    firm=Firms.YANGO_DELIVERY_BEOGRAD_1898,
    currency=Currencies.RSD,
    nds=NdsNew.RS,
    contract_type=ContractSubtype.GENERAL,
    is_offer=1,
    invoice_type=InvoiceType.PERSONAL_ACCOUNT,
    special_contract_params={'personal_account': 1},
    paysys=Paysyses.BANK_SRB_UR_RSD,
    contract_services=[Services.LOGISTICS_CLIENTS.id],
)

LOGISTICS_PARTNERS_YANGO_CHILE_SPA_CONTEXT_SPENDABLE = Context().new(
    name='LOGISTICS_PARTNERS_YANGO_CHILE_SPA_CONTEXT_SPENDABLE',
    service=Services.LOGISTICS_PARTNERS,
    person_type=PersonTypes.CL_UR,
    firm=Firms.YANGO_CHILE_SPA,
    currency=Currencies.CLP,
    nds=NdsNew.CL,
    contract_type=ContractSubtype.SPENDABLE,
    is_offer=1,
    special_contract_params={'country': Regions.CL.id},
    # thirdparty
    payment_currency=Currencies.CLP,
    # partner_act_data
    pad_type_id=6,
)

LOGISTICS_CLIENTS_YANGO_CHILE_SPA_CONTEXT_GENERAL = Context().new(
    name='LOGISTICS_CLIENTS_YANGO_CHILE_SPA_CONTEXT_GENERAL',
    service=Services.LOGISTICS_CLIENTS,
    person_type=PersonTypes.CL_UR,
    firm=Firms.YANGO_CHILE_SPA,
    currency=Currencies.CLP,
    nds=NdsNew.CL,
    contract_type=ContractSubtype.GENERAL,
    is_offer=1,
    invoice_type=InvoiceType.PERSONAL_ACCOUNT,
    special_contract_params={'personal_account': 1},
    paysys=Paysyses.BANK_CL_UR_CLP,
    contract_services=[Services.LOGISTICS_CLIENTS.id],
)

LOGISTICS_PARTNERS_BY_CONTEXT_SPENDABLE = Context().new(
    name='LOGISTICS_PARTNERS_BY_CONTEXT_SPENDABLE',
    service=Services.LOGISTICS_PARTNERS,
    person_type=PersonTypes.BYU,
    firm=Firms.YANDEX_DELIVERY_BY,
    currency=Currencies.BYN,
    nds=NdsNew.BELARUS,
    contract_type=ContractSubtype.SPENDABLE,
    is_offer=1,
    special_contract_params={'country': Regions.BY.id},
    # thirdparty
    tpt_paysys_type_cc=PaysysType.YANDEX,
    payment_currency=Currencies.BYN,
    # partner_act_data
    pad_type_id=6,
)


LOGISTICS_CLIENTS_BY_CONTEXT_GENERAL = Context().new(
    name='LOGISTICS_CLIENTS_BY_CONTEXT_GENERAL',
    service=Services.LOGISTICS_CLIENTS,
    person_type=PersonTypes.BYU,
    firm=Firms.YANDEX_DELIVERY_BY,
    currency=Currencies.BYN,
    nds=NdsNew.BELARUS,
    contract_type=ContractSubtype.GENERAL,
    is_offer=1,
    invoice_type=InvoiceType.PERSONAL_ACCOUNT,
    special_contract_params={'personal_account': 1},
    paysys=Paysyses.BANK_BYU_DELIVERY_BY_BYN,
    contract_services=[Services.LOGISTICS_CLIENTS.id],
)

LOGISTICS_PARTNERS_KZ_CONTEXT_SPENDABLE = Context().new(
    name='LOGISTICS_PARTNERS_KZ_CONTEXT_SPENDABLE',
    service=Services.LOGISTICS_PARTNERS,
    person_type=PersonTypes.KZU,
    firm=Firms.YANDEX_DELIVERY_KZ,
    currency=Currencies.KZT,
    nds=NdsNew.KAZAKHSTAN,
    contract_type=ContractSubtype.SPENDABLE,
    is_offer=1,
    special_contract_params={'country': Regions.KZ.id, 'partner_commission_pct': Decimal('3.4')},
    # thirdparty
    tpt_paysys_type_cc=PaysysType.YANDEX,
    payment_currency=Currencies.KZT,
    # partner_act_data
    pad_type_id=6,
)

LOGISTICS_CLIENTS_KZ_CONTEXT_GENERAL = Context().new(
    name='LOGISTICS_CLIENTS_KZ_CONTEXT_GENERAL',
    service=Services.LOGISTICS_CLIENTS,
    person_type=PersonTypes.KZU,
    firm=Firms.YANDEX_DELIVERY_KZ,
    currency=Currencies.KZT,
    nds=NdsNew.KAZAKHSTAN,
    contract_type=ContractSubtype.GENERAL,
    is_offer=1,
    invoice_type=InvoiceType.PERSONAL_ACCOUNT,
    special_contract_params={'personal_account': 1},
    paysys=Paysyses.BANK_KZU_DELIVERY_KZ_KZT,
    contract_services=[Services.LOGISTICS_CLIENTS.id],
)


LOGISTICS_PARTNERS_AM_CONTEXT_SPENDABLE = Context().new(
    name='LOGISTICS_PARTNERS_AM_CONTEXT_SPENDABLE',
    service=Services.LOGISTICS_PARTNERS,
    person_type=PersonTypes.AM_UR,
    firm=Firms.TAXI_CORP_AM,
    currency=Currencies.AMD,
    nds=NdsNew.ARMENIA,
    contract_type=ContractSubtype.SPENDABLE,
    is_offer=1,
    special_contract_params={'country': Regions.ARM.id, 'partner_commission_pct': Decimal('3.4')},
    # thirdparty
    tpt_paysys_type_cc=PaysysType.YANDEX,
    payment_currency=Currencies.AMD,
    # partner_act_data
    pad_type_id=6,
)

TAXI_SAMOKAT_RU_CONTEXT_GENERAL = Context().new(
    name='TAXI_SAMOKAT_RU_CONTEXT_GENERAL',
    service=Services.TAXI_SAMOKAT,
    person_type=PersonTypes.PH,
    firm=Firms.TAXI_13,
    currency=Currencies.RUB,
    nds=NdsNew.DEFAULT,
    contract_type=ContractSubtype.GENERAL,
    invoice_type=InvoiceType.PERSONAL_ACCOUNT,
    special_contract_params={'personal_account': 1},
    paysys=Paysyses.BANK_PH_RUB_TAXI,
    contract_services=[Services.TAXI_SAMOKAT.id],
    payment_currency=Currencies.RUB,
    partner_integration_cc='taxi_samokat_payments',
    partner_configuration_cc='taxi_samokat_payments_conf',
)

TAXI_SAMOKAT_MICRO_MOBILITY_RU_CONTEXT_GENERAL = TAXI_SAMOKAT_RU_CONTEXT_GENERAL.new(
    name='TAXI_SAMOKAT_MICRO_MOBILITY_RU_CONTEXT_GENERAL',
    firm=Firms.MICRO_MOBILITY,
    paysys=Paysyses.BANK_PH_RUB_MICRO_MOBILITY,
)

TAXI_SAMOKAT_WIND_CONTEXT = Context().new(
    name='TAXI_SAMOKAT_WIND_CONTEXT',
    service=Services.TAXI_SAMOKAT,
    person_type=PersonTypes.IL_PH,
    firm=Firms.WIND_1099,
    currency=Currencies.ILS,
    nds=NdsNew.ISRAEL,
    contract_type=ContractSubtype.GENERAL,
    invoice_type=InvoiceType.PERSONAL_ACCOUNT,
    special_contract_params={'personal_account': 1},
    paysys=Paysyses.BANK_IL_PH_ILS_SAMOKAT,
    contract_services=[
        Services.TAXI_SAMOKAT.id,
        # Services.TAXI_SAMOKAT_CARD.id
    ],
    payment_currency=Currencies.ILS,
    partner_integration_cc='taxi_samokat_payments',
    partner_configuration_cc='taxi_samokat_payments_wind_conf',
)

# PLUS
PLUS_2_0_INCOME_CONTEXT = Context().new(
    name='PLUS_2_0_INCOME',
    service=Services.PLUS_2_0_INCOME,
    person_type=PersonTypes.UR,
    firm=Firms.YANDEX_1,
    paysys=Paysyses.BANK_UR_RUB,
    currency=Currencies.RUB,
    nds=NdsNew.DEFAULT,
    contract_type=ContractSubtype.GENERAL,
    invoice_type=InvoiceType.PERSONAL_ACCOUNT,
    payment_currency=Currencies.RUB,
    special_contract_params={'personal_account': 1},
    tpt_paysys_type_cc=PaysysType.MONEY,
)

PLUS_2_0_EXPENDITURE_CONTEXT = PLUS_2_0_INCOME_CONTEXT.new(
    name='PLUS_2_0_EXPENDITURE',
    service=Services.PLUS_2_0_EXPENDITURE,
    contract_type=ContractSubtype.SPENDABLE,
    invoice_type=InvoiceType.PERSONAL_ACCOUNT,
)


# TICKETS
TICKETS_118_CONTEXT = Context().new(
    name='TICKETS_118_CONTEXT',
    service=Services.TICKETS,
    person_type=PersonTypes.UR,
    firm=Firms.MEDIASERVICES_121,
    paysys=Paysyses.BANK_UR_RUB_MEDIASERVICES,
    currency=Currencies.RUB,
    nds=NdsNew.DEFAULT,
    contract_type=ContractSubtype.GENERAL,
    invoice_type=InvoiceType.PERSONAL_ACCOUNT,
    special_contract_params={'personal_account': 1},
    tpt_payment_type=PaymentType.CARD,
    tpt_paysys_type_cc=PaysysType.MONEY,
    payment_currency=Currencies.RUB,
)

TICKETS_MOVIEPASS_CONTEXT = TICKETS_118_CONTEXT.new(
    # common
    name='TICKETS_MOVIEPASS_CONTEXT',
    service=Services.AFISHA_MOVIEPASS,
    contract_services=[Services.TICKETS.id, Services.AFISHA_MOVIEPASS.id]
)

TICKETS_MOVIEPASS_TARIFFICATOR_CONTEXT = TICKETS_MOVIEPASS_CONTEXT.new(
    # common
    name='TICKETS_MOVIEPASS_TARIFFICATOR_CONTEXT',
    service=Services.AFISHA_MOVIEPASS_TARIFFICATOR,
    contract_services=[Services.TICKETS.id, Services.AFISHA_MOVIEPASS_TARIFFICATOR.id, Services.AFISHA_MOVIEPASS.id]
)

EVENTS_TICKETS2_RU_CONTEXT = Context().new(
    # common
    name='EVENTS_TICKETS2_RU_CONTEXT',
    service=Services.EVENTS_TICKETS_NEW,
    person_type=PersonTypes.UR,
    firm=Firms.MEDIASERVICES_121,
    currency=Currencies.RUB,
    paysys=Paysyses.BANK_UR_RUB_MEDIASERVICES,
    paysys_wo_nds=Paysyses.BANK_UR_RUB_WO_NDS_MEDIASERVICES,
    nds=NdsNew.DEFAULT,
    contract_type=ContractSubtype.GENERAL,
    invoice_type=InvoiceType.PERSONAL_ACCOUNT,
    special_contract_params={'personal_account': 1, 'country': Regions.RU.id},
    contract_services=[Services.EVENTS_TICKETS_NEW.id],
    # thirdparty
    payment_currency=Currencies.RUB,
    tpt_payment_type=PaymentType.DIRECT_CARD,
    tpt_paysys_type_cc=PaysysType.MONEY,
    # min_commission=Decimal('0'),
)

EVENTS_TICKETS2_KZ_CONTEXT = EVENTS_TICKETS2_RU_CONTEXT.new(
    name='EVENTS_TICKETS2_KZ_CONTEXT',
    firm=Firms.KZ_25,
    currency=Currencies.KZT,
    payment_currency=Currencies.KZT,
    nds=NdsNew.KAZAKHSTAN,
    person_type=PersonTypes.KZU,
    paysys=Paysyses.BANK_KZ_UR_TG,
    tpt_paysys_type_cc=PaysysType.ALFA,
    special_contract_params={'personal_account': 1, 'country': Regions.KZ.id},
)


EVENTS_TICKETS3_RU_CONTEXT = Context().new(
    # common
    name='EVENTS_TICKETS3_RU_CONTEXT',
    service=Services.EVENTS_TICKETS3,
    person_type=PersonTypes.UR,
    paysys=Paysyses.BANK_UR_RUB_MEDIASERVICES,
    paysys_wo_nds=Paysyses.BANK_UR_RUB_WO_NDS_MEDIASERVICES,
    firm=Firms.MEDIASERVICES_121,
    currency=Currencies.RUB,
    nds=NdsNew.DEFAULT,
    invoice_type=InvoiceType.PERSONAL_ACCOUNT,
    contract_type=ContractSubtype.GENERAL,
    special_contract_params={'personal_account': 1, 'country': Regions.RU.id},
    # thirdparty
    tpt_payment_type=PaymentType.DIRECT_CARD,
    tpt_paysys_type_cc=PaysysType.MONEY,
    payment_currency=Currencies.RUB,
)

EVENTS_TICKETS_CONTEXT = EVENTS_TICKETS3_RU_CONTEXT.new(
    # common
    name='EVENTS_TICKETS_CONTEXT',
    service=Services.EVENTS_TICKETS,
)

SUBAGENCY_EVENTS_TICKETS2_RU_CONTEXT = EVENTS_TICKETS2_RU_CONTEXT.new(
    name='SUBAGENCY_EVENTS_TICKETS2_RU_CONTEXT',
    service=Services.SUBAGENCY_EVENTS_TICKETS_NEW,
    partner_integration={'scheme': SCHEMES.subagency_tickets.default},
    contract_services=[Services.SUBAGENCY_EVENTS_TICKETS_NEW.id],
)

SUBAGENCY_EVENTS_TICKETS3_RU_CONTEXT = EVENTS_TICKETS3_RU_CONTEXT.new(
    name='SUBAGENCY_EVENTS_TICKETS3_RU_CONTEXT',
    service=Services.SUBAGENCY_EVENTS_TICKETS3,
    partner_integration={'scheme': SCHEMES.subagency_tickets.default},
)

SUBAGENCY_EVENTS_TICKETS_CONTEXT = EVENTS_TICKETS_CONTEXT.new(
    name='SUBAGENCY_EVENTS_TICKETS_CONTEXT',
    service=Services.SUBAGENCY_EVENTS_TICKETS,
    partner_integration={'scheme': SCHEMES.subagency_tickets.default},
)

MEDIA_ADVANCE_RU_CONTEXT = Context().new(
    # common
    name='MEDIA_ADVANCE_RU_CONTEXT',
    service=Services.MEDIA_ADVANCE,
    person_type=PersonTypes.UR,
    paysys=Paysyses.BANK_UR_RUB_MEDIASERVICES,
    paysys_wo_nds=Paysyses.BANK_UR_RUB_WO_NDS_MEDIASERVICES,
    firm=Firms.MEDIASERVICES_121,
    currency=Currencies.RUB,
    nds=NdsNew.DEFAULT,
    invoice_type=InvoiceType.PERSONAL_ACCOUNT,
    contract_type=ContractSubtype.GENERAL,
    special_contract_params={'personal_account': 1, 'country': Regions.RU.id},
    # thirdparty
    tpt_payment_type=PaymentType.DIRECT_CARD,
    tpt_paysys_type_cc=PaysysType.MONEY,
    payment_currency=Currencies.RUB,
)


# CLOUD
CLOUD_RU_CONTEXT = Context().new(
    # common
    name='CLOUD_RU_CONTEXT',
    service=Services.CLOUD_143,
    person_type=PersonTypes.UR,
    firm=Firms.CLOUD_123,
    currency=Currencies.RUB,
    paysys=Paysyses.BANK_UR_RUB_CLOUD,
    nds=NdsNew.DEFAULT,
    contract_type=ContractSubtype.GENERAL,
    contract_services=[Services.CLOUD_143.id],
    invoice_type=InvoiceType.PERSONAL_ACCOUNT,
    special_contract_params={'personal_account': 1},
    product=Products.CLOUD,
    region=Regions.RU,
    tickets='TEST-0',
    memo='description',
)

CLOUD_KZ_CONTEXT = CLOUD_RU_CONTEXT.new(
    # common
    name='CLOUD_KZ_CONTEXT',
    person_type=PersonTypes.KZU,
    firm=Firms.CLOUD_KZ,
    currency=Currencies.KZT,
    paysys=Paysyses.BANK_UR_KZT_CLOUD,
    nds=NdsNew.KAZAKHSTAN,
    product=Products.CLOUD_KZ,
    region=Regions.KZ
)

CLOUD_AG_CONTEXT = CLOUD_RU_CONTEXT.new(
    name='CLOUD_AG_CONTEXT',
    firm=Firms.SERVICES_AG_16,
    person_type=PersonTypes.SW_YTPH,
    nds=NdsNew.NOT_RESIDENT,
)

CLOUD_MARKETPLACE_CONTEXT = CLOUD_RU_CONTEXT.new(
    name='CLOUD_MARKETPLACE_CONTEXT',
    service=Services.CLOUD_MARKETPLACE_144,
    contract_type=ContractSubtype.SPENDABLE,
    page_id=Pages.CLOUD_MARKETPLACE.id,
    contract_services=[Services.CLOUD_MARKETPLACE_144.id],
    pad_description=Pages.CLOUD_MARKETPLACE.desc,
    pad_type_id=6,
    source_id=30,
)

CLOUD_MARKETPLACE_SAG_CONTEXT = CLOUD_MARKETPLACE_CONTEXT.new(
    name='CLOUD_MARKETPLACE_SAG_CONTEXT',
    person_type=PersonTypes.SW_YT,
    firm=Firms.SERVICES_AG_16,
    currency=Currencies.USD,
    paysys=None,
    nds=NdsNew.ZERO,
    page_id=Pages.CLOUD_MARKETPLACE.id,
    pad_description=Pages.CLOUD_MARKETPLACE.desc,
    region=None
)

CLOUD_REFERAL_CONTEXT = CLOUD_RU_CONTEXT.new(
    name='CLOUD_REFERAL_CONTEXT',
    service=Services.CLOUD_REFERAL,
    contract_type=ContractSubtype.SPENDABLE,
    page_id=Pages.CLOUD_REFERAL.id,
    contract_services=[Services.CLOUD_REFERAL.id],
    pad_description=Pages.CLOUD_REFERAL.desc,
    pad_type_id=6,
    partner_integration={'scheme': SCHEMES.cloud_referal.cloud_referal},
)

HOSTING_SERVICE = Context().new(
    name='HOSTING_SERVICE',
    service=Services.HOSTING_SERVICE,
    person_type=PersonTypes.UR,
    firm=Firms.YANDEX_1,
    currency=Currencies.RUB,
    paysys=Paysyses.BANK_UR_RUB,
    nds=NdsNew.DEFAULT,
    contract_type=ContractSubtype.GENERAL,
    contract_services=[Services.HOSTING_SERVICE.id],
    invoice_type=InvoiceType.PERSONAL_ACCOUNT,
    special_contract_params={'personal_account': 1},
    product=Products.HOSTING_SERVICE,
    region=Regions.RU
)

BLUE_MARKET_PAYMENTS = Context().new(
    # common
    name='BLUE_MARKET_CONTEXT',
    service=Services.BLUE_MARKET_PAYMENTS,
    person_type=PersonTypes.UR,
    firm=Firms.MARKET_111,
    currency=Currencies.RUB,
    paysys=Paysyses.BANK_UR_RUB_MARKET,
    nds=NdsNew.DEFAULT,
    contract_type=ContractSubtype.GENERAL,
    invoice_type=InvoiceType.PERSONAL_ACCOUNT,
    special_contract_params={'personal_account': 1, 'netting': 1},
    manager=Managers.SOME_MANAGER.uid,
    contract_services=[Services.BLUE_MARKET_PAYMENTS.id, Services.BLUE_MARKET.id],
    # thirdparty
    payment_currency=Currencies.RUB,
    tpt_payment_type=PaymentType.CARD,
    tpt_paysys_type_cc=PaysysType.MONEY,  # PaysysType.ALFA
    # min_commission=Decimal('0'), #АВ всегда 0, т.к. на commission_category не смотрим, а в договоре поля для процента для России нет
    # precision=2, #точность округления АВ
    # currency_rate_src=CurrencyRateSource.CBRF,
)

BLUE_MARKET_612_ISRAEL = BLUE_MARKET_PAYMENTS.new(
    name='BLUE_MARKET_612_ISRAEL',
    person_type=PersonTypes.IL_UR,
    firm=Firms.MARKET_ISRAEL_1097,
    currency=Currencies.ILS,
    paysys=Paysyses.BANK_IL_UR_ILS_MARKET,  # без ндс надо?
    nds=NdsNew.ISRAEL,
    is_offer=1,

    payment_currency=Currencies.ILS,
    special_contract_params={
        'country': Regions.ISR.id,
        # 'partner_commission_pct': Decimal('3.4'),
        'israel_tax_pct': Decimal('7.1'),
        'personal_account': 1, 'netting': 1
    },
)

PURPLE_MARKET_612_USD = BLUE_MARKET_PAYMENTS.new(
    name='PURPLE_MARKET_612_USD',
    person_type=PersonTypes.YT,
    currency=Currencies.USD,
    nds=NdsNew.NONE,
    service=Services.BLUE_MARKET,
    contract_services=[Services.BLUE_MARKET.id],
    firm=Firms.MARKET_111,
    paysys=Paysyses.BANK_UR_RUB_MARKET,
    contract_type=ContractSubtype.GENERAL,
    invoice_type=InvoiceType.PERSONAL_ACCOUNT,
    special_contract_params={
        'personal_account': 1,
        'netting': 1,
        'netting_pct': Decimal('100')
    },
    manager=Managers.SOME_MANAGER.uid,
)

PURPLE_MARKET_612_EUR = PURPLE_MARKET_612_USD.new(
    name='PURPLE_MARKET_612_EUR',
    currency=Currencies.EUR,
)

BLUE_MARKET_REFUNDS = BLUE_MARKET_PAYMENTS.new(
    # common
    name='BLUE_MARKET_REFUNDS',
    service=Services.BLUE_MARKET_REFUNDS,
    person_type=PersonTypes.PH,
    contract_services=[Services.BLUE_MARKET_REFUNDS.id],
    contract_type=ContractSubtype.SPENDABLE,
    nds=NdsNew.ZERO,
    special_contract_params={'payment_type': 1},
)

BLUE_MARKET_SUBSIDY = BLUE_MARKET_PAYMENTS.new(
    # common
    name='BLUE_MARKET_SUBSIDY',
    service=Services.BLUE_MARKET_SUBSIDY,
    person_type=PersonTypes.UR,
    contract_type=ContractSubtype.SPENDABLE,
    contract_services=[Services.BLUE_MARKET_SUBSIDY.id],
    special_contract_params={},
    # thirdparty
    tpt_paysys_type_cc=PaysysType.MARKET,
    tpt_payment_type=PaymentType.SUBSIDY,
    # partner_act_data
    page_id=Pages.BLUEMARKETSUBSIDY.id,
    pad_description=Pages.BLUEMARKETSUBSIDY.desc,
    pad_type_id=6,
)
BLUE_MARKET_SUBSIDY_ISRAEL = BLUE_MARKET_612_ISRAEL.new(
    name='BLUE_MARKET_SUBSIDY_ISRAEL',
    service=Services.BLUE_MARKET_SUBSIDY,
    contract_type=ContractSubtype.SPENDABLE,
    contract_services=[Services.BLUE_MARKET_SUBSIDY.id],
    special_contract_params={
        'country': Regions.ISR.id,
        # 'partner_commission_pct': Decimal('3.4'),
        'israel_tax_pct': Decimal('8.4')
    },

    tpt_paysys_type_cc=PaysysType.MARKET,
    tpt_payment_type=PaymentType.SUBSIDY,

    page_id=Pages.BLUEMARKETSUBSIDY.id,
    pad_description=Pages.BLUEMARKETSUBSIDY.desc,
    pad_type_id=6,
)

BLUE_MARKET_SUBSIDY_SPASIBO = BLUE_MARKET_SUBSIDY.new(
    name='BLUE_MARKET_SUBSIDY_SPASIBO',
    # thirdparty
    tpt_paysys_type_cc=PaysysType.SPASIBO,
    tpt_payment_type=PaymentType.SPASIBO,
    # partner_act_data
    page_id=Pages.SPASIBO.id,
    pad_description=Pages.SPASIBO.desc,
)

BLUE_MARKET_PAYMENTS_TECH = BLUE_MARKET_PAYMENTS.new(
    # common
    name='BLUE_MARKET_PAYMENTS_TECH'
)

DRIVE_CONTEXT = Context().new(
    # common
    name='DRIVE_CONTEXT',
    service=Services.DRIVE,
    person_type=PersonTypes.PH,
    firm=Firms.DRIVE_30,
    currency=Currencies.RUB,
    paysys=Paysyses.BANK_PH_RUB_CARSHARING,
    nds=NdsNew.DEFAULT,
    contract_type=ContractSubtype.GENERAL,
    invoice_type=InvoiceType.PERSONAL_ACCOUNT,
    special_contract_params={'personal_account': 1},
    contract_services=[Services.DRIVE.id],
)

PLUS_DRIVE_CONTEXT = DRIVE_CONTEXT.new(
    name='PLUS_DRIVE_CONTEXT',
    service=Services.DRIVE_PLUS_2_0,
)

TELEMEDICINE_CONTEXT = Context().new(
    # common
    name='TELEMEDICINE_CONTEXT',
    service=Services.MEDICINE_PAY,
    person_type=PersonTypes.UR,
    firm=Firms.HEALTH_114,
    currency=Currencies.RUB,
    paysys=Paysyses.BANK_UR_RUB_MEDICINE,
    nds=NdsNew.DEFAULT,
    contract_type=ContractSubtype.GENERAL,
    invoice_type=InvoiceType.PERSONAL_ACCOUNT,
    special_contract_params={
        'personal_account': 1,
        'medicine_pay_commission': Decimal('30'),
        'medicine_pay_commission2': Decimal('20'),
        'commission': ContractCommissionType.NO_AGENCY.id
    },
    contract_services=[Services.MEDICINE_PAY.id, Services.TELEMEDICINE2.id],
    # thirdparty
    payment_currency=Currencies.RUB,
    tpt_payment_type=PaymentType.CARD,
    tpt_paysys_type_cc=PaysysType.MONEY,
    min_commission=Decimal('0.01'),
    precision=2,  # точность округления АВ
    product=Products.TELEMEDICINE2,
)

TELEMEDICINE_LICENSE_CONTEXT = TELEMEDICINE_CONTEXT.new(
    name='TELEMEDICINE_LICENSE_CONTEXT',
    nds=NdsNew.NONE,
    paysys=Paysyses.BANK_UR_RUB_MEDICINE_WO_NDS,
    special_contract_params={
        'personal_account': 1,
        'medicine_pay_commission': Decimal('0'),
        'medicine_pay_commission2': Decimal('100'),
        'commission': ContractCommissionType.LICENSE.id
    },
    product=Products.TELEMEDICINE2_WO_NDS
)

TELEMEDICINE_SPENDABLE_CONTEXT = TELEMEDICINE_CONTEXT.new(
    # common
    name='TELEMEDICINE_SPENDABLE_CONTEXT',
    service=Services.TELEMEDICINE_DONATE,
    contract_type=ContractSubtype.SPENDABLE,
    contract_services=[Services.TELEMEDICINE_DONATE.id],
    # thirdparty
    tpt_payment_type=PaymentType.COUPON,
    tpt_paysys_type_cc=PaysysType.YANDEX,
    # partner_act_data
    page_id=Pages.TELEMED_COUPON.id,
    pad_description=Pages.TELEMED_COUPON.desc,
    pad_type_id=6,
)

ZEN_SPENDABLE_COMMON = Context().new(
    # common
    name='__ZEN_SPENDABLE_COMMON',
    service=Services.ZEN,
    person_type=PersonTypes.PH,
    firm=Firms.ZEN_28,
    currency=Currencies.RUB,
    nds=NdsNew.DEFAULT,
    contract_type=ContractSubtype.SPENDABLE,
    contract_services=[Services.ZEN.id],
    oebs_contract_type=35,
    # thirdparty
    source_with_nds=True,
    payment_currency=Currencies.RUB,
    tpt_paysys_type_cc=PaysysType.MONEY,
    tpt_payment_type=PaymentType.BALALAYKA_PAYMENT,
    tpt_internal=None,
    # partner_act_data
    partner_acts=False,
    page_id=Pages.ZEN.id,
    pad_description=Pages.ZEN.desc,
    pad_type_id=4,
)

ZEN_SPENDABLE_CONTEXT = ZEN_SPENDABLE_COMMON.new(
    # Физики РФ
    # Скоро дожен отмереть, связанный договор с Firms.SERVICES_AG_16 создается без привязки к интеграции,
    # расходные акты не создаются, НДФЛ добавляется в ТТ
    name='ZEN_SPENDABLE_CONTEXT',
    partner_integration={'scheme': SCHEMES.dzen_writer.ph},
    link_contract_context_name='ZEN_SPENDABLE_SERVICES_AG_CONTEXT',
)

ZEN_SPENDABLE_NEW_CONTEXT = ZEN_SPENDABLE_COMMON.new(
    # Физики РФ
    # Должен заменить ZEN_SPENDABLE_CONTEXT, акты все также не создаются, связанного договра нет,
    # НДФЛ в ТТ добавлять не нужно, так как ОЕБС берет расчет НДФЛ на себя
    name='ZEN_SPENDABLE_NEW_CONTEXT',
    firm=Firms.YANDEX_1,
    partner_integration={'scheme': SCHEMES.dzen_writer.ph},
)

ZEN_SPENDABLE_IP_CONTEXT = Context().new(
    # ИП РФ, НДС скорей всего будет 0, так как ИП на УСН
    # расходные акты создаются, платежи сервис присылает по-новому в ЫТь,
    # суммы в платежах без НДС (добавляем сами в ТТ)
    # Новый service_id из-за требований ОЕБСs
    name='ZEN_SPENDABLE_IP_CONTEXT',
    service=Services.ZEN_IP_PAYMENT,
    person_type=PersonTypes.UR,
    firm=Firms.ZEN_28,
    currency=Currencies.RUB,
    nds=NdsNew.ZERO,
    contract_type=ContractSubtype.SPENDABLE,
    contract_services=[Services.ZEN_IP_PAYMENT.id],
    oebs_contract_type=35,
    # thirdparty
    source_with_nds=False,
    payment_currency=Currencies.RUB,
    tpt_paysys_type_cc=PaysysType.YANDEX,
    tpt_payment_type=PartnerPaymentType.WALLET,
    # partner_act_data
    partner_acts=True,
    page_id=Pages.ZEN_IP_PAYMENTS.id,
    pad_description=Pages.ZEN_IP_PAYMENTS.desc,
    pad_type_id=6,
    partner_integration={'scheme': SCHEMES.dzen_writer.ur_ip},
)

ZEN_SPENDABLE_UR_ORG_CONTEXT = ZEN_SPENDABLE_IP_CONTEXT.new(
    # ООО РФ, почти полный аналог ZEN_SPENDABLE_IP_CONTEXT, НДС может быть нулевой и стандартный
    # расходные акты создаются, платежи сервис присылает по-новому в ЫТь,
    # суммы в платежах без НДС (добавляем сами в ТТ)
    # Новый service_id, отличный от ZEN_SPENDABLE_IP_CONTEXT, из-за требований ОЕБСs
    name='ZEN_SPENDABLE_UR_ORG_CONTEXT',
    service=Services.ZEN_UR_ORG_PAYMENT,
    contract_services=[Services.ZEN_UR_ORG_PAYMENT.id],
    nds=NdsNew.DEFAULT,
    tpt_internal=1,  # выплачиваем по актам, раз в месяц
    page_id=Pages.ZEN_UR_ORG_PAYMENTS.id,
    pad_description=Pages.ZEN_UR_ORG_PAYMENTS.desc,
    partner_integration={'scheme': SCHEMES.dzen_writer.ur_org},
)

ZEN_SPENDABLE_SERVICES_AG_CONTEXT = ZEN_SPENDABLE_COMMON.new(
    # нерезиденты РФ
    name='ZEN_SPENDABLE_SERVICES_AG_CONTEXT',
    firm=Firms.SERVICES_AG_16,
    person_type=PersonTypes.SW_YTPH,
    nds=NdsNew.NOT_RESIDENT,
    tpt_internal=1,
    partner_integration={'scheme': SCHEMES.dzen_writer.non_resident},
)

DMP_SPENDABLE_CONTEXT = Context().new(
    # common
    name='DMP_SPENDABLE_CONTEXT',
    service=Services.DMP,
    person_type=PersonTypes.UR,
    firm=Firms.YANDEX_1,
    currency=Currencies.RUB,
    nds=NdsNew.DEFAULT,
    contract_type=ContractSubtype.SPENDABLE,
    contract_services=[Services.DMP.id],
    use_create_contract=True,
    # partner_act_data
    page_id=Pages.DMP.id,
    pad_description=Pages.DMP.desc,
    pad_type_id=6,
)

ADFOX_RU_CONTEXT = Context().new(
    # common
    name='ADFOX_RU_CONTEXT',
    service=Services.ADFOX,
    person_type=PersonTypes.UR,
    firm=Firms.YANDEX_1,
    currency=Currencies.RUB,
    paysys=Paysyses.BANK_UR_RUB,
    nds=NdsNew.DEFAULT,
    contract_type=ContractSubtype.GENERAL,
    contract_services=[Services.ADFOX.id],
)

ADFOX_SW_CONTEXT = ADFOX_RU_CONTEXT.new(
    # common
    name='ADFOX_SW_CONTEXT',
    person_type=PersonTypes.SW_YT,
    currency=Currencies.EUR,
    firm=Firms.EUROPE_AG_7,
    paysys=Paysyses.BANK_SW_UR_EUR,
    special_contract_params={'commission': 22},
    contract_services=[Services.ADFOX.id, Services.SHOP.id],
    use_create_contract=True,
)

AVIA_RU_CONTEXT = Context().new(
    # common
    name='AVIA_RU_CONTEXT',
    service=Services.KUPIBILET,
    person_type=PersonTypes.UR,
    firm=Firms.YANDEX_1,
    currency=Currencies.RUB,
    paysys=Paysyses.BANK_UR_RUB,
    nds=NdsNew.DEFAULT,
    contract_type=ContractSubtype.GENERAL,
    invoice_type=InvoiceType.PERSONAL_ACCOUNT,
    special_contract_params={'personal_account': 1},
    contract_services=[Services.KUPIBILET.id],
)

AVIA_SW_CONTEXT = AVIA_RU_CONTEXT.new(
    # common
    name='AVIA_SW_CONTEXT',
    person_type=PersonTypes.SW_UR,
    firm=Firms.SERVICES_AG_16,
    currency=Currencies.EUR,
    nds=NdsNew.SAG_RESIDENT,
    paysys=Paysyses.BANK_SW_UR_EUR_SAG,
    use_create_contract=True,
    special_contract_params={'personal_account': 1, 'commission': 22},
)

AVIA_RU_PH_CONTEXT = AVIA_RU_CONTEXT.new(
    name='AVIA_RU_PH_CONTEXT',
    person_type=PersonTypes.PH,
    paysys=Paysyses.BANK_PH_RUB
)

AVIA_RU_YT_CONTEXT = AVIA_RU_CONTEXT.new(
    name='AVIA_RU_YT_CONTEXT',
    person_type=PersonTypes.YT,
    paysys=Paysyses.BANK_YT_RUB
)

AVIA_SW_YT_CONTEXT = AVIA_SW_CONTEXT.new(
    name='AVIA_SW_YT_CONTEXT',
    person_type=PersonTypes.SW_YT,
    nds=NdsNew.NOT_RESIDENT,
    paysys=Paysyses.BANK_SW_YT_EUR_SAG,
)

DSP_RU_CONTEXT = Context().new(
    # common
    name='DSP_RU_CONTEXT',
    service=Services.DSP,
    person_type=PersonTypes.UR,
    firm=Firms.YANDEX_1,
    currency=Currencies.RUB,
    paysys=Paysyses.BANK_UR_RUB,
    nds=NdsNew.DEFAULT,
    contract_type=ContractSubtype.GENERAL,
    use_create_contract=True,
    contract_services=[Services.DSP.id],
)

DSP_SW_CONTEXT = DSP_RU_CONTEXT.new(
    # common
    name='DSP_SW_CONTEXT',
    person_type=PersonTypes.SW_YT,
    firm=Firms.EUROPE_AG_7,
    currency=Currencies.CHF,
    paysys=Paysyses.BANK_SW_YT_CHF,
    nds=NdsNew.NOT_RESIDENT,
    special_contract_params={'commission': 22, 'personal_account': 1},
)

DSP_US_UR_CONTEXT = DSP_RU_CONTEXT.new(
    name='DSP_US_UR_CONTEXT',
    person_type=PersonTypes.USU,
    firm=Firms.YANDEX_INC_4,
    currency=Currencies.USD,
    paysys=Paysyses.BANK_US_UR_USD,
    nds=NdsNew.NOT_RESIDENT,
    special_contract_params={'commission': 18, 'personal_account': 1},
)

DSP_US_PH_CONTEXT = DSP_US_UR_CONTEXT.new(
    name='DSP_US_PH_CONTEXT',
    person_type=PersonTypes.USP,
    paysys=Paysyses.BANK_US_PH_USD,
)

MESSENGER_CONTEXT = Context().new(
    # common
    name='MESSENGER_CONTEXT',
    service=Services.MESSENGER,
    person_type=PersonTypes.UR,
    firm=Firms.YANDEX_1,
    currency=Currencies.RUB,
    paysys=Paysyses.BANK_UR_RUB,
    nds=NdsNew.DEFAULT,
    contract_type=ContractSubtype.GENERAL,
    invoice_type=InvoiceType.PERSONAL_ACCOUNT,
    special_contract_params={'personal_account': 1, 'partner_commission_pct2': Decimal('3.3')},
    contract_services=[Services.MESSENGER.id],
    # thirdparty
    payment_currency=Currencies.RUB,
    tpt_payment_type=PaymentType.DIRECT_CARD,
    tpt_paysys_type_cc=PaysysType.MONEY,
    min_commission=Decimal('0.01'),
)

DISK_CONTEXT = Context().new(
    # common
    name='DISK_CONTEXT',
    service=Services.DISK,
    person_type=PersonTypes.PH,
    firm=Firms.YANDEX_1,
    currency=Currencies.RUB,
    paysys=Paysyses.BANK_PH_RUB,
    nds=NdsNew.DEFAULT,
    contract_type=ContractSubtype.GENERAL,
    invoice_type=InvoiceType.PERSONAL_ACCOUNT,
    special_contract_params={'personal_account': 1},
    contract_services=[Services.DISK.id],
    # thirdparty
    payment_currency=Currencies.RUB,
    tpt_payment_type=PaymentType.CARD,
    tpt_paysys_type_cc=PaysysType.MONEY,
    # contracts
    contracts=[
        {
            'commission': ContractType.NOT_AGENCY,
            'currency': Currencies.RUB,
            'nds': NdsNew.YANDEX_RESIDENT,
            'nds_flag': 1,
            'product': Products.DISK_RUB_WITH_NDS,
            'paysys': Paysyses.BANK_PH_RUB
        },
        # {
        #     'commission': ContractType.NOT_AGENCY,
        #     'currency': Currencies.USD,
        #     'nds': NdsNew.ZERO,
        #     'nds_flag': 1,
        #     'product': Products.DISK_USD_WO_NDS,
        #     'paysys': Paysyses.BANK_US_PH_USD
        # }
    ]
)

DISK_PLUS_CONTEXT = Context().new(
    # common
    name='DISK_PLUS_CONTEXT',
    service=Services.DISK_PLUS,
    person_type=PersonTypes.PH,
    firm=Firms.MEDIASERVICES_121,
    currency=Currencies.RUB,
    paysys=Paysyses.BANK_PH_RUB_MEDIASERVICES,
    nds=NdsNew.DEFAULT,
    contract_type=ContractSubtype.GENERAL,
    invoice_type=InvoiceType.PERSONAL_ACCOUNT,
    special_contract_params={'personal_account': 1},
    contract_services=[Services.DISK_PLUS_NEW.id],
    # thirdparty
    payment_currency=Currencies.RUB,
    tpt_payment_type=PaymentType.CARD,
    tpt_paysys_type_cc=PaysysType.MONEY,
)

DISK_PLUS_NEW_CONTEXT = DISK_PLUS_CONTEXT.new(
    # common
    name='DISK_PLUS_NEW_CONTEXT',
    service=Services.DISK_PLUS_NEW,
)


MUSIC_CONTEXT = Context().new(
    # common
    name='MUSIC_CONTEXT',
    service=Services.MUSIC,
    person_type=PersonTypes.PH,
    firm=Firms.YANDEX_1,
    currency=Currencies.RUB,
    paysys=Paysyses.BANK_PH_RUB,
    nds=NdsNew.DEFAULT,
    contract_type=ContractSubtype.GENERAL,
    invoice_type=InvoiceType.PERSONAL_ACCOUNT,
    special_contract_params={'personal_account': 1},
    contract_services=[Services.MUSIC.id],
    partner_integration={'scheme': SCHEMES.music.rub},
    # thirdparty
    payment_currency=Currencies.RUB,
    tpt_payment_type=PaymentType.CARD,
    tpt_paysys_type_cc=PaysysType.MONEY,
    # contracts
    contracts=[
        {
            'commission': ContractType.NOT_AGENCY,
            'currency': Currencies.RUB,
            'nds': NdsNew.DEFAULT,
            'nds_flag': 1,
            'product': Products.MUSIC,
            'paysys': Paysyses.BANK_PH_RUB
        }
    ]
)

MUSIC_MEDIASERVICE_CONTEXT = MUSIC_CONTEXT.new(
    name='MUSIC_MEDIASERVICE_CONTEXT',
    service=Services.MUSIC_MEDIASERVICE,
    firm=Firms.MEDIASERVICES_121,
    paysys=Paysyses.BANK_PH_RUB_MEDIASERVICES,
    contract_services=[Services.MUSIC_MEDIASERVICE.id],
    partner_integration=None,
    contracts=[
        {
            'commission': ContractType.NOT_AGENCY,
            'currency': Currencies.RUB,
            'nds': NdsNew.DEFAULT,
            'nds_flag': 1,
            'product': Products.MUSIC_MEDIASERVICE,
            'paysys': Paysyses.BANK_PH_RUB_MEDIASERVICES
        }
    ]
)

MUSIC_TARIFFICATOR_CONTEXT = MUSIC_CONTEXT.new(
    name='MUSIC_TARIFFICATOR_CONTEXT',
    service=Services.MUSIC_TARIFFICATOR,
    contract_services=[Services.MUSIC_TARIFFICATOR.id],
    service_fee_product_map={None: Products.MUSIC, 1: Products.MUSIC_TARIFFICATOR_DISK},
    partner_integration=None,
    contracts=[
        {
            'commission': ContractType.NOT_AGENCY,
            'currency': Currencies.RUB,
            'nds': NdsNew.DEFAULT,
            'nds_flag': 1,
            'product': Products.MUSIC_TARIFFICATOR,
            'paysys': Paysyses.BANK_PH_RUB
        }
    ]
)

MUSIC_MEDIASERVICE_TARIFFICATOR_CONTEXT = MUSIC_MEDIASERVICE_CONTEXT.new(
    name='MUSIC_MEDIASERVICE_TARIFFICATOR_CONTEXT',
    service=Services.MUSIC_MEDIASERVICE_TARIFFICATOR,
    contract_services=[Services.MUSIC_MEDIASERVICE_TARIFFICATOR.id],
    contracts=[
        {
            'commission': ContractType.NOT_AGENCY,
            'currency': Currencies.RUB,
            'nds': NdsNew.DEFAULT,
            'nds_flag': 1,
            'product': Products.MUSIC_MEDIASERVICE_TARIFFICATOR,
            'paysys': Paysyses.BANK_PH_RUB_MEDIASERVICES
        }
    ]
)

KINOPOISK_PLUS_CONTEXT = Context().new(
    # common
    name='KINOPOISK_PLUS_CONTEXT',
    service=Services.KINOPOISK_PLUS,
    person_type=PersonTypes.PH,
    firm=Firms.KINOPOISK_9,
    currency=Currencies.RUB,
    paysys=Paysyses.BANK_PH_RUB_KINOPOISK,
    nds=NdsNew.DEFAULT,
    contract_type=ContractSubtype.GENERAL,
    invoice_type=InvoiceType.PERSONAL_ACCOUNT,
    special_contract_params={'personal_account': 1},
    contract_services=[Services.KINOPOISK_PLUS.id],
    # thirdparty
    payment_currency=Currencies.RUB,
    tpt_payment_type=PaymentType.CARD,
    tpt_paysys_type_cc=PaysysType.MONEY,
    # contracts
    contracts=[
        {
            'commission': ContractType.NOT_AGENCY,
            'currency': Currencies.RUB,
            'nds': NdsNew.DEFAULT,
            'nds_flag': 1,
            'product': Products.KINOPOISK_WITH_NDS,
            'paysys': Paysyses.BANK_PH_RUB_KINOPOISK
        },
        {
            'commission': ContractType.LICENSE,
            'currency': Currencies.RUB,
            'nds': NdsNew.NOT_RESIDENT,
            'nds_flag': 0,
            'product': Products.KINOPOISK_WO_NDS,
            'paysys': Paysyses.BANK_PH_RUB_WO_NDS_KINOPOISK
        }
    ]
)

KINOPOISK_AMEDIATEKA_CONTEXT = Context().new(
    # common
    name='KINOPOISK_AMEDIATEKA_CONTEXT',
    service=Services.KINOPOISK_AMEDIATEKA,
    person_type=PersonTypes.PH,
    firm=Firms.KINOPOISK_9,
    currency=Currencies.RUB,
    paysys=Paysyses.BANK_PH_RUB_KINOPOISK,
    nds=NdsNew.DEFAULT,
    contract_type=ContractSubtype.GENERAL,
    invoice_type=InvoiceType.PERSONAL_ACCOUNT,
    special_contract_params={'personal_account': 1},
    contract_services=[Services.KINOPOISK_AMEDIATEKA.id],
    # thirdparty
    payment_currency=Currencies.RUB,
    tpt_payment_type=PaymentType.CARD,
    tpt_paysys_type_cc=PaysysType.MONEY,
)

KINOPOISK_AMEDIATEKA_TARIFFICATOR_CONTEXT = KINOPOISK_AMEDIATEKA_CONTEXT.new(
    name='KINOPOISK_AMEDIATEKA_TARIFFICATOR_CONTEXT',
    service=Services.KINOPOISK_AMEDIATEKA_TARIFFICATOR,
    contract_services=[Services.KINOPOISK_AMEDIATEKA_TARIFFICATOR.id],
)

REALTY_CONTEXT = Context().new(
    # common
    name='REALTY_CONTEXT',
    service=Services.REALTYPAY,
    person_type=PersonTypes.UR,
    firm=Firms.VERTICAL_12,
    currency=Currencies.RUB,
    paysys=Paysyses.BANK_UR_RUB_VERTICAL,
    nds=NdsNew.DEFAULT,
    contract_type=ContractSubtype.GENERAL,
    invoice_type=InvoiceType.PERSONAL_ACCOUNT,
    special_contract_params={'personal_account': 1},
    contract_services=[Services.REALTYPAY.id],
    # thirdparty
    payment_currency=Currencies.RUB,
    tpt_payment_type=PaymentType.CARD,
    tpt_paysys_type_cc=PaysysType.MONEY,
)

STATION_PAYMENTS_CONTEXT = Context().new(
    # common
    name='STATION_PAYMENTS_CONTEXT',
    service=Services.QUASAR,
    person_type=PersonTypes.UR,
    paysys=Paysyses.BANK_UR_RUB,
    contract_type=ContractSubtype.GENERAL,
    firm=Firms.YANDEX_1,
    currency=Currencies.RUB,
    nds=NdsNew.DEFAULT,
    invoice_type=InvoiceType.PERSONAL_ACCOUNT,
    special_contract_params={'personal_account': 1, 'partner_commission_pct2': Decimal('3.3'),
                             'commissions': json.dumps({u'Комиссия 1': 100, u'Комиссия 2': 300})},
    contract_services=[Services.QUASAR.id, Services.QUASAR_SRV.id],
    # thirdparty
    tpt_payment_type=PaymentType.CARD,
    tpt_paysys_type_cc=PaysysType.MONEY,
    payment_currency=Currencies.RUB,
)

STATION_SERVICES_CONTEXT = STATION_PAYMENTS_CONTEXT.new(
    # common
    name='STATION_SERVICES_CONTEXT',
    service=Services.QUASAR_SRV,
)

TRAVEL_EXPEDIA_CONTEXT = Context().new(
    # common
    name='TRAVEL_EXPEDIA_CONTEXT',
    service=Services.TRAVEL,
    person_type=PersonTypes.YT,
    firm=Firms.YANDEX_1,
    currency=Currencies.USD,
    contract_type=ContractSubtype.GENERAL,
    special_contract_params={'personal_account': 1, 'partner_credit': 1},
    contract_services=[Services.TRAVEL.id],
    nds=NdsNew.NOT_RESIDENT,
    invoice_type=InvoiceType.PERSONAL_ACCOUNT,
    paysys=Paysyses.BANK_YT_USD,
    # thirdparty
    payment_currency=Currencies.USD,
    tpt_payment_type=PaymentType.REWARD,
    tpt_paysys_type_cc=PaysysType.MONEY,
)

TRAVEL_CONTEXT_RUB = TRAVEL_EXPEDIA_CONTEXT.new(
    # common
    name='TRAVEL_CONTEXT_RUB',
    person_type=PersonTypes.UR,
    currency=Currencies.RUB,
    paysys=Paysyses.BANK_UR_RUB,
    nds=NdsNew.DEFAULT,
    # thirdparty
    payment_currency=Currencies.RUB
)

UFS_RU_CONTEXT = Context().new(
    # common
    name='UFS_RU_CONTEXT',
    service=Services.UFS,
    person_type=PersonTypes.UR,
    firm=Firms.YANDEX_1,
    currency=Currencies.RUB,
    paysys=Paysyses.BANK_UR_RUB,
    nds=NdsNew.DEFAULT,
    contract_type=ContractSubtype.GENERAL,
    invoice_type=InvoiceType.PERSONAL_ACCOUNT,
    use_create_contract=True,
    special_contract_params={'personal_account': 1, 'partner_commission_pct2': Decimal('3.3'),
                             'partner_commission_sum': Decimal('12.3'), 'partner_commission_sum2': Decimal('4.8')},
    # thirdparty
    payment_currency=Currencies.RUB,
    tpt_payment_type=PaymentType.CARD,
    tpt_paysys_type_cc=PaysysType.MONEY,
)

TRAVEL_ELECTRIC_TRAINS_RU_CONTEXT = Context().new(
    # common
    name='TRAVEL_ELECTRIC_TRAINS_RU_CONTEXT',
    service=Services.TRAVEL_ELECTRIC_TRAIN_CPPK,
    person_type=PersonTypes.UR,
    firm=Firms.YANDEX_1,
    currency=Currencies.RUB,
    paysys=Paysyses.BANK_UR_RUB,
    nds=NdsNew.DEFAULT,
    contract_type=ContractSubtype.GENERAL,
    invoice_type=InvoiceType.PERSONAL_ACCOUNT,
    use_create_contract=True,
    special_contract_params={'personal_account': 1},
    # thirdparty
    payment_currency=Currencies.RUB,
    tpt_payment_type=PaymentType.CARD,
    tpt_paysys_type_cc=PaysysType.MONEY,
)

AEROEXPRESS_CONTEXT = Context().new(
    # common
    name='AEROEXPRESS_CONTEXT',
    service=Services.AEROEXPRESS,
    person_type=PersonTypes.UR,
    firm=Firms.YANDEX_1,
    currency=Currencies.RUB,
    paysys=Paysyses.BANK_UR_RUB,
    nds=NdsNew.DEFAULT,
    contract_type=ContractSubtype.GENERAL,
    invoice_type=InvoiceType.PERSONAL_ACCOUNT,
    special_contract_params={'personal_account': 1, 'partner_commission_sum': Decimal('34.4')},
    # thirdparty
    payment_currency=Currencies.RUB,
    tpt_payment_type=PaymentType.CARD,
    tpt_paysys_type_cc=PaysysType.MONEY,
)

TOLOKA_CONTEXT = Context().new(
    # common
    name='TOLOKA_CONTEXT',
    service=Services.TOLOKA,
    person_type=PersonTypes.PH,
    firm=Firms.JAMS_120,
    currency=Currencies.RUB,
    nds=NdsNew.DEFAULT,
    contract_type=ContractSubtype.SPENDABLE,
    # thirdparty
    payment_currency=Currencies.RUB,
    tpt_payment_type=PaymentType.BALALAYKA_PAYMENT,
    tpt_paysys_type_cc=PaysysType.MONEY,
)

DOSTAVKA_CONTEXT = Context().new(
    # common
    name='DOSTAVKA_CONTEXT',
    service=Services.DOSTAVKA,
    person_type=PersonTypes.UR,
    firm=Firms.MARKET_111,
    currency=Currencies.RUB,
    paysys=Paysyses.BANK_UR_RUB_MARKET,
    nds=NdsNew.DEFAULT,
    contract_type=ContractSubtype.GENERAL,
    special_contract_params={'minimal_payment_commission': Decimal('1.2')},
    contract_services=[Services.DOSTAVKA.id, Services.DOSTAVKA_101.id],
    # thirdparty
    payment_currency=Currencies.RUB,
    tpt_payment_type=PaymentType.CASH,
    tpt_paysys_type_cc=PaysysType.DELIVERY,
)

APIKEYS_CONTEXT = Context().new(
    # common

    name='APIKEYS_CONTEXT',
    service=Services.APIKEYS,
    person_type=PersonTypes.UR,
    firm=Firms.YANDEX_1,
    currency=Currencies.RUB,
    nds=NdsNew.DEFAULT,
    contract_type=ContractSubtype.GENERAL,
    special_contract_params={'personal_account': 1},
    use_create_contract=True,
    # additional
    paysys=Paysyses.BANK_UR_RUB,
    invoice_type=InvoiceType.PERSONAL_ACCOUNT
)

APIKEYSAGENTS_CONTEXT = Context().new(
    # common

    name='APIKEYSAGENTS_CONTEXT',
    service=Services.APIKEYSAGENTS,
    person_type=PersonTypes.UR,
    firm=Firms.YANDEX_1,
    currency=Currencies.RUB,
    nds=NdsNew.DEFAULT,
    contract_type=ContractSubtype.GENERAL,
    special_contract_params={'personal_account': 1},
    use_create_contract=True,
    # additional
    paysys=Paysyses.BANK_UR_RUB,
    invoice_type=InvoiceType.PERSONAL_ACCOUNT
)


REFUELLER_CONTEXT = Context().new(
    # common
    name='REFUELLER_CONTEXT',
    service=Services.REFUELLER_PENALTY,
    person_type=PersonTypes.UR,
    firm=Firms.DRIVE_30,
    currency=Currencies.RUB,
    payment_currency=Currencies.RUB,
    nds=NdsNew.ZERO,
    invoice_type=InvoiceType.PERSONAL_ACCOUNT,
    contract_type=ContractSubtype.GENERAL,
    paysys=Paysyses.BANK_UR_WO_NDS_REFUELLER,
    special_contract_params={'personal_account': 1},
    tpt_payment_type=PaymentType.CORRECTION_COMMISSION,
    tpt_paysys_type_cc=PaysysType.EXTRA_PROFIT,
)


REFUELLER_SPENDABLE_CONTEXT = REFUELLER_CONTEXT.new(
    # common
    name='REFUELLER_SPENDABLE_CONTEXT',
    service=Services.REFUELLER_SPENDABLE,
    nds=NdsNew.ZERO,
    contract_type=ContractSubtype.SPENDABLE,

    tpt_payment_type=PaymentType.DRIVE_FUELLER,
    tpt_paysys_type_cc=PaysysType.DRIVE,
    page_id=Pages.REFUELLER.id,
    pad_description=Pages.REFUELLER.desc,
    pad_type_id=6,
)

FOOD_COURIER_CONTEXT = Context().new(
    name='FOOD_COURIER_CONTEXT',
    service=Services.FOOD_COURIER,
    person_type=PersonTypes.UR,
    firm=Firms.FOOD_32,
    currency=Currencies.RUB,
    payment_currency=Currencies.RUB,
    nds=NdsNew.YANDEX_RESIDENT,
    invoice_type=InvoiceType.PERSONAL_ACCOUNT,
    contract_type=ContractSubtype.GENERAL,
    paysys=Paysyses.BANK_UR_FOOD_RUB,
    min_commission=Decimal('0.01'),
    tpt_payment_type=PaymentType.CARD,
    tpt_paysys_type_cc=PaysysType.PAYTURE,
    special_contract_params={'personal_account': 1,
                             'partner_commission_pct2': Decimal('4.5'),
                             'country': Regions.RU.id
                             },
    migration_alias='food_couriers_payments',
)

FOOD_COURIER_SPENDABLE_CONTEXT = FOOD_COURIER_CONTEXT.new(
    name='FOOD_COURIER_SPENDABLE_CONTEXT',
    service=Services.FOOD_COURIER_SUBSIDY,
    contract_type=ContractSubtype.SPENDABLE,
    nds=NdsNew.ZERO,
    tpt_payment_type=PaymentType.SUBSIDY,
    tpt_paysys_type_cc=PaymentType.SUBSIDY,  # для субсидий в paysys_type_cc подставляем payment_type
    special_contract_params={
        'country': Regions.RU.id
    },
    page_id=Pages.FOOD_SUBSIDY.id,
    pad_description=Pages.FOOD_SUBSIDY.desc,
    pad_type_id=6,
)

FOOD_RESTAURANT_CONTEXT = FOOD_COURIER_CONTEXT.new(
    name='FOOD_RESTAURANT_CONTEXT',
    service=Services.FOOD_PAYMENTS,
    contract_services=[Services.FOOD_PAYMENTS.id, Services.FOOD_SERVICES.id],
    commission_service=Services.FOOD_SERVICES,
    payment_service=Services.FOOD_PAYMENTS,
    special_contract_params={'personal_account': 1,
                             'partner_commission_pct2': Decimal('0'),
                             'netting': 1,
                             'netting_pct': 100,
                             'country': Regions.RU.id
                             },
    migration_alias='food_payments',
)

FOOD_RESTAURANT_CONTEXT_WITH_MERCURY = FOOD_RESTAURANT_CONTEXT.new(
    name='FOOD_RESTAURANT_CONTEXT_WITH_MERCURY',
    contract_services=[Services.FOOD_PAYMENTS.id, Services.FOOD_SERVICES.id,
                       Services.FOOD_MERCURY_PAYMENTS.id, Services.FOOD_MERCURY_SERVICES.id],
)

FOOD_MERCURY_CONTEXT = FOOD_COURIER_CONTEXT.new(
    name='FOOD_MERCURY_CONTEXT',
    service=Services.FOOD_MERCURY_PAYMENTS,
    contract_services=[Services.FOOD_MERCURY_PAYMENTS.id, Services.FOOD_MERCURY_SERVICES.id],
    commission_service=Services.FOOD_MERCURY_SERVICES,
    payment_service=Services.FOOD_MERCURY_PAYMENTS,
    special_contract_params={'personal_account': 1,
                             'partner_commission_pct2': Decimal('0'),
                             'netting': 1,
                             'netting_pct': 100,
                             'country': Regions.RU.id},
    migration_alias='food_mercury_payments',
)

FOOD_FULL_MERCURY_CONTEXT = FOOD_MERCURY_CONTEXT.new(
    name='FOOD_FULL_MERCURY_CONTEXT',
    contract_services=[Services.FOOD_PAYMENTS.id, Services.FOOD_SERVICES.id,
                       Services.FOOD_MERCURY_PAYMENTS.id, Services.FOOD_MERCURY_SERVICES.id],
    migration_alias='food_mercury_payments',
)

FOOD_COURIER_KZ_CONTEXT = Context().new(
    name='FOOD_COURIER_KZ_CONTEXT',
    service=Services.FOOD_COURIER,
    person_type=PersonTypes.KZU,
    firm=Firms.TAXI_KAZ_24,
    currency=Currencies.KZT,
    payment_currency=Currencies.KZT,
    nds=NdsNew.KAZAKHSTAN,
    invoice_type=InvoiceType.PERSONAL_ACCOUNT,
    contract_type=ContractSubtype.GENERAL,
    paysys=Paysyses.BANK_KZ_UR_WO_NDS,
    min_commission=Decimal('15.0'),
    tpt_payment_type=PaymentType.CARD,
    tpt_paysys_type_cc=PaysysType.PAYTURE,
    special_contract_params={'personal_account': 1,
                             'partner_commission_pct2': Decimal('4.5'),
                             'country': Regions.KZ.id},
    migration_alias='food_couriers_payments',
)

FOOD_PICKER_CONTEXT = Context().new(
    name='FOOD_PICKER_CONTEXT',
    service=Services.FOOD_PICKER,
    person_type=PersonTypes.UR,
    firm=Firms.FOOD_32,
    currency=Currencies.RUB,
    payment_currency=Currencies.RUB,
    nds=NdsNew.YANDEX_RESIDENT,
    invoice_type=InvoiceType.PERSONAL_ACCOUNT,
    contract_type=ContractSubtype.GENERAL,
    paysys=Paysyses.BANK_UR_FOOD_RUB,
    min_commission=Decimal('0.01'),
    tpt_payment_type=PaymentType.CARD,
    tpt_paysys_type_cc=PaysysType.PAYTURE,
    special_contract_params={'personal_account': 1,
                             'partner_commission_pct2': Decimal('4.5'),
                             'country': Regions.RU.id
                             },
    migration_alias='food_pickers_payments',

)

FOOD_PICKER_BUILD_ORDER_CONTEXT = Context().new(
    name='FOOD_PICKER_BUILD_ORDER_CONTEXT',
    service=Services.FOOD_PICKER_BUILD_ORDER,
    person_type=PersonTypes.UR,
    firm=Firms.FOOD_32,
    currency=Currencies.RUB,
    payment_currency=Currencies.RUB,
    nds=NdsNew.YANDEX_RESIDENT,
    invoice_type=InvoiceType.PERSONAL_ACCOUNT,
    contract_type=ContractSubtype.GENERAL,
    paysys=Paysyses.BANK_UR_FOOD_RUB,
    min_commission=Decimal('0.01'),
    tpt_payment_type=PaymentType.CARD,
    tpt_paysys_type_cc=PaysysType.PAYTURE,
    special_contract_params={'personal_account': 1,
                             'partner_commission_pct2': Decimal('4.5'),
                             'country': Regions.RU.id
                             },
)

REST_SITES_CONTEXT = FOOD_COURIER_CONTEXT.new(
    name='REST_SITES_CONTEXT',
    service=Services.REST_SITES_PAYMENTS,
    contract_services=[Services.REST_SITES_PAYMENTS.id, Services.REST_SITES_SERVICES.id],
    commission_service=Services.REST_SITES_SERVICES,
    payment_service=Services.REST_SITES_PAYMENTS,
    special_contract_params={'personal_account': 1,
                             'partner_commission_pct2': Decimal('0'),
                             'netting': 1,
                             'netting_pct': 100,
                             'country': Regions.RU.id
                             },
)


SUPERCHECK_CONTEXT = Context().new(
    name='SUPERCHECK_CONTEXT',
    service=Services.SUPERCHECK,
    person_type=PersonTypes.UR,
    firm=Firms.MARKET_111,
    currency=Currencies.RUB,
    payment_currency=Currencies.RUB,
    nds=NdsNew.DEFAULT,
    invoice_type=InvoiceType.PERSONAL_ACCOUNT,
    contract_type=ContractSubtype.GENERAL,
    paysys=Paysyses.BANK_UR_RUB_MARKET,
    tpt_payment_type=PaymentType.CARD,
    tpt_paysys_type_cc=PaysysType.MONEY,
    min_commission=Decimal('0'),
    special_contract_params={'personal_account': 1},
    partner_integration={
        'scheme': SCHEMES.supercheck.default_conf
    },
)

MEDIASERVICES_SHOP_CONTEXT = Context().new(
    name='MEDIASERVICES_SHOP_CONTEXT',
    service=Services.SHOP,
    person_type=PersonTypes.UR,
    firm=Firms.MEDIASERVICES_121,
    paysys=Paysyses.BANK_UR_RUB_MEDIASERVICES,
    currency=Currencies.RUB,
    nds=NdsNew.DEFAULT,
    contract_type=ContractSubtype.GENERAL,
    invoice_type=InvoiceType.PERSONAL_ACCOUNT,
    special_contract_params={'personal_account': 1}
)

BUG_BOUNTY_CONTEXT = Context().new(
    name='BUG_BOUNTY_CONTEXT',
    service=Services.BUG_BOUNTY,
    contract_services=[Services.BUG_BOUNTY.id],
    firm=Firms.YANDEX_1,
    currency=Currencies.RUB,
    nds=NdsNew.ZERO,
    contract_type=ContractSubtype.SPENDABLE,
    page_id=Pages.BUG_BOUNTY.id,
    pad_description=Pages.BUG_BOUNTY.desc,
    pad_type_id=6,
    source_id=202,
)

FOOD_COURIER_SPENDABLE_KZ_CONTEXT = FOOD_COURIER_KZ_CONTEXT.new(
    name='FOOD_COURIER_SPENDABLE_KZ_CONTEXT',
    service=Services.FOOD_COURIER_SUBSIDY,
    contract_type=ContractSubtype.SPENDABLE,
    nds=NdsNew.TAXI_KAZAKHSTAN,
    # nds=NdsNew.KAZAKHSTAN,
    tpt_payment_type=PaymentType.SUBSIDY,
    tpt_paysys_type_cc=PaymentType.SUBSIDY,  # для субсидий в paysys_type_cc подставляем payment_type
    special_contract_params={'country': Regions.KZ.id},
    page_id=Pages.FOOD_SUBSIDY.id,
    pad_description=Pages.FOOD_SUBSIDY.desc,
    pad_type_id=6,
)

FOOD_RESTAURANT_KZ_CONTEXT = FOOD_COURIER_KZ_CONTEXT.new(
    name='FOOD_RESTAURANT_KZ_CONTEXT',
    service=Services.FOOD_PAYMENTS,
    contract_services=[Services.FOOD_PAYMENTS.id, Services.FOOD_SERVICES.id],
    commission_service=Services.FOOD_SERVICES,
    payment_service=Services.FOOD_PAYMENTS,
    special_contract_params={'personal_account': 1,
                             'partner_commission_pct2': Decimal('0'),
                             'netting': 1,
                             'netting_pct': 100,
                             'country': Regions.KZ.id},
    migration_alias='food_payments',
)

FOOD_COURIER_BY_TEMPLATE_CONTEXT = Context().new(
    name='FOOD_COURIER_BY_CONTEXT',
    service=Services.FOOD_COURIER,
    person_type=PersonTypes.EU_YT,
    firm=Firms.UBER_115,
    currency=Currencies.BYN,
    payment_currency=Currencies.BYN,
    nds=NdsNew.NOT_RESIDENT,
    invoice_type=InvoiceType.PERSONAL_ACCOUNT,
    contract_type=ContractSubtype.GENERAL,
    paysys=Paysyses.BANK_UR_UBER_BYN,  # TODO: check paysys for BY!!!
    min_commission=Decimal('0.01'),
    tpt_payment_type=PaymentType.CARD,
    tpt_paysys_type_cc=PaysysType.PAYTURE,
    expect_reference_currency=1,
    special_contract_params={'personal_account': 1,
                             'partner_commission_pct2': Decimal('4.5'),
                             'country': Regions.BY.id},
)

FOOD_COURIER_BY_CONTEXT = FOOD_COURIER_BY_TEMPLATE_CONTEXT.new(
    migration_alias='food_couriers_payments',
)


FOOD_COURIER_BY_TAXI_BV_CONTEXT = FOOD_COURIER_BY_CONTEXT.new(
    name='FOOD_COURIER_BY_TAXI_BV_CONTEXT',
    firm=Firms.TAXI_BV_22,
    paysys=Paysyses.BANK_UR_BYN_TAXI_BV,
)


FOOD_COURIER_BY_FOODTECH_DELIVERY_BV_CONTEXT = FOOD_COURIER_BY_CONTEXT.new(
    name='FOOD_COURIER_BY_FOODTECH_DELIVERY_BV_CONTEXT',
    firm=Firms.FOODTECH_DELIVERY_BV,
    paysys=Paysyses.BANK_EU_YT_FOODTECH_DELIVERY_BYN,
)


FOOD_COURIER_SPENDABLE_BY_CONTEXT = FOOD_COURIER_BY_TEMPLATE_CONTEXT.new(
    name='FOOD_COURIER_SPENDABLE_BY_CONTEXT',
    service=Services.FOOD_COURIER_SUBSIDY,
    contract_type=ContractSubtype.SPENDABLE,
    nds=NdsNew.ZERO,
    # nds=NdsNew.KAZAKHSTAN,
    tpt_payment_type=PaymentType.SUBSIDY,
    tpt_paysys_type_cc=PaymentType.SUBSIDY,  # для субсидий в paysys_type_cc подставляем payment_type
    special_contract_params={'country': Regions.BY.id},
    page_id=Pages.FOOD_SUBSIDY.id,
    pad_description=Pages.FOOD_SUBSIDY.desc,
    pad_type_id=6,
)

FOOD_RESTAURANT_BY_CONTEXT = FOOD_COURIER_BY_TEMPLATE_CONTEXT.new(
    name='FOOD_RESTAURANT_BY_CONTEXT',
    service=Services.FOOD_PAYMENTS,
    contract_services=[Services.FOOD_PAYMENTS.id, Services.FOOD_SERVICES.id],
    commission_service=Services.FOOD_SERVICES,
    payment_service=Services.FOOD_PAYMENTS,
    special_contract_params={'personal_account': 1,
                             'partner_commission_pct2': Decimal('0'),
                             'netting': 1,
                             'netting_pct': 100,
                             'country': Regions.BY.id},
    migration_alias='food_payments',
)

FOOD_RESTAURANT_BY_TAXI_BV_CONTEXT = FOOD_RESTAURANT_BY_CONTEXT.new(
    name='FOOD_RESTAURANT_BY_TAXI_BV_CONTEXT',
    firm=Firms.TAXI_BV_22,
    paysys=Paysyses.BANK_UR_BYN_TAXI_BV,
)

FOOD_RESTAURANT_BY_FOODTECH_DELIVERY_BV_CONTEXT = FOOD_RESTAURANT_BY_CONTEXT.new(
    name='FOOD_RESTAURANT_BY_FOODTECH_DELIVERY_BV_CONTEXT',
    firm=Firms.FOODTECH_DELIVERY_BV,
    paysys=Paysyses.BANK_EU_YT_FOODTECH_DELIVERY_BYN,
    migration_alias='food_payments',
)


LAVKA_COURIER_SPENDABLE_CONTEXT = FOOD_COURIER_SPENDABLE_CONTEXT.new(
    name='LAVKA_COURIER_SPENDABLE_CONTEXT',
    service=Services.LAVKA_COURIER_SUBSIDY,
    tpt_paysys_type_cc=PaysysType.YAEDA,
    tpt_payment_type=PaymentType.GROCERY_COURIER_DELIVERY,
    page_id=Pages.LAVKA_SUBSIDY.id,
    pad_description=Pages.LAVKA_SUBSIDY.desc,
)

LAVKA_COURIER_SPENDABLE_ISR_CONTEXT = LAVKA_COURIER_SPENDABLE_CONTEXT.new(
    name='LAVKA_COURIER_SPENDABLE_ISR_CONTEXT',
    person_type=PersonTypes.IL_UR,
    firm=Firms.YANDEX_GO_ISRAEL_35,
    currency=Currencies.ILS,
    payment_currency=Currencies.ILS,
    tpt_paysys_type_cc=PaysysType.YAEDA,
    nds=NdsNew.ISRAEL,
    special_contract_params={
        'country': Regions.ISR.id,
        'israel_tax_pct': Decimal('6.66')
    },
)

LAVKA_COURIER_SPENDABLE_FR_EUR_CONTEXT = LAVKA_COURIER_SPENDABLE_CONTEXT.new(
    name='LAVKA_COURIER_SPENDABLE_FR_EUR_CONTEXT',
    person_type=PersonTypes.FR_UR,
    firm=Firms.SAS_DELI_1080,
    currency=Currencies.EUR,
    payment_currency=Currencies.EUR,
    tpt_paysys_type_cc=PaysysType.YAEDA,
    nds=NdsNew.FR,
    special_contract_params={
        'country': Regions.FR.id,
    },
)

FOOD_SHOPS_CONTEXT = FOOD_RESTAURANT_CONTEXT.new(
    name='FOOD_SHOPS_CONTEXT',
    service=Services.FOOD_SHOPS_PAYMENTS,
    contract_services=[Services.FOOD_SHOPS_PAYMENTS.id, Services.FOOD_SHOPS_SERVICES.id],
    commission_service=Services.FOOD_SHOPS_SERVICES,
    payment_service=Services.FOOD_SHOPS_PAYMENTS,
    migration_alias='food_shops_srv',
)

LAVKA_FOOD_SHOPS_CONTEXT = FOOD_SHOPS_CONTEXT.new(
    name='LAVKA_FOOD_SHOPS_CONTEXT',
    paysys=Paysyses.BANK_UR_RUB_LAVKA
)


LAVKA_COURIER_SPENDABLE_KZ_CONTEXT = FOOD_COURIER_SPENDABLE_KZ_CONTEXT.new(
    name='LAVKA_COURIER_SPENDABLE_KZ_CONTEXT',
    service=Services.LAVKA_COURIER_SUBSIDY
)

FOOD_SHOPS_KZ_CONTEXT = FOOD_RESTAURANT_KZ_CONTEXT.new(
    name='FOOD_SHOPS_KZ_CONTEXT',
    service=Services.FOOD_SHOPS_PAYMENTS,
    contract_services=[Services.FOOD_SHOPS_PAYMENTS.id, Services.FOOD_SHOPS_SERVICES.id],
    commission_service=Services.FOOD_SHOPS_SERVICES,
    payment_service=Services.FOOD_SHOPS_PAYMENTS,
)

LAVKA_COURIER_BY_CONTEXT = FOOD_COURIER_BY_TEMPLATE_CONTEXT.new(
    name='LAVKA_COURIER_BY_CONTEXT',
    service=Services.LAVKA_COURIER
)
LAVKA_COURIER_SPENDABLE_BY_CONTEXT = FOOD_COURIER_SPENDABLE_BY_CONTEXT.new(
    name='LAVKA_COURIER_SPENDABLE_BY_CONTEXT',
    service=Services.LAVKA_COURIER_SUBSIDY
)

LAVKA_COURIER_IL_ILS_CONTEXT = Context().new(
    name='LAVKA_COURIER_IL_ILS_CONTEXT',
    service=Services.LAVKA_COURIER,
    person_type=PersonTypes.IL_UR,
    firm=Firms.YANDEX_GO_ISRAEL_35,
    currency=Currencies.ILS,
    paysys=Paysyses.BANK_IL_UR_ILS,
    nds=NdsNew.ISRAEL,
    invoice_type=InvoiceType.PERSONAL_ACCOUNT,
    contract_type=ContractSubtype.GENERAL,
    min_commission=Decimal('0'),
    special_contract_params={'personal_account': 1,
                             'partner_commission_pct2': Decimal('0'),
                             'country': Regions.ISR.id,
                             'israel_tax_pct': Decimal('7.1')},
)

LAVKA_COURIER_FR_EUR_CONTEXT = Context().new(
    name='LAVKA_COURIER_FR_EUR_CONTEXT',
    service=Services.LAVKA_COURIER,
    person_type=PersonTypes.FR_UR,
    firm=Firms.SAS_DELI_1080,
    currency=Currencies.EUR,
    paysys=Paysyses.BANK_FR_UR_EUR_BANK_NDS,
    nds=NdsNew.FR,
    payment_currency=Currencies.EUR,
    invoice_type=InvoiceType.PERSONAL_ACCOUNT,
    contract_type=ContractSubtype.GENERAL,
    min_commission=Decimal('15.0'),
    special_contract_params={'personal_account': 1,
                             'partner_commission_pct2': Decimal('4.5'),
                             'country': Regions.FR.id},
)

LAVKA_COURIER_GB_GBP_CONTEXT = Context().new(
    name='LAVKA_COURIER_GB_GBP_CONTEXT',
    service=Services.LAVKA_COURIER,
    person_type=PersonTypes.GB_UR,
    firm=Firms.DELI_INT_LIM_1083,
    currency=Currencies.GBP,
    paysys=Paysyses.BANK_GB_UR_GBP_BANK_NDS,
    nds=NdsNew.GB,
    payment_currency=Currencies.GBP,
    invoice_type=InvoiceType.PERSONAL_ACCOUNT,
    contract_type=ContractSubtype.GENERAL,
    min_commission=Decimal('15.0'),
    special_contract_params={'personal_account': 1,
                             'partner_commission_pct2': Decimal('4.5'),
                             'country': Regions.GB.id},
)


FOOD_SHOPS_BY_CONTEXT = FOOD_RESTAURANT_BY_CONTEXT.new(
    name='FOOD_SHOPS_BY_CONTEXT',
    service=Services.FOOD_SHOPS_PAYMENTS,
    contract_services=[Services.FOOD_SHOPS_PAYMENTS.id, Services.FOOD_SHOPS_SERVICES.id],
    commission_service=Services.FOOD_SHOPS_SERVICES,
    payment_service=Services.FOOD_SHOPS_PAYMENTS,
)

RSYA_SSP_RU = Context().new(
    name='RSYA_SSP_RU',
    person_type=PersonTypes.UR,
    contract_type=ContractSubtype.PARTNERS,
    nds=NdsNew.DEFAULT,
    firm=Firms.YANDEX_1,
    currency=Currencies.RUB,
    rsya_contract_type=RsyaContractType.SSP,
    use_create_contract=False,
    special_contract_params={
        'ctype': 'PARTNERS'
    }
)

RSYA_OFFER_RU = RSYA_SSP_RU.new(
    name='RSYA_OFFER_RU',
    rsya_contract_type=RsyaContractType.OFFER,
    use_create_contract=False,
    is_offer=1
)

RSYA_OFFER_BY = RSYA_OFFER_RU.new(
    name='RSYA_OFFER_BY',
    person_type=PersonTypes.BYU,
    nds=NdsNew.BELARUS,
    firm=Firms.REKLAMA_BEL_27,
    currency=Currencies.BYN
)

RSYA_OFFER_SW = RSYA_OFFER_RU.new(
    name='RSYA_OFFER_SW',
    person_type=PersonTypes.SW_UR,
    nds=NdsNew.EUROPE_AG_RESIDENT,
    firm=Firms.EUROPE_AG_7,
    currency=Currencies.USD
)

RSYA_LICENSE_SW_SERVICES_AG = RSYA_OFFER_SW.new(
    name='RSYA_LICENSE_SW_SERVICES_AG',
    is_offer=1,
    person_type=PersonTypes.SW_YT,
    nds=NdsNew.NOT_RESIDENT,
    firm=Firms.SERVICES_AG_16,
    currency=Currencies.USD,
    rsya_contract_type=RsyaContractType.LICENSE,
)

DISK_B2B_CONTEXT = Context().new(
        # common
        name='DISK_B2B_CONTEXT',
        service=Services.DISK_B2B,
        person_type=PersonTypes.UR,
        firm=Firms.YANDEX_1,
        paysys=Paysyses.BANK_UR_RUB,
        currency=Currencies.RUB,
        payment_currency=Currencies.RUB,
        nds=NdsNew.DEFAULT,
        invoice_type=InvoiceType.PERSONAL_ACCOUNT,
        contract_type=ContractSubtype.GENERAL,
        product=Products.DISK_B2B_RUB,
)

DRIVE_B2B_CONTEXT = Context().new(
    name='DRIVE_B2B_CONTEXT',
    service=Services.DRIVE_B2B,
    person_type=PersonTypes.UR,
    firm=Firms.TAXI_13,
    currency=Currencies.RUB,
    payment_currency=Currencies.RUB,
    nds=NdsNew.DEFAULT,
    invoice_type=InvoiceType.PERSONAL_ACCOUNT,
    contract_type=ContractSubtype.GENERAL,
    contract_services=[Services.DRIVE_B2B.id,],
    paysys=Paysyses.BANK_UR_RUB_TAXI,
    special_contract_params={'personal_account': 1},
    migration_alias='drive_b2b',
)

DRIVE_CORP_CONTEXT = DRIVE_B2B_CONTEXT.new(
    name='DRIVE_CORP_CONTEXT',
    service=Services.DRIVE_CORP,
    firm=Firms.DRIVE_30,
    contract_services=[Services.DRIVE_CORP.id,],
    paysys=Paysyses.BANK_UR_RUB_CARSHARING,
)

UNIFIED_CORP_CONTRACT_CONTEXT = Context().new(
    name='UNIFIED_CORP_CONTRACT_CONTEXT',
    service=Services.TAXI_CORP_CLIENTS,
    person_type=PersonTypes.UR,
    firm=Firms.TAXI_13,
    currency=Currencies.RUB,
    payment_currency=Currencies.RUB,
    nds=NdsNew.DEFAULT,
    invoice_type=InvoiceType.PERSONAL_ACCOUNT,
    contract_type=ContractSubtype.GENERAL,
    contract_services=UNIFIED_CORP_CONTRACT_SERVICES,
    paysys=Paysyses.BANK_UR_RUB_TAXI,
    special_contract_params={'personal_account': 1},
    migration_alias='corp_taxi',
)

UNIFIED_CORP_CONTRACT_CONTEXT_WITH_ZAXI = UNIFIED_CORP_CONTRACT_CONTEXT.new(
    name='UNIFIED_CORP_CONTRACT_CONTEXT_WITH_ZAXI',
    contract_services=UNIFIED_CORP_CONTRACT_SERVICES + [Services.ZAXI_UNIFIED_CONTRACT.id],
)

HEALTH_PAYMENTS_PH_CONTEXT = Context().new(
    name='HEALTH_PAYMENTS_PH_CONTEXT',
    service=Services.HEALTH_PAYMENTS_PH,
    person_type=PersonTypes.PH,
    firm=Firms.HEALTH_114,
    currency=Currencies.RUB,
    payment_currency=Currencies.RUB,
    nds=NdsNew.DEFAULT,
    invoice_type=InvoiceType.PERSONAL_ACCOUNT,
    contract_type=ContractSubtype.GENERAL,
    paysys=Paysyses.BANK_PH_RUB_MEDICINE,
    paysys_wo_nds=Paysyses.BANK_PH_RUB_MEDICINE_WO_VAT,
    contract_services=[Services.HEALTH_PAYMENTS_PH.id,],
    tpt_payment_type=PaymentType.CARD,
    tpt_paysys_type_cc=PaysysType.MONEY,
    partner_integration={'scheme': SCHEMES.health_payments.ph},
    special_contract_params={'personal_account': 1, 'offer_confirmation_type': 'no'},
)

LOGISTICS_LK_CONTEXT = Context().new(
    name='LOGISTICS_LK_CONTEXT',
    service=Services.LOGISTICS_LK,
    person_type=PersonTypes.PH,
    firm=Firms.LOGISTICS_130,
    currency=Currencies.RUB,
    payment_currency=Currencies.RUB,
    nds=NdsNew.DEFAULT,
    invoice_type=InvoiceType.PERSONAL_ACCOUNT,
    contract_type=ContractSubtype.GENERAL,
    paysys=Paysyses.BANK_PH_RUB_LOGISTICS,
    product=Products.LOGISTICS_LK_MAIN_RUB,
    contract_services=[Services.LOGISTICS_LK.id,],
    tpt_payment_type=PaymentType.CARD,
    tpt_paysys_type_cc=PaysysType.MONEY,
    partner_integration={'scheme': SCHEMES.logistics_lk.ph},
)


ANNOUNCEMENT_CONTEXT = Context().new(
    name='ANNOUNCEMENT_CONTEXT',
    service=Services.ANNOUNCEMENT,
    person_type=PersonTypes.PH,
    firm=Firms.VERTICAL_12,
    currency=Currencies.RUB,
    payment_currency=Currencies.RUB,
    contract_type=ContractSubtype.GENERAL,
    contract_services=[Services.ANNOUNCEMENT.id],
    tpt_payment_type=PaymentType.CARD,
    tpt_paysys_type_cc=PaysysType.MONEY,
    special_contract_params={'personal_account': 1},
    nds=NdsNew.DEFAULT,
    invoice_type=InvoiceType.PERSONAL_ACCOUNT,
    paysys=Paysyses.BANK_PH_RUB_VERTICAL,
    paysys_wo_nds=Paysyses.BANK_PH_RUB_VERTICAL,
)


GAMES_CONTEXT = Context().new(
    name='GAMES_CONTEXT',
    service=Services.GAMES_PAYMENTS,
    person_type=PersonTypes.PH,
    firm=Firms.YANDEX_1,
    currency=Currencies.RUB,
    payment_currency=Currencies.RUB,
    contract_type=ContractSubtype.GENERAL,
    contract_services=[Services.GAMES_PAYMENTS.id],
    tpt_payment_type=PaymentType.CARD,
    tpt_paysys_type_cc=PaysysType.MONEY,
    special_contract_params={'personal_account': 1},
    nds=NdsNew.DEFAULT,
    invoice_type=InvoiceType.PERSONAL_ACCOUNT,
    paysys=Paysyses.BANK_PH_RUB,
    paysys_wo_nds=Paysyses.BANK_PH_RUB,
    partner_integration_cc='games_payments',
    partner_configuration_cc='games_payments',
)


GAMES_CONTEXT_USD_TRY = GAMES_CONTEXT.new(
    name='GAMES_CONTEXT_TRY',
    firm=Firms.SERVICES_AG_16,
    person_type=PersonTypes.SW_YTPH,
    currency=Currencies.USD,
    payment_currency=Currencies.TRY,
    tpt_payment_type=PaymentType.CARD,
    tpt_paysys_type_cc=PaysysType.MONEY,
    paysys=Paysyses.BANK_SW_YTPH_USD,
    paysys_wo_nds=Paysyses.BANK_SW_YTPH_USD,
    nds=NdsNew.EUROPE_AG_NON_RESIDENT,
    partner_configuration_cc='games_payments_try',
)


@attr.s
class FoodContext(object):
    name = attr.ib(type=str)
    restaurant = attr.ib(type=Context)
    courier = attr.ib(type=Context)
    courier_spendable = attr.ib(type=Context)


FOOD_CONTEXTS = [
    FoodContext(name='RU', restaurant=FOOD_RESTAURANT_CONTEXT, courier=FOOD_COURIER_CONTEXT,
                courier_spendable=FOOD_COURIER_SPENDABLE_CONTEXT),
    FoodContext(name='KZ', restaurant=FOOD_RESTAURANT_KZ_CONTEXT, courier=FOOD_COURIER_KZ_CONTEXT,
                courier_spendable=FOOD_COURIER_SPENDABLE_KZ_CONTEXT),
    FoodContext(name='BY', restaurant=FOOD_RESTAURANT_BY_CONTEXT, courier=FOOD_COURIER_BY_CONTEXT,
                courier_spendable=FOOD_COURIER_SPENDABLE_BY_CONTEXT),
    # вроде как courier и courier_spendable сервисы не используются, ресторанный сервис лавки переделали под магазины
    FoodContext(name='RU', restaurant=FOOD_SHOPS_CONTEXT, courier=None,
                courier_spendable=LAVKA_COURIER_SPENDABLE_CONTEXT),
    # FoodContext(name='KZ', restaurant=LAVKA_RESTAURANT_KZ_CONTEXT, courier=LAVKA_COURIER_KZ_CONTEXT,
    #             courier_spendable=LAVKA_COURIER_SPENDABLE_KZ_CONTEXT),
    # FoodContext(name='BY', restaurant=LAVKA_RESTAURANT_BY_CONTEXT, courier=LAVKA_COURIER_BY_CONTEXT,
    #             courier_spendable=LAVKA_COURIER_SPENDABLE_BY_CONTEXT),
]

EDA_HELP_CONTEXT = Context().new(
    name='EDA_HELP_CONTEXT',
    service=Services.EDA_HELP_PAYMENTS,
    person_type=PersonTypes.UR,
    firm=Firms.FOOD_32,
    currency=Currencies.RUB,
    payment_currency=Currencies.RUB,
    nds=NdsNew.YANDEX_RESIDENT,
    invoice_type=InvoiceType.PERSONAL_ACCOUNT,
    contract_type=ContractSubtype.GENERAL,
    paysys=Paysyses.BANK_UR_FOOD_RUB,
    min_commission=Decimal('0'),
    tpt_payment_type=PaymentType.CARD,
    tpt_paysys_type_cc=PaysysType.PAYTURE,
    special_contract_params={'personal_account': 1},
    migration_alias='eda_help_payments',

)

MAILPRO_CONTEXT = Context().new(
    # common
    name='MAILPRO_CONTEXT',
    service=Services.MAIL_PRO,
    person_type=PersonTypes.PH,
    firm=Firms.YANDEX_1,
    currency=Currencies.RUB,
    paysys=Paysyses.BANK_PH_RUB,
    nds=NdsNew.DEFAULT,
    contract_type=ContractSubtype.GENERAL,
    invoice_type=InvoiceType.PERSONAL_ACCOUNT,
    special_contract_params={'personal_account': 1},
    contract_services=[Services.MAIL_PRO.id],
    # thirdparty
    payment_currency=Currencies.RUB,
    tpt_payment_type=PaymentType.CARD,
    tpt_paysys_type_cc=PaysysType.MONEY,
    product=Products.MAILPRO_RUB_WITH_NDS,
    region=Regions.RU,
    # contracts
    contracts=[
        {
            'commission': ContractType.NOT_AGENCY,
            'currency': Currencies.RUB,
            'nds': NdsNew.YANDEX_RESIDENT,
            'nds_flag': 1,
            'product': Products.MAILPRO_RUB_WITH_NDS,
            'paysys': Paysyses.BANK_PH_RUB
        }
    ]
)

MAILPRO_NONRESIDENT_SW_CONTEXT = MAILPRO_CONTEXT.new(
    name='MAILPRO_NONRESIDENT_SW_CONTEXT',
    firm=Firms.SERVICES_AG_16,
    person_type=PersonTypes.SW_YTPH,
    currency=Currencies.USD,
    paysys=Paysyses.BANK_SW_YT_USD,
    payment_currency=Currencies.USD,
    region=Regions.SW,
    product=Products.MAILPRO_USD_WITHOUT_NDS,
    contracts=[
    {
            'commission': ContractType.NOT_AGENCY,
            'currency': Currencies.USD,
            'nds': NdsNew.NOT_RESIDENT,
            'nds_flag': 1,
            'product': Products.MAILPRO_USD_WITHOUT_NDS,
            'paysys': Paysyses.BANK_SW_YT_USD
        }
    ]
)

INGAME_PURCHASES_CONTEXT = Context().new(
    name='INGAME_PURCHASES_CONTEXT',
    service=Services.INGAME_PURCHASES,
    person_type=PersonTypes.PH,
    contract_services=[Services.INGAME_PURCHASES.id],
    firm=Firms.YANDEX_1,
    currency=Currencies.RUB,
    nds=NdsNew.ZERO,
    contract_type=ContractSubtype.SPENDABLE,
    page_id=Pages.INGAME_PURCHASES.id,
    pad_description=Pages.INGAME_PURCHASES.desc,
    pad_type_id=6,
    source_id=207,
)

INGAME_PURCHASES_CONTEXT_SW_YT_USD = INGAME_PURCHASES_CONTEXT.new(
    name='INGAME_PURCHASES_CONTEXT_SW_YT_USD',
    person_type=PersonTypes.SW_YT,
    firm=Firms.SERVICES_AG_16,
    currency=Currencies.USD,
)

INGAME_PURCHASES_CONTEXT_SW_YTPH_EUR = INGAME_PURCHASES_CONTEXT.new(
    name='INGAME_PURCHASES_CONTEXT_SW_YTPH_EUR',
    person_type=PersonTypes.SW_YTPH,
    firm=Firms.SERVICES_AG_16,
    currency=Currencies.EUR,
)

ADVERTISEMENT_CONTEXT_PH = Context().new(
    name='ADVERTISEMENT_CONTEXT_PH',
    service=Services.ADVERTISEMENT,
    person_type=PersonTypes.PH,
    contract_services=[Services.ADVERTISEMENT.id],
    firm=Firms.YANDEX_1,
    currency=Currencies.RUB,
    nds=NdsNew.ZERO,
    contract_type=ContractSubtype.SPENDABLE,
    page_id=Pages.ADVERTISEMENT.id,
    pad_description=Pages.ADVERTISEMENT.desc,
    pad_type_id=6,
    source_id=214,
)

ADVERTISEMENT_CONTEXT_UR = ADVERTISEMENT_CONTEXT_PH.new(
    name='ADVERTISEMENT_CONTEXT_UR',
    person_type=PersonTypes.UR,
)

ADVERTISEMENT_CONTEXT_USD_SW_YT = ADVERTISEMENT_CONTEXT_PH.new(
    name='ADVERTISEMENT_CONTEXT_USD_SW_YT',
    person_type=PersonTypes.SW_YT,
    firm=Firms.EUROPE_AG_7,
    currency=Currencies.USD,
)

ADVERTISEMENT_CONTEXT_USD_SW_YTPH = ADVERTISEMENT_CONTEXT_USD_SW_YT.new(
    name='ADVERTISEMENT_CONTEXT_USD_SW_YTPH',
    person_type=PersonTypes.SW_YTPH,
)

ADVERTISEMENT_CONTEXT_EUR_SW_YT = ADVERTISEMENT_CONTEXT_USD_SW_YT.new(
    name='ADVERTISEMENT_CONTEXT_EUR_SW_YT',
    currency=Currencies.EUR,
)

ADVERTISEMENT_CONTEXT_EUR_SW_YTPH = ADVERTISEMENT_CONTEXT_USD_SW_YTPH.new(
    name='ADVERTISEMENT_CONTEXT_EUR_SW_YTPH',
    currency=Currencies.EUR,
)

ADVERTISEMENT_CONTEXT_BYU = ADVERTISEMENT_CONTEXT_PH.new(
    name='ADVERTISEMENT_CONTEXT_BYU',
    person_type=PersonTypes.BYU,
    firm=Firms.REKLAMA_BEL_27,
)

ADVERTISEMENT_CONTEXTS = [ADVERTISEMENT_CONTEXT_PH, ADVERTISEMENT_CONTEXT_UR,
                          ADVERTISEMENT_CONTEXT_USD_SW_YT, ADVERTISEMENT_CONTEXT_USD_SW_YTPH,
                          ADVERTISEMENT_CONTEXT_EUR_SW_YT, ADVERTISEMENT_CONTEXT_EUR_SW_YTPH,
                          ADVERTISEMENT_CONTEXT_BYU]

DIGITAL_COROBA_RU_CONTEXT_SPENDABLE = Context().new(
    name='DIGITAL_COROBA_RU_CONTEXT_SPENDABLE',
    service=Services.DIGITAL_COROBA,
    person_type=PersonTypes.UR,
    firm=Firms.TAXI_13,
    currency=Currencies.RUB,
    nds=NdsNew.DEFAULT,
    contract_type=ContractSubtype.SPENDABLE,
    # thirdparty
    tpt_paysys_type_cc=PaysysType.YANDEX,
    payment_currency=Currencies.RUB,
    # partner_act_data
    pad_type_id=6,
)

PVZ_RU_CONTEXT_SPENDABLE = Context().new(
    name='PVZ_RU_CONTEXT_SPENDABLE',
    service=Services.PVZ,
    person_type=PersonTypes.UR,
    firm=Firms.MARKET_111,
    currency=Currencies.RUB,
    nds=NdsNew.ZERO,
    contract_type=ContractSubtype.SPENDABLE,
    # is_offer=1,
    # thirdparty
    tpt_paysys_type_cc=PaysysType.YANDEX,
    payment_currency=Currencies.RUB,
    # partner_act_data
    pad_type_id=6,
    partner_integration={'scheme': SCHEMES.pvz.default_conf},
)

PRACTICUM_RU_CONTEXT_SPENDABLE = Context().new(
    name='PRACTICUM_RU_CONTEXT_SPENDABLE',
    service=Services.PRACTICUM_SPENDABLE,
    person_type=PersonTypes.UR,
    firm=Firms.SHAD_34,
    currency=Currencies.RUB,
    nds=NdsNew.ZERO,
    contract_type=ContractSubtype.SPENDABLE,
    # is_offer=1,
    # thirdparty
    tpt_paysys_type_cc=PaysysType.YANDEX,
    payment_currency=Currencies.RUB,
    # partner_act_data
    pad_type_id=6,
    partner_integration={'scheme': SCHEMES.practicum.default_conf},
)

Y_PAY_RU_CONTEXT = Context().new(
    name='Y_PAY_RU_CONTEXT_SPENDABLE',
    service=Services.Y_PAY,
    person_type=PersonTypes.UR,
    firm=Firms.YANDEX_1,
    currency=Currencies.RUB,
    contract_type=ContractSubtype.GENERAL,
    contract_services=[Services.Y_PAY.id],
    nds=NdsNew.DEFAULT,
    paysys=Paysyses.BANK_UR_RUB,

    payment_currency=Currencies.RUB,
    tpt_paysys_type_cc=PaysysType.MONEY,
)

ZAXI_SELFEMPLOYED_RU_CONTEXT_SPENDABLE = Context().new(
    name='ZAXI_SELFEMPLOYED_RU_CONTEXT_SPENDABLE',
    service=Services.ZAXI_SELFEMPLOYED_SPENDABLE,
    person_type=PersonTypes.PH,
    firm=Firms.GAS_STATIONS_124,
    currency=Currencies.RUB,
    nds=NdsNew.ZERO,
    contract_type=ContractSubtype.SPENDABLE,
    # is_offer=1,
    # thirdparty
    tpt_paysys_type_cc=PaysysType.YANDEX,
    tpt_payment_type=PaymentType.ZAXI_SELFEMPLOYED,
    payment_currency=Currencies.RUB,
    # partner_act_data
    pad_type_id=6,
    partner_integration={'scheme': SCHEMES.zaxi.selfemployed_spendable},
)

SORT_CENTER_CONTEXT_SPENDABLE = PVZ_RU_CONTEXT_SPENDABLE.new(
    name='SORT_CENTER_CONTEXT_SPENDABLE',
    service=Services.SORT_CENTER,
    nds=NdsNew.DEFAULT,
    partner_integration={'scheme': SCHEMES.pvz.sort_center_conf},
    tpt_paysys_type_cc=PaysysType.SORT_CENTER,
)

TAXI_RU_INTERCOMPANY_CONTEXT = TAXI_RU_CONTEXT_CLONE.new(
    name='TAXI_RU_INTERCOMPANY_CONTEXT',
    client_intercompany='RU10',
    special_contract_params={'personal_account': 1, 'country': Regions.RU.id,
                             'region': CountryRegion.RUS,
                             'nds_for_receipt': NdsNew.DEFAULT.nds_id,
                             'partner_commission_pct2': Decimal('10.2')},
)

DELIVERY_SERVICES_CONTEXT_SPENDABLE = PVZ_RU_CONTEXT_SPENDABLE.new(
    name='DELIVERY_SERVICES_CONTEXT_SPENDABLE',
    service=Services.MARKET_DELIVERY_SERVICES,
    nds=NdsNew.DEFAULT,
    is_offer=1,
    partner_integration={'scheme': SCHEMES.pvz.delivery_services_conf},
    tpt_paysys_type_cc=PaysysType.MARKET_DELIVERY,
)

INVESTMENTS_CONTEXT = Context().new(
    name='INVESTMENTS_CONTEXT',
    service=Services.INVESTMENTS,
    firm=Firms.YANDEX_1,
    currency=Currencies.RUB,
    contract_type=ContractSubtype.GENERAL,
    person_type=PersonTypes.UR,
    partner_integration={'scheme': SCHEMES.investments.default_conf},

    payment_currency=Currencies.RUB,
    tpt_payment_type=PaymentType.CARD,
    tpt_paysys_type_cc=PaysysType.MONEY,
)

USLUGI_CONTEXT = Context().new(
    name='USLUGI_CONTEXT',
    service=Services.USLUGI,
    firm=Firms.YANDEX_1,
    currency=Currencies.RUB,
    contract_type=ContractSubtype.GENERAL,
    person_type=PersonTypes.UR,
    partner_integration={'scheme': SCHEMES.uslugi.default_conf},

    payment_currency=Currencies.RUB,
    tpt_payment_type=PaymentType.CARD,
    tpt_paysys_type_cc=PaysysType.MONEY,
)

MARKET_BLUE_AGENCY_CONTEXT_SPENDABLE = Context().new(
    name='MARKET_BLUE_AGENCY_CONTEXT_SPENDABLE',
    service=Services.MARKET_BLUE_AGENCY,
    person_type=PersonTypes.UR,
    firm=Firms.MARKET_111,
    currency=Currencies.RUB,
    nds=NdsNew.DEFAULT,
    contract_type=ContractSubtype.SPENDABLE,
    # is_offer=1,
    # thirdparty
    tpt_paysys_type_cc=PaysysType.YANDEX,
    payment_currency=Currencies.RUB,
    # partner_act_data
    pad_type_id=6,
)

K50_COMMON_CONTEXT = Context().new(
    name='K50_COMMON_CONTEXT',
    service=Services.K50,
    firm=Firms.K50,
    currency=Currencies.RUB,
    payment_currency=Currencies.RUB,
    invoice_type=InvoiceType.PERSONAL_ACCOUNT,
    contract_type=ContractSubtype.GENERAL,
    product=Products.K50_MAIN_RUB,
    order_type2product={K50OrderType.GENERATOR: Products.K50_GENERATOR_RUB,
                        K50OrderType.OPTIMIZATOR: Products.K50_OPTIMIZATOR_RUB,
                        K50OrderType.TRACKER: Products.K50_TRACKER_RUB,
                        K50OrderType.BI: Products.K50_BI_RUB}
)

K50_UR_POSTPAY_CONTRACT_CONTEXT = K50_COMMON_CONTEXT.new(
    name='K50_UR_POSTPAY_CONTRACT_CONTEXT',
    nds=NdsNew.DEFAULT,
    person_type=PersonTypes.UR,
    paysys=Paysyses.BANK_UR_RUB_K50,
    partner_integration={'scheme': SCHEMES.k50.postpay_contract},
)

K50_UR_PREPAY_CONTRACT_CONTEXT = K50_UR_POSTPAY_CONTRACT_CONTEXT.new(
    name='K50_UR_PREPAY_CONTRACT_CONTEXT',
    partner_integration={'scheme': SCHEMES.k50.prepay_contract},
)

K50_UR_POSTPAY_OFFER_CONTEXT = K50_UR_POSTPAY_CONTRACT_CONTEXT.new(
    name='K50_UR_POSTPAY_OFFER_CONTEXT',
    partner_integration={'scheme': SCHEMES.k50.postpay_offer},
)

K50_UR_PREPAY_OFFER_CONTEXT = K50_UR_POSTPAY_CONTRACT_CONTEXT.new(
    name='K50_UR_PREPAY_OFFER_CONTEXT',
    partner_integration={'scheme': SCHEMES.k50.prepay_offer},
)

K50_YT_POSTPAY_CONTRACT_CONTEXT = K50_COMMON_CONTEXT.new(
    name='K50_YT_POSTPAY_CONTRACT_CONTEXT',
    nds=NdsNew.NOT_RESIDENT,
    person_type=PersonTypes.YT,
    paysys=Paysyses.BANK_YT_RUB_K50,
    partner_integration={'scheme': SCHEMES.k50.postpay_contract},
)

K50_YT_PREPAY_CONTRACT_CONTEXT = K50_YT_POSTPAY_CONTRACT_CONTEXT.new(
    name='K50_YT_PREPAY_CONTRACT_CONTEXT',
    partner_integration={'scheme': SCHEMES.k50.prepay_contract},
)

K50_YT_POSTPAY_OFFER_CONTEXT = K50_YT_POSTPAY_CONTRACT_CONTEXT.new(
    name='K50_YT_POSTPAY_OFFER_CONTEXT',
    partner_integration={'scheme': SCHEMES.k50.postpay_offer},
)

K50_YT_PREPAY_OFFER_CONTEXT = K50_YT_POSTPAY_CONTRACT_CONTEXT.new(
    name='K50_YT_PREPAY_OFFER_CONTEXT',
    partner_integration={'scheme': SCHEMES.k50.prepay_offer},
)

K50_YTPH_POSTPAY_CONTRACT_CONTEXT = K50_YT_POSTPAY_CONTRACT_CONTEXT.new(
    name='K50_YTPH_POSTPAY_CONTRACT_CONTEXT',
    person_type=PersonTypes.YTPH,
    paysys=Paysyses.BANK_YTPH_RUB_K50
)

MARKETING_SERVICES_CONTEXT = Context().new(
    # common
    name='MARKETING_SERVICES_CONTEXT',
    service=Services.BILLING_MARKETING_SERVICES,
    person_type=PersonTypes.UR,
    firm=Firms.MARKET_111,
    currency=Currencies.RUB,
    #paysys=Paysyses.BANK_UR_RUB_MARKET,
    nds=NdsNew.DEFAULT,
    contract_type=ContractSubtype.GENERAL,
    invoice_type=InvoiceType.PERSONAL_ACCOUNT,
    #special_contract_params={'personal_account': 1, 'netting': 1},
    manager=Managers.SOME_MANAGER.uid,
    #contract_services=[Services.BLUE_MARKET_PAYMENTS.id, Services.BLUE_MARKET.id],
    partner_integration={'scheme': SCHEMES.market_marketing_services.market_marketing_services_default_conf},
    # thirdparty
    payment_currency=Currencies.RUB,
    #tpt_payment_type=PaymentType.CARD,
    #tpt_paysys_type_cc=PaysysType.MONEY,  # PaysysType.ALFA
    # min_commission=Decimal('0'), #АВ всегда 0, т.к. на commission_category не смотрим, а в договоре поля для процента для России нет
    # precision=2, #точность округления АВ
    # currency_rate_src=CurrencyRateSource.CBRF,
)

NEWS_CONTEXT_SPENDABLE_PH = Context().new(
    name='NEWS_CONTEXT_SPENDABLE_PH',
    service=Services.NEWS_PAYMENT,
    person_type=PersonTypes.PH,
    firm=Firms.YANDEX_1,
    currency=Currencies.RUB,
    nds=NdsNew.ZERO,
    contract_type=ContractSubtype.SPENDABLE,
    page_id=Pages.NEWS_PAYMENT.id,
    contract_services=[Services.NEWS_PAYMENT.id],
    pad_description=Pages.NEWS_PAYMENT.desc,
    # is_offer=1,
    # thirdparty
    tpt_paysys_type_cc=PaysysType.NEWS_PAYMENT,
    tpt_payment_type=PaymentType.NEWS_PAYMENT,
    payment_currency=Currencies.RUB,
    # partner_act_data
    pad_type_id=6,
    partner_integration={'scheme': SCHEMES.news.default_conf},
)

NEWS_CONTEXT_SPENDABLE_UR = Context().new(
    name='NEWS_CONTEXT_SPENDABLE_UR',
    service=Services.NEWS_PAYMENT,
    person_type=PersonTypes.UR,
    firm=Firms.YANDEX_1,
    currency=Currencies.RUB,
    nds=NdsNew.ZERO,
    contract_type=ContractSubtype.SPENDABLE,
    page_id=Pages.NEWS_PAYMENT.id,
    contract_services=[Services.NEWS_PAYMENT.id],
    pad_description=Pages.NEWS_PAYMENT.desc,
    # is_offer=1,
    # thirdparty
    tpt_paysys_type_cc=PaysysType.NEWS_PAYMENT,
    tpt_payment_type=PaymentType.NEWS_PAYMENT,
    payment_currency=Currencies.RUB,
    # partner_act_data
    pad_type_id=6,
    partner_integration={'scheme': SCHEMES.news.default_conf},
)

NEWS_CONTEXT_SPENDABLE_PH_NDS = Context().new(
    name='NEWS_CONTEXT_SPENDABLE_PH_NDS',
    service=Services.NEWS_PAYMENT,
    person_type=PersonTypes.PH,
    firm=Firms.YANDEX_1,
    currency=Currencies.RUB,
    nds=NdsNew.DEFAULT,
    contract_type=ContractSubtype.SPENDABLE,
    page_id=Pages.NEWS_PAYMENT.id,
    contract_services=[Services.NEWS_PAYMENT.id],
    pad_description=Pages.NEWS_PAYMENT.desc,
    # is_offer=1,
    # thirdparty
    tpt_paysys_type_cc=PaysysType.NEWS_PAYMENT,
    tpt_payment_type=PaymentType.NEWS_PAYMENT,
    payment_currency=Currencies.RUB,
    # partner_act_data
    pad_type_id=6,
    partner_integration={'scheme': SCHEMES.news.default_conf},
)

NEWS_CONTEXT_SPENDABLE_UR_NDS = Context().new(
    name='NEWS_CONTEXT_SPENDABLE_UR_NDS',
    service=Services.NEWS_PAYMENT,
    person_type=PersonTypes.UR,
    firm=Firms.YANDEX_1,
    currency=Currencies.RUB,
    nds=NdsNew.DEFAULT,
    contract_type=ContractSubtype.SPENDABLE,
    page_id=Pages.NEWS_PAYMENT.id,
    contract_services=[Services.NEWS_PAYMENT.id],
    pad_description=Pages.NEWS_PAYMENT.desc,
    # is_offer=1,
    # thirdparty
    tpt_paysys_type_cc=PaysysType.NEWS_PAYMENT,
    tpt_payment_type=PaymentType.NEWS_PAYMENT,
    payment_currency=Currencies.RUB,
    # partner_act_data
    pad_type_id=6,
    partner_integration={'scheme': SCHEMES.news.default_conf},
)

TAXI_UBER_BV_BY_BYN_PLUS_AV_CONTEXT = TAXI_UBER_BV_BY_BYN_CONTEXT.new(
    # common
    name='TAXI_UBER_BV_BY_BYN_PLUS_AV_CONTEXT',
    special_contract_params={'personal_account': 1, 'country': Regions.BY.id,
                             'partner_commission_pct2': Decimal('5.7'),
                             'unilateral': 1},
    contract_services=[Services.TAXI.id, Services.UBER.id]
)


TAXI_YA_TAXI_CORP_KZ_KZT_PLUS_AV_CONTEXT = TAXI_YA_TAXI_CORP_KZ_KZT_CONTEXT.new(
    # common
    name='TAXI_YA_TAXI_CORP_KZ_KZT_PLUS_AV_CONTEXT',
    service=Services.TAXI,
    person_type=PersonTypes.UR_YTKZ,
    firm=Firms.TAXI_CORP_KZT_31,
    currency=Currencies.KZT,
    paysys=Paysyses.BANK_UR_KZT_TAXI_CORP_UR_YTKZ,
    nds=NdsNew.KAZAKHSTAN,
    contract_type=ContractSubtype.GENERAL,
    invoice_type=InvoiceType.PERSONAL_ACCOUNT,
    special_contract_params={'personal_account': 1, 'country': Regions.KZ.id,
                             'partner_commission_pct2': Decimal('5.7')},
    region=Regions.KZ,
    contract_services=[Services.TAXI.id, Services.UBER.id],
    client_intercompany='RU10',
    partner_integration={'scheme': '{"nds": {"default": 1}}'}
)

BLACK_MARKET_1173_CONTEXT = Context().new(
    # common
    name='BLACK_MARKET_1173_CONTEXT',
    service=Services.BLACK_MARKET,
    person_type=PersonTypes.UR,
    firm=Firms.MARKET_111,
    currency=Currencies.RUB,
    nds=NdsNew.DEFAULT,
    contract_type=ContractSubtype.GENERAL,
    manager=Managers.SOME_MANAGER.uid,

)

MAGISTRALS_CARRIER_RU_CONTEXT = Context().new(
    name='MAGISTRALS_CARRIER_RU_CONTEXT',
    service=Services.MAGISTRALS_CARRIER_COMMISSION,
    person_type=PersonTypes.UR,
    firm=Firms.LOGISTICS_130,
    currency=Currencies.RUB,
    nds=NdsNew.DEFAULT,
    contract_type=ContractSubtype.GENERAL,
    is_offer=1,
    invoice_type=InvoiceType.PERSONAL_ACCOUNT,
    region=Regions.RU,
    special_contract_params={'personal_account': 1, 'country': Regions.RU.id, 'netting': 1,
                             'netting_pct': Decimal('100')},
    paysys=Paysyses.BANK_UR_RUB_LOGISTICS,
    contract_services=[Services.MAGISTRALS_CARRIER_COMMISSION.id, Services.MAGISTRALS_CARRIER_AGENT.id],
    service_codes_params={
        'YANDEX_SERVICE': {
            'nds': NdsNew.DEFAULT,
            'paysys': Paysyses.BANK_UR_RUB_LOGISTICS,
        },
    }
)

MAGISTRALS_SENDER_RU_CONTEXT = Context().new(
    name='MAGISTRALS_SENDER_RU_CONTEXT',
    service=Services.MAGISTRALS_SENDER_COMMISSION,
    person_type=PersonTypes.UR,
    firm=Firms.LOGISTICS_130,
    currency=Currencies.RUB,
    nds=NdsNew.DEFAULT,
    contract_type=ContractSubtype.GENERAL,
    is_offer=1,
    invoice_type=InvoiceType.PERSONAL_ACCOUNT,
    region=Regions.RU,
    special_contract_params={'personal_account': 1, 'country': Regions.RU.id, },
    paysys=Paysyses.BANK_UR_RUB_LOGISTICS,
    contract_services=[Services.MAGISTRALS_SENDER_COMMISSION.id, Services.MAGISTRALS_SENDER_AGENT.id],
    service_codes_for_balances={'YANDEX_SERVICE': NdsNew.DEFAULT, 'AGENT_REWARD_MAGISTR': NdsNew.ZERO},
    service_codes_params={
        'AGENT_REWARD_MAGISTR': {
            'nds': NdsNew.ZERO,
            'paysys': Paysyses.BANK_UR_RUB_WO_NDS_LOGISTICS,
        },
        'YANDEX_SERVICE': {
            'nds': NdsNew.DEFAULT,
            'paysys': Paysyses.BANK_UR_RUB_LOGISTICS,
        },
    }
)

DOUBLE_CLOUD_USA_RESIDENT_UR = Context().new(
    name='DOUBLE_CLOUD_USA_RESIDENT_UR',
    service=Services.DOUBLE_CLOUD_GENERAL,
    person_type=PersonTypes.USU,
    firm=Firms.DOUBLE_CLOUD_INC,
    currency=Currencies.USD,
    nds=NdsNew.ZERO,  # в ЛС разный ндс, здесь просто чтобы тестовые методы не падали
    contract_type=ContractSubtype.GENERAL,
    is_offer=0,
    invoice_type=InvoiceType.PERSONAL_ACCOUNT,
    region=Regions.US,
    special_contract_params={'personal_account': 1, 'country': Regions.US.id, },
    paysys=Paysyses.BANK_USU_USD,
    contract_services=[Services.DOUBLE_CLOUD_GENERAL.id],
    partner_integration_cc='double_cloud',
    partner_configuration_cc='double_cloud_usa_resident',
)

DOUBLE_CLOUD_USA_RESIDENT_PH = Context().new(
    name='DOUBLE_CLOUD_USA_RESIDENT_PH',
    service=Services.DOUBLE_CLOUD_GENERAL,
    person_type=PersonTypes.USP,
    firm=Firms.DOUBLE_CLOUD_INC,
    currency=Currencies.USD,
    nds=NdsNew.ZERO,  # в ЛС разный ндс, здесь просто чтобы тестовые методы не падали
    contract_type=ContractSubtype.GENERAL,
    is_offer=0,
    invoice_type=InvoiceType.PERSONAL_ACCOUNT,
    region=Regions.US,
    special_contract_params={'personal_account': 1, 'country': Regions.US.id, },
    paysys=Paysyses.BANK_USP_USD,
    contract_services=[Services.DOUBLE_CLOUD_GENERAL.id],
    partner_integration_cc='double_cloud',
    partner_configuration_cc='double_cloud_usa_resident',
)

DOUBLE_CLOUD_USA_NONRESIDENT_UR = Context().new(
    name='DOUBLE_CLOUD_USA_NONRESIDENT_UR',
    service=Services.DOUBLE_CLOUD_GENERAL,
    person_type=PersonTypes.US_YT,
    firm=Firms.DOUBLE_CLOUD_INC,
    currency=Currencies.USD,
    nds=NdsNew.ZERO,  # в ЛС разный ндс, здесь просто чтобы тестовые методы не падали
    contract_type=ContractSubtype.GENERAL,
    is_offer=0,
    invoice_type=InvoiceType.PERSONAL_ACCOUNT,
    region=Regions.US,
    special_contract_params={'personal_account': 1, 'country': Regions.US.id, },
    paysys=Paysyses.BANK_USYT_USD,
    contract_services=[Services.DOUBLE_CLOUD_GENERAL.id],
    partner_integration_cc='double_cloud',
    partner_configuration_cc='double_cloud_usa_nonresident',
)

DOUBLE_CLOUD_USA_NONRESIDENT_PH = Context().new(
    name='DOUBLE_CLOUD_USA_NONRESIDENT_PH',
    service=Services.DOUBLE_CLOUD_GENERAL,
    person_type=PersonTypes.US_YT_PH,
    firm=Firms.DOUBLE_CLOUD_INC,
    currency=Currencies.USD,
    nds=NdsNew.ZERO,  # в ЛС разный ндс, здесь просто чтобы тестовые методы не падали
    contract_type=ContractSubtype.GENERAL,
    is_offer=0,
    invoice_type=InvoiceType.PERSONAL_ACCOUNT,
    region=Regions.US,
    special_contract_params={'personal_account': 1, 'country': Regions.US.id, },
    paysys=Paysyses.BANK_USYTPH_USD,
    contract_services=[Services.DOUBLE_CLOUD_GENERAL.id],
    partner_integration_cc='double_cloud',
    partner_configuration_cc='double_cloud_usa_nonresident',
)

DOUBLE_CLOUD_DE_RESIDENT_UR = Context().new(
    name='DOUBLE_CLOUD_DE_RESIDENT_UR',
    service=Services.DOUBLE_CLOUD_GENERAL,
    person_type=PersonTypes.DE_UR,
    firm=Firms.DOUBLE_CLOUD_GMBH,
    currency=Currencies.EUR,
    nds=NdsNew.ZERO,  # в ЛС разный ндс, здесь просто чтобы тестовые методы не падали
    contract_type=ContractSubtype.GENERAL,
    is_offer=0,
    invoice_type=InvoiceType.PERSONAL_ACCOUNT,
    region=Regions.GR,
    special_contract_params={'personal_account': 1, 'country': Regions.GR.id, },
    paysys=Paysyses.BANK_DEUR_EUR,
    contract_services=[Services.DOUBLE_CLOUD_GENERAL.id],
    partner_integration_cc='double_cloud',
    partner_configuration_cc='double_cloud_de_resident',
)

DOUBLE_CLOUD_DE_RESIDENT_PH = Context().new(
    name='DOUBLE_CLOUD_DE_RESIDENT_PH',
    service=Services.DOUBLE_CLOUD_GENERAL,
    person_type=PersonTypes.DE_PH,
    firm=Firms.DOUBLE_CLOUD_GMBH,
    currency=Currencies.EUR,
    nds=NdsNew.ZERO,  # в ЛС разный ндс, здесь просто чтобы тестовые методы не падали
    contract_type=ContractSubtype.GENERAL,
    is_offer=0,
    invoice_type=InvoiceType.PERSONAL_ACCOUNT,
    region=Regions.GR,
    special_contract_params={'personal_account': 1, 'country': Regions.GR.id, },
    paysys=Paysyses.BANK_DEPH_EUR,
    contract_services=[Services.DOUBLE_CLOUD_GENERAL.id],
    partner_integration_cc='double_cloud',
    partner_configuration_cc='double_cloud_de_resident',
)

DOUBLE_CLOUD_DE_NONRESIDENT_UR = Context().new(
    name='DOUBLE_CLOUD_DE_NONRESIDENT_UR',
    service=Services.DOUBLE_CLOUD_GENERAL,
    person_type=PersonTypes.DE_YT,
    firm=Firms.DOUBLE_CLOUD_GMBH,
    currency=Currencies.EUR,
    nds=NdsNew.ZERO,  # в ЛС разный ндс, здесь просто чтобы тестовые методы не падали
    contract_type=ContractSubtype.GENERAL,
    is_offer=0,
    invoice_type=InvoiceType.PERSONAL_ACCOUNT,
    region=Regions.GR,
    special_contract_params={'personal_account': 1, 'country': Regions.GR.id, },
    paysys=Paysyses.BANK_DEYT_EUR,
    contract_services=[Services.DOUBLE_CLOUD_GENERAL.id],
    partner_integration_cc='double_cloud',
    partner_configuration_cc='double_cloud_de_nonresident',
)

DOUBLE_CLOUD_DE_NONRESIDENT_PH = Context().new(
    name='DOUBLE_CLOUD_DE_NONRESIDENT_PH',
    service=Services.DOUBLE_CLOUD_GENERAL,
    person_type=PersonTypes.DE_YTPH,
    firm=Firms.DOUBLE_CLOUD_GMBH,
    currency=Currencies.EUR,
    nds=NdsNew.ZERO,  # в ЛС разный ндс, здесь просто чтобы тестовые методы не падали
    contract_type=ContractSubtype.GENERAL,
    is_offer=0,
    invoice_type=InvoiceType.PERSONAL_ACCOUNT,
    region=Regions.GR,
    special_contract_params={'personal_account': 1, 'country': Regions.GR.id, },
    paysys=Paysyses.BANK_DEYTPH_EUR,
    contract_services=[Services.DOUBLE_CLOUD_GENERAL.id],
    partner_integration_cc='double_cloud',
    partner_configuration_cc='double_cloud_de_nonresident',
)


TAXI_YANGO_DELIVERY_ISRAEL_CONTEXT = TAXI_DELIVERY_ISRAEL_CONTEXT.new(
    # common
    name='TAXI_YANGO_DELIVERY_ISRAEL_CONTEXT',
    firm=Firms.YANGO_DELIVERY,
    paysys=Paysyses.BANK_IL_UR_ILS_YANGO_DELIVERY,
)

TAXI_YANGO_DELIVERY_ISRAEL_CONTEXT_SPENDABLE = TAXI_DELIVERY_ISRAEL_CONTEXT_SPENDABLE.new(
    # common
    name='TAXI_YANGO_DELIVERY_ISRAEL_CONTEXT_SPENDABLE',
    firm=Firms.YANGO_DELIVERY,
    paysys=Paysyses.BANK_IL_UR_ILS_YANGO_DELIVERY,
)

LOGISTICS_PARTNERS_YANGO_DELIVERY_ISRAEL_CONTEXT_SPENDABLE = LOGISTICS_PARTNERS_ISRAEL_CONTEXT_SPENDABLE.new(
    name='LOGISTICS_PARTNERS_YANGO_DELIVERY_ISRAEL_CONTEXT_SPENDABLE',
    firm=Firms.YANGO_DELIVERY,
    paysys=Paysyses.BANK_IL_UR_ILS_YANGO_DELIVERY,
)

LOGISTICS_CLIENTS_YANGO_DELIVERY_ISRAEL_CONTEXT_GENERAL = LOGISTICS_CLIENTS_ISRAEL_CONTEXT_GENERAL.new(
    name='LOGISTICS_CLIENTS_YANGO_DELIVERY_ISRAEL_CONTEXT_GENERAL',
    firm=Firms.YANGO_DELIVERY,
    paysys=Paysyses.BANK_IL_UR_ILS_YANGO_DELIVERY,
)

TAXI_BV_COD_EUR_CONTEXT = TAXI_RU_CONTEXT.new(
    # common
    name='TAXI_BV_COD_EUR_CONTEXT',
    person_type=PersonTypes.EU_YT,
    firm=Firms.TAXI_BV_22,
    currency=Currencies.EUR,
    paysys=Paysyses.BANK_UR_EUR_TAXI_BV,
    nds=NdsNew.NOT_RESIDENT,
    special_contract_params={'personal_account': 1, 'country': Regions.COD.id,
                             'partner_commission_pct2': Decimal('10.2')},
    region=Regions.COD,
    contract_services=[Services.TAXI.id, Services.TAXI_111.id, Services.TAXI_128.id],
    monetization_services=[Services.TAXI_111.id, Services.TAXI_128.id],
    # thirdparty
    payment_currency=Currencies.EUR,
    tpt_paysys_type_cc=PaysysType.MONEY,  # PaysysType.WALLET
    min_commission=Decimal('0.01'),  # минимальная комиссия АВ, если в договоре процент не 0 или вообще не задан
    currency_rate_src=CurrencyRateSource.ECB,
)

TAXI_BV_COD_EUR_CONTEXT_SPENDABLE = TAXI_BV_COD_EUR_CONTEXT.new(
    # common
    name='TAXI_BV_COD_EUR_CONTEXT_SPENDABLE',
    service=Services.TAXI_DONATE,
    contract_type=ContractSubtype.SPENDABLE,
    special_contract_params={'country': Regions.COD.id},
    contract_services=[Services.TAXI_DONATE.id],
    # thirdparty
    tpt_paysys_type_cc=PaysysType.TAXI,
    # partner_act_data
    pad_type_id=6,
)

TAXI_BV_COD_EUR_CONTEXT_SPENDABLE_SCOUTS = TAXI_BV_COD_EUR_CONTEXT_SPENDABLE.new(
    # common
    name='TAXI_BV_COD_EUR_CONTEXT_SPENDABLE_SCOUTS',
    service=Services.SCOUTS,
    contract_services=[Services.SCOUTS.id],
    # thirdparty
    tpt_payment_type=PaymentType.SCOUT,
    oebs_contract_type=None,
    # partner_act_data
    page_id=Pages.SCOUTS.id,
    pad_description=Pages.SCOUTS.desc,
    # partner_act_data
    pad_type_id=4,
)

TAXI_UBER_BEL_BYN_CONTEXT = TAXI_RU_CONTEXT.new(
    # common
    name='TAXI_UBER_BEL_BYN_CONTEXT',
    service=Services.TAXI,
    person_type=PersonTypes.BYU,
    firm=Firms.UBER_SYSTEMS_BEL,
    currency=Currencies.BYN,
    paysys=Paysyses.BANK_UR_UBER_BEL_BYN,
    nds=NdsNew.NOT_RESIDENT,
    special_contract_params={'personal_account': 1, 'country': Regions.BY.id,
                             'partner_commission_pct2': Decimal('10.2')},
    region=Regions.BY,
    contract_services=[Services.TAXI.id, Services.TAXI_111.id, Services.TAXI_128.id,
                       Services.UBER.id, Services.UBER_ROAMING.id],
    monetization_services=[Services.TAXI_111.id, Services.TAXI_128.id],
    # thirdparty
    min_commission=Decimal('0.01'),  # минимальная комиссия АВ, если в договоре процент не 0 или вообще не задан
    currency_rate_src=CurrencyRateSource.ECB,
    payment_currency=Currencies.BYN,
    tpt_paysys_type_cc=PaysysType.MONEY,
)

TAXI_UBER_BEL_BYN_CONTEXT_SPENDABLE = TAXI_UBER_BEL_BYN_CONTEXT.new(
    # common
    name='TAXI_UBER_BEL_BYN_CONTEXT_SPENDABLE',
    service=Services.PAYMENT_BRAND,
    contract_type=ContractSubtype.SPENDABLE,
    special_contract_params={'country': Regions.BY.id},
    contract_services=[Services.PAYMENT_BRAND.id],
    # thirdparty
    tpt_paysys_type_cc=PaysysType.TAXI,
    # partner_act_data
    pad_type_id=6,
)

TAXI_UBER_BEL_BYN_CONTEXT_NDS = TAXI_UBER_BEL_BYN_CONTEXT.new(
    nds=NdsNew.BELARUS,
)

TAXI_UBER_BEL_BYN_CONTEXT_NDS_SPENDABLE = TAXI_UBER_BEL_BYN_CONTEXT_SPENDABLE.new(
    nds=NdsNew.BELARUS,
)


LOGISTICS_PARTNERS_YANDEX_LOG_OZB_USD_CONTEXT_SPENDABLE = Context().new(
    name='LOGISTICS_PARTNERS_YANDEX_LOG_OZB_USD_CONTEXT_SPENDABLE',
    service=Services.LOGISTICS_PARTNERS,
    person_type=PersonTypes.UZB_UR,
    firm=Firms.YANDEX_LOG_OZB,
    currency=Currencies.USD,
    nds=NdsNew.UZB,
    contract_type=ContractSubtype.SPENDABLE,
    is_offer=1,
    special_contract_params={'country': Regions.UZB.id},
    # thirdparty
    payment_currency=Currencies.USD,
    # partner_act_data
    pad_type_id=6,
)

LOGISTICS_CLIENTS_YANDEX_LOG_OZB_USD_CONTEXT_GENERAL = Context().new(
    name='LOGISTICS_CLIENTS_YANDEX_LOG_OZB_USD_CONTEXT_GENERAL',
    service=Services.LOGISTICS_CLIENTS,
    person_type=PersonTypes.UZB_UR,
    firm=Firms.YANDEX_LOG_OZB,
    currency=Currencies.USD,
    nds=NdsNew.UZB,
    contract_type=ContractSubtype.GENERAL,
    is_offer=1,
    invoice_type=InvoiceType.PERSONAL_ACCOUNT,
    special_contract_params={'personal_account': 1},
    paysys=Paysyses.BANK_UZB_UR_USD,
    contract_services=[Services.LOGISTICS_CLIENTS.id],
)

LOGISTICS_PARTNERS_YANDEX_LOG_OZB_RUB_CONTEXT_SPENDABLE = LOGISTICS_PARTNERS_YANDEX_LOG_OZB_USD_CONTEXT_SPENDABLE.new(
    name='LOGISTICS_PARTNERS_YANDEX_LOG_OZB_RUB_CONTEXT_SPENDABLE',
    currency=Currencies.RUB,
    payment_currency=Currencies.RUB,
)

LOGISTICS_CLIENTS_YANDEX_LOG_OZB_RUB_CONTEXT_GENERAL = LOGISTICS_CLIENTS_YANDEX_LOG_OZB_USD_CONTEXT_GENERAL.new(
    name='LOGISTICS_CLIENTS_YANDEX_LOG_OZB_RUB_CONTEXT_GENERAL',
    currency=Currencies.RUB,
    paysys=Paysyses.BANK_UZB_UR_RUB,
)

LOGISTICS_PARTNERS_YANDEX_LOG_OZB_UZS_CONTEXT_SPENDABLE = LOGISTICS_PARTNERS_YANDEX_LOG_OZB_USD_CONTEXT_SPENDABLE.new(
    name='LOGISTICS_PARTNERS_YANDEX_LOG_OZB_UZS_CONTEXT_SPENDABLE',
    currency=Currencies.UZS,
    payment_currency=Currencies.UZS,
)

LOGISTICS_CLIENTS_YANDEX_LOG_OZB_UZS_CONTEXT_GENERAL = LOGISTICS_CLIENTS_YANDEX_LOG_OZB_USD_CONTEXT_GENERAL.new(
    name='LOGISTICS_CLIENTS_YANDEX_LOG_OZB_UZS_CONTEXT_GENERAL',
    currency=Currencies.UZS,
    paysys=Paysyses.BANK_UZB_UR_UZS,
)
