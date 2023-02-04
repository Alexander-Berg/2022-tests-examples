# -*- coding: utf-8 -*-
import datetime as dt
import os
import contextlib
import exceptions
import pytest
import json

from decimal import Decimal as D

import btestlib.data.partner_contexts as pc
from balance.balance_steps.contract_steps import ContractSteps
from balance.balance_steps.export_steps import ExportSteps
from btestlib.constants import NdsNew, SpendablePaymentType

from btestlib import utils as utils, reporter as reporter
from balance import balance_steps as steps, balance_db as db

MONTH_START = dt.datetime(2022, 3, 1)
MONTH_END = dt.datetime(2022, 3, 31)


def uni(x):
    if isinstance(x, unicode):
        return x
    try:
        return unicode(x)
    except:
        pass
    try:
        return unicode(str(x), 'utf-8')
    except exceptions.UnicodeDecodeError:
        try:
            return unicode(str(x), 'cp1251')
        except exceptions.UnicodeDecodeError:
            return unicode(repr(x))


def trunc_date(date):
    return dt.datetime.combine(date.date(), dt.time(0))


class Reporter(object):
    def __init__(self):
        self.report = [u'\n']
        self.shift = 0
        self.shift_symbol = u' '

    @contextlib.contextmanager
    def report_shift(self, message=u'', shifts=2, shift_symbol=u' '):
        assert self.shift >= 0
        old_shift = self.shift
        old_shift_symbol = self.shift_symbol
        if message:
            self.append_line(message)
        try:
            self.shift += shifts
            self.shift_symbol = shift_symbol
            yield
        finally:
            self.shift = old_shift
            self.shift_symbol = old_shift_symbol

    def append_line(self, line):
        self.report.append(self.shift_symbol * self.shift + uni(line))

    @property
    def last_line(self):
        return self.report[-1]

    def pop_line(self):
        self.report.pop()

    def print_report(self):
        # report = [r.encode('utf-8').decode('unicode-escape') for r in self.report]
        report = u'\n'.join(self.report)
        # reporter.step(report)
        print(report)


GLOBAL_REP = Reporter()


def get_oebs_api_response(classname, object_id):
    log = steps.ExportSteps.get_oebs_api_response(classname, object_id)
    log = uni(json.dumps(log[0], ensure_ascii=False).encode('utf8')) if log else None
    return log


def get_contract2(contract_id):
    query = 'SELECT * FROM bo.t_contract2 where id=:contract_id'
    query_params = {'contract_id': contract_id}
    return db.balance().execute(query, query_params)


def get_products_by_orders(client_id):
    query = 'SELECT service_code FROM bo.t_order where client_id=:client_id'
    query_params = {'client_id': client_id}
    return [i['service_code'] for i in db.balance().execute(query, query_params)]


def get_linked_contract_ids(contract_id):
    query = """
select distinct ca.value_num link_contract_id
from bo.t_contract2 c
join bo.t_contract_collateral cc on cc.contract2_id = c.id
join bo.t_contract_attributes ca on ca.attribute_batch_id = cc.attribute_batch_id
  and ca.code = 'LINK_CONTRACT_ID'
  and ca.value_num is not null
where 1=1
  and (cc.is_signed is not null or cc.is_faxed is not null) and cc.is_cancelled is null
  and c.id = :contract_id"""
    query_params = {'contract_id': contract_id}
    linked_contract_ids = db.balance().execute(query, query_params)
    return [i['link_contract_id'] for i in linked_contract_ids]


def get_personal_accounts(contract_id):
    query = """
select i.id, i.external_id, ep.value_str service_code, i.dt
from bo.t_invoice i
left join bo.t_extprops ep on ep.object_id = i.id
  and ep.classname = 'PersonalAccount'
  and ep.attrname = 'service_code'
where 1=1
  and i.contract_id = :contract_id
  and i.type = 'personal_account'
"""
    query_params = {'contract_id': contract_id}
    return db.balance().execute(query, query_params)


