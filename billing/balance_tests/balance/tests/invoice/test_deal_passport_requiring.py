# coding: utf-8

from decimal import Decimal as D
from xmlrpclib import Fault

import pytest
from hamcrest import equal_to

import btestlib.constants as const
import btestlib.utils as utils
from balance import balance_steps as steps
from balance.features import Features, AuditFeatures
from btestlib import reporter
from btestlib.data.defaults import Date
from temp.igogor.balance_objects import Contexts

WEEK_AGO = utils.Date.shift_date(Date.NOW(), days=-7)

UR_CTX = Contexts.DIRECT_FISH_RUB_CONTEXT
YT_CTX = Contexts.DIRECT_FISH_YT_RUB_CONTEXT

# лимит 5 999 000 rub (DEAL_PASSPORT_OFERT_LIMIT в t_config)
DEAL_PASSPORT_OFERT_LIMIT = D('5999000')
LESS_THAN_DEAL_PASSPORT_OFERT_LIMIT = DEAL_PASSPORT_OFERT_LIMIT - D('1')
MORE_THAN_DEAL_PASSPORT_OFERT_LIMIT = DEAL_PASSPORT_OFERT_LIMIT + D('1')

pytestmark = [reporter.feature(Features.DEAL_PASSPORT, Features.PAYSTEP, Features.INVOICE),
              pytest.mark.tickets('BALANCE-24338')]


@pytest.mark.parametrize('data',
                         [
                             pytest.mark.smoke(utils.aDict(
                                 ctx=UR_CTX,
                                 inn_region='99',
                                 invoice_sum_rub=MORE_THAN_DEAL_PASSPORT_OFERT_LIMIT,
                                 invoice_allowed=False
                             )),
                             pytest.mark.smoke(utils.aDict(
                                 ctx=UR_CTX,
                                 inn_region='99',
                                 invoice_sum_rub=LESS_THAN_DEAL_PASSPORT_OFERT_LIMIT,
                                 invoice_allowed=True
                             )),
                             utils.aDict(
                                 ctx=YT_CTX,
                                 invoice_sum_rub=MORE_THAN_DEAL_PASSPORT_OFERT_LIMIT,
                                 invoice_allowed=False
                             ),
                             utils.aDict(
                                 ctx=YT_CTX,
                                 invoice_sum_rub=LESS_THAN_DEAL_PASSPORT_OFERT_LIMIT,
                                 invoice_allowed=True
                             ),
                             # курс применяется на дату счета
                             utils.aDict(
                                 ctx=UR_CTX,
                                 inn_region='99',
                                 invoice_sum_rub=MORE_THAN_DEAL_PASSPORT_OFERT_LIMIT,
                                 invoice_date=WEEK_AGO,
                                 invoice_allowed=False
                             ),
                             utils.aDict(
                                 ctx=UR_CTX,
                                 inn_region='99',
                                 invoice_sum_rub=LESS_THAN_DEAL_PASSPORT_OFERT_LIMIT,
                                 invoice_date=WEEK_AGO,
                                 invoice_allowed=True
                             ),
                             # с другим ИНН правила не применяются
                             utils.aDict(
                                 ctx=UR_CTX,
                                 inn_region='78',
                                 invoice_sum_rub=MORE_THAN_DEAL_PASSPORT_OFERT_LIMIT,
                                 invoice_allowed=True
                             ),
                         ])
def test_deal_passport_wo_contract(data):
    client_id = steps.ClientSteps.create()

    if data.ctx.person_type == const.PersonTypes.UR:
        person_params = {'inn': utils.InnGenerator.generate_inn_rus_ur_10(data.inn_region)}
    else:
        person_params = None
        steps.CommonSteps.set_extprops('Client', client_id, 'can_issue_initial_invoice', {'value_num': 1})

    person_id = steps.PersonSteps.create(client_id, data.ctx.person_type.code, person_params)

    invoice_date = data.get('invoice_date', Date.TODAY)

    qty = data.invoice_sum_rub / data.ctx.price

    reporter.attach('Суммы',
                    'invoice_sum_rub={}, price={}, qty={}'.format(
                        data.invoice_sum_rub, data.ctx.price, qty))

    service_order_id = steps.OrderSteps.next_id(service_id=data.ctx.service.id)
    steps.OrderSteps.create(client_id=client_id, product_id=data.ctx.product.id, service_id=data.ctx.service.id,
                            service_order_id=service_order_id)

    orders_list = [{'ServiceID': data.ctx.service.id, 'ServiceOrderID': service_order_id, 'Qty': qty}]
    request_id = steps.RequestSteps.create(client_id=client_id, orders_list=orders_list,
                                           additional_params={'InvoiceDesireDT': invoice_date})

    if data.invoice_allowed:
        steps.InvoiceSteps.create(request_id, person_id, data.ctx.paysys.id)
    else:
        check_invoice_is_not_allowed(request_id, person_id, data.ctx.paysys.id, exc_code=u'DEAL_PASSPORT_REQUIRED')


@reporter.feature(Features.CONTRACT)
@pytest.mark.parametrize('payment_type', [
    pytest.mark.audit(reporter.feature(AuditFeatures.TR_C19)(
        const.ContractPaymentType.POSTPAY
    )),
    pytest.mark.smoke_exclude(const.ContractPaymentType.PREPAY)
],
                         ids=lambda payment_type: const.ContractPaymentType.name(payment_type).lower())
