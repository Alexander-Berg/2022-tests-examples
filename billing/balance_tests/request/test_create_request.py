import pytest
import datetime
import hamcrest as hm

from balance.exc import CONTRACT_NOT_FOUND
from balance.mapper import Request, RequestOrder
from balance.constants import RegionId, ServiceId, POSTPAY_PAYMENT_TYPE, ContractTypeId
from balance import exc
from butils.decimal_unit import DecimalUnit as DU

from tests.balance_tests.request.request_common import (
    create_order,
    create_manager,
    create_client,
    create_currency,
    create_firm,
    create_service,
    logic,
    create_agency,
    create_contract, NOW, create_person_category, create_person, create_pay_policy, create_paysys,
    create_price_tax_rate, BANK,
)


@pytest.mark.parametrize('props', [None, {'key': 'value'}])
def test_request_default_params(session, client, props, logic):
    order = create_order(session, client)
    res = logic._CreateRequest(session=session, operator_uid=-1, client_id=order.client.id,
                               orders=[{'ServiceID': order.service.id,
                                        'ServiceOrderID': order.service_order_id,
                                        'Qty': 1}],
                               props=props)

    request = session.query(Request).getone(res['RequestID'])
    assert request.promo_code is None
    assert request.force_unmoderated == 0
    assert request.adjust_qty == 0
    assert request.patch_amount_to_qty == 0
    assert request.desired_invoice_dt is None
    assert request.invoice_desired_type is None
    assert request.deny_promocode == 0
    assert request.service_promocode is None
    assert request.firm_id is None
    assert request.turn_on_rows is False
    assert request.agency_discount_pct == DU('0', '%')


def test_params_from_props(session, order, logic):
    invoice_desire_dt = datetime.datetime.now()
    res = logic._CreateRequest(session=session, operator_uid=-1, client_id=order.client.id,
                               orders=[{'ServiceID': order.service.id,
                                        'ServiceOrderID': order.service_order_id,
                                        'Qty': 1}],
                               props={'AdjustQty': 1,
                                      'QtyIsAmount': 1,
                                      'InvoiceDesireDT': invoice_desire_dt,
                                      'InvoiceDesireType': 'charge_note',
                                      'DenyPromocode': 1,
                                      'ServicePromocode': 1,
                                      'FirmID': 1,
                                      'TurnOnRows': True,
                                      'ForceUnmoderated': 1
                                      })

    request = session.query(Request).getone(res['RequestID'])
    assert request.adjust_qty == 1
    assert request.patch_amount_to_qty == 1
    assert request.desired_invoice_dt == invoice_desire_dt
    assert request.invoice_desired_type == 'charge_note'
    assert request.deny_promocode == 1
    assert request.service_promocode == 1
    assert request.firm_id == 1
    assert request.turn_on_rows is True
    assert request.force_unmoderated == 1


def test_request_orders_default_params_check(session, order, logic):
    res = logic._CreateRequest(session=session, operator_uid=-1, client_id=order.client.id,
                               orders=[{'ServiceID': order.service.id,
                                        'ServiceOrderID': order.service_order_id,
                                        'Qty': 1}],
                               props={})

    request_orders = session.query(RequestOrder).filter(RequestOrder.request_id == res['RequestID']).all()
    assert len(request_orders) == 1

    assert request_orders[0].rur_price == 0
    assert request_orders[0].order_sum == 0
    assert request_orders[0].seqnum == 0
    assert request_orders[0].product.id == order.service_code
    assert request_orders[0].quantity == DU('1', 'QTY')
    assert request_orders[0].order == order
    assert request_orders[0].client == order.client
    assert request_orders[0].begin_dt is None
    assert request_orders[0].u_discount_pct is None
    assert request_orders[0].discount_pct == DU('0', '%')
    assert request_orders[0].dynamic_discount_pct is None
    assert request_orders[0].user_data is None
    assert request_orders[0].text == order.product.name
    assert request_orders[0].act_row is None
    assert request_orders[0]._region_id is None
    assert request_orders[0].markups == set([])


