# -*- coding: utf-8 -*-

__author__ = 'atkaya'

from decimal import Decimal

import pytest
from dateutil.relativedelta import relativedelta
from hamcrest import empty

import btestlib.reporter as reporter
from balance import balance_steps as steps
from balance.features import Features, AuditFeatures
from btestlib import utils
from btestlib.constants import Services, TransactionType, NdsNew, SpendablePaymentType, Pages, PaymentType, Currencies
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



import datetime
MONTH_START = datetime.datetime(2022, 1, 1)
MONTH_END = datetime.datetime(2022, 1, 31)

from collections import namedtuple
class Regions(utils.ConstantsContainer):
    Region = namedtuple('Region', 'id,name,currency,rate_scr_id')
    constant_type = Region

    ZAM = Region(id=21196, name=u'Замбия', currency=None, rate_scr_id=None)
    ANG = Region(id=21182, name=u'Ангола', currency=None, rate_scr_id=None)

ZAMB_GEN = TAXI_ZA_USD_CONTEXT.new(
    name=u'ZAMBIA_COMMISSION',
    special_contract_params={'personal_account': 1, 'country': Regions.ZAM.id, },
    region=Regions.ZAM,
    contract_services=[Services.TAXI_111.id, Services.TAXI_128.id, ],
)
ANGOLA_GEN = TAXI_MLU_EUROPE_CAMEROON_EUR_CONTEXT.new(
    name=u'ANGOLA_COMMISSION',
    special_contract_params={'personal_account': 1, 'country': Regions.ANG.id},
    region=Regions.ANG,
)

ZAMB_SPEND = ZAMB_GEN.new(
    # common
    name='ZAMBIA_SPENDABLE',
    service=Services.TAXI_DONATE,
    contract_type=ContractSubtype.SPENDABLE,
    special_contract_params={'country': Regions.ZAM.id, 'partner_commission_pct2': Decimal('10.2')},
    contract_services=[Services.TAXI_DONATE.id],
    # thirdparty
    payment_currency=Currencies.USD,
    tpt_paysys_type_cc=PaysysType.TAXI,
    # partner_act_data
    pad_type_id=6,
)


ANGOLA_SPEND = ANGOLA_GEN.new(
    # common
    name='ANGOLA_SPENDABLE',
    service=Services.TAXI_DONATE,
    contract_type=ContractSubtype.SPENDABLE,
    special_contract_params={'country': Regions.ANG.id},
    contract_services=[Services.TAXI_DONATE.id],
    # thirdparty
    tpt_paysys_type_cc=PaysysType.TAXI,
    payment_currency=Currencies.EUR,
    # partner_act_data
    pad_type_id=6,
)

@pytest.mark.parametrize('context_general, context_spendable, nds', [
    (ZAMB_GEN, ZAMB_SPEND, NdsNew.ZERO),
    (ANGOLA_GEN, ANGOLA_SPEND, NdsNew.ZERO),
], ids=lambda g, s, nds: s.name + '_NDS id = {}'.format(nds.nds_id))
def test_taxi_donate_acts(context_general, context_spendable, nds):
    client_id, spendable_person_id, spendable_contract_id, spendable_contract_eid, \
    gen_person_id, gen_contract_id, gen_contract_eid = create_client_and_contract(context_general, context_spendable, nds)

    pa_id, pa_eid, service_code = \
        steps.InvoiceSteps.get_invoice_by_service_or_service_code(gen_contract_id, service_code='YANDEX_SERVICE')

    client_log = steps.ExportSteps.get_oebs_api_response('Client', client_id)
    spendable_person_log = steps.ExportSteps.get_oebs_api_response('Person', spendable_person_id)
    spendable_contract_log = steps.ExportSteps.get_oebs_api_response('Contract', spendable_contract_id)
    pa_log = steps.ExportSteps.get_oebs_api_response('Invoice', pa_id)
    gen_person_log = steps.ExportSteps.get_oebs_api_response('Person', gen_person_id)
    gen_contract_log = steps.ExportSteps.get_oebs_api_response('Contract', gen_contract_id)

    report_dir = u'/Users/sfreest/Documents/reports'
    report = [
        u'{region_name} {currency}'.format(region_name=context_spendable.region.name,
                                           currency=context_spendable.currency.iso_code),
        u'Client: {client_id}, {log}'.format(client_id=client_id, log=client_log[0]),
        u'Person general: {person_id}, {log}'.format(person_id=gen_person_id, log=gen_person_log[0]),
        u'Contract general: {contract_eid}, {log}'.format(contract_eid=gen_contract_eid, log=gen_contract_log[0]),
        u'Invoice: {pa_eid}, {log}'.format(pa_eid=pa_eid, log=pa_log[0]),
        u'Person spendable: {person_id}, {log}'.format(person_id=spendable_person_id, log=spendable_person_log[0]),
        u'Contract spendable: {contract_eid}, {log}'.format(contract_eid=spendable_contract_eid,
                                                            log=spendable_contract_log[0]),
        u'\n',
    ]
    report = [r.encode('utf8') for r in report]
    with open(u'{report_dir}/spendable-{reg}-{cur}.txt'.format(report_dir=report_dir,
                                                               reg=context_spendable.region.name,
                                                                cur=context_spendable.currency.iso_code).encode('utf8'),
              'w') as output_file:
        output_file.write(u'\n'.encode('utf8').join(report))

# ------------------------------------------------------------
# Utils
def create_client_and_contract(context_general, context_spendable, nds, start_dt=FIRST_MONTH,
                               payment_type=SpendablePaymentType.MONTHLY,
                               is_spendable_unsigned=False, add_scouts=False):

    additional_params = {'start_dt': start_dt}
    client_id, person_id, contract_id, contract_eid = steps.ContractSteps.create_partner_contract(context_general,
                                                                               additional_params=additional_params)
    additional_params.update({'nds': nds.nds_id, 'payment_type': payment_type, 'link_contract_id': contract_id})
    if add_scouts:
        additional_params.update({'services': [Services.TAXI_DONATE.id, Services.SCOUTS.id]})

    _, spendable_person_id, spendable_contract_id, spendable_contract_eid = steps.ContractSteps.create_partner_contract(context_spendable,
                                                                                                   client_id=client_id,
                                                                                                   unsigned=is_spendable_unsigned,
                                                                                                   additional_params=additional_params)

    return client_id, spendable_person_id, spendable_contract_id, spendable_contract_eid, person_id, contract_id, contract_eid



