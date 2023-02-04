# -*- coding: utf-8 -*-
import datetime
import pytest
import hamcrest

from balance import balance_steps as steps
from btestlib.constants import Services, Products, Firms, PersonTypes, ContractCommissionType, Currencies, \
    ContractPaymentType, Users
from dateutil.relativedelta import relativedelta
from btestlib import utils, reporter
from balance import balance_api as api
from balance.features import Features

pytestmark = [reporter.feature(Features.CONTRACT)]


@pytest.mark.tickets('BALANCE-29986')
def test_get_request_choices_active_and_inactive_contract():
    client_id = steps.ClientSteps.create()
    person_id = steps.PersonSteps.create(client_id, PersonTypes.UR.code)

    dt = datetime.datetime.now()

    contract_external_id = '29986/' + datetime.datetime.now().strftime('%Y%m%d%H%M')

    with reporter.step(u'Создаем истекший договор с contract_external_id: {}'.format(contract_external_id)):
        inactive_contract_id, _ = steps.ContractSteps.create_contract_new(ContractCommissionType.OPT_AGENCY,
                                                                          {'CLIENT_ID': client_id,
                                                                           'PERSON_ID': person_id,
                                                                           'SERVICES': [Services.SHOP.id,
                                                                                        Services.DIRECT.id],
                                                                           'FIRM': Firms.YANDEX_1.id,
                                                                           'CURRENCY': Currencies.RUB.num_code,
                                                                           'PAYMENT_TYPE': ContractPaymentType.PREPAY,
                                                                           'DT': dt - relativedelta(months=15),
                                                                           'FINISH_DT': dt - relativedelta(days=2),
                                                                           'IS_SIGNED': utils.Date.to_iso(
                                                                               utils.Date.nullify_time_of_date(
                                                                                   dt - relativedelta(months=15))),
                                                                           'EXTERNAL_ID': contract_external_id
                                                                           })

    with reporter.step(u'Создаем действующий договор с contract_external_id: {}'.format(contract_external_id)):
        active_contract_id, _ = steps.ContractSteps.create_contract_new(ContractCommissionType.OPT_AGENCY,
                                                                        {'CLIENT_ID': client_id,
                                                                         'PERSON_ID': person_id,
                                                                         'SERVICES': [Services.SHOP.id,
                                                                                      Services.DIRECT.id],
                                                                         'FIRM': Firms.YANDEX_1.id,
                                                                         'CURRENCY': Currencies.RUB.num_code,
                                                                         'PAYMENT_TYPE': ContractPaymentType.PREPAY,
                                                                         'DT': dt - relativedelta(days=1),
                                                                         'FINISH_DT': dt + relativedelta(
                                                                             months=12),
                                                                         'IS_SIGNED': utils.Date.to_iso(
                                                                             utils.Date.nullify_time_of_date(
                                                                                 dt - relativedelta(days=1))),
                                                                         'EXTERNAL_ID': contract_external_id
                                                                         })

    with reporter.step(u'Создаем реквест'):
        service_id = Services.DIRECT.id
        product_id = Products.DIRECT_FISH.id
        now = datetime.datetime.now()
        service_order_id = steps.OrderSteps.next_id(service_id=service_id)  # внешний ID заказа
        steps.OrderSteps.create(client_id, service_order_id, service_id=service_id,
                                product_id=product_id, params={'AgencyID': None})
        orders_list = [{'ServiceID': service_id, 'ServiceOrderID': service_order_id, 'Qty': 100, 'BeginDT': now}]
        request_id = steps.RequestSteps.create(client_id, orders_list, additional_params=dict(InvoiceDesireDT=now))

    with reporter.step(u'Зовем GetRequestChoices и передаем ему contract_external_id'):
        overdraft_params = api.medium().GetRequestChoices({'OperatorUid': Users.YB_ADM.uid,
                                                           'RequestID': request_id,
                                                           'PersonID': person_id,
                                                           'ContractExternalID': contract_external_id})
    utils.check_that(overdraft_params['pcp_list'][0]['contract']['id'],
                     hamcrest.equal_to(active_contract_id),
                     step=u"Проверяем, что платить можно по действующему договору")