def test_request_orders_agency(session, agency, logic):
    order = create_order(session, agency=agency, client=create_client(session))
    order_2 = create_order(session, agency=agency, client=create_client(session))
    res = logic._CreateRequest(session=session, operator_uid=-1, client_id=agency.id,
                               orders=[{'ServiceID': order.service.id,
                                        'ServiceOrderID': order.service_order_id,
                                        'Qty': 1,
                                        },
                                       {'ServiceID': order_2.service.id,
                                        'ServiceOrderID': order_2.service_order_id,
                                        'Qty': 1,
                                        }
                                       ],
                               props={})

    request_orders = session.query(RequestOrder).filter(RequestOrder.request_id == res['RequestID']).all()
    assert len(request_orders) == 2
    assert request_orders[0].request.client == agency
    hm.assert_that(
        request_orders,
        hm.contains_inanyorder(*[
            hm.has_properties('client', order_.client,
                              'order', order_)
            for order_ in [order, order_2]
        ]),
    )


def test_request_orders_non_default_params_check(session, order, logic):
    begin_dt = datetime.datetime.now()
    discount_pct = 5
    res = logic._CreateRequest(session=session, operator_uid=-1, client_id=order.client.id,
                               orders=[{'ServiceID': order.service.id,
                                        'ServiceOrderID': order.service_order_id,
                                        'Qty': 1,
                                        'BeginDT': begin_dt,
                                        'Discount': discount_pct,
                                        'Markups': [1, 2]}],
                               props={})

    request_orders = session.query(RequestOrder).filter(RequestOrder.request_id == res['RequestID']).all()
    assert len(request_orders) == 1
    assert request_orders[0].begin_dt == begin_dt
    assert request_orders[0].u_discount_pct == discount_pct
    assert request_orders[0].markups == {1, 2}


def test_request_orders_region_check(session, order, logic):
    order.region_id = RegionId.RUSSIA
    res = logic._CreateRequest(session=session, operator_uid=-1, client_id=order.client.id,
                               orders=[{'ServiceID': order.service.id,
                                        'ServiceOrderID': order.service_order_id,
                                        'Qty': 1}],
                               props={})

    request_orders = session.query(RequestOrder).filter(RequestOrder.request_id == res['RequestID']).all()
    assert len(request_orders) == 1
    assert request_orders[0]._region_id == order.region_id
    assert request_orders[0].region_id == order.region_id


@pytest.mark.moderation
def test_request_orders_unmoderated(session, client, logic):
    moderated_order = create_order(session, client=client)
    unmoderated_order = create_order(session, client=client)
    unmoderated_order.unmoderated = 1
    with pytest.raises(exc.INVALID_PARAM) as exc_info:
        logic._CreateRequest(session=session, operator_uid=-1, client_id=client.id,
                             orders=[{'ServiceID': moderated_order.service.id,
                                      'ServiceOrderID': moderated_order.service_order_id,
                                      'Qty': 1},
                                     {'ServiceID': unmoderated_order.service.id,
                                      'ServiceOrderID': unmoderated_order.service_order_id,
                                      'Qty': 1}
                                     ],
                             props={})
    assert exc_info.value.msg == 'Invalid parameter for function: Invalid unmoderated request'


@pytest.mark.moderation
def test_request_orders_force_moderated(session, client, logic):
    moderated_order = create_order(session, client=client)
    unmoderated_order = create_order(session, client=client)
    unmoderated_order.unmoderated = 1
    res = logic._CreateRequest(session=session, operator_uid=-1, client_id=client.id,
                               orders=[{'ServiceID': moderated_order.service.id,
                                        'ServiceOrderID': moderated_order.service_order_id,
                                        'Qty': 1},
                                       {'ServiceID': unmoderated_order.service.id,
                                        'ServiceOrderID': unmoderated_order.service_order_id,
                                        'Qty': 1}
                                       ],
                               props={'ForceUnmoderated': 1})
    request = session.query(Request).getone(res['RequestID'])
    assert request.is_unmoderated == 0


