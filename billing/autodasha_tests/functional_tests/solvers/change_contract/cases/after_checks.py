# -*- coding: utf-8 -*-

from __future__ import unicode_literals

import datetime as dt

from autodasha.core.api.tracker import IssueTransitions

from tests.autodasha_tests.common import db_utils
from tests.autodasha_tests.functional_tests import case_utils
from tests.autodasha_tests.functional_tests.solvers.change_contract.common import (
    AbstractBaseDBTestCase, RequiredResult, get_approve_message)


__all__ = ['AbstractAfterChecksDBTestCase']


class AbstractAfterChecksDBTestCase(AbstractBaseDBTestCase):
    _cases = []
    __metaclass__ = case_utils.TestCaseMeta

    author = 'noob'

    def get_comments(self):
        return [
            ('boss', 'Подтверждено'),
            ('accountant', 'Подтверждено'),
        ]


class ContractMovedFinishDTDBTestCase(AbstractAfterChecksDBTestCase):
    _representation = 'c_moved_finish_dt'

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        self.contract = db_utils.create_general_contract(session, client, person, dt.datetime(2016, 1, 1),
                                                         finish_dt=dt.datetime(2016, 2, 1))
        self.contract_info = db_utils.get_contract_information_for_support_approve(session, self.contract)

        order = db_utils.create_order(session, client)
        invoice = db_utils.create_invoice(session, client, [(order, 1)], person=person)
        invoice.contract = self.contract
        invoice.dt = dt.datetime(2016, 1, 20)

        return self.contract, [
            ('finish_dt', dt.datetime(2016, 1, 10)),
        ]

    def get_result(self):
        res = RequiredResult(self.contract, self.collateral, delay=True,
                             transition=IssueTransitions.none,
                             state=[('finish_dt', dt.datetime(2016, 2, 1))],
                             assignee='mscnad7')
        res.add_message('кто:mscnad7, проверь, пожалуйста, и подтверди.')
        res.add_message('В договоре № %s от %s,' %
                        (self.contract.external_id, self.contract.col0.dt.strftime('%d.%m.%Y')))
        res.add_message('тип - %s, фирма - %s, сервисы - %s:' % self.contract_info)
        res.add_message('* меняем параметр "Дата окончания" с 01.02.2016 на 10.01.2016;')
        res.add_message('')
        res.add_message('Коллеги, обратите, пожалуйста, внимание:')
        res.add_message('* Есть счета, выставленные после даты окончания договора (10.01.2016);')
        res.add_message(get_approve_message())

        return res


class ColMovedFinishDTDBTestCase(AbstractAfterChecksDBTestCase):
    _representation = 'col_moved_finish_dt'

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        self.contract = db_utils.create_general_contract(session, client, person, dt.datetime(2016, 1, 1))
        self.contract_info = db_utils.get_contract_information_for_support_approve(session, self.contract)
        self.collateral = db_utils.add_collateral(self.contract, 90, dt.datetime(2016, 2, 1),
                                                  is_signed=dt.datetime(2016, 2, 1),
                                                  finish_dt=dt.datetime(2016, 2, 1))

        order = db_utils.create_order(session, client)
        invoice = db_utils.create_invoice(session, client, [(order, 1)], person=person)
        invoice.contract = self.contract
        invoice.dt = dt.datetime(2016, 1, 20)

        return self.contract, [
            ('col', self.collateral),
            ('dt', dt.datetime(2016, 1, 10)),
            ('finish_dt', dt.datetime(2016, 1, 10)),
        ]

    def get_result(self):
        res = RequiredResult(self.contract, self.collateral, delay=True,
                             transition=IssueTransitions.none,
                             state=[
                                 ('dt', dt.datetime(2016, 2, 1)),
                                 ('finish_dt', dt.datetime(2016, 2, 1)),
                             ],
                             assignee='mscnad7')
        res.add_message('кто:mscnad7, проверь, пожалуйста, и подтверди.')
        res.add_message('В договоре № %s от %s, ДС № 01 (расторжение договора) от 01.02.2016,' %
                        (self.contract.external_id, self.contract.col0.dt.strftime('%d.%m.%Y')))
        res.add_message('тип - %s, фирма - %s, сервисы - %s:' % self.contract_info)
        res.add_message('* меняем параметр "Дата начала" с 01.02.2016 на 10.01.2016;')
        res.add_message('* меняем параметр "Дата окончания" с 01.02.2016 на 10.01.2016;')
        res.add_message('')
        res.add_message('Коллеги, обратите, пожалуйста, внимание:')
        res.add_message('* Есть счета, выставленные после даты окончания договора (10.01.2016);')
        res.add_message(get_approve_message())

        return res


