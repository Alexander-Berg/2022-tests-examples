# -*- coding: utf-8 -*-

import datetime
import copy
import pytest
from hamcrest import has_entries

from balance import balance_db as db
from balance import balance_steps as steps
from btestlib import utils as utils
from temp.igogor.balance_objects import Contexts, Products, Firms, Paysyses, PersonTypes, Currencies, Regions, \
    ContractCommissionType, Services

QTY = 10

to_iso = utils.Date.date_to_iso_format
dt_delta = utils.Date.dt_delta

NOW = datetime.datetime.now()
NOW_ISO = to_iso(NOW)
HALF_YEAR_AFTER_NOW_ISO = to_iso(NOW + datetime.timedelta(days=180))
HALF_YEAR_BEFORE_NOW_ISO = to_iso(NOW - datetime.timedelta(days=180))

DIRECT_BEL_FIRM_BYN = Contexts.DIRECT_FISH_RUB_CONTEXT.new(service=Services.DIRECT, product=Products.DIRECT_BYN,
                                                           firm=Firms.REKLAMA_BEL_27, person_type=PersonTypes.BYU,
                                                           paysys=Paysyses.BANK_BY_UR_BYN, region=Regions.BY,
                                                           currency=Currencies.BYN,
                                                           contract_type=ContractCommissionType.BEL_OPT_AGENCY)

DIRECT_FISH_RUB_CONTEXT = Contexts.DIRECT_FISH_RUB_CONTEXT.new(contract_type=ContractCommissionType.OPT_AGENCY)

DEFAULT_CONTRACT_PARAMS = {'FINISH_DT': HALF_YEAR_AFTER_NOW_ISO,
                           'IS_SIGNED': HALF_YEAR_BEFORE_NOW_ISO,
                           'DT': HALF_YEAR_BEFORE_NOW_ISO,
                           'DEAL_PASSPORT': HALF_YEAR_BEFORE_NOW_ISO
                           }


def create_request(service_id, client_id, product_id, promocode_code=None, invoice_dt=None, agency_id=None, qty=None):
    qty = QTY
    service_order_id = steps.OrderSteps.next_id(service_id=service_id)
    steps.OrderSteps.create(client_id=client_id, product_id=product_id, service_id=service_id,
                            service_order_id=service_order_id, params={'AgencyID': agency_id})
    orders_list = [{'ServiceID': service_id, 'ServiceOrderID': service_order_id, 'Qty': qty, 'BeginDT': NOW}]
    request_id = steps.RequestSteps.create(client_id=agency_id if agency_id else client_id, orders_list=orders_list,
                                           additional_params={'PromoCode': promocode_code,
                                                              'InvoiceDesireDT': invoice_dt})
    return request_id, orders_list


def create_payed_invoice(request_id, person_id, paysys_id):
    invoice_id, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id, paysys_id=paysys_id,
                                                 credit=0, contract_id=None, overdraft=0, endbuyer_id=None)

    steps.InvoiceSteps.pay(invoice_id)
    steps.CampaignsSteps.do_campaigns(context.service.id, service_order_id, {context.product.type.code: qty}, 0, dt)
    act_id = steps.ActsSteps.generate(client_id, force=1, date=dt)[0]
    return invoice_id