@pytest.mark.parametrize('data',
                         [
                             # нет поля Заявка на ПС, ИНН 99 или YT
                             pytest.mark.smoke(
                                 utils.aDict(is_agency=True,
                                             ctx=UR_CTX,
                                             inn_region='99',
                                             deal_passport=False,
                                             invoice_sum_rub=D('1'),
                                             invoice_allowed=False
                                             )),
                             utils.aDict(is_agency=False,
                                         ctx=UR_CTX,
                                         inn_region='99',
                                         deal_passport=False,
                                         invoice_sum_rub=D('1'),
                                         invoice_allowed=False
                                         ),
                             pytest.mark.smoke(
                                 utils.aDict(is_agency=True,
                                             ctx=YT_CTX,
                                             deal_passport=False,
                                             invoice_sum_rub=D('1'),
                                             invoice_allowed=False
                                             )),
                             utils.aDict(is_agency=False,
                                         ctx=YT_CTX,
                                         deal_passport=False,
                                         invoice_sum_rub=D('1'),
                                         invoice_allowed=False
                                         ),
                             # есть поле Заявка на ПС
                             utils.aDict(is_agency=True,
                                         ctx=UR_CTX,
                                         inn_region='99',
                                         deal_passport=True,
                                         invoice_sum_rub=MORE_THAN_DEAL_PASSPORT_OFERT_LIMIT,
                                         invoice_allowed=True
                                         ),
                             pytest.mark.smoke(
                                 utils.aDict(is_agency=True,
                                             ctx=YT_CTX,
                                             deal_passport=True,
                                             invoice_sum_rub=MORE_THAN_DEAL_PASSPORT_OFERT_LIMIT,
                                             invoice_allowed=True
                                             )),
                             #  нет поля Заявка на ПС, ИНН != 99
                             utils.aDict(is_agency=True,
                                         ctx=UR_CTX,
                                         inn_region='78',
                                         deal_passport=False,
                                         invoice_sum_rub=MORE_THAN_DEAL_PASSPORT_OFERT_LIMIT,
                                         invoice_allowed=True
                                         ),
                         ])
def test_deal_passport_contract(payment_type, data):
    agency_id = steps.ClientSteps.create_agency() if data.is_agency else None
    client_id = steps.ClientSteps.create()

    order_owner = client_id
    invoice_owner = agency_id or client_id

    person_params = {'inn': utils.InnGenerator.generate_inn_rus_ur_10(data.inn_region)} \
        if data.ctx.person_type == const.PersonTypes.UR else None

    person_id = steps.PersonSteps.create(invoice_owner, data.ctx.person_type.code, person_params)

    # usd_rate = D(api.medium().GetCurrencyRate(const.Currencies.USD.char_code, Date.TODAY)[2]['rate'])
    # invoice_sum_rub = data.invoice_sum_usd * usd_rate

    contract_type = const.ContractCommissionType.PR_AGENCY if data.is_agency else \
        const.ContractCommissionType.OPT_CLIENT

    contract_params = {
        'CLIENT_ID': invoice_owner,
        'PERSON_ID': person_id,
        'CURRENCY': const.Currencies.RUB.num_code,
        'PAYMENT_TYPE': payment_type,
        'SERVICES': [data.ctx.service.id],
        'DT': Date.TODAY_ISO,
        'FINISH_DT': Date.YEAR_AFTER_TODAY_ISO,
        'IS_SIGNED': Date.TODAY_ISO,
    }
    if payment_type == const.ContractPaymentType.POSTPAY:
        contract_params.update({
            'CREDIT_TYPE': const.ContractCreditType.BY_TERM_AND_SUM,
            'CREDIT_LIMIT_SINGLE': utils.roundup(data.invoice_sum_rub, -3)
        })

    if data.deal_passport:
        contract_params.update({'DEAL_PASSPORT': Date.TODAY_ISO})

    contract_id, _ = steps.ContractSteps.create_contract_new(contract_type, contract_params)

    service_order_id = steps.OrderSteps.next_id(service_id=data.ctx.service.id)
    steps.OrderSteps.create(client_id=order_owner, product_id=data.ctx.product.id, service_id=data.ctx.service.id,
                            service_order_id=service_order_id, params={'AgencyID': agency_id})
    qty = data.invoice_sum_rub / data.ctx.price
    orders_list = [{'ServiceID': data.ctx.service.id, 'ServiceOrderID': service_order_id, 'Qty': qty}]
    request_id = steps.RequestSteps.create(client_id=invoice_owner, orders_list=orders_list)

    is_credit = 1 if payment_type == const.ContractPaymentType.POSTPAY else 0
    if data.invoice_allowed:
        steps.InvoiceSteps.create(request_id, person_id, data.ctx.paysys.id,
                                  credit=is_credit, contract_id=contract_id)
    else:
        # код DEAL_PASSPORT_REQUIRED убрали из ошибки - BALANCE-26720

        # проверяем, что нельзя выставиться по договору
        check_invoice_is_not_allowed(request_id, person_id, data.ctx.paysys.id,
                                     credit=is_credit, contract_id=contract_id,
                                     exc_code=u'INCOMPATIBLE_INVOICE_PARAMS')
        if data.invoice_sum_rub > DEAL_PASSPORT_OFERT_LIMIT or data.is_agency or not is_credit:

            # проверяем, что нельзя выставиться по оферте
            check_invoice_is_not_allowed(request_id, person_id, data.ctx.paysys.id,
                                         exc_code=u'INCOMPATIBLE_INVOICE_PARAMS')
        else:
            steps.InvoiceSteps.create(request_id, person_id, data.ctx.paysys.id,
                                      credit=0, contract_id=None)


def check_invoice_is_not_allowed(request_id, person_id, paysys_id, exc_code, credit=0, contract_id=None):
    with pytest.raises(Fault) as exc:
        steps.InvoiceSteps.create(request_id, person_id, paysys_id, credit=credit, contract_id=contract_id)
    utils.check_that(steps.CommonSteps.get_exception_code(exc.value), equal_to(exc_code))