class ColCancelledFinishDTDBTestCase(AbstractAfterChecksDBTestCase):
    _representation = 'col_cancelled_finish_dt'

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        self.contract = db_utils.create_general_contract(session, client, person, dt.datetime(2016, 1, 1),
                                                         finish_dt=dt.datetime(2016, 1, 10))
        self.contract_info = db_utils.get_contract_information_for_support_approve(session, self.contract)
        self.collateral = db_utils.add_collateral(self.contract, 90, dt.datetime(2016, 2, 1),
                                                  is_signed=dt.datetime(2016, 2, 1),
                                                  finish_dt=dt.datetime(2016, 2, 1))

        order = db_utils.create_order(session, client)
        invoice = db_utils.create_invoice(session, client, [(order, 1)], person=person)
        invoice.contract = self.contract
        invoice.dt = dt.datetime(2016, 1, 20)

        return self.contract, [
            ('col', self.collateral),
            ('remove_is_signed', True),
            ('is_cancelled', True),
        ]

    def get_result(self):
        res = RequiredResult(self.contract, self.collateral, delay=True,
                             transition=IssueTransitions.none,
                             state=[
                                 ('is_signed', dt.datetime(2016, 2, 1)),
                                 ('is_cancelled', None),
                             ],
                             assignee='mscnad7')
        res.add_message('кто:mscnad7, проверь, пожалуйста, и подтверди.')
        res.add_message('В договоре № %s от %s, ДС № 01 (расторжение договора) от 01.02.2016,' %
                        (self.contract.external_id, self.contract.col0.dt.strftime('%d.%m.%Y')))
        res.add_message('тип - %s, фирма - %s, сервисы - %s:' % self.contract_info)
        res.add_message('* меняем параметр "Аннулирован" с Нет на Да;')
        res.add_message('* убираем параметр "Подписан", сейчас 01.02.2016;')
        res.add_message('')
        res.add_message('Коллеги, обратите, пожалуйста, внимание:')
        res.add_message('* Есть счета, выставленные после даты окончания договора (10.01.2016);')
        res.add_message(get_approve_message())

        return res


class UnchangedFinishDTDBTestCase(AbstractAfterChecksDBTestCase):
    _representation = 'unchanged_finish_dt'

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        self.contract = db_utils.create_general_contract(session, client, person, dt.datetime(2016, 1, 1),
                                                         finish_dt=dt.datetime(2016, 2, 1))
        self.contract_info = db_utils.get_contract_information_for_support_approve(session, self.contract)
        order = db_utils.create_order(session, client)
        invoice = db_utils.create_invoice(session, client, [(order, 1)], person=person)
        invoice.contract = self.contract
        invoice.dt = dt.datetime(2016, 2, 20)

        return self.contract, [
            ('is_faxed', dt.datetime(2016, 3, 3)),
        ]

    def get_result(self):
        res = RequiredResult(self.contract, self.collateral, delay=False, done=True, enqueued=True,
                             transition=IssueTransitions.none,
                             state=[
                                 ('is_signed', dt.datetime(2016, 1, 1)),
                                 ('is_faxed', dt.datetime(2016, 3, 3)),
                                 ('finish_dt', dt.datetime(2016, 2, 1)),
                             ])
        return res