@pytest.mark.parametrize('context', [
    DIRECT_BEL_FIRM_BYN,
    DIRECT_FISH_RUB_CONTEXT

])
def test_export_contract(context):
    agency_id = steps.ClientSteps.create({'IS_AGENCY': 1})
    steps.ClientSteps.create({'IS_AGENCY': 0})
    invoice_owner = agency_id
    person_id = steps.PersonSteps.create(invoice_owner, context.person_type.code)
    contract_params = copy.deepcopy(DEFAULT_CONTRACT_PARAMS)
    contract_params.update({'CLIENT_ID': invoice_owner, 'PERSON_ID': person_id})
    contract_id, _ = steps.ContractSteps.create_contract_new(context.contract_type, contract_params)
    if context == DIRECT_BEL_FIRM_BYN:
        contract_export_data = steps.ExportSteps.get_export_data(classname='Contract', object_id=contract_id,
                                                                 queue_type='BY_FILE')
        assert contract_export_data['state'] == '0'
        collateral_id = db.get_collaterals_by_contract(contract_id)[0]['id']
        collateral_export_data = steps.ExportSteps.get_export_data(classname='ContractCollateral',
                                                                   object_id=collateral_id, queue_type='BY_FILE')
        assert collateral_export_data['state'] == '0'
        # steps.CommonSteps.export('BY_FILE', classname='Contract', object_id=contract_id)
        steps.CommonSteps.export('BY_FILE', classname='ContractCollateral', object_id=collateral_id)
        client_export_data = steps.ExportSteps.get_export_data(classname='Client', object_id=agency_id,
                                                               queue_type='BY_FILE')
        assert client_export_data['state'] == '0'
        person_export_data = steps.ExportSteps.get_export_data(classname='Person', object_id=person_id,
                                                               queue_type='BY_FILE')
        assert person_export_data['state'] == '0'

        # steps.CommonSteps.export('BY_FILE', classname='Client', object_id=agency_id)
        # steps.CommonSteps.export('BY_FILE', classname='Person', object_id=person_id)
        # steps.CommonSteps.export('OEBS', classname='Contract', object_id=contract_id)
    else:
        contract_export_data = steps.ExportSteps.get_export_data(classname='Contract', object_id=contract_id,
                                                                 queue_type='BY_FILE')
        assert contract_export_data['state'] == '1'
        collateral_id = db.get_collaterals_by_contract(contract_id)[0]['id']
        collateral_export_data = steps.ExportSteps.get_export_data(classname='ContractCollateral',
                                                                   object_id=collateral_id, queue_type='BY_FILE')
        assert collateral_export_data['state'] == '1'
        steps.CommonSteps.export('BY_FILE', classname='Contract', object_id=contract_id)
        steps.CommonSteps.export('BY_FILE', classname='ContractCollateral', object_id=collateral_id)
        try:
            client_export_data = steps.ExportSteps.get_export_data(classname='Client', object_id=agency_id,
                                                                   queue_type='BY_FILE')
            assert client_export_data['state'] == '1'
            assert 1 == 0
        except Exception as exc:
            print exc
        try:
            person_export_data = steps.ExportSteps.get_export_data(classname='Person', object_id=person_id,
                                                                   queue_type='BY_FILE')
            assert person_export_data['state'] == '1'
            steps.CommonSteps.export('BY_FILE', classname='Client', object_id=agency_id)
            steps.CommonSteps.export('BY_FILE', classname='Person', object_id=person_id)
            assert 1 == 0
        except Exception as exc:
            print exc


@pytest.mark.parametrize('context', [
    DIRECT_BEL_FIRM_BYN,
    DIRECT_FISH_RUB_CONTEXT

])
def test_export_client(context):
    agency_id = steps.ClientSteps.create({'IS_AGENCY': 1})
    try:
        client_export_data = steps.ExportSteps.get_export_data(classname='Client', object_id=agency_id,
                                                               queue_type='BY_FILE')
        assert client_export_data['state'] == '0'
        assert 1 == 0
    except Exception as exc:
        print exc


@pytest.mark.parametrize('context', [
    DIRECT_BEL_FIRM_BYN,
    DIRECT_FISH_RUB_CONTEXT

])
def test_export_person(context):
    agency_id = steps.ClientSteps.create({'IS_AGENCY': 1})
    invoice_owner = agency_id
    person_id = steps.PersonSteps.create(invoice_owner, context.person_type.code)
    try:
        person_export_data = steps.ExportSteps.get_export_data(classname='Person', object_id=person_id,
                                                               queue_type='BY_FILE')
        assert person_export_data['state'] == '0'
        assert 1 == 0
    except Exception as exc:
        print exc