@pytest.mark.moderation
def test_request_is_valid_unmoderated(session, client, logic):
    unmoderated_order = create_order(session, client=client, service_id=ServiceId.DIRECT)
    unmoderated_order.unmoderated = 1
    unmoderated_order.manager = None
    res = logic._CreateRequest(session=session, operator_uid=-1, client_id=client.id,
                               orders=[{'ServiceID': unmoderated_order.service.id,
                                        'ServiceOrderID': unmoderated_order.service_order_id,
                                        'Qty': 1}],
                               props={})
    request = session.query(Request).getone(res['RequestID'])
    assert request.is_valid_unmoderated_request == 1


@pytest.mark.moderation
def test_request_is_valid_unmoderated_manager(session, client, logic):
    unmoderated_order = create_order(session, client=client)
    unmoderated_order.unmoderated = 1
    unmoderated_order.manager = create_manager(session)
    with pytest.raises(exc.INVALID_PARAM) as exc_info:
        logic._CreateRequest(session=session, operator_uid=-1, client_id=client.id,
                             orders=[{'ServiceID': unmoderated_order.service.id,
                                      'ServiceOrderID': unmoderated_order.service_order_id,
                                      'Qty': 1}],
                             props={})
    assert exc_info.value.msg == 'Invalid parameter for function: Invalid unmoderated request'


@pytest.mark.parametrize(
    "contract_id", [-1, None, 1],
)
def test_request_desired_contract_id(session, client, logic, currency, firm, service, contract_id):
    person_category = create_person_category(session, country=firm.country, ur=0, resident=1)
    person = create_person(session, client=client, person_category=person_category)

    if contract_id not in (-1, None):
        contract = create_contract(session, is_signed=NOW, dt=NOW, currency=currency.num_code,
                                   payment_type=POSTPAY_PAYMENT_TYPE, services={service.id},
                                   person=person, client=client, ctype='GENERAL', commission=ContractTypeId.NON_AGENCY,
                                   personal_account=1, firm=firm.id)
    else:
        contract = None

    order = create_order(session, client=client, service_id=service.id)

    create_pay_policy(session, firm_id=firm.id, legal_entity=0, paymethods_params=[(currency.char_code, BANK)],
                      region_id=firm.country.region_id, service_id=order.service.id,
                      is_agency=0)
    create_paysys(session, firm=firm, iso_currency=currency.iso_code,
                  currency=currency.char_code, category=person_category.category,
                  group_id=0, payment_method_id=BANK, extern=1, cc='paysys_cc')
    create_price_tax_rate(session, order.product, firm.country, currency, price=1)

    req_c_id = contract.id if contract else contract_id

    res = logic.CreateRequest2(session.oper_id, client.id,
                               [{'ServiceID': order.service.id,
                                 'ServiceOrderID': order.service_order_id,
                                 'Qty': 1}],
                               {'InvoiceDesireContractID': req_c_id,
                                'FirmID': firm.id})
    request = session.query(Request).getone(res['RequestID'])
    assert request.invoice_desired_contract_id == req_c_id


@pytest.mark.parametrize(
    "with_contract", [True, False],
)
def test_raises_if_no_payment_methods_for_desired_contract(session, client, logic, currency, firm, service, with_contract):
    person_category = create_person_category(session, country=firm.country, ur=0, resident=1)
    person = create_person(session, client=client, person_category=person_category)

    if with_contract:
        contract = create_contract(session, is_signed=NOW, dt=NOW, currency=currency.num_code,
                                   payment_type=POSTPAY_PAYMENT_TYPE, services={service.id},
                                   person=person, client=client, ctype='GENERAL', commission=ContractTypeId.NON_AGENCY,
                                   personal_account=1, firm=firm.id)
        req_c_id = contract.id
    else:
        req_c_id = -1

    order = create_order(session, client=client, service_id=service.id)

    with pytest.raises(exc.PAYSTEP_INVALID_PAYMENT_CHOICE):
        logic._CreateRequest(session=session, operator_uid=-1, client_id=client.id,
                             orders=[{'ServiceID': order.service.id,
                                      'ServiceOrderID': order.service_order_id,
                                      'Qty': 1}],
                             props={'InvoiceDesireContractID': req_c_id,
                                    'FirmID': firm.id})