class NoFinishDTDBTestCase(AbstractAfterChecksDBTestCase):
    _representation = 'no_finish_dt'

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        self.contract = db_utils.create_general_contract(session, client, person, dt.datetime(2016, 1, 1),
                                                         is_faxed=dt.datetime(2015, 1, 1))
        self.contract_info = db_utils.get_contract_information_for_support_approve(session, self.contract)
        order = db_utils.create_order(session, client)
        invoice = db_utils.create_invoice(session, client, [(order, 1)], person=person)
        invoice.contract = self.contract
        invoice.dt = dt.datetime(2016, 2, 20)

        return self.contract, [
            ('is_faxed', dt.datetime(2016, 1, 1)),
        ]

    def get_result(self):
        res = RequiredResult(self.contract, self.collateral, delay=False, done=True, enqueued=True,
                             transition=IssueTransitions.none,
                             state=[
                                 ('is_signed', dt.datetime(2016, 1, 1)),
                                 ('is_faxed', dt.datetime(2016, 1, 1)),
                             ])
        return res


class RemovedFinishDTDBTestCase(AbstractAfterChecksDBTestCase):
    _representation = 'removed_finish_dt'

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        self.contract = db_utils.create_general_contract(session, client, person, dt.datetime(2016, 1, 1))
        self.contract_info = db_utils.get_contract_information_for_support_approve(session, self.contract)
        self.collateral = db_utils.add_collateral(self.contract, 90, dt.datetime(2016, 3, 1),
                                                  is_signed=dt.datetime(2016, 1, 1),
                                                  finish_dt=dt.datetime(2016, 3, 1))

        order = db_utils.create_order(session, client)
        invoice = db_utils.create_invoice(session, client, [(order, 1)], person=person)
        invoice.contract = self.contract
        invoice.dt = dt.datetime(2016, 2, 20)

        return self.contract, [
            ('col', self.collateral),
            ('remove_is_signed', True),
            ('is_cancelled', True)
        ]

    def get_result(self):
        res = RequiredResult(self.contract, self.collateral, delay=True,
                             transition=IssueTransitions.none,
                             state=[
                                 ('is_signed', dt.datetime(2016, 1, 1)),
                                 ('finish_dt', dt.datetime(2016, 3, 1)),
                                 ('is_cancelled', False),
                             ],
                             assignee='mscnad7')
        res.add_message('кто:mscnad7, проверь, пожалуйста, и подтверди.')
        res.add_message('В договоре № %s от %s, ДС № 01 (расторжение договора) от 01.03.2016,' %
                        (self.contract.external_id, self.contract.col0.dt.strftime('%d.%m.%Y')))
        res.add_message('тип - %s, фирма - %s, сервисы - %s:' % self.contract_info)
        res.add_message('* убираем параметр "Подписан", сейчас 01.01.2016;')
        res.add_message('* меняем параметр "Аннулирован" с Нет на Да;')
        res.add_message('')
        res.add_message(get_approve_message())

        return res


class FictiveInvoiceDBTestCase(AbstractAfterChecksDBTestCase):
    _representation = 'fictive_invoice_finish_dt'

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        self.contract = db_utils.create_general_contract(session, client, person, dt.datetime(2016, 1, 1),
                                                         payment_type=3)
        self.contract_info = db_utils.get_contract_information_for_support_approve(session, self.contract)
        order = db_utils.create_order(session, client)
        invoice = db_utils.create_fictive_invoice(session, self.contract, [(order, 1)])
        invoice.dt = dt.datetime(2016, 2, 20)
        self.contract.col0.finish_dt = dt.datetime(2016, 3, 1)

        return self.contract, [
            ('finish_dt', dt.datetime(2016, 2, 1)),
        ]

    def get_result(self):
        res = RequiredResult(self.contract, self.collateral, delay=True,
                             transition=IssueTransitions.none,
                             state=[
                                 ('is_signed', dt.datetime(2016, 1, 1)),
                                 ('finish_dt', dt.datetime(2016, 3, 1)),
                             ],
                             assignee='mscnad7')
        res.add_message('кто:mscnad7, проверь, пожалуйста, и подтверди.')
        res.add_message('В договоре № %s от %s,' %
                        (self.contract.external_id, self.contract.col0.dt.strftime('%d.%m.%Y')))
        res.add_message('тип - %s, фирма - %s, сервисы - %s:' % self.contract_info)
        res.add_message('* меняем параметр "Дата окончания" с 01.03.2016 на 01.02.2016;')
        res.add_message('')
        res.add_message(get_approve_message())

        return res


