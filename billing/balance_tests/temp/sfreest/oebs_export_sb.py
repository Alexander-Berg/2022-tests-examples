# -*- coding: utf-8 -*-
__author__ = 'sfreest'

import contextlib
import exceptions
from decimal import Decimal as D

import datetime as dt
import json

import btestlib.utils as utils
import btestlib.reporter as reporter
from balance import balance_steps as steps
from balance import balance_db as db


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


def export_contract(contract_id, with_client=True, with_person=True):
    data, = get_contract2(contract_id)
    client_id, person_id, external_id = data['client_id'], data['person_id'], uni(data['external_id'])
    if with_client:
        export_object('Client', client_id)
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


def export_personal_accounts(contract_id, update_dt_to_current_month=False, with_acts=False):
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
            export_acts(invoice_id)

def export_acts(invoice_id):
    acts_data = get_acts(invoice_id)
    for act_data in acts_data:
        act_id, act_eid, act_dt = act_data['id'], act_data['external_id'], act_data['dt']
        export_object(
            'Act', act_id,
            success_message=u'Act: {act_eid} (id: {act_id}), dt: {act_dt}'.format(
                act_eid=act_eid, act_id=act_id, act_dt=act_dt),
            error_message=u'Act: {act_eid} (id: {act_id}), dt: {act_dt}'.format(
                act_eid=act_eid, act_id=act_id, act_dt=act_dt),
        )


def export_all_by_contract(contract_id, with_linked_contracts=True, with_personal_accounts=True, with_acts=True):
    with GLOBAL_REP.report_shift(u'>EXPORTING ALL FOR CONTRACT_ID {contract_id}'.format(contract_id=contract_id)):
        if with_linked_contracts:
            with GLOBAL_REP.report_shift(u'>EXPORTING LINKED CONTRACTS'):
                linked_contracts_ids = get_linked_contract_ids(contract_id)
                if not linked_contracts_ids:
                    GLOBAL_REP.append_line(u'!NO LINKED CONTRACTS FOUND')
                else:
                    for link_contract_id in linked_contracts_ids:
                        with GLOBAL_REP.report_shift(u'>EXPORTING CONTRACT_ID {contract_id}'.format(contract_id=contract_id)):
                            export_contract(link_contract_id)
                            if with_personal_accounts:
                                export_personal_accounts(link_contract_id, update_dt_to_current_month=False, with_acts=with_acts)

        with GLOBAL_REP.report_shift(u'>EXPORTING MAIN CONTRACT'):
            export_contract(contract_id)
            if with_personal_accounts:
                export_personal_accounts(contract_id, update_dt_to_current_month=False, with_acts=with_acts)


def test_olol():
    # try:
    #     # export_all_by_contract(16615056)
    #     export_all_by_contract(17903198, with_linked_contracts=False)
    # except:
    #     GLOBAL_REP.print_report()
    #     raise
    # GLOBAL_REP.print_report()
    # for payment_id in (
    #     6629641456,
    #     6629641487,
    #     6629641157,
    # ):
    #     steps.CommonPartnerSteps.export_payment(payment_id)
    #
    # # такси ЛСД
    # steps.ExportSteps.export_oebs(client_id=1355234904)
    # steps.ExportSteps.export_oebs(person_id=19309702)
    # steps.ExportSteps.export_oebs(contract_id=15556336)
    #
    # #AGENT_REWARD
    # steps.ExportSteps.export_oebs(invoice_id=147183040)
    # #DEPOSITION
    # steps.ExportSteps.export_oebs(invoice_id=147183038)
    # #YANDEX_SERVICE
    # steps.ExportSteps.export_oebs(invoice_id=147183041)
    #
    # # такси ЛСЗ
    #
    # steps.ExportSteps.export_oebs(contract_id=15798024)
    # steps.ExportSteps.export_oebs(person_id=19309702)
    # steps.ExportSteps.export_oebs(invoice_id=147572797)
    #
    # # АЗС
    # steps.ExportSteps.export_oebs(client_id=1355478807)
    # steps.ExportSteps.export_oebs(person_id=19659323)
    # steps.ExportSteps.export_oebs(contract_id=15835402)
    # steps.ExportSteps.export_oebs(invoice_id=147630990)
    #
    # tt_ids = [
    #     193749030199,
    #     193749030209,
    #     193749097299,
    #     193749208979,
    #     193749208989,
    #     193749472219,
    #     193749209299,
    #     193749209309,
    #     193749518699,
    #     193749210639,
    #     193749210649,
    #     193749518749,
    #     193749241439,
    #     193749241449,
    #     193749472249,
    #     193749241979,
    #     193749241989,
    #     193749472279,
    #     193749518779,
    #     193749254819,
    #     193749254829,
    #     193749259989,
    #     193749259999,
    #     193749472309,
    #     193749263199,
    #     193749263209,
    #     193749472339,
    #     193749271209,
    #     193749271219,
    #     193749472369,
    #     193749308819,
    #     193749308829,
    #     193749518809,
    #     193749463849,
    #     193749463859,
    #     193749518849,
    #     193749464219,
    #     193749518879,
    #     193749464209,
    # ]
    #
    # for tt_id in tt_ids:
    #     str_tt_id = '66' + str(tt_id)
    #     try:
    #         steps.ExportSteps.create_export_record(str_tt_id, classname='ThirdPartyTransaction', type='OEBS')
    #     except:
    #         pass
    #     steps.ExportSteps.export_oebs(transaction_id=str_tt_id)
    #
    steps.ExportSteps.export_oebs(product_id=513753)
    steps.ExportSteps.export_oebs(product_id=513755)

    steps.ExportSteps.export_oebs(act_id=156670126)
    steps.ExportSteps.export_oebs(act_id=156670127)






# client_log = steps.ExportSteps.get_oebs_api_response('Client', client_id)
# person_log = steps.ExportSteps.get_oebs_api_response('Person', person_id)
# contract_log = steps.ExportSteps.get_oebs_api_response('Contract', contract_id)
# pa_log = steps.ExportSteps.get_oebs_api_response('Invoice', pa_id)
# act_log = steps.ExportSteps.get_oebs_api_response('Act', act_data['id'])

# report_dir = u'/Users/sfreest/Documents/reports'
# report = [
#     u'{region_name} {currency}'.format(region_name=context.region.name, currency=context.currency.iso_code),
#     u'Client: {client_id}, {log}'.format(client_id=client_id, log=client_log[0]),
#     u'Person: {person_id}, {log}'.format(person_id=person_id, log=person_log[0]),
#     u'Contract: {contract_eid}, {log}'.format(contract_eid=contract_eid, log=contract_log[0]),
#     u'Invoice: {pa_eid}, {log}'.format(pa_eid=pa_eid, log=pa_log[0]),
#     u'Act: {act_eid}, {log}'.format(act_eid=act_data['external_id'], log=act_log[0]),
#     u'\n',
# ]
# report = [r.encode('utf8') for r in report]