def get_acts(invoice_id):
    query = """
select id, external_id, dt from bo.t_act
where invoice_id=:invoice_id
"""
    query_params = {'invoice_id': invoice_id}
    return db.balance().execute(query, query_params)


export_param_name_mapping = {
    'Client': 'client_id',
    'Person': 'person_id',
    'Contract': 'contract_id',
    'Invoice': 'invoice_id',
    'Act': 'act_id',
    'Product': 'product_id',
}


def export_object(classname, object_id, success_message=None, error_message=None, silent=False):
    success_message = success_message or u'{classname} id: {object_id}'.format(classname=classname, object_id=object_id)
    error_message = error_message or u'{classname} id: {object_id}'.format(classname=classname, object_id=object_id)
    with GLOBAL_REP.report_shift():
        param = {export_param_name_mapping[classname]: object_id}
        try:
            steps.ExportSteps.export_oebs(**param)
            if not silent:
                log = get_oebs_api_response(classname, object_id)
                GLOBAL_REP.append_line(u'{success_message} ---> {log}'.format(success_message=success_message, log=log))
        except utils.TestsError as e:
            if not silent:
                log = get_oebs_api_response(classname, object_id)
                GLOBAL_REP.append_line(u'{error_message} -X-> {log}'.format(error_message=error_message, log=log))
            raise


def export_contract(contract_id, with_client=True, with_person=True, with_products=False):
    data, = get_contract2(contract_id)
    client_id, person_id, external_id = data['client_id'], data['person_id'], uni(data['external_id'])
    if with_client:
        export_object('Client', client_id)
    if with_products:
        products = get_products_by_orders(client_id)
        for product_id in products:
            export_object('Product', product_id)
    if with_person:
        export_object('Person', person_id)
    export_object(
        'Contract', contract_id,
         success_message=u'Contract: {external_id} (id: {contract_id})'.format(external_id=external_id, contract_id=contract_id),
         error_message=u'Contract: {external_id} (id: {contract_id})'.format(external_id=external_id, contract_id=contract_id)
    )


def update_invoice_dt(invoice_id, invoice_dt=None):
    if invoice_dt is None:
        invoice_dt = trunc_date(dt.datetime.now()).replace(day=1)
    str_date = invoice_dt.strftime('%Y-%m-%d')
    query = """
update bo.t_invoice
set dt = date'{str_date}'
where id={invoice_id}
    """.format(str_date=str_date, invoice_id=invoice_id)
    return db.balance().execute(query)


def export_personal_accounts(contract_id, update_dt_to_current_month=False, with_acts=False,
                             skip_acts_in_closed_periods=False):
    personal_accounts_data = get_personal_accounts(contract_id)
    for inv_data in personal_accounts_data:
        invoice_id, invoice_eid, service_code = inv_data['id'], inv_data['external_id'], uni(inv_data['service_code'])
        if update_dt_to_current_month:
            update_invoice_dt(invoice_id, invoice_dt=dt.datetime(2022, 3, 1))
        export_object(
            'Invoice', invoice_id,
            success_message=u'Personal account: {invoice_eid} (id: {invoice_id}) [{service_code}]'.format(
                invoice_eid=invoice_eid, invoice_id=invoice_id, service_code=service_code),
            error_message=u'Personal account: {invoice_eid} (id: {invoice_id}) [{service_code}]'.format(
                invoice_eid=invoice_eid, invoice_id=invoice_id, service_code=service_code)
        )
        if with_acts:
            export_acts(invoice_id, skip_acts_in_closed_periods=skip_acts_in_closed_periods)


act_period_error_template = u'Попытка создания акта не в открытом периоде'