class PartnersMovedFinishDTDBTrueTestCase(AbstractAfterChecksDBTestCase):
    _representation = 'partners_moved_finish_dt_true_check'

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        self.contract = db_utils.create_partners_contract(session, client, person, dt.datetime(2016, 1, 1),
                                                          end_dt=dt.datetime(2016, 3, 1))
        self.contract_info = db_utils.get_contract_information_for_support_approve(session, self.contract)
        ins_q = 'INSERT INTO bo.t_partner_act_data (partner_contract_id, status, dt) VALUES (:id, 0, :dt)'
        session.execute(ins_q, {'id': self.contract.id, 'dt': dt.datetime(2016, 2, 1)})

        return self.contract, [
            ('finish_dt', dt.datetime(2016, 1, 10)),
        ]

    def get_result(self):
        res = RequiredResult(self.contract, self.collateral, delay=True,
                             transition=IssueTransitions.none,
                             state=[('end_dt', dt.datetime(2016, 3, 1))],
                             assignee='mscnad7')
        res.add_message('кто:mscnad7, проверь, пожалуйста, и подтверди.')
        res.add_message('В договоре № %s от %s,' %
                        (self.contract.external_id, self.contract.col0.dt.strftime('%d.%m.%Y')))
        res.add_message('тип - %s, фирма - %s, сервисы - %s:' % self.contract_info)
        res.add_message('* меняем параметр "Дата окончания" с 01.03.2016 на 10.01.2016;')
        res.add_message('')
        res.add_message('Коллеги, обратите, пожалуйста, внимание:')
        res.add_message('* Есть партнёские акты (t_partner_act_data) после новой даты окончания договора;')
        res.add_message(get_approve_message())

        return res


class PartnersMovedFinishDTDBFalseTestCase(AbstractAfterChecksDBTestCase):
    _representation = 'partners_moved_finish_dt_false_check'

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        self.contract = db_utils.create_partners_contract(session, client, person, dt.datetime(2016, 1, 1),
                                                          end_dt=dt.datetime(2016, 3, 1))
        self.contract_info = db_utils.get_contract_information_for_support_approve(session, self.contract)
        ins_q = 'INSERT INTO bo.t_partner_act_data (partner_contract_id, status, dt) VALUES (:id, 0, :dt)'
        session.execute(ins_q, {'id': self.contract.id, 'dt': dt.datetime(2016, 2, 1)})

        return self.contract, [
            ('finish_dt', dt.datetime(2016, 2, 10)),
        ]

    def get_result(self):
        res = RequiredResult(self.contract, self.collateral, delay=True,
                             transition=IssueTransitions.none,
                             state=[('end_dt', dt.datetime(2016, 3, 1))], assignee='mscnad7')
        res.add_message('кто:mscnad7, проверь, пожалуйста, и подтверди.')
        res.add_message('В договоре № %s от %s,' %
                        (self.contract.external_id, self.contract.col0.dt.strftime('%d.%m.%Y')))
        res.add_message('тип - %s, фирма - %s, сервисы - %s:' % self.contract_info)
        res.add_message('* меняем параметр "Дата окончания" с 01.03.2016 на 10.02.2016;')
        res.add_message('')
        res.add_message(get_approve_message())

        return res