@pytest.mark.parametrize('context', [
    DIRECT_BEL_FIRM_BYN,
    DIRECT_FISH_RUB_CONTEXT

])
def test_export_invoice(context):
    agency_id = steps.ClientSteps.create({'IS_AGENCY': 1})
    steps.ClientSteps.create({'IS_AGENCY': 0})
    invoice_owner = agency_id
    person_id = steps.PersonSteps.create(invoice_owner, context.person_type.code)
    qty = QTY
    service_order_id = steps.OrderSteps.next_id(service_id=context.service.id)
    steps.OrderSteps.create(client_id=agency_id, product_id=context.product.id, service_id=context.service.id,
                            service_order_id=service_order_id, params={'AgencyID': agency_id})
    orders_list = [{'ServiceID': context.service.id, 'ServiceOrderID': service_order_id, 'Qty': qty, 'BeginDT': NOW}]
    request_id = steps.RequestSteps.create(client_id=agency_id if agency_id else agency_id, orders_list=orders_list,
                                           additional_params={'InvoiceDesireDT': NOW})

    invoice_id, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id,
                                                 paysys_id=context.paysys.id,
                                                 credit=0, contract_id=None, overdraft=0, endbuyer_id=None)

    steps.InvoiceSteps.pay(invoice_id)
    if context == DIRECT_BEL_FIRM_BYN:
        invoice_export_data = steps.ExportSteps.get_export_data(classname='Invoice', object_id=invoice_id,
                                                                queue_type='BY_FILE')
        assert invoice_export_data['state'] == '0'

        steps.CommonSteps.export('BY_FILE', classname='Invoice', object_id=invoice_id)
        client_export_data = steps.ExportSteps.get_export_data(classname='Client', object_id=agency_id,
                                                               queue_type='BY_FILE')
        assert client_export_data['state'] == '0'
        person_export_data = steps.ExportSteps.get_export_data(classname='Person', object_id=person_id,
                                                               queue_type='BY_FILE')
        assert person_export_data['state'] == '0'

        steps.CommonSteps.export('BY_FILE', classname='Client', object_id=agency_id)
        steps.CommonSteps.export('BY_FILE', classname='Person', object_id=person_id)
        steps.PersonSteps.create(client_id=agency_id, params={'person_id': person_id, 'phone': '11'},
                                 type_=context.person_type.code)
        person_export_data = steps.ExportSteps.get_export_data(classname='Person', object_id=person_id,
                                                               queue_type='BY_FILE')
        assert person_export_data['state'] == '0'
        steps.CommonSteps.export('BY_FILE', classname='Person', object_id=person_id)
        steps.CommonSteps.export('OEBS', classname='Invoice', object_id=invoice_id)
    else:
        try:
            invoice_export_data = steps.ExportSteps.get_export_data(classname='Invoice', object_id=invoice_id,
                                                                    queue_type='BY_FILE')
            assert 1 == 0
            assert invoice_export_data['state'] == '1'
        except Exception as exc:
            print exc

        try:
            client_export_data = steps.ExportSteps.get_export_data(classname='Client', object_id=agency_id,
                                                                   queue_type='BY_FILE')
            assert client_export_data['state'] == '1'
            assert 1 == 0
        except Exception as exc:
            print exc
        try:
            person_export_data = steps.ExportSteps.get_export_data(classname='Person', object_id=person_id,
                                                                   queue_type='BY_FILE')
            assert person_export_data['state'] == '1'
            steps.CommonSteps.export('BY_FILE', classname='Client', object_id=agency_id)
            steps.CommonSteps.export('BY_FILE', classname='Person', object_id=person_id)
            assert 1 == 0
        except Exception as exc:
            print exc