def export_acts(invoice_id, skip_acts_in_closed_periods=False):
    acts_data = get_acts(invoice_id)
    for act_data in acts_data:
        act_id, act_eid, act_dt = act_data['id'], act_data['external_id'], act_data['dt']
        try:
            export_object(
                'Act', act_id,
                success_message=u'Act: {act_eid} (id: {act_id}), dt: {act_dt}'.format(
                    act_eid=act_eid, act_id=act_id, act_dt=act_dt),
                error_message=u'Act: {act_eid} (id: {act_id}), dt: {act_dt}'.format(
                    act_eid=act_eid, act_id=act_id, act_dt=act_dt),
            )
        except utils.TestsError as e:
            if skip_acts_in_closed_periods and act_period_error_template in GLOBAL_REP.last_line:
                GLOBAL_REP.pop_line()
                db.balance().execute(
                    '''update t_export set state = 1 where classname = :classname and object_id = :object_id''',
                    {
                        'object_id': act_id, 'classname': 'Act'
                    }
                )
            else:
                raise


def export_all_by_contract(contract_id, with_linked_contracts=True, with_personal_accounts=True, with_acts=True,
                           skip_acts_in_closed_periods=False, with_products=False):
    with GLOBAL_REP.report_shift(u'>EXPORTING ALL FOR CONTRACT_ID {contract_id}'.format(contract_id=contract_id)):
        if with_linked_contracts:
            with GLOBAL_REP.report_shift(u'>EXPORTING LINKED CONTRACTS'):
                linked_contracts_ids = get_linked_contract_ids(contract_id)
                if not linked_contracts_ids:
                    GLOBAL_REP.append_line(u'!NO LINKED CONTRACTS FOUND')
                else:
                    for link_contract_id in linked_contracts_ids:
                        with GLOBAL_REP.report_shift(u'>EXPORTING CONTRACT_ID {contract_id}'.format(contract_id=link_contract_id)):
                            export_contract(link_contract_id, with_products=with_products)
                            if with_personal_accounts:
                                export_personal_accounts(link_contract_id, update_dt_to_current_month=False, with_acts=with_acts)

        with GLOBAL_REP.report_shift(u'>EXPORTING MAIN CONTRACT'):
            export_contract(contract_id, with_products=with_products)
            if with_personal_accounts:
                export_personal_accounts(contract_id, update_dt_to_current_month=False, with_acts=with_acts,
                                         skip_acts_in_closed_periods=skip_acts_in_closed_periods)


@pytest.mark.parametrize(
    "context, spendable_contexts",
    [
        (
            pc.TAXI_MLU_EUROPE_CONGO_EUR_CONTEXT,
            [
                pc.TAXI_MLU_EUROPE_CONGO_EUR_CONTEXT_SPENDABLE,
                pc.TAXI_MLU_EUROPE_CONGO_EUR_CONTEXT_SPENDABLE_SCOUTS,
            ],
        ),
    ],
    ids=lambda c, s: c.name,
)
def test_create_general_spendable_and_scouts_contracts(context, spendable_contexts):
    additional_params = {"start_dt": MONTH_START}

    (
        client_id,
        person_id,
        contract_id,
        contract_eid,
    ) = ContractSteps.create_partner_contract(
        context, additional_params=additional_params
    )

    additional_params.update(
        {
            "nds": NdsNew.ZERO.nds_id,
            "payment_type": SpendablePaymentType.MONTHLY,
            "link_contract_id": contract_id,
        }
    )

    for ctx in spendable_contexts:
        (
            _,
            spendable_person_id,
            spendable_contract_id,
            spendable_contract_eid,
        ) = ContractSteps.create_partner_contract(
            ctx,
            client_id=client_id,
            unsigned=False,
            additional_params=additional_params,
        )


def test_export():
    try:
        export_all_by_contract(17990457, with_linked_contracts=False, skip_acts_in_closed_periods=True)
    except Exception as e:
        GLOBAL_REP.print_report()
        raise
    GLOBAL_REP.print_report()
