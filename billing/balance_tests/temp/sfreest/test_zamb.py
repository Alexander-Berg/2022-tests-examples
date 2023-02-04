# -*- coding: utf-8 -*-

from datetime import datetime
from decimal import Decimal

import pytest
from dateutil.relativedelta import relativedelta
from hamcrest import empty

import btestlib.reporter as reporter
from balance.balance_steps import new_taxi_steps as steps
from balance.balance_steps.acts_steps import ActsSteps
from balance.balance_steps.invoice_steps import InvoiceSteps
from balance.balance_steps.export_steps import ExportSteps

from balance.balance_steps.consume_steps import ConsumeSteps
from balance.balance_steps.contract_steps import ContractSteps
from balance.balance_steps.common_data_steps import CommonData
from balance.balance_steps.partner_steps import CommonPartnerSteps
from balance.balance_steps.order_steps import OrderSteps
from balance.balance_steps.other_steps import SharedBlocks
from balance.features import Features, AuditFeatures
from balance.utils import get_config_item
from btestlib import shared
from btestlib import utils
from btestlib.constants import Currencies
from btestlib.constants import InvoiceType
from btestlib.constants import PaymentType
from btestlib.constants import Services
from btestlib.constants import TaxiOrderType
from btestlib.data.defaults import TaxiNewPromo as Taxi
from btestlib.matchers import close_to
from btestlib.matchers import contains_dicts_equal_to
from btestlib.matchers import contains_dicts_with_entries
from btestlib.matchers import equal_to
from balance.balance_steps.new_taxi_steps import DEFAULT_TAXI_CONTEXTS, DEFAULT_PARAMETRIZATION, \
    DEFAULT_TAXI_CONTEXTS_WITH_MARKS
from btestlib.data.partner_contexts import TAXI_RU_CONTEXT, TAXI_YANDEX_GO_SRL_CONTEXT

pytestmark = [
    reporter.feature(Features.TAXI)
]

CONTRACT_START_DT = utils.Date.first_day_of_month(datetime.now() - relativedelta(months=1))
ACT_DT = utils.Date.get_last_day_of_previous_month()
COMPLETION_DT = utils.Date.first_day_of_month(ACT_DT)

DEFAULT_SERVICES = [Services.TAXI_111.id,
                    Services.TAXI_128.id,
                    Services.TAXI.id,
                    Services.UBER.id,
                    Services.UBER_ROAMING.id,
                    Services.TAXI_SVO.id]

DEFAULT_SERVICES_WO_CASH = [
    Services.TAXI_128.id,
    Services.TAXI.id,
    Services.UBER.id,
    Services.UBER_ROAMING.id
]

expected_conusme_amount_fields = ['act_qty', 'act_sum', 'completion_qty', 'completion_sum', 'current_qty']

# Общий кейс:
# Оплата на ЛС
# Открутки в первом месяце и втором месяце
# Генерация актов за первый месяц
# Открутки во втором месяце, дополнительные в первом месяце
# Генерация актов за второй месяц

import datetime
MONTH_START = datetime.datetime(2022, 1, 1)
MONTH_END = datetime.datetime(2022, 1, 31)

from collections import namedtuple
class Regions(utils.ConstantsContainer):
    Region = namedtuple('Region', 'id,name,currency,rate_scr_id')
    constant_type = Region

    ZAM = Region(id=21196, name=u'Замбия', currency=None, rate_scr_id=None)
    ANG = Region(id=21182, name=u'Ангола', currency=None, rate_scr_id=None)

import btestlib.data.partner_contexts as pc


@pytest.mark.parametrize('context',
    [
        pc.TAXI_ZA_USD_CONTEXT.new(
            name=u'ZAMBIA_COMMISSION',
            special_contract_params={'personal_account': 1, 'country': Regions.ZAM.id, },
            region=Regions.ZAM,
            contract_services=[Services.TAXI_111.id, Services.TAXI_128.id, ],
        ),
        pc.TAXI_MLU_EUROPE_CAMEROON_EUR_CONTEXT.new(
            name=u'ANGOLA_COMMISSION',
            special_contract_params={'personal_account': 1, 'country': Regions.ANG.id},
            region=Regions.ANG,
        ),
     ],
    ids=lambda c: c.name
)
def test_2_months_common_case(context):
    client_id, person_id, contract_id, contract_eid = ContractSteps.create_partner_contract(
        context, is_postpay=0, is_offer=1, additional_params={'start_dt': MONTH_START}
    )
    compls_data_3rd_month = steps.TaxiData.generate_default_oebs_compls_data(MONTH_START,
                                                                             context.currency.iso_code,
                                                                             MONTH_START)
    CommonPartnerSteps.create_partner_oebs_completions(contract_id, client_id, compls_data_3rd_month)
    CommonPartnerSteps.generate_partner_acts_fair_and_export(client_id, contract_id, MONTH_END)

    act_data = ActsSteps.get_all_act_data(client_id, dt=MONTH_END)[0]
    pa_id, pa_eid, service_code = \
        InvoiceSteps.get_invoice_by_service_or_service_code(contract_id, service_code='YANDEX_SERVICE')

    client_log = ExportSteps.get_oebs_api_response('Client', client_id)
    person_log = ExportSteps.get_oebs_api_response('Person', person_id)
    contract_log = ExportSteps.get_oebs_api_response('Contract', contract_id)
    pa_log = ExportSteps.get_oebs_api_response('Invoice', pa_id)
    act_log = ExportSteps.get_oebs_api_response('Act', act_data['id'])

    report_dir = u'/Users/sfreest/Documents/reports'
    report = [
        u'{region_name} {currency}'.format(region_name=context.region.name, currency=context.currency.iso_code),
        u'Client: {client_id}, {log}'.format(client_id=client_id, log=client_log[0]),
        u'Person: {person_id}, {log}'.format(person_id=person_id, log=person_log[0]),
        u'Contract: {contract_eid}, {log}'.format(contract_eid=contract_eid, log=contract_log[0]),
        u'Invoice: {pa_eid}, {log}'.format(pa_eid=pa_eid, log=pa_log[0]),
        u'Act: {act_eid}, {log}'.format(act_eid=act_data['external_id'], log=act_log[0]),
        u'\n',
    ]
    report = [r.encode('utf8') for r in report]
    with open(u'{report_dir}/comm-{reg}-{cur}.txt'.format(report_dir=report_dir,
                                                          reg=context.region.name,
                                                          cur=context.currency.iso_code).encode('utf8'),
              'w') as output_file:
        output_file.write(u'\n'.encode('utf8').join(report))