class PartnersColMovedFinishDTDBTrueTestCase(AbstractAfterChecksDBTestCase):
    _representation = 'partners_col_moved_finish_dt_true_check'

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        self.contract = db_utils.create_partners_contract(session, client, person, dt.datetime(2016, 1, 1))
        self.contract_info = db_utils.get_contract_information_for_support_approve(session, self.contract)
        self.collateral = db_utils.add_collateral(self.contract, 2090, dt.datetime(2016, 3, 1),
                                                  end_dt=dt.datetime(2016, 3, 1),
                                                  is_signed=dt.datetime(2016, 3, 1))

        ins_q = 'INSERT INTO bo.t_partner_act_data (partner_contract_id, status, dt) VALUES (:id, 0, :dt)'
        session.execute(ins_q, {'id': self.contract.id, 'dt': dt.datetime(2016, 2, 1)})

        return self.contract, [
            ('col', self.collateral),
            ('dt', dt.datetime(2016, 1, 10)),
            ('finish_dt', dt.datetime(2016, 1, 10)),
        ]

    def get_result(self):
        res = RequiredResult(self.contract, self.collateral, delay=True,
                             transition=IssueTransitions.none,
                             state=[
                                 ('dt', dt.datetime(2016, 3, 1)),
                                 ('end_dt', dt.datetime(2016, 3, 1)),
                             ],
                             assignee='mscnad7')
        res.add_message('кто:mscnad7, проверь, пожалуйста, и подтверди.')
        res.add_message('В договоре № %s от %s, ДС № 01 (РСЯ: о закрытии договора) от 01.03.2016,' %
                        (self.contract.external_id, self.contract.col0.dt.strftime('%d.%m.%Y')))
        res.add_message('тип - %s, фирма - %s, сервисы - %s:' % self.contract_info)
        res.add_message('* меняем параметр "Дата начала" с 01.03.2016 на 10.01.2016;')
        res.add_message('* меняем параметр "Дата окончания" с 01.03.2016 на 10.01.2016;')
        res.add_message('')
        res.add_message('Коллеги, обратите, пожалуйста, внимание:')
        res.add_message('* Есть партнёские акты (t_partner_act_data) после новой даты окончания договора;')
        res.add_message(get_approve_message())

        return res


class PartnersColMovedFinishDTDBFalseTestCase(AbstractAfterChecksDBTestCase):
    _representation = 'partners_col_moved_finish_dt_false_check'

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        self.contract = db_utils.create_partners_contract(session, client, person, dt.datetime(2016, 1, 1))
        self.contract_info = db_utils.get_contract_information_for_support_approve(session, self.contract)
        self.collateral = db_utils.add_collateral(self.contract, 2090, dt.datetime(2016, 3, 1),
                                                  end_dt=dt.datetime(2016, 3, 1),
                                                  is_signed=dt.datetime(2016, 3, 1))

        ins_q = 'INSERT INTO bo.t_partner_act_data (partner_contract_id, status, dt) VALUES (:id, 0, :dt)'
        session.execute(ins_q, {'id': self.contract.id, 'dt': dt.datetime(2016, 2, 1)})

        return self.contract, [
            ('col', self.collateral),
            ('dt', dt.datetime(2016, 1, 10)),
            ('finish_dt', dt.datetime(2016, 2, 10)),
        ]

    def get_result(self):
        res = RequiredResult(self.contract, self.collateral, delay=True,
                             transition=IssueTransitions.none,
                             state=[
                                 ('dt', dt.datetime(2016, 3, 1)),
                                 ('end_dt', dt.datetime(2016, 3, 1)),
                             ],
                             assignee='mscnad7')
        res.add_message('кто:mscnad7, проверь, пожалуйста, и подтверди.')
        res.add_message('В договоре № %s от %s, ДС № 01 (РСЯ: о закрытии договора) от 01.03.2016,' %
                        (self.contract.external_id, self.contract.col0.dt.strftime('%d.%m.%Y')))
        res.add_message('тип - %s, фирма - %s, сервисы - %s:' % self.contract_info)
        res.add_message('* меняем параметр "Дата начала" с 01.03.2016 на 10.01.2016;')
        res.add_message('* меняем параметр "Дата окончания" с 01.03.2016 на 10.02.2016;')
        res.add_message('')
        res.add_message(get_approve_message())

        return res