@pytest.mark.parametrize('context', [
    DIRECT_BEL_FIRM_BYN,
    DIRECT_FISH_RUB_CONTEXT

])
def test_export_act(context):
    agency_id = steps.ClientSteps.create({'IS_AGENCY': 1})
    steps.ClientSteps.create({'IS_AGENCY': 0})
    invoice_owner = agency_id
    person_id = steps.PersonSteps.create(invoice_owner, context.person_type.code)
    qty = QTY
    service_order_id = steps.OrderSteps.next_id(service_id=context.service.id)
    steps.OrderSteps.create(client_id=agency_id, product_id=context.product.id, service_id=context.service.id,
                            service_order_id=service_order_id, params={'AgencyID': agency_id})
    orders_list = [{'ServiceID': context.service.id, 'ServiceOrderID': service_order_id, 'Qty': qty, 'BeginDT': NOW}]
    request_id = steps.RequestSteps.create(client_id=agency_id if agency_id else agency_id, orders_list=orders_list,
                                           additional_params={'InvoiceDesireDT': NOW})

    invoice_id, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id,
                                                 paysys_id=context.paysys.id,
                                                 credit=0, contract_id=None, overdraft=0, endbuyer_id=None)

    steps.InvoiceSteps.pay(invoice_id)
    steps.CampaignsSteps.do_campaigns(context.service.id, service_order_id, {context.product.type.code: qty}, 0, NOW)
    act_id = steps.ActsSteps.generate(agency_id, force=1, date=NOW)[0]

    if context == DIRECT_BEL_FIRM_BYN:
        act_export_data = steps.ExportSteps.get_export_data(classname='Act', object_id=act_id,
                                                            queue_type='BY_FILE')
        assert act_export_data['state'] == '0'
        steps.CommonSteps.export('BY_FILE', classname='Act', object_id=act_id)
        steps.CommonSteps.export('OEBS', classname='Act', object_id=act_id)
    else:
        try:
            act_export_data = steps.ExportSteps.get_export_data(classname='Act', object_id=act_id,
                                                                queue_type='BY_FILE')
            assert 1 == 0
            assert act_export_data['state'] == '1'
        except Exception as exc:
            print exc

        try:
            client_export_data = steps.ExportSteps.get_export_data(classname='Client', object_id=agency_id,
                                                                   queue_type='BY_FILE')
            assert client_export_data['state'] == '1'
            assert 1 == 0
        except Exception as exc:
            print exc
        try:
            person_export_data = steps.ExportSteps.get_export_data(classname='Person', object_id=person_id,
                                                                   queue_type='BY_FILE')
            assert person_export_data['state'] == '1'
            steps.CommonSteps.export('BY_FILE', classname='Client', object_id=agency_id)
            steps.CommonSteps.export('BY_FILE', classname='Person', object_id=person_id)
            assert 1 == 0
        except Exception as exc:
            print exc


@pytest.mark.parametrize('context', [
    DIRECT_BEL_FIRM_BYN,
    DIRECT_FISH_RUB_CONTEXT

])
def test_export_act_internal(context):
    agency_id = steps.ClientSteps.create({'IS_AGENCY': 1})
    steps.ClientSteps.create({'IS_AGENCY': 0})
    invoice_owner = agency_id
    person_id = steps.PersonSteps.create(invoice_owner, context.person_type.code)
    qty = QTY
    service_order_id = steps.OrderSteps.next_id(service_id=context.service.id)
    order_id =steps.OrderSteps.create(client_id=agency_id, product_id=context.product.id, service_id=context.service.id,
                            service_order_id=service_order_id, params={'AgencyID': agency_id})
    orders_list = [{'ServiceID': context.service.id, 'ServiceOrderID': service_order_id, 'Qty': qty, 'BeginDT': NOW}]
    steps.InvoiceSteps.pay_with_certificate_or_compensation(order_id=order_id, sum=300)
    steps.CampaignsSteps.do_campaigns(context.service.id, service_order_id, {context.product.type.code: qty}, 0, NOW)
    act_id = steps.ActsSteps.generate(agency_id, force=1, date=NOW)[0]

    if context == DIRECT_BEL_FIRM_BYN:
        act_export_data = steps.ExportSteps.get_export_data(classname='Act', object_id=act_id,
                                                            queue_type='BY_FILE')
        assert act_export_data['state'] == '0'
        steps.CommonSteps.export('BY_FILE', classname='Act', object_id=act_id)
        steps.CommonSteps.export('OEBS', classname='Act', object_id=act_id)
    else:
        try:
            act_export_data = steps.ExportSteps.get_export_data(classname='Act', object_id=act_id,
                                                                queue_type='BY_FILE')
            assert 1 == 0
            assert act_export_data['state'] == '1'
        except Exception as exc:
            print exc

        try:
            client_export_data = steps.ExportSteps.get_export_data(classname='Client', object_id=agency_id,
                                                                   queue_type='BY_FILE')
            assert client_export_data['state'] == '1'
            assert 1 == 0
        except Exception as exc:
            print exc
        try:
            person_export_data = steps.ExportSteps.get_export_data(classname='Person', object_id=person_id,
                                                                   queue_type='BY_FILE')
            assert person_export_data['state'] == '1'
            steps.CommonSteps.export('BY_FILE', classname='Client', object_id=agency_id)
            steps.CommonSteps.export('BY_FILE', classname='Person', object_id=person_id)
            assert 1 == 0
        except Exception as exc:
            print exc