class PartnersMovedStartDTDBTrueTestCase(AbstractAfterChecksDBTestCase):
    _representation = 'partners_moved_start_dt_true_check'

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        self.contract = db_utils.create_partners_contract(session, client, person, dt.datetime(2016, 1, 1),
                                                          end_dt=dt.datetime(2016, 3, 1))
        self.contract_info = db_utils.get_contract_information_for_support_approve(session, self.contract)
        ins_q = 'INSERT INTO bo.t_partner_act_data (partner_contract_id, status, dt) VALUES (:id, 0, :dt)'
        session.execute(ins_q, {'id': self.contract.id, 'dt': dt.datetime(2016, 1, 1)})

        return self.contract, [
            ('dt', dt.datetime(2016, 2, 1)),
        ]

    def get_result(self):
        res = RequiredResult(self.contract, self.collateral, delay=True,
                             transition=IssueTransitions.none,
                             state=[('dt', dt.datetime(2016, 1, 1))],
                             assignee='mscnad7')
        res.add_message('кто:mscnad7, проверь, пожалуйста, и подтверди.')
        res.add_message('В договоре № %s от %s,' %
                        (self.contract.external_id, self.contract.col0.dt.strftime('%d.%m.%Y')))
        res.add_message('тип - %s, фирма - %s, сервисы - %s:' % self.contract_info)
        res.add_message('* меняем параметр "Дата начала" с 01.01.2016 на 01.02.2016;')
        res.add_message('')
        res.add_message('Коллеги, обратите, пожалуйста, внимание:')
        res.add_message('* Есть партнёские акты (t_partner_act_data) до новой даты начала действия договора;')
        res.add_message(get_approve_message())

        return res


class PartnersMovedStartDTDBFalseTestCase(AbstractAfterChecksDBTestCase):
    _representation = 'partners_moved_start_dt_false_check'

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        self.contract = db_utils.create_partners_contract(session, client, person, dt.datetime(2016, 1, 1),
                                                          end_dt=dt.datetime(2016, 3, 1))
        self.contract_info = db_utils.get_contract_information_for_support_approve(session, self.contract)
        ins_q = 'INSERT INTO bo.t_partner_act_data (partner_contract_id, status, dt) VALUES (:id, 0, :dt)'
        session.execute(ins_q, {'id': self.contract.id, 'dt': dt.datetime(2016, 1, 1)})

        return self.contract, [
            ('dt', dt.datetime(2016, 1, 6)),
        ]

    def get_result(self):
        res = RequiredResult(self.contract, self.collateral, delay=True,
                             transition=IssueTransitions.none,
                             state=[('dt', dt.datetime(2016, 1, 1))], assignee='mscnad7')
        res.add_message('кто:mscnad7, проверь, пожалуйста, и подтверди.')
        res.add_message('В договоре № %s от %s,' %
                        (self.contract.external_id, self.contract.col0.dt.strftime('%d.%m.%Y')))
        res.add_message('тип - %s, фирма - %s, сервисы - %s:' % self.contract_info)
        res.add_message('* меняем параметр "Дата начала" с 01.01.2016 на 06.01.2016;')
        res.add_message('')
        res.add_message(get_approve_message())

        return res


class PartnersColMovedStartDTDBTestCase(AbstractAfterChecksDBTestCase):
    _representation = 'partners_col_moved_start_dt'

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        self.contract = db_utils.create_partners_contract(session, client, person, dt.datetime(2016, 1, 1),
                                                          end_dt=dt.datetime(2016, 3, 1))
        self.contract_info = db_utils.get_contract_information_for_support_approve(session, self.contract)
        self.collateral = db_utils.add_collateral(self.contract, dt_=dt.datetime(2016, 1, 2),
                                                  is_signed=dt.datetime(2016, 1, 2),
                                                  collateral_type_id=2050)
        ins_q = 'INSERT INTO bo.t_partner_act_data (partner_contract_id, status, dt) VALUES (:id, 0, :dt)'
        session.execute(ins_q, {'id': self.contract.id, 'dt': dt.datetime(2016, 1, 1)})

        return self.contract, [
            ('col', self.collateral),
            ('dt', dt.datetime(2016, 2, 1)),
        ]

    def get_result(self):
        res = RequiredResult(self.contract, self.collateral, delay=True,
                             transition=IssueTransitions.none,
                             state=[('dt', dt.datetime(2016, 1, 2))],
                             assignee='mscnad7')
        res.add_message('кто:mscnad7, проверь, пожалуйста, и подтверди.')
        res.add_message('В договоре № %s от %s, ДС № 01 (РСЯ: расторжение договора) от 02.01.2016,' %
                        (self.contract.external_id, self.contract.col0.dt.strftime('%d.%m.%Y')))
        res.add_message('тип - %s, фирма - %s, сервисы - %s:' % self.contract_info)
        res.add_message('* меняем параметр "Дата начала" с 02.01.2016 на 01.02.2016;')
        res.add_message('')
        res.add_message(get_approve_message())

        return res


class PartnersDisabledDBTestCase(AbstractAfterChecksDBTestCase):
    _representation = 'partners_disabled'

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        self.contract = db_utils.create_partners_contract(session, client, person, dt.datetime(2016, 1, 1))
        self.contract_info = db_utils.get_contract_information_for_support_approve(session, self.contract)
        ins_q = 'INSERT INTO bo.t_partner_act_data (partner_contract_id, status, dt) VALUES (:id, 0, :dt)'
        session.execute(ins_q, {'id': self.contract.id, 'dt': dt.datetime(2016, 2, 1)})

        return self.contract, [
            ('remove_is_signed', True),
        ]

    def get_result(self):
        res = RequiredResult(self.contract, self.collateral, delay=True,
                             transition=IssueTransitions.none,
                             state=[('is_signed', dt.datetime(2016, 1, 1))],
                             assignee='mscnad7')
        res.add_message('кто:mscnad7, проверь, пожалуйста, и подтверди.')
        res.add_message('В договоре № %s от %s,' %
                        (self.contract.external_id, self.contract.col0.dt.strftime('%d.%m.%Y')))
        res.add_message('тип - %s, фирма - %s, сервисы - %s:' % self.contract_info)
        res.add_message('* убираем параметр "Подписан", сейчас 01.01.2016;')
        res.add_message('')
        res.add_message('Коллеги, обратите, пожалуйста, внимание:')
        res.add_message('* Есть партнёские акты (t_partner_act_data);')
        res.add_message(get_approve_message())

        return res


class ContractHasTTRow(AbstractAfterChecksDBTestCase):
    _representation = 'contract_has_tt_row'

    def setup_config(self, session, config):
        config['CHANGE_CONTRACT']['payments_check'] = True

    def _get_data(self, session):
        contract_params = {
            'country': 225,
            'partner_commission_pct2': 1,
            'firm': 13,
            'services': {124},
        }
        client, person = db_utils.create_client_person(session)
        self.contract = db_utils.create_general_contract(session, client, person, dt.datetime(2016, 1, 1),
                                                         **contract_params)
        col_attrs = {
        'print_form_type': 3,
        'partner_commission_pct': 66,
        'partner_commission_pct2': 666,
        'partner_commission_type': 2,
        'partner_min_commission_sum': 6,
        'partner_max_commission_sum': 6666,
        'num': 'super cool дс'
        }
        self.collateral = db_utils.add_collateral(self.contract, 1030, dt_=dt.datetime(2016, 6, 3), **col_attrs)
        self.contract_info = db_utils.get_contract_information_for_support_approve(session, self.contract)

        payment = db_utils.create_thirdparty_transaction(session, self.contract, 124, **{'dt': dt.datetime(2016, 6, 4)})

        return self.contract, [
            ('col', self.collateral),
            ('is_signed', dt.datetime(2016, 6, 5)),
        ]

    def get_result(self):
        res = RequiredResult(self.contract, self.collateral, delay=True,
                             transition=IssueTransitions.none,
                             state=[('is_signed', None)],
                             assignee='mscnad7')
        res.add_message('кто:mscnad7, проверь, пожалуйста, и подтверди.')
        res.add_message('В договоре № %s от %s, ДС № %s (%s) от %s,' %
                        (
                            self.contract.external_id,
                            self.contract.col0.dt.strftime('%d.%m.%Y'),
                            self.collateral.num,
                            self.collateral.collateral_type.caption,
                            self.collateral.dt.strftime('%d.%m.%Y')
                        )
                        )
        res.add_message('тип - %s, фирма - %s, сервисы - %s:' % self.contract_info)
        res.add_message('* добавляем параметр "Подписан", будет 05.06.2016;')
        res.add_message('')
        res.add_message('Коллеги, обратите, пожалуйста, внимание:')
        res.add_message('* !!(кра)Вносимые изменения затрагивают существующие транзакции по договору.')
        res.add_message('Подтверди, пожалуйста, если изменения всё-таки нужно внести или закрой тикет!!;')
        res.add_message(get_approve_message())

        return res


class ContractOverlapping(AbstractAfterChecksDBTestCase):
    _representation = 'contract_overlapping'

    def setup_config(self, session, config):
        config['CHANGE_CONTRACT']['payments_check'] = True

    def _get_data(self, session):
        contract_params = {
            'country': 225,
            'partner_commission_pct2': 1,
            'firm': 13,
            'services': {124},
        }
        client, person = db_utils.create_client_person(session)
        self.contract = db_utils.create_general_contract(session, client, person, dt.datetime(2016, 1, 1),
                                                         **contract_params)
        payment = db_utils.create_thirdparty_transaction(session, self.contract, 124, **{'dt': dt.datetime(2016, 6, 4)})
        contract_params.update({'finish_dt': dt.datetime(2015, 12, 31)})
        self.older_contract = db_utils.create_general_contract(session, client, person, dt.datetime(2015, 1, 1),
                                                         **contract_params)

        self.contract_info = db_utils.get_contract_information_for_support_approve(session, self.contract)

        return self.contract, [
            ('dt', dt.datetime(2015, 12, 1)),
        ]

    def get_result(self):
        res = RequiredResult(self.contract, self.collateral, delay=True,
                             transition=IssueTransitions.none,
                             state=[('dt', dt.datetime(2016, 1, 1))],
                             assignee='mscnad7')
        res.add_message('кто:mscnad7, проверь, пожалуйста, и подтверди.')
        res.add_message('В договоре № %s от %s,' %
                        (
                            self.contract.external_id,
                            self.contract.col0.dt.strftime('%d.%m.%Y'),
                        )
                        )
        res.add_message('тип - %s, фирма - %s, сервисы - %s:' % self.contract_info)
        res.add_message('* меняем параметр "Дата начала" с 01.01.2016 на 01.12.2015;')
        res.add_message('')
        res.add_message('Коллеги, обратите, пожалуйста, внимание:')
        res.add_message('* !!(кра)Вносимые изменения затрагивают существующие транзакции по договору.')
        res.add_message(
            'После изменений договор станет пересекающимся с договорами имеющими id: {}'.format(
                self.older_contract.id
        ))
        res.add_message('Подтверди, пожалуйста, если изменения всё-таки нужно внести или закрой тикет!!;')
        res.add_message(get_approve_message())

        return res


# class UnmatchedFinishDTDBTestCase(AbstractAfterChecksDBTestCase):
#     _representation = 'unmatched_finish_dt'
#
#     def _get_data(self, session):
#         client, person = db_utils.create_client_person(session)
#         self.contract = db_utils.create_general_contract(session, client, person, dt.datetime(2016, 1, 1))
#         self.collateral = db_utils.add_collateral(self.contract, 90, dt.datetime(2016, 2, 1),
#                                                   is_signed=dt.datetime(2016, 2, 1),
#                                                   finish_dt=dt.datetime(2016, 2, 1))
#
#         return self.contract, [
#             ('col', self.collateral),
#             ('finish_dt', dt.datetime(2016, 3, 1)),
#         ]
#
#     def get_result(self):
#         res = RequiredResult(self.contract, self.collateral, delay=True,
#                              transition=IssueTransitions.none,
#                              state=[
#                                  ('dt', dt.datetime(2016, 2, 1)),
#                                  ('finish_dt', dt.datetime(2016, 2, 1)),
#                              ],
#                              assignee='mscnad7')
#         res.add_message('кто:mscnad7, проверь, пожалуйста, и подтверди.')
#         res.add_message('В договоре № %s от %s, ДС № 01 от 01.02.2016:' %
#                         (self.contract.external_id, self.contract.col0.dt.strftime('%d.%m.%Y')))
#         res.add_message('* меняем параметр "Дата окончания" с 01.02.2016 на 01.03.2016;')
#         res.add_message('')
#         res.add_message('Коллеги, обратите, пожалуйста, внимание:')
#         res.add_message('* В ДС "на расторжение" даты начала (01.02.2016) и расторжения (01.03.2016) отличаются;')
#         res.add_message(get_approve_message())
#
#         return res
